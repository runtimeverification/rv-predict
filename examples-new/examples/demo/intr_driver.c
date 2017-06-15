#include <err.h>
#include <errno.h>
#include <pthread.h>
#include <sched.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

struct _isr_info;
typedef struct _isr_info isr_info_t;

typedef struct _isr_thrinfo {
	pthread_t thread;
	sigset_t wakeup_sigset;
	int wakeup_signum;
} isr_thrinfo_t;

struct _isr_info {
	isr_info_t *next;
	const char *name;
	void (*isr)(void);
	pthread_mutex_t mtx;
	isr_thrinfo_t target,	// target of interrupts
		      source;	// source of interrupts (also runs ISR)
	volatile int ninterrupt;
	volatile int nserviced;
};

static isr_info_t *isr_info_chain = NULL;

static int nisr1, nisr2;

void rvpc_enable_isr(isr_info_t *);
void rvpc_disable_isr(isr_info_t *);
void rvpc_establish_isr(const char *, void (*)(void), isr_info_t *);

static void *
driver(void *arg)
{
	isr_info_t *info = arg;
	const struct timespec one_hundredth_sec = {.tv_sec = 0,
						   .tv_nsec = 10 * 1000 * 1000};

	pthread_sigmask(SIG_BLOCK, &info->source.wakeup_sigset, NULL);

	for (;;) {
		int sig = sigtimedwait(&info->source.wakeup_sigset, NULL,
		    &one_hundredth_sec);

		pthread_mutex_lock(&info->mtx);
		if (sig == -1 && errno == EAGAIN) {
			pthread_kill(info->target.thread,
			    info->target.wakeup_signum);
		} else if (sig == info->source.wakeup_signum) {
			if (info->nserviced < info->ninterrupt)
				(*info->isr)();
			info->nserviced = info->ninterrupt;
		}
		pthread_mutex_unlock(&info->mtx);
	}
	return NULL;
}

static void
interrupt(int signum)
{
	isr_info_t *info;

	for (info = isr_info_chain; info != NULL; info = info->next) {
		if (info->target.thread != pthread_self())
			continue;
		++info->ninterrupt;
		pthread_kill(info->source.thread, info->source.wakeup_signum);
	}

	for (info = isr_info_chain; info != NULL; info = info->next) {
		if (info->target.thread != pthread_self())
			continue;
		while (info->nserviced < info->ninterrupt)
			;	/* do nothing */
	}
}

static void
setup_signal(isr_thrinfo_t *thri, int signum)
{
	sigemptyset(&thri->wakeup_sigset);
	thri->wakeup_signum = signum;
	sigaddset(&thri->wakeup_sigset, thri->wakeup_signum);
}

void
rvpc_enable_isr(isr_info_t *info)
{
	pthread_mutex_unlock(&info->mtx);
}

void
rvpc_disable_isr(isr_info_t *info)
{
	pthread_mutex_lock(&info->mtx);
	info->nserviced = info->ninterrupt;
}

void
rvpc_establish_isr(const char *name, void (*isr)(void), isr_info_t *info)
{
	struct sigaction sa;

	setup_signal(&info->target, SIGUSR1);
	setup_signal(&info->source, SIGUSR2);

	info->name = name;
	info->isr = isr;
	info->nserviced = info->ninterrupt = 0;

	if (pthread_mutex_init(&info->mtx, NULL) != 0)
		err(EXIT_FAILURE, "%s: pthread_mutex_init", __func__);

	info->target.thread = pthread_self();

	if (pthread_create(&info->source.thread, NULL, driver, info) != 0)
		err(EXIT_FAILURE, "%s: pthread_create", __func__);

	pthread_sigmask(SIG_BLOCK, &info->target.wakeup_sigset, NULL);
	pthread_sigmask(SIG_BLOCK, &info->source.wakeup_sigset, NULL);
	memset(&sa, 0, sizeof(sa));
	sa.sa_handler = interrupt;
	if (sigemptyset(&sa.sa_mask) != 0)
		err(EXIT_FAILURE, "%s: sigemptyset", __func__);

	if (sigaction(info->target.wakeup_signum, &sa, NULL) != 0)
		err(EXIT_FAILURE, "%s: sigaction", __func__);
	pthread_sigmask(SIG_UNBLOCK, &info->target.wakeup_sigset, NULL);
	info->next = isr_info_chain;
	isr_info_chain = info;
}

static void
isr1(void)
{
	char msg[] = "enter isr 1\n";

	nisr1++;

	(void)write(STDOUT_FILENO, msg, sizeof(msg) - 1);
}

static void
isr2(void)
{
	char msg[] = "enter isr 2\n";

	nisr2++;

	(void)write(STDOUT_FILENO, msg, sizeof(msg) - 1);
}

int
main(int argc, char **argv)
{
	isr_info_t info1, info2;

	rvpc_establish_isr("test1", isr1, &info1);
	rvpc_establish_isr("test2", isr2, &info2);
	for (;;)
		sched_yield();
	return EXIT_SUCCESS;
}
