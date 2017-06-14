/*
 *	SCCS: @(#)thr_create.c	1.20 (99/11/15)
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1996 X/Open Company Limited
 *
 * All rights reserved.  No part of this source code may be reproduced,
 * stored in a retrieval system, or transmitted, in any form or by any
 * means, electronic, mechanical, photocopying, recording or otherwise,
 * except as stated in the end-user licence agreement, without the prior
 * permission of the copyright owners.
 * A copy of the end-user licence agreement is contained in the file
 * Licence which accompanies this distribution.
 * 
 * X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
 * the UK and other countries.
 */

#ifndef lint
static char sccsid[] = "@(#)thr_create.c	1.20 (99/11/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)thr_create.c	1.20 99/11/15 TETware release 3.8
NAME:		'C' API start new thread function
PRODUCT:	TETware
AUTHOR:		Geoff Clare, UniSoft Ltd.
DATE CREATED:	July 1996
SYNOPSIS:

    UI Threads:
	int	tet_thr_create(void *stack_base, size_t stack_size,
			void *(*start_routine)(void *), void *arg,
			long flags, thread_t *new_thread, int waittime);

	int	tet_thr_join(thread_t thread, void **value_ptr);

    POSIX Threads:
	int	tet_pthread_create(pthread_t *new_thread,
			pthread_attr_t *attr,
			void *(*start_routine)(void *), void *arg,
			int waittime);

	int	tet_pthread_join(pthread_t thread, void **value_ptr);

	int	tet_pthread_detach(pthread_t thread);

    NT Threads:
	unsigned long tet_beginthreadex(void *security,
			unsigned int stack_size,
			unsigned int (__stdcall *start_routine)(void *),
			void *arg, unsigned int flags,
			unsigned int *new_thread, int waittime);

    All Threads:
	void	tet_cln_threads(int signum);
	void	tet_thrtab_reset(void);

DESCRIPTION:

	Tet_thr_create(), tet_pthread_create() and tet_beginthreadex() are
	wrappers for thr_create(), pthread_create() and _beginthreadex()
	respectively.  They store information about the new thread for use
	by the TCM.  The waittime argument specifies the time (in seconds)
	to wait for the thread to die if it is still active when the main
	thread returns to the TCM.

	Calls to tet_setblock() are made to distinguish journal output
	from the new thread and from the calling thread before and
	after creation of the new thread.

	Tet_thr_join(), tet_pthread_join() and tet_pthread_detach() are
	wrappers for thr_join(), pthread_join() and pthread_detach()
	respectively.  They should be used instead of the equivalent
	direct calls when the target thread was created by
	tet_thr_create() or tet_pthread_create().  Note that the
	calling convention for tet_thr_join() differs from thr_join().
	It must be called with a specific target thread ID, and does
	not pass back the joined thread ID.

	Tet_cln_threads() and tet_thrtab_reset() are not part of the
	API.  They are used internally to clean up left-over threads
	and to empty the thread table respectively.

MODIFICATIONS:

	Geoff Clare, UniSoft Ltd., September 1996
	Added tet_pthread_create().

	Geoff Clare, UniSoft Ltd., Oct 1996
	Use TET_THR_EQUAL() to compare thread IDs.

	Geoff Clare, UniSoft Ltd., June-July 1997
	Changes to support NT threads.

	Geoff Clare, UniSoft Ltd., December 1997
	Support for threads implementations which may re-use a
	thread ID immediately a previously allocated thread ID becomes
	available.

	Andrew Dingwall, UniSoft Ltd., February 1998
	Use TETware-specific macros to access threads functions and
	data items.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
	Andrew Dingwall, UniSoft Ltd., July 1999
	added support for 'other language' APIs

	Andrew Dingwall, UniSoft Ltd., September 1999
	when updating an existing thrtab entry in ttadd(), remember the next and
	last pointers before overwriting them

	Geoff Clare, UniSoft Ltd., October 1999
	Added tet_thr_join(), tet_pthread_join(), and tet_pthread_detach().
	When cleaning up threads, run through the thread table backwards
	(to avoid simultaneous calls to pthread_join()).
	Allow for either ETIMEDOUT or ETIME to be returned when
	TET_COND_TIMEDWAIT() times out.

	Andrew Dingwall, UniSoft Ltd., October 1999
	added support for strict POSIX threads

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
#  include <unistd.h>
#  include <signal.h>
#endif		/* -WIN32-CUT-LINE- */
#include <errno.h>
#include <time.h>
#include "dtmac.h"
#include "dtthr.h"
#include "sigsafe.h"
#include "alarm.h"
#include "error.h"
#include "llist.h"
#include "tet_api.h"
#include "apilib.h"

#ifndef NOTRACE
#  include "ltoa.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;
#endif

#define KILLWAIT	5	/* seconds to wait for child to exit */
#define THRKILLWAIT	12	/* secs to wait for thread after forcing
				   it to exit (should be > 2*KILLWAIT) */

extern tet_mutex_t tet_thrtab_mtx;
extern tet_mutex_t tet_thrwait_mtx;

/* Thread table */
struct thrtab {
        struct thrtab *next;     /* ptr to next element - must be 1st */
	struct thrtab *last;     /* ptr to previous element - must be 2nd */
#ifdef _WIN32	/* -START-WIN32-CUT- */
	unsigned long handle;
#endif		/* -END-WIN32-CUT- */
	tet_thread_t tid;
	int waittime;
};
static struct thrtab *thrtab;

struct wrap_arg {
#ifdef _WIN32	/* -START-WIN32-CUT- */
	unsigned int (__stdcall *start_routine)(void *);
#else		/* -END-WIN32-CUT- */
	void *(*start_routine)();
#endif		/* -WIN32-CUT-LINE- */
	void *arg;
};

struct clnarg {
	tet_thread_t tid;
	int waittime;
};

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
static tet_thread_t	target_tid;
static struct sigaction	oldsigact;
static tet_cond_t	thrwait_cv;
static int		joined;
#endif		/* -WIN32-CUT-LINE- */

static void *	cln_thr2();
static int	ttadd();

static
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
  void *
#else		/* -START-WIN32-CUT- */
  unsigned int __stdcall
#endif		/* -END-WIN32-CUT- */
start_wrapper(vwrap_arg)
void *vwrap_arg;
{
	/* wrapper for user-specified thread start routine */

	struct wrap_arg *wrap_arg = vwrap_arg;
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	void *(*start_routine)();
#else		/* -START-WIN32-CUT- */
	unsigned int (__stdcall *start_routine)(void *);
#endif		/* -END-WIN32-CUT- */
	void *arg;
	int newerrno = 0;
	long newblock = 0;
	long newsequence = 0;
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	int newchild = 0;
	int newalrm_flag = 0;
#endif		/* -WIN32-CUT-LINE- */

	tet_api_status |= TET_API_MULTITHREAD;

	/* set thread-specific data for new thread */
	TET_THR_SETSPECIFIC(tet_errno_key, (void *)&newerrno);
	TET_THR_SETSPECIFIC(tet_block_key, (void *)&newblock);
	TET_THR_SETSPECIFIC(tet_sequence_key, (void *)&newsequence);
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	TET_THR_SETSPECIFIC(tet_child_key, (void *)&newchild);
	TET_THR_SETSPECIFIC(tet_alarm_flag_key, (void *)&newalrm_flag);
#endif		/* -WIN32-CUT-LINE- */

	tet_setblock();

	start_routine = wrap_arg->start_routine;
	arg = wrap_arg->arg;
	TRACE2(tet_Tbuf, 6, "free wrap_arg = %s", tet_i2x(wrap_arg));
	free((void *)wrap_arg);

	return (*start_routine)(arg);
}

#ifdef _WIN32	/* -START-WIN32-CUT- */
TET_IMPORT unsigned long
tet_beginthreadex(void *security, unsigned int stack_size,
	unsigned int (__stdcall *start_routine)(void *),
	void *arg, unsigned int flags, unsigned int *new_thread,
	int waittime)
#else /* !_WIN32 */	/* -END-WIN32-CUT- */
#  ifndef TET_POSIX_THREADS
int
tet_thr_create(stack_base, stack_size, start_routine, arg, flags,
		new_thread, waittime)
void *stack_base;
size_t stack_size;
void *(*start_routine)();
void *arg;
long flags;
thread_t *new_thread;
int waittime;
#  else /* TET_POSIX_THREADS */
int
tet_pthread_create(new_thread, attr, start_routine, arg, waittime)
pthread_t *new_thread;
pthread_attr_t *attr;
void *(*start_routine)();
void *arg;
int waittime;
#  endif /* TET_POSIX_THREADS */
#endif /* !_WIN32 */	/* -WIN32-CUT-LINE- */
{
	tet_thread_t tid;
	struct wrap_arg *wrap_arg;
	struct thrtab *ttp;
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	int rtval;
#else		/* -START-WIN32-CUT- */
	unsigned long rtval;
	int sav_errno;
#endif		/* -END-WIN32-CUT- */
	int detached;

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	/* no API_LOCK here */

	if (!start_routine)
		return EINVAL;

	/* put start routine and its argument in an argument to wrapper */

	wrap_arg = (struct wrap_arg *) malloc(sizeof(*wrap_arg));
	if (wrap_arg == NULL)
		return ENOMEM;
	TRACE2(tet_Tbuf, 6, "allocate wrap_arg = %s", tet_i2x(wrap_arg));
	wrap_arg->start_routine = start_routine;
	wrap_arg->arg = arg;

	/* allocate a new entry in thread table, if not detached */

#ifdef _WIN32	/* -START-WIN32-CUT- */
	detached = 0;
#else		/* -END-WIN32-CUT- */
#  ifndef TET_POSIX_THREADS
	detached = (flags & THR_DETACHED);
#  else
	{
		int dstate;
		if (attr != NULL &&
		    pthread_attr_getdetachstate(attr, &dstate) == 0)
			detached = (dstate == PTHREAD_CREATE_DETACHED);
		else
			detached = 0;
	}
#  endif /* TET_POSIX_THREADS */
#endif /* !_WIN32 */	/* -WIN32-CUT-LINE- */

	if (!detached)
	{
		ttp = (struct thrtab *) malloc(sizeof(*ttp));
		if (ttp == NULL)
		{
			TRACE2(tet_Tbuf, 6, "free wrap_arg = %s",
				tet_i2x(wrap_arg));
			free((void *)wrap_arg);
			return ENOMEM;
		}
		TRACE2(tet_Tbuf, 6, "allocate thrtab entry = %s", tet_i2x(ttp));
	}

/* ????
 * should block tet_blockable_sigs here? (and unblock after
 * thrtab update) - means passing old sigset to wrapper
 */

#ifdef _WIN32	/* -START-WIN32-CUT- */
	rtval = _beginthreadex(security, stack_size, start_wrapper,
		(void *)wrap_arg, flags, &tid);
	sav_errno = errno;
	if (rtval != 0)
		tet_api_status |= TET_API_MULTITHREAD;
#else		/* -END-WIN32-CUT- */
#  ifndef TET_POSIX_THREADS
	rtval = thr_create(stack_base, stack_size, start_wrapper,
		(void *)wrap_arg, flags, &tid);
#  else
	rtval = pthread_create(&tid, attr, start_wrapper, (void *)wrap_arg);
#  endif /* TET_POSIX_THREADS */
	if (rtval == 0)
		tet_api_status |= TET_API_MULTITHREAD;
#endif /* !_WIN32 */	/* -WIN32-CUT-LINE- */

	if (!detached)
	{
		int added = 0;

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
		if (rtval == 0)
		{
			/* store new thread ID and waittime in table */
			ttp->tid = tid;
			ttp->waittime = waittime;
			added = ttadd(ttp);
		}
#else /* _WIN32 */	/* -START-WIN32-CUT- */
		if (rtval != 0)
		{
			/* store new thread handle, ID and waittime in table */

			/* duplicate the handle first, as the original
			   one will normally be closed before thread
			   clean-up is done */
			
			if (DuplicateHandle(GetCurrentProcess(), (HANDLE)rtval,
				GetCurrentProcess(), (HANDLE)&ttp->handle, 0,
				FALSE, DUPLICATE_SAME_ACCESS))
			{
				ttp->tid = tid;
				ttp->waittime = waittime;
				added = ttadd(ttp);
			}
		}
#endif /* _WIN32 */	/* -END-WIN32-CUT- */

		if (!added)
		{
			TRACE2(tet_Tbuf, 6, "free thrtab entry = %s",
				tet_i2x(ttp));
			free((void *)ttp);
		}
	}

	if (new_thread)
		*new_thread = tid;

	tet_setblock();

	/* wrap_arg is freed in start_wrapper after it has finished with it */

#ifdef _WIN32	/* -START-WIN32-CUT- */
	errno = sav_errno;
#endif		/* -END-WIN32-CUT- */
	return rtval;
}

#ifndef _WIN32
#  ifndef TET_POSIX_THREADS
int
tet_thr_join(thread, value_ptr)
thread_t thread;
void **value_ptr;
#  else /* TET_POSIX_THREADS */
int
tet_pthread_join(thread, value_ptr)
pthread_t thread;
void **value_ptr;
#  endif /* TET_POSIX_THREADS */
{
	struct thrtab *ttp = 0;
	int err;

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	/* no API_LOCK here */

	MTX_LOCK(&tet_thrtab_mtx);
	/* First move the thread ID to the head of the list, so that
	   the cleanup code will join it *after* the current thread.  
	   This avoids simultaneous calls to pthread_join(). */
	for (ttp = thrtab; ttp; ttp = ttp->next)
		if (TET_THR_EQUAL(ttp->tid, thread))
			break;
	if (ttp)
	{
		tet_listremove((struct llist **) &thrtab, (struct llist *) ttp);
		tet_listinsert((struct llist **) &thrtab, (struct llist *) ttp);
	}
	MTX_UNLOCK(&tet_thrtab_mtx);

	/* Do the join even if the thread wasn't found in the table */

	err = TET_THR_JOIN(thread, value_ptr);

	if (err == 0 || err == ESRCH || err == EINVAL)
	{
		/* Either the thread has been joined, or it doesn't
		   exist or it isn't joinable.  For all of these cases,
		   we don't want it in the thread table any more. */

		if (ttp)
		{
			MTX_LOCK(&tet_thrtab_mtx);

			/* Find the table entry again (it could have moved) */
			for (ttp = thrtab; ttp; ttp = ttp->next)
				if (TET_THR_EQUAL(ttp->tid, thread))
					break;
			if (ttp)
				tet_listremove((struct llist **) &thrtab,
						(struct llist *) ttp);

			MTX_UNLOCK(&tet_thrtab_mtx);

			if (ttp)
			{
				TRACE2(tet_Tbuf, 6, "free thrtab entry = %s",
					tet_i2x(ttp));
				free((void *)ttp);
			}
		}
	}

	return err;
}

#  ifdef TET_POSIX_THREADS
int
tet_pthread_detach(thread)
pthread_t thread;
{
	struct thrtab *ttp = 0;
	int err;

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	/* no API_LOCK here */

	MTX_LOCK(&tet_thrtab_mtx);

	err = pthread_detach(thread);
	if (err == 0 || err == ESRCH || err == EINVAL)
	{
		/* Either the thread has been detached, or it doesn't
		   exist or it isn't joinable.  For all of these cases,
		   we don't want it in the thread table any more. */

		for (ttp = thrtab; ttp; ttp = ttp->next)
			if (TET_THR_EQUAL(ttp->tid, thread))
				break;
		if (ttp)
			tet_listremove((struct llist **) &thrtab,
					(struct llist *) ttp);

	}

	MTX_UNLOCK(&tet_thrtab_mtx);

	if (ttp)
	{
		TRACE2(tet_Tbuf, 6, "free thrtab entry = %s",
			tet_i2x(ttp));
		free((void *)ttp);
	}

	return err;
}
#  endif /* TET_POSIX_THREADS */
#endif /* !_WIN32 */

#ifdef _WIN32	/* -START-WIN32-CUT- */

TET_IMPORT void tet_cln_threads(signum)
int signum;
{
	/* wait for any left-over threads */

	time_t start_time;
	DWORD rc, waittime = 0;
	unsigned long err;
	int clean_ok = 1;
	struct thrtab *ttp, *ttpnext;

	if (signum == 0)
		start_time = time((time_t *)0);

	MTX_LOCK(&tet_thrtab_mtx);

	for (ttp = thrtab; ttp; ttp = ttpnext)
	{
		ttpnext = ttp->next; /* save value before ttp is removed */

		/* If we are cleaning up on receiving a signal, leave
		   wait time as zero, otherwise use wait time specified
		   in the original tet_beginthreadex() call (minus the
		   time since the TP ended). */
		if (signum == 0)
		{
			if (ttp->waittime >= 0)
				waittime = (DWORD) 1000 * (ttp->waittime - 
				   	   (time((time_t *)0) - start_time));
			else
				waittime = INFINITE;
		}

		/* wait for thread to exit */
		/* if it doesn't exit within the wait time, there is
		   no safe way to force it to terminate, so terminate
		   the whole process (after waiting for other threads) */

		switch (rc = WaitForSingleObject((HANDLE)ttp->handle, waittime))
		{
		case WAIT_OBJECT_0:
			break;

		case WAIT_FAILED:
			err = (unsigned long) GetLastError();
			error(tet_w32err2errno(err),
				"WaitForSingleObject() with thread handle failed, error =",
				tet_i2a(err));
			clean_ok = 0;
			break;

		case WAIT_TIMEOUT:
			error(0, "Thread wait timed out for thread ID",
				tet_i2a(ttp->tid));
			clean_ok = 0;
			break;

		default:
			error(0, "WaitForSingleObject() with thread handle returned unexpected value:",
				tet_i2a(rc));
			clean_ok = 0;
			break;
		}

		(void) CloseHandle((HANDLE)ttp->handle);

		tet_listremove((struct llist **) &thrtab, (struct llist *) ttp);
		TRACE2(tet_Tbuf, 6, "free thrtab entry = %s", tet_i2x(ttp));
		free((void *)ttp);
	}

	if (!clean_ok)
		fatal(0, "Wait failed for one or more threads", (char *)0);

	thrtab = NULL;

	MTX_UNLOCK(&tet_thrtab_mtx);
}

#else /* !_WIN32 */	/* -END-WIN32-CUT- */

static void
do_oldabort(sig)
int sig;
{
	/* wrong thread received SIGABRT signal - try to do what it
	   would have done */

	(void) sigaction(SIGABRT, &oldsigact, (struct sigaction *)NULL);
	if (oldsigact.sa_handler == SIG_DFL)
	{
		abort();
		fatal(0, "abort() returned!!!", (char *)0);
	}
	else if (oldsigact.sa_handler != SIG_IGN)
	{
		/* hope it wasn't expecting more arguments */
		(*oldsigact.sa_handler)(sig);
	}
	/* else SIG_IGN: all we can do is return (something might get EINTR) */
}

static void
make_thr_exit(sig)
int sig;
{
	/* signal handler used to force the target thread to exit when
	   it is sent a SIGABRT with TET_THR_KILL() */

	if (!TET_THR_EQUAL(TET_THR_SELF(), target_tid))
	{
		do_oldabort(sig);
		return;
	}

	if (tet_child > 0)
		(void) tet_killw(tet_child, KILLWAIT);

	TET_THR_EXIT((void *)0);
}

TET_IMPORT void tet_cln_threads(signum)
int signum;
{
	/* clean up any left-over threads */

	tet_thread_t tid2;
	time_t start_time;
	int err, waittime = 0;
	struct thrtab *ttp, *ttpnext;
	struct clnarg arg;

	if (signum == 0)
		start_time = time((time_t *)0);

	MTX_LOCK(&tet_thrtab_mtx);

	(void) TET_COND_INIT(&thrwait_cv);

	/* Start at the end of the table and work backwards.  This is
	   so that if a call to tet_pthread_join() is in progress, the
	   target thread of that join will be cleaned up after the
	   thread that was doing the join, thus avoiding simultaneous
	   calls to pthread_join().  NB this relies on tet_pthread_join()
	   moving the target thread ID to the head of the table before
	   calling pthread_join(). */
	for (ttp = thrtab; ttp; ttp = ttp->next)
		 if (ttp->next == 0)
			break;

	for ( ; ttp; ttp = ttpnext)
	{
		ttpnext = ttp->last; /* save value before ttp is removed */

		/* If we are cleaning up on receiving a signal, leave
		   wait time as zero, otherwise use wait time specified
		   in the original tet_thr_create() call (minus the time
		   since the TP ended). */
		if (signum == 0)
			waittime = ttp->waittime - 
				   (time((time_t *)0) - start_time);

		joined = 0;

		/* call cln_thr2() in a new thread */
		arg.tid = ttp->tid;
		arg.waittime = waittime;
		if ((err = TET_THR_CREATE(cln_thr2, (void *) &arg, &tid2)) != 0)
			fatal(err, "thr_create() failed in tet_cln_threads()",
				(char *)0);

		/*
		 * If the target thread exits within waittime, cln_thr2
		 * will see this via TET_COND_TIMEDWAIT() and will return.
		 * If not, it will try to force the target thread to exit
		 * from a SIGABRT signal handler, and will do another
		 * TET_COND_TIMEDWAIT().  If the target thread is blocking
		 * SIGABRT the TET_THR_JOIN() below will hang and cln_thr2
		 * will time out on a second TET_COND_TIMEDWAIT().  If this
		 * happens we give up - we could arrange to longjmp out of
		 * the TET_THR_JOIN(), but this would leave a rogue thread
		 * which could interfere with later TP's.
		 */

		if (!TET_THR_EQUAL(ttp->tid, tid2))
			(void) TET_THR_JOIN(ttp->tid, (void **) NULL);
		TET_MUTEX_LOCK(&tet_thrwait_mtx);
		joined = 1;
		(void) TET_COND_SIGNAL(&thrwait_cv);
		TET_MUTEX_UNLOCK(&tet_thrwait_mtx);
		(void) TET_THR_JOIN(tid2, (void **) NULL);
		tet_listremove((struct llist **) &thrtab, (struct llist *) ttp);
		TRACE2(tet_Tbuf, 6, "free thrtab entry = %s", tet_i2x(ttp));
		free((void *)ttp);
	}
	thrtab = NULL;

	(void) TET_COND_DESTROY(&thrwait_cv);

	MTX_UNLOCK(&tet_thrtab_mtx);
}

static void *
cln_thr2(arg)
void *arg;
{
	/* force the specified thread to exit, after timeout of waittime
	   (this function is executed in a new thread) */

	struct clnarg *carg = (struct clnarg *)arg;
	struct sigaction sa;
	tet_timestruc_t abstime;
	int err;

	if (carg->waittime > 0)
	{
		TET_MUTEX_LOCK(&tet_thrwait_mtx);
		abstime.tv_sec = time((time_t *)0) + carg->waittime;
		abstime.tv_nsec = 0;
		while (!joined)
		{
			err = TET_COND_TIMEDWAIT(&thrwait_cv,
				&tet_thrwait_mtx, &abstime);
			if (err != EINTR)
				break;
		}
		if (joined)
			err = 0;
		TET_MUTEX_UNLOCK(&tet_thrwait_mtx);
		if (err == 0)
			return (void *)0;
	}

	/* Install a SIGABRT handler which calls TET_THR_EXIT() and send the
	   target thread a SIGABRT.  Don't restore the old handler until
	   we know our handler has been executed */
	target_tid = carg->tid;
	sa.sa_handler = make_thr_exit;
	sa.sa_flags = 0; 
	(void) sigemptyset(&sa.sa_mask); 
	(void) sigaction(SIGABRT, &sa, &oldsigact);
	err = TET_THR_KILL(carg->tid, SIGABRT);
	switch (err)
	{
	case 0:
		break;
	case ESRCH:
		/* thread has gone away already */
		(void) sigaction(SIGABRT, &oldsigact, (struct sigaction *)0);
		return (void *)0;
	default:
		fatal(err, "TET_THR_KILL() failed in cln_thr2()", (char *)0);
	}

	TET_MUTEX_LOCK(&tet_thrwait_mtx);
	abstime.tv_sec = time((time_t *)0) + THRKILLWAIT;
	abstime.tv_nsec = 0;
	while (!joined)
	{
		err = TET_COND_TIMEDWAIT(&thrwait_cv, &tet_thrwait_mtx,
			&abstime);
		if (err != EINTR)
			break;
	}
	if (joined)
		err = 0;
	TET_MUTEX_UNLOCK(&tet_thrwait_mtx);
	if (err == 0)
		(void) sigaction(SIGABRT, &oldsigact, (struct sigaction *)0);
	else if (1
#ifdef ETIME
		 && err != ETIME
#endif
#ifdef ETIMEDOUT
		 && err != ETIMEDOUT
#endif
		)
		fatal(err, "TET_COND_TIMEDWAIT() failed in cln_thr2()",
			(char *) 0);
	else
		fatal(0, "cln_thr2() caller thread did not return from TET_THR_JOIN()",
			(char *)0);

	return (void *)0;
}

#endif /* !_WIN32 */	/* -WIN32-CUT-LINE- */

void
tet_thrtab_reset()
{
	/* empty the thread table */

	struct thrtab *ttp, *ttpnext;

	/* no MTX_LOCK here - function only called when just one thread
	   exists, e.g. in child after fork1(), and/or when thrtab mutex
	   is already locked */

	for (ttp = thrtab; ttp; ttp = ttpnext)
	{
		ttpnext = ttp->next;
		tet_listremove((struct llist **) &thrtab, (struct llist *) ttp);
		TRACE2(tet_Tbuf, 6, "free thrtab entry = %s", tet_i2x(ttp));
		free((void *)ttp);
	}
	thrtab = NULL;
}

static int
ttadd(newttp)
struct thrtab *newttp;
{
	/* add or update a thread in the thread table */
	/* return 1 if entry added, 0 if existing entry updated */

	register struct thrtab *ttp;
	int added = 0;

	MTX_LOCK(&tet_thrtab_mtx);

	/* check if the tid/handle is already in the list
	   (thread ID's can be reused) */

	for (ttp = thrtab; ttp; ttp = ttp->next)
		if (
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
		    TET_THR_EQUAL(ttp->tid, newttp->tid)
#else		/* -START-WIN32-CUT- */
		    ttp->handle == newttp->handle
#endif		/* -END-WIN32-CUT- */
		)
			break;

	if (ttp)
	{
		/* update the existing entry */
		newttp->next = ttp->next;
		newttp->last = ttp->last;
		*ttp = *newttp;
	}
	else
	{
		/* insert new entry */
		tet_listinsert((struct llist **) &thrtab,
			(struct llist *) newttp);
		added = 1;
	}

	MTX_UNLOCK(&tet_thrtab_mtx);

	return added;
}

