/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_INIT_H_
#define _RVP_INIT_H_

#include "intrinit.h"

void __rvpredict_init(void);
void rvp_lock_init(void);
void rvp_signal_init(void);
void rvp_thread_init(void);
void rvp_static_intrs_init(void);

#endif /* _RVP_INIT_H_ */
