/*
 * stru.c - Handles routines like strlen
 *        - We just need to get in the ring the memory areas referenced
 *        - There is no independent RV implementation (like for memcpy).
 */

#include "access.h"
#include "init.h"
#include "interpose.h"
#include "nbcompat.h"
#include "str.h"
#include "supervise.h"
#include "text.h"
#include "tracefmt.h"
#include "stru.h"

#if 0
       size_t strlen(const char *s);
INTERPOSE_DECLS(size_t , strlen, void *, const char *);

lock.c:	ESTABLISH_PTR_TO_REAL(int (*)(pthread_mutex_t *), pthread_mutex_lock);
:int __rvpredict_pthread_mutex_lock(pthread_mutex_t *);
:REAL_DECL(int, pthread_mutex_lock, pthread_mutex_t *);
:REAL_DEFN(int, pthread_mutex_lock, pthread_mutex_t *);


REAL_DEFN(void *, memcpy, void *, const void *, size_t) =
    __rvpredict_internal_memcpy;
REAL_DEFN(void *, memmove, void *, const void *, size_t) =
    __rvpredict_internal_memmove;
REAL_DEFN(void *, memset, void *, int, size_t) =
    __rvpredict_internal_memset;

void
rvp_str_prefork_init(void)
{
	ESTABLISH_PTR_TO_REAL(void *(*)(void *, const void *, size_t), memcpy);
	ESTABLISH_PTR_TO_REAL(void *(*)(void *, const void *, size_t), memmove);
	ESTABLISH_PTR_TO_REAL(void *(*)(void *, int, size_t), memset);
}

void *
__rvpredict_memmove1(const void *retaddr,
    const rvp_addr_t dst, const rvp_addr_t src, size_t n)
{
	rvp_addr_t to, from;


 error: redeclaration of 'real_strlen' with a 
      different type: 'size_t (*)(char *)' (aka 'unsigned long (*)(char *)') vs
                      'size_t (*)(const char *)' (aka 'unsigned long (*)(const char *)')

#endif
size_t __rvpredict_strlen(const char*);

REAL_DECL(size_t, strlen, const char *);
REAL_DEFN(size_t, strlen, const char *);

void
rvp_stru_prefork_init(void)
{	/* Called by rvp_str_prefork_init in str.c */

	ESTABLISH_PTR_TO_REAL(size_t (*)(const char *), strlen);
	return;
}
/*
 *   Now the routines called from the users program (after
 * RPredict has fiddled with the llvm
 */
size_t 
__rvpredict_strlen(const char* s)
{
	size_t ii;
	ii = real_strlen(s);
	return ii;
}

INTERPOSE_DECLS(size_t , strlen,  const char *);
