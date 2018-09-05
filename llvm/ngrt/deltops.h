/* Copyright (c) 2017, 2018 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_DELTOPS_H_
#define _RVP_DELTOPS_H_

#include "tracefmt.h"

extern __section(".text") deltops_t deltops;

static inline deltop_t *
rvp_vec_and_op_to_deltop(int jmpvec, rvp_op_t op)
{
	deltop_t *deltop =
	    &deltops.matrix[__arraycount(deltops.matrix) / 2 + jmpvec][op];

	if (deltop < &deltops.matrix[0][0] ||
		     &deltops.matrix[RVP_NJMPS - 1][RVP_NOPS - 1] < deltop)
		return NULL;
	
	return deltop;
}

#endif /* _RVP_DELTOPS_H_ */
