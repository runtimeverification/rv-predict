#include <err.h>
#include <stdlib.h> /* for EXIT_FAILURE */

#include "init.h"
#include "interpose.h"
#include "lock.h"
#include "thread.h"
#include "trace.h"

REAL_DEFN(int, pthread_mutex_lock, pthread_mutex_t *);
REAL_DEFN(int, pthread_mutex_trylock, pthread_mutex_t *);
REAL_DEFN(int, pthread_mutex_unlock, pthread_mutex_t *);
REAL_DEFN(int, pthread_mutex_init, pthread_mutex_t *restrict,
   const pthread_mutexattr_t *restrict);

void
rvp_lock_init(void)
{
	ESTABLISH_PTR_TO_REAL(int (*)(pthread_mutex_t *), pthread_mutex_lock);
	ESTABLISH_PTR_TO_REAL(int (*)(pthread_mutex_t *),
	    pthread_mutex_trylock);
	ESTABLISH_PTR_TO_REAL(int (*)(pthread_mutex_t *), pthread_mutex_unlock);
	ESTABLISH_PTR_TO_REAL(
	    int (*)(pthread_mutex_t *restrict,
	            const pthread_mutexattr_t *restrict), pthread_mutex_init);
}

int
__rvpredict_pthread_mutex_init(pthread_mutex_t *restrict mtx,
   const pthread_mutexattr_t *restrict attr)
{
	if (attr != NULL) 
		errx(EXIT_FAILURE, "%s: attr != NULL not supported", __func__);
	return real_pthread_mutex_init(mtx, attr);
}

static inline void
trace_mutex_op(const void *retaddr, pthread_mutex_t *mtx, rvp_op_t op)
{
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;
	uint64_t gen;

	if (op == RVP_OP_ACQUIRE)
		rvp_buf_trace_load_cog(&b, &r->r_lgen);
	else
		gen = rvp_ggen_before_store();

	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, retaddr, op);
	rvp_buf_put_voidptr(&b, mtx);

	if (op != RVP_OP_ACQUIRE)
		rvp_buf_trace_cog(&b, &r->r_lgen, gen);

	rvp_ring_put_buf(r, b);
}

int
__rvpredict_pthread_mutex_lock(pthread_mutex_t *mtx)
{
	int rc;

	rc = real_pthread_mutex_lock(mtx);
	trace_mutex_op(__builtin_return_address(0), mtx, RVP_OP_ACQUIRE);

	return rc;
}

int
__rvpredict_pthread_mutex_trylock(pthread_mutex_t *mtx)
{
	int rc;

	if ((rc = real_pthread_mutex_trylock(mtx)) != 0)
		return rc;

	trace_mutex_op(__builtin_return_address(0), mtx, RVP_OP_ACQUIRE);

	return 0;
}

int
__rvpredict_pthread_mutex_unlock(pthread_mutex_t *mtx)
{
	trace_mutex_op(__builtin_return_address(0), mtx, RVP_OP_RELEASE);

	return real_pthread_mutex_unlock(mtx);
}

INTERPOSE(int, pthread_mutex_lock, pthread_mutex_t *);
INTERPOSE(int, pthread_mutex_unlock, pthread_mutex_t *);
INTERPOSE(int, pthread_mutex_init, pthread_mutex_t *restrict,
   const pthread_mutexattr_t *restrict);
