/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_TEXT_H_
#define _RVP_TEXT_H_

#include <stdbool.h>

extern const char __rvpredict_text_begin __section(".text.rvpredict");
extern const char __rvpredict_text_end __section(".text.rvpredict");

static inline bool
instruction_is_in_rvpredict(const void *retaddr)
{
	return (uintptr_t)&__rvpredict_text_begin <= (uintptr_t)retaddr &&
	    (uintptr_t)retaddr <= (uintptr_t)&__rvpredict_text_end;
}

#endif /* _RVP_TEXT_H_ */
