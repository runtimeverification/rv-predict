#include <err.h>
#include <signal.h>
#include <stdio.h>

int
main(int argc, char **argv)
{
	sigset_t ss;
	int signum;

	sigfillset(&ss);

	for (signum = 0; ; signum++) {
		switch (sigismember(&ss, signum)) {
		case 1:
			printf("%d is a member\n", signum);
			break;
		case 0:
			printf("%d is NOT a member\n", signum);
			break;
		case -1:
			warn("%d", signum);
			break;
		}
	}
	return 0;
}
