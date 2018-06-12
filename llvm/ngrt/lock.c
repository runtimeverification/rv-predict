#include <err.h>
#include <stdlib.h> /* for EXIT_FAILURE */

#include "init.h"
#include "interpose.h"
#include "lock.h"
#include "thread.h"
#include "trace.h"

REAL_DEFN(int, pthread_cond_timedwait, pthread_cond_t *restrict,
    pthread_mutex_t *restrict, const struct timespec *restrict);
REAL_DEFN_VER3(2, 2, 5, int, pthread_cond_timedwait, pthread_cond_t *restrict,
    pthread_mutex_t *restrict, const struct timespec *restrict);
REAL_DEFN_VER3(2, 3, 2, int, pthread_cond_timedwait, pthread_cond_t *restrict,
    pthread_mutex_t *restrict, const struct timespec *restrict);

#if 0
REAL_DEFN(int, pthread_cond_signal, pthread_cond_t *);
REAL_DEFN_VER3(2, 2, 5, int, pthread_cond_signal, pthread_cond_t *);
REAL_DEFN_VER3(2, 3, 2, int, pthread_cond_signal, pthread_cond_t *);
#endif

REAL_DEFN(int, pthread_cond_wait, pthread_cond_t *restrict,
    pthread_mutex_t *restrict);
REAL_DEFN_VER3(2, 2, 5, int, pthread_cond_wait, pthread_cond_t *restrict,
    pthread_mutex_t *restrict);
REAL_DEFN_VER3(2, 3, 2, int, pthread_cond_wait, pthread_cond_t *restrict,
    pthread_mutex_t *restrict);

REAL_DEFN(int, pthread_cond_init, pthread_cond_t *restrict,
    const pthread_condattr_t *restrict);
REAL_DEFN_VER3(2, 2, 5, int, pthread_cond_init, pthread_cond_t *restrict,
    const pthread_condattr_t *restrict);
REAL_DEFN_VER3(2, 3, 2, int, pthread_cond_init, pthread_cond_t *restrict,
    const pthread_condattr_t *restrict);

REAL_DEFN(int, pthread_mutex_lock, pthread_mutex_t *);
REAL_DEFN(int, pthread_mutex_trylock, pthread_mutex_t *);
REAL_DEFN(int, pthread_mutex_unlock, pthread_mutex_t *);
REAL_DEFN(int, pthread_mutex_init, pthread_mutex_t *restrict,
   const pthread_mutexattr_t *restrict);

void
rvp_lock_prefork_init(void)
{
	ESTABLISH_PTR_TO_REAL(int (*)(pthread_mutex_t *), pthread_mutex_lock);
	ESTABLISH_PTR_TO_REAL(int (*)(pthread_mutex_t *),
	    pthread_mutex_trylock);
	ESTABLISH_PTR_TO_REAL(int (*)(pthread_mutex_t *), pthread_mutex_unlock);
	ESTABLISH_PTR_TO_REAL(
	    int (*)(pthread_mutex_t *restrict,
	            const pthread_mutexattr_t *restrict), pthread_mutex_init);

	ESTABLISH_PTR_TO_REAL_VER3(2, 2, 5, int (*)(pthread_cond_t *restrict,
	    pthread_mutex_t *restrict, const struct timespec *restrict),
	    pthread_cond_timedwait);
	ESTABLISH_PTR_TO_REAL_VER3(2, 3, 2, int (*)(pthread_cond_t *restrict,
	    pthread_mutex_t *restrict, const struct timespec *restrict),
	    pthread_cond_timedwait);

#if 0
	ESTABLISH_PTR_TO_REAL_VER3(2, 2, 5, int (*)(pthread_cond_t *),
	    pthread_cond_signal);
	ESTABLISH_PTR_TO_REAL_VER3(2, 3, 2, int (*)(pthread_cond_t *),
	    pthread_cond_signal);
#endif

	ESTABLISH_PTR_TO_REAL_VER3(2, 2, 5, int (*)(pthread_cond_t *restrict,
	    pthread_mutex_t *restrict), pthread_cond_wait);
	ESTABLISH_PTR_TO_REAL_VER3(2, 3, 2, int (*)(pthread_cond_t *restrict,
	    pthread_mutex_t *restrict), pthread_cond_wait);

	ESTABLISH_PTR_TO_REAL_VER3(2, 2, 5, int (*)(pthread_cond_t *restrict,
	    const pthread_condattr_t *restrict), pthread_cond_init);
	ESTABLISH_PTR_TO_REAL_VER3(2, 3, 2, int (*)(pthread_cond_t *restrict,
	    const pthread_condattr_t *restrict), pthread_cond_init);
}

int
__rvpredict_pthread_mutex_init(pthread_mutex_t *restrict mtx,
   const pthread_mutexattr_t *restrict attr)
{
#if 0
	/* TBD check an environment variable (RVP_STRICT=warn, say) and
	 * print this warning when it is set.
	 *
	 * Possibly quit if RVP_STRICT=abort.
	 */
	if (attr != NULL) {
		warnx("%s: not all pthread mutex attributes are supported",
		    __func__);
	}
#endif
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

int
__rvpredict225_pthread_cond_init(pthread_cond_t *restrict cv,
   const pthread_condattr_t *restrict attr)
{
#if 0
	/* TBD check an environment variable (RVP_STRICT=warn, say) and
	 * print this warning when it is set.
	 *
	 * Possibly quit if RVP_STRICT=abort.
	 */
	if (attr != NULL) {
		warnx("%s: not all POSIX condition-variable attributes are "
		    "supported", __func__);
	}
#endif
	return real225_pthread_cond_init(cv, attr);
}

int
__rvpredict232_pthread_cond_init(pthread_cond_t *restrict cv,
   const pthread_condattr_t *restrict attr)
{
#if 0
	/* TBD check an environment variable (RVP_STRICT=warn, say) and
	 * print this warning when it is set.
	 *
	 * Possibly quit if RVP_STRICT=abort.
	 */
	if (attr != NULL) {
		warnx("%s: not all POSIX condition-variable attributes are "
		    "supported", __func__);
	}
#endif
	return real232_pthread_cond_init(cv, attr);
}

#if 0
int
__rvpredict225_pthread_cond_signal(pthread_cond_t *cv)
{
	return real225_pthread_cond_signal(cv);
}

int
__rvpredict232_pthread_cond_signal(pthread_cond_t *cv)
{
	return real232_pthread_cond_signal(cv);
}
#endif

int
__rvpredict225_pthread_cond_timedwait(pthread_cond_t *cv, pthread_mutex_t *mtx,
    const struct timespec *abstime)
{
	int rc;

	trace_mutex_op(__builtin_return_address(0), mtx, RVP_OP_RELEASE);
	rc = real225_pthread_cond_timedwait(cv, mtx, abstime);
	trace_mutex_op(__builtin_return_address(0), mtx, RVP_OP_ACQUIRE);

	return rc;
}

int
__rvpredict232_pthread_cond_timedwait(pthread_cond_t *cv, pthread_mutex_t *mtx,
    const struct timespec *abstime)
{
	int rc;

	trace_mutex_op(__builtin_return_address(0), mtx, RVP_OP_RELEASE);
	rc = real232_pthread_cond_timedwait(cv, mtx, abstime);
	trace_mutex_op(__builtin_return_address(0), mtx, RVP_OP_ACQUIRE);

	return rc;
}


int
__rvpredict225_pthread_cond_wait(pthread_cond_t *cv, pthread_mutex_t *mtx)
{
	int rc;

	trace_mutex_op(__builtin_return_address(0), mtx, RVP_OP_RELEASE);
	rc = real225_pthread_cond_wait(cv, mtx);
	trace_mutex_op(__builtin_return_address(0), mtx, RVP_OP_ACQUIRE);

	return rc;
}

int
__rvpredict232_pthread_cond_wait(pthread_cond_t *cv, pthread_mutex_t *mtx)
{
	int rc;

	trace_mutex_op(__builtin_return_address(0), mtx, RVP_OP_RELEASE);
	rc = real232_pthread_cond_wait(cv, mtx);
	trace_mutex_op(__builtin_return_address(0), mtx, RVP_OP_ACQUIRE);

	return rc;
}

INTERPOSE(int, pthread_mutex_lock, pthread_mutex_t *);
INTERPOSE(int, pthread_mutex_unlock, pthread_mutex_t *);
INTERPOSE(int, pthread_mutex_init, pthread_mutex_t *restrict,
   const pthread_mutexattr_t *restrict);

INTERPOSE_VER3(2, 2, 5, int, pthread_cond_timedwait, pthread_cond_t *restrict,
    pthread_mutex_t *restrict, const struct timespec *restrict);
INTERPOSE_DEFAULT_VER3(2, 3, 2, int, pthread_cond_timedwait,
    pthread_cond_t *restrict, pthread_mutex_t *restrict,
    const struct timespec *restrict);

#if 0
INTERPOSE_VER3(2, 2, 5, int, pthread_cond_signal, pthread_cond_t *);
INTERPOSE_DEFAULT_VER3(2, 3, 2, int, pthread_cond_signal, pthread_cond_t *);
#endif

INTERPOSE_VER3(2, 2, 5, int, pthread_cond_wait, pthread_cond_t *restrict,
    pthread_mutex_t *restrict);
INTERPOSE_DEFAULT_VER3(2, 3, 2, int, pthread_cond_wait,
    pthread_cond_t *restrict, pthread_mutex_t *restrict);

INTERPOSE_VER3(2, 2, 5, int, pthread_cond_init, pthread_cond_t *restrict,
    const pthread_condattr_t *restrict);
INTERPOSE_DEFAULT_VER3(2, 3, 2, int, pthread_cond_init,
    pthread_cond_t *restrict, const pthread_condattr_t *restrict);
