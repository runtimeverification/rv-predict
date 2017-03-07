/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_EXCHANGE_H_
#define _RVP_EXCHANGE_H_

#include "rmw.h"
#include "rvpint.h"

/* void fn(T *addr, T oval, T nval, int32_t memory_order) */ 
rvp_rmw1_func_t __rvpredict_atomic_exchange1;
rvp_rmw2_func_t __rvpredict_atomic_exchange2;
rvp_rmw4_func_t __rvpredict_atomic_exchange4;
rvp_rmw8_func_t __rvpredict_atomic_exchange8;
rvp_rmw16_func_t __rvpredict_atomic_exchange16;

#endif /* _RVP_EXCHANGE_H_ */
