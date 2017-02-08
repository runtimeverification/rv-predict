#include <err.h>
#include <signal.h>	/* for sigismember */
#include <stdlib.h>	/* for malloc */

#include "sigutil.h"
#include "lock.h"	/* for real_pthread_mutex_lock */
#include "rvpsignal.h"	/* for rvp_sigblockset_t */

bool
sigeqset(const sigset_t *l, const sigset_t *r)
{
	sigset_t all;
	int i, memb;

	if (sigfillset(&all) == -1)
		err(EXIT_FAILURE, "%s: sigfillset", __func__);

	/* skip over leading invalid signals */
	for (i = 0; sigismember(&all, i) == -1; i++)
		;	// do nothing

	for (; (memb = sigismember(&all, i)) == 1; i++) {
		int lmemb, rmemb;

		if ((lmemb = sigismember(l, i)) == -1)
			err(EXIT_FAILURE, "%s: sigismember", __func__);
		if ((rmemb = sigismember(r, i)) == -1)
			err(EXIT_FAILURE, "%s: sigismember", __func__);

		if (lmemb != rmemb) 
			return false;
	}
	if (memb == -1)
		err(EXIT_FAILURE, "%s: sigismember", __func__);
	return true;
}

rvp_sigblockset_t *
intern_sigset(const sigset_t *s)
{
	rvp_sigblockset_t *bs;
	static rvp_sigblockset_t *sigblockset_head = NULL;
	static uint32_t nsigblocksets = 0;
	static pthread_mutex_t sigblockset_lock = PTHREAD_MUTEX_INITIALIZER;

	real_pthread_mutex_lock(&sigblockset_lock);
	for (bs = sigblockset_head; bs != NULL; bs = bs->bs_next) {
		if (sigeqset(&bs->bs_sigset, s))
			break;
	}

	if (bs == NULL) {
		if ((bs = malloc(sizeof(*bs))) == NULL)
			err(EXIT_FAILURE, "%s: malloc", __func__);
		bs->bs_number = nsigblocksets++;
		bs->bs_sigset = *s;
		bs->bs_next = sigblockset_head;
		sigblockset_head = bs;
		// TBD write to trace, and write COG? 
	}

	real_pthread_mutex_unlock(&sigblockset_lock);

	return bs;
}


