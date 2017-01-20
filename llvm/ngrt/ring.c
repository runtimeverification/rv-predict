#include <err.h>
#include <pthread.h>
#include <sched.h>	/* for sched_yield() */
#include <stdlib.h>

#include <sys/param.h>	/* for MIN(a, b) */

#include "thread.h"

void
rvp_ring_init(rvp_ring_t *r, uint32_t *items, size_t nitems)
{
	r->r_producer = r->r_consumer = r->r_items = items;
	r->r_last = &r->r_items[nitems - 1];
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
