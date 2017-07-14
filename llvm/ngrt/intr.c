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
	int			si_signum;
} rvp_static_intr_t;

static rvp_static_intr_t rvp_static_intr[128];

static int rvp_static_nintrs = 0;

static bool intr_debug = false;

static const int avail_signals[] = {SIGALRM, SIGCHLD, SIGUSR2};
static sigset_t intr_mask;

static int
rvp_static_intr_compare(const void *l, const void *r)
{
	const rvp_static_intr_t *lsi = l, *rsi = r;

	return lsi->si_prio - rsi->si_prio;
}

static void
rvp_static_intr_handler(int signum)
{
	int i;

	for (i = 0; i < rvp_static_nintrs; i++) {
		const rvp_static_intr_t *si = &rvp_static_intr[i];
		if (si->si_signum == signum)
			(*si->si_handler)();
	}
}

#if 0
static void
rvp_static_intr_fire_all(void)
{
	int i;

	for (i = 0; i < rvp_static_nintrs; i++) {
		const rvp_static_intr_t *si = &rvp_static_intr[i];
		raise(si->si_signum);
	}
}
#endif

void
rvp_static_intrs_init(void)
{
	int i, j, nprio = 0, rc;
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

	for (i = 0; i < __arraycount(avail_signals); i++) {
		if (sigismember(&original_mask, avail_signals[i]) == 1) {
			errx(EXIT_FAILURE,
			    "%s: did not expect signal %d to be masked",
			    __func__, avail_signals[i]);
		}
		if (sigaddset(&intr_mask, avail_signals[i]) != 0)
			errx(EXIT_FAILURE, "%s: sigaddset", __func__);
	}

	qsort(rvp_static_intr, rvp_static_nintrs, sizeof(rvp_static_intr[0]),
	    rvp_static_intr_compare);

	for (i = 0; i < rvp_static_nintrs; i++) {
		if (i == 0 ||
		    rvp_static_intr[i - 1].si_prio !=
		    rvp_static_intr[i].si_prio)
			nprio++;
	}

	if (nprio > __arraycount(avail_signals))
		errx(EXIT_FAILURE, "too many interrupt priorities");

	for (i = j = 0; i < rvp_static_nintrs; i++) {
		if (i > 0 &&
		    rvp_static_intr[i - 1].si_prio !=
		    rvp_static_intr[i].si_prio)
			j++;
		rvp_static_intr[i].si_signum = avail_signals[j];
	}

	for (i = 0; i < nprio; i++) {
		struct sigevent sigev = {
			  .sigev_notify = SIGEV_SIGNAL
			, .sigev_signo = avail_signals[i]
		};
		struct sigaction sa;
		timer_t timerid;
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

		memset(&sa, 0, sizeof(sa));

		if (timer_create(CLOCK_MONOTONIC, &sigev, &timerid) == -1)
			err(EXIT_FAILURE, "%s: timer_create", __func__);

		if (sigemptyset(&sa.sa_mask) == -1)
			err(EXIT_FAILURE, "%s: sigemptyset", __func__);

		for (j = i; j < nprio; j++) {
			if (sigaddset(&sa.sa_mask, avail_signals[j]) == -1)
				err(EXIT_FAILURE, "%s: sigaddset", __func__);
		}
		sa.sa_handler = rvp_static_intr_handler;
		if (sigaction(avail_signals[i], &sa, NULL) == -1)
			err(EXIT_FAILURE, "%s: sigaction", __func__);

		timer_settime(timerid, 0, &it, NULL);
	}
}

void
__rvpredict_intr_disable(void)
{
	int rc;

#if 0
	rvp_static_intr_fire_all();
#endif
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
#if 0
	rvp_static_intr_fire_all();
#endif
}

void
__rvpredict_intr_register(void (*handler)(void), int32_t prio)
{
	prio = 0;
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
	    (rvp_static_intr_t){.si_handler = handler, .si_prio = prio};
}
