#include "access.h"
#include "nbcompat.h"
#include "notimpl.h"
#include "rvpint.h"
#include "thread.h"
#include "trace.h"

/* void fn(T *addr, T val) */
void
__rvpredict_load1(uint8_t *addr, uint8_t val)
{
	rvp_ring_t *r = rvp_ring_for_curthr();

	rvp_ring_put_pc_and_op(r, __builtin_return_address(0), RVP_OP_LOAD1);
	rvp_ring_put_addr(r, addr);
	rvp_ring_put(r, val);
}

void
__rvpredict_load2(uint16_t *addr, uint16_t val)
{
	rvp_ring_t *r = rvp_ring_for_curthr();

	rvp_ring_put_pc_and_op(r, __builtin_return_address(0), RVP_OP_LOAD2);
	rvp_ring_put_addr(r, addr);
	rvp_ring_put(r, val);
}

void
__rvpredict_load4(uint32_t *addr, uint32_t val)
{
	rvp_ring_t *r = rvp_ring_for_curthr();

	rvp_ring_put_pc_and_op(r, __builtin_return_address(0), RVP_OP_LOAD4);
	rvp_ring_put_addr(r, addr);
	rvp_ring_put(r, val);
}

void
__rvpredict_load8(uint64_t *addr, uint64_t val)
{
	rvp_ring_t *r = rvp_ring_for_curthr();

	rvp_ring_put_pc_and_op(r, __builtin_return_address(0), RVP_OP_LOAD8);
	rvp_ring_put_addr(r, addr);
	rvp_ring_put_u64(r, val);

}

void
__rvpredict_load16(rvp_uint128_t *addr, rvp_uint128_t val)
{
	not_implemented(__func__);
}


/* void fn(T *addr, T val) */
void
__rvpredict_store1(uint8_t *addr, uint8_t val)
{
	rvp_ring_t *r = rvp_ring_for_curthr();

	rvp_ring_put_pc_and_op(r, __builtin_return_address(0), RVP_OP_STORE1);
	rvp_ring_put_addr(r, addr);
	rvp_ring_put(r, val);
}

void
__rvpredict_store2(uint16_t *addr, uint16_t val)
{
	rvp_ring_t *r = rvp_ring_for_curthr();

	rvp_ring_put_pc_and_op(r, __builtin_return_address(0), RVP_OP_STORE2);
	rvp_ring_put_addr(r, addr);
	rvp_ring_put(r, val);
}

void
__rvpredict_store4(uint32_t *addr, uint32_t val)
{
	rvp_ring_t *r = rvp_ring_for_curthr();

	rvp_ring_put_pc_and_op(r, __builtin_return_address(0), RVP_OP_STORE4);
	rvp_ring_put_addr(r, addr);
	rvp_ring_put(r, val);
}

void
__rvpredict_store8(uint64_t *addr, uint64_t val)
{
	rvp_ring_t *r = rvp_ring_for_curthr();

	rvp_ring_put_pc_and_op(r, __builtin_return_address(0), RVP_OP_STORE8);
	rvp_ring_put_addr(r, addr);
	rvp_ring_put_u64(r, val);
}

void
__rvpredict_store16(rvp_uint128_t *addr, rvp_uint128_t val)
{
	not_implemented(__func__);
}
