#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <cassert>
#include <string>
#include <algorithm>
#include <curl/curl.h>

// g++ -o curldump curldump.cc -Wall -lcurl

bool curlGet(const std::string &qdb, const std::string &key, std::string *data);
bool curlPut(const std::string &sos, const std::string &key, const std::string &data);

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
  
  while (fgets(buffer, nbuffer, stdin)) {
    size_t len = strlen(buffer);
    assert(buffer[len-1] == '\n');
    
    std::string key(buffer, len-1);
    std::string data;
    if (!curlGet(qdb, key, &data)) {
      fprintf(stderr, "qdb get error %s", buffer);
      return EXIT_FAILURE;
    }
    if (!curlPut(sos, key, data)) {
      fprintf(stderr, "sos put error %s", buffer);
      return EXIT_FAILURE;
    }
  }
  return EXIT_SUCCESS;
}

size_t curlGetData(void *ptr, size_t size, size_t nmemb, void *data)
{
  std::string *str = (std::string *) data;
  str->append((char *) ptr, size * nmemb);
  return size * nmemb;
}

bool curlGet(const std::string &qdb, const std::string &key, std::string *data)
{
  std::string url = qdb + "?key=" + key;
  
  CURL *curl = curl_easy_init();
  curl_easy_setopt(curl, CURLOPT_URL, url.c_str());
  curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, curlGetData);
  curl_easy_setopt(curl, CURLOPT_WRITEDATA, data);

  CURLcode rc = curl_easy_perform(curl);
  if (rc != CURLE_OK) {
    curl_easy_cleanup(curl);
    return false;
  }
  long code;
  curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &code);

  curl_easy_cleanup(curl);
  return code == 200;
}

struct CurlPutDataCtx {
  const std::string *data;
  size_t pos;
};

size_t curlPutData(void *ptr, size_t size, size_t nmemb, void *data)
{
  CurlPutDataCtx *ctx = (CurlPutDataCtx *) data;
  size_t min = std::min(size * nmemb, ctx->data->size() - ctx->pos);
  memcpy(ptr, ctx->data->c_str() + ctx->pos, min);
  ctx->pos += min;
  return min;
}

bool curlPut(const std::string &sos, const std::string &key, const std::string &data)
{
  size_t idx = key.find('.');
  assert(idx != std::string::npos);

  std::string url = sos + "/" + key.substr(idx + 1) + "/" + key;
  CurlPutDataCtx ctx = { &data, 0 };

  CURL *curl = curl_easy_init();
  curl_easy_setopt(curl, CURLOPT_URL, url.c_str());
  curl_easy_setopt(curl, CURLOPT_PUT, 1L);
  curl_easy_setopt(curl, CURLOPT_INFILESIZE_LARGE, (curl_off_t) data.size());
  curl_easy_setopt(curl, CURLOPT_READFUNCTION, curlPutData);
  curl_easy_setopt(curl, CURLOPT_READDATA, &ctx);

  CURLcode rc = curl_easy_perform(curl);
  if (rc != CURLE_OK) {
    curl_easy_cleanup(curl);
    return false;
  }

  long code;
  curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &code);

  curl_easy_cleanup(curl);
  return code == 200;
}
