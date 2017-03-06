/* Copyright (c) 2016 Runtime Verification, Inc.  All rights reserved. */

#include <features.h>
#include <assert.h>
#include <err.h>
#include <inttypes.h>	/* for PRIu64 */
#include <limits.h>
#include <stdbool.h>
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

#include "lpcq.h"

typedef struct _item {
	void *next;
	int idx;
} item_t;

#ifndef __arraycount
#define __arraycount(__a)	(sizeof(__a) / sizeof((__a)[0]))
#endif

pthread_mutex_t *mutexp = NULL;
item_t *items;
lpcq_t *sigq = NULL;

int nitems = 5;

bool use_mask = false, use_signal = false;
bool timing = false;

static void
signals_changemask(int how, int signum, sigset_t *oset)
{
	int rc;
	sigset_t set;

	if (sigemptyset(&set) == -1 || sigaddset(&set, signum) == -1)
		err(EXIT_FAILURE, "%s: sig{add,empty}set", __func__);

	if ((rc = pthread_sigmask(how, &set, oset)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}
}

static void
signals_unmask(int signum, sigset_t *oset)
{
	signals_changemask(SIG_UNBLOCK, signum, oset);
}

static void
signals_mask(int signum, sigset_t *oset)
{
	signals_changemask(SIG_BLOCK, signum, oset);
}

static void
signals_restore(const sigset_t *oset)
{
	int rc;

	if ((rc = pthread_sigmask(SIG_SETMASK, oset, NULL)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}
}

static inline void
acquire_queue(sigset_t *oset)
{
	int rc;

	if (use_mask)
		signals_mask(SIGALRM, oset);
		
	if (mutexp == NULL)
		return;

	rc = pthread_mutex_lock(mutexp);
	assert(rc == 0);
}

static inline void
release_queue(const sigset_t *oset)
{
	int rc;

	if (use_mask)
		signals_restore(oset);

	if (mutexp == NULL)
		return;

	rc = pthread_mutex_unlock(mutexp);
	assert(rc == 0);
}

static void *
consume(void *arg)
{
	int nread;
	lpcq_t *q = arg;
	uint64_t elapsed_ns;
	struct timespec resolution, start, stop;
	const uint32_t ns_per_s = 1000 * 1000 * 1000;
	uint64_t busy_loops = 0;
	const struct timespec half_second = {
		  .tv_sec = 0
		, .tv_nsec = 500 * 1000 * 1000
	};

	signals_unmask(SIGALRM, NULL);

	nanosleep(&half_second, NULL);

	if (clock_getres(CLOCK_MONOTONIC, &resolution) != 0)
		err(EXIT_FAILURE, "%s: clock_getres", __func__);

	if (clock_gettime(CLOCK_MONOTONIC, &start) != 0)
		err(EXIT_FAILURE, "%s: clock_gettime", __func__);

	for (nread = 0; nread < nitems; nread++) {
		unsigned int loops;
		item_t *item;

		for (loops = 0; loops < 10000; loops++) {
			sigset_t omask;
			acquire_queue(&omask);
			item = lpcq_get(q);
			release_queue(&omask);
			if (item != NULL) 
				break;
			sched_yield();
		}
		busy_loops += loops;
		printf("read item %d\n", item->idx);
	}

	if (clock_gettime(CLOCK_MONOTONIC, &stop) != 0)
		err(EXIT_FAILURE, "%s: clock_gettime", __func__);

	elapsed_ns = (stop.tv_sec - start.tv_sec) * ns_per_s +
	    stop.tv_nsec - start.tv_nsec;

	if (timing) {
		fprintf(stderr,
		    "%" PRIu64 " ns / %d items, %" PRIu64 " ns resolution\n",
		    elapsed_ns, nitems,
		    resolution.tv_sec * ns_per_s + resolution.tv_nsec);
		fprintf(stderr, "%" PRIu64 " busy loops\n", busy_loops);
	}

	return NULL;
}

static void
handler(int signum)
{
	lpcq_t *q = sigq;
	static int i = 0;

	if (i < nitems)
		lpcq_put(q, &items[i++]);
}

static void
establish(lpcq_t *q)
{
	struct sigaction sa;

	sigq = q;
	memset(&sa, 0, sizeof(sa));
	sa.sa_handler = handler;
	if (sigemptyset(&sa.sa_mask) == -1)
		err(EXIT_FAILURE, "%s: sigemptyset", __func__);
	if (sigaction(SIGALRM, &sa, NULL) == -1)
		err(EXIT_FAILURE, "%s: sigaction", __func__);
}

static void *
produce(void *arg)
{
	lpcq_t *q = arg;
	int i;

	for (i = 0; i < nitems; i++) {
		sigset_t omask;
		acquire_queue(&omask);
		lpcq_put(q, &items[i]);
		release_queue(&omask);
	}

	return NULL;
}

static void
usage(const char *progname)
{
	fprintf(stderr, "Usage: %s [-l]\n", progname);
	exit(EXIT_FAILURE);
}

int
main(int argc, char **argv)
{
	int i, opt;
	lpcq_t q;
	pthread_mutex_t mutex;
	pthread_t producer, consumer;

	while ((opt = getopt(argc, argv, "slmn:t")) != -1) {
		unsigned long v;
		char *end;

		switch (opt) {
		case 'l':
			mutexp = &mutex;
			break;
		case 'm':
			use_mask = true;
			break;
		case 'n':
			if (*optarg == '-' ||
			    (v = strtoul(optarg, &end, 10)) == ULONG_MAX ||
			    *end != '\0' || v > INT_MAX)
				errx(EXIT_FAILURE, "%s: malformed or out-of-range -n argument", __func__);
			nitems = (int)v;
			break;
		case 's':
			use_signal = true;
			break;
		case 't':
			timing = true;
			break;
		default:
			usage(argv[0]);
		}
	}

	if ((items = calloc(nitems, sizeof(items[0]))) == NULL)
		err(EXIT_FAILURE, "%s: calloc", __func__);

	if (pthread_mutex_init(&mutex, NULL) != 0)
		err(EXIT_FAILURE, "%s: pthread_mutex_init", __func__);

	lpcq_init(&q, offsetof(item_t, next));

	for (i = 0; i < nitems; i++)
		items[i].idx = i;

	sigset_t oset;
	signals_mask(SIGALRM, &oset);

	pthread_create(&consumer, NULL, &consume, &q);

	if (use_signal) {
		struct itimerval it;

		establish(&q);
		if (getitimer(ITIMER_REAL, &it) == -1)
			err(EXIT_FAILURE, "%s: getitimer", __func__);
		it.it_interval = (struct timeval){.tv_sec = 0, .tv_usec = 100};
		it.it_value.tv_usec++;
		if (setitimer(ITIMER_REAL, &it, NULL) == -1)
			err(EXIT_FAILURE, "%s: setitimer", __func__);
	} else {
		pthread_create(&producer, NULL, &produce, &q);
		pthread_join(producer, NULL);
	}
	pthread_join(consumer, NULL);
	signals_restore(&oset);
	(void)pthread_mutex_destroy(&mutex);
}
