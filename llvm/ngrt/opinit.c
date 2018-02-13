#include "init.h"
#include "trace.h"

rvp_jumpless_op_t rvp_jumpless_op;

void
rvp_deltop_init(void)
{
	rvp_jumpless_op.jo_sigdepth =
	    (rvp_addr_t)rvp_vec_and_op_to_deltop(0, RVP_OP_SIGDEPTH);
	rvp_jumpless_op.jo_switch =
	    (rvp_addr_t)rvp_vec_and_op_to_deltop(0, RVP_OP_SWITCH);
}

