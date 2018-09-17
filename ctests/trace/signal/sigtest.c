#include <assert.h>
#include <signal.h>
#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

int	glbl_uu;

void
gnutest_handler(int sig)
{
	static int entry_count;
	struct sigaction sa;

	assert(sig == SIGABRT);
	assert(sigaction (SIGABRT, NULL, &sa) == 0);
	assert((sa.sa_flags & SA_SIGINFO) == 0);
	switch (entry_count++) {
	case 0:
		assert((sa.sa_flags & SA_RESETHAND) == 0);
		assert(sa.sa_handler == gnutest_handler); 
		break;
	case 1:
		/* This assertion fails on glibc-2.3.6 systems with LinuxThreads,
		when this program is linked with -lpthread, due to the sigaction()
		override in libpthread.so.  */
#if !(defined __GLIBC__ || defined __UCLIBC__)
		assert(sa.sa_handler == SIG_DFL);
#endif
		break;
	default:
		assert(false);
	}
}

void
usr_1_handler(int signum)
{
	glbl_uu = 1;
  	assert(signum == SIGUSR1);
}

void
usr_2_handler(int signum)
{
	glbl_uu = 2;
  	assert(signum == SIGUSR2);
}

int
main(void)
{
	/* Sanity check on signal for SIGABRT */ 
	assert(signal(SIGABRT, NULL) == 0);
	assert(signal(SIGABRT, gnutest_handler) == 0);
	assert(signal(SIGABRT, SIG_DFL) == gnutest_handler);

	/* Sanity check on USR1 and USR2 */
  	assert(signal(SIGUSR1, usr_1_handler) == 0); 
	assert(signal(SIGUSR1, SIG_DFL) == usr_1_handler);
  	assert(signal(SIGUSR2, usr_2_handler) == 0); 
	assert(signal(SIGUSR2, SIG_DFL) == usr_2_handler);

  	assert(signal(SIGUSR1, usr_1_handler) == 0); 
  	assert(signal(SIGUSR2, usr_2_handler) == 0); 

	/* Tests for usr handlers */
	glbl_uu = 0;
	raise(SIGUSR1);
	assert(glbl_uu == 1);
	raise(SIGUSR2);
	assert(glbl_uu == 2);
	return 0;
}
