/* Copyright (c) 2017, 2018 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_DELTOPS_H_
#define _RVP_DELTOPS_H_

#include "tracefmt.h"

extern __section(".text") deltops_t deltops;

static inline deltop_t *
rvp_vec_and_op_to_deltop(int jmpvec, rvp_op_t op)
{
	const int halfjmps = RVP_NJMPS / 2;

	if (__predict_true(-halfjmps <= jmpvec && jmpvec < halfjmps &&
	                   0 <= op && op < RVP_NOPS))
		return &deltops.matrix[halfjmps + jmpvec][op];
	return NULL;
}

#endif /* _RVP_DELTOPS_H_ */
