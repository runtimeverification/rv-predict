/*
 *	SCCS: @(#)tet_fork.c	1.27 (99/11/15)
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

/*
 * Copyright 1990 Open Software Foundation (OSF)
 * Copyright 1990 Unix International (UI)
 * Copyright 1990 X/Open Company Limited (X/Open)
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose and without fee is hereby granted, provided
 * that the above copyright notice appear in all copies and that both that
 * copyright notice and this permission notice appear in supporting
 * documentation, and that the name of OSF, UI or X/Open not be used in 
 * advertising or publicity pertaining to distribution of the software 
 * without specific, written prior permission.  OSF, UI and X/Open make 
 * no representations about the suitability of this software for any purpose.  
 * It is provided "as is" without express or implied warranty.
 *
 * OSF, UI and X/Open DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, 
 * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO 
 * EVENT SHALL OSF, UI or X/Open BE LIABLE FOR ANY SPECIAL, INDIRECT OR 
 * CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF 
 * USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR 
 * OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR 
 * PERFORMANCE OF THIS SOFTWARE.
 */

#ifndef lint
static char sccsid[] = "@(#)tet_fork.c	1.27 (99/11/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tet_fork.c	1.27 99/11/15 TETware release 3.8
NAME:		'C' API fork process function
PRODUCT:	TETware
AUTHOR:		Geoff Clare, UniSoft Ltd.
DATE CREATED:	26 July 1990
SYNOPSIS:

	int	tet_fork(void (*childproc)(), void (*parentproc)(),
		     int waittime, int exitvals);

	int	tet_fork1(void (*childproc)(), void (*parentproc)(),
		     int waittime, int exitvals);

DESCRIPTION:

	Tet_fork() forks a child process and calls (*childproc)() in the
	child and (*parentproc)() (if != TET_NULLFP) in the parent.  The
	child calls tet_setcontext() to distinguish it's results file
	output from the parent's.  Calls to tet_setblock() are made in
	the parent to separate output made before, during and after
	execution of the child process.

	Waittime controls whether, and for how long, tet_fork() waits for
	the child after (*parentproc)() has returned.  If waittime < 0 no
	wait is done and the child is killed if it is still alive.  If
	waittime is zero tet_fork() waits indefinitely, otherwise the
	wait is timed out after waittime seconds.  If the child is going
	to be waited for, signals which are being caught in the parent
	are set to SIG_DFL in the child so unexpected signals will come
	through to the wait status.

	Exitvals is a bit mask of valid child exit codes.  Tet_fork()
	returns the child exit code (if valid), otherwise -1.  If
	(*childproc)() returns rather than exiting, or no wait was done
	then tet_fork() returns 0.  If tet_fork() returns -1 it first
	writes an information line and an UNRESOLVED result code to the
	execution results file.

	Tet_fork1() is only provided in the threads version of the API.
	It is equivalent to tet_fork() except that it calls fork1()
	instead of fork().

	These functions are not implemented on WIN32 platforms.


MODIFICATIONS:

	June 1992
	This file is derived from TET release 1.10

	Andrew Dingwall, UniSoft Ltd., October 1992
	Update tet_mypid after a fork().

	Denis McConalogue, UniSoft Limited, August 1993
                changed dtet to tet2 in #include

	Denis McConalogue, UniSoft Limited, September 1993
	make sure all connections inherited by child processes are closed.
	logon again to SYNCD and XRESD in child process.
	make sure that if the child process returns, any new connections
	are logged off

	Geoff Clare, UniSoft Ltd., July-August 1996
	Changes for TETWare.

	Geoff Clare, UniSoft Ltd., Sept 1996
	Added alarm.h.

	Andrew Dingwall, UniSoft Ltd., September 1996
	Removed tet_errname() from here in favour of the more comprehensive
	version in dtet2lib.

	Geoff Clare, UniSoft Ltd., Sept 1996
	Changes for TETWare-Lite.

	Geoff Clare, UniSoft Ltd., Oct 1996
	Added dtetlib.h (for tet_errname decl).

	Andrew Dingwall, UniSoft Ltd., June 1997
	added call to tet_xdxrsend() after tet_xdlogon() -
	needed to make parallel remote and distributed test cases work
	correctly

	Andrew Dingwall, UniSoft Ltd., February 1998
	Use TETware-specific macros to access threads functions and
	data items.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Changed the way that tet_fork() operates when TET_POSIX_THREADS
	is defined.
	This is because in UI threads:
		fork() = forkall() and there is also a fork1() system call.
	whereas in POSIX threads:
		fork() = fork1() and there is no forkall() system call.
	Added support for shared API libraries.
 
	Andrew Dingwall, UniSoft Ltd., July 1999
	tet_child, tet_killw(), tet_signame() moved to separate files.

	Andrew Dingwall, UniSoft Ltd., October 1999
	added support for strict POSIX threads

************************************************************************/

#ifndef _WIN32	/* -WIN32-CUT-LINE- */

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <unistd.h>
#include <errno.h>
#include <signal.h>
#include <sys/wait.h>
#include "dtmac.h"
#include "dtthr.h"
#include "globals.h"
#include "alarm.h"
#include "error.h"
#include "servlib.h"
#include "dtetlib.h"
#include "sigsafe.h"
#include "tet_api.h"
#include "apilib.h"

#ifndef TET_LITE	/* -START-LITE-CUT- */
#  ifdef TET_STRICT_POSIX_THREADS
#    include "tslib.h"
#  endif
#endif			/* -END-LITE-CUT- */

#define	KILLWAIT	10

#  ifdef NEEDsrcFile
static char srcFile[] = __FILE__;
#  endif


/* ARGSUSED */
static void
sig_term(sig)
int sig;
{
	/* clean up on receipt of SIGTERM, but arrange for wait
	   status still to show termination by SIGTERM */

	struct sigaction sa;

	if (tet_child > 0)
		(void) tet_killw(tet_child, KILLWAIT);

	sa.sa_handler = SIG_DFL;
	sa.sa_flags = 0; 
	(void) sigemptyset(&sa.sa_mask); 
	(void) sigaction(SIGTERM, &sa, (struct sigaction *)NULL);
	(void) kill(getpid(), SIGTERM);
}

#  ifdef FORK1
TET_IMPORT int tet_fork1(childproc, parentproc, waittime, exitvals)
#  else
TET_IMPORT int tet_fork(childproc, parentproc, waittime, exitvals)
#  endif
void (*childproc) ();
void (*parentproc) ();
int	waittime;
int	exitvals;
{
	int	rtval, err, status;
	pid_t	savchild, pid;
	char	buf[256];
	struct sigaction new_sa; 
	struct alrmaction new_aa, old_aa; 
#  ifdef TET_THREADS
	sigset_t oldset;
#    if !defined(FORK1) && !defined(TET_POSIX_THREADS)
	extern tet_mutex_t tet_top_mtx;
#    endif
#  endif /* TET_THREADS */

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	(void) fflush(stdout);
	(void) fflush(stderr);

	/* Save old value of tet_child in case of recursive calls
	   to tet_fork().  RESTORE tet_child BEFORE ALL RETURNS. */
	savchild = tet_child;

#  ifdef TET_THREADS
	/*
	** tet_fork1() and POSIX tet_fork() must obtain all mutexes and
	** locks before forking, to ensure they are not held by threads
	** which will not exist in the child.
	**
	** UI tet_fork() must obtain tet_top_mtx, to ensure other threads
	** in the child do not try to access the servers before this thread
	** disconnects the inherited parent connections and logs on again.
	**
	** Note that by obtaining tet_top_mtx without going through API_LOCK
	** we are assuming that these functions are never called from other
	** API functions
	*/

	err = TET_THR_SIGSETMASK(SIG_BLOCK, &tet_blockable_sigs, &oldset);
	if (err != 0)
	{
		/* not fatal, as we haven't forked yet */
		error(err, "TET_THR_SIGSETMASK() failed in tet_fork1()", (char *)0);
		/* tet_child = savchild; not needed */
		tet_errno = TET_ER_ERR;
		return -1;
	}

#    if defined(FORK1) || defined(TET_POSIX_THREADS)
	tet_mtx_lock();
#    else
	TET_MUTEX_LOCK(&tet_top_mtx);
#    endif

#  endif /* TET_THREADS */

#  ifndef TET_LITE	/* -START-LITE-CUT- */
#    ifdef TET_STRICT_POSIX_THREADS
	/*
	** when multithreaded in strict POSIX mode, we need to put
	** TET_TIARGS and TET_TSARGS in the environment before the fork()
	** because they might be needed for a subsequent call to
	** tet_exec() and the child can't do it then
	*/
	if (tet_api_status & TET_API_MULTITHREAD) {
		if (tet_ti_tcmputenv(tet_mysysid, tet_snid, tet_xrid, tet_snames, tet_Nsname) < 0)
			tet_error(errno, "warning: can't add TET_TIARGS to environment in tet_fork()");
		if (tet_ts_tcmputenv() < 0) 
			tet_error(errno, "warning: can't add TET_TSARGS to environment in tet_fork()");
	}
#    endif
#  endif		/* -END-LITE-CUT- */

#  if defined(FORK1) && !defined(TET_POSIX_THREADS)
	pid = fork1();
#  else
	pid = fork();
#  endif

	/*
	** if this is a child process, we must update tet_api_status
	** before touching tet_child
	** (this is because in the thread-safe API tet_child is a macro
	** which invokes a function and the function uses the value of
	** tet_api_status)
	*/
	if (pid == 0) {
		/*
		** only async-signal safe calls here when in
		** strict POSIX mode
		*/
		if (tet_api_status & TET_API_MULTITHREAD) {
			tet_api_status |= TET_API_CHILD_OF_MULTITHREAD;
#  if defined(FORK1) || defined(TET_POSIX_THREADS)
			tet_api_status &= ~TET_API_MULTITHREAD;
#  endif
		}
		tet_mypid = getpid();
	}

	switch (tet_child = pid)
	{
	
	case -1:
		err = errno;
#  ifdef TET_THREADS
#    if defined(FORK1) || defined(TET_POSIX_THREADS)
		tet_mtx_unlock();
#    else
		TET_MUTEX_UNLOCK(&tet_top_mtx);
#    endif
		(void) TET_THR_SIGSETMASK(SIG_SETMASK, &oldset, (sigset_t *)0);
#  endif /* TET_THREADS */

		(void) sprintf(buf,
#  ifdef FORK1
#    ifdef TET_POSIX_THREADS
			"fork() failed in tet_fork1() - errno %d (%s)",
#    else
			"fork1() failed in tet_fork1() - errno %d (%s)",
#    endif
#  else
			"fork() failed in tet_fork() - errno %d (%s)",
#  endif
			err, tet_errname(err));
		tet_infoline(buf);
		tet_result(TET_UNRESOLVED);
		tet_child = savchild;
		tet_errno = TET_ER_FORK;
		return -1;

	case 0:
		/* child process */

		/*
		** NOTE: if we are running in strict POSIX mode and the
		** parent was multi-threaded, only calls to async-signal
		** safe functions are allowed in the child process 
		*/

#  if defined(FORK1) || defined(TET_POSIX_THREADS)
#    ifdef TET_STRICT_POSIX_THREADS
		if (!IS_CHILD_OF_MULTITHREAD_PARENT)
#    endif
		{
			tet_start_tid = TET_THR_SELF();
			/* empty the thread table */
			tet_thrtab_reset();
		}
#  endif

		/* reset all the signals in the child if so required */
		if (waittime >= 0)
			tet_sigreset();

#  ifndef TET_LITE /* -START-LITE-CUT- */
		/* disconnect from all connected servers */
		tet_disconnect();

		/* logon again to XRESD and SYNCD */
#    ifdef TET_STRICT_POSIX_THREADS
		if (IS_CHILD_OF_MULTITHREAD_PARENT)
			tet_combined_ok = 0;
		else
#    endif
		{
			if (tet_xdlogon() == 0)
				(void) tet_xdxrsend(tet_xrid);
			(void) tet_sdlogon();
		}
#  endif /* -END-LITE-CUT- */

#  ifdef TET_THREADS
#    ifdef TET_STRICT_POSIX_THREADS
		if (IS_CHILD_OF_MULTITHREAD_PARENT)
		{
			(void) sigprocmask(SIG_SETMASK, &oldset,
				(sigset_t *) 0);
		}
		else
#    endif
		{
#    if defined(FORK1) || defined(TET_POSIX_THREADS)
			tet_mtx_unlock();
#    else
			TET_MUTEX_UNLOCK(&tet_top_mtx);
#    endif
			(void) TET_THR_SIGSETMASK(SIG_SETMASK, &oldset,
				(sigset_t *) 0);
		}
#  endif /* TET_THREADS */

		/* change context to distinguish output from parent's */
#  ifdef TET_STRICT_POSIX_THREADS
		if (IS_CHILD_OF_MULTITHREAD_PARENT)
		{
			/*
			** tet_setcontext() and tet_setblock() aren't
			** async-signal safe, so we must fudge it here
			*/
			tet_context = (long) getpid();
			tet_next_block = tet_block = 1;
			tet_sequence = 1;
		}
		else
#  endif
		{
			tet_setcontext();
		}

#  if defined(FORK1) || defined(TET_POSIX_THREADS)
		/* threads version of tet_setcontext() does not reset the
		   block number(s), but for tet_fork1() and POSIX
		   tet_fork() we know there is only one thread in the
		   child, so it's OK to reset it. */
#    ifdef TET_STRICT_POSIX_THREADS
		if (!IS_CHILD_OF_MULTITHREAD_PARENT)
#    endif
		{
			tet_next_block = 0;
			tet_setblock();
		}
#  endif
		
		/* call child function, and if it returns exit with code 0 */
		(*childproc) ();
#  ifdef TET_THREADS
#    ifdef TET_STRICT_POSIX_THREADS
		if (!IS_CHILD_OF_MULTITHREAD_PARENT)
#    endif
		{
			tet_cln_threads(0);
		}
#  endif

#  ifdef TET_STRICT_POSIX_THREADS
		if (IS_CHILD_OF_MULTITHREAD_PARENT)
			_exit(0);
		else
#  endif
		{
			tet_exit(0);
		}
	}

	/* parent process */

	/* if SIGTERM is set to default (e.g. if this tet_fork() was
	   called from a child), catch it so we can propagate tet_killw() */
	if (sigaction(SIGTERM, (struct sigaction *)NULL, &new_sa) != -1 &&
		new_sa.sa_handler == SIG_DFL)
	{
		new_sa.sa_handler = sig_term;
		(void) sigaction(SIGTERM, &new_sa, (struct sigaction *)NULL);
	}

#  ifdef TET_THREADS
#    if defined(FORK1) || defined(TET_POSIX_THREADS)
	tet_mtx_unlock();
#    else
	TET_MUTEX_UNLOCK(&tet_top_mtx);
#    endif
	(void) TET_THR_SIGSETMASK(SIG_SETMASK, &oldset, (sigset_t *)0);
#  endif /* TET_THREADS */

	if (parentproc != NULL)
	{
		tet_setblock();
		(*parentproc) ();
	}

	tet_setblock();

	/* no API_LOCK here */

	/* negative waittime means no wait required (i.e. parentproc does
	   the wait, or the child is to be killed if still around) */

	if (waittime < 0)
	{
		(void) tet_killw(tet_child, KILLWAIT);
		tet_child = savchild;
		return 0;
	}

	/* wait for child, with timeout if required */

	if (waittime > 0)
	{
		new_aa.waittime = waittime; 
		new_aa.sa.sa_handler = tet_catch_alarm; 
		new_aa.sa.sa_flags = 0; 
		(void) sigemptyset(&new_aa.sa.sa_mask); 
		tet_alarm_flag = 0; 
		if (tet_set_alarm(&new_aa, &old_aa) == -1)
			fatal(errno, "failed to set alarm", (char *)0);
	}

	rtval = waitpid(tet_child, &status, WUNTRACED);
	err = errno; 

	if (waittime > 0)
		(void) tet_clr_alarm(&old_aa);

	/* check child wait status shows valid exit code, if not
	   report wait status and give UNRESOLVED result */

	if (rtval == -1)
	{
		if (tet_alarm_flag > 0)
			(void) sprintf(buf, "child process timed out");
		else
			(void) sprintf(buf, "waitpid() failed - errno %d (%s)",
				err, tet_errname(err));
		tet_infoline(buf);
		tet_result(TET_UNRESOLVED);
		(void) tet_killw(tet_child, KILLWAIT);

		switch (err)
		{
		case ECHILD: tet_errno = TET_ER_PID; break;
		case EINVAL: tet_errno = TET_ER_INVAL; break;
		case EINTR:  tet_errno = TET_ER_WAIT; break;
		default:
#  ifdef FORK1
			error(err, "tet_fork1() got unexpected errno value from waitpid()",
			    (char *)0);
#  else
			error(err, "tet_fork() got unexpected errno value from waitpid()",
			    (char *)0);
#  endif
			tet_errno = TET_ER_ERR;
		}
		tet_child = savchild;
		return -1;
	}
	else if (WIFEXITED(status))
	{
		status = WEXITSTATUS(status);

		if ((status & ~exitvals) == 0)
		{
			/* Valid exit code */

			tet_child = savchild;
			return status;
		}
		else
		{
			(void) sprintf(buf,
				"child process gave unexpected exit code %d",
				status);
			tet_infoline(buf);
		}
	}
	else if (WIFSIGNALED(status))
	{
		status = WTERMSIG(status);
		(void) sprintf(buf,
			"child process was terminated by signal %d (%s)",
			status, tet_signame(status));
		tet_infoline(buf);
	}
	else if (WIFSTOPPED(status))
	{
		status = WSTOPSIG(status);
		(void) sprintf(buf,
			"child process was stopped by signal %d (%s)",
			status, tet_signame(status));
		tet_infoline(buf);
		(void) tet_killw(tet_child, KILLWAIT);
	}
	else
	{
		(void) sprintf(buf,
			"child process returned bad wait status (%#x)", status);
		tet_infoline(buf);
	}

	tet_result(TET_UNRESOLVED);

	tet_child = savchild;
	tet_errno = TET_ER_ERR;
	return -1;
}

#else		/* -START-WIN32-CUT- */

int tet_fork_not_implemented;

#endif		/* -END-WIN32-CUT- */

