/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_EXCHANGE_H_
#define _RVP_EXCHANGE_H_

#include "rvpint.h"

/* T fn(T *addr, T val, int32_t memory_order) */ 
uint8_t __rvpredict_atomic_exchange1(uint8_t *, uint8_t, int32_t);
uint16_t __rvpredict_atomic_exchange2(uint16_t *, uint16_t, int32_t);
uint32_t __rvpredict_atomic_exchange4(uint32_t *, uint32_t, int32_t);
uint64_t __rvpredict_atomic_exchange8(uint64_t *, uint64_t, int32_t);
rvp_uint128_t __rvpredict_atomic_exchange16(rvp_uint128_t *, rvp_uint128_t,
    int32_t);

#endif /* _RVP_EXCHANGE_H_ */
