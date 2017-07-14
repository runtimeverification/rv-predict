#include <err.h>
#include <signal.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>

int
main(int argc, char **argv)
{
	sigset_t ss;
	int first, last, signum;
	bool leading_errors = true;
	sigset_t oldmask;

	pthread_sigmask(SIG_BLOCK, NULL, &oldmask);
	sigfillset(&ss);

	for (signum = 0; ; signum++) {
		switch (sigismember(&ss, signum)) {
		case 1:
			if (leading_errors) {
				first = signum;
				leading_errors = false;
			}
			printf("%d is a member\n", signum);
			break;
		case 0:
			if (leading_errors) {
				first = signum;
				leading_errors = false;
			}
			printf("%d is NOT a member\n", signum);
			break;
		case -1:
			if (leading_errors)
				continue;
			goto out;
		}
	}
out:
	last = signum;
	printf("last (exclusive) signal, #%d\n", last);
	for (signum = first; signum < last; signum++) {
		if (sigismember(&ss, signum) != 1)
			continue;
		if (sigismember(&oldmask, signum) == 1) {
			printf("%d is in the initial mask\n", signum);
		} else {
			printf("%d is NOT in the initial mask\n", signum);
		}
	}
	return 0;
}
