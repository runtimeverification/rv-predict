#include "func.h"
#include "thread.h"
#include "trace.h"

void
__rvpredict_func_entry(const void *retaddr)
{
	rvp_ring_t *r = rvp_ring_for_curthr();

	rvp_ring_put_pc_and_op(r, retaddr, RVP_OP_ENTERFN);
}

void
__rvpredict_func_exit(void)
{
	rvp_ring_t *r = rvp_ring_for_curthr();

	rvp_ring_put_pc_and_op(r, __builtin_return_address(1), RVP_OP_EXITFN);
}
