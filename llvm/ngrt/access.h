/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#include "aligned.h"
#include "atomic.h"
#include "cas.h"
#include "exchange.h"
#include "notimpl.h"
#include "ring.h"
#include "thread.h"
#include "unaligned.h"

static inline void
trace_load(const char *retaddr, rvp_op_t op, rvp_addr_t addr, uint32_t val)
{
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;

	rvp_buf_trace_load_cog(&b, &r->r_lgen);
	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, retaddr, op);
	rvp_buf_put_addr(&b, addr);
	rvp_buf_put(&b, val);
	rvp_ring_put_buf(r, b);
}

static inline void
trace_store(const char *retaddr, rvp_op_t op, rvp_addr_t addr, uint32_t val)
{
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;
	uint64_t gen;

	gen = rvp_ggen_before_store();
	atomic_thread_fence(memory_order_acquire);
	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, retaddr, op);
	rvp_buf_put_addr(&b, addr);
	rvp_buf_put(&b, val);
	rvp_buf_trace_cog(&b, &r->r_lgen, gen);
	rvp_ring_put_buf(r, b);
}
