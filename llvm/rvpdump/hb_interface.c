#include <umang's hb stuff here>

#include "reader.h"

extern "C" void hb_perform_nop(const rvp_pstate_t *ps, const rvp_ubuf_t *ub)
{
	hb_object *hb = (safe cast here)ps;
	hb->perform_nop(ps, ub);
}

extern "C" void hb_perform_op(const rvp_pstate_t *ps, const rvp_ubuf_t *ub,
    rvp_op_t op, bool is_load, int field_width)
{
	hb_object *hb = (safe cast here)ps;
	hb->perform_op(ps, ub, op);
}


