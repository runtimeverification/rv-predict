#ifndef _NBCOMPAT_H_
#define _NBCOMPAT_H_

#ifndef __printflike
#define	__printflike(fmtarg, firstvararg)	\
	__attribute__((__format__ (__printf__, fmtarg, firstvararg)))
#endif

#ifndef __unused
#define	__unused	__attribute__((__unused__))
#endif

#ifndef offsetof
#define offsetof(__type, __member)	\
	(size_t)((char *)&((__type)NULL)->__member - (char *)((__type)NULL))
#endif

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

#endif /* _NBCOMPAT_H_ */
