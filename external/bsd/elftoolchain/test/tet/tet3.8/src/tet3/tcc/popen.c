/*
 *	SCCS: @(#)popen.c	1.2 (02/01/18)
 *
 *	UniSoft Ltd., London, England
 *
 * Copyright (c) 1999 The Open Group
 * All rights reserved.
 *
 * No part of this source code may be reproduced, stored in a retrieval
 * system, or transmitted, in any form or by any means, electronic,
 * mechanical, photocopying, recording or otherwise, except as stated
 * in the end-user licence agreement, without the prior permission of
 * the copyright owners.
 * A copy of the end-user licence agreement is contained in the file
 * Licence which accompanies this distribution.
 * 
 * Motif, OSF/1, UNIX and the "X" device are registered trademarks and
 * IT DialTone and The Open Group are trademarks of The Open Group in
 * the US and other countries.
 *
 * X/Open is a trademark of X/Open Company Limited in the UK and other
 * countries.
 *
 */

#ifndef lint
static char sccsid[] = "@(#)popen.c	1.2 (02/01/18) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)popen.c	1.2 02/01/18 TETware release 3.8
NAME:		popen.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	July 1999

DESCRIPTION:
	popen() and pclose() for tcc

	On UNIX systems these functions are needed because poepn() and
	pclose() are not in POSIX.1.
	On Win32 systems these functions are simply wrappers for _popen()
	and _pclose() in the MS C runtime support library.

	On UNIX systems the return value for tcc_pclose() is taken from
	the exit status returned by the underlying waitpid() call.
	It should be interpreted by the W*() macros in <sys/wait.h>.
	On Win32 systems the return value for tcc_pclose() is taken from
	the exit status returned by the underlying _cwait() call.
	It is _not_ byte-swapped, despite what is claimed in the MS
	documentation for _pclose().

	On UNIX systems the pipeline is executed with SIGINT and SIGQUIT
	ignored; thus a keyboard signal does not interrupt the pipeline
	command.
	This means that keyboard signals may be used to interrupt test
	case processing without interrupting the pipeline command as well.
	Unfortunately, it doesn't seem to be possible to do the same on
	a Win32 system.
	Signals are always reset to the default action in a child process,
	even when ignored in the parent process.
	When keyboard signals are turned off for the duration of the call
	to _popen() by using SetConsoleCtrlHandler(), this condition
	doesn't seem to get propagated to a process started by a call
	to _popen().
	So the pipeline command must do this itself (sigh!).

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#ifndef _WIN32			/* -WIN32-CUT-LINE- */
#  include <errno.h>
#  include <unistd.h>
#  include <signal.h>
#  include <fcntl.h>
#  include <sys/types.h>
#  include <sys/wait.h>
#endif				/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#ifndef _WIN32			/* -WIN32-CUT-LINE- */
#  include "error.h"
#  include "bstring.h"
#  include "ltoa.h"
#  include "dtetlib.h"
#endif				/* -WIN32-CUT-LINE- */


#ifndef _WIN32			/* -WIN32-CUT-LINE- */

#  ifdef NEEDsrcFile
     static char srcFile[] = __FILE__;	/* file name for error reporting */
#  endif


   /*
   ** structure to associate a process ID with the stdio stream pointer
   ** returned by tcc_popen()
   */
   struct popentab {
   	FILE *po_fp;		/* stream pointer */
   	pid_t po_pid;		/* process ID */
   };

   /*
   ** pointer to the popen table, its length (in bytes) and number of
   ** entries in the table
   */
   static struct popentab *popentab;
   static int lpopentab, npopentab;


   /* static function declarations */
   static struct popentab *poalloc PROTOLIST((void));
   static struct popentab *pofind PROTOLIST((FILE *));

#endif				/* -WIN32-CUT-LINE- */


/*
**	tcc_popen() - open a pipe to a process and return a stdio
**		stream pointer thereto
**
**	return (FILE *) 0 with errno set on error
*/

FILE *tcc_popen(cmd, mode)
char *cmd, *mode;
{
#ifdef _WIN32			/* -START-WIN32-CUT- */

	return(_popen(cmd, mode));

#else				/* -END-WIN32-CUT- */

	int errsave, fd, fdfrom, fdto, pfd[2];
	struct popentab *pop;
	struct sigaction sa;
	static char shell[] = "/bin/sh";
	static char *sh_args[] = { "sh", "-c", (char *) 0, (char *) 0 };

	/* do a sanity check on the mode argument */
	switch (*mode) {
	case 'r':
	case 'w':
		break;
	default:
		error(0, "tcc_popen(): invalid mode", mode);
		errno = EINVAL;
		return((FILE *) 0);
	}

	/* allocate a popentab element */
	if ((pop = poalloc()) == (struct popentab *) 0)
		return((FILE *) 0);

	/* create a pipe */
	if (pipe(pfd) < 0) {
		errsave = errno;
		error(errno, "can't create pipe", (char *) 0);
		errno = errsave;
		return((FILE *) 0);
	}

	/* create a child process */
	if ((pop->po_pid = (pid_t) tet_dofork()) < 0) {
		/* can't fork */
		errsave = errno;
		error(errno, "can't fork", (char *) 0);
		(void) close(pfd[0]);
		(void) close(pfd[1]);
		errno = errsave;
	}
	else if (pop->po_pid == 0) {
		/*
		** in child -
		** ignore keyboard signals;
		** close one side of the pipe and attach the other side
		** to the std{in|out};
		** ensure that files other then stdin, stdout and stderr are
		** closed and exec the shell
		*/
		sa.sa_handler = SIG_IGN;
		sa.sa_flags = 0;
		(void) sigemptyset(&sa.sa_mask);
		(void) sigaction(SIGINT, &sa, (struct sigaction *) 0);
		sa.sa_handler = SIG_IGN;
		sa.sa_flags = 0;
		(void) sigemptyset(&sa.sa_mask);
		(void) sigaction(SIGQUIT, &sa, (struct sigaction *) 0);
		if (*mode == 'r') {
			(void) close(pfd[0]);
			fdfrom = pfd[1];
			fdto = 1;
		}
		else {
			(void) close(pfd[1]);
			fdfrom = pfd[0];
			fdto = 0;
		}
		(void) close(fdto);
		errno = 0;
		if (fcntl(fdfrom, F_DUPFD, fdto) != fdto) {
			error(errno, "can't dup pipe fd", tet_i2a(fdfrom));
			_exit(127);
		}
		(void) close(fdfrom);
		for (fd = tet_getdtablesize() - 1; fd > 2; fd--)
			if (fd != fdto && fd != fdfrom)
				(void) close(fd);
		sh_args[2] = cmd;
		(void) execv(shell, sh_args);
		error(errno, "can't exec", shell);
		_exit(127);
	}
	else {
		/*
		** in parent -
		** close one side of the pipe and ensure that the other side
		** is marked close-on-exec;
		** attach the open side of the pipe to a stream
		*/
		if (*mode == 'r') {
			(void) close(pfd[1]);
			fdfrom = pfd[0];
		}
		else {
			(void) close(pfd[0]);
			fdfrom = pfd[1];
		}
		if (
			tet_fioclex(fdfrom) == 0 &&
			(pop->po_fp = fdopen(fdfrom, mode)) == (FILE *) 0
		) {
			errsave = errno;
			error(errno, "fdopen() failed on fd", tet_i2a(fdfrom));
			errno = errsave;
		}
		if (pop->po_fp == (FILE *) 0) {
			errsave = errno;
			(void) close(fdfrom);
			(void) waitpid(pop->po_pid, (int *) 0, 0);
			pop->po_pid = (pid_t) -1;
			errno = errsave;
		}
	}

	return(pop->po_fp);

#endif				/* -WIN32-CUT-LINE- */
}

/*
**	tcc_pclose() - close a pipe opened by tcc_popen() and wait for
**		the child process to finish
**
**	return the child's exit status or -1 with errno set on error
*/
int tcc_pclose(fp)
FILE *fp;
{
#ifdef _WIN32			/* -START-WIN32-CUT- */

	return(_pclose(fp));

#else				/* -END-WIN32-CUT- */

	struct popentab *pop;
	int status;

	/* find the PID associated with fp */
	if ((pop = pofind(fp)) == (struct popentab *) 0) {
		(void) fclose(fp);
		errno = EINVAL;
		return(-1);
	}

	ASSERT(fp == pop->po_fp);

	/* close the pipe */
	if (fclose(fp) == EOF)
		error(errno, "fclose() failed", (char *) 0);
	pop->po_fp = (FILE *) 0;

	/* wait for the child process to terminate */
	if (waitpid(pop->po_pid, &status, 0) == (pid_t) -1)
		status = -1;
	pop->po_pid = (pid_t) -1;

	return(status);

#endif				/* -WIN32-CUT-LINE- */
}


#ifndef _WIN32			/* -WIN32-CUT-LINE- */
/*
**	poalloc() - allocate a popentab element and return a pointer thereto
**
**	return (struct popentab *) 0 on error
*/
static struct popentab *poalloc()
{
	struct popentab *pop;
	int needlen;

	/* see if there is an empty element */
	if ((pop = pofind((FILE *) 0)) == (struct popentab *) 0) {
		/* none found - grow the table */
		needlen = (npopentab + 1) * (int) sizeof *popentab;
		if (BUFCHK((char **) &popentab, &lpopentab, needlen) < 0)
			return((struct popentab *) 0);
		pop = popentab + npopentab++;
	}

	/* initialise the element and return */
	bzero((char *) pop, sizeof *pop);
	pop->po_pid = (pid_t) -1;
	return(pop);
}

/*
**	pofind() - find the popentab element corresponding to fp and
**		return a pointer thereto
**
**	return (struct popentab *) 0 if the element cannot be found
*/
static struct popentab *pofind(fp)
FILE *fp;
{
	struct popentab *pop;

	if (popentab)
		for (pop = popentab; pop < popentab + npopentab; pop++)
			if (pop->po_fp == fp)
				return(pop);

	return((struct popentab *) 0);
}

#endif				/* -WIN32-CUT-LINE- */

