#include <err.h>
#include <errno.h>
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
	rvp_intr_handler_t		si_handler;
	volatile _Atomic int32_t	si_prio;
	volatile _Atomic int		si_signum;
	volatile _Atomic int		si_nactive;
	volatile _Atomic uint32_t	si_times;
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
		rvp_static_intr_t *si = &rvp_static_intr[i];
		if (si->si_signum == signum) {
			(*si->si_handler)();
			si->si_times++;
		}
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
		/* Don't let this signal run on top of itself. */
		if (si->si_nactive > 0)
			continue;
		++si->si_nactive;
		raise(si->si_signum);
		--si->si_nactive;
	}
}

void
__rvpredict_isr_fire(void (*isr)(void))
{
	int i;
	bool fired = false;
	sigset_t nonintr_mask;

	if (sigemptyset(&nonintr_mask) == -1)
		err(EXIT_FAILURE, "%s: sigemptyset", __func__);

	for (i = 0; i < nassigned; i++) {
		rvp_static_intr_t *si = &rvp_static_intr[i];
		if (si->si_handler != isr)
			continue;
		if (si->si_signum == -1)
			continue;
		const uint32_t times = si->si_times;
		raise(si->si_signum);
		while (times == si->si_times) {
			(void)sigsuspend(&nonintr_mask);
		}
		fired = true;
	}
	if (!fired)
		abort();
}

void
rvp_static_intrs_reinit(void)
{
	int i;

	if (rvp_static_nintrs > _POSIX_RTSIG_MAX)
		errx(EXIT_FAILURE, "too many interrupt priorities");

	for (i = nassigned; i < rvp_static_nintrs; i++) {
		struct sigaction sa;
		int signum = SIGRTMIN + i;
		rvp_static_intr[i].si_signum = signum;

		memset(&sa, 0, sizeof(sa));

		sa.sa_mask = intr_mask;

		sa.sa_handler = rvp_static_intr_handler;
		if (sigaction(signum, &sa, NULL) == -1)
			err(EXIT_FAILURE, "%s: sigaction", __func__);

		long nsec = 1000 * 1000;	// 1 millisecond
		const char *ivalenv = getenv("RVP_INTR_INTERVAL");
		if (ivalenv != NULL) {
			char *end;
			errno = 0;
			nsec = strtol(ivalenv, &end, 10);
			if (errno != 0) {
				err(EXIT_FAILURE,
				    "could not interpret "
				    "RVP_INTR_INTERVAL (%s) "
				    "as a decimal number", ivalenv);
			} else if (*end != '\0') {
				errx(EXIT_FAILURE,
				    "garbage at end of RVP_INTR_INTERVAL (%s)",
				    ivalenv);
			}
		}
		const struct itimerspec it = {
			  .it_value = {
				  .tv_sec = 0
				, .tv_nsec = nsec
			  }
			, .it_interval = {
				  .tv_sec = 0
				, .tv_nsec = nsec
			  }
		};

		struct sigevent sigev = {
			  .sigev_notify = SIGEV_SIGNAL
			, .sigev_signo = signum
		};
		timer_t timerid;
		if (timer_create(CLOCK_MONOTONIC, &sigev, &timerid) == -1)
			err(EXIT_FAILURE, "%s: timer_create", __func__);
		timer_settime(timerid, 0, &it, NULL);
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
		int signum = SIGRTMIN + i;
		if (sigismember(&original_mask, signum) == 1) {
			errx(EXIT_FAILURE,
			    "%s: did not expect signal %d to be masked",
			    __func__, signum);
		}
		if (intr_debug) {
			fprintf(stderr, "DI masks %d\n", signum);  
		}
		if (sigaddset(&intr_mask, signum) != 0)
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
