/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_LOCK_H_
#define _RVP_LOCK_H_

#include <pthread.h>

#include "interpose.h"

int __rvpredict_pthread_cond_timedwait(pthread_cond_t *restrict,
    pthread_mutex_t *restrict, const struct timespec *restrict);
int __rvpredict_pthread_cond_signal(pthread_cond_t *);
int __rvpredict_pthread_cond_wait(pthread_cond_t *restrict,
    pthread_mutex_t *restrict);
int __rvpredict_pthread_cond_init(pthread_cond_t *restrict,
    const pthread_condattr_t *restrict);

int __rvpredict_pthread_mutex_init(pthread_mutex_t *,
    const pthread_mutexattr_t *);
int __rvpredict_pthread_mutex_lock(pthread_mutex_t *);
int __rvpredict_pthread_mutex_trylock(pthread_mutex_t *);
int __rvpredict_pthread_mutex_unlock(pthread_mutex_t *);

REAL_DECL(int, pthread_cond_timedwait, pthread_cond_t *restrict,
    pthread_mutex_t *restrict, const struct timespec *restrict);
REAL_DECL(int, pthread_cond_signal, pthread_cond_t *);
REAL_DECL(int, pthread_cond_wait, pthread_cond_t *restrict,
    pthread_mutex_t *restrict);
REAL_DECL(int, pthread_cond_init, pthread_cond_t *restrict,
    const pthread_condattr_t *restrict);

REAL_DECL(int, pthread_mutex_lock, pthread_mutex_t *);
REAL_DECL(int, pthread_mutex_trylock, pthread_mutex_t *);
REAL_DECL(int, pthread_mutex_unlock, pthread_mutex_t *);
REAL_DECL(int, pthread_mutex_init, pthread_mutex_t *restrict,
   const pthread_mutexattr_t *restrict);

#endif /* _RVP_LOCK_H_ */
