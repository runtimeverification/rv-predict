/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */
/*
 * Copyright (c) 1995, 1996 Carnegie-Mellon University.
 * All rights reserved.
 *
 * Author: Chris G. Demetriou
 *
 * Permission to use, copy, modify and distribute this software and
 * its documentation is hereby granted, provided that both the copyright
 * notice and this permission notice appear in all copies of the
 * software, derivative works or modified versions, and any portions
 * thereof, and that both notices appear in supporting documentation.
 *
 * CARNEGIE MELLON ALLOWS FREE USE OF THIS SOFTWARE IN ITS "AS IS"
 * CONDITION.  CARNEGIE MELLON DISCLAIMS ANY LIABILITY OF ANY KIND
 * FOR ANY DAMAGES WHATSOEVER RESULTING FROM THE USE OF THIS SOFTWARE.
 *
 * Carnegie Mellon requests users of this software to return to
 *
 *  Software Distribution Coordinator  or  Software.Distribution@CS.CMU.EDU
 *  School of Computer Science
 *  Carnegie Mellon University
 *  Pittsburgh PA 15213-3890
 *
 * any improvements or extensions that they make and grant Carnegie the
 * rights to redistribute these changes.
 */

/*
 * NetBSD compatibility macros
 *
 * NetBSD provides a lot of handy macros that Linux does not.  Many
 * of the macros even have documentation---see https://man-k.org/.
 */

#ifndef _RVP_NBCOMPAT_H_
#define _RVP_NBCOMPAT_H_

#include <sys/param.h> /* for MIN, MAX, NBBY */

#ifndef MIN
#define        MIN(__x, __y)   (((__x) < (__y)) ? (__x) : (__y))
#endif

#ifndef MAX
#define        MAX(__x, __y)   (((__x) > (__y)) ? (__x) : (__y))
#endif

#include <sys/cdefs.h>
#include <limits.h>

#include <sys/param.h>
#include <stddef.h>

#ifndef __NetBSD__

#ifndef __used
#define __used    __attribute__((__used__))
#endif /* __used */

/*
 * On multiprocessor systems we can gain an improvement in performance
 * by being mindful of which cachelines data is placed in.
 *
 * __read_mostly:
 *
 *      It makes sense to ensure that rarely modified data is not
 *      placed in the same cacheline as frequently modified data.
 *      To mitigate the phenomenon known as "false-sharing" we
 *      can annotate rarely modified variables with __read_mostly.
 *      All such variables are placed into the .data.read_mostly
 *      section in the kernel ELF.
 *
 *      Prime candidates for __read_mostly annotation are variables
 *      which are hardly ever modified and which are used in code
 *      hot-paths, e.g. pmap_initialized.
 *
 * __cacheline_aligned:
 *
 *      Some data structures (mainly locks) benefit from being aligned
 *      on a cacheline boundary, and having a cacheline to themselves.
 *      This way, the modification of other data items cannot adversely
 *      affect the lock and vice versa.
 *
 *      Any variables annotated with __cacheline_aligned will be
 *      placed into the .data.cacheline_aligned ELF section.
 */
#define __read_mostly                                           \
    __attribute__((__section__(".data.read_mostly")))

#if 0
#define __cacheline_aligned                                     \
    __attribute__((__aligned__(COHERENCY_UNIT),                 \
                 __section__(".data.cacheline_aligned")))
#endif

/*
 * GNU C version 2.96 adds explicit branch prediction so that
 * the CPU back-end can hint the processor and also so that
 * code blocks can be reordered such that the predicted path
 * sees a more linear flow, thus improving cache behavior, etc.
 *
 * The following two macros provide us with a way to use this
 * compiler feature.  Use __predict_true() if you expect the expression
 * to evaluate to true, and __predict_false() if you expect the
 * expression to evaluate to false.
 *
 * A few notes about usage:
 *
 *      * Generally, __predict_false() error condition checks (unless
 *        you have some _strong_ reason to do otherwise, in which case
 *        document it), and/or __predict_true() `no-error' condition
 *        checks, assuming you want to optimize for the no-error case.
 *
 *      * Other than that, if you don't know the likelihood of a test
 *        succeeding from empirical or other `hard' evidence, don't
 *        make predictions.
 *
 *      * These are meant to be used in places that are run `a lot'.
 *        It is wasteful to make predictions in code that is run
 *        seldomly (e.g. at subsystem initialization time) as the
 *        basic block reordering that this affects can often generate
 *        larger code.
 */
#define __predict_true(exp)     __builtin_expect((exp) != 0, 1)
#define __predict_false(exp)    __builtin_expect((exp) != 0, 0)

/* On some systems, for assembly language to refer to a C symbol,
 * you have to add an underscore (_) to the name.  _C_LABEL() and
 * _C_LABEL_STRING() change a C symbol to the proper convention for
 * assembly.
 */
#define _C_LABEL(x)     x
#define _C_LABEL_STRING(x)      x

/* In the object file for this module, introduce a new symbol, `alias`,
 * that is an alias for the C symbol `sym`.  The alias is "strong," so
 * the linker will use it in preference to any "weak" symbol by the name
 * `alias`.
 *
 * The C compiler emits strong symbols by default.
 *
 * Strong aliases can be used to make the same function available by two
 * or more names.
 */
#define	__strong_alias(alias,sym)					\
    __asm(".global " _C_LABEL_STRING(#alias) "\n"			\
	    _C_LABEL_STRING(#alias) " = " _C_LABEL_STRING(#sym));

/* In the object file for this module, introduce a new symbol, `alias`,
 * that is an alias for the C symbol `sym`.  The alias is "weak," so a
 * strong symbol will override.
 *
 * The C compiler emits strong symbols by default.
 *
 * Oftentimes weak aliases are used to provide a default implementation
 * of a C function.  By linking in an object file with a specialized
 * implementation, the default implementation can be overridden.
 */
#define	__weak_alias(alias,sym)					\
    __asm(".weak " _C_LABEL_STRING(#alias) "\n"			\
	    _C_LABEL_STRING(#alias) " = " _C_LABEL_STRING(#sym));

/* Macros for counting and rounding from NetBSD.
 *
 * Documentation from the NetBSD manual page, roundup(9):
 *
 *   The roundup() and rounddown() macros return an integer from rounding x up
 *   and down, respectively, to the next size.  The howmany() macro in turn
 *   reveals how many times size fits into x, rounding the residual up.

 *   The roundup2() macro also rounds up, but with the assumption that size is
 *   a power of two.  If x is indeed a power of two, powerof2() return 1.
 */
#ifndef howmany
#define howmany(x, y)   (((x)+((y)-1))/(y))
#endif
#ifndef roundup
#define roundup(x, y)   ((((x)+((y)-1))/(y))*(y))
#endif
#define rounddown(x,y)  (((x)/(y))*(y))
#define roundup2(x, m)  (((x) + (m) - 1) & ~((m) - 1))
#ifndef powerof2
#define powerof2(x)     ((((x)-1)&(x))==0)
#endif

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

#define	__unused	__attribute__((__unused__))

#endif /* __NetBSD__ */

#endif /* _RVP_NBCOMPAT_H_ */
