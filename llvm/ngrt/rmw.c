#include "access.h"
#include "atomic.h"
#include "notimpl.h"
#include "ring.h"
#include "thread.h"
#include "trace.h"

/* TBD emit requisite changes of generation. */
static inline void
trace_atomic_rmw4(const void *retaddr, rvp_addr_t addr,
    uint32_t oval, uint32_t nval, int32_t memory_order __unused)
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
__rvpredict_atomic_fetch_add4(volatile _Atomic uint32_t *addr,
    uint32_t oval, uint32_t arg, int32_t memory_order __unused)
{
	trace_atomic_rmw4(
	    __builtin_return_address(0), (rvp_addr_t)addr, oval, oval + arg,
	    memory_order);
}

/* T fn(T *addr, T expected, T desired,
 *      int32_t memory_order_success,
 *      int32_t memory_order_failure)
 */
uint8_t
__rvpredict_atomic_cas1(volatile _Atomic uint8_t *addr __unused,
    uint8_t expected __unused, uint8_t desired __unused,
    int32_t memory_order_success __unused,
    int32_t memory_order_failure __unused)
{
	not_implemented(__func__);
	return 0;
}

uint16_t
__rvpredict_atomic_cas2(volatile _Atomic uint16_t *addr __unused,
    uint16_t expected __unused, uint16_t desired __unused,
    int32_t memory_order_success __unused,
    int32_t memory_order_failure __unused)
{
	not_implemented(__func__);
	return 0;
}

uint32_t
__rvpredict_atomic_cas4(volatile _Atomic uint32_t *addr,
    uint32_t expected, uint32_t desired,
    int32_t memory_order_success __unused,
    int32_t memory_order_failure __unused)
{
	if (atomic_compare_exchange_strong_explicit(addr, &expected, desired,
	    memory_order_success, memory_order_failure)) {
		trace_atomic_rmw4(__builtin_return_address(0),
		    (rvp_addr_t)addr, expected, desired, memory_order_success);
	} else {
		/* `expected` took the unexpected value that was found
		 * at `addr`
		 */
		/* TBD pass memory_order_failure */
		trace_load(__builtin_return_address(0), RVP_OP_ATOMIC_LOAD4,
		    (rvp_addr_t)addr, expected);
	}
	return expected;
}

uint64_t
__rvpredict_atomic_cas8(volatile _Atomic uint64_t *addr __unused,
    uint64_t expected __unused, uint64_t desired __unused,
    int32_t memory_order_success __unused,
    int32_t memory_order_failure __unused)
{
	not_implemented(__func__);
	return 0;
}

rvp_uint128_t
__rvpredict_atomic_cas16(volatile _Atomic rvp_uint128_t *addr __unused,
    rvp_uint128_t expected __unused, rvp_uint128_t desired __unused,
    int32_t memory_order_success __unused,
    int32_t memory_order_failure __unused)
{
	not_implemented(__func__);
	return (rvp_uint128_t){.elts = {0, 0}};
}
