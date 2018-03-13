#include <err.h>
#include <limits.h>	/* for _POSIX_RTSIG_MAX */
#include <signal.h>
#include <stdlib.h>
#include <stdio.h>	/* for fprintf(3) */
#include <string.h>	/* for strerror(3) */
#include <time.h>	/* for `struct itimerspec` */

#include "intr.h"

static void renesas_78k0_init(void);
static void renesas_78k0_reinit(void);
static void __rvpredict_renesas_78k0_enable(void);
static void __rvpredict_renesas_78k0_disable(void);
void __rvpredict_renesas_78k0_handler(int);

/* 78k0 interrupt state */
struct _renesas_78k0_state {
	_Atomic bool enabled;	/* interrupts enabled
				 * (equivalent to PSW.IE)
				 */
	_Atomic bool hipri;	/* a high-priority interrupt is being
				 * serviced (equivalent to ¬PSW.ISP)
				 */
};

typedef struct _renesas_78k0_state renesas_78k0_state_t;

static renesas_78k0_state_t state = {.enabled = false, .hipri = false};

const rvp_intr_personality_t renesas_78k0_intr_personality = {
	  .ip_name = "78k0"
	, .ip_init = renesas_78k0_init
	, .ip_reinit = renesas_78k0_reinit
	, .ip_enable = __rvpredict_renesas_78k0_enable
	, .ip_splhigh = NULL
	, .ip_splx = NULL
	, .ip_disable = __rvpredict_renesas_78k0_disable
};

static sigset_t mask0, maskall, maskhigh, masklow;

static void
renesas_78k0_init(void)
{
	if (sigemptyset(&mask0) != 0)
		errx(EXIT_FAILURE, "%s: sigemptyset", __func__);
	if (sigemptyset(&maskall) != 0)
		errx(EXIT_FAILURE, "%s: sigemptyset", __func__);
	if (sigemptyset(&maskhigh) != 0)
		errx(EXIT_FAILURE, "%s: sigemptyset", __func__);
	if (sigemptyset(&masklow) != 0)
		errx(EXIT_FAILURE, "%s: sigemptyset", __func__);
}

void
__rvpredict_renesas_78k0_handler(int signum)
{
	int i;

	for (i = 0; i < rvp_static_nassigned; i++) {
		rvp_static_intr_t *si = &rvp_static_intr[i];

		if (si->si_signum != signum)
			continue;

		renesas_78k0_state_t ostate = state;

		state.hipri = si->si_prio > 0;
		state.enabled = false;

		++si->si_nactive;
		(*si->si_handler)();
		--si->si_nactive;
		si->si_times++;

		state = ostate;
	}
}

static void
renesas_78k0_reinit(void)
{
	int i, rc;
	sigset_t omask;

	if (rvp_static_nintrs > _POSIX_RTSIG_MAX)
		errx(EXIT_FAILURE, "too many interrupts");

	if (rvp_static_nassigned == rvp_static_nintrs)
		return;

	const struct itimerspec it = rvp_static_intr_interval();

	/* extend the mask */
	for (i = rvp_static_nassigned; i < rvp_static_nintrs; i++) {
		int signum = SIGRTMIN + i;
		sigset_t *maskp = (rvp_static_intr[i].si_prio == 0)
		    ? &masklow
		    : &maskhigh;

		if (rvp_static_intr_debug) {
			fprintf(stderr, "DI masks %d\n", signum);
		}
		if (sigaddset(&maskall, signum) == -1)
			errx(EXIT_FAILURE, "%s: sigaddset", __func__);
		if (sigaddset(maskp, signum) == -1)
			errx(EXIT_FAILURE, "%s: sigaddset", __func__);
	}
	if ((rc = pthread_sigmask(SIG_BLOCK, &maskall, &omask)) != 0) {
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
	/* update the mask on the previously established signals */
	for (i = 0; i < rvp_static_nassigned; i++) {
		struct sigaction sa;
		int signum = SIGRTMIN + i;
		memset(&sa, 0, sizeof(sa));

		sa.sa_mask = maskall;

		sa.sa_handler = __rvpredict_renesas_78k0_handler;
		if (sigaction(signum, &sa, NULL) == -1)
			err(EXIT_FAILURE, "%s: sigaction", __func__);
	}
	/* establish new signals */
	for (i = rvp_static_nassigned; i < rvp_static_nintrs; i++) {
		timer_t timerid;
		struct sigaction sa;
		int signum = SIGRTMIN + i;
		rvp_static_intr[i].si_signum = signum;

		memset(&sa, 0, sizeof(sa));

		sa.sa_mask = maskall;

		sa.sa_handler = __rvpredict_renesas_78k0_handler;
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

static void
__rvpredict_renesas_78k0_enable(void)
{
	renesas_78k0_state_t ostate = state;

	state.enabled = true;

	if (ostate.enabled == state.enabled)
		return;	// no change, nothing more to do

	if (state.hipri) {
		if (pthread_sigmask(SIG_UNBLOCK, &maskhigh, NULL) == -1) {
			// XXX signal safety
			err(EXIT_FAILURE, "%s.%d: pthread_sigmask", __func__,
			    __LINE__);
		}
	} else if (pthread_sigmask(SIG_UNBLOCK, &maskall, NULL) == -1) {
		// XXX signal safety
		err(EXIT_FAILURE, "%s.%d: pthread_sigmask", __func__, __LINE__);
	}
	/* To guarantee that every interrupt is observed at least once
	 * in each interrupts-enabled interval, add a request to fire
	 * all interrupts each time they are enabled.
	 */
	rvp_static_intr_fire_all();
}

static void
__rvpredict_renesas_78k0_disable(void)
{
	renesas_78k0_state_t ostate = state;

	/* To guarantee that every interrupt is observed at least once
	 * in each interrupts-enabled interval, add a request to fire
	 * all interrupts before disabling them.
	 */
	rvp_static_intr_fire_all();

	state.enabled = false;

	if (ostate.enabled == state.enabled)
		return;	// no change, nothing more to do

	if (pthread_sigmask(SIG_BLOCK, &maskall, NULL) == -1) {
		// XXX signal safety
		err(EXIT_FAILURE, "%s.%d: pthread_sigmask", __func__, __LINE__);
	}
}
