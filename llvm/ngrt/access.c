#include "access.h"
#include "nbcompat.h"
#include "notimpl.h"
#include "rvpint.h"
#include "thread.h"
#include "trace.h"

__weak_alias(__rvpredict_unaligned_load1, __rvpredict_load1)
__weak_alias(__rvpredict_unaligned_load2, __rvpredict_load2)
__weak_alias(__rvpredict_unaligned_load4, __rvpredict_load4)
__weak_alias(__rvpredict_unaligned_load8, __rvpredict_load8)
__weak_alias(__rvpredict_unaligned_load16, __rvpredict_load16)

__weak_alias(__rvpredict_unaligned_store1, __rvpredict_store1)
__weak_alias(__rvpredict_unaligned_store2, __rvpredict_store2)
__weak_alias(__rvpredict_unaligned_store4, __rvpredict_store4)
__weak_alias(__rvpredict_unaligned_store8, __rvpredict_store8)
__weak_alias(__rvpredict_unaligned_store16, __rvpredict_store16)

void
__rvpredict_atomic_load1(uint8_t *addr, uint8_t val, int32_t memory_order __unused)
{
	trace_load(__builtin_return_address(0), RVP_OP_ATOMIC_LOAD1,
	    (rvp_addr_t)addr, val);
}

void
__rvpredict_atomic_load2(uint16_t *addr, uint16_t val, int32_t memory_order __unused)
{
	trace_load(__builtin_return_address(0), RVP_OP_ATOMIC_LOAD2,
	    (rvp_addr_t)addr, val);
}

void
__rvpredict_atomic_load4(uint32_t *addr, uint32_t val, int32_t memory_order __unused)
{
	trace_load(__builtin_return_address(0), RVP_OP_ATOMIC_LOAD4,
	    (rvp_addr_t)addr, val);
}

void
__rvpredict_atomic_load8(uint64_t *addr, uint64_t val, int32_t memory_order __unused)
{
	trace_load8(__builtin_return_address(0), RVP_OP_ATOMIC_LOAD8,
	    (rvp_addr_t)addr, val);
}

/* void fn(T *addr, T val) */
void
__rvpredict_store1(uint8_t *addr, uint8_t val)
{
	trace_store(__builtin_return_address(0), RVP_OP_STORE1,
	    (rvp_addr_t)addr, val);
}

void
__rvpredict_store2(uint16_t *addr, uint16_t val)
{
	trace_store(__builtin_return_address(0), RVP_OP_STORE2,
	    (rvp_addr_t)addr, val);
}

void
__rvpredict_store4(uint32_t *addr, uint32_t val)
{
	trace_store(__builtin_return_address(0), RVP_OP_STORE4,
	    (rvp_addr_t)addr, val);
}

void
__rvpredict_atomic_store1(uint8_t *addr, uint8_t val, int32_t memory_order __unused)
{
	trace_store(__builtin_return_address(0), RVP_OP_ATOMIC_STORE1,
	    (rvp_addr_t)addr, val);
}

void
__rvpredict_atomic_store2(uint16_t *addr, uint16_t val, int32_t memory_order __unused)
{
	trace_store(__builtin_return_address(0), RVP_OP_ATOMIC_STORE2,
	    (rvp_addr_t)addr, val);
}

void
__rvpredict_atomic_store4(uint32_t *addr, uint32_t val, int32_t memory_order __unused)
{
	trace_store(__builtin_return_address(0), RVP_OP_ATOMIC_STORE4,
	    (rvp_addr_t)addr, val);
}

void
__rvpredict_atomic_store8(uint64_t *addr, uint64_t val,
    int32_t memory_order __unused)
{
	trace_store8(__builtin_return_address(0), RVP_OP_ATOMIC_STORE8,
	    (rvp_addr_t)addr, val);
}

void
__rvpredict_store8(uint64_t *addr, uint64_t val)
{
	trace_store8(__builtin_return_address(0), RVP_OP_STORE8,
	    (rvp_addr_t)addr, val);
}

void
__rvpredict_store16(rvp_uint128_t *addr __unused, rvp_uint128_t val __unused)
{
	int i;
	const char *retaddr = __builtin_return_address(0);

	for (i = 0; i < __arraycount(addr->elts); i++) {
		trace_store8(retaddr, RVP_OP_STORE8,
		    (rvp_addr_t)&addr->elts[i], val.elts[i]);
	}
}
