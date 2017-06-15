/* Copyright (c) 2016 Runtime Verification, Inc.  All rights reserved. */

#include <features.h>
#include <assert.h>
#include <err.h>
#include <inttypes.h>	/* for PRIu64 */
#include <limits.h>
#include <stdbool.h>
#include <stdatomic.h>
#include <stddef.h>
#include <stdint.h>	/* for uint64_t */
#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <signal.h>	/* sigaction(2) */
#include <string.h>	/* memset(3) */
#include <time.h>
#include <unistd.h>

#include <sys/time.h>	/* getitimer(2) */

#include "signals.h"

#ifndef __unused
#define	__unused	__attribute__((__unused__))
#endif /* __unused */

bool use_signal = false;

typedef struct _shared {
	volatile int count;
	volatile _Atomic bool alarm_blocked;
	volatile bool interrupted;
} shared_t;

static shared_t shared = {
	  .count = 1
	, .alarm_blocked = ATOMIC_VAR_INIT(false)
	, .interrupted = false
};

static void
alarm_handler(int signum __unused)
{
	if (shared.alarm_blocked)
		return;

	if (shared.count < -1 || 1 < shared.count)
		abort();
}

static void *
consume(void *arg __unused)
{
	while (!shared.interrupted) {
		alarm_handler(SIGALRM);
		sched_yield();
	}
	return NULL;
}

static void
interrupt_handler(int signum __unused)
{
	char msg[] = "interrupted\n";
	(void)write(STDOUT_FILENO, msg, sizeof(msg));
	shared.interrupted = true;
}

static void
establish(int signum, void (*handler)(int))
{
	struct sigaction sa;

	memset(&sa, 0, sizeof(sa));
	sa.sa_handler = handler;
	if (sigemptyset(&sa.sa_mask) == -1)
		err(EXIT_FAILURE, "%s: sigemptyset", __func__);
	if (sigaction(signum, &sa, NULL) == -1)
		err(EXIT_FAILURE, "%s: sigaction", __func__);
}

static void
usage(const char *progname)
{
	fprintf(stderr, "usage: %s [-s|-v|-m]\n", progname);
	exit(EXIT_FAILURE);
}

int
main(int argc, char **argv)
{
	int i, opt;
	sigset_t oset;
	pthread_t consumer;
	bool block_with_variable = false, block_with_mask = false;

	while ((opt = getopt(argc, argv, "msv")) != -1) {
		switch (opt) {
		case 'm':
			block_with_mask = true;
			break;
		case 's':
			use_signal = true;
			break;
		case 'v':
			block_with_variable = true;
			break;
		default:
			usage(argv[0]);
		}
	}

	signals_mask(SIGALRM, &oset);
	signals_mask(SIGINT, NULL);

	establish(SIGINT, interrupt_handler);

	signals_unmask(SIGINT, NULL);

	if (use_signal) {
		struct itimerval it;

		establish(SIGALRM, alarm_handler);
		if (getitimer(ITIMER_REAL, &it) == -1)
			err(EXIT_FAILURE, "%s: getitimer", __func__);
		it.it_interval = (struct timeval){.tv_sec = 0, .tv_usec = 1000 * 1000 / 10};
		it.it_value.tv_usec++;
		if (setitimer(ITIMER_REAL, &it, NULL) == -1)
			err(EXIT_FAILURE, "%s: setitimer", __func__);
		signals_unmask(SIGALRM, NULL);
	} else {
		pthread_create(&consumer, NULL, &consume, NULL);
	}
	for (i = 0; i < 25; i++) {
		sigset_t tset;
		if (block_with_variable)
			shared.alarm_blocked = true;
		else if (block_with_mask)
			signals_mask(SIGALRM, &tset);
		shared.count = -shared.count;
		if (block_with_variable)
			shared.alarm_blocked = false;
		else if (block_with_mask)
			signals_restore(&tset);
		if (use_signal)
			pause();
		else
			sched_yield();
	}
	if (!use_signal) {
		shared.interrupted = true;
		pthread_join(consumer, NULL);
	}
	signals_restore(&oset);
	return EXIT_SUCCESS;
}
