#ifndef _RVP_RMW_H_
#define _RVP_RMW_H_

#include <stdint.h>
#include "rvpint.h"

typedef void rvp_rmw1_func_t(uint8_t *, uint8_t, uint8_t, int32_t);
typedef void rvp_rmw2_func_t(uint16_t *, uint16_t, uint16_t, int32_t);
typedef void rvp_rmw4_func_t(uint32_t *, uint32_t, uint32_t, int32_t);
typedef void rvp_rmw8_func_t(uint64_t *, uint64_t, uint64_t, int32_t);
typedef void rvp_rmw16_func_t(rvp_uint128_t *, rvp_uint128_t, rvp_uint128_t,
    int32_t);

#endif /* _RVP_RMW_H_ */
