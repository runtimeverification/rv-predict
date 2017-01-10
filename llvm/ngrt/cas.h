/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_CAS_H_
#define _RVP_CAS_H_

#include "rvpint.h"

/* T fn(T *addr, T expected, T desired,
 *      int32_t memory_order_success,
 *      int32_t memory_order_failure)
 */
uint8_t __rvpredict_atomic_cas1(uint8_t *, uint8_t, uint8_t, int32_t, int32_t);
uint16_t __rvpredict_atomic_cas2(uint16_t *, uint16_t, uint16_t,
    int32_t, int32_t);
uint32_t __rvpredict_atomic_cas4(uint32_t *, uint32_t, uint32_t,
    int32_t, int32_t);
uint64_t __rvpredict_atomic_cas8(uint64_t *, uint64_t, uint64_t,
    int32_t, int32_t);
rvp_uint128_t __rvpredict_atomic_cas16(rvp_uint128_t *,
    rvp_uint128_t, rvp_uint128_t, int32_t, int32_t);

#endif /* _RVP_CAS_H_ */
