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

#endif /* __NetBSD__ */

#endif /* _RVP_NBCOMPAT_H_ */
