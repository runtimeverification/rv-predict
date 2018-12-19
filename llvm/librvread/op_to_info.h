#ifndef _RVP_OP_TO_INFO_H_
#define _RVP_OP_TO_INFO_H_

#include "nbcompat.h"
#include "tracefmt.h"

__BEGIN_EXTERN_C

typedef struct {
	size_t oi_reclen;
	const char *oi_descr;
} op_info_t;

#define OP_INFO_INIT(__ty, __descr)	\
	{.oi_reclen = sizeof(__ty), .oi_descr = __descr}

#define OP_INFO_DEFAULT	\
	{.oi_reclen = 0, .oi_descr = NULL}

static const op_info_t op_to_info[RVP_NOPS] = {
	  [RVP_OP_BEGIN] = OP_INFO_INIT(rvp_begin_t, "begin thread")
	, [RVP_OP_END] = OP_INFO_INIT(rvp_end_exitfn_t, "end thread")
	, [RVP_OP_LOAD1] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t, "load 1")
	, [RVP_OP_LOAD2] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t, "load 2")
	, [RVP_OP_LOAD4] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t, "load 4")
	, [RVP_OP_LOAD8] = OP_INFO_INIT(rvp_load8_store8_t, "load 8")
	, [RVP_OP_LOAD16] = OP_INFO_DEFAULT
	, [RVP_OP_STORE1] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t, "store 1")
	, [RVP_OP_STORE2] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t, "store 2")
	, [RVP_OP_STORE4] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t, "store 4")
	, [RVP_OP_STORE8] = OP_INFO_INIT(rvp_load8_store8_t, "store 8")
	, [RVP_OP_STORE16] = OP_INFO_DEFAULT
	, [RVP_OP_FORK] = OP_INFO_INIT(rvp_fork_join_switch_t,
	    "fork thread")
	, [RVP_OP_JOIN] = OP_INFO_INIT(rvp_fork_join_switch_t,
	    "join thread")
	, [RVP_OP_ACQUIRE] = OP_INFO_INIT(rvp_acquire_release_t,
	    "acquire mutex")
	, [RVP_OP_RELEASE] = OP_INFO_INIT(rvp_acquire_release_t,
	    "release mutex")
	, [RVP_OP_ENTERFN] = OP_INFO_INIT(rvp_enterfn_t, "enter function")
	, [RVP_OP_EXITFN] = OP_INFO_INIT(rvp_end_exitfn_t, "exit function")
	, [RVP_OP_SWITCH] = OP_INFO_INIT(rvp_fork_join_switch_t,
					 "switch thread")
	, [RVP_OP_ATOMIC_RMW1] = OP_INFO_INIT(rvp_rmw1_2_t, "atomic rmw 1")
	, [RVP_OP_ATOMIC_RMW2] = OP_INFO_INIT(rvp_rmw1_2_t, "atomic rmw 2")
	, [RVP_OP_ATOMIC_RMW4] = OP_INFO_INIT(rvp_rmw4_t, "atomic rmw 4")
	, [RVP_OP_ATOMIC_RMW8] = OP_INFO_INIT(rvp_rmw8_t, "atomic rmw 8")
	, [RVP_OP_ATOMIC_RMW16] = OP_INFO_DEFAULT
	, [RVP_OP_ATOMIC_LOAD1] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t,
					       "atomic load 1")
	, [RVP_OP_ATOMIC_LOAD2] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t,
					       "atomic load 2")
	, [RVP_OP_ATOMIC_LOAD4] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t,
					       "atomic load 4")
	, [RVP_OP_ATOMIC_LOAD8] = OP_INFO_INIT(rvp_load8_store8_t,
					       "atomic load 8")
	, [RVP_OP_ATOMIC_LOAD16] = OP_INFO_DEFAULT
	, [RVP_OP_ATOMIC_STORE1] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t,
						"atomic store 1")
	, [RVP_OP_ATOMIC_STORE2] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t,
						"atomic store 2")
	, [RVP_OP_ATOMIC_STORE4] = OP_INFO_INIT(rvp_load1_2_4_store1_2_4_t,
						"atomic store 4")
	, [RVP_OP_ATOMIC_STORE8] = OP_INFO_INIT(rvp_load8_store8_t,
					        "atomic store 8")
	, [RVP_OP_ATOMIC_STORE16] = OP_INFO_DEFAULT
	, [RVP_OP_COG] = OP_INFO_INIT(rvp_cog_t, "change of generation")
	, [RVP_OP_SIGEST] =
	    OP_INFO_INIT(rvp_sigest_t, "establish signal action")
	, [RVP_OP_ENTERSIG] = OP_INFO_INIT(rvp_entersig_t,
	    "enter signal handler")
	, [RVP_OP_EXITSIG] = OP_INFO_INIT(rvp_exitsig_t, "exit signal handler")
	, [RVP_OP_SIGDIS] =
	    OP_INFO_INIT(rvp_sigdis_t, "disestablish signal action")
	, [RVP_OP_SIGMASKMEMO] =
	    OP_INFO_INIT(rvp_sigmaskmemo_t, "memoize signal mask")
	, [RVP_OP_SIGSETMASK] =
	    OP_INFO_INIT(rvp_sigmask_access_t, "set signal mask")
	, [RVP_OP_SIGDEPTH] = OP_INFO_INIT(rvp_sigdepth_t,
	    "signal depth")
	, [RVP_OP_SIGBLOCK] =
	    OP_INFO_INIT(rvp_sigmask_access_t, "block signals")
	, [RVP_OP_SIGUNBLOCK] =
	    OP_INFO_INIT(rvp_sigmask_access_t, "unblock signals")
	, [RVP_OP_SIGGETSETMASK] = OP_INFO_INIT(rvp_sigmask_rmw_t,
	    "get & set signal mask")
	, [RVP_OP_SIGGETMASK] =
	    OP_INFO_INIT(rvp_sigmask_access_t, "get signal mask")
};

__END_EXTERN_C

#endif /* _RVP_OP_TO_INFO_H_ */
