/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_TRACE_H_
#define _RVP_TRACE_H_

#include "ring.h"
#include "tracefmt.h"

typedef struct _rvp_jumpless_op {
	rvp_addr_t jo_sigdepth;
	rvp_addr_t jo_switch;
} rvp_jumpless_op_t;

int rvp_trace_begin(void);

deltop_t *rvp_vec_and_op_to_deltop(int, rvp_op_t);
void rvp_ring_put_addr(rvp_ring_t *, const void *);
void rvp_ring_put_begin(rvp_ring_t *, uint32_t, uint64_t);
void rvp_ring_put_pc_and_op(rvp_ring_t *, const char *, rvp_op_t);
void rvp_ring_put_u64(rvp_ring_t *, uint64_t);

extern int64_t rvp_trace_size_limit;
extern int64_t rvp_trace_size;
extern rvp_jumpless_op_t rvp_jumpless_op;

static inline void
rvp_ring_reset_pc(rvp_ring_t *r)
{
	r->r_lastpc = rvp_vec_and_op_to_deltop(0, RVP_OP_BEGIN);
}

#endif /* _RVP_TRACE_H_ */
