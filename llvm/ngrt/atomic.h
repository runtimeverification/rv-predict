/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_ATOMIC_H_
#define _RVP_ATOMIC_H_

#include "rvpint.h"

/* T fn(T *addr, int32_t memory_order) */ 
void __rvpredict_atomic_load1(uint8_t *, uint8_t, int32_t);
void __rvpredict_atomic_load2(uint16_t *, uint16_t, int32_t);
void __rvpredict_atomic_load4(uint32_t *, uint32_t, int32_t);
void __rvpredict_atomic_load8(uint64_t *, uint64_t, int32_t);
void __rvpredict_atomic_load16(rvp_uint128_t *, rvp_uint128_t, int32_t);

/* T fn(T *addr, T val, int32_t memory_order) */ 
void __rvpredict_atomic_store1(uint8_t *, uint8_t, int32_t);
void __rvpredict_atomic_store2(uint16_t *, uint16_t, int32_t);
void __rvpredict_atomic_store4(uint32_t *, uint32_t, int32_t);
void __rvpredict_atomic_store8(uint64_t *, uint64_t, int32_t);
void __rvpredict_atomic_store16(rvp_uint128_t *, rvp_uint128_t, int32_t);

/* T fn(T *addr, T val, int32_t memory_order) */ 
void __rvpredict_atomic_fetch_add1(uint8_t *, uint8_t, uint8_t, int32_t);
void __rvpredict_atomic_fetch_add2(uint16_t *, uint16_t, uint16_t, int32_t);
void __rvpredict_atomic_fetch_add4(uint32_t *, uint32_t, uint32_t, int32_t);
void __rvpredict_atomic_fetch_add8(uint64_t *, uint64_t, uint64_t, int32_t);
void __rvpredict_atomic_fetch_add16(rvp_uint128_t *, rvp_uint128_t,
    rvp_uint128_t, int32_t);

/* T fn(T *addr, T val, int32_t memory_order) */ 
uint8_t __rvpredict_atomic_fetch_and1(uint8_t *, uint8_t, int32_t);
uint16_t __rvpredict_atomic_fetch_and2(uint16_t *, uint16_t, int32_t);
uint32_t __rvpredict_atomic_fetch_and4(uint32_t *, uint32_t, int32_t);
uint64_t __rvpredict_atomic_fetch_and8(uint64_t *, uint64_t, int32_t);
rvp_uint128_t __rvpredict_atomic_fetch_and16(rvp_uint128_t *, rvp_uint128_t,
    int32_t);

/* T fn(T *addr, T val, int32_t memory_order) */ 
uint8_t __rvpredict_atomic_fetch_nand1(uint8_t *, uint8_t, int32_t);
uint16_t __rvpredict_atomic_fetch_nand2(uint16_t *, uint16_t, int32_t);
uint32_t __rvpredict_atomic_fetch_nand4(uint32_t *, uint32_t, int32_t);
uint64_t __rvpredict_atomic_fetch_nand8(uint64_t *, uint64_t, int32_t);
rvp_uint128_t __rvpredict_atomic_fetch_nand16(rvp_uint128_t *, rvp_uint128_t,
    int32_t);

/* T fn(T *addr, T val, int32_t memory_order) */ 
uint8_t __rvpredict_atomic_fetch_or1(uint8_t *, uint8_t, int32_t);
uint16_t __rvpredict_atomic_fetch_or2(uint16_t *, uint16_t, int32_t);
uint32_t __rvpredict_atomic_fetch_or4(uint32_t *, uint32_t, int32_t);
uint64_t __rvpredict_atomic_fetch_or8(uint64_t *, uint64_t, int32_t);
rvp_uint128_t __rvpredict_atomic_fetch_or16(rvp_uint128_t *, rvp_uint128_t,
    int32_t);

/* T fn(T *addr, T val, int32_t memory_order) */ 
uint8_t __rvpredict_atomic_fetch_sub1(uint8_t *, uint8_t, int32_t);
uint16_t __rvpredict_atomic_fetch_sub2(uint16_t *, uint16_t, int32_t);
uint32_t __rvpredict_atomic_fetch_sub4(uint32_t *, uint32_t, int32_t);
uint64_t __rvpredict_atomic_fetch_sub8(uint64_t *, uint64_t, int32_t);
rvp_uint128_t __rvpredict_atomic_fetch_sub16(rvp_uint128_t *, rvp_uint128_t,
    int32_t);

/* T fn(T *addr, T val, int32_t memory_order) */ 
uint8_t __rvpredict_atomic_fetch_xor1(uint8_t *, uint8_t, int32_t);
uint16_t __rvpredict_atomic_fetch_xor2(uint16_t *, uint16_t, int32_t);
uint32_t __rvpredict_atomic_fetch_xor4(uint32_t *, uint32_t, int32_t);
uint64_t __rvpredict_atomic_fetch_xor8(uint64_t *, uint64_t, int32_t);
rvp_uint128_t __rvpredict_atomic_fetch_xor16(rvp_uint128_t *, rvp_uint128_t,
    int32_t);

#endif /* _RVP_ATOMIC_H_ */
