#include <errno.h>
#include <inttypes.h>	/* for PRIu32 */
#include <signal.h>
#include <stdatomic.h>
#include <stdint.h>	/* for uint32_t */

#include "backoff.h"
#include "init.h"
#include "interpose.h"
#include "relay.h"
#include "rvpsignal.h"
#include "sigutil.h"
#include "text.h"
#include "trace.h"

#if defined(SIGINFO)
#define	RVP_INFO_SIGNUM	SIGINFO
#else
#define	RVP_INFO_SIGNUM	SIGPWR
#endif

REAL_DEFN(int, sigaction, int, const struct sigaction *, struct sigaction *);
REAL_DEFN(int, sigprocmask, int, const sigset_t *, sigset_t *);
REAL_DEFN(int, pthread_sigmask, int, const sigset_t *, sigset_t *);
REAL_DEFN(int, sigsuspend, const sigset_t *);

typedef int (*rvp_change_sigmask_t)(int, const sigset_t *, sigset_t *);

typedef struct _rvp_signal_couplet {
	pthread_mutex_t	sc_lock;
	rvp_signal_t	sc_alternate[2];
} rvp_signal_couplet_t;

uint64_t rvp_unmaskable;

static rvp_signal_couplet_t *signal_storage = NULL;
static rvp_signal_t * _Atomic *signal_tbl = NULL;
static int nsignals = 0;
static int signals_origin = -1;

static _Atomic int nrings_needed = 0;

static _Atomic uint32_t nsigblocksets = 0;
static rvp_sigblockset_t * volatile _Atomic sigblockset_head = NULL;
static rvp_sigblockset_t * volatile _Atomic sigblockset_freehead = NULL;

static rvp_ring_t * volatile _Atomic signal_rings = NULL;

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

	if ((nsignals - signals_origin + NBBY - 1) / NBBY >
	    (int)sizeof(uint64_t)) {
		errx(EXIT_FAILURE, "%s: too many signals (%d - %d = %d) "
		     "for a trace to represent", __func__, nsignals,
		     signals_origin, nsignals - signals_origin);
	}
}

void
rvp_signal_prefork_init(void)
{
	ESTABLISH_PTR_TO_REAL(
	    int (*)(int, const struct sigaction *, struct sigaction *),
	    sigaction);
	ESTABLISH_PTR_TO_REAL(
	    int (*)(int, const sigset_t *, sigset_t *),
	    sigprocmask);
	ESTABLISH_PTR_TO_REAL(
	    int (*)(int, const sigset_t *, sigset_t *),
	    pthread_sigmask);
	ESTABLISH_PTR_TO_REAL(
	    int (*)(const sigset_t *),
	    sigsuspend);
}

static void
rvp_siginfo_handler(int signum __unused)
{
	rvp_info_dump_request();
}

void
rvp_signal_init(void)
{
	rvp_signal_table_init();
	struct sigaction osa, sa;
	sigset_t unmaskable_set;

	if (sigemptyset(&unmaskable_set) != 0)
		err(EXIT_FAILURE, "%s.%d: sigemptyset", __func__, __LINE__);

	if (sigaddset(&unmaskable_set, SIGSTOP) != 0)
		err(EXIT_FAILURE, "%s.%d: sigaddset", __func__, __LINE__);

	if (sigaddset(&unmaskable_set, SIGKILL) != 0)
		err(EXIT_FAILURE, "%s.%d: sigaddset", __func__, __LINE__);

#if 1
	rvp_unmaskable = sigset_to_mask(&unmaskable_set);
#else
	rvp_unmaskable = 0;
#endif

	memset(&sa, 0, sizeof(sa));
	if (sigemptyset(&sa.sa_mask) != 0)
		err(EXIT_FAILURE, "%s.%d: sigemptyset", __func__, __LINE__);
	sa.sa_handler = rvp_siginfo_handler;
	if (real_sigaction(RVP_INFO_SIGNUM, &sa, &osa) == -1)
		err(EXIT_FAILURE, "%s: sigaction", __func__);

	if ((osa.sa_flags & SA_SIGINFO) != 0 || osa.sa_handler != SIG_DFL) {
		errx(EXIT_FAILURE,
		    "%s: signal %d unexpectedly had a non-default action",
		    __func__, RVP_INFO_SIGNUM);
	}
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

#if 0
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
#endif

static rvp_ring_t *
rvp_signal_ring_acquire_scan(rvp_thread_t *t, uint32_t idepth)
{
	rvp_ring_t *r;
	const uint32_t tid = t->t_id;

	assert(idepth > 0);

	/* XXX In principle, between reading the list of
	 * signal rings, and checking a ring's state, 
	 * the ring can be freed.  In practice, I never free a ring.
	 * In the future, I probably should use passive
	 * synchronization between scanning and freeing.
	 */
	for (r = signal_rings; r != NULL; r = r->r_next) {
		rvp_ring_state_t dirty = RVP_RING_S_DIRTY;

		if (!atomic_compare_exchange_strong(&r->r_state, &dirty,
						 RVP_RING_S_INUSE))
			continue;
		if (r->r_tid == tid && r->r_idepth == idepth) {
			// We may reuse tid's, so make sure t_stats is
			// up-to-date.
			if (r->r_stats != &t->t_stats)
				r->r_stats = &t->t_stats;
			return r;
		}
		/* Not a match, put it back. */
		atomic_store_explicit(&r->r_state, RVP_RING_S_DIRTY,
		    memory_order_relaxed);
	}

	for (r = signal_rings; r != NULL; r = r->r_next) {
		rvp_ring_state_t clean = RVP_RING_S_CLEAN;

		if (atomic_compare_exchange_strong(&r->r_state, &clean,
						 RVP_RING_S_INUSE)) {
			r->r_tid = tid;
			r->r_idepth = idepth;
			r->r_stats = &t->t_stats;
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

/* If nrings_needed == 0, do nothing.
 *
 * If nrings_needed != 0, then ensure that there are nrings_needed + 1 clean
 * rings, and reset nrings_needed to 0.
 *
 * Calls to rvp_signal_rings_replenish() are synchronized by
 * the serialization thread.
 *
 */
void
rvp_signal_rings_replenish(void)
{
	int nclean = 0;
	const int nneeded = atomic_exchange(&nrings_needed, 0);
	int nallocated;
	rvp_ring_t *r;
	rvp_backoff_t b;

	if (nneeded == 0)
		return;

	for (r = signal_rings;
	     nclean <= nneeded && r != NULL;
	     r = r->r_next) {
		if (r->r_state == RVP_RING_S_CLEAN)
			nclean++;
	}

	for (nallocated = nclean; nallocated <= nneeded; nallocated++) {
		rvp_ring_t *r = calloc(sizeof(*r), 1);
		if (r == NULL && nallocated == 0)
			err(EXIT_FAILURE, "%s: calloc", __func__);
		else if (r == NULL)
			break;

		rvp_ring_stdinit(r);
		r->r_state = RVP_RING_S_CLEAN;
		r->r_next = signal_rings;

		for (rvp_backoff_first(&b);
		     !atomic_compare_exchange_weak(&signal_rings,
		         &r->r_next, r);
		     rvp_backoff_next(&b))
			rvp_backoff_pause(&b);
	}
}

static rvp_ring_t *
rvp_signal_ring_acquire(rvp_thread_t *t, uint32_t idepth)
{
	rvp_ring_t *r;

	if ((r = rvp_signal_ring_acquire_scan(t, idepth)) != NULL)
		return r;

	nrings_needed++;

	do {
		rvp_wake_replenisher();
	} while ((r = rvp_signal_ring_acquire_scan(t, idepth)) == NULL);

	return r;
}

void
rvp_signal_ring_put(rvp_thread_t *t __unused, rvp_ring_t *r)
{
	rvp_ring_state_t inuse = RVP_RING_S_INUSE;

	rvp_ring_state_t nstate = rvp_ring_is_dirty(r)
	    ? RVP_RING_S_DIRTY
	    : RVP_RING_S_CLEAN;

	if (!atomic_compare_exchange_strong(&r->r_state, &inuse, nstate))
		abort();
}

static void
__rvpredict_handler_wrapper(int signum, siginfo_t *info, void *ctx)
{
	/* XXX rvp_thread_for_curthr() calls pthread_getspecific(), which
	 * is not guaranteed to be async signal-safe.  However, it is
	 * known to be safe on Linux, and it is probably safe on many other
	 * operating systems.  Check: the C11 equivalent is async signal-
	 * safe, isn't it?
	 */
	rvp_thread_t *t = rvp_thread_for_curthr();
	rvp_signal_t *s = rvp_signal_lookup(signum);
	uint64_t omask = t->t_intrmask;
	uint32_t idepth = atomic_fetch_add_explicit(&t->t_idepth, 1,
	    memory_order_acquire);
	rvp_ring_t *r = rvp_signal_ring_acquire(t, idepth + 1);
	rvp_ring_t *oldr = atomic_exchange(&t->t_intr_ring, r);
	rvp_interruption_t *it = rvp_ring_put_interruption(oldr, r,
	    r->r_producer - r->r_items);
	rvp_buf_t b = RVP_BUF_INITIALIZER;

	t->t_intrmask = omask | sigset_to_mask(&s->s_blockset->bs_sigset);
	r->r_lgen = oldr->r_lgen;

	/* When the serializer reaches this ring, it will emit a
	 * change of PC, a change of thread, and a change in outstanding
	 * interrupts, if necessary, before emitting the events on the ring.
	 */
	r->r_lastpc = rvp_vec_and_op_to_deltop(0, RVP_OP_BEGIN);
	rvp_buf_put_voidptr(&b, rvp_vec_and_op_to_deltop(0, RVP_OP_ENTERSIG));
	rvp_buf_put_voidptr(&b,
	    (s->s_handler != NULL)
	        ? (const void *)s->s_handler
		: (const void *)s->s_sigaction);
	rvp_buf_put_u64(&b, r->r_lgen);
	rvp_buf_put(&b, signum);
	rvp_ring_put_buf(r, b);

	if (s->s_handler != NULL)
		(*s->s_handler)(signum);
	else
		(*s->s_sigaction)(signum, info, ctx);

	b = RVP_BUF_INITIALIZER;

	/* I copy the local generation back so that the interrupted sequence
	 * does not have to resynchronize with ggen.  It's ok to copy back
	 * the local generation, here: this ring's local generation is not
	 * going to increase any more in this signal, and oldr is not
	 * visible until I copy it back to t_intr_ring. 
	 */
	oldr->r_lgen = r->r_lgen;

	atomic_store_explicit(&t->t_intr_ring, oldr, memory_order_release);
	/* At this juncture, a new signal could start with parent
	 * t_intr_ring and a greater idepth than this thread's, but
	 * that's ok, it will just get a new ring.
	 */
	rvp_buf_put_voidptr(&b, rvp_vec_and_op_to_deltop(0, RVP_OP_EXITSIG));
	rvp_ring_put_buf(r, b);
	rvp_interruption_close(it, r->r_producer - r->r_items);
	rvp_signal_ring_put(t, r);
	t->t_intrmask = omask;
	/* I wait until after the ring is relinquished to restore the old
	 * idepth so that the relinquished ring is available for reuse.
	 */
	atomic_store_explicit(&t->t_idepth, idepth, memory_order_release);
}

sigset_t *
mask_to_sigset(uint64_t mask, sigset_t *set)
{
	int rc, signum;

	if (sigemptyset(set) != 0)
		err(EXIT_FAILURE, "%s: sigemptyset", __func__);

	for (signum = signals_origin; signum < nsignals; signum++) {
		uint64_t testbit = (uint64_t)1 << (signum - signals_origin);
		if ((mask & testbit) != 0 && (rc = sigaddset(set, signum)) != 0)
			err(EXIT_FAILURE, "%s: sigaddset", __func__);
	}
	return set;
}

uint64_t
sigset_to_mask(const sigset_t *set)
{
	int signum;
	uint64_t mask = 0;

	for (signum = signals_origin; signum < nsignals; signum++) {
		if (sigismember(set, signum) == 1)
			mask |= (uint64_t)1 << (signum - signals_origin);
	}
	return mask;
}

rvp_sigblockset_t *
rvp_sigblocksets_emit(int fd, rvp_sigblockset_t *last_head)
{
	rvp_sigblockset_t *bs, *head;

	head = sigblockset_head;

	// newest sigblocksets are always at the head of the list, so
	// emit them until we see the previous head
	for (bs = head; bs != NULL; bs = bs->bs_next) {
		if (bs == last_head)
			break;
		rvp_sigmaskmemo_t map = (rvp_sigmaskmemo_t){
			  .deltop = (rvp_addr_t)rvp_vec_and_op_to_deltop(0,
			      RVP_OP_SIGMASKMEMO)
			, .mask = sigset_to_mask(&bs->bs_sigset)
			, .origin = signals_origin
			, .masknum = bs->bs_number
		};
		if (write(fd, &map, sizeof(map)) == -1)
			err(EXIT_FAILURE, "%s: write", __func__);
		bs->bs_serialized = true;
	}

	return head;
}

/* Signal handlers call intern_sigset(), so it has to be lockless, and
 * it cannot call malloc(3) to get a new sigblockset_t.
 *
 * General idea: keep a free list, which is refreshed by the serialization
 * thread.  If the free list is empty, and we're in interrupt context,
 * then signal the relay to wake the serialization thread.
 *
 * sigblockset_head is an atomic pointer.  intern_sigset() performs these
 * actions in a loop:
 * 1) `head = sigblockset_head;`
 * 2) scan from `head` until end for a match `bs` for `s`
 * 3) finding a match, return it.
 * 4) finding no match, take an item off of the free list and
 *    fill it.
 * 5) use a compare-and-set to replace `head` with the new item
 *    at `sigblockset_head`; failing that, use a compare-and-set
 *    to put back the new item and start over at (1).
 * 6) return the new item
 */
static rvp_sigblockset_t *
intern_sigset_try_once(const sigset_t *s)
{
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_sigblockset_t *head, *bs;
	rvp_backoff_t b;

	head = sigblockset_head;
	for (bs = head; bs != NULL; bs = bs->bs_next) {
		if (sigeqset(&bs->bs_sigset, s)) {
			for (rvp_backoff_first(&b);
			     !bs->bs_serialized;
			     rvp_backoff_next(&b))
				rvp_backoff_pause(&b);
			return bs;
		}
	}

	assert(bs == NULL);

	for (rvp_backoff_first(&b); ; rvp_backoff_next(&b)) {
		if (bs == NULL && (bs = sigblockset_freehead) == NULL) {
			// consider replacing with rvp_ring_request_service()
			// or the equivalent
			rvp_wake_replenisher();
			rvp_backoff_pause(&b);
			continue;
		}
		if (atomic_compare_exchange_strong(&sigblockset_freehead, &bs,
		    bs->bs_next))
			break;
	}
	bs->bs_next = head;
	bs->bs_sigset = *s;
	bs->bs_serialized = false;
	if (atomic_compare_exchange_strong(&sigblockset_head, &head, bs)) {
		// new memo: kick serializer so that it's available soon
		rvp_ring_request_service(r);
		return bs;
	}

	/* The sigblockset list changed, so we need to re-scan to see if
	 * the new blockset is necessary.  Put this one back on the free list
	 * in the mean time.  
	 */
	bs->bs_next = sigblockset_freehead;
	for (rvp_backoff_first(&b);
	       !atomic_compare_exchange_strong(&sigblockset_freehead,
	           &bs->bs_next, bs);
	       rvp_backoff_next(&b))
		rvp_backoff_pause(&b);

	return NULL;
}

static void
rvp_sigblocksets_replenish_once(void)
{
	rvp_sigblockset_t *bs;
	rvp_backoff_t b;

	if ((bs = malloc(sizeof(*bs))) == NULL)
		err(EXIT_FAILURE, "%s: malloc", __func__);
	bs->bs_number = nsigblocksets++;
	bs->bs_next = sigblockset_freehead;
	for (rvp_backoff_first(&b);
	     !atomic_compare_exchange_strong(&sigblockset_freehead,
	         &bs->bs_next, bs);
	     rvp_backoff_next(&b))
		rvp_backoff_pause(&b);
}

void
rvp_sigblocksets_replenish(void)
{
	int i;

	if (sigblockset_freehead != NULL)
		return;

	for (i = 0; i < 5; i++)
		rvp_sigblocksets_replenish_once();
}

rvp_sigblockset_t *
intern_sigset(const sigset_t *s)
{
	rvp_sigblockset_t *bs;
	while ((bs = intern_sigset_try_once(s)) == NULL)
		;	// do nothing
	return bs;
}

static void
rvp_trace_sigest(int signum, rvp_addr_t handler, uint32_t masknum,
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
rvp_trace_getsetmask(uint32_t omasknum, uint32_t masknum,
    const void *return_address)
{
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;

	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, return_address,
	    RVP_OP_SIGGETSETMASK);
	rvp_buf_put(&b, omasknum);
	rvp_buf_put(&b, masknum);
	rvp_ring_put_buf(r, b);
	rvp_ring_request_service(r);
}

static void
rvp_trace_mask(rvp_op_t op, uint32_t masknum, const void *return_address)
{
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;

	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, return_address, op);
	rvp_buf_put(&b, masknum);
	rvp_ring_put_buf(r, b);
	rvp_ring_request_service(r);
}

static void
rvp_thread_trace_getsetmask(rvp_thread_t *t __unused,
    uint64_t omask, uint64_t mask, const void *retaddr)
{
	sigset_t oset, set;
	rvp_sigblockset_t *obs, *bs;

	obs = intern_sigset(mask_to_sigset(omask, &oset));
	bs = intern_sigset(mask_to_sigset(mask, &set));
	rvp_trace_getsetmask(obs->bs_number, bs->bs_number, retaddr);
}

static void
rvp_thread_trace_setmask(rvp_thread_t *t __unused, int how, uint64_t mask,
    const void *retaddr)
{
	rvp_op_t op;
	sigset_t set;
	rvp_sigblockset_t *bs;

	if (how == SIG_SETMASK)
		op = RVP_OP_SIGSETMASK;
	else if (how == SIG_BLOCK)
		op = RVP_OP_SIGBLOCK;
	else if (how == SIG_UNBLOCK)
		op = RVP_OP_SIGUNBLOCK;
	else 
		errx(EXIT_FAILURE, "%s: unknown `how`, %d", __func__, how);

	bs = intern_sigset(mask_to_sigset(mask, &set));
	rvp_trace_mask(op, bs->bs_number, retaddr);
}

static void
rvp_thread_trace_getmask(rvp_thread_t *t __unused,
    uint64_t omask, const void *retaddr)
{
	sigset_t set;
	rvp_sigblockset_t *bs;

	bs = intern_sigset(mask_to_sigset(omask, &set));
	rvp_trace_mask(RVP_OP_SIGGETMASK, bs->bs_number, retaddr);
}

#if 0
static inline void
trace_sigmask_op(const void *retaddr, int how)
{
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;
	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, retaddr, op);
	rvp_buf_put_voidptr(&b, mtx);

	rvp_ring_put_buf(r, b);
}
#endif

static int
rvp_change_sigmask(rvp_change_sigmask_t changefn, const void *retaddr, int how,
    const sigset_t *set, sigset_t *oldset)
{
	rvp_thread_t *t = rvp_thread_for_curthr();
	uint64_t maskchg, nmask, omask;
	int rc;

	/* TBD trace a read from `set` and, if `oldset` is not NULL,
	 * a write to it.
	 */

	/* TBD optionally change generation number before loading
	 * and after storing the signal mask
	 *
	 * TBD freeze generation number across a mask get-and-set.
	 */
#if 0
	uint64_t gen;

	if (how == SIG_BLOCK)
		rvp_buf_trace_load_cog(&b, &r->r_lgen);
	else
		gen = rvp_ggen_before_store();
#endif

	omask = t->t_intrmask;

	maskchg = sigset_to_mask(set);

	switch (how) {
	case SIG_BLOCK:
		nmask = omask | maskchg;
		break;
	case SIG_UNBLOCK:
		nmask = omask & ~maskchg;
		break;
	case SIG_SETMASK:
		nmask = maskchg;
		break;
	}

	if (set == NULL)
		;
	else if (oldset != NULL)
		rvp_thread_trace_getsetmask(t, omask, nmask, retaddr);
	else {
                /* It's ok we're passing `maskchg` instead of
                 * the absolute mask, `t->t_intrmask`, because
		 * _trace_setmask() expects it.  Note that we pass
		 * the operation, `how`, too.
		 */
		rvp_thread_trace_setmask(t, how, maskchg, retaddr);
	}

	if ((rc = (*changefn)(how, set, oldset)) != 0)
		return rc;

	t->t_intrmask = nmask & ~rvp_unmaskable;

	if (oldset != NULL) {
		const uint64_t actual_omask = sigset_to_mask(oldset);

                /* The mask that was in `t->t_intrmask` when we entered
                 * this function should precisely match the mask
                 * returned by `changefn` (`real_pthread_sigmask`),
                 * above, *except* in a signal.
		 *
                 * In a signal, the mask that is actually in effect will
                 * block at least all of the signals blocked in
		 * `t->t_intrmask`, however, it may block more.
		 */
		if (t->t_intr_ring == &t->t_ring &&
		    actual_omask != 0 && omask != actual_omask)
			abort();
	}

	if (set == NULL && oldset != NULL)
		rvp_thread_trace_getmask(t, omask, retaddr);

#if 0
	if (how != SIG_BLOCK)
		rvp_buf_trace_cog(&b, &r->r_lgen, gen);
#endif
	return 0;
}

int
__rvpredict_sigsuspend(const sigset_t *mask)
{
	rvp_thread_t *t = rvp_thread_for_curthr();
	const void *retaddr = __builtin_return_address(0);
	uint64_t omask = t->t_intrmask, nmask = sigset_to_mask(mask);
	rvp_thread_trace_getsetmask(t, omask, nmask, retaddr);
	t->t_intrmask = nmask & ~rvp_unmaskable;
	/* TBD record read of `mask` */
	const int rc = real_sigsuspend(mask);
	t->t_intrmask = omask & ~rvp_unmaskable;
	const int errno_copy = errno;
	rvp_thread_trace_setmask(t, SIG_SETMASK, omask, retaddr);
	errno = errno_copy;
	return rc;
}

int
__rvpredict_pthread_sigmask(int how, const sigset_t *set, sigset_t *oldset)
{
	const void *retaddr = __builtin_return_address(0);

	return rvp_change_sigmask(real_pthread_sigmask,
	    retaddr, how, set, oldset);
}

int
__rvpredict_sigprocmask(int how, const sigset_t *set, sigset_t *oldset)
{
	return rvp_change_sigmask(real_sigprocmask,
	    __builtin_return_address(0), how, set, oldset);
}

/* XXX sigaction(2) is async-signal-safe, so we have to
 * XXX take care to avoid async-signal-UNSAFE functions in
 * XXX our implementation.  rvp_signal_lock() calls
 * XXX pthread_mutex_lock(), which is not async-signal-safe.
 * XXX So I need to fix that someday. 
 */
int
__rvpredict_sigaction(int signum, const struct sigaction *act0,
    struct sigaction *oact)
{
	rvp_signal_t *s;
	sigset_t mask, savedmask;
	struct sigaction act_copy;
	const struct sigaction *act = act0;
	int rc;
	rvp_addr_t handler;

	if (act == NULL)
		goto out;

	/* XXX sigaction(2) is supposed to be async-signal-safe, so instead of
	 * acquiring a lock here, I should use a signal-safe approach to
	 * allocating & establishing a new rvp_signal_t, possibly copying
	 * from intern_sigset().
	 */
	rvp_signal_lock(signum, &savedmask);

	if ((s = rvp_signal_alternate_lookup(signum)) == NULL)
		err(EXIT_FAILURE, "%s: rvp_signal_alternate_lookup", __func__);

	if ((act->sa_flags & SA_SIGINFO) != 0) {
		handler = (rvp_addr_t)(s->s_sigaction = act->sa_sigaction);
	} else {
		handler = (rvp_addr_t)(s->s_handler = act->sa_handler);

		if (s->s_handler == SIG_IGN || s->s_handler == SIG_DFL) {
			rvp_trace_sigdis(signum, __builtin_return_address(0));
			goto out;
		}
	}

	mask = act->sa_mask;

	// add signum to the set unless this signal can preempt itself 
	if ((act->sa_flags & SA_NODEFER) == 0)
		sigaddset(&mask, signum);
	
	s->s_blockset = intern_sigset(&mask);

	rvp_trace_sigest(signum, handler, s->s_blockset->bs_number,
	    __builtin_return_address(0));

	act_copy = *act;
	act_copy.sa_flags |= SA_SIGINFO;
	act_copy.sa_sigaction = __rvpredict_handler_wrapper;
	act = &act_copy;

	rvp_signal_select_alternate(signum, s);

out:
	if (signum == RVP_INFO_SIGNUM && (act->sa_flags & SA_SIGINFO) == 0 &&
	    s->s_handler == SIG_DFL) {
		act_copy = *act;
		act_copy.sa_handler = rvp_siginfo_handler;
		act = &act_copy;
	}

	rc = real_sigaction(signum, act, oact);
	if (oact == NULL || (oact->sa_flags & SA_SIGINFO) == 0 ||
	    oact->sa_sigaction != __rvpredict_handler_wrapper) {
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
INTERPOSE(int, sigsuspend, const sigset_t *);
