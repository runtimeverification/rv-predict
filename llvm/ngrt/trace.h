/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_TRACE_H_
#define _RVP_TRACE_H_

#include "ring.h"
#include "tracefmt.h"

int rvp_trace_open(void);

deltop_t *rvp_vec_and_op_to_deltop(int, rvp_op_t);
void rvp_ring_put_addr(rvp_ring_t *, const void *);
void rvp_ring_put_begin(rvp_ring_t *, uint32_t);
void rvp_ring_put_pc_and_op(rvp_ring_t *, const char *, rvp_op_t);
void rvp_ring_put_u64(rvp_ring_t *, uint64_t);

#endif /* _RVP_TRACE_H_ */
