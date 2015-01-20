#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>

int main()
{
  size_t N = (size_t) 1024 * 1024 * 1000;

  void *p = mmap(NULL, N, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE | MAP_32BIT,
                 -1, 0);
  if (p != MAP_FAILED) {
    memset(p, 0x00, N);
    munmap(p, N);
    printf("mmap BIG region OK\n");
  } else {
    printf("mmap BIG region FALSE\n");
  }

  return 0;
}
