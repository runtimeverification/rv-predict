/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#include "init.h"
#include "aligned.h"
#include "atomic.h"
#include "cas.h"
#include "exchange.h"
#include "notimpl.h"
#include "ring.h"
#include "thread.h"
#include "unaligned.h"

extern const char __rvpredict_cov_begin;
extern const char __rvpredict_cov_end;

/* Return true if we should not trace this variable because
 * it belongs to the LLVM coverage runtime.  Otherwise, return false.
 */
static inline bool
data_is_in_coverage(rvp_addr_t addr)
{
	return (uintptr_t)&__rvpredict_cov_begin <= (uintptr_t)addr &&
	       (uintptr_t)addr <= (uintptr_t)&__rvpredict_cov_end;
}

static inline void
trace_load(const char *retaddr, rvp_op_t op, rvp_addr_t addr, uint32_t val)
{
	if (__predict_false(data_is_in_coverage(addr) || !ring_operational()))
		return;

	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;

	rvp_buf_trace_load_cog(&b, &r->r_lgen);
	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, retaddr, op);
	rvp_buf_put_addr(&b, addr);
	rvp_buf_put(&b, val);
	rvp_ring_put_buf(r, b);
}

static inline void
trace_load8(const char *retaddr, rvp_op_t op, rvp_addr_t addr, uint64_t val)
{
	if (__predict_false(data_is_in_coverage(addr) || !ring_operational()))
		return;

	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;

	rvp_buf_trace_load_cog(&b, &r->r_lgen);
	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, retaddr, op);
	rvp_buf_put_addr(&b, addr);
	rvp_buf_put_u64(&b, val);
	rvp_ring_put_buf(r, b);
}

static inline void
trace_store(const char *retaddr, rvp_op_t op, rvp_addr_t addr, uint32_t val)
{
	if (__predict_false(data_is_in_coverage(addr) || !ring_operational()))
		return;

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

static inline void
trace_store8(const char *retaddr, rvp_op_t op, rvp_addr_t addr, uint64_t val)
{
	if (__predict_false(data_is_in_coverage(addr) || !ring_operational()))
		return;

	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;
	uint64_t gen;

	gen = rvp_ggen_before_store();
	atomic_thread_fence(memory_order_acquire);
	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, retaddr, op);
	rvp_buf_put_addr(&b, addr);
	rvp_buf_put_u64(&b, val);
	rvp_buf_trace_cog(&b, &r->r_lgen, gen);
	rvp_ring_put_buf(r, b);
}
