#include <openssl/pem.h>
#include <openssl/ssl.h>
#include <openssl/rsa.h>
#include <openssl/bio.h>
#include <openssl/evp.h>
#include <openssl/err.h>
#include <openssl/rand.h>
#include <cstdio>
#include <cstring>
#include <cmath>
#include <iostream>
#include <string>
#include <vector>

bool readfile(const std::string &f, std::string *c)
{
  FILE *fp = fopen(f.c_str(), "r");
  if (!fp) return false;

  size_t nbuf = 128;
  char buf[128];

  size_t n;
  while ((n = fread(buf, 1, nbuf, fp))) {
    c->append(buf, n);
  }

  fclose(fp);
  return true;
}

bool readfile(const std::string &f, std::vector<std::string> *l)
{
  FILE *fp = fopen(f.c_str(), "r");
  if (!fp) return false;

  size_t nbuf = 1024;
  char buf[1024];

  while (fgets(buf, nbuf, fp)) {
    buf[strlen(buf)-1] = '\0';
    l->push_back(std::string(buf));
  }

  fclose(fp);
  return true;
}

bool public_encrypt(const std::string &pubkey, const std::string &data,
                    std::vector<std::string> *lines)
{
  int padding = RSA_PKCS1_OAEP_PADDING;
  
  BIO *keybio = BIO_new_mem_buf((void *) pubkey.c_str(), -1);
  if (!keybio) return false;

  RSA *rsa = NULL;
  rsa = PEM_read_bio_RSA_PUBKEY(keybio, NULL, NULL, NULL);
  if (!rsa) {
    BIO_free(keybio);
    return false;
  }

  std::cout << RSA_size(rsa) << std::endl;
  size_t maxflen = RSA_size(rsa) - 41 - 1;
  size_t n = 0;

  while (n < data.size()) {
    int flen = (data.size() - n > maxflen) ? maxflen : data.size() - n;

    char buf[RSA_size(rsa)];
    int tlen = RSA_public_encrypt(flen, (unsigned char *) (data.data() + n),
                                  (unsigned char *) buf, rsa, padding);
    if (tlen == -1) {
      BIO_free(keybio);
      RSA_free(rsa);
      ERR_print_errors_fp(stdout);
      return false;
    }
    lines->push_back(std::string(buf, tlen));
    n += flen;
  }

  BIO_free(keybio);
  RSA_free(rsa);
  return true;
}

bool private_decrypt(const std::string &prikey, const std::vector<std::string> &lines,
                     std::string *data)
{
  int padding = RSA_PKCS1_OAEP_PADDING;
  
  BIO *keybio = BIO_new_mem_buf((void *) prikey.c_str(), -1);
  if (!keybio) return false;
    
  RSA *rsa;
  rsa = PEM_read_bio_RSAPrivateKey(keybio, NULL, NULL, NULL);
  if (!rsa) {
    BIO_free(keybio);
    return false;
  }

  char buf[RSA_size(rsa)];

  for (size_t i = 0; i < lines.size(); ++i) {
    int len = RSA_private_decrypt(lines[i].size(), (unsigned char *) (lines[i].data()),
                                  (unsigned char *) buf, rsa, padding);
    if (len == -1) {
      BIO_free(keybio);
      RSA_free(rsa);
      std::cout << "HERE\n";
      ERR_print_errors_fp(stdout);
      
      return false;
    }
    data->append(buf, len);
  }
  
  BIO_free(keybio);
  RSA_free(rsa);
  return true;
}

std::string encode_base64(const std::string &data, bool nl = false)
{
  BIO *bio, *b64;

  size_t len = 4 * ceil((double) data.size()/3) + 1;
  if (nl) len += (len / 65) + 1;

  std::string str(len, '\0');
  FILE* stream = fmemopen((void *) str.data(), str.size(), "w");

  b64 = BIO_new(BIO_f_base64());
  bio = BIO_new_fp(stream, BIO_NOCLOSE);
  bio = BIO_push(b64, bio);
  if (!nl) BIO_set_flags(bio, BIO_FLAGS_BASE64_NO_NL);
  BIO_write(bio, data.data(), data.size());
  BIO_flush(bio);

  BIO_free_all(bio);
  fclose(stream);
  
  str.resize(strlen(str.c_str()));
  return str;
}

std::string decode_base64(const std::string &str, bool nl = false)
{
  BIO *bio, *b64;

  std::string data(str.size(), '\0');
  FILE* stream = fmemopen((void *) str.c_str(), str.size(), "r");

  b64 = BIO_new(BIO_f_base64());
  bio = BIO_new_fp(stream, BIO_NOCLOSE);
  bio = BIO_push(b64, bio);
  //Do not use newlines to flush buffer  
  if (!nl) BIO_set_flags(bio, BIO_FLAGS_BASE64_NO_NL);
  int len = BIO_read(bio, (void*) data.data(), data.size());
  data.resize(len);
  
  BIO_free_all(bio);
  fclose(stream);

  return data;
}
bool test()
{
  FILE *fp = fopen("/tmp/rsa/priv.pem", "r");
  if (!fp) {
    std::cout << "open /tmp/rsa/priv.pem failed\n";
    return false;
  }
  
  RSA *rsa = NULL;
  rsa = PEM_read_RSAPrivateKey(fp, NULL, NULL, NULL);
  if (rsa) {
    std::cout << "PEM_read_RSAPrivateKey ok\n";
  } else {
    std::cout << "PEM_read_RSAPrivateKey failed\n";
    fclose(fp);
    return false;
  }

  fclose(fp);
  
  std::string str, data;
  if (!readfile("/tmp/rsa/encrypted.txt", &data)) {
    std::cout << "readfile encrypted error\n";
    RSA_free(rsa);
    return false;
  }

  size_t pos = 0;
  int nchunk = RSA_size(rsa);

  char *buffer = new char[nchunk * 2];
  char *from = buffer;
  char *to = buffer + nchunk;

  int flen, tlen;

  for ( ;; ) {
    size_t nlpos = data.find('\n', pos);
    if (nlpos == pos) {
      continue;
    } else if (nlpos == std::string::npos) {
      nlpos = data.size();
    }

    fp = fmemopen((void *) (data.c_str() + pos), nlpos - pos, "r");

    BIO *b64 = BIO_new(BIO_f_base64());
    BIO *bio = BIO_new_fp(fp, BIO_NOCLOSE);
    bio = BIO_push(b64, bio);
    BIO_set_flags(bio, BIO_FLAGS_BASE64_NO_NL);

    flen = BIO_read(bio, (void *)from, nchunk);
    
    if (flen > 0) {
      tlen = RSA_private_decrypt(flen, (unsigned char *) from,
                                 (unsigned char *) to, rsa, RSA_PKCS1_OAEP_PADDING);
      if (tlen == -1) {
        std::cout << "decrypt error";
        return false;
      }

      str.append(to, tlen);
    } else {
      std::cout << "decode_base64 error " << flen << "\n";
      return false;
    }

    BIO_free_all(bio);
    fclose(fp);

    if (nlpos + 1 == data.size() || nlpos == data.size()) break;
    else pos = nlpos + 1;
  }

  std::cout << encode_base64(str, true) << "\n";
  
  delete[] buffer;
  RSA_free(rsa);
  return true;
}  
  
int main()
{
  ERR_load_crypto_strings();
  ERR_load_BIO_strings();
  OpenSSL_add_all_algorithms();

  // RAND_load_file("/dev/urandom", 256);

  test();
  
  std::string prikey;
  if (!readfile("/tmp/rsa/priv.pem", &prikey)) return EXIT_FAILURE;

  std::string pubkey;
  if (!readfile("/tmp/rsa/pub.pem", &pubkey)) return EXIT_FAILURE;

  std::string data;
  if (!readfile("/tmp/rsa/plain.txt", &data)) return EXIT_FAILURE;

  if (decode_base64(encode_base64(prikey)) != prikey) {
    std::cout << "baes64 encode/decode error\n";
    return EXIT_FAILURE;
  }

  if (decode_base64(encode_base64(prikey, true), true) != prikey) {
    std::cout << "baes64 encode/decode with line error\n";
    return EXIT_FAILURE;
  }


  std::string o;
  std::vector<std::string> lines;
  // if (!public_encrypt(pubkey, data, &lines)) return EXIT_FAILURE;
  if (!readfile("/tmp/rsa/encrypted.txt", &lines)) return EXIT_FAILURE;
  for (size_t i = 0; i < lines.size(); ++i) {
    lines[i] = decode_base64(lines[i]);
  }
  
  for (int i = 0; i < 1; ++i) {
    o.clear();
    if (!private_decrypt(prikey, lines, &o)) return EXIT_FAILURE;
  }

  // std::cout << data << std::endl;

  std::string pem = encode_base64(o, true);
  std::cout << pem.size() << "\n";
  std::cout << pem << std::endl;

  pem = encode_base64(o);
  std::cout << pem.size() << "\n";
  std::cout << pem << std::endl;

  pubkey.clear();
  if (!readfile("/tmp/jobworker.pub.pem", &pubkey)) return EXIT_FAILURE;

  data.clear();
  if (!readfile("/tmp/jobworker.pem", &data)) return EXIT_FAILURE;

  lines.clear();
  if (!public_encrypt(pubkey, decode_base64(data, true), &lines)) return EXIT_FAILURE;

  for (size_t i = 0; i < lines.size(); ++i) {
    std::cout << encode_base64(lines[i]) << "\n";
  }
  std::cout << "HERE";

  return EXIT_SUCCESS;
}
