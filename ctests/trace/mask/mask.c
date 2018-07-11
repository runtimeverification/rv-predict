#include <err.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>

#include "nbcompat.h"

int
main(int argc __unused, char **argv __unused)
{
	int rc;
	sigset_t empty, full, oset;
	sigfillset(&full);
	sigemptyset(&empty);

	if ((rc = pthread_sigmask(SIG_BLOCK, &full, NULL)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}

	if ((rc = pthread_sigmask(SIG_BLOCK, &full, &oset)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}

	if ((rc = pthread_sigmask(SIG_SETMASK, NULL, &oset)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}

	if ((rc = pthread_sigmask(SIG_SETMASK, &oset, NULL)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}

	if ((rc = pthread_sigmask(SIG_SETMASK, &empty, &oset)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}

	return EXIT_SUCCESS; 
}
