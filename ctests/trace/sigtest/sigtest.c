
/*  * #include <config.h> */

#include <signal.h>


#include <stddef.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <stdint.h>

#include "macros.h"


/* Define a mask of flags required by POSIX.  Some implementations
   provide other flags as extensions, such as SA_RESTORER, that we
   must ignore in this test.  */
#define MASK_SA_FLAGS (SA_NOCLDSTOP | SA_ONSTACK | SA_RESETHAND | SA_RESTART \
                       | SA_SIGINFO | SA_NOCLDWAIT | SA_NODEFER)


/*
 * The gnu signal test handler
 */

__sighandler_t
gnutest_handler(int sig)
{
  static int entry_count;
  struct sigaction sa;
  
  ASSERT (sig == SIGABRT);
  ASSERT (sigaction (SIGABRT, NULL, &sa) == 0);
  ASSERT ((sa.sa_flags & SA_SIGINFO) == 0);
  switch (entry_count++)
    {
    case 0:
      ASSERT ((sa.sa_flags & SA_RESETHAND) == 0);
      ASSERT (sa.sa_handler == (void*)gnutest_handler); 
      break;
    case 1:
      /* This assertion fails on glibc-2.3.6 systems with LinuxThreads,
         when this program is linked with -lpthread, due to the sigaction()
         override in libpthread.so.  */
#if !(defined __GLIBC__ || defined __UCLIBC__)
      ASSERT (sa.sa_handler == SIG_DFL);
#endif
      break;
    default:
      ASSERT (0);
    }

  return NULL; /* FIX IT */

}
/*
 * Routines to write trace messages in what is supposed to be
 * a signal safe manner.
 */
extern int ___rvpredict_set_signal_trace(int x);
void wr_sig_safe(int kd, char* str,int nn,int sn,int v1,int v2);
int trace = 1;
#define MAXBUF 140

#define wr_sig_safe_0(str)	wr_sig_safe(0, str, 0, 0, 0, 0)


	/* Utility trace write routine */
void wr_sig_safe(int kd, char* str,int nn,int sn,int v1,int v2){
	char buf[MAXBUF+2];
	if(!trace)
		return;
	switch(kd){
		case 0: /* This supposed to be a signal safe way to trace things*/
			snprintf(buf,sizeof(buf),"%s", str);
			write(1, buf, strlen(buf));
			return;
		case 1: /* This supposed to be a signal safe way to trace things*/
			snprintf(buf,sizeof(buf), str,nn);
			write(1, buf, strlen(buf));
			return;

		case 2: 
			snprintf(buf,sizeof(buf), str,nn,sn);
			write(1, buf, strlen(buf));
			return;

		default:
			return; /* Maybe should whine */
	}
	return;
}
/*
 * Tests
 *  1: While in handler for SIGUSR1 raise SIGUSR2
 *     The test of recursive signals postponed.
 */
_Atomic uint32_t  current_test = 0;
uint32_t  test_resp = -1;

int get_current_test(){return current_test;}

__sighandler_t
usr_1_handler(int signum)
{
 	if(trace) { 
		wr_sig_safe(2,"  In usr_%d_handler:signum=%d\n",1,signum,0,0);
	}
  	ASSERT(signum == SIGUSR1);
	switch(get_current_test()){
	case 1:
		/* This signal would trigger a user2 signal.
		 * We are not testing recursive signals at the moment
		 *              raise(2);
		 */
		break;
	default:
		break;
 	}
  return NULL; /* FIX IT */
}

__sighandler_t
usr_2_handler(int signum)
{
  	if(trace) {
		wr_sig_safe(2,"  In usr_%d_handler:signum=%d\n",2,signum,0,0);
		}
  	ASSERT(signum == SIGUSR2);
	switch(get_current_test()){
	case 1:
//		test_resp = 1;
		break;
	default:
		break;
 	}

  return NULL; /* FIX IT */
}

int
main (void)
{
	/* Control tracing  */
	trace = 0; /* =0 Tracing off, =1 traceing on */
	___rvpredict_set_signal_trace(trace);

	/* Sanity check on signal for SIGABRT */ 
	ASSERT (signal(SIGABRT, NULL) == 0);
	ASSERT (signal (SIGABRT, (void*)gnutest_handler) == 0);
	ASSERT (signal (SIGABRT, SIG_DFL) == (void*)gnutest_handler);

	/* Sanity check on USR1 and USR2 */
  	ASSERT (signal (SIGUSR1, (void*)usr_1_handler) == 0); 
	ASSERT (signal (SIGUSR1, SIG_DFL) == (void*)usr_1_handler);
  	ASSERT (signal (SIGUSR2, (void*)usr_2_handler) == 0); 
	ASSERT (signal (SIGUSR2, SIG_DFL) == (void*)usr_2_handler);

  	ASSERT (signal (SIGUSR1, (void*)usr_1_handler) == 0); 
  	ASSERT (signal (SIGUSR2, (void*)usr_2_handler) == 0); 

	wr_sig_safe(1,"--Doing test %d \n",1,0,0,0);
	/* Do test 1 */
	current_test = 1;
	test_resp = -1;
	raise(SIGUSR1);
	ASSERT(test_resp == -1);
	#if 0
		/* Now ignore SIGUSR2 */
  		ASSERT (signal (SIGUSR2, SIG_IGN) == 0); 
		test_resp = 8;
		wr_sig_safe_0("  Raising SIGUSR1 after SIGUSR2 set to ignore \n");
		raise(SIGUSR1);
		ASSERT(test_resp == 8);
		wr_sig_safe_0("  fini \n");	
	#endif
	return 0;
}
