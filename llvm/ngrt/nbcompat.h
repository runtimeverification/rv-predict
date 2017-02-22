/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_NBCOMPAT_H_
#define _RVP_NBCOMPAT_H_

#include <sys/param.h>

#ifndef __NetBSD__

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

#endif /* __NetBSD_Version__ */

#endif /* _RVP_NBCOMPAT_H_ */
