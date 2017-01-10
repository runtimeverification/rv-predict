#include "atomic.h"
#include "ring.h"
#include "thread.h"
#include "trace.h"

void
__rvpredict_atomic_load4(uint32_t *addr, uint32_t val, int32_t memory_order)
{
	rvp_ring_t *r = rvp_ring_for_curthr();

	rvp_ring_put_pc_and_op(r, __builtin_return_address(1),
	    RVP_OP_ATOMIC_LOAD4);
	rvp_ring_put_addr(r, addr);
	rvp_ring_put(r, val);
}

void
__rvpredict_atomic_store4(uint32_t *addr, uint32_t val, int32_t memory_order)
{
	rvp_ring_t *r = rvp_ring_for_curthr();

	rvp_ring_put_pc_and_op(r, __builtin_return_address(1),
	    RVP_OP_ATOMIC_STORE4);
	rvp_ring_put_addr(r, addr);
	rvp_ring_put(r, val);
}

static inline void
rvp_trace_atomic_rmw4(const void *retaddr, uint32_t *addr, uint32_t oval,
    uint32_t nval, int32_t memory_order)
{
	rvp_ring_t *r = rvp_ring_for_curthr();

	rvp_ring_put_pc_and_op(r, retaddr, RVP_OP_ATOMIC_RMW4);
	rvp_ring_put_addr(r, addr);
	rvp_ring_put(r, oval);
	rvp_ring_put(r, nval);
}

void
__rvpredict_atomic_fetch_add4(uint32_t *addr, uint32_t oval, uint32_t arg,
    int32_t memory_order)
{
	rvp_trace_atomic_rmw4(
	    __builtin_return_address(1), addr, oval, oval + arg, memory_order);
}
