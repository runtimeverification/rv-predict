/* Copyright (c) 2016,2017,2018 Runtime Verification, Inc.
 * All rights reserved.
 */
#include <err.h>
#include <signal.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <sys/time.h>

static void
setalarm(void)
{
	struct itimerval it;

	if (getitimer(ITIMER_REAL, &it) == -1)
		err(EXIT_FAILURE, "%s: getitimer", __func__);
	it.it_interval = (struct timeval){.tv_sec = 0, .tv_usec = 100};
	it.it_value.tv_usec++;
	if (setitimer(ITIMER_REAL, &it, NULL) == -1)
		err(EXIT_FAILURE, "%s: setitimer", __func__);
}

void
establish(void (*handler)(int), bool recursive)
{
	sigset_t blockset;
	struct sigaction sa;

	memset(&sa, 0, sizeof(sa));
	sa.sa_handler = handler;
	if (recursive)
		sa.sa_flags = SA_NODEFER;
	sigemptyset(&sa.sa_mask);

	sigemptyset(&blockset);
	sigaddset(&blockset, SIGALRM);

	pthread_sigmask(SIG_BLOCK, &blockset, NULL);
	sigaction(SIGALRM, &sa, NULL);
	setalarm();
	pthread_sigmask(SIG_UNBLOCK, &blockset, NULL);
}
