/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#include "access.h"
#include "tracefmt.h"
#include "vptr.h"

void
__rvpredict_vptr_update(void **vptr, void *v)
{
	rvp_op_t op;
	const void *retaddr = __builtin_return_address(0);

	switch (sizeof(rvp_addr_t)) {
	case 1:
		op = RVP_OP_STORE1;
		break;
	case 2:
		op = RVP_OP_STORE2;
		break;
	case 4:
		op = RVP_OP_STORE4;
		break;
	case 8:
		trace_store8(retaddr, RVP_OP_STORE8, (rvp_addr_t)vptr,
		    (rvp_addr_t)v);
		return;
	default:
		errx(EXIT_FAILURE, "%s: unimplemented pointer width %zu",
		    __func__, sizeof(rvp_addr_t));
	}
	trace_store(retaddr, op, (rvp_addr_t)vptr, (rvp_addr_t)v);
}

void
__rvpredict_vptr_load(void **vptr, void *v)
{
	rvp_op_t op;
	const void *retaddr = __builtin_return_address(0);

	switch (sizeof(rvp_addr_t)) {
	case 1:
		op = RVP_OP_LOAD1;
		break;
	case 2:
		op = RVP_OP_LOAD2;
		break;
	case 4:
		op = RVP_OP_LOAD4;
		break;
	case 8:
		trace_load8(retaddr, RVP_OP_LOAD8, (rvp_addr_t)vptr,
		    (rvp_addr_t)v);
		return;
	default:
		errx(EXIT_FAILURE, "%s: unimplemented pointer width %zu",
		    __func__, sizeof(rvp_addr_t));
	}
	trace_load(retaddr, op, (rvp_addr_t)vptr, (rvp_addr_t)v);
}
