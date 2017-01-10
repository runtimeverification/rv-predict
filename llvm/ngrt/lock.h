/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_LOCK_H_
#define _RVP_LOCK_H_

#include <pthread.h>

int __rvpredict_pthread_mutex_init(pthread_mutex_t *,
    const pthread_mutexattr_t *);
int __rvpredict_pthread_mutex_lock(pthread_mutex_t *);
int __rvpredict_pthread_mutex_trylock(pthread_mutex_t *);
int __rvpredict_pthread_mutex_unlock(pthread_mutex_t *);

#endif /* _RVP_LOCK_H_ */
