#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main(int argc, char *argv[])
{
  if (argc != 2) {
    fprintf(stderr, "usage: %s netmask\nreading ip from stdin, one ip per line\n", argv[0]);
    fprintf(stderr, "  echo '172.134.75.31' | %s 21", argv[0]);
    return EXIT_FAILURE;
  }

  int netmask = atoi(argv[1]);
  if (netmask <= 0 || netmask > 32) {
    fprintf(stderr, "invalid netmask %s, netmask must > 0 and <= 32\n", argv[1]);
    return EXIT_FAILURE;
  }

  unsigned int unet = ~((1 << (32 - netmask)) -1);

  char buffer[17];
  while (fgets(buffer, 17, stdin)) {
    unsigned int intip = 0;
    char *ptr;
    int n, val = 0;
    for (n = 0, ptr = buffer; n < 4 && *ptr; ++ptr) {
      if (*ptr >= '0' && *ptr <= '9') {
        val = val * 10 + *ptr - '0';
      } else if ((*ptr == '.' || *ptr == '\n') && val <= 255) {
        intip = (intip << 8) | val;
        val = 0;
        ++n;
        if (*ptr == '\n') *ptr = '\0';
      } else {
        fprintf(stderr, "invalid ip %s", buffer);
      }
    }

    if (n == 4) {
      unsigned int net = intip & unet;
      printf("%s %d.%d.%d.%d/%d\n", buffer,
             net >> 24, (net >> 16) & 0xFF, (net >> 8) & 0xFF, net & 0xFF, netmask);
    } else {
      fprintf(stderr, "invalid ip %s\n", buffer);
    }
  }
  return EXIT_SUCCESS;
}
