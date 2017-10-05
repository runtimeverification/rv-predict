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
	const size_t ringsz = ((2 * PATH_MAX) / pgsz + 1) * pgsz;
	const size_t items_per_ring = ringsz / sizeof(*r->r_items);
	uint32_t *items;

	assert(ringsz != 0);

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
		if (r->r_idepth == 0) {
			sched_yield();	/* not async-signal-safe */
			continue;
		}
		for (j = 0; j < i; j++)
			;
	}
}

/* Visualize the buffer as a ring where the consumer pointer
 * follows the producer pointer in a clockwise direction. When the
 * producer and the consumer are equal, then they indicate a point. When
 * the producer and the consumer are unequal, then they indicate a
 * clockwise directed arc, consumer->producer.
 *
 * If the producer and consumer indicate a point, `P`, then
 * rvp_ring_cp_arc_contains_lr_arc() returns true only if `lidx == ridx
 * == P`.
 *
 * If the producer and consumer indicate an arc, consumer->producer,
 * then rvp_ring_cp_arc_contains_lr_arc() returns true if `lidx ==
 * ridx == P` for `P` a point on that arc, or if the clockwise arc
 * lidx->ridx is entirely contained in consumer->producer. Otherwise,
 * rvp_ring_cp_arc_contains_lr_arc() returns false.
 */
static bool
rvp_ring_cp_arc_contains_lr_arc(const rvp_ring_t *r, int lidx, int ridx)
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

/* Let us call the path from `lidx` to `ridx` an arc, always.  If
 * the two pointers indicate the same point, then let us call it a
 * "degenerate arc."  If the two pointers indicate different points,
 * then let us call the path a "proper arc."  If `lidx` and `ridx` form
 * a proper arc, then rvp_ring_cp_arc_contains_proper_lr_arc() returns
 * rvp_ring_cp_arc_contains_lr_arc(r, lidx, ridx).  Otherwise, it returns
 * false.
 */
static inline bool
rvp_ring_cp_arc_contains_proper_lr_arc(rvp_ring_t *r, int lidx, int ridx)
{
	return rvp_ring_cp_arc_contains_lr_arc(r, lidx, ridx) && lidx != ridx;
}

/* Drop the first interruption on the ring `r` and return either the one
 * after or NULL if there are no interruptions left.
 */
static rvp_interruption_t *
rvp_ring_drop_interruption(rvp_ring_t *r)
{
	rvp_iring_t *ir = &r->r_iring;
	rvp_interruption_t *prev = ir->ir_consumer;
	rvp_interruption_t *producer = ir->ir_producer;
	rvp_interruption_t *next =
	    (prev == rvp_iring_last(ir)) ? &ir->ir_items[0] : (prev + 1);

#if 0	// XXX This looks unnecessary for making the ring eligible for
	// reuse
	rvp_signal_ring_put(NULL, prev->it_interruptor);
#endif

	assert(prev != ir->ir_producer);

	atomic_store_explicit(&ir->ir_consumer, next, memory_order_release);

	if (next == producer)
		return NULL;

	return next;
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

/* Fill I/O vectors with all of the ring content between `first` and
 * `last` and, if necessary, a vector for a change of signal depth.  If
 * there are not enough vectors, fill none.
 *
 * If there were enough I/O vectors between *iovp and
 * lastiov to hold the ring content between `first` and `last` and any change
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
    const struct iovec *lastiov, int first, int last, uint32_t *idepthp)
{
	uint32_t *end = &r->r_items[last], *start = &r->r_items[first];
	struct iovec *iov = *iovp;
	bool writeback_depth = false;

	if (start == end)
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

	if (start < end) {
		*iov++ = (struct iovec){
			  .iov_base = start
			, .iov_len = (end - start) *
				     sizeof(start[0])
		};
	} else {	/* start > end */
		*iov++ = (struct iovec){
			  .iov_base = start
			, .iov_len = (r->r_last + 1 - start) *
				     sizeof(start[0])
		};

		if (iov == lastiov)
			return -1;

		*iov++ = (struct iovec){
			  .iov_base = r->r_items
			, .iov_len = (end - r->r_items) *
				     sizeof(r->r_items[0])
		};
	}
	*iovp = iov;
	rvp_debugf("%s.%d r %p span %td -> %td\n",
	    __func__, __LINE__, (void *)r, first, end - r->r_items);
	if (writeback_depth) {
		rvp_debugf(
		    "%s.%d r %p inserted %zu-byte sigdepth %p at first %d\n",
		    __func__, __LINE__, (void *)r, sizeof(r->r_sigdepth),
		    &r->r_sigdepth, first);
		*idepthp = r->r_idepth;
	}
	return lastiov - *iovp;
}

/* Fill iovecs with content of ring `r` beginning at its consumer pointer.
 * Recurse into any interrupting rings.  Fill the iovecs beginning at
 * the one at `*iovp`, and advance `*iovp` as each iovec is filled, but do
 * not fill the iovec at `lastiov` or after.
 *
 * Return -1 if an unfinished interrupt is encountered or if there are
 * more discontinuous spans of events to serialize than there were
 * I/O vectors between `*iovp` and `lastiov`, exclusive.  Otherwise,
 * return the number of I/O vectors that remain unfilled.
 *
 * rvp_ring_get_iovs() and rvp_ring_discard_iovs() form a pair:
 * rvp_ring_get_iovs() examines the event "tree" rooted at `r` and
 * produces an array of I/O vectors for the unserialized events,
 * while rvp_ring_discard_iovs() uses the I/O vector array created by
 * rvp_ring_get_iovs() to advance the consumer pointers of that
 * event tree.
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
	     rvp_ring_cp_arc_contains_proper_lr_arc(r, end, pidx))
                ? end
                : pidx;
#endif
	ptrdiff_t residue;

	assert(bracket == NULL ||
	    rvp_ring_cp_arc_contains_lr_arc(r, cidx,
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

		/* There may be interruptions on this ring that did not
		 * interrupt `bracket`.  Rather, they interrupted
		 * some interrupt that preceded `bracket`. Skip them.
		 */
		if (rvp_ring_cp_arc_contains_proper_lr_arc(r, intr, first)) {
			rvp_debugf(
			    "%s.%d: r %p it %p is before first; skipping\n",
			    __func__, __LINE__, (void *)r, (const void *)it);
			continue;
		}

		/* An interruption after `last` may not have affected the
		 * interrupt that we are presently serializing, but some
		 * subsequent interrupt, so we had better stop here.
		 */
		if (rvp_ring_cp_arc_contains_proper_lr_arc(r, last, intr)) {
			rvp_debugf("%s.%d: r %p it %p is beyond last\n",
			    __func__, __LINE__, (void *)r, (const void *)it);
			break;
		}

		if (rvp_ring_cp_arc_contains_proper_lr_arc(r, first, intr) &&
		    (residue = rvp_ring_get_iovs_between(r, iovp, lastiov, first, intr,
		            idepthp)) < 0)
			break;

		assert(r->r_idepth != it->it_interruptor->r_idepth);

		first = intr;

		rvp_debugf("%s.%d: r %p #iovs %td\n",
		    __func__, __LINE__, (void *)r, lastiov - *iovp);

		residue = rvp_ring_get_iovs(it->it_interruptor,
		    it, iovp, lastiov, idepthp);

		if (residue < 0)
			break;

		rvp_debugf("%s.%d: r %p #iovs %td\n",
		    __func__, __LINE__, (void *)r, residue);
	}
	if (residue > 0) {
		residue = rvp_ring_get_iovs_between(r, iovp, lastiov,
		    first, last, idepthp);
	}

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
	       rvp_ring_cp_arc_contains_lr_arc(r, cidx,
	           bracket->it_interruptor_sidx));

	rvp_debugf("%s.%d: r %p enter cidx %d first %d last %d pidx %d\n",
	    __func__, __LINE__, (void *)r, cidx, first, last, pidx);

	for (it = rvp_ring_first_interruption(r);
	     (residue = lastiov - *iovp) > 0 && it != NULL;
	     it = rvp_ring_drop_interruption(r)) {
		const int intr = it->it_interrupted_idx;

		rvp_debugf(
		    "%s.%d: r %p it %p #iovs %td first %d intr %d last %d\n",
		    __func__, __LINE__, (void *)r, (const void *)it,
		    residue, first, intr, last);

		if (rvp_ring_cp_arc_contains_proper_lr_arc(r, intr, first)) {
			rvp_debugf("%s.%d: r %p it %p is before first; "
			    "should have been dropped already\n",
			    __func__, __LINE__, (void *)r, (const void *)it);
			assert(false);
		}

		if (rvp_ring_cp_arc_contains_proper_lr_arc(r, last, intr)) {
			rvp_debugf("%s.%d: r %p it %p is beyond last\n",
			    __func__, __LINE__, (void *)r, (const void *)it);
			break;
		}

		if (rvp_ring_cp_arc_contains_proper_lr_arc(r, first, intr) &&
		    (residue = rvp_ring_discard_iovs_between(r, iovp, lastiov,
		     first, idepthp)) < 0)
			break;

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

		if (residue < 0)
			break;

		rvp_debugf("%s.%d: r %p dropping it %p #iovs %td\n",
		    __func__, __LINE__, (void *)r, (void *)it, residue);
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

		if (rvp_ring_cp_arc_contains_proper_lr_arc(r, first, last)) {
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
		if (rvp_ring_cp_arc_contains_lr_arc(r, intr, last)) {
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
