#include <pthread.h>
#include <stdbool.h>
#include <stdlib.h>

#include "nbcompat.h"

pthread_mutex_t m = PTHREAD_MUTEX_INITIALIZER;
pthread_cond_t cv = PTHREAD_COND_INITIALIZER;
volatile int shared;

volatile bool frozen = true;

static void *
accessor1(void *arg __unused)
{
	(void)pthread_mutex_lock(&m);
	shared = 1;
	(void)pthread_mutex_unlock(&m);
	return NULL;
}

static void *
accessor2(void *arg __unused)
{
	shared = 2;
	return NULL;
}

static void *
waker(void *arg __unused)
{
	pthread_mutex_lock(&m);
	frozen = false;
	pthread_cond_signal(&cv);
	pthread_mutex_unlock(&m);
	return NULL;
}

int
main(void)
{
	pthread_t t1, t2, t3;

	shared = 0;

	pthread_mutex_lock(&m);

	(void)pthread_create(&t1, NULL, accessor1, NULL);
	(void)pthread_create(&t2, NULL, accessor2, NULL);
	(void)pthread_create(&t3, NULL, waker, NULL);

	while (frozen)
		pthread_cond_wait(&cv, &m);

	(void)pthread_join(t2, NULL);
	pthread_mutex_unlock(&m);
	(void)pthread_join(t1, NULL);
	(void)pthread_join(t3, NULL);

	return EXIT_SUCCESS;
}
