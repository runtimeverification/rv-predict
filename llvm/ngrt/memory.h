/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_MEMORY_H_
#define _RVP_MEMORY_H_

void *__rvpredict_memcpy(void *, const void *, size_t);
void *__rvpredict_memmove(void *, const void *, size_t);
void *__rvpredict_memset(void *, int, size_t);

#endif /* _RVP_MEMORY_H_ */
