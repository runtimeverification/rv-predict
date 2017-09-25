#include <err.h>
#include <errno.h>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>

#include "nbcompat.h"

enum {
	  PARENT = 0
	, CHILD
};

static void
do_nothing(int signum __unused)
{
	return;
}

int
main(int argc __unused, char **argv __unused)
{
	ssize_t nwritten;
	pid_t pid;
	int epipefd[2];
	int rdfd, wrfd;
	struct sigaction sa;

	memset(&sa, 0, sizeof(sa));
	if (sigemptyset(&sa.sa_mask) == -1)
		err(EXIT_FAILURE, "%s: sigemptyset", __func__);
	sa.sa_handler = do_nothing;

	if (sigaction(SIGINT, &sa, NULL) == -1)
		err(EXIT_FAILURE, "%s: sigaction", __func__);

	if (sigaction(SIGPIPE, &sa, NULL) == -1)
		err(EXIT_FAILURE, "%s: sigaction", __func__);

	if (pipe(epipefd) == -1)
		err(EXIT_FAILURE, "%s: pipe", __func__);

	rdfd = epipefd[0];
	wrfd = epipefd[1];

	if ((pid = fork()) == -1)
		err(EXIT_FAILURE, "%s: fork", __func__);

	if (pid == 0) {
		// child
		int rc, wbuflen, wbuflen0;
		char *wbuf;
		sigset_t allmask;
		sigset_t waitmask;

		if (sigaction(SIGINT, &sa, NULL) == -1)
			err(EXIT_FAILURE, "%s: sigaction", __func__);

		if (sigaction(SIGPIPE, &sa, NULL) == -1)
			err(EXIT_FAILURE, "%s: sigaction", __func__);

		if (sigfillset(&allmask) == -1)
			err(EXIT_FAILURE, "%s: sigfillset", __func__);

		if (sigemptyset(&waitmask) == -1)
			err(EXIT_FAILURE, "%s: sigemptyset", __func__);

		if (sigaddset(&waitmask, SIGHUP) == -1)
			err(EXIT_FAILURE, "%s: sigaddset", __func__);

		if (sigaddset(&waitmask, SIGPIPE) == -1)
			err(EXIT_FAILURE, "%s: sigaddset", __func__);

		if ((rc = pthread_sigmask(SIG_BLOCK, &allmask, NULL)) != 0) {
			errx(EXIT_FAILURE, "%s: pthread_sigmask: %s",
			    __func__, strerror(rc));
		}
		errno = 0;
		if ((wbuflen0 = fpathconf(wrfd, _PC_PIPE_BUF)) == -1)
			err(EXIT_FAILURE, "%s: fpathconf", __func__);
		wbuflen = wbuflen0 * 2;
		if ((wbuf = malloc(wbuflen)) == NULL)
			err(EXIT_FAILURE, "%s: malloc", __func__);
		fprintf(stderr, "child writing %d bytes\n", wbuflen);
		close(rdfd);
		while ((nwritten = write(wrfd, wbuf, wbuflen)) == wbuflen) {
			fprintf(stderr, "child wrote %zd bytes\n",
			    nwritten);
		}
		if (nwritten == -1)
			err(EXIT_FAILURE, "%s: write", __func__);
		fprintf(stderr, "child exiting normally\n");
	} else {
		sigset_t nomask;
		struct timespec ts = {.tv_sec = 5, .tv_nsec = 0};

		if (sigemptyset(&nomask) == -1)
			err(EXIT_FAILURE, "%s: sigemptyset", __func__);
		if (sigaddset(&nomask, SIGINT) == -1)
			err(EXIT_FAILURE, "%s: sigaddset", __func__);
		// parent
		close(wrfd);
		if (sigtimedwait(&nomask, NULL, &ts) == -1 && errno != EAGAIN)
			err(EXIT_FAILURE, "%s: sigtimedwait", __func__);
		fprintf(stderr, "parent timed out\n");
		close(rdfd);
		if (waitpid(pid, NULL, 0) == -1)
			err(EXIT_FAILURE, "%s: waitpid", __func__);
		fprintf(stderr, "parent exiting normally\n");
	}
	return EXIT_SUCCESS;
}
