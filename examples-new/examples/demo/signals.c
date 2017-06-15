#include <features.h>
#include <err.h>
#include <limits.h>
#include <string.h>	/* strerror(3) */
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <signal.h>	/* sigaction(2) */
#include <time.h>
#include <unistd.h>

void
signals_changemask(int how, int signum, sigset_t *oset)
{
	int rc;
	sigset_t set;

	if (sigemptyset(&set) == -1 || sigaddset(&set, signum) == -1)
		err(EXIT_FAILURE, "%s: sig{add,empty}set", __func__);

	if ((rc = pthread_sigmask(how, &set, oset)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}
}

void
signals_unmask(int signum, sigset_t *oset)
{
	signals_changemask(SIG_UNBLOCK, signum, oset);
}

void
signals_mask(int signum, sigset_t *oset)
{
	signals_changemask(SIG_BLOCK, signum, oset);
}

void
signals_restore(const sigset_t *oset)
{
	int rc;

	if ((rc = pthread_sigmask(SIG_SETMASK, oset, NULL)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}
}
