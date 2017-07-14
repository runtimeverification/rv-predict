#include <err.h>
#include <signal.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

static void
handler(int signum)
{
	const char msg[] = "got signal\n";
	write(STDOUT_FILENO, msg, strlen(msg));
}

static void
usage(const char *progname)
{
	fprintf(stderr, "usage: %s [block|unblock]\n", progname);
	exit(EXIT_FAILURE);
}

int
main(int argc, char **argv)
{
	struct sigaction sa;
	sigset_t full, omask;
	bool use_mask;

	memset(&sa, 0, sizeof(sa));
	sigemptyset(&sa.sa_mask);
	sa.sa_handler = handler;

	switch (argc) {
	default:
		usage(argv[0]);
		break;
	case 2:
		if (strcmp(argv[1], "block") == 0)
			use_mask = true;
		else if (strcmp(argv[1], "unblock") == 0)
			use_mask = false;
		else
			usage(argv[0]);
		break;
	case 1:
		use_mask = true;
		break;
	}

	if (sigaction(SIGALRM, &sa, NULL) == -1)
		err(EXIT_FAILURE, "%s: sigaction", __func__);

	if (sigfillset(&full) == -1)
		err(EXIT_FAILURE, "%s: sigfillset", __func__);

	if (use_mask && sigprocmask(SIG_SETMASK, &full, &omask) == -1)
		err(EXIT_FAILURE, "%s: sigprocmask", __func__);

	raise(SIGALRM);

	if (sigprocmask(SIG_SETMASK, &omask, NULL) == -1)
		err(EXIT_FAILURE, "%s: sigprocmask", __func__);

	return EXIT_SUCCESS;
}
