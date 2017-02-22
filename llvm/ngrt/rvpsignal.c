#include <inttypes.h>	/* for PRIu32 */
#include <signal.h>
#include <stdatomic.h>
#include <stdint.h>	/* for uint32_t */

#include "init.h"
#include "interpose.h"
#include "relay.h"
#include "rvpsignal.h"
#include "sigutil.h"
#include "trace.h"

REAL_DEFN(int, sigaction, int, const struct sigaction *, struct sigaction *);
REAL_DEFN(int, sigprocmask, int, const sigset_t *, sigset_t *);
REAL_DEFN(int, pthread_sigmask, int, const sigset_t *, sigset_t *);

typedef struct _rvp_signal_couplet {
	pthread_mutex_t	sc_lock;
	rvp_signal_t	sc_alternate[2];
} rvp_signal_couplet_t;

static rvp_signal_couplet_t *signal_storage = NULL;
static rvp_signal_t * _Atomic *signal_tbl = NULL;
static int nsignals = 0;
static int signals_origin = -1;

static uint32_t nsigblocksets = 0;
static rvp_sigblockset_t *sigblockset_head = NULL;
static pthread_mutex_t sigblockset_lock = PTHREAD_MUTEX_INITIALIZER;

static rvp_ring_t * _Atomic signal_rings = NULL;

static void
rvp_signal_table_init(void)
{
	sigset_t ss;
	rvp_signal_t * _Atomic *tbl;
	int i, lastsig, ninvalid = 0;
	rvp_signal_couplet_t *storage;

	sigfillset(&ss);
	for (lastsig = 0; ; lastsig++) { 
		if (sigismember(&ss, lastsig) == -1) {
			if (++ninvalid == 2)
				break;
		}
		if (signals_origin == -1 && sigismember(&ss, lastsig) == 1)
			signals_origin = lastsig;
	}

	if ((storage = calloc(sizeof(*storage), lastsig)) == NULL)
		err(EXIT_FAILURE, "%s.%d: calloc", __func__, __LINE__);

	if ((tbl = calloc(sizeof(*signal_tbl), lastsig)) == NULL)
		err(EXIT_FAILURE, "%s.%d: calloc", __func__, __LINE__);

	for (i = 0; i < lastsig; i++)
		real_pthread_mutex_init(&storage[i].sc_lock, NULL);

	signal_storage = storage;
	signal_tbl = tbl;
	nsignals = lastsig;

	if ((nsignals - signals_origin + 7) / 8 > sizeof(uint64_t)) {
		errx(EXIT_FAILURE, "%s: too many signals (%d - %d = %d) "
		     "for a trace to represent", __func__, nsignals,
		     signals_origin, nsignals - signals_origin);
	}
}

void
rvp_signal_init(void)
{
	ESTABLISH_PTR_TO_REAL(sigaction);
	ESTABLISH_PTR_TO_REAL(sigprocmask);
	ESTABLISH_PTR_TO_REAL(pthread_sigmask);
	rvp_signal_table_init();
}

static void
rvp_signal_lock(int signum, sigset_t *oldmask)
{
	sigset_t mask;

	if (sigemptyset(&mask) != 0 ||
	    sigaddset(&mask, signum) != 0 ||
	    real_pthread_sigmask(SIG_BLOCK, &mask, oldmask) != 0 ||
	    real_pthread_mutex_lock(&signal_storage[signum].sc_lock) != 0)
		errx(EXIT_FAILURE, "%s", __func__);
}

static void
rvp_signal_unlock(int signum, const sigset_t *oldmask)
{
	if (real_pthread_mutex_unlock(&signal_storage[signum].sc_lock) != 0 ||
	    real_pthread_sigmask(SIG_SETMASK, oldmask, NULL) != 0)
		errx(EXIT_FAILURE, "%s", __func__);
}

/* Caller must hold signal_storage[signum].sc_lock. */
static rvp_signal_t *
rvp_signal_alternate_lookup(int signum)
{
	return (signal_tbl[signum] == &signal_storage[signum].sc_alternate[0])
	    ? &signal_storage[signum].sc_alternate[1]
	    : &signal_storage[signum].sc_alternate[0];
}

static void
rvp_signal_select_alternate(int signum, rvp_signal_t *s)
{
	atomic_store_explicit(&signal_tbl[signum], s, memory_order_release);
}

rvp_signal_t *
rvp_signal_lookup(int signum)
{
	return atomic_load_explicit(&signal_tbl[signum], memory_order_acquire);
}

bool
rvp_signal_rings_flush_to_fd(int fd, rvp_lastctx_t *lc)
{
	rvp_ring_t *r;
	bool any_emptied = false;

	for (r = signal_rings; r != NULL; r = r->r_next) {
		rvp_ring_state_t state = RVP_RING_S_DIRTY;
		/* Take ownership of a dirty ring (-> in-use), flush,
		 * and change its state to clean, OR
		 * skip a clean ring, OR
		 * flush an in-use ring.
		 */
		if (atomic_compare_exchange_strong(&r->r_state, &state,
		    RVP_RING_S_INUSE)) {
			any_emptied |= rvp_ring_flush_to_fd(r, fd, lc);
			r->r_state = RVP_RING_S_CLEAN;
		} else if (state == RVP_RING_S_INUSE) {
			any_emptied |= rvp_ring_flush_to_fd(r, fd, lc);
		}
	}
	return any_emptied;
}

static rvp_ring_t *
rvp_signal_ring_get_scan(rvp_thread_t *t, uint32_t noutst)
{
	rvp_ring_t *r;
	const uint32_t tid = t->t_id;

	assert(noutst > 0);

	/* XXX In principle, between reading the list of
	 * signal rings, and checking a ring's state, 
	 * the ring can be freed.  In practice, I never free a ring.
	 * In the future, I probably should use passive
	 * synchronization between scanning and freeing.
	 */
	for (r = signal_rings; r != NULL; r = r->r_next) {
		rvp_ring_state_t dirty = RVP_RING_S_DIRTY;

		if (!atomic_compare_exchange_weak(&r->r_state, &dirty,
						 RVP_RING_S_INUSE))
			continue;
		if (r->r_tid == tid && r->r_nintr_outst == noutst)
			return r;
		/* Not a match, put it back. */
		atomic_store_explicit(&r->r_state, RVP_RING_S_DIRTY,
		    memory_order_relaxed);
	}

	for (r = signal_rings; r != NULL; r = r->r_next) {
		rvp_ring_state_t clean = RVP_RING_S_CLEAN;

		if (atomic_compare_exchange_weak(&r->r_state, &clean,
						 RVP_RING_S_INUSE)) {
			r->r_tid = tid;
			r->r_nintr_outst = noutst;
			break;
		}
	}

	return r;
}

static void
rvp_wake_replenisher(void)
{
	rvp_wake_relay();
}

/* Calls to rvp_signal_rings_replenish() are synchronized by
 * the serialization thread, for now.
 */
void
rvp_signal_rings_replenish(void)
{
	int nallocated;

	for (nallocated = 0; nallocated < 5; nallocated++) {
		rvp_ring_t *r = calloc(sizeof(*r), 1);
		if (r == NULL && nallocated == 0)
			err(EXIT_FAILURE, "%s: calloc", __func__);
		else if (r == NULL)
			break;

		rvp_ring_stdinit(r);
		r->r_state = RVP_RING_S_CLEAN;
		r->r_next = signal_rings;

		while (!atomic_compare_exchange_weak(&signal_rings,
		    &r->r_next, r))
			;	// do nothing
	}
}

rvp_ring_t *
rvp_signal_ring_get(rvp_thread_t *t, uint32_t noutst)
{
	rvp_ring_t *r;

	while ((r = rvp_signal_ring_get_scan(t, noutst)) == NULL) {
		// TBD backoff
		rvp_wake_replenisher();
	}
	return r;
}

void
rvp_signal_ring_put(rvp_thread_t *t, rvp_ring_t *r)
{
	rvp_ring_state_t inuse = RVP_RING_S_INUSE;

	rvp_ring_state_t nstate = rvp_ring_nfull(r) != 0
	    ? RVP_RING_S_DIRTY
	    : RVP_RING_S_CLEAN;

	if (!atomic_compare_exchange_strong(&r->r_state, &inuse, nstate))
		abort();
}

static void
handler_wrapper(int signum, siginfo_t *info, void *ctx)
{
	/* XXX rvp_thread_for_curthr() calls pthread_getspecific(), which
	 * is not guaranteed to be async signal-safe.  However, it is
	 * known to be safe on Linux, and it is probably safe on many other
	 * operating systems.  Check: the C11 equivalent is async signal-
	 * safe, isn't it?
	 */
	rvp_thread_t *t = rvp_thread_for_curthr();
	rvp_signal_t *s = rvp_signal_lookup(signum);
	uint32_t noutst = atomic_fetch_add_explicit(&t->t_nintr_outst, 1,
	    memory_order_acquire) + 1;
	rvp_ring_t *r = rvp_signal_ring_get(t, noutst);
	rvp_ring_t *oldr = atomic_exchange(&t->t_intr_ring, r);
	rvp_buf_t b = RVP_BUF_INITIALIZER;

	/* When the serializer reaches this ring, it will emit a
	 * change of thread and change in outstanding interrupts, if
	 * necessary, before emitting the events on the ring.
	 *
	 * We only have to record the signal handler that was entered, here.
	 */
	rvp_buf_put_addr(&b, rvp_vec_and_op_to_deltop(0, RVP_OP_ENTERSIG));
	rvp_buf_put(&b, signum);
	rvp_ring_put_buf(r, b);

	if (s->s_handler != NULL)
		(*s->s_handler)(signum);
	else
		(*s->s_sigaction)(signum, info, ctx);

	b = RVP_BUF_INITIALIZER;

	atomic_store(&t->t_intr_ring, oldr);
	noutst = atomic_fetch_sub_explicit(&t->t_nintr_outst, 1,
	    memory_order_release) - 1;
	rvp_buf_put_addr(&b, rvp_vec_and_op_to_deltop(0, RVP_OP_EXITSIG));
	rvp_ring_put_buf(r, b);
	rvp_signal_ring_put(t, r);
}

static sigset_t *
mask_to_sigset(uint64_t mask, sigset_t *set)
{
	int rc, signum;

	if (sigemptyset(set) != 0)
		err(EXIT_FAILURE, "%s: sigemptyset", __func__);

	for (signum = signals_origin; signum < nsignals; signum++) {
		uint64_t testbit = 1U << (signum - signals_origin);
		if ((mask & testbit) != 0 && (rc = sigaddset(set, signum)) != 0)
			err(EXIT_FAILURE, "%s: sigaddset", __func__);
	}
	return set;
}

static uint64_t
sigset_to_mask(const sigset_t *set)
{
	int signum;
	uint64_t mask = 0;

	for (signum = signals_origin; signum < nsignals; signum++) {
		if (sigismember(set, signum) == 1)
			mask |= 1U << (signum - signals_origin);
	}
	return mask;
}

uint32_t
rvp_sigblocksets_emit(int fd, uint32_t lastn)
{
	rvp_sigblockset_t *bs;
	uint32_t nsets;

	real_pthread_mutex_lock(&sigblockset_lock);

	for (bs = sigblockset_head; bs != NULL; bs = bs->bs_next) {
		if (bs->bs_number < lastn)
			continue;
		rvp_sigmaskmemo_t map = (rvp_sigmaskmemo_t){
			  .deltop = (uintptr_t)rvp_vec_and_op_to_deltop(0,
			      RVP_OP_SIGMASKMEMO)
			, .mask = sigset_to_mask(&bs->bs_sigset)
			, .origin = signals_origin
			, .masknum = bs->bs_number
		};
		if (write(fd, &map, sizeof(map)) == -1)
			err(EXIT_FAILURE, "%s: write", __func__);
	}

	nsets = nsigblocksets;

	real_pthread_mutex_unlock(&sigblockset_lock);

	return nsets;
}

rvp_sigblockset_t *
intern_sigset(const sigset_t *s)
{
	rvp_sigblockset_t *bs;

	real_pthread_mutex_lock(&sigblockset_lock);
	for (bs = sigblockset_head; bs != NULL; bs = bs->bs_next) {
		if (sigeqset(&bs->bs_sigset, s))
			break;
	}

	if (bs == NULL) {
		if ((bs = malloc(sizeof(*bs))) == NULL)
			err(EXIT_FAILURE, "%s: malloc", __func__);
		bs->bs_number = nsigblocksets++;
		bs->bs_sigset = *s;
		bs->bs_next = sigblockset_head;
		sigblockset_head = bs;
	}

	real_pthread_mutex_unlock(&sigblockset_lock);

	return bs;
}

static void
rvp_trace_sigest(int signum, const void *handler, uint32_t masknum,
    const void *return_address)
{
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;

	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, return_address, RVP_OP_SIGEST);
	rvp_buf_put_addr(&b, handler);
	rvp_buf_put(&b, signum);
	rvp_buf_put(&b, masknum);
	rvp_ring_put_buf(r, b);
	rvp_ring_request_service(r);
}

static void
rvp_trace_sigdis(int signum, const void *return_address)
{
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;

	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, return_address, RVP_OP_SIGDIS);
	rvp_buf_put(&b, signum);
	rvp_ring_put_buf(r, b);
	rvp_ring_request_service(r);
}

static void
rvp_trace_masksigs(uint32_t masknum, const void *return_address)
{
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;

	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, return_address,
	    RVP_OP_MASKSIGS);
	rvp_buf_put(&b, masknum);
	rvp_ring_put_buf(r, b);
	rvp_ring_request_service(r);
}

static void
rvp_thread_finish_sigmask(rvp_thread_t *t, int how, uint64_t mask)
{
	sigset_t set;
	rvp_sigblockset_t *bs;

	switch (how) {
	case SIG_BLOCK:
		t->t_intrmask |= mask;
		break;
	case SIG_UNBLOCK:
		t->t_intrmask &= ~mask;
		break;
	case SIG_SETMASK:
		t->t_intrmask = mask;
		break;
	}
	bs = intern_sigset(mask_to_sigset(t->t_intrmask, &set));
	rvp_trace_masksigs(bs->bs_number, __builtin_return_address(0));
}

int
__rvpredict_pthread_sigmask(int how, const sigset_t *set, sigset_t *oldset)
{
	rvp_thread_t *t = rvp_thread_for_curthr();
	int rc;

	if ((rc = real_pthread_sigmask(how, set, oldset)) != 0)
		return rc;

	rvp_thread_finish_sigmask(t, how, sigset_to_mask(set));

	return 0;
}

int
__rvpredict_sigprocmask(int how, const sigset_t *set, sigset_t *oldset)
{
	rvp_thread_t *t = rvp_thread_for_curthr();
	int rc;

	if ((rc = real_sigprocmask(how, set, oldset)) != 0)
		return rc;

	rvp_thread_finish_sigmask(t, how, sigset_to_mask(set));

	return 0;
}

int
__rvpredict_sigaction(int signum, const struct sigaction *act0,
    struct sigaction *oact)
{
	rvp_signal_t *s;
	sigset_t mask, savedmask;
	struct sigaction act_copy;
	const struct sigaction *act = act0;
	int rc;

	if (act == NULL)
		goto out;

	rvp_signal_lock(signum, &savedmask);

	if ((s = rvp_signal_alternate_lookup(signum)) == NULL)
		err(EXIT_FAILURE, "%s: malloc", __func__);

	if ((act->sa_flags & SA_SIGINFO) != 0) {
		s->s_sigaction = act->sa_sigaction;
	} else {
		void (*handler)(int);

		handler = s->s_handler = act->sa_handler;

		if (handler == SIG_IGN || handler == SIG_DFL) {
			rvp_trace_sigdis(signum, __builtin_return_address(0));
			goto out;
		}
	}

	mask = act->sa_mask;

	// add signum to the set unless this signal can preempt itself 
	if ((act->sa_flags & SA_NODEFER) == 0)
		sigaddset(&mask, signum);
	
	s->s_blockset = intern_sigset(&mask);

	rvp_trace_sigest(signum, s->s_handler, s->s_blockset->bs_number,
	    __builtin_return_address(0));

	act_copy = *act;
	act_copy.sa_flags |= SA_SIGINFO;
	act_copy.sa_sigaction = handler_wrapper;
	act = &act_copy;

	rvp_signal_select_alternate(signum, s);

out:
	rc = real_sigaction(signum, act, oact);
	if (oact == NULL || (oact->sa_flags & SA_SIGINFO) == 0 ||
	    oact->sa_sigaction != handler_wrapper) {
		;	// old sigaction not requested, or wrapper not installed
	} else if (s->s_handler != NULL) {
		oact->sa_flags &= ~SA_SIGINFO;
		oact->sa_handler = s->s_handler;
	} else {
		assert(s->s_sigaction != NULL);
		oact->sa_flags |= SA_SIGINFO;
		oact->sa_sigaction = s->s_sigaction;
	}
	rvp_signal_unlock(signum, &savedmask);
	return rc;
}

INTERPOSE(int, sigprocmask, int, const sigset_t *, sigset_t *);
INTERPOSE(int, pthread_sigmask, int, const sigset_t *, sigset_t *);
INTERPOSE(int, sigaction, int, const struct sigaction *, struct sigaction *);
