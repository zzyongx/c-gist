#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>

#define BIGS 1024 * 1024 * 1024 * 500L

#define BIGF1 "/tmp/bigfile.1"
#define BIGF2 "/tmp/bigfile.2"
#define BIGF3 "/tmp/bigfile.3"

/* ftruncate can make big file, without allocate disk space
 * posix_fallocate, allocate disk space
 * compile with -D_FILE_OFFSET_BITS=64 -D_LARGEFILE64_SOURCE
 * ftruncate will be defined to ftruncate64, and so on
 * off_t will be defined to off64_t
 */

int main()
{
  int fd = open(BIGF1, O_CREAT | O_LARGEFILE | O_RDWR, 0644);
  if (fd == -1) {
    fprintf(stderr, "open %s error\n", BIGF1);
    return EXIT_FAILURE;
  }

  if (ftruncate(fd, BIGS) != 0) {
    fprintf(stderr, "truncate %s error, %d:%s\n", BIGF1, errno, strerror(errno));
    return EXIT_FAILURE;
  }

  close(fd);

  fd = open(BIGF2, O_CREAT | O_LARGEFILE | O_RDWR, 0644);
  if (fd == -1) {
    fprintf(stderr, "open %s error\n", BIGF2);
    return EXIT_FAILURE;
  }

  if (lseek(fd, BIGS, SEEK_SET) == (off_t)-1) {
    fprintf(stderr, "lseek %s error, %d:%s\n", BIGF2, errno, strerror(errno));
    return EXIT_FAILURE;
  }
  write(fd, "eof", 3);

  close(fd);

  fd = open(BIGF3, O_CREAT | O_LARGEFILE | O_RDWR, 0644);
  if (fd == -1) {
    fprintf(stderr, "open %s error\n", BIGF3);
    return EXIT_FAILURE;
  }

  if (pwrite(fd, "eof", 3, BIGS) != 3) {
    fprintf(stderr, "pwrite %s error, %d:%s\n", BIGF3, errno, strerror(errno));
    return EXIT_FAILURE;
  }
  
  close(fd);

  return EXIT_SUCCESS;
}
  
