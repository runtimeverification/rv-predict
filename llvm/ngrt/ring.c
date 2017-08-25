#include <err.h>
#include <errno.h>
#include <pthread.h>
#include <sched.h>	/* for sched_yield(2) */
#include <stdio.h>	/* for stderr, fprintf(3) */
#include <stdlib.h>

#include <sys/param.h>	/* for MIN(a, b) */

#include "thread.h"
#include "trace.h"	/* for rvp_vec_and_op_to_deltop() */

static long pgsz = 0;
static bool rvp_do_debug = false;

static void
rvp_debugf(const char *fmt, ...)
{
	va_list ap;
	va_start(ap, fmt);
	if (!rvp_do_debug)
		return;
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

/* If there is not an item on the ring at index `lidx`, or if there is
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
		return false;

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

/* This does not recurse into interruptors.  Returns the number of slots
 * discarded.
 */
static int
rvp_ring_discard_to(rvp_ring_t *r, const int next, uint32_t *idepthp)
{
	const uint32_t * const producer = r->r_producer,
	    * const consumer = r->r_consumer;
	const int cidx = consumer - &r->r_items[0],
		  pidx = producer - &r->r_items[0];
	int preconsumed = 0;

	if (cidx == pidx)
		return 0;

	if (*idepthp != r->r_idepth) {
		preconsumed = sizeof(r->r_sigdepth) / sizeof(r->r_items[0]);
		rvp_debugf("%s.%d inserted sigdepth %zd at %d\n",
		    __func__, __LINE__,
		    preconsumed * sizeof(r->r_items[0]), cidx);
		*idepthp = r->r_idepth;
	}

	r->r_consumer = &r->r_items[next];

	if (cidx < next && next <= pidx)
		return preconsumed + next - cidx;

	assert(pidx < cidx);

	if (cidx < next)
		return preconsumed + next - cidx;
	assert(next <= pidx);
	return preconsumed + next + rvp_ring_capacity(r) + 1 - cidx; 
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

/* Return `n < 0` if slots up to `end` were discarded but more remain in
 * `r`, `n = 0` if `nslots` slots were discarded and no more remain in
 * `r`, and `n > 0` if `n < nslots` more slots remain to be discarded.
 */
static int
rvp_ring_discard_by_slots(rvp_ring_t *r, const int nslots,
    const int start, const int end, uint32_t *idepthp)
{
	int lastidx, residue = nslots;
	const int pidx = r->r_producer - r->r_items;
	int cidx = r->r_consumer - r->r_items;
	const rvp_interruption_t *it = NULL;

	assert(residue >= 0);

	assert(start < 0 || rvp_ring_index_consumed_before(r, cidx, start));

	lastidx = rvp_ring_residue_and_end_to_consumer_index(r,
	    residue, end);

	rvp_debugf(
	    "%s.%d: r %p enter residue %d cidx %d start %d end %d pidx %d\n",
	    __func__, __LINE__, (void *)r, residue, cidx, start, end, pidx);

	for (it = rvp_ring_first_interruption(r);
	     it != NULL && residue > 0;
	     it = rvp_ring_next_interruption(r, it)) {
		const int intr = it->it_interrupted_idx;
		rvp_debugf(
		    "%s.%d: r %p it %p residue %d lastidx %d cidx %d intr %d\n",
		    __func__, __LINE__, (void *)r, (const void *)it, residue,
		    lastidx, cidx, intr);
		if (rvp_ring_index_properly_consumed_before(r, lastidx, intr))
			break;
		if (rvp_ring_index_properly_consumed_before(r, cidx, intr)) {
			residue -= rvp_ring_discard_to(r, intr, idepthp);
			assert(residue >= 0);
		}
		assert(r->r_idepth != it->it_interruptor->r_idepth);
		cidx = intr;
		rvp_debugf("%s.%d: r %p residue %d\n",
		    __func__, __LINE__, (void *)r, residue);
		residue = rvp_ring_discard_by_slots(it->it_interruptor, residue,
		    it->it_interruptor_sidx, rvp_interruption_get_end(it),
		    idepthp);
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
		/* The call to rvp_ring_discard_by_slots can reduce the residue
		 * without consuming any items on this ring, so we have
		 * to recompute the last index on this ring.
		 */
		lastidx = rvp_ring_residue_and_end_to_consumer_index(r,
		    residue, end);
		rvp_debugf("%s.%d: r %p lastidx %d\n",
		    __func__, __LINE__, (void *)r, lastidx);
	}
	if (residue > 0) {
		residue -= rvp_ring_discard_to(r, lastidx, idepthp);
		rvp_debugf("%s.%d: r %p residue %d\n",
		    __func__, __LINE__, (void *)r, residue);
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
	   rvp_ring_discard_by_slots(r, nwritten / slotsz, -1, -1, idepthp) *
	   slotsz;
}

/* Fill I/O vectors with the ring content between `cidx` and `pidx`
 * until vectors run out.  Return true if there were enough I/O vectors
 * between *iovp and lastiov to hold the ring content between cidx and
 * pidx.  Return false if there were no vectors or only enough vectors
 * for a partial write.
 */
static bool
rvp_ring_get_iovs_between(rvp_ring_t *r, struct iovec ** const iovp,
    const struct iovec *lastiov, int cidx, int pidx, uint32_t *idepthp)
{
	uint32_t *producer = &r->r_items[pidx], *consumer = &r->r_items[cidx];

	if (consumer == producer)
		return false;

	if (*iovp == lastiov)
		return false;

	if (r->r_idepth != *idepthp) {
		/* Need at least two iovecs so that we can insert at least
		 * one item from the ring after this change of interrupt
		 * depth.
		 */
		if (*iovp + 1 == lastiov)
			return false;

		r->r_sigdepth = (rvp_sigdepth_t){
			  .deltop = (rvp_addr_t)rvp_vec_and_op_to_deltop(0,
			      RVP_OP_SIGDEPTH)
			, .depth = r->r_idepth
		};

		*(*iovp)++ = (struct iovec){
			  .iov_base = &r->r_sigdepth
			, .iov_len = sizeof(r->r_sigdepth)
		};
		rvp_debugf("%s.%d inserting sigdepth %zu at cidx %d\n",
		    __func__, __LINE__, sizeof(r->r_sigdepth), cidx);
		*idepthp = r->r_idepth;
	}

	if (consumer < producer) {
		*(*iovp)++ = (struct iovec){
			  .iov_base = consumer
			, .iov_len = (producer - consumer) *
				     sizeof(consumer[0])
		};
	} else {	/* consumer > producer */
		*(*iovp)++ = (struct iovec){
			  .iov_base = consumer
			, .iov_len = (r->r_last + 1 - consumer) *
				     sizeof(consumer[0])
		};

		if (*iovp == lastiov)
			return false;

		*(*iovp)++ = (struct iovec){
			  .iov_base = r->r_items
			, .iov_len = (producer - r->r_items) *
				     sizeof(r->r_items[0])
		};
	}
	return true;
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
bool
rvp_ring_get_iovs(rvp_ring_t *r, int start, int end,
    struct iovec **iovp, const struct iovec *lastiov, uint32_t *idepthp)
{
	struct iovec *iov;
	struct iovec * const iov0 = *iovp;
	const rvp_interruption_t *it = NULL;
	const int pidx = r->r_producer - r->r_items;
	const int cidx = r->r_consumer - r->r_items;
	int first = (start >= 0)
	    ? start
	    : r->r_consumer - r->r_items;
	const int last =
	    (end >= 0 &&
	     rvp_ring_index_properly_consumed_before(r, end, pidx))
                ? end
                : pidx;

	assert(start < 0 ||
	       rvp_ring_index_consumed_before(r, cidx, start));

	rvp_debugf("%s.%d: r %p enter cidx %d start %d end %d pidx %d\n",
	    __func__, __LINE__, (void *)r,
	    cidx, start, end, pidx);

	for (it = rvp_ring_first_interruption(r);
	     it != NULL && *iovp < lastiov;
	     it = rvp_ring_next_interruption(r, it)) {
		const int intr = it->it_interrupted_idx;

		rvp_debugf(
		    "%s.%d: r %p it %p #iovs %td first %d intr %d last %d\n",
		    __func__, __LINE__, (void *)r, (const void *)it,
		    lastiov - *iovp, first, intr, last);

		if (rvp_ring_index_properly_consumed_before(r, last, intr))
			break;

		assert((first == last && last == intr) ||
		       (first < last && first <= intr &&
		        intr <= last) ||
		       (last < first &&
		        (intr <= last || first <= intr)));

		if (rvp_ring_index_properly_consumed_before(r, first, intr) &&
		    !rvp_ring_get_iovs_between(r, iovp, lastiov, first, intr,
		            idepthp))
			goto out;

		assert(r->r_idepth != it->it_interruptor->r_idepth);

		first = intr;

		rvp_debugf("%s.%d: r %p #iovs %td\n",
		    __func__, __LINE__, (void *)r, lastiov - *iovp);

		// TBD the proper fix is to pass `it` or NULL both to
		// rvp_ring_get_iovs() and to rvp_ring_discard_by_bytes().
		// In rvp_ring_discard_by_bytes(), advance
		// it->it_interruptor_sidx as well as the ring's consumer
		// pointer.
		if (!rvp_ring_get_iovs(it->it_interruptor,
		    it->it_interruptor_sidx, rvp_interruption_get_end(it),
		    iovp, lastiov, idepthp))
			goto out;

		rvp_debugf("%s.%d: r %p #iovs %td\n",
		    __func__, __LINE__, (void *)r, lastiov - *iovp);
	}
	if (*iovp < lastiov) {
		(void)rvp_ring_get_iovs_between(r, iovp, lastiov, first, last,
		    idepthp);
	}

out:
	for (iov = iov0; iov < *iovp; iov++) {
		rvp_debugf("%s.%d: r %p iov[%td].iov_len = %zu\n",
		    __func__, __LINE__, (void *)r, iov - iov0, iov->iov_len);
	}
	rvp_debugf("%s.%d: r %p exit #iovs %td\n",
	    __func__, __LINE__, (void *)r, lastiov - *iovp);
	return iov0 < *iovp;
}
