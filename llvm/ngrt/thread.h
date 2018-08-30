/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_THREAD_H_
#define _RVP_THREAD_H_

#include <err.h>
#include <pthread.h>
#include <stdbool.h>
#include <stdlib.h>	/* for EXIT_FAILURE */

#include "interpose.h"
#include "ring.h"
#include "specific.h"

typedef struct _rvp_maskchg {
	uint32_t	mc_idepth;	// the interrupt depth for the sequence
					// that was interrupted while it tried
					// to establish
	uint64_t	mc_nmask;	// this new mask,
	rvp_buf_t	mc_buf;		// and the trace event that would
					// establish it
} rvp_maskchg_t;

typedef struct _rvp_thread rvp_thread_t;

struct _rvp_thread {
	volatile bool		t_garbage;
	pthread_t		t_pthread;
	uint32_t		t_id;
	rvp_thread_t * volatile	t_next;
	void			*t_arg;
	void			*(*t_routine)(void *);
	rvp_ring_t		t_ring;
	/* t_intrmask is initialized by __rvpredict_pthread_create() with
	 * the signal mask that should take effect after the thread has
	 * entered __rvpredict_thread_wrapper() but before the thread's
	 * start_routine---third argument to pthread_create(3)---is called.
	 * In the mean time, the runtime blocks all signals to the thread.
	 *
	 * After a thread enters its start_routine, t_intrmask tracks
	 * the signal mask that's in effect.
	 */
	uint64_t _Atomic	t_intrmask;
	uint32_t _Atomic	t_idepth;
	const rvp_maskchg_t * volatile _Atomic	t_maskchg;
	rvp_ring_t * _Atomic	t_intr_ring;
	rvp_ring_stats_t	t_stats;
	int			t_nthrspecs;
	rvp_thrspec_t		*t_thrspec;
};

int __rvpredict_pthread_create(pthread_t *, const pthread_attr_t *,
    void *(*)(void *), void *);
void __rvpredict_pthread_exit(void *);
int __rvpredict_pthread_join(pthread_t, void **);

bool rvp_thread_flush_to_fd(rvp_thread_t *, int, bool);
rvp_thread_t *rvp_pthread_to_thread(pthread_t);
void rvp_info_dump_request(void);

REAL_DECL(int, pthread_join, pthread_t, void **);
REAL_DECL(int, pthread_create, pthread_t *, const pthread_attr_t *,
    void *(*)(void *), void *);
REAL_DECL(void, pthread_exit, void *);

extern pthread_key_t rvp_thread_key;

static inline rvp_thread_t *
rvp_thread_for_curthr(void)
{
	rvp_thread_t *t;

	if ((t = pthread_getspecific(rvp_thread_key)) == NULL) {
#if 1
		abort();
#else
		warnx("%s: pthread_getspecific -> NULL", __func__);
		return rvp_pthread_to_thread(pthread_self());
#endif
	}

	return t;
}

static inline rvp_ring_t *
rvp_ring_for_curthr(void)
{
	rvp_thread_t *t = rvp_thread_for_curthr();

	return t->t_intr_ring;
}

#endif /* _RVP_THREAD_H_ */
