#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <cassert>
#include <string>
#include <utility>
#include <set>
#include <errno.h>
#include <stdint.h>
#include <pthread.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <sys/epoll.h>

#include <curl/curl.h>
#include <offdb/QuickdbAdapter.h>

// g++ -o dumper dumper.cc -Wall -D_FILE_OFFSET_BITS=64 -loffdb -lpthread -lcurl

#define DEFAULT_CURSOR_TYPE double_buffer_cursor
#define DEFAULT_OFFSET      0

struct RoutineData {
  std::set<std::string> appids;
  std::string sos;
  
  int         accept;
  int         efd;
  CURLM      *multi;
  bool        quit;
};

struct OneTaskReq {
  void   *key;
  size_t  keylen;
  void   *value;
  size_t  valuelen;
  size_t  pos;
};

static void *routine(void *);
static bool qdbForeach(const char *, int);

int main(int argc, char *argv[])
{
  if (argc < 4) {
    fprintf(stderr, "%s qdb sos app1 app2 ... \n", argv[0]);
    return EXIT_FAILURE;
  }

  int fd[2];
  if (pipe(fd) != 0) {
    fprintf(stderr, "pipe error\n");
    return EXIT_FAILURE;
  }

  RoutineData r;
  r.sos   = argv[2];
  r.accept = fd[0];
  r.quit   = false;

  for (int i = 3; i < argc; ++i) {  
    r.appids.insert(argv[i]);
  }

  pthread_t tid;
  if (pthread_create(&tid, NULL, routine, &r) != 0) {
    fprintf(stderr, "pthread create error\n");
    return EXIT_FAILURE;
  }
  
  if (!qdbForeach(argv[1], fd[1])) return false;
  r.quit = true;

  void *status;
  pthread_join(tid, &status);
  if (status != (void *) 0) {
    fprintf(stderr, "%s", (char *) status);
    return EXIT_FAILURE;
  }

  return EXIT_SUCCESS;
}

bool qdbForeach(const char *addr, int server)
{
  QuickdbAdapter qdb;
  if (qdb.openc(addr, DEFAULT_CURSOR_TYPE)) {
    fprintf(stderr, "open QDB %s error\n", addr);
    return false;
  }

  uint32_t offset = DEFAULT_OFFSET;
  OneTaskReq req;

  while (true) {
    int ret = qdb.next(req.key, req.keylen, req.value, req.valuelen, &offset);
    if (ret > 0) {
      return true;
    } else if (ret < 0) {
      fprintf(stderr, "fail to get QDB %s data\n", addr);
      return false;
    }

    if (write(server, &req, sizeof(req)) != sizeof(req)) {
      fprintf(stderr, "request to local server error, %d:%s\n", errno, strerror(errno));
      return false;
    }
  }
  qdb.closec();
  return true;
}

int multiOnSock(CURL *e, curl_socket_t s, int what, void *data, void *sockp)
{
  struct epoll_event event;
  event.data.fd = s;

  RoutineData *r = (RoutineData *) data;
  if (what == CURL_POLL_REMOVE) {
    epoll_ctl(r->efd, EPOLL_CTL_DEL, s, NULL);
  } else {
    if (what == CURL_POLL_IN) {
      event.events = EPOLLIN;
    } else if (what == CURL_POLL_OUT) {
      event.events = EPOLLOUT;
    } else if (what == CURL_POLL_INOUT) {
      event.events = EPOLLIN | EPOLLOUT;
    } else {
      event.events = 0;
    }
    if (sockp) {
      epoll_ctl(r->efd, EPOLL_CTL_MOD, s, &event);
    } else {
      epoll_ctl(r->efd, EPOLL_CTL_ADD, s, &event);
      curl_multi_assign(r->multi, s, (void *) 1);
    }
  }
  return 0;
}

int multiOnTimer(CURLM *multi, long timeout, void *data)
{
  return 0;
}

size_t curlReadCb(void *ptr, size_t size, size_t nmemb, void *data)
{
  OneTaskReq *req = (OneTaskReq *) data;
  size_t min = std::min(size * nmemb, req->valuelen - req->pos);
  memcpy(ptr, (char *) req->value + req->pos , min);
  req->pos += min;
  return min;
}

inline void taskFinish(OneTaskReq *task)
{
  free(task->key);
  free(task->value);
  delete task;
}

bool newTask(RoutineData *r)
{
  bool ret = true;
  
  while (true) {
    uint32_t bytes;
    ioctl(r->accept, FIONREAD, &bytes);
    if (bytes == 0) break;
    
    assert(bytes >= sizeof(OneTaskReq));

    OneTaskReq *req = new OneTaskReq;
    read(r->accept, req, sizeof(OneTaskReq));
    req->pos = 0;

    std::string url;
    
    char *dot = (char *) memchr(req->key, '.', req->keylen);
    size_t appidlen = (char *) req->key + req->keylen - (dot+1);
    std::set<std::string>::iterator pos;
    if (dot && (pos = r->appids.find(std::string(dot+1, appidlen))) != r->appids.end()) {
      url = r->sos + "/" + *pos + "/";
      url.append((char *) req->key, req->keylen);
    } else {
      taskFinish(req);
      continue;
    }

    printf("%.*s\n", (int) req->keylen, (char *) req->key);

    CURL *curl = curl_easy_init();
    curl_easy_setopt(curl, CURLOPT_URL, url.c_str());
    curl_easy_setopt(curl, CURLOPT_PUT, 1L);
    curl_easy_setopt(curl, CURLOPT_INFILESIZE_LARGE, (curl_off_t) req->valuelen);
    curl_easy_setopt(curl, CURLOPT_READFUNCTION, curlReadCb);
    curl_easy_setopt(curl, CURLOPT_READDATA, req);
    curl_easy_setopt(curl, CURLOPT_PRIVATE, req);
    
    if (curl_multi_add_handle(r->multi, curl) != CURLM_OK) {
      taskFinish(req);
      curl_easy_cleanup(curl);
      ret = false;
      break;
    }
    int running;
    curl_multi_socket_action(r->multi, CURL_SOCKET_TIMEOUT, 0, &running);
  }

  return ret;
}

bool checkMultiInfo(RoutineData *r)
{
  CURLMsg *msg;
  int left;
  while ((msg = curl_multi_info_read(r->multi, &left))) {
    if (msg->msg != CURLMSG_DONE) continue;

    CURL *easy = msg->easy_handle;
    long code;
    curl_easy_getinfo(easy, CURLINFO_RESPONSE_CODE, &code);
    if (code != 200) {
      char *url;
      curl_easy_getinfo(easy, CURLINFO_EFFECTIVE_URL, &url);
      fprintf(stderr, "%ld %s\n", code, url);
    }
    curl_multi_remove_handle(r->multi, easy);
    OneTaskReq *req;
    curl_easy_getinfo(easy, CURLINFO_PRIVATE, &req);
    taskFinish(req);
    curl_easy_cleanup(easy);
  }
  return true;
}
  
bool eventTask(RoutineData *r, int fd, int events)
{
  int action = (events & EPOLLIN ? CURL_CSELECT_IN : 0) |
    (events & EPOLLOUT ? CURL_CSELECT_OUT : 0);
  int running;
  CURLMcode rc = curl_multi_socket_action(r->multi, fd, action, &running);
  if (rc != CURLM_OK && rc != CURLM_CALL_MULTI_PERFORM) {
    fprintf(stderr, "curl_multi_socket_action %d %d %s\n",
            fd, events, curl_multi_strerror(rc));
    // ignore error
    // return false; 
  }
  return checkMultiInfo(r);
}

void *routine(void *data)
{
  RoutineData *r = (RoutineData *) data;

  r->multi = curl_multi_init();
  curl_multi_setopt(r->multi, CURLMOPT_SOCKETFUNCTION, multiOnSock);
  curl_multi_setopt(r->multi, CURLMOPT_SOCKETDATA, r);
  curl_multi_setopt(r->multi, CURLMOPT_TIMERFUNCTION, multiOnTimer);
  curl_multi_setopt(r->multi, CURLMOPT_TIMERDATA, r);

  const char *err = 0;

  r->efd = epoll_create(1024);
  if (r->efd == -1) return (void *) "epoll_create error";

  long on = 1;
  ioctl(r->accept, FIONBIO, &on);

  struct epoll_event event;
  event.events = EPOLLIN;
  event.data.fd = r->accept;
  epoll_ctl(r->efd, EPOLL_CTL_ADD, r->accept, &event);

  struct epoll_event *events = new epoll_event[1024];
  while (!r->quit) {
    int n = epoll_wait(r->efd, events, 1024, 1000);
    if (n < 0 && errno != EINTR) {
      err = "epoll_wait error";
      break;
    }

    for (int i = 0; i < n; ++i) {
      if (events[i].data.fd == r->accept) {
        if (!newTask(r)) {
          err = "accept new task error";
          break;
        }
      } else {
        if (!eventTask(r, events[i].data.fd, events[i].events)) {
          err = "execute task error";
          break;
        }
      }
    }
    checkMultiInfo(r);
  }

  close(r->accept);   // deny new request
  close(r->efd);
  curl_multi_cleanup(r->multi);
  delete[] events;
  return (void *) err;  
}
