#ifndef _RVP_HB_HANDLERS_H_
#define _RVP_HB_HANDLERS_H_

#include "hb_state.h"
#include "event_type.h"

typedef union {
	rvp_addr_t ub_pc;
	rvp_begin_t ub_begin;
	rvp_fork_join_switch_t ub_fork_join_switch;
	rvp_load1_2_4_store1_2_4_t ub_load1_2_4_store1_2_4;
	rvp_acquire_release_t ub_acquire_release;
	rvp_load8_store8_t ub_load8_store8;
	rvp_cog_t ub_cog;
	rvp_entersig_t ub_entersig;
	rvp_exitsig_t ub_exitsig;
	rvp_sigdepth_t ub_sigdepth;
	rvp_sigest_t ub_sigest;
	rvp_sigdis_t ub_sigdis;
	rvp_sigmaskmemo_t ub_sigmaskmemo;
	rvp_sigmask_access_t ub_sigmask_access;
	rvp_sigmask_rmw_t ub_sigmask_rmw;
	rvp_rmw1_2_t ub_rmw1_2;
	rvp_rmw4_t ub_rmw4;
	rvp_rmw8_t ub_rmw8;
	rvp_enterfn_t ub_enterfn;
	char ub_bytes[4096];
} rvp_ubuf_t;

bool hb_handler(hb_state* state_ptr, uint32_t tid, rvp_op_t op, rvp_ubuf_t& decor);

bool hb_handler_read(hb_state* state_ptr, uint32_t tid, std::size_t t_idx, rvp_addr_t addr);

bool hb_handler_write(hb_state* state_ptr, uint32_t tid, std::size_t t_idx, rvp_addr_t addr);

bool hb_handler_acquire(hb_state* state_ptr, uint32_t tid, std::size_t t_idx, rvp_addr_t lock);

bool hb_handler_release(hb_state* state_ptr, uint32_t tid, std::size_t t_idx, rvp_addr_t lock);

bool hb_handler_fork(hb_state* state_ptr, uint32_t tid, std::size_t t_idx, uint32_t child);

bool hb_handler_join(hb_state* state_ptr, uint32_t tid, std::size_t t_idx, uint32_t child);

#endif /* _RVP_HB_HANDLERS_H_ */