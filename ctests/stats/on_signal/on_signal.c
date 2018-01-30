#include <err.h>
#include <errno.h>
#include <pthread.h>
#include <signal.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "nbcompat.h"

#ifdef SIGINFO
#define RVP_INFO_SIGNAL	SIGINFO
#else
#define RVP_INFO_SIGNAL	SIGPWR
#endif

static pthread_cond_t cv = PTHREAD_COND_INITIALIZER;
static pthread_mutex_t mtx = PTHREAD_MUTEX_INITIALIZER;
static sigset_t allmask, mask;
static bool started = false;

void
hup(int signo __unused)
{
	return;
}

void *
await_signal(void *arg)
{
	sigset_t omask;

	pthread_sigmask(SIG_BLOCK, &allmask, &omask);
	pthread_mutex_lock(&mtx);
	started = true;
	pthread_cond_signal(&cv);
	(void)pthread_mutex_unlock(&mtx);
	(void)sigsuspend(&mask);
	if (errno != EINTR)
		err(EXIT_FAILURE, "%s: sigsuspend", __func__);
	(void)pthread_sigmask(SIG_SETMASK, &omask, NULL);
	return arg;
}

int
main(void)
{
	struct sigaction sa;
	pthread_t child;
	int rc;

	if (sigfillset(&allmask) == -1)
		err(EXIT_FAILURE, "%s.%d: sigfillset", __func__, __LINE__);

	if (sigfillset(&mask) == -1)
		err(EXIT_FAILURE, "%s.%d: sigfillset", __func__, __LINE__);

	if (sigdelset(&mask, SIGINT) == -1)
		err(EXIT_FAILURE, "%s: sigdelset", __func__);

	if (sigdelset(&mask, RVP_INFO_SIGNAL) == -1)
		err(EXIT_FAILURE, "%s: sigdelset", __func__);

	memset(&sa, '\0', sizeof(sa));
	if (sigemptyset(&sa.sa_mask) == -1)
		err(EXIT_FAILURE, "%s: sigemptyset", __func__);
	sa.sa_handler = hup;
	if (sigaction(SIGINT, &sa, NULL) == -1)
		err(EXIT_FAILURE, "%s: sigaction", __func__);

	if ((rc = pthread_create(&child, NULL, await_signal, NULL)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_create: %s", __func__,
		    strerror(rc));
	}

	(void)pthread_mutex_lock(&mtx);
	while (!started)
		(void)pthread_cond_wait(&cv, &mtx);
	(void)pthread_mutex_unlock(&mtx);
	(void)pthread_kill(child, RVP_INFO_SIGNAL);

	(void)pthread_join(child, NULL);
	return EXIT_SUCCESS;
}
