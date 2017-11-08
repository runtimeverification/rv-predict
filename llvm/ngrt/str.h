#ifndef _RVP_STRING_H_
#define _RVP_STRING_H_

#include <string.h>

#include "interpose.h"

void *__rvpredict_memcpy(void *, const void *, size_t);
void *__rvpredict_memmove(void *, const void *, size_t);
void *__rvpredict_memset(void *, int, size_t);

void *__rvpredict_memmove1(const void *,
    const rvp_addr_t, const rvp_addr_t, size_t);
void *__rvpredict_memset1(const void *, const rvp_addr_t, int, size_t);

#endif /* _RVP_STRING_H_ */
