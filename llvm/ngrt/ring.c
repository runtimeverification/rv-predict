#include <err.h>
#include <errno.h>
#include <pthread.h>
#include <sched.h>	/* for sched_yield() */
#include <stdlib.h>

#include <sys/param.h>	/* for MIN(a, b) */

#include "thread.h"

static long pgsz = 0;

void
rvp_rings_init(void)
{
	if (pgsz == 0 && (pgsz = sysconf(_SC_PAGE_SIZE)) == -1)
		err(EXIT_FAILURE, "%s: sysconf", __func__);
}

void
rvp_ring_init(rvp_ring_t *r, uint32_t *items, size_t nitems)
{
	r->r_producer = r->r_consumer = r->r_items = items;
	r->r_last = &r->r_items[nitems - 1];
	r->r_state = RVP_RING_S_INUSE;
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
	int i, j;

	for (i = 32; rvp_ring_nempty(r) < nempty; i = MIN(16384, i + 1)) {
		for (j = 0; j < i; j++)
			;
		/* XXX not async-signal-safe */
		sched_yield();
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

bool
rvp_ring_get_iovs(rvp_ring_t *r, struct iovec **iovpp, uint32_t **next_consumer)
{
	uint32_t *producer = r->r_producer, *consumer = r->r_consumer;
	struct iovec *iovp = *iovpp;

	if (consumer == producer)
		return false;

	if (consumer < producer) {
		*iovp++ = (struct iovec){
			  .iov_base = consumer
			, .iov_len = (producer - consumer) *
				     sizeof(consumer[0])
		};
	} else {	/* consumer > producer */
		*iovp++ = (struct iovec){
			  .iov_base = consumer
			, .iov_len = (r->r_last + 1 - consumer) *
				     sizeof(consumer[0])
		};
		*iovp++ = (struct iovec){
			  .iov_base = r->r_items
			, .iov_len = (producer - r->r_items) *
				     sizeof(r->r_items[0])
		};
	}
	*iovpp = iovp;
	*next_consumer = producer;
	return true;
}
