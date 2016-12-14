/* Copyright (c) 2016 Runtime Verification, Inc.  All rights reserved. */

#include <features.h>
#include <assert.h>
#include <err.h>
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <unistd.h>
#include "spcq.h"

typedef struct _item {
	int idx;
} item_t;

#ifndef __arraycount
#define __arraycount(__a)	(sizeof(__a) / sizeof((__a)[0]))
#endif

pthread_mutex_t *mutexp = NULL;
item_t items[500];

const int nitems = __arraycount(items);

static inline void
acquire_queue(void)
{
	int rc;

	if (mutexp == NULL)
		return;

	rc = pthread_mutex_lock(mutexp);
	assert(rc == 0);
}

static inline void
release_queue(void)
{
	int rc;

	if (mutexp == NULL)
		return;

	rc = pthread_mutex_unlock(mutexp);
	assert(rc == 0);
}

void *
consume(void *arg)
{
	int nread;
	spcq_t *q = arg;

	for (nread = 0; nread < nitems; nread++) {
		int loops;
		item_t *item;

		for (loops = 0; loops < 10000; loops++) {
			acquire_queue();
			item = spcq_get(q);
			release_queue();
			if (item != NULL) 
				break;
			sched_yield();
		}
		printf("read item %d\n", item->idx);
	}

	return NULL;
}

void *
produce(void *arg)
{
	spcq_t *q = arg;
	int i;

	for (i = 0; i < nitems; i++) {
		acquire_queue();
		while (!spcq_put(q, &items[i])) {
			release_queue();
			sched_yield();
			acquire_queue();
		}
		release_queue();
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
	spcq_t *q;
	pthread_mutex_t mutex;

	while ((opt = getopt(argc, argv, "l")) != -1) {
		switch (opt) {
		case 'l':
			fprintf(stderr, "locking enabled\n");
			mutexp = &mutex;
			break;
		default:
			usage(argv[0]);
		}
	}

	if (pthread_mutex_init(&mutex, NULL) != 0)
		err(EXIT_FAILURE, "%s: pthread_mutex_init", __func__);

	if ((q = spcq_alloc(100)) == NULL)
		err(EXIT_FAILURE, "%s: spcq_alloc", __func__);
	pthread_t producer, consumer;
	for (i = 0; i < nitems; i++)
		items[i].idx = i;
	pthread_create(&consumer, NULL, &consume, q);
	pthread_create(&producer, NULL, &produce, q);
	pthread_join(producer, NULL);
	pthread_join(consumer, NULL);
	(void)pthread_mutex_destroy(&mutex);
}
