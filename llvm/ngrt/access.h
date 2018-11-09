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
inline bool
data_is_in_coverage(rvp_addr_t addr)
{
	return (uintptr_t)&__rvpredict_cov_begin <= (uintptr_t)addr &&
	       (uintptr_t)addr <= (uintptr_t)&__rvpredict_cov_end;
}

inline void
trace_load(const char *retaddr, rvp_op_t op, rvp_addr_t addr, uint32_t val)
{
	if (data_is_in_coverage(addr) || !ring_operational())
		return;

	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;

	rvp_buf_trace_load_cog(&b, &r->r_lgen);
	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, retaddr, op);
	rvp_buf_put_addr(&b, addr);
	rvp_buf_put(&b, val);
	rvp_ring_put_buf(r, b);
}

inline void
trace_load8(const char *retaddr, rvp_op_t op, rvp_addr_t addr, uint64_t val)
{
	if (data_is_in_coverage(addr) || !ring_operational())
		return;

	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;

	rvp_buf_trace_load_cog(&b, &r->r_lgen);
	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, retaddr, op);
	rvp_buf_put_addr(&b, addr);
	rvp_buf_put_u64(&b, val);
	rvp_ring_put_buf(r, b);
}

/* void fn(T *addr, T val) */
inline void
__rvpredict_load1(uint8_t *addr, uint8_t val)
{
#if 1
	*addr = val;
#else
	trace_load(__builtin_return_address(0), RVP_OP_LOAD1,
	    (rvp_addr_t)addr, val);
#endif
}

inline void
__rvpredict_load2(uint16_t *addr, uint16_t val)
{
#if 1
	*addr = val;
#else
	trace_load(__builtin_return_address(0), RVP_OP_LOAD2,
	    (rvp_addr_t)addr, val);
#endif
}

inline void
__rvpredict_load4(uint32_t *addr, uint32_t val)
{
#if 1
	*addr = val;
#else
	trace_load(__builtin_return_address(0), RVP_OP_LOAD4,
	    (rvp_addr_t)addr, val);
#endif
}

inline void
__rvpredict_load8(uint64_t *addr, uint64_t val)
{
#if 1
	*addr = val;
#else
	trace_load8(__builtin_return_address(0), RVP_OP_LOAD8,
	    (rvp_addr_t)addr, val);
#endif
}

inline void
__rvpredict_load16(rvp_uint128_t *addr __unused, rvp_uint128_t val __unused)
{
	int i;
	const char *retaddr = __builtin_return_address(0);

	for (i = 0; i < __arraycount(addr->elts); i++) {
		trace_load8(retaddr, RVP_OP_LOAD8,
		    (rvp_addr_t)&addr->elts[i], val.elts[i]);
	}
}

static inline void
trace_store(const char *retaddr, rvp_op_t op, rvp_addr_t addr, uint32_t val)
{
	if (data_is_in_coverage(addr) || !ring_operational())
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
	if (data_is_in_coverage(addr) || !ring_operational())
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
