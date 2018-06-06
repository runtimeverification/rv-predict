#ifndef _RVP_REAL_H_
#define _RVP_REAL_H_

#include <dlfcn.h>	/* for dlsym(3) */

#define	REAL_DECL(__rettype, __func, ...)				\
	extern __rettype (*real_##__func)(__VA_ARGS__)

#define	REAL_DEFN(__rettype, __func, ...)				\
	__rettype (*real_##__func)(__VA_ARGS__)

#define	ESTABLISH_PTR_TO_REAL(__fntype, __fn)	do {		\
	real_##__fn = (__fntype)dlsym(RTLD_NEXT, #__fn);	\
} while (/*CONSTCOND*/false)

#endif /* _RVP_REAL_H_ */
