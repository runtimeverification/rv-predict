/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_RING_H_
#define _RVP_RING_H_

#include <stdatomic.h>	/* for atomic_store_explicit */
#include <stdint.h>	/* for uint32_t */
#include <string.h>	/* for memcpy(3) */
#include <unistd.h>	/* for size_t */

#include "buf.h"

struct _rvp_ring;
typedef struct _rvp_ring rvp_ring_t;

struct _rvp_ring {
	uint32_t * _Atomic volatile r_producer, * _Atomic volatile r_consumer;
	uint32_t *r_last;
	uint32_t *r_items;
	const char *r_lastpc;
	uint64_t r_lgen;	// thread-local generation number
};

extern volatile _Atomic uint64_t rvp_ggen;

static inline void
rvp_increase_ggen(void)
{
	(void)atomic_fetch_add_explicit(&rvp_ggen, 1, memory_order_release);
}

static inline uint64_t
rvp_ggen_before_store(void)
{
	// acquire semantics ensure that the global generation load
	// precedes the following instrumented store
	return atomic_load_explicit(&rvp_ggen, memory_order_acquire);
}

static inline uint64_t
rvp_ggen_after_load(void)
{
	// ensure that the instrumented load precedes the global
	// generation load
	atomic_thread_fence(memory_order_acquire);
	return atomic_load_explicit(&rvp_ggen, memory_order_acquire);
}

static inline void
rvp_buf_trace_cog(rvp_buf_t *b, uint64_t *lgenp, uint64_t gen)
{
	if (*lgenp < gen) {
		*lgenp = gen;
		rvp_buf_put_cog(b, gen);
	}
}

static inline void
rvp_buf_trace_load_cog(rvp_buf_t *b, uint64_t *lgenp)
{
	rvp_buf_trace_cog(b, lgenp, rvp_ggen_after_load());
}

void rvp_ring_init(rvp_ring_t *, uint32_t *, size_t);
void rvp_ring_wait_for_slot(rvp_ring_t *, uint32_t *);
void rvp_ring_wait_for_nempty(rvp_ring_t *, int);
void rvp_wake_transmitter(void);

static inline int
rvp_ring_nfull(rvp_ring_t *r)
{
	uint32_t *producer = r->r_producer,
	         *consumer = r->r_consumer;

	if (producer >= consumer)
		return producer - consumer;

	return (r->r_last - r->r_items) + 1 - (consumer - producer);
}

static inline int
rvp_ring_capacity(rvp_ring_t *r)
{
	return r->r_last - r->r_items;
}

static inline int
rvp_ring_nempty(rvp_ring_t *r)
{
	return rvp_ring_capacity(r) - rvp_ring_nfull(r);
}

static inline void
rvp_ring_request_service(rvp_ring_t *r)
{
	rvp_wake_transmitter();
}

static inline void
rvp_ring_await_nempty(rvp_ring_t *r, int nempty)
{
	rvp_ring_request_service(r);
	rvp_ring_wait_for_nempty(r, nempty);
}

static inline void
rvp_ring_open_slot(rvp_ring_t *r, uint32_t *slot)
{
	rvp_ring_request_service(r);
	rvp_ring_wait_for_slot(r, slot);
}

static inline void
rvp_ring_put_multiple(rvp_ring_t *r, const uint32_t *item, int nitems)
{
	uint32_t *prev = r->r_producer;
	uint32_t *next = (prev == r->r_last) ? r->r_items : (prev + 1);

	if (prev + nitems <= r->r_last) {
		next = prev + nitems;
	} else {
		const int ringsz = r->r_last + 1 - r->r_items;
		next = prev + (nitems - ringsz);
	}

	/* TBD do we need to order the r_consumer, r_producer reads? */

	while (rvp_ring_nempty(r) < nitems)
		rvp_ring_await_nempty(r, nitems);

	if (prev < next) {
		memcpy(prev, item, nitems * sizeof(prev[0]));
	} else {
		int nfirst = r->r_last - prev + 1,
		    nlast = next - r->r_items;
		memcpy(prev, item, nfirst * sizeof(item[0]));
		memcpy(r->r_items, &item[nfirst], nlast * sizeof(item[0]));
	}

	atomic_store_explicit(&r->r_producer, next, memory_order_release);
	if (rvp_ring_nfull(r) * 2 == rvp_ring_capacity(r) + 1) {
		rvp_ring_request_service(r);
	}
}

static inline void
rvp_ring_put_buf(rvp_ring_t *r, rvp_buf_t b)
{
	rvp_ring_put_multiple(r, &b.b_word[0], b.b_nwords);
}

#endif /* _RVP_RING_H_ */
