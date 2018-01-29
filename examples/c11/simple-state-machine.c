/* Copyright (c) 2016,2017,2018 Runtime Verification, Inc.
 * All rights reserved.
 */

#include <pthread.h>
#include <sched.h>
#include <stdbool.h>
#include "nbcompat.h"

pthread_mutex_t l = PTHREAD_MUTEX_INITIALIZER;
bool ready = false;
typedef enum { STOP, INIT, START } state_t;
state_t state = STOP;

static void *
init(void *arg __unused)
{
	pthread_mutex_lock(&l);
	ready = true;
	pthread_mutex_unlock(&l);
	state = INIT;
	pthread_mutex_lock(&l);
	ready = true;
	pthread_mutex_unlock(&l);
	return NULL;
}

static void *
start(void *arg __unused)
{
	sched_yield();
	pthread_mutex_lock(&l);
	if (ready && state == INIT) {
		state = START;
	}
	pthread_mutex_unlock(&l);
	return NULL;
}

static void *
stop(void *arg __unused)
{
	pthread_mutex_lock(&l);
	ready = false;
	state = STOP;
	pthread_mutex_unlock(&l);
	return NULL;
}

int
main()
{
	pthread_t t1, t2, t3;
	pthread_create(&t1, NULL, init, NULL);
	pthread_create(&t2, NULL, start, NULL);
	pthread_create(&t3, NULL, stop, NULL);
	pthread_join(t1, NULL);
	pthread_join(t2, NULL);
	pthread_join(t3, NULL);
	return 0;
}
