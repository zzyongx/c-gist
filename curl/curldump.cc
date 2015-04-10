#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <cassert>
#include <vector>
#include <string>
#include <map>
#include <algorithm>
#include <curl/curl.h>

// g++ -o curldump curldump.cc -Wall -lcurl

typedef std::vector<std::string> StringList;
typedef std::map<std::string, std::string> StringMap;
static const size_t NP = 100;

bool curlmGet(const std::string &qdb, StringList *keys, StringMap *items);
bool curlmPut(const std::string &sos, StringMap *items);

int main(int argc, char *argv[])
{
  if (argc != 3) {
    fprintf(stderr, "%s qdb sos\n", argv[0]);
    return EXIT_FAILURE;
  }

  std::string qdb(argv[1]);
  std::string sos(argv[2]);

  char buffer[128];
  size_t nbuffer = 128;

  StringList keys;
  StringMap  items;
  
  while (fgets(buffer, nbuffer, stdin)) {
    size_t len = strlen(buffer);
    assert(len >= 2 && buffer[len-1] == '\n');

    if (keys.size() >= NP) {
      if (!curlmGet(qdb, &keys, &items) || !curlmPut(sos, &items)) {
        return EXIT_FAILURE;
      }
    }
    
    keys.push_back(std::string(buffer, len-1));
  }

  if (!curlmGet(qdb, &keys, &items) || !curlmPut(sos, &items)) {
    return EXIT_FAILURE;
  }

  printf("OK\n");
  return EXIT_SUCCESS;
}

size_t curlGetData(void *ptr, size_t size, size_t nmemb, void *data)
{
  std::string *str = (std::string *) data;
  str->append((char *) ptr, size * nmemb);
  return size * nmemb;
}

bool curlmGet(const std::string &qdb, StringList *keys, StringMap *items)
{
  int running;
  CURLMcode mc;
  bool ret = true;
  size_t finished = 0;
  
  CURLM *multi = curl_multi_init();
  StringList pkeys(*keys);
  StringList datas(keys->size());

  for (size_t i = 0; i < keys->size(); ++i) {
    std::string url = qdb + "?key=" + keys->at(i);
    CURL *curl = curl_easy_init();
    curl_easy_setopt(curl, CURLOPT_URL, url.c_str());
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, curlGetData);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &(datas[i]));
    curl_easy_setopt(curl, CURLOPT_PRIVATE, (void *)i);

    curl_multi_add_handle(multi, curl);
  }

  keys->clear();
  curl_multi_perform(multi, &running);

  do {
    int numfds;

    mc = curl_multi_wait(multi, NULL, 0, 5000, &numfds);
    if (mc != CURLM_OK) {
      fprintf(stderr, "curl_multi_wait %s\n", curl_multi_strerror(mc));
      ret = false;
    }
    /* use still_running to judge while loop has problem */
    mc = curl_multi_perform(multi, &running);
    if (mc != CURLM_OK) {
      fprintf(stderr, "curl_multi_perform %s\n", curl_multi_strerror(mc));
      ret = false;
    }

    CURLMsg *msg;
    int left;
    while ((msg = curl_multi_info_read(multi, &left))) {
      if (msg->msg != CURLMSG_DONE) continue;

      CURL *curl = msg->easy_handle;
      long code;
      curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &code);
      size_t idx;
      curl_easy_getinfo(curl, CURLINFO_PRIVATE, &idx);
      
      if (code == 200) {
        // fprintf(stderr, "%s %d\n", pkeys[idx].c_str(), (int) datas[idx].size());
        items->insert(std::make_pair(pkeys[idx], datas[idx]));
      } else {
        char *url;
        curl_easy_getinfo(curl, CURLINFO_EFFECTIVE_URL, &url);
        fprintf(stderr, "GET %ld %s %s\n", code, pkeys[idx].c_str(), url);
        if (code == 0) keys->push_back(pkeys[idx]);
        else ret = false;
      }
      finished++;
      curl_multi_remove_handle(multi, curl);
      curl_easy_cleanup(curl);
    }
  } while (finished < pkeys.size());

  curl_multi_cleanup(multi);
  return ret;
}

struct CurlPutDataCtx {
  std::string key;
  std::string data;
  size_t pos;
  CurlPutDataCtx() {}
  CurlPutDataCtx(const std::string &k, const std::string &d)
    : key(k), data(d), pos(0) {}
};

size_t curlPutData(void *ptr, size_t size, size_t nmemb, void *data)
{
  CurlPutDataCtx *ctx = (CurlPutDataCtx *) data;
  size_t min = std::min(size * nmemb, ctx->data.size() - ctx->pos);
  memcpy(ptr, ctx->data.c_str() + ctx->pos, min);
  ctx->pos += min;
  return min;
}

bool curlmPut(const std::string &sos, StringMap *items)
{
  int running;
  CURLMcode mc;
  bool ret = true;
  size_t finished = 0;
  
  CURLM *multi = curl_multi_init();
  std::vector<CurlPutDataCtx> ctxs;
  ctxs.resize(items->size());

  size_t idx = 0;
  for (StringMap::iterator ite = items->begin(); ite != items->end(); ++ite, ++idx) {
    size_t pos = ite->first.find('.');
    assert(pos != std::string::npos);

    std::string url = sos + "/" + ite->first.substr(pos + 1) + "/" + ite->first;
    ctxs[idx] = CurlPutDataCtx(ite->first, ite->second);
    // fprintf(stderr, "%s %d\n", ite->first.c_str(), (int) ite->second.size());

    CURL *curl = curl_easy_init();
    curl_easy_setopt(curl, CURLOPT_URL, url.c_str());
    curl_easy_setopt(curl, CURLOPT_PUT, 1L);
    curl_easy_setopt(curl, CURLOPT_INFILESIZE_LARGE, (curl_off_t) ctxs[idx].data.size());
    curl_easy_setopt(curl, CURLOPT_READFUNCTION, curlPutData);
    curl_easy_setopt(curl, CURLOPT_READDATA, &ctxs[idx]);
    curl_easy_setopt(curl, CURLOPT_PRIVATE, (void *) idx);

    curl_multi_add_handle(multi, curl);
  }

  curl_multi_perform(multi, &running);

  do {
    int numfds;

    mc = curl_multi_wait(multi, NULL, 0, 5000, &numfds);
    if (mc != CURLM_OK) {
      fprintf(stderr, "curl_multi_wait %s\n", curl_multi_strerror(mc));
      ret = false;
    }
    mc = curl_multi_perform(multi, &running);
    if (mc != CURLM_OK) {
      fprintf(stderr, "curl_multi_perform %s\n", curl_multi_strerror(mc));
      ret = false;
    }

    CURLMsg *msg;
    int left;
    while ((msg = curl_multi_info_read(multi, &left))) {
      if (msg->msg != CURLMSG_DONE) continue;

      CURL *curl = msg->easy_handle;
      long code;
      curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &code);
      curl_easy_getinfo(curl, CURLINFO_PRIVATE, &idx);

      if (code == 200) {
        items->erase(ctxs[idx].key);
      } else {
        char *url;
        curl_easy_getinfo(curl, CURLINFO_EFFECTIVE_URL, &url);
        fprintf(stderr, "PUT %ld %s\n", code, url);
        if (code != 0) ret = false;
      }
      finished++;
      curl_multi_remove_handle(multi, curl);
      curl_easy_cleanup(curl);
    }
  } while (finished < ctxs.size());

  curl_multi_cleanup(multi);
  return ret;
}
