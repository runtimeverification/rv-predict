#include <err.h>
#include <errno.h>
#include <pthread.h>
#include <sched.h>	/* for sched_yield(2) */
#include <stdio.h>	/* for stderr, fprintf(3) */
#include <stdlib.h>

#include <sys/param.h>	/* for MIN(a, b) */

#include "rvpsignal.h"
#include "thread.h"
#include "trace.h"	/* for rvp_vec_and_op_to_deltop() */

static long pgsz = 0;
static const bool rvp_do_debug = false;

static void
rvp_debugf(const char *fmt, ...)
{
	va_list ap;
	if (!rvp_do_debug)
		return;
	va_start(ap, fmt);
	vfprintf(stderr, fmt, ap);
	va_end(ap);
}

void
rvp_rings_init(void)
{
	if (pgsz == 0 && (pgsz = sysconf(_SC_PAGE_SIZE)) == -1)
		err(EXIT_FAILURE, "%s: sysconf", __func__);
}

static void
rvp_iring_init(rvp_iring_t *ir)
{
	ir->ir_producer = ir->ir_consumer = &ir->ir_items[0];
}

void
rvp_ring_init(rvp_ring_t *r, uint32_t *items, size_t nitems)
{
	r->r_producer = r->r_consumer = r->r_items = items;
	r->r_last = &r->r_items[nitems - 1];
	r->r_state = RVP_RING_S_INUSE;
	rvp_iring_init(&r->r_iring);
}

int
rvp_ring_stdinit(rvp_ring_t *r)
{
	const size_t ringsz = pgsz;
	const size_t items_per_ring = ringsz / sizeof(*r->r_items);
	uint32_t *items;

	assert(pgsz != 0);

	items = calloc(items_per_ring, sizeof(*r->r_items));
	if (items == NULL)
		return ENOMEM;

	rvp_ring_init(r, items, items_per_ring);

	return 0;
}

void
rvp_ring_wait_for_nempty(rvp_ring_t *r, int nempty)
{
	int i;
	volatile int j;

	for (i = 32; rvp_ring_nempty(r) < nempty; i = MIN(16384, i + 1)) {
		for (j = 0; j < i; j++)
			;
		if (r->r_idepth == 0) {
			/* not async-signal-safe */
			sched_yield();
		}
	}
}

void
rvp_ring_wait_for_slot(rvp_ring_t *r, uint32_t *slot)
{
	int i, j;

	for (i = 32; slot == r->r_consumer; i = MIN(16384, i + 1)) {
		for (j = 0; j < i; j++)
			;
		/* XXX not async-signal-safe */
		sched_yield();
	}
}

/* If the ring is empty but `lidx == ridx` and `lidx` will be the
 * next item consumed when the ring re-fills, then return true.
 *
 * If there is not an item on the ring at index `lidx`, or if there is
 * not an item at `ridx` and `ridx` does not equal the producer index,
 * then return false.
 *
 * Otherwise, return true if ring index `lidx` points at an item that
 * will be consumed before the item at index `ridx`, or if `lidx ==
 * ridx`.
 */
static bool
rvp_ring_index_consumed_before(const rvp_ring_t *r, int lidx, int ridx)
{
	uint32_t *producer = r->r_producer, *consumer = r->r_consumer;
	const int cidx = consumer - &r->r_items[0],
		  pidx = producer - &r->r_items[0];

	//             pidx
	//              v
	// |...............................................|
	//              ^
	//             cidx
	if (cidx == pidx)
		return cidx == lidx && lidx == ridx;

	// |............***********************............|
	//              ^                      ^
	//            cidx                    pidx
	//                       ^
	//                      lidx
	//                       +-------> ridx
	if (cidx < pidx)
		return cidx <= lidx && lidx <= ridx && ridx <= pidx;

	//
	// Hereafter, pidx < cidx:
	//

	// |************.......................************|
	//              ^                      ^   
	//            pidx                    cidx 
	//                                         ^
	//                                        lidx
	//                                         +-------> ridx
	if (cidx <= lidx && lidx <= ridx)
		return true;

	// |************.......................************|
	//              ^                      ^   
	//            pidx                    cidx 
	//     ^
	//    lidx
	//     +-------> ridx
	if (lidx <= ridx && ridx <= pidx)
		return true;

	// |************.......................************|
	//              ^                      ^   
	//            pidx                    cidx 
	//       ^                                  ^
	//      ridx                               lidx
	return cidx <= lidx && ridx <= pidx;
}

/* Like rvp_ring_index_consumed_before, but return false if `lidx == ridx`. */
static inline bool
rvp_ring_index_properly_consumed_before(rvp_ring_t *r, int lidx, int ridx)
{
	return rvp_ring_index_consumed_before(r, lidx, ridx) && lidx != ridx;
}

static inline int
rvp_ring_residue_and_end_to_consumer_index(const rvp_ring_t *r,
    int residue, int end)
{
	if (residue < rvp_ring_nfull(r)) {
		const int ridx =
		    rvp_ring_consumer_index_advanced_by(r, residue);
		if (end >= 0 &&
		    rvp_ring_index_consumed_before(r, end, ridx))
			return end;
		else
			return ridx;
	} else if (end >= 0)
		return end;
	else
		return r->r_producer - r->r_items;
}

/* This does not recurse into interruptors.  Returns the number of slots
 * discarded.
 */
static int
rvp_ring_discard_to(rvp_ring_t *r, int residue, int *nextp,
    uint32_t *idepthp)
{
	const uint32_t * const producer = r->r_producer,
	    * const consumer = r->r_consumer;
	const int cidx = consumer - &r->r_items[0],
		  pidx = producer - &r->r_items[0];
	int preconsumed = 0;
	int next0 = *nextp;

	if (cidx == pidx)
		return 0;

	if (*idepthp != r->r_idepth) {
		preconsumed = sizeof(r->r_sigdepth) / sizeof(r->r_items[0]);
		residue -= preconsumed;
		rvp_debugf("%s.%d inserted %zd-byte sigdepth at %d\n",
		    __func__, __LINE__,
		    preconsumed * sizeof(r->r_items[0]), cidx);
		*idepthp = r->r_idepth;
	}

	assert(0 < residue);
	const int next = rvp_ring_residue_and_end_to_consumer_index(r,
	    residue, next0);
	*nextp = next;

	assert(rvp_ring_index_consumed_before(r, next, pidx));

	r->r_consumer = &r->r_items[next];

	if (cidx < next)
		return preconsumed + next - cidx;

	assert(pidx < cidx && next <= pidx);

	return preconsumed + next + rvp_ring_capacity(r) + 1 - cidx; 
}

static void
rvp_ring_drop_interruption(rvp_ring_t *r)
{
	rvp_iring_t *ir = &r->r_iring;
	rvp_interruption_t *prev = ir->ir_consumer;
	rvp_interruption_t *next =
	    (prev == rvp_iring_last(ir)) ? &ir->ir_items[0] : (prev + 1);

#if 0	// XXX This looks unnecessary for making the ring eligible for
	// reuse
	rvp_signal_ring_put(NULL, prev->it_interruptor);
#endif

	assert(prev != ir->ir_producer);

	atomic_store_explicit(&ir->ir_consumer, next, memory_order_release);
}

/* Return `n < 0` if slots up to `bracket->it_interruptor_eidx` were
 * discarded but more remain in `r`, `n = 0` if `nslots` slots were
 * discarded and no more remain in `r`, and `n > 0` if `n < nslots` more
 * slots remain to be discarded.
 *
 * If bracket != NULL, write the new consumer index for `r` back to
 * bracket->it_interruptor_sidx every time it advances.
 */
static int
rvp_ring_discard_by_slots(rvp_ring_t *r, const int nslots,
    rvp_interruption_t *bracket, uint32_t *idepthp)
{
	int lastidx, residue = nslots;
	const int pidx = r->r_producer - r->r_items;
	int cidx = r->r_consumer - r->r_items;
	rvp_interruption_t *it = NULL;
	const int start = (bracket != NULL)
	    ? bracket->it_interruptor_sidx
	    : -1;
	const int end = (bracket != NULL)
	    ? rvp_interruption_get_end(bracket, NULL)
	    : -1;

	assert(bracket == NULL || bracket->it_interruptor == r);

	assert(residue >= 0);

	assert(start < 0 || rvp_ring_index_consumed_before(r, cidx, start));

	lastidx = rvp_ring_residue_and_end_to_consumer_index(r, residue, end);

	rvp_debugf(
	    "%s.%d: r %p enter residue %d cidx %d start %d end %d pidx %d\n",
	    __func__, __LINE__, (void *)r, residue, cidx, start, end, pidx);

	for (it = rvp_ring_first_interruption(r);
	     it != NULL && residue > 0;
	     it = rvp_ring_next_interruption(r, it)) {
		int intr = it->it_interrupted_idx;
		rvp_debugf(
		    "%s.%d: r %p it %p residue %d lastidx %d cidx %d intr %d\n",
		    __func__, __LINE__, (void *)r, (const void *)it, residue,
		    lastidx, cidx, intr);
		if (rvp_ring_index_properly_consumed_before(r, lastidx, intr))
			break;
		if (rvp_ring_index_properly_consumed_before(r, cidx, intr)) {
			residue -= rvp_ring_discard_to(r, residue, &intr, idepthp);
			assert(residue >= 0);
			if (bracket != NULL)
				bracket->it_interruptor_sidx = intr;
		}
		assert(r->r_idepth != it->it_interruptor->r_idepth);
		cidx = intr;
		rvp_debugf("%s.%d: r %p residue %d\n",
		    __func__, __LINE__, (void *)r, residue);
		residue = rvp_ring_discard_by_slots(it->it_interruptor, residue,
		    it, idepthp);
		rvp_debugf("%s.%d: r %p residue %d\n",
		    __func__, __LINE__, (void *)r, residue);
		/* If residue < 0, then rvp_ring_discard_by_slots() didn't
		 * empty it->it_interruptor to rvp_interruption_get_end(it),
		 * however, it did run down residue to 0.  So leave `it` on
		 * the interruption ring of `r` and return residue < 0 so that
		 * this ring, too, is left on its parent's ring.
		 */
		if (residue < 0)
			break;
		rvp_ring_drop_interruption(r);
		/* The call to rvp_ring_discard_by_slots can reduce the
		 * residue without consuming any items on this ring, so
		 * we have to recompute the last index on this ring.
		 */
		lastidx = rvp_ring_residue_and_end_to_consumer_index(r,
		    residue, end);
		rvp_debugf("%s.%d: r %p lastidx %d\n",
		    __func__, __LINE__, (void *)r, lastidx);
	}
	if (residue > 0) {
		residue -= rvp_ring_discard_to(r, residue, &lastidx, idepthp);
		assert(residue >= 0);
		rvp_debugf("%s.%d: r %p residue %d\n",
		    __func__, __LINE__, (void *)r, residue);
		if (bracket != NULL)
			bracket->it_interruptor_sidx = lastidx;
	}
	if (residue == 0 && end >= 0) {
		rvp_debugf("%s.%d: r %p cidx %d\n",
		    __func__, __LINE__, (void *)r, cidx);

		if (rvp_ring_index_properly_consumed_before(r, cidx, end))
			return -1;

		if ((it = rvp_ring_first_interruption(r)) == NULL)
			return 0;

		const int intr = it->it_interrupted_idx;

		rvp_debugf(
		    "%s.%d: r %p exit -1 intr %d\n",
		    __func__, __LINE__, (void *)r, intr);

		/* XXX Should be consumed before rather than properly consumed
		 * XXX before?  Can't there be an interruption right after
		 * XXX the last event?
		 */
		if (rvp_ring_index_properly_consumed_before(r, intr, end))
			return -1;
	}
	rvp_debugf("%s.%d: r %p exit residue %d\n",
	    __func__, __LINE__, (void *)r, residue);
	return residue;
}

/* Advance the consumer pointer of `r` and its interruptors for a
 * write of `nwritten` bytes starting at r->r_consumer.  If `nwritten`
 * is greater than the number of bytes in `r` and its interruptors,
 * then return the number of bytes left to advance.
 */
ssize_t
rvp_ring_discard_by_bytes(rvp_ring_t *r, const ssize_t nwritten,
    uint32_t *idepthp)
{
	const ssize_t slotsz = sizeof(r->r_items[0]);
	return
	   rvp_ring_discard_by_slots(r, nwritten / slotsz, NULL, idepthp) *
	   slotsz;
}

/* Fill I/O vectors with all of the ring content between `cidx` and
 * `pidx` and, if necessary, a vector for a change of signal depth.  If
 * there are not enough vectors, fill none.
 *
 * If there were enough I/O vectors between *iovp and
 * lastiov to hold the ring content between cidx and pidx and any change
 * of signal depth, return the number of I/O vectors that remain.
 * Return -1 if there were not enough vectors.
 *
 * XXX It's important to write whole events.  The producer pointer only
 * advances to event boundaries.  An interrupt always begins and ends on
 * an event boundary, too.  So as long as we end at a producer pointer
 * or at an interrupt, we will write whole events.  But we cannot stop
 * writing at an arbitrary slot in the ring or else the bytes of an
 * event may be split by another thread's events.
 */
static int
rvp_ring_get_iovs_between(rvp_ring_t *r, struct iovec ** const iovp,
    const struct iovec *lastiov, int cidx, int pidx, uint32_t *idepthp)
{
	uint32_t *producer = &r->r_items[pidx], *consumer = &r->r_items[cidx];
	struct iovec *iov = *iovp;
	bool writeback_depth = false;

	if (consumer == producer)
		return -1;

	if (iov == lastiov)
		return -1;

	if (r->r_idepth != *idepthp) {
		/* Need at least two iovecs so that we can insert at least
		 * one item from the ring after this change of interrupt
		 * depth.
		 */
		if (iov + 1 == lastiov)
			return -1;

		r->r_sigdepth = (rvp_sigdepth_t){
			  .deltop = (rvp_addr_t)rvp_vec_and_op_to_deltop(0,
			      RVP_OP_SIGDEPTH)
			, .depth = r->r_idepth
		};

		*iov++ = (struct iovec){
			  .iov_base = &r->r_sigdepth
			, .iov_len = sizeof(r->r_sigdepth)
		};
		writeback_depth = true;
	}

	if (consumer < producer) {
		*iov++ = (struct iovec){
			  .iov_base = consumer
			, .iov_len = (producer - consumer) *
				     sizeof(consumer[0])
		};
	} else {	/* consumer > producer */
		*iov++ = (struct iovec){
			  .iov_base = consumer
			, .iov_len = (r->r_last + 1 - consumer) *
				     sizeof(consumer[0])
		};

		if (iov == lastiov)
			return -1;

		*iov++ = (struct iovec){
			  .iov_base = r->r_items
			, .iov_len = (producer - r->r_items) *
				     sizeof(r->r_items[0])
		};
	}
	*iovp = iov;
	rvp_debugf("%s.%d r %p span %td -> %td\n",
	    __func__, __LINE__, (void *)r, cidx, producer - r->r_items);
	if (writeback_depth) {
		rvp_debugf(
		    "%s.%d r %p inserted %zu-byte sigdepth %p at cidx %d\n",
		    __func__, __LINE__, (void *)r, sizeof(r->r_sigdepth),
		    &r->r_sigdepth, cidx);
		*idepthp = r->r_idepth;
	}
	return lastiov - *iovp;
}

rvp_interruption_t *
rvp_ring_put_interruption(rvp_ring_t *r, rvp_ring_t *interruptor, int sidx)
{
	rvp_iring_t *ir = &r->r_iring;
	rvp_interruption_t *prev = ir->ir_producer;
	rvp_interruption_t *next =
	    (prev == rvp_iring_last(ir)) ? &ir->ir_items[0] : (prev + 1);

	while (rvp_iring_nempty(ir) < 1)
		rvp_iring_await_one_empty(r);

	prev->it_interruptor = interruptor;
	prev->it_interrupted_idx = r->r_producer - r->r_items;
	prev->it_interruptor_sidx = sidx;
	prev->it_interruptor_eidx = -1;
	atomic_store_explicit(&ir->ir_producer, next, memory_order_release);
	return prev;
}

/* Fill iovecs with the ring content beginning at the consumer pointer.
 * Recurse into any interrupting rings.  Fill the iovecs beginning at
 * the one at *iovp, and advance *iovp as each iovec is filled, but do
 * not fill the iovec at lastiov or after.  Return true if any iovecs
 * were filled, false otherwise.
 */
int
rvp_ring_get_iovs(rvp_ring_t *r, rvp_interruption_t *bracket,
    struct iovec **iovp, const struct iovec *lastiov, uint32_t *idepthp)
{
	struct iovec *iov;
	struct iovec * const iov0 = *iovp;
	rvp_interruption_t *it = NULL;
	const int pidx = r->r_producer - r->r_items;
	const int cidx = r->r_consumer - r->r_items;
	int first = (bracket != NULL)
	    ? bracket->it_interruptor_sidx
	    : cidx;
	bool unfinished = false;
	const int last = (bracket != NULL)
	    ? rvp_interruption_get_end(bracket, &unfinished)
	    : pidx;
	/* XXX clamp last to pidx? */
#if 0
	    (end >= 0 &&
	     rvp_ring_index_properly_consumed_before(r, end, pidx))
                ? end
                : pidx;
#endif
	ptrdiff_t residue;

	assert(bracket == NULL ||
	    rvp_ring_index_consumed_before(r, cidx,
	        bracket->it_interruptor_sidx));

	rvp_debugf("%s.%d: r %p enter cidx %d first %d last %d pidx %d\n",
	    __func__, __LINE__, (void *)r, cidx, first, last, pidx);

	for (it = rvp_ring_first_interruption(r);
	     (residue = lastiov - *iovp) > 0 && it != NULL;
	     it = rvp_ring_next_interruption(r, it)) {
		const int intr = it->it_interrupted_idx;

		rvp_debugf(
		    "%s.%d: r %p it %p #iovs %td first %d intr %d last %d\n",
		    __func__, __LINE__, (void *)r, (const void *)it,
		    residue, first, intr, last);

		if (rvp_ring_index_properly_consumed_before(r, intr, first)) {
			rvp_debugf(
			    "%s.%d: r %p it %p is before first; skipping\n",
			    __func__, __LINE__, (void *)r, (const void *)it);
			continue;
		}

		if (rvp_ring_index_properly_consumed_before(r, last, intr)) {
			rvp_debugf("%s.%d: r %p it %p is beyond last\n",
			    __func__, __LINE__, (void *)r, (const void *)it);
			break;
		}

		if (rvp_ring_index_properly_consumed_before(r, first, intr) &&
		    (residue = rvp_ring_get_iovs_between(r, iovp, lastiov, first, intr,
		            idepthp)) < 0)
			goto out;

		assert(r->r_idepth != it->it_interruptor->r_idepth);

		first = intr;

		rvp_debugf("%s.%d: r %p #iovs %td\n",
		    __func__, __LINE__, (void *)r, lastiov - *iovp);

		residue = rvp_ring_get_iovs(it->it_interruptor,
		    it, iovp, lastiov, idepthp);

		if (residue < 0)
			goto out;

		rvp_debugf("%s.%d: r %p #iovs %td\n",
		    __func__, __LINE__, (void *)r, residue);
	}
	if (residue > 0) {
		residue = rvp_ring_get_iovs_between(r, iovp, lastiov,
		    first, last, idepthp);
	}

out:
	for (iov = iov0; iov < *iovp; iov++) {
		rvp_debugf("%s.%d: r %p iov[%td].iov_len = %zu\n",
		    __func__, __LINE__, (void *)r, iov - iov0, iov->iov_len);
	}
	rvp_debugf("%s.%d: r %p exit #iovs %td\n",
	    __func__, __LINE__, (void *)r, residue);
	return unfinished ? -1 : residue;
}

static int
rvp_ring_discard_iovs_between(rvp_ring_t *r, const struct iovec ** const iovp,
    const struct iovec *lastiov, int cidx, uint32_t *idepthp)
{
	uint32_t *consumer = &r->r_items[cidx], *producer;
	const struct iovec *iov = *iovp;
	bool writeback_depth = false;

	if (iov == lastiov)
		return -1;

	if (r->r_idepth != *idepthp) {
		/* Need at least two iovecs so that we can insert at least
		 * one item from the ring after this change of interrupt
		 * depth.
		 */
		if (iov + 1 == lastiov)
			return -1;

		assert(iov->iov_base == &r->r_sigdepth &&
		       iov->iov_len == sizeof(r->r_sigdepth));
		iov++;
		writeback_depth = true;
	}

	assert(iov->iov_base == consumer);

	if (iov->iov_len == (r->r_last + 1 - consumer) * sizeof(consumer[0])) {
		iov++;
		if (iov == lastiov)
			return -1;
		assert(iov->iov_base == r->r_items);
		producer = iov->iov_len / sizeof(r->r_items[0]) + r->r_items;
		iov++;
	} else {
		producer = iov->iov_len / sizeof(consumer[0]) + consumer;
		iov++;
	}
	assert(consumer == r->r_consumer);
	r->r_consumer = producer;

	rvp_debugf("%s.%d r %p span %td -> %td\n",
	    __func__, __LINE__, (void *)r, cidx, producer - r->r_items);
	*iovp = iov;
	if (writeback_depth) {
		rvp_debugf(
		    "%s.%d r %p discarded %zu-byte sigdepth %p at cidx %d\n",
		    __func__, __LINE__, (void *)r,
		    sizeof(r->r_sigdepth), &r->r_sigdepth, cidx);
		*idepthp = r->r_idepth;
	}
	return lastiov - *iovp;
}

/* Return -1 if the I/O vectors were exhausted and `r` or its interruptors
 * still contain events, or if interruptions are unfinished.
 *
 * Return 0 if the I/O vectors were exhausted and neither `r` nor its
 * interruptors contain more events.
 *
 * Otherwise, return the number of I/O vectors that remain.
 */
int
rvp_ring_discard_iovs(rvp_ring_t *r, rvp_interruption_t *bracket,
    const struct iovec **iovp, const struct iovec *lastiov, uint32_t *idepthp)
{
	const struct iovec *iov;
	const struct iovec * const iov0 = *iovp;
	rvp_interruption_t *it = NULL;
	const int pidx = r->r_producer - r->r_items;
	const int cidx = r->r_consumer - r->r_items;
	int first = (bracket != NULL)
	    ? bracket->it_interruptor_sidx
	    : r->r_consumer - r->r_items;
	bool unfinished = false;
	const int last = (bracket != NULL)
	    ? rvp_interruption_get_end(bracket, &unfinished)
	    : pidx;
	ptrdiff_t residue;

	assert(bracket == NULL ||
	       rvp_ring_index_consumed_before(r, cidx,
	           bracket->it_interruptor_sidx));

	rvp_debugf("%s.%d: r %p enter cidx %d first %d last %d pidx %d\n",
	    __func__, __LINE__, (void *)r, cidx, first, last, pidx);

	for (it = rvp_ring_first_interruption(r);
	     (residue = lastiov - *iovp) > 0 && it != NULL;
	     it = rvp_ring_next_interruption(r, it)) {
		const int intr = it->it_interrupted_idx;

		rvp_debugf(
		    "%s.%d: r %p it %p #iovs %td first %d intr %d last %d\n",
		    __func__, __LINE__, (void *)r, (const void *)it,
		    residue, first, intr, last);

		if (rvp_ring_index_properly_consumed_before(r, intr, first)) {
			rvp_debugf("%s.%d: r %p it %p is before first; "
			    "should have been dropped already\n",
			    __func__, __LINE__, (void *)r, (const void *)it);
			assert(false);
		}

		if (rvp_ring_index_properly_consumed_before(r, last, intr)) {
			rvp_debugf("%s.%d: r %p it %p is beyond last\n",
			    __func__, __LINE__, (void *)r, (const void *)it);
			break;
		}

		if (rvp_ring_index_properly_consumed_before(r, first, intr) &&
		    (residue = rvp_ring_discard_iovs_between(r, iovp, lastiov,
		     first, idepthp)) < 0)
			goto out;

		if (bracket != NULL) {
			bracket->it_interruptor_sidx =
			    r->r_consumer - r->r_items; 
		}
		assert(r->r_idepth != it->it_interruptor->r_idepth);

		first = intr;

		rvp_debugf("%s.%d: r %p #iovs %td\n",
		    __func__, __LINE__, (void *)r, lastiov - *iovp);

		residue = rvp_ring_discard_iovs(it->it_interruptor,
		    it, iovp, lastiov, idepthp);

		if (residue < 0) {
			break;
		}

		rvp_debugf("%s.%d: r %p dropping it %p #iovs %td\n",
		    __func__, __LINE__, (void *)r, (void *)it, residue);
		rvp_ring_drop_interruption(r);
	}
	if (residue > 0) {
		residue = rvp_ring_discard_iovs_between(r, iovp, lastiov, first,
		    idepthp);
		if (residue < 0)
			goto out;
		first = r->r_consumer - r->r_items;
		if (bracket != NULL) {
			bracket->it_interruptor_sidx = first; 
		}
	}
	if (residue == 0) {
		rvp_debugf("%s.%d: r %p residue 0 first %d last %d\n",
		    __func__, __LINE__, (void *)r, first, last);

		if (rvp_ring_index_properly_consumed_before(r, first, last)) {
			residue = -1;
			goto out;
		}

		if ((it = rvp_ring_first_interruption(r)) == NULL)
			goto out;

		const int intr = it->it_interrupted_idx;

		rvp_debugf(
		    "%s.%d: r %p intr %d\n",
		    __func__, __LINE__, (void *)r, intr);

		/* XXX Should be consumed before rather than properly consumed
		 * XXX before?  Can't there be an interruption right after
		 * XXX the last event?
		 */
		if (rvp_ring_index_consumed_before(r, intr, last)) {
			residue = -1;
			goto out;
		}
	}

out:
	for (iov = iov0; iov < *iovp; iov++) {
		rvp_debugf("%s.%d: r %p iov[%td].iov_len = %zu\n",
		    __func__, __LINE__, (void *)r, iov - iov0, iov->iov_len);
	}
	rvp_debugf("%s.%d: r %p exit #iovs %td\n",
	    __func__, __LINE__, (void *)r, residue);
	return unfinished ? -1 : residue;
}
