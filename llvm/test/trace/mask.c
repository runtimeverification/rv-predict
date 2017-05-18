#include <err.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>

#include "nbcompat.h"

int
main(int argc __unused, char **argv __unused)
{
	int rc;
	sigset_t oset, set;
	sigfillset(&set);

	if ((rc = pthread_sigmask(SIG_BLOCK, &set, NULL)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}

	if ((rc = pthread_sigmask(SIG_BLOCK, &set, &oset)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}

	return EXIT_SUCCESS; 
}
