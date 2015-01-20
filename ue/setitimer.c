#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/wait.h>
#include <sys/time.h>
#include <pthread.h>

/* timer accumulate thread time but not children
 */

void *runcpu(void *data)
{
  int i, j, k;
  for (i = 0; i < 200000; i++) {
    for (j = 0; j < 10000; j++) {
      k = i * j;
    }
  }
  return NULL;
}
  
int main()
{
  struct timeval tv = {4, 0};
  struct itimerval itv = {tv, tv};
  
  setitimer(ITIMER_PROF, &itv, NULL);

#ifdef TEST_FORK
  pid_t pid = fork();
  if (pid == 0) {
    runcpu(NULL);
    exit(0);
  }
  wait(NULL);
#else
  pthread_t ptid;
  pthread_create(&ptid, NULL, runcpu, NULL);
  
  pthread_join(ptid, NULL);
#endif
  
  printf("OK\n");
  return 0;
}

