#ifndef _RVP_INTERPOSE_H_
#define _RVP_INTERPOSE_H_

#include <dlfcn.h> /* for dlsym(3) */
#include <pthread.h>
#include <signal.h>

#define	REAL_DECL(__rettype, __func, ...)				\
	extern __rettype (*real_##__func)(__VA_ARGS__)

#define	REAL_DEFN(__rettype, __func, ...)				\
	__rettype (*real_##__func)(__VA_ARGS__)

#define	INTERPOSE_DECLS(__rettype, __func, ...)				\
extern __rettype (*real_##__func)(__VA_ARGS__);				\
__rettype __rvpredict_##__func(__VA_ARGS__)

#define	INTERPOSE(__rettype, __func, ...)				\
__rettype __func(__VA_ARGS__) __attribute__((weak,			\
			             alias("__rvpredict_" #__func),	\
				     visibility("default")))

#define	ESTABLISH_PTR_TO_REAL(__func)	do {		\
	real_##__func = dlsym(RTLD_NEXT, #__func);	\
} while (/*CONSTCOND*/false)

INTERPOSE_DECLS(int, pthread_join, pthread_t, void **);
INTERPOSE_DECLS(int, pthread_create, pthread_t *, const pthread_attr_t *,
    void *(*)(void *), void *);
INTERPOSE_DECLS(void, pthread_exit, void *);

INTERPOSE_DECLS(int, pthread_mutex_lock, pthread_mutex_t *);
INTERPOSE_DECLS(int, pthread_mutex_trylock, pthread_mutex_t *);
INTERPOSE_DECLS(int, pthread_mutex_unlock, pthread_mutex_t *);
INTERPOSE_DECLS(int, pthread_mutex_init, pthread_mutex_t *restrict,
   const pthread_mutexattr_t *restrict);

INTERPOSE_DECLS(int, sigaction, int signum, const struct sigaction *,
    struct sigaction *);

#endif /* _RVP_INTERPOSE_H_ */
