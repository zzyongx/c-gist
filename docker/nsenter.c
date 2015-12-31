#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/syscall.h>
#include <sys/wait.h>

#define NBUFFER 256

static char *ftpl[] = {
    "/proc/%s/ns/ipc",
    "/proc/%s/ns/uts",
    "/proc/%s/ns/net",
    "/proc/%s/ns/pid",
    "/proc/%s/ns/mnt",
    0
};

int main(int argc, char *argv[])
{
    if (argc < 3) {
        fprintf(stderr, "%s PID cmd [arg...]\n", argv[0]);
        exit(EXIT_FAILURE);
    }

    int i;
    char fname[NBUFFER];

    for (i = 0; ftpl[i]; ++i) {
        snprintf(fname, NBUFFER-1, ftpl[i], argv[1]);
        int fd = open(fname, O_RDONLY);
        if (fd == -1) {
            if (errno == ENOENT) {
                fprintf(stderr, "%s not exist, ignore\n", fname);
                continue;
            } else {
                fprintf(stderr, "enter error %s\n", strerror(errno));
                exit(EXIT_FAILURE);
            }
        } else {
            syscall(308,fd,0);
            close(fd);
        }
    }

    pid_t pid = fork();
    if (pid == 0) {
        /* Execute a command in namespace */
        execvp(argv[2], &argv[2]);
        fprintf(stderr, "execv %s error %s", argv[2], strerror(errno));
        exit(EXIT_FAILURE);
    }

    int status;
    wait(&status);

    exit(WEXITSTATUS(status));
}
