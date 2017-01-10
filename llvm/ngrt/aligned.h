/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_ALIGNED_H_
#define _RVP_ALIGNED_H_

#include "rvpint.h"

/* void fn(T *addr, T val) */
void __rvpredict_load1(uint8_t *, uint8_t);
void __rvpredict_load2(uint16_t *, uint16_t);
void __rvpredict_load4(uint32_t *, uint32_t);
void __rvpredict_load8(uint64_t *, uint64_t);
void __rvpredict_load16(rvp_uint128_t *, rvp_uint128_t);

/* void fn(T *addr, T val) */
void __rvpredict_store1(uint8_t *, uint8_t);
void __rvpredict_store2(uint16_t *, uint16_t);
void __rvpredict_store4(uint32_t *, uint32_t);
void __rvpredict_store8(uint64_t *, uint64_t);
void __rvpredict_store16(rvp_uint128_t *, rvp_uint128_t);

#endif /* _RVP_ALIGNED_H_ */
