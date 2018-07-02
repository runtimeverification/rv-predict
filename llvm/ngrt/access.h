/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#include "aligned.h"
#include "atomic.h"
#include "cas.h"
#include "exchange.h"
#include "notimpl.h"
#include "ring.h"
#include "thread.h"
#include "unaligned.h"

#if 1
extern struct llvm_profile_data __start___llvm_prf_data;
extern struct llvm_profile_data __stop___llvm_prf_data;

extern char __rvpredict_cov_begin;
extern uint64_t __start___llvm_prf_cnts;
extern uint64_t __stop___llvm_prf_cnts;
extern char __start___llvm_prf_names;
extern char __stop___llvm_prf_names;
extern char __rvpredict_cov_end;
/* do not trace LLVM coverage runtime counters */
static inline int
no_trace(rvp_addr_t addr)
{        /* return true if we should not trace this variable */
         if ( 
              ((void*) addr) >= ((void*) & __rvpredict_cov_begin) 
                 &&
              ((void*) addr) <= ((void*) & __rvpredict_cov_end) 
            )
         {
              return 1; /* Its sn LLVM coverage variable - do not trace */
         } else {
              return 0;
         }
}
#endif

static inline void
trace_load(const char *retaddr, rvp_op_t op, rvp_addr_t addr, uint32_t val)
{
#if 1
       if(no_trace(addr))
        {
             return;
        }
#endif
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;

	rvp_buf_trace_load_cog(&b, &r->r_lgen);
	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, retaddr, op);
	rvp_buf_put_addr(&b, addr);
	rvp_buf_put(&b, val);
	rvp_ring_put_buf(r, b);
}

static inline void
trace_load8(const char *retaddr, rvp_op_t op, rvp_addr_t addr, uint64_t val)
{
#if 1
       if(no_trace(addr))
        {
             return;
        }
#endif
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;

	rvp_buf_trace_load_cog(&b, &r->r_lgen);
	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, retaddr, op);
	rvp_buf_put_addr(&b, addr);
	rvp_buf_put_u64(&b, val);
	rvp_ring_put_buf(r, b);
}

static inline void
trace_store(const char *retaddr, rvp_op_t op, rvp_addr_t addr, uint32_t val)
{
#if 1
       if(no_trace(addr))
        {
             return;
        }
#endif
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;
	uint64_t gen;

	gen = rvp_ggen_before_store();
	atomic_thread_fence(memory_order_acquire);
	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, retaddr, op);
	rvp_buf_put_addr(&b, addr);
	rvp_buf_put(&b, val);
	rvp_buf_trace_cog(&b, &r->r_lgen, gen);
	rvp_ring_put_buf(r, b);
}

static inline void
trace_store8(const char *retaddr, rvp_op_t op, rvp_addr_t addr, uint64_t val)
{
#if 1
       if(no_trace(addr))
        {
            return;
        }
#endif
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_t b = RVP_BUF_INITIALIZER;
	uint64_t gen;

	gen = rvp_ggen_before_store();
	atomic_thread_fence(memory_order_acquire);
	rvp_buf_put_pc_and_op(&b, &r->r_lastpc, retaddr, op);
	rvp_buf_put_addr(&b, addr);
	rvp_buf_put_u64(&b, val);
	rvp_buf_trace_cog(&b, &r->r_lgen, gen);
	rvp_ring_put_buf(r, b);
}
