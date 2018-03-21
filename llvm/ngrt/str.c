#include "access.h"
#include "init.h"
#include "interpose.h"
#include "nbcompat.h"
#include "str.h"
#include "supervise.h"
#include "text.h"
#include "tracefmt.h"

REAL_DEFN(void *, memcpy, void *, const void *, size_t) =
    __rvpredict_internal_memcpy;
REAL_DEFN(void *, memmove, void *, const void *, size_t) =
    __rvpredict_internal_memmove;
REAL_DEFN(void *, memset, void *, int, size_t) =
    __rvpredict_internal_memset;

void
rvp_str_prefork_init(void)
{
	ESTABLISH_PTR_TO_REAL(void *(*)(void *, const void *, size_t), memcpy);
	ESTABLISH_PTR_TO_REAL(void *(*)(void *, const void *, size_t), memmove);
	ESTABLISH_PTR_TO_REAL(void *(*)(void *, int, size_t), memset);
}

void *
__rvpredict_memmove1(const void *retaddr,
    const rvp_addr_t dst, const rvp_addr_t src, size_t n)
{
	rvp_addr_t to, from;
	size_t ncopied = 0;
	int astep, width;
	bool backwards = src < dst && src + n > dst;

	/* Copy from higher to lower address if the source precedes
	 * the destination and the source and destination overlap.
	 * Otherwise, copy from lower to higher address.
	 */
	if (backwards) {
		from = src + n;
		to = dst + n;
	} else {
		from = src;
		to = dst;
	}
	for (width = sizeof(uint64_t); width > 1; width /= 2) {
		if (n < width)
			continue;
		if (to % width == 0 && from % width == 0)
			break;
	}
	if (backwards) {
		from -= width;
		to -= width;
		astep = -width;
	} else {
		astep = width;
	}
	size_t nwhole = rounddown(n, width);
	for (ncopied = 0;
	     ncopied < nwhole;
	     ncopied += width, from += astep, to += astep) {
		switch (width) {
		case 1:
		  {
			uint8_t val = *(uint8_t *)from;
			trace_load(retaddr, RVP_OP_LOAD1, from, val);
			trace_store(retaddr, RVP_OP_STORE1, to, val);
			*(uint8_t *)to = val;
			break;
		  }
		case 2:
		  {
			uint16_t val = *(uint16_t *)from;
			trace_load(retaddr, RVP_OP_LOAD2, from, val);
			trace_store(retaddr, RVP_OP_STORE2, to, val);
			*(uint16_t *)to = val;
			break;
		  }
		case 4:
		  {
			uint32_t val = *(uint32_t *)from;
			trace_load(retaddr, RVP_OP_LOAD4, from, val);
			trace_store(retaddr, RVP_OP_STORE4, to, val);
			*(uint32_t *)to = val;
			break;
		  }
		case 8:
		  {
			uint64_t val = *(uint64_t *)from;
			trace_load8(retaddr, RVP_OP_LOAD8, from, val);
			trace_store8(retaddr, RVP_OP_STORE8, to, val);
			*(uint64_t *)to = val;
			break;
		  }
		}
	}
	if (ncopied == n)
		;	// finished copying, do nothing
	else if (astep < 0)
		(void)__rvpredict_memmove1(retaddr, dst, src, n - ncopied);
	else {
		(void)__rvpredict_memmove1(retaddr, to, from, n - ncopied);
	}
	return (void *)dst;
}

void *
__rvpredict_memset1(const void *retaddr, const rvp_addr_t dst, int c, size_t n)
{
	union {
		uint64_t u64;
		uint32_t u32;
		uint16_t u16;
		uint8_t u8;
	} u;
	size_t ncopied = 0;
	int width;
	rvp_addr_t to = dst;

	real_memset(&u, c, sizeof(u));

	for (width = sizeof(uint64_t); width > 1; width /= 2) {
		if (n < width)
			continue;
		if (to % width == 0)
			break;
	}
	size_t nwhole = rounddown(n, width);
	for (ncopied = 0;
	     ncopied < nwhole;
	     ncopied += width, to += width) {
		switch (width) {
		case 1:
			trace_store(retaddr, RVP_OP_STORE1, to, u.u8);
			*(uint8_t *)to = u.u8;
			break;
		case 2:
			trace_store(retaddr, RVP_OP_STORE2, to, u.u16);
			*(uint16_t *)to = u.u16;
			break;
		case 4:
			trace_store(retaddr, RVP_OP_STORE4, to, u.u32);
			*(uint32_t *)to = u.u32;
			break;
		case 8:
			trace_store8(retaddr, RVP_OP_STORE8, to, u.u64);
			*(uint64_t *)to = u.u64;
			break;
		}
	}
	if (ncopied == n)
		return (void *)dst;
	(void)__rvpredict_memset1(retaddr, to - width, c, n - ncopied);
	return (void *)dst;
}

void *
__rvpredict_memcpy(void *dst, const void *src, size_t n)
{
	const void *retaddr = __builtin_return_address(0);

	if (__predict_false(!rvp_initialized ||
	                    instruction_is_in_rvpredict(retaddr)))
		return real_memcpy(dst, src, n);

	return __rvpredict_memmove1(retaddr,
	    (rvp_addr_t)dst, (rvp_addr_t)src, n);
}

void *
__rvpredict_memmove(void *dst, const void *src, size_t n)
{
	const void *retaddr = __builtin_return_address(0);

	if (__predict_false(!rvp_initialized ||
	                    instruction_is_in_rvpredict(retaddr)))
		return real_memmove(dst, src, n);

	return __rvpredict_memmove1(retaddr,
	    (rvp_addr_t)dst, (rvp_addr_t)src, n);
}

void *
__rvpredict_memset(void *dst, int c, size_t n)
{
	const void *retaddr = __builtin_return_address(0);

	if (__predict_false(!rvp_initialized ||
	                    instruction_is_in_rvpredict(retaddr)))
		return real_memset(dst, c, n);

	return __rvpredict_memset1(retaddr, (rvp_addr_t)dst,
	    c, n);
}

INTERPOSE(void *, memcpy, void *, const void *, size_t);
INTERPOSE(void *, memmove, void *, const void *, size_t);
INTERPOSE(void *, memset, void *, int, size_t);
