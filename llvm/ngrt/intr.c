#include <err.h>
#include <errno.h>
#include <inttypes.h>	/* for PRId32 */
#include <limits.h>	/* for _POSIX_RTSIG_MAX */
#include <signal.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>	/* for timer_create(2) */

#include "func.h"
#include "init.h"
#include "intr.h"	/* for prototypes */
#include "ring.h"	/* for rvp_ring_t */
#include "thread.h"	/* for rvp_ring_for_curthr() */
#include "rvpredict_intr.h"
#include "nbcompat.h"

rvp_static_intr_t rvp_static_intr[128];

int rvp_static_nintrs = 0;
int rvp_static_nassigned = 0;

bool rvp_static_intr_debug = false;

struct itimerspec
rvp_static_intr_interval(void)
{
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
	return (struct itimerspec){
		  .it_value = {
			  .tv_sec = 0
			, .tv_nsec = nsec
		  }
		, .it_interval = {
			  .tv_sec = 0
			, .tv_nsec = nsec
		  }
	};
}

void
rvp_static_intr_fire_all(void)
{
	__rvpredict_intr_personality_fire_all();
}

void
__rvpredict_isr_fire(void (*isr)(void))
{
	const void *retaddr = __rvpredict_func_entry(
	    __builtin_dwarf_cfa(), __builtin_return_address(0));
	int i;
	bool fired = false;
	sigset_t nonintr_mask;

	if (sigemptyset(&nonintr_mask) == -1)
		err(EXIT_FAILURE, "%s: sigemptyset", __func__);

	for (i = 0; i < rvp_static_nassigned; i++) {
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
	__rvpredict_func_exit(retaddr);
}

void
rvp_static_intrs_reinit(void)
{
	__rvpredict_intr_personality_reinit();
}

void
rvp_static_intrs_init(void)
{
	const char *debugenv = getenv("RVP_INTR_DEBUG");

	rvp_static_intr_debug = debugenv != NULL &&
	    strcmp(debugenv, "yes") == 0;

	if (rvp_static_intr_debug) {
		fprintf(stderr, "%d signal handlers found\n",
		    rvp_static_nintrs);
	}

	if (rvp_static_intr_debug) {
		warnx("Established %s interrupt personality.",
		    __rvpredict_intr_personality_name);
	}

	__rvpredict_intr_personality_init();
}

void
__rvpredict_intr_disable(void)
{
	const void *retaddr = __rvpredict_func_entry(
	    __builtin_dwarf_cfa(), __builtin_return_address(0));
	__rvpredict_intr_personality_disable();
	__rvpredict_func_exit(retaddr);
}

/* XXX This isn't suitable for multithreaded systems. */
int
__rvpredict_splhigh(void)
{
	const void *retaddr = __rvpredict_func_entry(
	    __builtin_dwarf_cfa(), __builtin_return_address(0));

	const int retval = __rvpredict_intr_personality_splhigh();
	__rvpredict_func_exit(retaddr);
	return retval;
}

void
__rvpredict_splx(int level)
{
	const void *retaddr = __rvpredict_func_entry(
	    __builtin_dwarf_cfa(), __builtin_return_address(0));

	__rvpredict_intr_personality_splx(level);
	__rvpredict_func_exit(retaddr);
}

void
__rvpredict_intr_enable(void)
{
	const void *retaddr = __rvpredict_func_entry(
	    __builtin_dwarf_cfa(), __builtin_return_address(0));
	__rvpredict_intr_personality_enable();
	__rvpredict_func_exit(retaddr);
}

void
__rvpredict_intr_register(void (*handler)(void), int32_t prio)
{
	if (rvp_static_intr_debug) {
		fprintf(stderr, "%s: handler %p prio %" PRId32 "\n",
		    __func__, (const void *)(uintptr_t)handler, prio);
	}
	if (rvp_static_nintrs >= __arraycount(rvp_static_intr)) {
		errx(EXIT_FAILURE,
		    "%s: no room for handler %p prio %" PRId32 "\n",
		    __func__, (const void *)(uintptr_t)handler, prio);
	}
	rvp_static_intr[rvp_static_nintrs++] =
	    (rvp_static_intr_t){.si_handler = handler, .si_prio = prio,
	                        .si_signum = -1, .si_nactive = 0};
}
