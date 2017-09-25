#ifndef _RVP_REGISTER_H_
#define _RVP_REGISTER_H_

#include "rvpint.h"

/* void fn(T *addr, T val) */
uint8_t __rvpredict_peek1(uint8_t *);
uint16_t __rvpredict_peek2(uint16_t *);
uint32_t __rvpredict_peek4(uint32_t *);
uint64_t __rvpredict_peek8(uint64_t *);
rvp_uint128_t __rvpredict_peek16(rvp_uint128_t *);

/* void fn(T *addr, T val) */
void __rvpredict_poke1(uint8_t *, uint8_t);
void __rvpredict_poke2(uint16_t *, uint16_t);
void __rvpredict_poke4(uint32_t *, uint32_t);
void __rvpredict_poke8(uint64_t *, uint64_t);
void __rvpredict_poke16(rvp_uint128_t *, rvp_uint128_t);

#endif /* _RVP_REGISTER_H_ */
