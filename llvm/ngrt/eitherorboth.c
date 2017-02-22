#include <err.h>
#include <pthread.h>
#include <signal.h>
#include <stdatomic.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>	/* for getpid */
#include <unistd.h>

static volatile _Atomic int handled = 0;
static const int estbsig = SIGHUP;
static pthread_mutex_t mtx = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t cv = PTHREAD_COND_INITIALIZER;
static sigset_t sigset;
static pthread_t main_thd, sender_thd, waiter_thd;

static void
handler(int signum)
{
	handled++;
}

static void *
waiter(void *arg)
{
	struct sigaction sa;

	memset(&sa, 0, sizeof(sa));
	sigemptyset(&sa.sa_mask);
	sa.sa_handler = handler;

	pthread_sigmask(SIG_UNBLOCK, &sigset, NULL);

	if (sigaction(estbsig, &sa, NULL) == -1)
		err(EXIT_FAILURE, "%s: sigaction", __func__);

	pthread_mutex_lock(&mtx);
	pthread_cond_signal(&cv);
	pthread_mutex_unlock(&mtx);

	printf("%s: waiting\n", __func__);

	for (;;) {
		pause();
		printf("%s: woke, handled %d\n", __func__, handled);
	}

	return NULL;
}

static void *
sender(void *arg)
{
	int stage = 0;
	pid_t mypid = getpid();

	for (stage = 0; ; stage++) {
		sleep(1);
		printf("%s: stage %d\n", __func__, stage % 3);
		switch (stage % 3) {
		case 0:
			pthread_kill(main_thd, estbsig);
			break;
		case 1:
			pthread_kill(waiter_thd, estbsig);
			break;
		case 2:
			kill(mypid, estbsig);
			break;
		}
	}
}

int
main(int argc, char **argv)
{
	int rcvdsig = 0;
	int rc;

	sigemptyset(&sigset);
	sigaddset(&sigset, estbsig);
	pthread_sigmask(SIG_BLOCK, &sigset, NULL);

	main_thd = pthread_self();

	if ((rc = pthread_create(&waiter_thd, NULL, waiter, NULL)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_create: %s", __func__,
		    strerror(rc));
	}

	if ((rc = pthread_create(&sender_thd, NULL, sender, NULL)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_create: %s", __func__,
		    strerror(rc));
	}

	pthread_mutex_lock(&mtx);
	pthread_cond_wait(&cv, &mtx);
	pthread_mutex_unlock(&mtx);

	printf("%s: waiting\n", __func__);

	for (;;) {
#if 1
		if ((rc = sigwait(&sigset, &rcvdsig)) != 0) {
			errx(EXIT_FAILURE, "%s: sigwait: %s", __func__,
			    strerror(rc));
		}
#else
		pause();
#endif

		printf("expected signal %d got %d handled %d signals\n",
		    estbsig, rcvdsig, handled);
	}

	return 0;
}
