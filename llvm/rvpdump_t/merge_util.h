#ifndef _RVP_MERGE_UTIL_H_
#define _RVP_MERGE_UTIL_H_

#include "nbcompat.h"
#include "tracefmt.h"

__BEGIN_EXTERN_C

extern rvp_trace_header_t expected_trace_header;

uint64_t
rvp_read_header(int fd, uint32_t tid, rvp_pstate_t *ps, rvp_ubuf_t *ub);

uint64_t
rvp_trace_dump_until_cog(int fd, uint32_t tid, rvp_pstate_t *ps, rvp_ubuf_t *ub);

__END_EXTERN_C

#endif /* _RVP_MERGE_UTIL_H_ */
