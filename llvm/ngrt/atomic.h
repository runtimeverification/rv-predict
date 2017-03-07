/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_ATOMIC_H_
#define _RVP_ATOMIC_H_

#include "rmw.h"
#include "rvpint.h"

/* void fn(T *addr, T val, int32_t memory_order) */ 
void __rvpredict_atomic_load1(uint8_t *, uint8_t, int32_t);
void __rvpredict_atomic_load2(uint16_t *, uint16_t, int32_t);
void __rvpredict_atomic_load4(uint32_t *, uint32_t, int32_t);
void __rvpredict_atomic_load8(uint64_t *, uint64_t, int32_t);
void __rvpredict_atomic_load16(rvp_uint128_t *, rvp_uint128_t, int32_t);

/* void fn(T *addr, T val, int32_t memory_order) */ 
void __rvpredict_atomic_store1(uint8_t *, uint8_t, int32_t);
void __rvpredict_atomic_store2(uint16_t *, uint16_t, int32_t);
void __rvpredict_atomic_store4(uint32_t *, uint32_t, int32_t);
void __rvpredict_atomic_store8(uint64_t *, uint64_t, int32_t);
void __rvpredict_atomic_store16(rvp_uint128_t *, rvp_uint128_t, int32_t);

/* void fn(T *addr, T oval, T arg, int32_t memory_order) */ 
rvp_rmw1_func_t __rvpredict_atomic_fetch_add1;
rvp_rmw2_func_t __rvpredict_atomic_fetch_add2;
rvp_rmw4_func_t __rvpredict_atomic_fetch_add4;
rvp_rmw8_func_t __rvpredict_atomic_fetch_add8;
rvp_rmw16_func_t __rvpredict_atomic_fetch_add16;

/* void fn(T *addr, T val, T arg, int32_t memory_order) */ 
rvp_rmw1_func_t __rvpredict_atomic_fetch_and1;
rvp_rmw2_func_t __rvpredict_atomic_fetch_and2;
rvp_rmw4_func_t __rvpredict_atomic_fetch_and4;
rvp_rmw8_func_t __rvpredict_atomic_fetch_and8;
rvp_rmw16_func_t __rvpredict_atomic_fetch_and16;

/* void fn(T *addr, T val, T arg, int32_t memory_order) */ 
rvp_rmw1_func_t __rvpredict_atomic_fetch_nand1;
rvp_rmw2_func_t __rvpredict_atomic_fetch_nand2;
rvp_rmw4_func_t __rvpredict_atomic_fetch_nand4;
rvp_rmw8_func_t __rvpredict_atomic_fetch_nand8;
rvp_rmw16_func_t __rvpredict_atomic_fetch_nand16;

/* void fn(T *addr, T val, T arg, int32_t memory_order) */ 
rvp_rmw1_func_t __rvpredict_atomic_fetch_or1;
rvp_rmw2_func_t __rvpredict_atomic_fetch_or2;
rvp_rmw4_func_t __rvpredict_atomic_fetch_or4;
rvp_rmw8_func_t __rvpredict_atomic_fetch_or8;
rvp_rmw16_func_t __rvpredict_atomic_fetch_or16;

/* void fn(T *addr, T val, T arg, int32_t memory_order) */ 
rvp_rmw1_func_t __rvpredict_atomic_fetch_sub1;
rvp_rmw2_func_t __rvpredict_atomic_fetch_sub2;
rvp_rmw4_func_t __rvpredict_atomic_fetch_sub4;
rvp_rmw8_func_t __rvpredict_atomic_fetch_sub8;
rvp_rmw16_func_t __rvpredict_atomic_fetch_sub16;

/* void fn(T *addr, T val, T arg, int32_t memory_order) */ 
rvp_rmw1_func_t __rvpredict_atomic_fetch_xor1;
rvp_rmw2_func_t __rvpredict_atomic_fetch_xor2;
rvp_rmw4_func_t __rvpredict_atomic_fetch_xor4;
rvp_rmw8_func_t __rvpredict_atomic_fetch_xor8;
rvp_rmw16_func_t __rvpredict_atomic_fetch_xor16;

#endif /* _RVP_ATOMIC_H_ */
