#include <err.h>
#include <stdlib.h> /* for EXIT_FAILURE */

#include "lock.h"
#include "thread.h"
#include "trace.h"

int
__rvpredict_pthread_mutex_init(pthread_mutex_t *restrict mtx,
   const pthread_mutexattr_t *restrict attr)
{
	if (attr != NULL) 
		errx(EXIT_FAILURE, "%s: attr != NULL not supported", __func__);
	return pthread_mutex_init(mtx, attr);
}

static inline void
trace_mutex_op(const void *retaddr, pthread_mutex_t *mtx, rvp_op_t op)
{
	rvp_ring_t *r = rvp_ring_for_curthr();

	rvp_ring_put_pc_and_op(r, retaddr, op);
	rvp_ring_put_addr(r, mtx);
}

int
__rvpredict_pthread_mutex_lock(pthread_mutex_t *mtx)
{
	int rc;

	rc = pthread_mutex_lock(mtx);
	trace_mutex_op(__builtin_return_address(1), mtx, RVP_OP_ACQUIRE);

	return rc;
}

int
__rvpredict_pthread_mutex_trylock(pthread_mutex_t *mtx)
{
	int rc;

	if ((rc = pthread_mutex_trylock(mtx)) != 0)
		return rc;

	trace_mutex_op(__builtin_return_address(1), mtx, RVP_OP_ACQUIRE);

	return 0;
}

int
__rvpredict_pthread_mutex_unlock(pthread_mutex_t *mtx)
{
	trace_mutex_op(__builtin_return_address(1), mtx, RVP_OP_RELEASE);

	return pthread_mutex_unlock(mtx);
}
