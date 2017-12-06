/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#include <assert.h>
#include <err.h> /* for err(3) */
#include <errno.h> /* for ESRCH */
#include <inttypes.h> /* for PRIu32 */
#include <signal.h> /* for pthread_sigmask */
#include <stdatomic.h> /* for atomic_is_lock_free */
#include <stdint.h> /* for uint32_t */
#include <stdlib.h> /* for EXIT_FAILURE */
#include <stdio.h> /* for fprintf(3) */
#include <string.h> /* for strerror(3), strcasecmp(3) */
#include <unistd.h> /* for sysconf */

#include "init.h"
#include "interpose.h"
#include "relay.h"
#include "rvpsignal.h"
#include "supervise.h"
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
static sigset_t allmask;
static pthread_mutex_t thread_mutex;
static rvp_thread_t * volatile thread_head = NULL;
static uint32_t next_id = 0;

static pthread_t serializer;
static pthread_cond_t wakecond;
static pthread_cond_t stopcond;
static _Atomic int nwake = 0;
static bool forked = false;
static int serializer_fd;
static rvp_lastctx_t serializer_lc;
static bool stopping = false;
bool rvp_trace_only = true;
ssize_t rvp_trace_size_limit;

pthread_key_t rvp_thread_key;
static pthread_once_t rvp_postfork_init_once = PTHREAD_ONCE_INIT;
static pthread_once_t rvp_prefork_init_once = PTHREAD_ONCE_INIT;

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
	r->r_lastpc = rvp_vec_and_op_to_deltop(0, RVP_OP_BEGIN);
	rvp_ring_put_begin(r, t->t_id, r->r_lgen);
}

static void
thread_lock(sigset_t *oldmask)
{
	if (real_pthread_sigmask(SIG_BLOCK, &allmask, oldmask) != 0)
		err(EXIT_FAILURE, "%s: pthread_sigmask", __func__);
	if (real_pthread_mutex_lock(&thread_mutex) != 0)
		err(EXIT_FAILURE, "%s: pthread_mutex_lock", __func__);
}

static void
thread_unlock(const sigset_t *oldmask)
{
	if (real_pthread_mutex_unlock(&thread_mutex) != 0)
		err(EXIT_FAILURE, "%s: pthread_mutex_unlock", __func__);
	if (real_pthread_sigmask(SIG_SETMASK, oldmask, NULL) != 0)
		err(EXIT_FAILURE, "%s: pthread_sigmask", __func__);
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
	sigset_t oldmask;

	thread_lock(&oldmask);
	rvp_wake_transmitter_locked();
	stopping = true;
	while (nwake > 0) {
		rc = pthread_cond_wait(&stopcond, &thread_mutex);
		if (rc != 0) {
			errx(EXIT_FAILURE, "%s: pthread_cond_wait: %s",
			    __func__, strerror(rc));
		}
	}
	stopping = false;
	thread_unlock(&oldmask);
}

static void *
serialize(void *arg __unused)
{
	int fd = serializer_fd;
	rvp_thread_t *t, *next_t;
	rvp_sigblockset_t *blocksets_last = NULL;
	sigset_t maskall, oldmask;

	/* This shouldn't be necessary: this thread is created with
	 * all signals blocked.
	 */
	sigfillset(&maskall);
	real_pthread_sigmask(SIG_BLOCK, &maskall, NULL);

	thread_lock(&oldmask);
	for (;;) {
		bool any_emptied;

		while (nwake == 0) {
			int rc;

			if (stopping &&
			    (rc = pthread_cond_broadcast(&stopcond)) != 0) {
				errx(EXIT_FAILURE,
				    "%s: pthread_cond_broadcast: %s",
				    __func__, strerror(rc));
			}

			rc = pthread_cond_wait(&wakecond, &thread_mutex);
			if (rc != 0) {
				errx(EXIT_FAILURE, "%s: pthread_cond_wait: %s",
				    __func__, strerror(rc));
			}
		}
		nwake--;

		rvp_increase_ggen();

		rvp_sigblocksets_replenish();
		rvp_signal_rings_replenish();

		do {

			/* TBD increase global generation every so
			 * many words flushed?
			 */

			blocksets_last = rvp_sigblocksets_emit(fd,
			    blocksets_last);
			any_emptied = false;
			for (t = thread_head; t != NULL; t = t->t_next) {
				any_emptied |= rvp_ring_flush_to_fd(&t->t_ring,
				    fd, &serializer_lc);
			}
#if 0
			any_emptied |= rvp_signal_rings_flush_to_fd(fd,
			    &serializer_lc);
#endif
		} while (any_emptied);

		if (rvp_trace_size_limit <= rvp_trace_size) {
			warnx("trace-file size %zd %s limit (%zd)",
			    rvp_trace_size,
			    (rvp_trace_size_limit < rvp_trace_size)
			        ? "exceeded"
				: "reached",
			    rvp_trace_size_limit);
			abort();
		}
		for (t = rvp_collect_garbage(); t != NULL; t = next_t) {
			next_t = t->t_next;
			rvp_thread_destroy(t);
		}
	}
	thread_unlock(&oldmask);
	if (close(fd) == -1)
		warn("%s: close", __func__);
	return NULL;
}

static void
rvp_serializer_create(void)
{
	int rc;
	sigset_t oldmask;
	const char *s;

	if ((s = getenv("RVP_TRACE_SIZE_LIMIT")) != NULL) {
		const uintmax_t k = 1024, M = k * k, G = k * k * k;
		uintmax_t factor, limit;
		char *end;

		limit = strtoumax(s, &end, 10);
		switch (*end) {
		case 'k':
			factor = k;
			end++;
			break;
		case 'M':
			factor = M;
			end++;
			break;
		case 'G':
			factor = G;
			end++;
			break;
		default:
			factor = 1;
			break;
		}

		if (*end != '\0') {
			errx(EXIT_FAILURE,
			    "RVP_TRACE_SIZE_LIMIT (%s) ends with "
			    "extraneous characters (%s)", s, end);
		}

		if ((limit == INTMAX_MIN || limit == INTMAX_MAX) &&
		    errno == ERANGE) {
			err(EXIT_FAILURE, "RVP_TRACE_SIZE_LIMIT (%s)", s);
		}

		if (SSIZE_MAX / factor < limit) {
			errx(EXIT_FAILURE,
			    "RVP_TRACE_SIZE_LIMIT (%s) too large", s);
		}

		rvp_trace_size_limit = limit * factor;
	}

	serializer_fd = rvp_trace_open();

	thread_lock(&oldmask);
	assert(thread_head->t_next == NULL);
	/* I don't use rvp_thread_flush_to_fd() here because I do not
	 * want to log a change of thread here under any circumstances.
	 */
	rvp_ring_flush_to_fd(&thread_head->t_ring, serializer_fd, NULL);
	serializer_lc = (rvp_lastctx_t){
		  .lc_tid = thread_head->t_id
		, .lc_idepth = 0
	};
	thread_unlock(&oldmask);

	sigfillset(&allmask);

	if ((rc = real_pthread_sigmask(SIG_BLOCK, &allmask, &oldmask)) != 0) {
		errx(EXIT_FAILURE, "%s.%d: pthread_sigmask: %s", __func__,
		    __LINE__, strerror(rc));
	}

	rc = real_pthread_create(&serializer, NULL, serialize, NULL);

	if (rc != 0) {
		errx(EXIT_FAILURE, "%s: pthread_create: %s", __func__,
		    strerror(rc));
	}

	if ((rc = real_pthread_sigmask(SIG_SETMASK, &oldmask, NULL)) != 0) {
		errx(EXIT_FAILURE, "%s.%d: pthread_sigmask: %s", __func__,
		    __LINE__, strerror(rc));
	}
}

void
rvp_thread_prefork_init(void)
{
	ESTABLISH_PTR_TO_REAL(int (*)(pthread_t, void **), pthread_join);
	ESTABLISH_PTR_TO_REAL(
	    int (*)(pthread_t *, const pthread_attr_t *, void *(*)(void *),
	        void *),
	    pthread_create);
	ESTABLISH_PTR_TO_REAL(void (*)(void *), pthread_exit);
}

#define	ASSERT_LOCK_FREENESS(_type)	do {			\
	typedef _type x_t;					\
	const volatile x_t x;					\
	if (!atomic_is_lock_free(&x)) {				\
		errx(EXIT_FAILURE,				\
		    "Quitting: atomic operations on type "	\
		    "`volatile " #_type "` are not lock free "	\
		    "as RV-Predict/C requires.");		\
	}							\
} while (false)

static void
rvp_assert_atomicity(void)
{
	ASSERT_LOCK_FREENESS(bool);
	ASSERT_LOCK_FREENESS(int);
	ASSERT_LOCK_FREENESS(uint64_t);
	ASSERT_LOCK_FREENESS(int64_t);
	ASSERT_LOCK_FREENESS(uint32_t);
	ASSERT_LOCK_FREENESS(int32_t);
	ASSERT_LOCK_FREENESS(uint16_t);
	ASSERT_LOCK_FREENESS(int16_t);
	ASSERT_LOCK_FREENESS(uint8_t);
	ASSERT_LOCK_FREENESS(int8_t);
	ASSERT_LOCK_FREENESS(rvp_ring_t *);
	ASSERT_LOCK_FREENESS(rvp_ring_state_t);
	ASSERT_LOCK_FREENESS(rvp_signal_t *);
}

static void
rvp_prefork_init(void)
{
	rvp_assert_atomicity();
	rvp_lock_prefork_init();	// needed by rvp_signal_init()
	rvp_signal_prefork_init();
	rvp_str_prefork_init();
	rvp_thread_prefork_init();
}

static void
rvp_postfork_init(void)
{
	rvp_signal_init();
	rvp_rings_init();

	if (pthread_key_create(&rvp_thread_key, NULL) != 0) 
		err(EXIT_FAILURE, "%s: pthread_key_create", __func__);
	if (sigfillset(&allmask) == -1)
		err(EXIT_FAILURE, "%s: sigfillset", __func__);
	if (pthread_mutex_init(&thread_mutex, NULL) != 0)
		err(EXIT_FAILURE, "%s: pthread_mutex_init", __func__);
	if (pthread_cond_init(&wakecond, NULL) != 0)
		err(EXIT_FAILURE, "%s: pthread_cond_init", __func__);
	if (pthread_cond_init(&stopcond, NULL) != 0)
		err(EXIT_FAILURE, "%s: pthread_cond_init", __func__);
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

/* If RVP_TRACE_ONLY == "yes", then every __rvpredict_init() call
 * begins and ends in the application process.
 *
 * If RVP_TRACE_ONLY != "yes", then one __rvpredict_init() call
 * begins in the supervisor's process---the process that
 * forks the application thread, waits for the application to finish,
 * and then runs the data-race predictor.  That call ends in the
 * application process.  Every other __rvpredict_init() call begins
 * and ends in the application process.
 */
void
__rvpredict_init(void)
{
	const char *s;

	rvp_trace_only = (s = getenv("RVP_TRACE_ONLY")) != NULL &&
	    strcasecmp(s, "yes") == 0;

	if (rvp_trace_only) {
		(void)pthread_once(&rvp_prefork_init_once, rvp_prefork_init);
	} else if (!forked) {
		forked = true;
		rvp_prefork_init();
		rvp_supervision_start();
	}
	(void)pthread_once(&rvp_postfork_init_once, rvp_postfork_init);
	rvp_static_intrs_reinit();
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
	sigset_t oldmask;
	thread_lock(&oldmask);

	if ((t->t_id = ++next_id) == 0) {
		thread_unlock(&oldmask);
		errx(EXIT_FAILURE, "%s: out of thread IDs", __func__);
	}

	rvp_update_nthreads(t->t_id);

	t->t_ring.r_tid = t->t_id;
	t->t_ring.r_idepth = 0;

	t->t_next = thread_head;
	thread_head = t;

	thread_unlock(&oldmask);

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
	sigset_t oldmask;

	thread_lock(&oldmask);

	for (tp = &thread_head; *tp != NULL && *tp != tgt; tp = &(*tp)->t_next)
		;

	if (*tp != NULL) {
		*tp = (*tp)->t_next;
		rc = 0;
	} else
		rc = ENOENT;

	thread_unlock(&oldmask);

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

	t->t_intr_ring = &t->t_ring;

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
	sigset_t oldmask;

	thread_lock(&oldmask);

	for (t = thread_head; t != NULL; t = t->t_next) {
		if (t->t_pthread == pthread)
			break;
	}

	thread_unlock(&oldmask);

	if (t == NULL)
		errno = ESRCH;

	return t;
}

void
rvp_wake_transmitter(void)
{
	sigset_t oldmask;

	thread_lock(&oldmask);
	rvp_wake_transmitter_locked();
	thread_unlock(&oldmask);
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
	r->r_lastpc = rvp_vec_and_op_to_deltop(0, RVP_OP_BEGIN);
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
