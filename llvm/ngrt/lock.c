#include <err.h>
#include <stdlib.h> /* for EXIT_FAILURE */
#include <stdatomic.h>

#include "init.h"
#include "interpose.h"
#include "lock.h"
#include "thread.h"
#include "fence.h"
#include "trace.h"

#if 0   /* https://www.ibm.com/support/knowledgecenter/en/SSLTBW_2.1.0/com.ibm.zos.v2r1.cbclx01/atomiccpp_memory_order.htm */
namespace std {
  typedef enum memory_order {
    memory_order_relaxed, 
    memory_order_consume, 
    memory_order_acquire,
    memory_order_release, 
    memory_order_acq_rel, 
    memory_order_seq_cst
  } memory_order;
}
#endif

#if 0 /* atomic_thread_fence is a macro with param, and a Clang built in */
/usr/lib/llvm-4.0/bin/../lib/clang/4.0.0/include/stdatomic.h:78:36 
In stdatomic,h: #define atomic_thread_fence(order) __c11_atomic_thread_fence(order)
  Note: the function is a built in - so we can't point to it.

RVPredict implements the routine __rvpredict_atomic_thread_fence( memory_order order)
                                 which invokes the macro atomic_thread_fence
                                    which expands to _c11_atomic_thread_fence.
#endif


REAL_DEFN(int, pthread_mutex_lock, pthread_mutex_t *);
REAL_DEFN(int, pthread_mutex_trylock, pthread_mutex_t *);
REAL_DEFN(int, pthread_mutex_unlock, pthread_mutex_t *);
REAL_DEFN(int, pthread_mutex_init, pthread_mutex_t *restrict,
   const pthread_mutexattr_t *restrict);

/*
 *   Programs will call lock functions during their initialization before
 * rv-predict can get tracing setup. This requires verifying that things 
 * are initialized. If rv-predict is not initialized, then we will not trace 
 * to the rv_predict ring.
 *
 * There are two stages:
 * Stage 1: Nothing is initialized (rvp_real_locks_initialized == false)
 * 	Call rvp_lock_prefork_init to get ptrs to real pthread_mutex_... routines established)
 * Stage 2: The ring is ready (rvp_initialized == true)
 * 
 *   We use the inline routine rvp_ring_initialized to indicate when one can
 * trace to the ring.
 */


volatile _Atomic bool __read_mostly rvp_real_locks_initialized = false ;

static inline bool
		if (!rvp_real_locks_initialized)
=======
is_ring_initialized(void) 
{
	if (!rvp_initialized) {
		if (!rvp_real_locks_initialized)
			rvp_lock_prefork_init();
		return false;
	}
	return true;
}

void
rvp_lock_prefork_init(void)
{
	ESTABLISH_PTR_TO_REAL(int (*)(pthread_mutex_t *), pthread_mutex_lock);
	ESTABLISH_PTR_TO_REAL(int (*)(pthread_mutex_t *), pthread_mutex_trylock);
	ESTABLISH_PTR_TO_REAL(int (*)(pthread_mutex_t *), pthread_mutex_unlock);
	ESTABLISH_PTR_TO_REAL(
	    int (*)(pthread_mutex_t *restrict,
	            const pthread_mutexattr_t *restrict), pthread_mutex_init);
	rvp_real_locks_initialized = true;
<<<<<<< HEAD
}

void 
__rvpredict_atomic_thread_fence( memory_order order)
{
	atomic_thread_fence( order); /* Let define in stdatomic.h expand to _c11_atomic_thread_fence */
	return;
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

	if (is_ring_initialized()) {
		return real_pthread_mutex_init(mtx, attr);
	}
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
	if (is_ring_initialized()) {
		trace_mutex_op(__builtin_return_address(0), mtx, RVP_OP_ACQUIRE);
	}

	return rc;
}

int
__rvpredict_pthread_mutex_trylock(pthread_mutex_t *mtx)
{
	int rc;
	bool ri;

	ri = is_ring_initialized();
	rc = real_pthread_mutex_trylock(mtx);
	if (rc != 0)
		return rc;
	if (ri) {
		trace_mutex_op(__builtin_return_address(0), mtx, RVP_OP_ACQUIRE);
	}
	return 0;
}

int
__rvpredict_pthread_mutex_unlock(pthread_mutex_t *mtx)
{
	if (is_ring_initialized()) {
		trace_mutex_op(__builtin_return_address(0), mtx, RVP_OP_RELEASE);
	}
	return real_pthread_mutex_unlock(mtx);
}
INTERPOSE(int, pthread_mutex_lock, pthread_mutex_t *);
INTERPOSE(int, pthread_mutex_unlock, pthread_mutex_t *);
INTERPOSE(int, pthread_mutex_init, pthread_mutex_t *restrict,
   const pthread_mutexattr_t *restrict);
