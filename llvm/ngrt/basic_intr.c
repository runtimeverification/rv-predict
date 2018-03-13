#include <err.h>
#include <errno.h>
#include <stdio.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>

#include "intr.h"
#include "thread.h"

enum {
	  RVP_IPL_0 = 0
	, RVP_IPL_HIGH
};

static void basic_init(void);
static void basic_reinit(void);
static void basic_enable(void);
static int basic_splhigh(void);
static void basic_splx(int);
static void basic_disable(void);

const rvp_intr_personality_t basic_intr_personality = {
	  .ip_name = "basic"
	, .ip_init = basic_init
	, .ip_reinit = basic_reinit
	, .ip_enable = basic_enable
	, .ip_splhigh = basic_splhigh
	, .ip_splx = basic_splx
	, .ip_disable = basic_disable
};

static sigset_t intr_mask;
static sigset_t mask0;

static volatile _Atomic int rvp_ipl = RVP_IPL_HIGH;

static void
basic_init(void)
{
	if (sigemptyset(&mask0) != 0)
		errx(EXIT_FAILURE, "%s: sigemptyset", __func__);
	if (sigemptyset(&intr_mask) != 0)
		errx(EXIT_FAILURE, "%s: sigemptyset", __func__);
}

static void
basic_disable(void)
{
	int rc;

	/* To guarantee that every interrupt is observed at least once
	 * in each interrupts-enabled interval, add a request to fire
	 * all interrupts before disabling them.
	 */
	rvp_static_intr_fire_all();
	if ((rc = pthread_sigmask(SIG_BLOCK, &intr_mask, NULL)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}
}

static void
basic_enable(void)
{
	int rc;

	rvp_ipl = RVP_IPL_0;

	if ((rc = pthread_sigmask(SIG_UNBLOCK, &intr_mask, NULL)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}

	/* To guarantee that every interrupt is observed at least once
	 * in each interrupts-enabled interval, add a request to fire
	 * all interrupts each time they are enabled.
	 */
	rvp_static_intr_fire_all();
}

static int
basic_splhigh(void)
{
	int rc;
	int old_ipl;
	rvp_ring_t *r = rvp_ring_for_curthr();

	if (rvp_ipl == RVP_IPL_HIGH)
		return rvp_ipl;

	/* To guarantee that every interrupt is observed at least once
	 * in each interrupts-enabled interval, add a request to fire
	 * all interrupts before disabling them.
	 */
	rvp_static_intr_fire_all();
	if ((rc = pthread_sigmask(SIG_BLOCK, &intr_mask,
	                          &r->r_intr_hack.ih_mask)) != 0) {
		// XXX signal safety
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}
	old_ipl = rvp_ipl;
	rvp_ipl = RVP_IPL_HIGH;
	return old_ipl;
}

static void
basic_splx(int level)
{
	int rc;
	rvp_ring_t *r = rvp_ring_for_curthr();

	switch (level) {
	case RVP_IPL_HIGH:
		return;
	case RVP_IPL_0:
		break;
	default:
		// XXX signal safety
		errx(EXIT_FAILURE, "%s: unknown level %d", __func__, level);
	}

	rvp_ipl = level;

	if ((rc = pthread_sigmask(SIG_SETMASK,
	                          &r->r_intr_hack.ih_mask, NULL)) != 0) {
		// XXX signal safety
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}

	/* To guarantee that every interrupt is observed at least once
	 * in each interrupts-enabled interval, add a request to fire
	 * all interrupts each time they are enabled.
	 */
	rvp_static_intr_fire_all();
}

static void
basic_reinit(void)
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
		if (rvp_static_intr_debug) {
			fprintf(stderr, "DI masks %d\n", signum);
		}
		if (sigaddset(&intr_mask, signum) != 0)
			errx(EXIT_FAILURE, "%s: sigaddset", __func__);
	}
	if ((rc = pthread_sigmask(SIG_BLOCK, &intr_mask, &omask)) != 0) {
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

		sa.sa_mask = intr_mask;

		sa.sa_handler = __rvpredict_static_intr_handler;
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

		sa.sa_mask = intr_mask;

		sa.sa_handler = __rvpredict_static_intr_handler;
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
