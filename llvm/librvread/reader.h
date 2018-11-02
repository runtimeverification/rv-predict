#ifndef _RVP_READER_H_
#define _RVP_READER_H_

#include "nbcompat.h"
#include "tracefmt.h"

__BEGIN_EXTERN_C

typedef enum _rvp_output_type {
	  RVP_OUTPUT_PLAIN_TEXT = 0
	, RVP_OUTPUT_SYMBOL_FRIENDLY
	, RVP_OUTPUT_BINARY
	, RVP_OUTPUT_LEGACY_BINARY
} rvp_output_type_t;

typedef struct _rvp_output_params {
	rvp_output_type_t	op_type;
	bool			op_emit_generation;
	size_t			op_nrecords;
} rvp_output_params_t;

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

struct _rvp_pstate;
typedef struct _rvp_pstate rvp_pstate_t;

typedef struct _rvp_emitters {
	void (*init)(const rvp_pstate_t *, const rvp_trace_header_t *);
	void (*emit_nop)(const rvp_pstate_t *, const rvp_ubuf_t *);
	void (*emit_op)(const rvp_pstate_t *, const rvp_ubuf_t *, rvp_op_t,
	    bool, int);
	char *(*dataptr_to_string)(const rvp_pstate_t *, char *, size_t,
	    rvp_addr_t);
	char *(*insnptr_to_string)(const rvp_pstate_t *, char *, size_t,
	    rvp_addr_t);
} rvp_emitters_t;

typedef struct _rvp_frame {
	rvp_addr_t	f_pc;	/* Program Counter where runtime logged
				 * function entry
				 */
	rvp_addr_t	f_cfa;	/* DWARF Canonical Frame Address */
} rvp_frame_t;

typedef struct _rvp_call {
	rvp_frame_t	*cs_frame;
	int		cs_depth;
	int		cs_nframes;
} rvp_callstack_t;

#define	RVP_SIGNAL_DEPTH	16

/* parse state: per-thread */
typedef struct _rvp_thread_pstate {
	bool		ts_present;
	rvp_callstack_t	ts_callstack[RVP_SIGNAL_DEPTH];
	rvp_addr_t	ts_lastpc[RVP_SIGNAL_DEPTH];
	uint64_t	ts_generation[RVP_SIGNAL_DEPTH];
	uint64_t	ts_nops[RVP_SIGNAL_DEPTH];
	uint64_t	ts_last_gid[RVP_SIGNAL_DEPTH];
	bool		ts_sigs_masked;
} rvp_thread_pstate_t;

/* parse state: global */
struct _rvp_pstate {
	rvp_thread_pstate_t	*ps_thread;
	uint32_t		ps_nthreads;
	rvp_addr_t		ps_deltop_first, ps_deltop_center,
				ps_deltop_last;
	uint32_t		ps_curthread;
	const rvp_emitters_t	*ps_emitters;
	uint32_t		ps_idepth;
	int			ps_zeromasknum;
	bool			ps_emit_generation;
};

void rvp_trace_dump(const rvp_output_params_t *, int);
void rvp_trace_dump_with_emitters(bool, size_t, const rvp_emitters_t *, int);

__END_EXTERN_C

#endif /* _RVP_READER_H_ */
