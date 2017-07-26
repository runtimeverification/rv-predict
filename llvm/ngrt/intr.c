#include <err.h>
#include <inttypes.h> /* for PRId32 */
#include <signal.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>	/* for timer_create(2) */

#include "init.h"
#include "intr_exports.h"
#include "nbcompat.h"

typedef void (*rvp_intr_handler_t)(void);

typedef struct _rvp_static_intr {
	rvp_intr_handler_t	si_handler;
	int32_t			si_prio;
	volatile _Atomic int	si_signum;
	volatile _Atomic int	si_nactive;
} rvp_static_intr_t;

static rvp_static_intr_t rvp_static_intr[128];

static int rvp_static_nintrs = 0;
static int nassigned = 0;

static bool intr_debug = false;

static sigset_t intr_mask;

static void
rvp_static_intr_handler(int signum)
{
	int i;

	for (i = 0; i < nassigned; i++) {
		const rvp_static_intr_t *si = &rvp_static_intr[i];
		if (si->si_signum == signum)
			(*si->si_handler)();
	}
}

static void
rvp_static_intr_fire_all(void)
{
	int i;

	for (i = 0; i < nassigned; i++) {
		rvp_static_intr_t *si = &rvp_static_intr[i];
		if (si->si_signum == -1)
			continue;
		if (si->si_nactive > 1)
			continue;
		++si->si_nactive;
		raise(si->si_signum);
		--si->si_nactive;
	}
}

void
rvp_static_intrs_reinit(void)
{
	int i, j;

	if (rvp_static_nintrs > _POSIX_RTSIG_MAX)
		errx(EXIT_FAILURE, "too many interrupt priorities");

	for (i = nassigned; i < rvp_static_nintrs; i++) {
		struct sigaction sa;
#ifdef RVP_PERIODIC
		const struct itimerspec it = {
			  .it_value = {
				  .tv_sec = 0
				, .tv_nsec = 100 * 1000
			  }
			, .it_interval = {
				  .tv_sec = 0
				, .tv_nsec = 100 * 1000
			  }
		};

		struct sigevent sigev = {
			  .sigev_notify = SIGEV_SIGNAL
			, .sigev_signo = SIGRTMIN + i
		};
		timer_t timerid;
		if (timer_create(CLOCK_MONOTONIC, &sigev, &timerid) == -1)
			err(EXIT_FAILURE, "%s: timer_create", __func__);
#endif
		rvp_static_intr[i].si_signum = SIGRTMIN + i;

		memset(&sa, 0, sizeof(sa));

		if (sigemptyset(&sa.sa_mask) == -1)
			err(EXIT_FAILURE, "%s: sigemptyset", __func__);

		for (j = 0; j < _POSIX_RTSIG_MAX; j++) {
			if (intr_debug) {
				fprintf(stderr, "signal %d masks %d\n",
				    rvp_static_intr[i].si_signum,
				    SIGRTMIN + j);  
			}
			if (sigaddset(&sa.sa_mask, SIGRTMIN + j) == -1)
				err(EXIT_FAILURE, "%s: sigaddset", __func__);
		}
		sa.sa_handler = rvp_static_intr_handler;
		if (sigaction(SIGRTMIN + i, &sa, NULL) == -1)
			err(EXIT_FAILURE, "%s: sigaction", __func__);

#ifdef RVP_PERIODIC
		timer_settime(timerid, 0, &it, NULL);
#endif
	}
	nassigned = rvp_static_nintrs;
}

void
rvp_static_intrs_init(void)
{
	int i, rc;
	sigset_t original_mask;

	const char *debugenv = getenv("RVP_INTR_DEBUG");

	intr_debug = debugenv != NULL && strcmp(debugenv, "yes") == 0;

	if (intr_debug) {
		fprintf(stderr, "%d signal handlers found\n",
		    rvp_static_nintrs);
	}

	rc = pthread_sigmask(SIG_BLOCK, NULL, &original_mask);
	if (rc != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}

	if (sigemptyset(&intr_mask) != 0)
		errx(EXIT_FAILURE, "%s: sigemptyset", __func__);

	for (i = 0; i < _POSIX_RTSIG_MAX; i++) {
		if (sigismember(&original_mask, SIGRTMIN + i) == 1) {
			errx(EXIT_FAILURE,
			    "%s: did not expect signal %d to be masked",
			    __func__, SIGRTMIN + i);
		}
		if (sigaddset(&intr_mask, SIGRTMIN + i) != 0)
			errx(EXIT_FAILURE, "%s: sigaddset", __func__);
	}
}

void
__rvpredict_intr_disable(void)
{
	int rc;

	rvp_static_intr_fire_all();
	if ((rc = pthread_sigmask(SIG_BLOCK, &intr_mask, NULL)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}
}

void
__rvpredict_intr_enable(void)
{
	int rc;

	if ((rc = pthread_sigmask(SIG_UNBLOCK, &intr_mask, NULL)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_sigmask: %s", __func__,
		    strerror(rc));
	}

	rvp_static_intr_fire_all();
}

void
__rvpredict_intr_register(void (*handler)(void), int32_t prio)
{
	if (intr_debug) {
		fprintf(stderr, "%s: handler %p prio %" PRId32 "\n",
		    __func__, (const void *)handler, prio);
	}
	if (rvp_static_nintrs >= __arraycount(rvp_static_intr)) {
		errx(EXIT_FAILURE,
		    "%s: no room for handler %p prio %" PRId32 "\n",
		    __func__, (const void *)handler, prio);
	}
	rvp_static_intr[rvp_static_nintrs++] =
	    (rvp_static_intr_t){.si_handler = handler, .si_prio = prio,
	                        .si_signum = -1, .si_nactive = 0};
}
