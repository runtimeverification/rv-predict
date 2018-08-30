/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_SPECIFIC_H_
#define _RVP_SPECIFIC_H_

typedef struct _rvp_thrspec rvp_thrspec_t;

/* Thread-specific data for keys that have destructors connected. */
struct _rvp_thrspec {
	pthread_key_t	ts_key;
	const void 	*ts_value;
};

int __rvpredict_pthread_key_create(pthread_key_t *, void (*)(void *));
int __rvpredict_pthread_key_delete(pthread_key_t);
int __rvpredict_pthread_setspecific(pthread_key_t, const void *);

REAL_DECL(int, pthread_key_create, pthread_key_t *, void (*)(void *));
REAL_DECL(int, pthread_key_delete, pthread_key_t);
REAL_DECL(int, pthread_setspecific, pthread_key_t, const void *);

void rvp_thread_destructor(void *);

#endif /* _RVP_SPECIFIC_H_ */
