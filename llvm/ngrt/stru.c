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

/* Forwards and REAL decs */
/* 
 * There are unresolved issues with strchr,strudupa,strndup
 *
 */


size_t __rvpredict_strlen(const char*);

char * __rvpredict_strchrnul(const char *, int );
//char * __rvpredict_strchr(const char *, int );
char * __rvpredict_strcpy(char *, const char *);
char * __rvpredict_strdup(const char *);
//char * __rvpredict_strdupa(const char *);
//char * __rvpredict_strndup(const char *, size_t);
char * __rvpredict_strndupa(const char *, size_t );
char * __rvpredict_strncpy(char *, const char *, size_t );
char * __rvpredict_strrchr(const char *, int );


REAL_DECL(size_t, strlen, const char *);
REAL_DEFN(size_t, strlen, const char *);

REAL_DECL(char *, strchrnul, const char *, int );
REAL_DEFN(char *, strchrnul, const char *, int );
//REAL_DECL(char *, strchr, const char *, int );
//REAL_DEFN(char *, strchr, const char *, int );
REAL_DECL(char *, strcpy, char *, const char *);
REAL_DEFN(char *, strcpy, char *, const char *);
REAL_DECL(char *, strdup, const char *);
REAL_DEFN(char *, strdup, const char *);
//REAL_DECL(char *, strdupa, const char *);
//REAL_DEFN(char *, strdupa, const char *);
REAL_DECL(char *, strndup, const char *, size_t);
REAL_DEFN(char *, strndup, const char *, size_t);
//REAL_DECL(char *, strndupa, const char *, size_t );
//REAL_DEFN(char *, strndupa, const char *, size_t );
REAL_DECL(char *, strncpy, char *, const char *, size_t );
REAL_DEFN(char *, strncpy, char *, const char *, size_t );
REAL_DECL(char *, strrchr, const char *, int );
REAL_DEFN(char *, strrchr, const char *, int );

/*
 * Do the prefork setup
 */
void
rvp_stru_prefork_init(void)
{	/* Called by rvp_str_prefork_init in str.c */

	ESTABLISH_PTR_TO_REAL(size_t (*)(const char *), strlen);
	ESTABLISH_PTR_TO_REAL(char * (*)(const char *, int) , strchrnul);
//	ESTABLISH_PTR_TO_REAL(char * (*)(const char *, int) , strchr);
	ESTABLISH_PTR_TO_REAL(char * (*)(char *, const char *), strcpy);
	ESTABLISH_PTR_TO_REAL(char * (*)(const char *), strdup);
//	ESTABLISH_PTR_TO_REAL(char * (*)(const char *), strdupa);
//	ESTABLISH_PTR_TO_REAL(char * (*)(const char *, size_t) , strndupa);
	ESTABLISH_PTR_TO_REAL(char * (*)(const char *, size_t), strndup);
	ESTABLISH_PTR_TO_REAL(char * (*)(char *, const char *, size_t) , strncpy);
	ESTABLISH_PTR_TO_REAL(char * (*)(const char *, int) , strrchr);
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

char *
__rvpredict_strrchr(const char *pp, int  q)
{
   return real_strrchr(pp,q);
}

char *
__rvpredict_strdup(const char * q)
{
   return real_strdup(q);
}

#if 0
char *
__rvpredict_strdupa(const char * q)
{
   return real_strdupa(q);
}
char *
__rvpredict_strndupa(const char *pp, size_t  q)
{
   return real_strndupa(pp,q);
}
#endif

char *
__rvpredict_strndup(const char *pp, size_t q)
{
   return real_strndup(pp,q);
}

char *
__rvpredict_strncpy(char *dest, const char *src, size_t  q)
{
   return real_strncpy(dest,src,q);
}

char *
__rvpredict_strcpy(char *pp, const char * q)
{
   return real_strcpy(pp,q);
}

#if 0
char *
__rvpredict_strchr(const char *, int  q)
{
   return real_strchr(q);
}
#endif

char *
__rvpredict_strchrnul(const char *pp, int  q)
{
   return real_strchrnul(pp,q);
}

INTERPOSE(size_t , strlen,  const char *);
INTERPOSE(char * , strchrnul, const char *, int );
//INTERPOSE(char * , strchr,    const char *, int );
INTERPOSE(char * , strcpy,    char *, const char *);
INTERPOSE(char * , strdup,    const char *);
//INTERPOSE(char * , strdupa,   const char *);
//INTERPOSE(char * , strndupa,  const char *, size_t );
INTERPOSE(char * , strndup,   const char *, size_t);
INTERPOSE(char * , strncpy,   char *, const char *, size_t );
INTERPOSE(char * , strrchr,   const char *, int );
