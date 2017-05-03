#include "access.h"
#include "nbcompat.h"
#include "notimpl.h"
#include "rvpint.h"
#include "thread.h"
#include "trace.h"

static inline void
trace_load(const char *retaddr, rvp_op_t op, void *addr,
    uint32_t val)
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
trace_store(const char *retaddr, rvp_op_t op, void *addr,
    uint32_t val)
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

/* void fn(T *addr, T val) */
void
__rvpredict_load1(uint8_t *addr, uint8_t val)
{
	trace_load(__builtin_return_address(0), RVP_OP_LOAD1, addr, val);
}

void
__rvpredict_load2(uint16_t *addr, uint16_t val)
{
	trace_load(__builtin_return_address(0), RVP_OP_LOAD2, addr, val);
}

void
__rvpredict_load4(uint32_t *addr, uint32_t val)
{
	trace_load(__builtin_return_address(0), RVP_OP_LOAD4, addr, val);
}

void
__rvpredict_load8(uint64_t *addr, uint64_t val)
{
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;

	rvp_buf_trace_load_cog(&b, &r->r_lgen);
	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, __builtin_return_address(0),
	    RVP_OP_LOAD8);
	rvp_buf_put_addr(&b, addr);
	rvp_buf_put_u64(&b, val);

	rvp_ring_put_buf(r, b);
}

void
__rvpredict_load16(rvp_uint128_t *addr, rvp_uint128_t val)
{
	not_implemented(__func__);
}


void
__rvpredict_atomic_load1(uint8_t *addr, uint8_t val, int32_t memory_order)
{
	trace_load(__builtin_return_address(0), RVP_OP_ATOMIC_LOAD1, addr, val);
}

void
__rvpredict_atomic_load2(uint16_t *addr, uint16_t val, int32_t memory_order)
{
	trace_load(__builtin_return_address(0), RVP_OP_ATOMIC_LOAD2, addr, val);
}

void
__rvpredict_atomic_load4(uint32_t *addr, uint32_t val, int32_t memory_order)
{
	trace_load(__builtin_return_address(0), RVP_OP_ATOMIC_LOAD4, addr, val);
}

void
__rvpredict_atomic_load8(uint64_t *addr, uint64_t val, int32_t memory_order)
{
	trace_load(__builtin_return_address(0), RVP_OP_ATOMIC_LOAD8, addr, val);
}

/* void fn(T *addr, T val) */
void
__rvpredict_store1(uint8_t *addr, uint8_t val)
{
	trace_store(__builtin_return_address(0), RVP_OP_STORE1, addr, val);
}

void
__rvpredict_store2(uint16_t *addr, uint16_t val)
{
	trace_store(__builtin_return_address(0), RVP_OP_STORE2, addr, val);
}

void
__rvpredict_store4(uint32_t *addr, uint32_t val)
{
	trace_store(__builtin_return_address(0), RVP_OP_STORE4, addr, val);
}

void
__rvpredict_atomic_store1(uint8_t *addr, uint8_t val, int32_t memory_order)
{
	trace_store(__builtin_return_address(0), RVP_OP_ATOMIC_STORE1,
	    addr, val);
}

void
__rvpredict_atomic_store4(uint32_t *addr, uint32_t val, int32_t memory_order)
{
	trace_store(__builtin_return_address(0), RVP_OP_ATOMIC_STORE4,
	    addr, val);
}

void
__rvpredict_store8(uint64_t *addr, uint64_t val)
{
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;
	uint64_t gen;

	gen = rvp_ggen_before_store();
	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, __builtin_return_address(0),
	    RVP_OP_STORE8);
	rvp_buf_put_addr(&b, addr);
	rvp_buf_put_u64(&b, val);
	rvp_buf_trace_cog(&b, &r->r_lgen, gen);
	rvp_ring_put_buf(r, b);
}

void
__rvpredict_store16(rvp_uint128_t *addr, rvp_uint128_t val)
{
	not_implemented(__func__);
}
