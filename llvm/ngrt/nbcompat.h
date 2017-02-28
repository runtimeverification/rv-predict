/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_NBCOMPAT_H_
#define _RVP_NBCOMPAT_H_

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

#ifndef	__aligned
#define __aligned(x)	__attribute__((__aligned__(x)))
#endif /* __aligned */

#ifndef __packed
#define	__packed	__attribute__((__packed__))
#endif /* __packed */

#endif /* _RVP_NBCOMPAT_H_ */
