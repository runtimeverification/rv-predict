/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_TRACEFMT_H_
#define _RVP_TRACEFMT_H_

#include <stdint.h>

#include "nbcompat.h"

#if defined(RVP_ADDR_TYPE)
typedef RVP_ADDR_TYPE rvp_addr_t;
#else
typedef uintptr_t rvp_addr_t;
#endif

/* RV-Predict trace file header.  Located at byte 0 of a trace file.  The
 * trace starts at the first rvp_trace_t-sized boundary after the header,
 * and it ends at EOF.
 */
struct _rvp_trace_header {
	char th_magic[4];               // 'R' 'V' 'P' '_'
					//
	uint8_t th_version[4];          // major minor teeny tiny
					// 0     0     0     1
					//
	uint32_t th_byteorder;          // byte-order indication,
					// see discussion
					//
	uint8_t th_pointer_width;       // width of a pointer, in bytes
					//
	uint8_t th_data_width;          // default data width, in bytes
					//
	uint8_t th_pad1[2];
} __aligned(sizeof(uint32_t)) __packed;

typedef struct _rvp_trace_header rvp_trace_header_t;

typedef enum _rvp_op {
	  RVP_OP_BEGIN		=  0	// start of a thread
	, RVP_OP_LEGEND	= RVP_OP_BEGIN	// alias for 'begin'
	, RVP_OP_END		=  1	// thread termination
	, RVP_OP_LOAD1		=  2	// load: 1, 2, 4, 8, 16 bytes wide
	, RVP_OP_LOAD2		=  3
	, RVP_OP_LOAD4		=  4
	, RVP_OP_LOAD8		=  5
	, RVP_OP_LOAD16		=  6
	, RVP_OP_STORE1		=  7	// store: 1, 2, 4, 8, 16 bytes wide
	, RVP_OP_STORE2		=  8
	, RVP_OP_STORE4		=  9
	, RVP_OP_STORE8		= 10
	, RVP_OP_STORE16	= 11
	, RVP_OP_FORK		= 12	// create a new thread
	, RVP_OP_JOIN		= 13	// join an existing thread
	, RVP_OP_ACQUIRE	= 14	// acquire lock
	, RVP_OP_RELEASE	= 15	// release lock
	, RVP_OP_ENTERFN	= 16	// enter a function
	, RVP_OP_EXITFN		= 17	// exit a function
	, RVP_OP_SWITCH		= 18	// switch thread context

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
	, RVP_OP_COG		= 34	// change of generation
	, RVP_OP_SIGEST		= 35	// establish signal action
	, RVP_OP_ENTERSIG	= 36	// signal delivery
	, RVP_OP_EXITSIG	= 37
	, RVP_OP_SIGDIS		= 38	// disestablish signal action
	, RVP_OP_SIGMASKMEMO	= 39	// establish a new number -> mask
					// mapping (memoize mask)
	, RVP_OP_SIGSETMASK	= 40	// set signal mask
	, RVP_OP_SIGDEPTH	= 41	// set the number of signals
					// running concurrently on the
					// current thread.
	, RVP_OP_SIGBLOCK	= 42	// block signals
	, RVP_OP_SIGUNBLOCK	= 43	// unblock signals
	, RVP_OP_SIGGETSETMASK	= 44	// get old mask, set new mask
	, RVP_OP_SIGGETMASK	= 45	// get current mask
	, RVP_NOPS
} rvp_op_t;

#define	RVP_NJMPS	256

typedef const char deltop_t;

struct _deltops {
	char rsvd;
	deltop_t matrix[RVP_NJMPS][RVP_NOPS];
};

typedef struct _deltops deltops_t;

typedef struct {
	rvp_addr_t deltop;
	uint64_t generation;
} __packed __aligned(sizeof(uint32_t)) rvp_cog_t;

typedef struct {
	rvp_addr_t deltop;
	uint32_t tid;
	uint64_t generation;
} __packed __aligned(sizeof(uint32_t)) rvp_begin_t;

typedef struct {
	rvp_addr_t deltop;
	uint32_t tid;
} __packed __aligned(sizeof(uint32_t)) rvp_fork_join_switch_t;

typedef struct {
	rvp_addr_t deltop;
	rvp_addr_t handler;
	uint64_t generation;
	uint32_t signum;
} __packed __aligned(sizeof(uint32_t)) rvp_entersig_t;

typedef struct {
	rvp_addr_t deltop;
	uint32_t depth;
} __packed __aligned(sizeof(uint32_t)) rvp_sigdepth_t;

typedef struct {
	rvp_addr_t deltop;
	uint32_t masknum;
} __packed __aligned(sizeof(uint32_t)) rvp_sigmask_access_t;

typedef struct {
	rvp_addr_t deltop;
	uint32_t omasknum;
	uint32_t masknum;
} __packed __aligned(sizeof(uint32_t)) rvp_sigmask_rmw_t;

typedef struct {
	rvp_addr_t deltop;
	uint64_t mask;
	uint32_t origin;	// this should probably be constant throughout
				// a trace.  set up in header?
	uint32_t masknum;
} __packed __aligned(sizeof(uint32_t)) rvp_sigmaskmemo_t;

typedef struct {
	rvp_addr_t deltop;
	rvp_addr_t handler;
	uint32_t signum;
	uint32_t masknum;
} __packed __aligned(sizeof(uint32_t)) rvp_sigest_t;

typedef struct {
	rvp_addr_t deltop;
	uint32_t signum;
} __packed __aligned(sizeof(uint32_t)) rvp_sigdis_t;

/* Assumes a data width of 32 bits. */
typedef struct {
	rvp_addr_t deltop;
	rvp_addr_t addr;
	uint32_t onval;
} __packed __aligned(sizeof(uint32_t)) rvp_rmw1_2_t;

/* Assumes a data width of 32 bits. */
typedef struct {
	rvp_addr_t deltop;
	rvp_addr_t addr;
	uint32_t oval, nval;
} __packed __aligned(sizeof(uint32_t)) rvp_rmw4_t;

/* Assumes a data width of 64 bits or less. */
typedef struct {
	rvp_addr_t deltop;
	rvp_addr_t addr;
	uint64_t oval, nval;
} __packed __aligned(sizeof(uint32_t)) rvp_rmw8_t;

typedef struct {
	rvp_addr_t deltop;
	rvp_addr_t addr;
	uint32_t data;
} __packed __aligned(sizeof(uint32_t)) rvp_load1_2_4_store1_2_4_t;

typedef struct {
	rvp_addr_t deltop;
	rvp_addr_t addr;
	uint64_t data;
} __packed __aligned(sizeof(uint32_t)) rvp_load8_store8_t;

typedef struct {
	rvp_addr_t deltop;
} __packed __aligned(sizeof(uint32_t)) rvp_end_exitfn_t;

typedef struct {
	rvp_addr_t deltop;
	rvp_addr_t cfa;
	rvp_addr_t callsite;
} __packed __aligned(sizeof(uint32_t)) rvp_enterfn_t;

typedef struct {
	rvp_addr_t deltop;
} rvp_exitsig_t;

typedef struct {
	rvp_addr_t deltop;
	rvp_addr_t addr;
} __packed __aligned(sizeof(uint32_t)) rvp_acquire_release_t;

#endif /* _RVP_TRACEFMT_H_ */
