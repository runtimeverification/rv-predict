/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_NBCOMPAT_H_
#define _RVP_NBCOMPAT_H_

#include <sys/param.h>
#include <stddef.h>

#ifndef __NetBSD__

#ifndef __dead
#define	__dead	__attribute__((__noreturn__))
#endif

#ifndef offsetof
#define offsetof __builtin_offsetof
#endif

#ifndef __arraycount
#define __arraycount(__a)	(sizeof(__a) / sizeof((__a)[0]))
#endif /* __arraycount */

#ifndef __aligned
#define __aligned(x)	__attribute__((__aligned__(x)))
#endif /* __aligned */

#ifndef __section
#define __section(x)	__attribute__((__section__(x)))
#endif /* __section */

#ifndef __packed
#define	__packed	__attribute__((__packed__))
#endif /* __packed */

/* From <sys/cdefs.h> on NetBSD: */

#define __printflike(fmtarg, firstvararg)       \
            __attribute__((__format__ (__printf__, fmtarg, firstvararg)))
#define __scanflike(fmtarg, firstvararg)        \
            __attribute__((__format__ (__scanf__, fmtarg, firstvararg)))
#define __format_arg(fmtarg)    __attribute__((__format_arg__ (fmtarg)))

/* __BIT(n): nth bit, where __BIT(0) == 0x1. */
#define	__BIT(__n)	\
    (((uintmax_t)(__n) >= NBBY * sizeof(uintmax_t)) ? 0 : ((uintmax_t)1 << (uintmax_t)((__n) & (NBBY * sizeof(uintmax_t) - 1))))

/* __BITS(m, n): bits m through n, m < n. */
#define	__BITS(__m, __n)	\
        ((__BIT(MAX((__m), (__n)) + 1) - 1) ^ (__BIT(MIN((__m), (__n))) - 1))

/* find least significant bit that is set */
#define __LOWEST_SET_BIT(__mask) ((((__mask) - 1) & (__mask)) ^ (__mask))

#define	__PRIuBIT	PRIuMAX
#define	__PRIuBITS	__PRIuBIT

#define	__PRIxBIT	PRIxMAX
#define	__PRIxBITS	__PRIxBIT

#define	__SHIFTOUT(__x, __mask) (((__x) & (__mask)) / __LOWEST_SET_BIT(__mask))
#define	__SHIFTIN(__x, __mask) ((__x) * __LOWEST_SET_BIT(__mask))
#define	__SHIFTOUT_MASK(__mask) __SHIFTOUT((__mask), (__mask))

#endif /* __NetBSD__ */

#endif /* _RVP_NBCOMPAT_H_ */
