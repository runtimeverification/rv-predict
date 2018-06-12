#ifndef _RVP_INTERPOSE_H_
#define _RVP_INTERPOSE_H_

#include <dlfcn.h>	/* for dlsym(3) */
#include <pthread.h>	/* for pthread_{join,create,exit}(3),
			 * pthread_mutex_{init,lock,trylock,unlock}(3),
			 * pthread_cond_{init,broadcast,signal,wait}(3), etc.
			 */
#include <signal.h>	/* for sigprocmask(3), sigaction(3), signal(3),
			 * sigsuspend(3), etc.
			 */
#include <string.h>	/* for memcpy(3), memmove(3), memset(3) */

#define	REAL_DECL(__rettype, __func, ...)				\
	extern __rettype (*real_##__func)(__VA_ARGS__)

#define	REAL_DEFN(__rettype, __func, ...)				\
	__rettype (*real_##__func)(__VA_ARGS__)

#define	REAL_DEFN_VER3(__major, __minor, __teeny, __rettype, __func, ...) \
	__rettype (*real##__major##__minor##__teeny##_##__func)(__VA_ARGS__)

#define	INTERPOSITION_ATTRIBUTE	__attribute__((visibility("default")))

#define	INTERPOSE_DEFN(__rettype, __func, ...)				\
INTERPOSITION_ATTRIBUTE __rettype __rvpredict_##__func(__VA_ARGS__)

#define	INTERPOSE_DECLS(__rettype, __func, ...)				\
extern __rettype (*real_##__func)(__VA_ARGS__);				\
__rettype __rvpredict_##__func(__VA_ARGS__)

#if defined(_FORTIFY_SOURCE) && _FORTIFY_SOURCE != 0
#error "_FORTIFY_SOURCE != 0 is not compatible with RV-Predict/C"
#endif

#define	INTERPOSE(__rettype, __func, ...)				\
__rettype __func(__VA_ARGS__) __attribute__((weak,			\
			             alias("__rvpredict_" #__func),	\
				     visibility("default")))

#define	INTERPOSE_DECLS_VER3(__major, __minor, __teeny, __rettype, __fn, ...) \
extern __rettype							\
	(*real##__major##__minor##__teeny##_##__fn)(			\
	    __VA_ARGS__);						\
	__rettype							\
	__rvpredict##__major##__minor##__teeny##_##__fn(		\
	    __VA_ARGS__)

#define	_INTERPOSE_VER3_IMPL(__major, __minor, __teeny, __delim, __rettype, __fn, ...) \
	__rettype							\
	__rvpredict##__major##__minor##__teeny##_##__fn(		\
	    __VA_ARGS__);						\
	__asm(".symver __rvpredict" #__major #__minor #__teeny "_" #__fn ", " #__fn __delim "GLIBC_" #__major "." #__minor "." #__teeny)

#define	INTERPOSE_DEFAULT_VER3(__major, __minor, __teeny, __rettype, __fn, ...)\
	_INTERPOSE_VER3_IMPL(__major, __minor, __teeny, "@@",		\
	                     __rettype, __fn, __VA_ARGS__)

#define	INTERPOSE_VER3(__major, __minor, __teeny, __rettype, __fn, ...) \
	_INTERPOSE_VER3_IMPL(__major, __minor, __teeny, "@",		\
	                     __rettype, __fn, __VA_ARGS__)

// asm(".symver " __rvpredict##__major##__minor##__teeny##_##__fn " " #__fn "@GLIBC_" #__major "." #__minor "." #__teeny 
#define	ESTABLISH_PTR_TO_REAL(__fntype, __fn)	do {		\
	real_##__fn = (__fntype)(uintptr_t)dlsym(RTLD_NEXT, #__fn);	\
} while (/*CONSTCOND*/false)

#define	ESTABLISH_PTR_TO_REAL_VER3(__major, __minor, __teeny, __fntype, __fn) \
do {									\
	if ((real##__major##__minor##__teeny##_##__fn =			\
	    (__fntype)dlvsym(RTLD_NEXT, #__fn,				\
	    "GLIBC_" #__major "." #__minor "." #__teeny)) == NULL)	\
		break;							\
	real_##__fn = real##__major##__minor##__teeny##_##__fn;		\
} while (/*CONSTCOND*/false)

INTERPOSE_DECLS(int, pthread_join, pthread_t, void **);
INTERPOSE_DECLS(int, pthread_create, pthread_t *, const pthread_attr_t *,
    void *(*)(void *), void *);
INTERPOSE_DECLS(void, pthread_exit, void *);

INTERPOSE_DECLS_VER3(2, 2, 5, int, pthread_cond_init, pthread_cond_t *restrict,
    const pthread_condattr_t *restrict);
INTERPOSE_DECLS_VER3(2, 3, 2, int, pthread_cond_init, pthread_cond_t *restrict,
    const pthread_condattr_t *restrict);

INTERPOSE_DECLS_VER3(2, 2, 5, int, pthread_cond_timedwait,
    pthread_cond_t *restrict, pthread_mutex_t *restrict,
    const struct timespec *restrict);
INTERPOSE_DECLS_VER3(2, 3, 2, int, pthread_cond_timedwait,
    pthread_cond_t *restrict, pthread_mutex_t *restrict,
    const struct timespec *restrict);

INTERPOSE_DECLS_VER3(2, 2, 5, int, pthread_cond_signal, pthread_cond_t *);
INTERPOSE_DECLS_VER3(2, 3, 2, int, pthread_cond_signal, pthread_cond_t *);

INTERPOSE_DECLS_VER3(2, 2, 5, int, pthread_cond_wait, pthread_cond_t *restrict,
    pthread_mutex_t *restrict);
INTERPOSE_DECLS_VER3(2, 3, 2, int, pthread_cond_wait, pthread_cond_t *restrict,
    pthread_mutex_t *restrict);

INTERPOSE_DECLS(int, pthread_mutex_lock, pthread_mutex_t *);
INTERPOSE_DECLS(int, pthread_mutex_trylock, pthread_mutex_t *);
INTERPOSE_DECLS(int, pthread_mutex_unlock, pthread_mutex_t *);
INTERPOSE_DECLS(int, pthread_mutex_init, pthread_mutex_t *restrict,
   const pthread_mutexattr_t *restrict);

INTERPOSE_DECLS(int, sigprocmask, int, const sigset_t *, sigset_t *);
INTERPOSE_DECLS(int, pthread_sigmask, int, const sigset_t *, sigset_t *);

INTERPOSE_DECLS(int, sigaction, int, const struct sigaction *,
    struct sigaction *);

INTERPOSE_DECLS(sighandler_t, signal, int, sighandler_t);

INTERPOSE_DECLS(int, sigsuspend, const sigset_t *);

INTERPOSE_DECLS(void *, memset, void *, int, size_t);
INTERPOSE_DECLS(void *, memcpy, void *, const void *, size_t);
INTERPOSE_DECLS(void *, memmove, void *, const void *, size_t);

#endif /* _RVP_INTERPOSE_H_ */
