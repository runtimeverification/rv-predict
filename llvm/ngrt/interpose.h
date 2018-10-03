#ifndef _RVP_INTERPOSE_H_
#define _RVP_INTERPOSE_H_


#include <stdlib.h>	/* For strlen, strchr, ... */
#include <dlfcn.h>	/* for dlsym(3) */
#include <pthread.h>	/* for pthread_{join,create,exit}(3),
			 * pthread_mutex_{init,lock,trylock,unlock}(3), etc.
			 */
#include <signal.h>	/* for sigprocmask(3), sigaction(3), signal(3),
			 * sigsuspend(3), etc.
			 */
#include <stdbool.h>	/* for false */
#include <string.h>	/* for memcpy(3), memmove(3), memset(3) */
#include <unistd.h>	/* for fork(2) */

#include "atomic.h"

#define	REAL_DECL(__rettype, __func, ...)				\
	extern __rettype (*real_##__func)(__VA_ARGS__)

#define	REAL_DEFN(__rettype, __func, ...)				\
	__rettype (*real_##__func)(__VA_ARGS__)

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

#define	ESTABLISH_PTR_TO_REAL(__fntype, __fn)	do {		\
	real_##__fn = (__fntype)(uintptr_t)dlsym(RTLD_NEXT, #__fn);	\
} while (/*CONSTCOND*/false)

INTERPOSE_DECLS(pid_t, fork, void);
INTERPOSE_DECLS(int, pthread_join, pthread_t, void **);
INTERPOSE_DECLS(int, pthread_create, pthread_t *, const pthread_attr_t *,
    void *(*)(void *), void *);
INTERPOSE_DECLS(void, pthread_exit, void *);

INTERPOSE_DECLS(int, pthread_key_create, pthread_key_t *, void (*)(void *));
INTERPOSE_DECLS(int, pthread_key_delete, pthread_key_t);
INTERPOSE_DECLS(int, pthread_setspecific, pthread_key_t, const void *);

INTERPOSE_DECLS(int, pthread_mutex_lock, pthread_mutex_t *);
INTERPOSE_DECLS(int, pthread_mutex_trylock, pthread_mutex_t *);
INTERPOSE_DECLS(int, pthread_mutex_unlock, pthread_mutex_t *);
INTERPOSE_DECLS(int, pthread_mutex_init, pthread_mutex_t *restrict,
   const pthread_mutexattr_t *restrict);

INTERPOSE_DECLS(int, sigprocmask, int, const sigset_t *, sigset_t *);
INTERPOSE_DECLS(int, pthread_sigmask, int, const sigset_t *, sigset_t *);

INTERPOSE_DECLS(int, sigaction, int, const struct sigaction *,
    struct sigaction *);

typedef void (*rvp_sighandler_t)(int);

INTERPOSE_DECLS(rvp_sighandler_t, __sysv_signal, int, rvp_sighandler_t);
INTERPOSE_DECLS(rvp_sighandler_t, signal, int, rvp_sighandler_t);

INTERPOSE_DECLS(int, sigsuspend, const sigset_t *);

INTERPOSE_DECLS(void *, memset, void *, int, size_t);
INTERPOSE_DECLS(void *, memcpy, void *, const void *, size_t);
INTERPOSE_DECLS(void *, memmove, void *, const void *, size_t);

INTERPOSE_DECLS(size_t , strlen,  const char *);
INTERPOSE_DECLS(char * , strchrnul ,  const char *, int );
////INTERPOSE_DECLS(char * , strchr ,  const char *, int );
INTERPOSE_DECLS(char * , strcpy ,  char *, const char *);
INTERPOSE_DECLS(char * , strdup ,  const char *);
//INTERPOSE_DECLS(char * , strdupa ,  const char *);
INTERPOSE_DECLS(char * , strndup ,  const char *, size_t);
//INTERPOSE_DECLS(char * , strndupa ,  const char *, size_t );
INTERPOSE_DECLS(char * , strncpy ,  char *, const char *, size_t );
INTERPOSE_DECLS(char * , strrchr ,  const char *, int );

#endif /* _RVP_INTERPOSE_H_ */
