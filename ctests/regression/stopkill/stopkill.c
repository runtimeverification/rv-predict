/* The RV-Predict/C runtime has diagnostic code that calls `abort()` if
 * it detects a discrepancy between the expected interrupt/signal mask
 * (stored in `rvp_thread_t` member `t_intrmask`) and the old mask that
 * `pthread_sigmask()` reads back.  Frequently the discrepancy arose
 * because you cannot actually block SIGKILL and SIGSTOP.  So after a
 * program sets a mask that contains SIGKILL and SIGSTOP, it will read
 * back a mask that contains neither.  The runtime needs to account for
 * this, and it will.
 *
 * This program is designed to `abort()` and, as a side-effect, produce
 * an unexpected trace file, if the runtime is not accounting properly
 * for the unblockability of SIGKILL and SIGSTOP.
 */

#include <err.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>

#include "nbcompat.h"

int
main(void)
{
	sigset_t mask, omask;

	if (sigfillset(&mask) != 0)
		err(EXIT_FAILURE, "%s: sigfillset", __func__);

	if (pthread_sigmask(SIG_SETMASK, &mask, NULL) != 0)
		err(EXIT_FAILURE, "%s.%d: pthread_sigmask", __func__, __LINE__);

	if (pthread_sigmask(SIG_SETMASK, &mask, &omask) != 0)
		err(EXIT_FAILURE, "%s.%d: pthread_sigmask", __func__, __LINE__);

	return EXIT_SUCCESS;
}
