#include "atomic.h"
#include "ring.h"
#include "thread.h"
#include "trace.h"

static inline void
trace_atomic_rmw4(const void *retaddr, uint32_t *addr, uint32_t oval,
    uint32_t nval, int32_t memory_order)
{
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;

	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, retaddr, RVP_OP_ATOMIC_RMW4);
	rvp_buf_put_addr(&b, addr);
	rvp_buf_put(&b, oval);
	rvp_buf_put(&b, nval);
	rvp_ring_put_buf(r, b);
}

void
__rvpredict_atomic_fetch_add4(uint32_t *addr, uint32_t oval, uint32_t arg,
    int32_t memory_order)
{
	trace_atomic_rmw4(
	    __builtin_return_address(0), addr, oval, oval + arg, memory_order);
}
