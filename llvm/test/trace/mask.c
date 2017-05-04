#include <err.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>

int
main(int argc, char **argv)
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
