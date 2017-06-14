/*
 *	SCCS: @(#)tet_spawn.c	1.21 (99/11/15)
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
static char sccsid[] = "@(#)tet_spawn.c	1.21 (99/11/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tet_spawn.c	1.21 99/11/15 TETware release 3.8
NAME:		'C' API spawn process function
PRODUCT:	TETware
AUTHOR:		Geoff Clare, UniSoft Ltd.
DATE CREATED:	July 1996
SYNOPSIS:

	pid_t	tet_spawn(char *file, char *argv[], char *envp[]);
	int	tet_wait(pid_t pid, int *statp);
	int	tet_kill(pid_t pid, int sig);

DESCRIPTION:

	Tet_spawn() executes the specified file in a child process.
	The argv[] is passed to the tet_main() routine in the new
	process image.  The child calls tet_setcontext() to distinguish
	it's results file output from the parent's.  A call to
	tet_setblock() is made before returning control in the parent,
	to separate output made before and during execution of the child
	process.  Signals which are being caught in the parent are set
	to SIG_DFL in the child so unexpected signals will come
	through to the wait status.  If the exec() of the specified
	file fails, the child exits with code 127.

	Tet_wait() is used to obtain the wait status of a process
	previously spawned with tet_spawn().  A call to tet_setblock()
	is made, to separate output made during and after execution of
	the child.

	Tet_kill() sends the specified signal to the specified process.

MODIFICATIONS:
	Geoff Clare, UniSoft Ltd., Sept 1996
	Changes for TETWare-Lite.

	Andrew Dingwall, UniSoft Ltd., April 1997
	fixed problem with tet_spawn on Windows NT;
	improved thread-safety in mt environment

	Andrew Dingwall, UniSoft Ltd., May 1997
	port to Windows 95

	Andrew Dingwall, UniSoft Ltd., October 1997
	on UNIX systems, use a pipe to pass tet_errno from child to parent
	after a failed tet_exec();
	this enables tet_spawn() to return an error if the exec fails,
	like it does on Win32 systems

	Andrew Dingwall, UniSoft Ltd., February 1998
	Use TETware-specific macros to access threads functions and
	data items.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
	Andrew Dingwall, UniSoft Ltd., July 1999
	added support for 'other language' APIs
	enhanced signal reset code - moved to sigreset.c

	Andrew Dingwall, UniSoft Ltd., October 1999
	added support for strict POSIX threads


************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/types.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <process.h>
#else		/* -END-WIN32-CUT- */
#  include <unistd.h>
#  include <sys/wait.h>
#  include <signal.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtthr.h"
#include "globals.h"
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

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;
#endif

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
static pid_t	childpid;
#ifdef TET_THREADS
static sigset_t oldset;
#endif		/* TET_THREADS */
#endif		/* -WIN32-CUT-LINE- */


/* static function declarations */
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
static pid_t tet_sp2 PROTOLIST((char *, char *[], char *[]));
static pid_t tet_sp3 PROTOLIST((char *, char *[], char *[], int[]));
#endif		/* -WIN32-CUT-LINE- */

#ifndef _WIN32	/* -WIN32-CUT-LINE- */

/* ARGSUSED */
static void
sig_term(sig)
int sig;
{
	/* clean up on receipt of SIGTERM, but arrange for wait
	   status still to show termination by SIGTERM */

	struct sigaction sa;

	if (childpid > 0)
		(void) tet_killw(childpid, KILLWAIT);

	sa.sa_handler = SIG_DFL;
	sa.sa_flags = 0; 
	(void) sigemptyset(&sa.sa_mask); 
	(void) sigaction(SIGTERM, &sa, (struct sigaction *)NULL);
	(void) kill(getpid(), SIGTERM);
}

#endif		/* -WIN32-CUT-LINE- */


TET_IMPORT pid_t tet_spawn(file, argv, envp)
char *file;
char *argv[];
char *envp[];
{
	pid_t pid;

#ifdef _WIN32	/* -START-WIN32-CUT- */
	char **newargv = (char **) 0, **newenvp = (char **) 0;
	int rc;
#else		/* -END-WIN32-CUT- */
#  ifdef TET_THREADS
	int err;
#  endif /* TET_THREADS */
#endif		/* -WIN32-CUT-LINE- */

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	if (!file || !*file || !argv || !envp) {
		tet_errno = TET_ER_INVAL;
		return(-1);
	}

	(void) fflush(stdout);
	(void) fflush(stderr);

#ifdef _WIN32	/* -START-WIN32-CUT- */

	API_LOCK;
	rc = tet_exec_prep(file, argv, envp, &newargv, &newenvp);
	API_UNLOCK;

	if (rc < 0)
		pid = -1;
	else {
		ASSERT(newargv != (char **) 0);
		ASSERT(newenvp != (char **) 0);
		(void) _flushall();
		if ((pid = (pid_t) tet_spawnvpe(file, newargv, newenvp)) == -1)
			switch (errno) {
			case E2BIG:
				tet_errno = TET_ER_2BIG;
				break;
			case EINVAL:
				tet_errno = TET_ER_INVAL;
				break;
			case ENOENT:
				tet_errno = TET_ER_NOENT;
				break;
			case ENOEXEC:
			case ENOMEM:
			default:
				error(errno, "spawn failed in tet_spawn():",
					file);
				tet_errno = TET_ER_ERR;
				break;
			}
	}

	tet_exec_cleanup(envp, newargv, newenvp);

#else /* _WIN32 */	/* -END-WIN32-CUT- */

#  ifdef TET_THREADS
	/*
	 * Must obtain all mutexes and locks before fork1(), to ensure
	 * they are not held by threads which will not exist in the
	 * child.  Note that by obtaining tet_top_mtx without going
	 * through API_LOCK we are assuming that this function is never
	 * called from other API functions.
	 */
	err = TET_THR_SIGSETMASK(SIG_BLOCK, &tet_blockable_sigs, &oldset);
	if (err != 0)
	{
		/* not fatal, as we haven't forked yet */
		error(err, "TET_THR_SIGSETMASK() failed in tet_spawn()", (char *)0);
		tet_errno = TET_ER_ERR;
		return -1;
	}
	tet_mtx_lock();
#  endif /* TET_THREADS */

	/* fork and exec the new process */
	pid = tet_sp2(file, argv, envp);

#  ifdef TET_THREADS
	/* release all the mutexes and restore the signal mask */
	tet_mtx_unlock();
	(void) TET_THR_SIGSETMASK(SIG_SETMASK, &oldset, (sigset_t *)0);
#  endif /* TET_THREADS */

#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */

	tet_setblock();
	return pid;
}


#ifndef _WIN32		/* -WIN32-CUT-LINE- */

/*
**	tet_sp2() - extend the tet_spawn() processing on UNIX systems
**
**	return pid of spawned process if successful or -1 on error
*/

static pid_t tet_sp2(file, argv, envp)
char *file;
char *argv[];
char *envp[];
{
	int pfd[2];
	pid_t pid;

#  ifndef TET_LITE	/* -START-LITE-CUT- */
#    ifdef TET_STRICT_POSIX_THREADS
	/*
	** when multithreaded in strict POSIX mode, we need to put
	** TET_TIARGS and TET_TSARGS in the environment before the fork()
	** because the child can't do it
	*/
	if (tet_api_status & TET_API_MULTITHREAD) {
		if (tet_ti_tcmputenv(tet_mysysid, tet_snid, tet_xrid, tet_snames, tet_Nsname) < 0) {
			tet_error(errno, "can't add TET_TIARGS to environment in tet_spawn()");
			tet_errno = TET_ER_ERR;
			return(-1);
		}
		if (tet_ts_tcmputenv() < 0)  {
			tet_error(errno, "can't add TET_TSARGS to environment in tet_spawn()");
			tet_errno = TET_ER_ERR;
			return(-1);
		}
	}
#    endif
#  endif		/* -END-LITE-CUT- */

	/*
	** create a pipe that the parent can use to find out whether
	** the exec was successful in the child;
	*/
	if (pipe(pfd) < 0) {
		error(errno, "can't create pipe in tet_spawn()", (char *) 0);
		tet_errno = TET_ER_ERR;
		return(-1);
	}

	/* fork and exec the new process */
	pid = tet_sp3(file, argv, envp, pfd);

	/* close the pipe and return */
	if (pfd[0] >= 0)
		(void) close(pfd[0]);
	if (pfd[1] >= 0)
		(void) close(pfd[1]);
	return(pid);
}

/*
**	tet_sp3() - extend the tet_spawn() processing some more
**
**	return pid of spawned process if successful or -1 on error
*/

static pid_t tet_sp3(file, argv, envp, pfd)
char *file;
char *argv[];
char *envp[];
int pfd[];
{
	pid_t pid;
	int err, rc;
	struct sigaction new_sa; 
	int status;

	/*
	** make the pipe close-on-exec so that the parent reads EOF
	** if the exec succeeds in the child
	*/
	if (tet_fioclex(pfd[0]) < 0 || tet_fioclex(pfd[1]) < 0) {
		tet_errno = TET_ER_ERR;
		return(-1);
	}

	/* fork a (single-threaded) child process */
#  if defined(TET_THREADS) && !defined(TET_POSIX_THREADS)
	pid = fork1();
#  else /* TET_THREADS */
	pid = fork();
#  endif /* TET_THREADS */

	childpid = pid;
	switch (pid)
	{
	case -1:
		tet_errno = TET_ER_FORK;
		return -1;

	case 0:
		/* child process */

		/*
		** NOTE: if we are running in strict POSIX mode and the
		** parent was multi-threaded, only calls to async-signal
		** safe functions are allowed in the child process.
		*/

		/* update tet_api_status - must be first */
		if (tet_api_status & TET_API_MULTITHREAD) {
			tet_api_status |= TET_API_CHILD_OF_MULTITHREAD;
			tet_api_status &= ~TET_API_MULTITHREAD;
		}

		tet_child = 0;
		tet_mypid = getpid();

		/* close the read side of the pipe */
		(void) close(pfd[0]);
		pfd[0] = -1;

#  ifdef TET_THREADS
#    ifdef TET_STRICT_POSIX_THREADS
		if (!IS_CHILD_OF_MULTITHREAD_PARENT)
#    endif
		{
			tet_start_tid = TET_THR_SELF();
			/* empty the thread table */
			tet_thrtab_reset();
		}
#  endif /* TET_THREADS */

		/* set signals which were caught in parent to default */
		tet_sigreset();

#  ifndef TET_LITE /* -START-LITE-CUT- */
		/* disconnect from all connected servers */
		tet_disconnect();

		/* logon again to XRESD */
#    ifdef TET_STRICT_POSIX_THREADS
		if (IS_CHILD_OF_MULTITHREAD_PARENT)
			tet_combined_ok = 0;
		else
#    endif
		{
			if (tet_xdlogon() == 0)
				(void) tet_xdxrsend(tet_xrid);
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
			tet_mtx_unlock();
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

#  ifdef TET_THREADS
		/* threads version of tet_setcontext() does not reset the
		   block number(s), but as we called fork1() we know there
		   is only one thread in the child, so it's OK to reset it. */ 
#    ifdef TET_STRICT_POSIX_THREADS
		if (!IS_CHILD_OF_MULTITHREAD_PARENT)
#    endif
		{
			tet_next_block = 0;
			tet_setblock();
		}
#  endif /* TET_THREADS */

		/* execute specified file */
		errno = 0;
		(void) tet_exec(file, argv, envp);
		if (errno == ENOMEM)
		{
			/* This message is to distinguish malloc()
			   failure from exec() failure */
			error(errno, "tet_exec() failed:", file);
#  ifdef TET_STRICT_POSIX_THREADS
			if (!IS_CHILD_OF_MULTITHREAD_PARENT)
#  endif
			{
				(void) fflush(stderr);
			}
		}
		/* send tet_errno to the parent process */
		if ((rc = write(pfd[1], (void *) &tet_errno, sizeof tet_errno)) != sizeof tet_errno)
			error(rc < 0 ? errno : 0,
				"pipe write error in tet_spawn()", (char *) 0);
		(void) close(pfd[1]);
		pfd[1] = -1;
#  ifdef TET_STRICT_POSIX_THREADS
		if (!IS_CHILD_OF_MULTITHREAD_PARENT)
#  endif
		{
			tet_logoff();
		}
		_exit(127);
	}

	/* parent process */

	/* close the write side of the pipe */
	(void) close(pfd[1]);
	pfd[1] = -1;

	/*
	** read the pipe from the child -
	** the pipe is close-on-exec in the child, so if we get EOF this
	** (probably) means that the exec succeeded;
	** otherwise, the child writes its tet_errno value to the pipe
	*/
	err = 0;
	if ((rc = read(pfd[0], (void *) &err, sizeof err)) <= 0) {
		if (rc < 0) {
			/* pipe read error - could this ever happen ? */
			error(errno, "pipe read error in tet_spawn()",
				(char *) 0);
		}
		/*
		** here, the other side of the pipe is closed after
		** a successful exec -
		** if SIGTERM is set to default (e.g. if this tet_spawn()
		** was called from a child), catch it so we can propagate
		** SIGTERM to the child
		*/
		if (sigaction(SIGTERM, (struct sigaction *)NULL, &new_sa) != -1 &&
			new_sa.sa_handler == SIG_DFL)
		{
			new_sa.sa_handler = sig_term;
			(void) sigaction(SIGTERM, &new_sa,
				(struct sigaction *)NULL);
		}
	}
	else {
		/*
		** the child exec failed so update tet_errno and wait for
		** the child to terminate
		*/
		if (rc != sizeof err) {
			error(0, "unexpected partial read from pipe",
				"in tet_spawn()");
			tet_errno = TET_ER_INTERN;
		}
		else
			tet_errno = err;
		if (waitpid(pid, &status, 0) == -1)
			error(errno, "waitpid() failed in tet_spawn()",
				"after child exec failed");
		pid = -1;
	}

	return(pid);
}

#endif /* !_WIN32 */	/* -WIN32-CUT-LINE- */


TET_IMPORT int tet_wait(pid, statp)
pid_t pid;
int *statp;
{
	pid_t rtpid;
	int err;

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	struct sigaction new_sa; 
#endif		/* -WIN32-CUT-LINE- */

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	/* must wait for a specific process: no zero or -ve pid values
	   that waitpid() would treat specially */
#ifdef _WIN32	/* -START-WIN32-CUT- */
	if (pid == -1 || pid == -2)
#else		/* -END-WIN32-CUT- */
	if (pid <= 0)
#endif		/* -WIN32-CUT-LINE- */
	{
		tet_errno = TET_ER_INVAL;
		return -1;
	}

#ifdef _WIN32	/* -START-WIN32-CUT- */
	rtpid = _cwait(statp, pid, 0);
#else		/* -END-WIN32-CUT- */
	rtpid = waitpid(pid, statp, 0);
#endif		/* -WIN32-CUT-LINE- */
	err = errno;

	if (rtpid == -1)
	{
		switch (err)
		{
		case ECHILD:
			tet_errno = TET_ER_PID;
			break;
		case EINVAL:
			tet_errno = TET_ER_INVAL;
			break;
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
		case EINTR:
			tet_errno = TET_ER_WAIT;
			break;
#endif		/* -WIN32-CUT-LINE- */
		default:
			/* since error() is a macro, we can't just have the
			   _cwait()/waitpid() part conditional on _WIN32
			   (at least on MSVC++) we must repeat the lot!
			*/
#ifdef _WIN32	/* -START-WIN32-CUT- */
			error(err,
				"tet_wait() got unexpected errno value from",
				"_cwait()");
#else		/* -END-WIN32-CUT- */
			error(err,
				"tet_wait() got unexpected errno value from",
				"waitpid()");
#endif		/* -WIN32-CUT-LINE- */
			tet_errno = TET_ER_ERR;
		}
	}
	else
	{
		/* only do these things if the wait succeeds */

		API_LOCK;

		tet_setblock();

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
		/* undo SIGTERM handling */
		if (sigaction(SIGTERM, (struct sigaction *)NULL, &new_sa) != -1 &&
			new_sa.sa_handler == sig_term)
		{
			new_sa.sa_handler = SIG_DFL;
			(void) sigaction(SIGTERM, &new_sa,
				(struct sigaction *)NULL);
		}
#endif		/* -WIN32-CUT-LINE- */

		API_UNLOCK;
	}

	errno = err;
	return (rtpid == -1 ? -1 : 0);
}

TET_IMPORT int tet_kill(pid, sig)
pid_t pid;
int sig;
{
	int rtval;

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	/* must specify a single process: no zero or -ve pid values
	   that kill() would treat specially */
#ifdef _WIN32	/* -START-WIN32-CUT- */
	if (pid == -1 || pid == -2)
#else		/* -END-WIN32-CUT- */
	if (pid <= 0)
#endif		/* -WIN32-CUT-LINE- */
	{
		tet_errno = TET_ER_INVAL;
		return -1;
	}

	rtval = KILL(pid, sig);
	if (rtval == -1)
	{
		switch (errno)
		{
		case EINVAL:
			tet_errno = TET_ER_INVAL;
			break;
		case ESRCH:
			tet_errno = TET_ER_PID;
			break;
		case EPERM:
			tet_errno = TET_ER_PERM;
			break;
		default:
			error(errno,
			    "tet_kill() got unexpected errno value from KILL()",
			    (char *)0);
			tet_errno = TET_ER_ERR;
		}
	}

	return rtval;
}

