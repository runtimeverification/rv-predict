#include "access.h"
#include "buf.h"
#include "init.h"
#include "ring.h"
#include "thread.h"

extern inline bool data_is_in_coverage(rvp_addr_t);
extern inline bool ring_operational(void);
extern inline int rvp_iring_nfull(const rvp_iring_t *);
extern inline int rvp_ring_capacity(rvp_ring_t *);
extern inline int rvp_ring_nempty(rvp_ring_t *);
extern inline int rvp_ring_nfull(const rvp_ring_t *);
extern inline rvp_ring_t *rvp_ring_for_curthr(void);
extern inline rvp_thread_t *rvp_thread_for_curthr(void);
extern inline uint64_t rvp_ggen_after_load(void);
extern inline void rvp_buf_put_addr(rvp_buf_t *, rvp_addr_t);
extern inline void rvp_buf_put(rvp_buf_t *, uint32_t);
extern inline void rvp_buf_put_u64(rvp_buf_t *, uint64_t);
extern inline void rvp_buf_trace_cog(rvp_buf_t *, volatile uint64_t *,
    uint64_t);
extern inline void rvp_buf_trace_load_cog(rvp_buf_t *, volatile uint64_t *);
extern inline void rvp_increase_ggen(void);
extern inline void __rvpredict_load16(rvp_uint128_t *, rvp_uint128_t);
extern inline void __rvpredict_load1(uint8_t *, uint8_t);
extern inline void __rvpredict_load2(uint16_t *, uint16_t);
extern inline void __rvpredict_load4(uint32_t *, uint32_t);
extern inline void __rvpredict_load8(uint64_t *, uint64_t);
extern inline void rvp_ring_await_nempty(rvp_ring_t *, int);
extern inline void rvp_ring_put_buf(rvp_ring_t *, rvp_buf_t);
extern inline void rvp_ring_put_multiple(rvp_ring_t *, const uint32_t *, int);
extern inline void rvp_ring_request_service(rvp_ring_t *);
extern inline void trace_load8(const char *, rvp_op_t, rvp_addr_t, uint64_t);
extern inline void trace_load(const char *, rvp_op_t, rvp_addr_t, uint32_t);
