/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_INIT_H_
#define _RVP_INIT_H_

#ifndef _RVP_SUPERVISE_H_
#	include "supervise.h"
#endif

#include "intrinit.h"


extern void
rvp_prefork_init(void);


static inline void
rvp_ensure_initialization(void){
	if(!rvp_initialized)
		rvp_prefork_init();
}
void rvp_lock_prefork_init(void);
void rvp_signal_prefork_init(void);
void rvp_str_prefork_init(void);
void rvp_thread_prefork_init(void);

void __rvpredict_init(void);
void rvp_lock_init(void);
void rvp_signal_init(void);
void rvp_thread_init(void);
void rvp_static_intrs_init(void);
void rvp_deltop_init(void);

#endif /* _RVP_INIT_H_ */
