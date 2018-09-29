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

INTERPOSE(size_t , strlen,  const char *);
