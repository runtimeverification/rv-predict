/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_FENCE_H_
#define _RVP_FENCE_H_

void __rvpredict_atomic_thread_fence(memory_order order); 
void __rvpredict_atomic_signal_fence(memory_order order);

#endif /* _RVP_FENCE_H_ */
