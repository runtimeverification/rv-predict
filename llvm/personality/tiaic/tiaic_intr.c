#include <assert.h>
#include <err.h>
#include <limits.h>	/* for _POSIX_RTSIG_MAX */
#include <signal.h>
#include <stdlib.h>
#include <stdio.h>	/* for fprintf(3) */
#include <string.h>	/* for strerror(3) */
#include <time.h>	/* for `struct itimerspec` */

#include "intr.h"
#include "private_intr_tiaic.h"
#include "rvpredict_intr_tiaic.h"
#include "rvpsignal.h"
#include "nbcompat.h"

#define tiaic_personality_name __rvpredict_intr_personality_name 
#define tiaic_init __rvpredict_intr_personality_init
#define tiaic_reinit __rvpredict_intr_personality_reinit
#define __rvpredict_tiaic_fire_all __rvpredict_intr_personality_fire_all

const char tiaic_personality_name[] = "tiaic";

tiaic_state_t tiaic_state;

void
tiaic_init(void)
{
	tiaic_state.total_mask = 0;
}

void
__rvpredict_tiaic_handler(int signum)
{
	int i;

	for (i = 0; i < rvp_static_nassigned; i++) {
		rvp_static_intr_t *si = &rvp_static_intr[i];

		if (si->si_signum != signum)
			continue;

		const int irq = si->si_prio;
		const int channel = irq_to_channel(irq);

		uint32_t *hinlr = (channel < 2)
		    ? &tiaic_reg[TIAIC_HINLR1]
		    : &tiaic_reg[TIAIC_HINLR2];

		// save nesting level
		uint32_t olevel = __SHIFTOUT(*hinlr, TIAIC_HINLR_NESTLVL);

		*hinlr = __SHIFTIN(MIN(olevel, channel), TIAIC_HINLR_NESTLVL);

		++si->si_nactive;
		(*si->si_handler)();
		--si->si_nactive;
		si->si_times++;

		// restore nesting level
		*hinlr = __SHIFTIN(olevel, TIAIC_HINLR_NESTLVL);
	}
}

void
tiaic_reinit(void)
{
	int i, rc;
	sigset_t omask, tmpmask;

	if (rvp_static_nintrs > _POSIX_RTSIG_MAX)
		errx(EXIT_FAILURE, "too many interrupts");

	if (rvp_static_nassigned == rvp_static_nintrs)
		return;

	const struct itimerspec it = rvp_static_intr_interval();

	/* extend the mask */
	for (i = rvp_static_nassigned; i < rvp_static_nintrs; i++) {
		int signum = SIGRTMIN + i;

		if (rvp_static_intr_debug) {
			fprintf(stderr, "total_mask contains %d\n", signum);
		}
		tiaic_state.total_mask |= __BIT(signo_to_bitno(signum));
	}
	if ((rc = pthread_sigmask(SIG_BLOCK, mask_to_sigset(tiaic_state.total_mask, &tmpmask), &omask)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}
	for (i = rvp_static_nassigned; i < rvp_static_nintrs; i++) {
		int signum = SIGRTMIN + i;
		if (sigismember(&omask, signum) == 1) {
			errx(EXIT_FAILURE,
			    "%s: did not expect signal %d to be masked",
			    __func__, signum);
		}
	}
	/* establish new signals */
	for (i = rvp_static_nassigned; i < rvp_static_nintrs; i++) {
		timer_t timerid;
		struct sigaction sa;
		int signum = SIGRTMIN + i;
		rvp_static_intr[i].si_signum = signum;

		memset(&sa, 0, sizeof(sa));

		if (sigemptyset(&sa.sa_mask) == -1)
			err(EXIT_FAILURE, "%s: sigemptyset", __func__);

		sa.sa_handler = __rvpredict_tiaic_handler;
		if (sigaction(signum, &sa, NULL) == -1)
			err(EXIT_FAILURE, "%s: sigaction", __func__);

		struct sigevent sigev = {
			  .sigev_notify = SIGEV_SIGNAL
			, .sigev_signo = signum
		};

		if (timer_create(CLOCK_MONOTONIC, &sigev, &timerid) == -1)
			err(EXIT_FAILURE, "%s: timer_create", __func__);
		timer_settime(timerid, 0, &it, NULL);
	}
	rvp_static_nassigned = rvp_static_nintrs;
}

