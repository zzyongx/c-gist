#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>

void *routine(void *data) {
  FILE *fp = fopen("/mnt/test", "a");
  fprintf(fp, "pid %d\n", getpid());
  fclose(fp);
  return NULL;
}

int main(int argc, char *argv[]) {
  pthread_t tid;
  pthread_create(&tid, NULL, routine, NULL);
  sleep(5);  // wait thread run
  return EXIT_SUCCESS;
}
