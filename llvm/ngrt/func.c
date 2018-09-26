#include "init.h"
#include "func.h"
#include "thread.h"
#include "trace.h"

const void *
__rvpredict_func_entry(const void *cfa, const void *callsite)
{
	if (__predict_false(!ring_operational()))
		return __builtin_return_address(0);

	rvp_ring_t *r = rvp_ring_for_curthr();
	const void *retaddr = __builtin_return_address(0);

#if !defined(use_cursor)
	rvp_buf_t b = RVP_BUF_INITIALIZER;
	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, retaddr, RVP_OP_ENTERFN);
	rvp_buf_put_voidptr(&b, cfa);
	rvp_buf_put_voidptr(&b, callsite);
	rvp_ring_put_buf(r, b);
#else
	rvp_cursor_t c = rvp_cursor_for_ring(r);
	rvp_cursor_put_pc_and_op(&c, &r->r_lastpc, retaddr, RVP_OP_ENTERFN);
	rvp_cursor_put_voidptr(&c, cfa);
	rvp_cursor_put_voidptr(&c, callsite);
	rvp_ring_advance_to_cursor(r, &c);
#endif

	return retaddr;
}

void
__rvpredict_func_exit(const void *retaddr)
{
	if (__predict_false(!ring_operational()))
		return;

	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;

	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, retaddr, RVP_OP_EXITFN);
	rvp_ring_put_buf(r, b);
}
