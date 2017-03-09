/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#include <assert.h>
#include <err.h> /* for err(3) */
#include <errno.h> /* for ESRCH */
#include <inttypes.h> /* for PRIu32 */
#include <signal.h> /* for pthread_sigmask */
#include <stdint.h> /* for uint32_t */
#include <stdlib.h> /* for EXIT_FAILURE */
#include <stdio.h> /* for fprintf(3) */
#include <string.h> /* for strerror(3) */
#include <unistd.h> /* for sysconf */

#include "init.h"
#include "interpose.h"
#include "relay.h"
#include "rvpsignal.h"
#include "thread.h"
#include "trace.h"

REAL_DEFN(int, pthread_join, pthread_t, void **);
REAL_DEFN(int, pthread_create, pthread_t *, const pthread_attr_t *,
    void *(*)(void *), void *);
REAL_DEFN(void, pthread_exit, void *);

static rvp_thread_t *rvp_thread_create(void *(*)(void *), void *);

volatile _Atomic uint64_t rvp_ggen = 0;	// global generation number

unsigned int rvp_log2_nthreads = 1;

/* thread_mutex protects thread_head, next_id */
static pthread_mutex_t thread_mutex;
static rvp_thread_t * volatile thread_head = NULL;
static uint32_t next_id = 0;

static pthread_t serializer;
static pthread_cond_t wakecond;
static int nwake = 0;
static int serializer_fd;
static rvp_lastctx_t serializer_lc;
static bool transmit = true;

pthread_key_t rvp_thread_key;
static pthread_once_t rvp_init_once = PTHREAD_ONCE_INIT;

static int rvp_thread_detach(rvp_thread_t *);
static void rvp_thread_destroy(rvp_thread_t *);
static void rvp_thread0_create(void);
static rvp_thread_t *rvp_collect_garbage(void);

static inline void
rvp_trace_fork(uint32_t id, const void *retaddr)
{
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;
	uint64_t gen;

	gen = rvp_ggen_before_store();
	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, retaddr, RVP_OP_FORK);
	rvp_buf_put(&b, id);
	if (r->r_lgen < gen) {
		r->r_lgen = gen;
		rvp_buf_put_cog(&b, gen);
	}
	rvp_ring_put_buf(r, b);
}

static inline void
rvp_trace_join(uint32_t id, const void *retaddr)
{
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;

	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, retaddr, RVP_OP_JOIN);
	rvp_buf_put(&b, id);
	rvp_ring_put_buf(r, b);
}

static inline void
rvp_trace_end(void)
{
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;

	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, __builtin_return_address(0),
	    RVP_OP_END);
	rvp_ring_put_buf(r, b);
	rvp_ring_request_service(r);
}

static void
rvp_thread0_create(void)
{
	rvp_thread_t *t;
	rvp_ring_t *r;

	if ((t = rvp_thread_create(NULL, NULL)) == NULL)
		err(EXIT_FAILURE, "%s: rvp_thread_create", __func__);

	if (pthread_setspecific(rvp_thread_key, t) != 0)
		err(EXIT_FAILURE, "%s: pthread_setspecific", __func__);

	t->t_pthread = pthread_self();

	assert(t->t_id == 1);

	r = &t->t_ring;
	r->r_lgen = rvp_ggen_after_load();
	rvp_ring_put_begin(r, t->t_id, r->r_lgen);
}

static void
thread_lock(void)
{
	if (real_pthread_mutex_lock(&thread_mutex) != 0)
		err(EXIT_FAILURE, "%s: pthread_mutex_lock", __func__);
}

static void
thread_unlock(void)
{
	if (real_pthread_mutex_unlock(&thread_mutex) != 0)
		err(EXIT_FAILURE, "%s: pthread_mutex_unlock", __func__);
}

/* Caller must hold thread_mutex. */
static void
rvp_wake_transmitter_locked(void)
{
	int rc;

	nwake++;
	if ((rc = pthread_cond_signal(&wakecond)) != 0)
		errx(EXIT_FAILURE, "%s: %s", __func__, strerror(rc));
}

static void
rvp_stop_transmitter(void)
{
	int rc;

	thread_lock();
	transmit = false;
	rvp_wake_transmitter_locked();
	thread_unlock();

	if ((rc = real_pthread_join(serializer, NULL)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_join: %s",
		    __func__, strerror(rc));
	}
}

static void *
serialize(void *arg)
{
	int fd = serializer_fd;
	rvp_thread_t *t, *next_t;
	uint32_t nblocksets_last = 0;
	sigset_t maskall;

	sigfillset(&maskall);

	real_pthread_sigmask(SIG_BLOCK, &maskall, NULL);

	thread_lock();
	while (transmit || nwake > 0) {
		bool any_emptied;

		while (nwake == 0) {
			int rc = pthread_cond_wait(&wakecond, &thread_mutex);
			if (rc != 0) {
				errx(EXIT_FAILURE, "%s: pthread_cond_wait: %s",
				    __func__, strerror(rc));
			}
		}
		nwake--;

		rvp_increase_ggen();

		rvp_signal_rings_replenish();

		do {

			/* TBD increase global generation every so
			 * many words flushed?
			 */

			nblocksets_last = rvp_sigblocksets_emit(fd,
			    nblocksets_last);
			any_emptied = false;
			for (t = thread_head; t != NULL; t = t->t_next) {
				any_emptied |= rvp_ring_flush_to_fd(&t->t_ring,
				    fd, &serializer_lc);
			}
			any_emptied |= rvp_signal_rings_flush_to_fd(fd,
			    &serializer_lc);
		} while (any_emptied);

		for (t = rvp_collect_garbage(); t != NULL; t = next_t) {
			next_t = t->t_next;
			rvp_thread_destroy(t);
		}
	}
	thread_unlock();
	if (close(fd) == -1)
		warn("%s: close", __func__);
	return NULL;
}

static void
rvp_serializer_create(void)
{
	int rc;

	if ((serializer_fd = rvp_trace_open()) == -1)
		err(EXIT_FAILURE, "%s: rvp_trace_open", __func__);

	thread_lock();
	assert(thread_head->t_next == NULL);
	/* I don't use rvp_thread_flush_to_fd() here because I do not
	 * want to log a change of thread here under any circumstances.
	 */
	rvp_ring_flush_to_fd(&thread_head->t_ring, serializer_fd, NULL);
	serializer_lc = (rvp_lastctx_t){
		  .lc_tid = thread_head->t_id
		, .lc_nintr_outst = 0
	};
	thread_unlock();

	rc = real_pthread_create(&serializer, NULL, serialize, NULL);

	if (rc != 0) {
		errx(EXIT_FAILURE, "%s: pthread_create: %s", __func__,
		    strerror(rc));
	}
}

void
rvp_thread_init(void)
{
	ESTABLISH_PTR_TO_REAL(pthread_join);
	ESTABLISH_PTR_TO_REAL(pthread_create);
	ESTABLISH_PTR_TO_REAL(pthread_exit);
}

static void
rvp_init(void)
{
	rvp_lock_init();	// needed by rvp_signal_init()
	rvp_signal_init();
	rvp_thread_init();
	rvp_rings_init();

	if (pthread_key_create(&rvp_thread_key, NULL) != 0) 
		err(EXIT_FAILURE, "%s: pthread_key_create", __func__);
	if (pthread_mutex_init(&thread_mutex, NULL) != 0)
		err(EXIT_FAILURE, "%s: pthread_mutex_init", __func__);
	if (pthread_cond_init(&wakecond, NULL) != 0)
		err(EXIT_FAILURE, "%s: pthread_mutex_init", __func__);
	/* The 'begin' op for the first thread (tid 1) has to be
	 * written directly after the header, and it is by virtue of
	 * rvp_serializer_create() being called directly after
	 * rvp_thread0_create(), before any other thread has an opportunity
	 * to start.
	 */
	rvp_thread0_create();
	rvp_relay_create();
	rvp_serializer_create();

	rvp_static_intrs_init();

	atexit(rvp_stop_transmitter);
}

void
__rvpredict_init(void)
{
	(void)pthread_once(&rvp_init_once, rvp_init);
}

static void
rvp_update_nthreads(uint32_t n)
{
	while ((1 << rvp_log2_nthreads) < n)
		rvp_log2_nthreads <<= 1;
}

static int
rvp_thread_attach(rvp_thread_t *t)
{
	thread_lock();

	if ((t->t_id = ++next_id) == 0) {
		thread_unlock();
		errx(EXIT_FAILURE, "%s: out of thread IDs", __func__);
	}

	rvp_update_nthreads(t->t_id);

	t->t_ring.r_tid = t->t_id;
	t->t_ring.r_nintr_outst = 0;

	t->t_next = thread_head;
	thread_head = t;

	thread_unlock();

	return 0;
}

/* Caller must hold thread_mutex. */ 
static rvp_thread_t *
rvp_collect_garbage(void)
{
	rvp_thread_t * volatile *tp;
	rvp_thread_t *garbage_head = NULL;
	rvp_thread_t * volatile *gtp = &garbage_head;

	for (tp = &thread_head; *tp != NULL;) {
		rvp_thread_t *t = *tp;
		if (t->t_garbage) {
			*tp = (*tp)->t_next;
			*gtp = t;
			gtp = &t->t_next;
		} else
			tp = &(*tp)->t_next;
	}
	*gtp = NULL;
	return garbage_head;
}

static int
rvp_thread_detach(rvp_thread_t *tgt)
{
	rvp_thread_t * volatile *tp;
	int rc;

	thread_lock();

	for (tp = &thread_head; *tp != NULL && *tp != tgt; tp = &(*tp)->t_next)
		;

	if (*tp != NULL) {
		*tp = (*tp)->t_next;
		rc = 0;
	} else
		rc = ENOENT;

	thread_unlock();

	return rc;
}

static void
rvp_thread_destroy(rvp_thread_t *t)
{
	free(t->t_ring.r_items);
	free(t);
}

static rvp_thread_t *
rvp_thread_create(void *(*routine)(void *), void *arg)
{
	rvp_thread_t *t;
	int rc;

	if ((t = calloc(1, sizeof(*t))) == NULL) {
		errno = ENOMEM;
		return NULL;
	}

	t->t_routine = routine;
	t->t_arg = arg;

	if ((rc = rvp_ring_stdinit(&t->t_ring)) == -1) {
		free(t);
		errno = rc;
		return NULL;
	}

	rvp_thread_attach(t);

	return t;
}

static void
rvp_thread_mark(rvp_thread_t *t)
{
	t->t_garbage = true;
	rvp_wake_transmitter();
}

rvp_thread_t *
rvp_pthread_to_thread(pthread_t pthread)
{
	rvp_thread_t *t;

	thread_lock();

	for (t = thread_head; t != NULL; t = t->t_next) {
		if (t->t_pthread == pthread)
			break;
	}

	thread_unlock();

	if (t == NULL)
		errno = ESRCH;

	return t;
}

/* TBD For signal-safety, avoid using mutexes and condition variables.
 * Use pthread_kill(3) and an atomic?
 */
void
rvp_wake_transmitter(void)
{
	thread_lock();
	rvp_wake_transmitter_locked();
	thread_unlock();
}

static void *
__rvpredict_thread_wrapper(void *arg)
{
	sigset_t set, oset;
	int rc;
	void *retval;
	rvp_thread_t *t = arg;
	rvp_ring_t *r = &t->t_ring;

	assert(pthread_getspecific(rvp_thread_key) == NULL);

	if (pthread_setspecific(rvp_thread_key, t) != 0)
		err(EXIT_FAILURE, "%s: pthread_setspecific", __func__);

	r->r_lgen = rvp_ggen_after_load();
	rvp_ring_put_begin(&t->t_ring, t->t_id, r->r_lgen);

	rc = real_pthread_sigmask(SIG_SETMASK,
	    mask_to_sigset(t->t_intrmask, &set), &oset);

	if (rc != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}

	retval = (*t->t_routine)(t->t_arg);

	/* XXX Probably should run hooks established by pthread_cleanup_push
	 * before restoring the runtime's signal mask, but we shouldn't
	 * run all of real_pthread_exit() with signals unblocked.
	 * pthread_cleanup_push() something that restores the runtime's
	 * signal mask?
	 */
	rc = real_pthread_sigmask(SIG_SETMASK, &oset, NULL);

	if (rc != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}

	__rvpredict_pthread_exit(retval);
	/* probably never reached */
	return retval;
}

int
__rvpredict_pthread_create(pthread_t *thread,
    const pthread_attr_t *attr, void *(*start_routine) (void *), void *arg)
{
	int rc, rc2;
	rvp_thread_t *t;

	if ((t = rvp_thread_create(start_routine, arg)) == NULL)
		return errno;

	/* Block all signals, initialize t_intrmask with the
	 * mask that used to be in effect, call real_pthread_create,
	 * and re-establish the mask that used to be in effect.
	 *
	 * __rvpredict_thread_wrapper establishes the mask in t_intrmask
	 * before calling start_routine.
	 *
	 * TBD After start_routine finishes, block all signals again?
	 * Nah, I cannot think of a reason for it.
	 *
	 * Careful: call real_pthread_sigmask to establish masks!
	 */
	sigset_t fullset, oset;
	if (sigfillset(&fullset) == -1)
		err(EXIT_FAILURE, "%s: sigfillset", __func__);
	if ((rc = real_pthread_sigmask(SIG_BLOCK, &fullset, &oset)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}
	t->t_intrmask = sigset_to_mask(&oset);

	rc = real_pthread_create(&t->t_pthread, attr,
	    __rvpredict_thread_wrapper, t);

	rc2 = real_pthread_sigmask(SIG_SETMASK, &oset, NULL);

	if (rc2 != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc2));
	}

	if (rc == 0) {
		*thread = t->t_pthread;
		rvp_trace_fork(t->t_id, __builtin_return_address(0));
		return 0;
	}

	if (rvp_thread_detach(t) != 0)
		err(EXIT_FAILURE, "%s: rvp_thread_detach", __func__);

	rvp_thread_destroy(t);

	return rc;
}

void
__rvpredict_pthread_exit(void *retval)
{
	rvp_trace_end();
	real_pthread_exit(retval);
	/* TBD flag change of status so that we can flush the trace
	 * and reclaim resources---e.g., munmap/free the ring
	 * once it's empty.  Careful: need to hang around for _join().
	 */
}

int
__rvpredict_pthread_join(pthread_t pthread, void **retval)
{
	int rc;
	rvp_thread_t *t;

	if ((rc = real_pthread_join(pthread, retval)) != 0)
		return rc;

	if ((t = rvp_pthread_to_thread(pthread)) == NULL)
		err(EXIT_FAILURE, "%s: rvp_pthread_to_thread", __func__);

	rvp_trace_join(t->t_id, __builtin_return_address(0));

	rvp_thread_mark(t);

	return 0;
}

INTERPOSE(int, pthread_join, pthread_t, void **);
INTERPOSE(int, pthread_create, pthread_t *, const pthread_attr_t *,
    void *(*)(void *), void *);
INTERPOSE(void, pthread_exit, void *);
