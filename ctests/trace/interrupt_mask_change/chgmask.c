#include <err.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "nbcompat.h"

void
handler(int signo __unused)
{
	const char msg[] = "abc 123\n";
	(void)write(STDOUT_FILENO, msg, strlen(msg));
}

int
main(int argc __unused, char **argv __unused)
{
	int rc;
	sigset_t empty, full, oset;
	struct sigaction osa, sa;

	if (sigfillset(&full) == -1)
		err(EXIT_FAILURE, "%s: sigfillset", __func__);

	if (sigemptyset(&empty) == -1)
		err(EXIT_FAILURE, "%s: sigemptyset", __func__);

	if ((rc = pthread_sigmask(SIG_SETMASK, &full, &oset)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}

	memset(&sa, '\0', sizeof(sa));
	(void)sigemptyset(&sa.sa_mask);
	sa.sa_handler = handler;

	if (sigaction(SIGUSR1, &sa, &osa) == -1)
		err(EXIT_FAILURE, "%s: sigaction", __func__);

	if ((rc = pthread_sigmask(SIG_SETMASK, &empty, NULL)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}

	if ((rc = pthread_sigmask(SIG_SETMASK, &full, NULL)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}

	return EXIT_SUCCESS; 
}
