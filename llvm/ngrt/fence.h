/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_FENCE_H_
#define _RVP_FENCE_H_

void __rvpredict_atomic_thread_fence(int32_t memory_order);
void __rvpredict_atomic_signal_fence(int32_t memory_order);

#endif /* _RVP_FENCE_H_ */
