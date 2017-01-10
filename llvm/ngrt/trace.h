/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_TRACE_H_
#define _RVP_TRACE_H_

#include "ring.h"

typedef enum _rvp_op {
	  RVP_OP_BEGIN		=  0	// start of a thread
	, RVP_OP_LEGEND	= RVP_OP_BEGIN	// alias for 'begin'
	, RVP_OP_END		=  1	// thread termination
	, RVP_OP_LOAD1		=  2	// load: 1, 2, 4, 8, 16 bytes wide
	, RVP_OP_LOAD2		=  3
	, RVP_OP_LOAD4		=  4
	, RVP_OP_LOAD8		=  5
	, RVP_OP_LOAD16	=  6
	, RVP_OP_STORE1	=  7	// store: 1, 2, 4, 8, 16 bytes wide
	, RVP_OP_STORE2	=  8
	, RVP_OP_STORE4	=  9
	, RVP_OP_STORE8	= 10
	, RVP_OP_STORE16	= 11
	, RVP_OP_FORK		= 12	// create a new thread
	, RVP_OP_JOIN		= 13	// join an existing thread
	, RVP_OP_ACQUIRE	= 14	// acquire lock
	, RVP_OP_RELEASE	= 15	// release lock
	, RVP_OP_ENTERFN	= 16	// enter a function
	, RVP_OP_EXITFN	= 17	// exit a function
	, RVP_OP_SWITCH	= 18	// switch thread context

	, RVP_OP_ATOMIC_RMW1	= 19	// atomic read-modify-write:
					// 1, 2, 4, 8, 16 bytes wide
	, RVP_OP_ATOMIC_RMW2	= 20
	, RVP_OP_ATOMIC_RMW4	= 21
	, RVP_OP_ATOMIC_RMW8	= 22
	, RVP_OP_ATOMIC_RMW16	= 23

	, RVP_OP_ATOMIC_LOAD1	= 24	// atomic load:
					// 1, 2, 4, 8, 16 bytes wide
	, RVP_OP_ATOMIC_LOAD2	= 25
	, RVP_OP_ATOMIC_LOAD4	= 26
	, RVP_OP_ATOMIC_LOAD8	= 27
	, RVP_OP_ATOMIC_LOAD16	= 28

	, RVP_OP_ATOMIC_STORE1	= 29	// atomic store:
					// 1, 2, 4, 8, 16 bytes wide
	, RVP_OP_ATOMIC_STORE2	= 30
	, RVP_OP_ATOMIC_STORE4	= 31
	, RVP_OP_ATOMIC_STORE8	= 32
	, RVP_OP_ATOMIC_STORE16	= 33
	, RVP_NOPS
} rvp_op_t;

#define	RVP_NJMPS	256

typedef const char deltop_t;

int rvp_trace_open(void);

deltop_t *rvp_vec_and_op_to_deltop(int, rvp_op_t);
void rvp_ring_put_addr(rvp_ring_t *, const void *);
void rvp_ring_put_begin(rvp_ring_t *, uint32_t);
void rvp_ring_put_pc_and_op(rvp_ring_t *, const char *, rvp_op_t);
void rvp_ring_put_u64(rvp_ring_t *, uint64_t);

#endif /* _RVP_TRACE_H_ */
