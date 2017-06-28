#include <err.h>
#include <signal.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>

int
main(int argc, char **argv)
{
	sigset_t ss;
	int signum;
	bool leading_errors = true; 

	sigfillset(&ss);

	for (signum = 0; ; signum++) {
		switch (sigismember(&ss, signum)) {
		case 1:
			leading_errors = false;
			printf("%d is a member\n", signum);
			break;
		case 0:
			leading_errors = false;
			printf("%d is NOT a member\n", signum);
			break;
		case -1:
			if (leading_errors)
				continue;
			err(EXIT_FAILURE, "signal #%d", signum);
			break;
		}
	}
	return 0;
}
