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

#endif /* _NBCOMPAT_H_ */
