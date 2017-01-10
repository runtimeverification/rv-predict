/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_THREAD_INTERNAL_H_
#define _RVP_THREAD_INTERNAL_H_

#include <err.h>
#include <pthread.h>	/* for pthread_getspecific_t, etc. */
#include <stdlib.h>	/* for EXIT_FAILURE */

struct _rvp_thread;

extern pthread_key_t rvp_thread_key;

static inline rvp_thread_t *
rvp_thread_for_curthr(void)
{
	rvp_thread_t *t;

	if ((t = pthread_getspecific(rvp_thread_key)) == NULL)
		errx(EXIT_FAILURE, "%s: pthread_getspecific -> NULL", __func__);

	return t;
}

#endif /* _RVP_THREAD_INTERNAL_H_ */

