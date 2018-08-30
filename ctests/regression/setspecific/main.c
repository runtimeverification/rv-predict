#include <err.h>
#include <errno.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>

static pthread_key_t msg_key;

#define NUMTHREADS 4

void
specific_destroy(void *sp)
{
}

void *
thread_func(void *param)
{
	int rc;

	if ((rc = pthread_setspecific(msg_key, param)) != 0) {
		errno = rc;
		err(EXIT_FAILURE, "%s: pthread_setspecific", __func__);
	}
	return NULL;
}

int
main(int argc, const char *argv[])
{
	pthread_t thread[NUMTHREADS];
	int i, rc;

	if ((rc = pthread_key_create(&msg_key, specific_destroy)) != 0) {
		errno = rc;
		err(EXIT_FAILURE, "%s: pthread_key_create", __func__);
	}
	for (i = 0; i < NUMTHREADS; ++i) {
		char * m = malloc(sizeof(char));
		pthread_create(&thread[i], NULL, thread_func, m);
	}

	for (i = 0; i < NUMTHREADS; i++)
		pthread_join(thread[i], NULL);

	return 0;
}
