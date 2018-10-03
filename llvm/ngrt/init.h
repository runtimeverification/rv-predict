/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_INIT_H_
#define _RVP_INIT_H_

#include "nbcompat.h"	/* for __predict_false */
#include "supervise.h"
#include "intrinit.h"

void rvp_prefork_init(void);

void rvp_lock_prefork_init(void);
void rvp_signal_prefork_init(void);
void rvp_str_prefork_init(void);
void rvp_thread_prefork_init(void);
void rvp_specific_prefork_init(void);

void __rvpredict_init(void);
void rvp_fork_init(void);
void rvp_lock_init(void);
void rvp_signal_init(void);
void rvp_thread_init(void);
void rvp_static_intrs_init(void);
void rvp_deltop_init(void);

extern _Atomic bool __read_mostly rvp_real_initialized;

/*
 * Before the Predict runtime has initialized, some programs will load
 * shared objects whose initializers call libc or libpthread functions
 * where we interpose the Predict runtime.  The Predict runtime
 * must make sure that pointers to the "real" implementation are established
 * before it calls functions through those pointers.  
 *
 * `ensure_real_initialized()` makes sure that if the pointers
 * were not already established, then they are established before it
 * returns.
 */
static inline void
ensure_real_initialized()
{
	if (__predict_false(!rvp_real_initialized))
		rvp_prefork_init();
}

static inline bool
ring_operational()
{
	return rvp_initialized;
}

#endif /* _RVP_INIT_H_ */
