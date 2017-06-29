#include <err.h>
#include <signal.h>	/* for sigismember */
#include <stdlib.h>	/* for malloc */

#include "sigutil.h"
#include "lock.h"	/* for real_pthread_mutex_lock */
#include "rvpsignal.h"	/* for rvp_sigblockset_t */

/* Return the least signal number that's present in `s` or -1 if
 * no signals are present.
 */
int
sigfirstmember(const sigset_t *s)
{
	sigset_t all;
	int i, memb;

	if (sigfillset(&all) == -1)
		err(EXIT_FAILURE, "%s: sigfillset", __func__);

	/* skip over leading invalid signals */
	for (i = 0; sigismember(&all, i) == -1; i++)
		;	// do nothing

	for (; (memb = sigismember(&all, i)) != -1; i++) {
		int smemb;

		if (memb == 0)
			continue;

		if ((smemb = sigismember(s, i)) == -1)
			err(EXIT_FAILURE, "%s: sigismember", __func__);

		if (smemb == 1)
			return i;
	}
	return -1;
}

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

	for (; (memb = sigismember(&all, i)) != -1; i++) {
		int lmemb, rmemb;

		if (memb == 0)
			continue;

		if ((lmemb = sigismember(l, i)) == -1)
			err(EXIT_FAILURE, "%s: sigismember", __func__);
		if ((rmemb = sigismember(r, i)) == -1)
			err(EXIT_FAILURE, "%s: sigismember", __func__);

		if (lmemb != rmemb)
			return false;
	}
	return true;
}
