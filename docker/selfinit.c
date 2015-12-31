#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <signal.h>
#include <unistd.h>

void sighand(int signo);
static void spawn(const char *cmd);

int main(int argc, char *argv[])
{
  int nstart = 0, nstop = 0;
  int i;
  for (i = 1; i < argc; ++i) {
    if (strcmp(argv[i], "--start") == 0) nstart++;
    if (strcmp(argv[i], "--stop") == 0) nstop++;
  }

  char **starts = malloc(sizeof(char *) * nstart);
  char **stops  = malloc(sizeof(char *) * nstop);

  nstart = nstop = 0;
  for (i = 1; i < argc; i+=2) {
    if (strcmp(argv[i], "--start") == 0) {
      if (i+1 < argc) {
        starts[nstart++] = argv[i+1];
      }
    } else if (strcmp(argv[i], "--stop") == 0) {
      if (i+1 < argc) {
        stops[nstop++] = argv[i+1];
      }
    }
  }

  for (i = 0; i < nstart; i++) {
    spawn(starts[i]);
  }

  signal(SIGTERM, sighand);
  signal(SIGCHLD, SIG_IGN);

  pause();

  for (i = 0; i < nstop; ++i) {
    spawn(stops[i]);
  }

  return 0;
}

void spawn(const char *cmd)
{
  pid_t pid = fork();
  if (pid == 0) {
    printf("run %s\n", cmd);
    execlp("/bin/sh", "/bin/sh", "-c", cmd, NULL);
    exit(0);
  }
}

void sighand(int signo) 
{
  /* do nothing */
}
