#include <signal.h>	/* for pthread_sigmask(3) */
#include <unistd.h>	/* for pause(2) */

#include "relay.h"
#include "thread.h"

int relay_signum = SIGUSR1;

static _Atomic int nwake = 0;
static pthread_t relay_thread;

void
rvp_wake_relay(void)
{
	int rc;
	
	nwake++;
	rc = pthread_kill(relay_thread, relay_signum);
	if (rc != 0) {
		errx(EXIT_FAILURE, "%s: pthread_kill: %s", __func__,
		    strerror(rc));
	}
}

static void *
relay(void *arg)
{
	sigset_t sigset, osigset;
	int expected_signum = -1, rc, rcvd_signum;

	sigemptyset(&sigset);

	for (;;) {
		if (expected_signum != relay_signum) {
			osigset = sigset;

			sigemptyset(&sigset);
			sigaddset(&sigset, relay_signum);

			real_pthread_sigmask(SIG_BLOCK, &sigset, NULL);
			real_pthread_sigmask(SIG_UNBLOCK, &osigset, NULL);
		}
		rc = sigwait(&sigset, &rcvd_signum);
		if (rc != 0) {
			errx(EXIT_FAILURE, "%s: pthread_kill: %s", __func__,
			    strerror(rc));
		}
		assert(rcvd_signum == expected_signum);
		while (nwake > 0) {
			rvp_wake_transmitter();
			nwake--;
		}
	}
	return NULL;
}

void
rvp_relay_create(void)
{
	int rc;

	if ((rc = real_pthread_create(&relay_thread, NULL, relay, NULL)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_create: %s", __func__,
		    strerror(rc));
	}
}
