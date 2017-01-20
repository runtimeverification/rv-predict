#include "func.h"
#include "thread.h"
#include "trace.h"

void
__rvpredict_func_entry(const void *retaddr)
{
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;

	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, retaddr, RVP_OP_ENTERFN);
	rvp_ring_put_buf(r, b);
}

void
__rvpredict_func_exit(void)
{
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;

	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, __builtin_return_address(0),
	    RVP_OP_EXITFN);
	rvp_ring_put_buf(r, b);
}
