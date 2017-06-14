/*
 *	SCCS: @(#)tcfexec.c	1.7 (99/11/15)
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
static char sccsid[] = "@(#)tcfexec.c	1.7 (99/11/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcfexec.c	1.7 99/11/15 TETware release 3.8
NAME:		tcfexec.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	October 1996

DESCRIPTION:
	tcc action function - start a new process, possibly with redirected
	stdout and stderr

	this function moved from tccd/exec.c to here

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., May 1997
	port to Windows 95

	Andrew Dingwall, UniSoft Ltd., July 1997
	Only do NO sync from a child process so as to avoid deadlock.
	Added support the MT DLL version of the C runtime support library
	on Win32 systems.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <errno.h>
#include <fcntl.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <io.h>
#else		/* -END-WIN32-CUT- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtmsg.h"
#include "error.h"
#include "globals.h"
#include "synreq.h"
#include "servlib.h"
#include "dtetlib.h"
#include "tcclib.h"

#ifndef NOTRACE
#  include "ltoa.h"
#endif

/* open mode for files */
#define MODEANY \
	((mode_t) (S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH))

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


#ifndef _WIN32	/* -WIN32-CUT-LINE- */
extern char **ENVIRON;
#endif		/* -WIN32-CUT-LINE- */

/* static function declarations */
#ifdef _WIN32	/* -START-WIN32-CUT- */
static int do_spawn PROTOLIST((char *, char **));
static int do_spawn_redirect PROTOLIST((char *, char **, char *));
static int dsr2 PROTOLIST((char *, char **, int));
static int dsr3 PROTOLIST((char *, char **, int));
#else		/* -END-WIN32-CUT- */
static int checkexec PROTOLIST((char *));
static void donasync PROTOLIST((long));
#endif		/* -WIN32-CUT-LINE- */


/*
**	tcf_exec() - start a new process
**
**	return ER_OK if successful or other ER_* error code on error
**
**	if successful, the pid of the new process is returned indirectly
**	through *pidp
*/

int tcf_exec(path, argv, outfile, snid, flag, pidp)
char *path, **argv, *outfile;
long snid;
int flag, *pidp;
{
	register int pid, rc;

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	register int fd, n;
#endif		/* -WIN32-CUT-LINE- */

#ifndef NOTRACE
	register char **ap;
#endif /* !NOTRACE */

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	/* see if an exec has any chance of succeeding */
	if (checkexec(path) < 0) {
		TRACE3(Ttcclib, 4, "checkexec(\"%s\") failed, errno = %s",
			path, tet_errname(errno));
		return(ER_NOENT);
	}
#endif		/* -WIN32-CUT-LINE- */

#ifndef NOTRACE
	if (Ttcclib > 0) {
		TRACE2(Ttcclib, 4, "exec \"%s\"", path);
		for (ap = argv; *ap; ap++)
			TRACE2(Ttcclib, 6, "arg = \"%s\"", *ap);
		for (ap = ENVIRON; *ap; ap++)
			TRACE2(Ttcclib, 8, "env = \"%s\"", *ap);
	}
#endif /* !NOTRACE */

#ifdef _WIN32		/* -START-WIN32-CUT- */
	/*
	** if an output file has been specified, save stdout and stderr;
	** then open the output file as stdout and stderr immediately
	** before the spawn(), then restore them immediately afterwards
	*/

	if (outfile && *outfile)
		pid = do_spawn_redirect(path, argv, outfile);
	else
		pid = do_spawn(path, argv);
	rc = (pid == -1) ? ER_ERR : ER_OK;

#else /* _WIN32 */	/* -END-WIN32-CUT- */

	/* open the output file if one has been specified */
	if (outfile && *outfile) {
		TRACE2(Ttcclib, 4, "send output to \"%s\"", outfile);
		if ((fd = open(outfile, O_WRONLY | O_CREAT | O_TRUNC, MODEANY)) < 0) {
			error(errno, "can't open", outfile);
			return(ER_ERR);
		}
		else if (tet_fioclex(fd) < 0) {
			(void) close(fd);
			return(ER_ERR);
		}
	}
	else
		fd = -1;

	/* do the fork and exec -
		in the child process:
		if outfile was specified, attach stdout and stderr to outfile
		the original outfile fd is already close-on-exec
		stdin is attached to /dev/null
		close all other file descriptors
		the "can't exec" message goes to outfile if one is specified */
	if ((pid = tet_dofork()) == 0) {
		/* in child */
		if (fd >= 0) {
			(void) fflush(stdout);
			(void) close(1);
			if (fcntl(fd, F_DUPFD, 1) != 1) {
				error(errno, "can't dup stdout", (char *) 0);
				_exit(~0);
			}
			(void) fflush(stderr);
			(void) close(2);
			if (fcntl(fd, F_DUPFD, 2) != 2) {
				error(errno, "can't dup stderr", (char *) 0);
				_exit(~0 - 1);
			}
		}
		for (n = tet_getdtablesize() - 1; n > 2; n--) {
			if (n != fd)
				(void) close(n);
		}
		tcc_exec_signals();
		(void) execvp(path, argv);
		error(errno, "can't exec", path);
		switch (flag) {
		case TCF_EXEC_TEST:
		case TCF_EXEC_USER:
			donasync(snid);
			break;
		}
		_exit(~0);
		/* NOTREACHED */
	}
	else if (pid < 0) {
		error(errno, "fork failed: path =", path);
		rc = ER_FORK;
	}
	else {
		/* in parent */
		rc = ER_OK;
	}

	/* close outfile in the parent if one was specified */
	if (fd >= 0)
		(void) close(fd);

#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */

	TRACE3(Ttcclib, 4, "after exec: pid = %s, return %s",
		tet_i2a(pid), tet_ptrepcode(rc));

	*pidp = pid;
	return(rc);
}


#ifndef _WIN32		/* -WIN32-CUT-LINE- */

/*
**	checkexec() - see if an exec has any chance of succeeding
**
**	return 0 if successful or -1 on error
*/

static int checkexec(file)
char *file;
{
	register char *p1, *p2;
	register char *path;
	char fname[MAXPATH];

	/* see if there is a / in the file name */
	for (p1 = file; *p1; p1++)
		if (*p1 == '/')
			break;

	/* if there is or there is no PATH, just check the file name */
	if (*p1 || (path = getenv("PATH")) == (char *) 0 || !*path) {
		TRACE2(Ttcclib, 6, "checkexec: try \"%s\"", file);
		return(tet_eaccess(file, 01));
	}

	/*
	** otherwise, try prepending each component of the PATH environment
	** variable
	*/
	TRACE2(Ttcclib, 6, "checkexec: PATH = \"%s\"", path);
	p1 = path;
	p2 = fname;
	do {
		if (!*p1 || *p1 == ':') {
			if (p2 > fname && p2 < &fname[sizeof fname - 2])
				*p2++ = '/';
			*p2 = '\0';
			(void) sprintf(p2, "%.*s",
				(int) (&fname[sizeof fname - 1] - p2), file);
			TRACE2(Ttcclib, 6, "checkexec: try \"%s\"", fname);
			if (tet_eaccess(fname, 01) == 0)
				return(0);
			p2 = fname;
		}
		else if (p2 < &fname[sizeof fname - 2])
			*p2++ = *p1;
	} while (*p1++);

	/* here if not found anywhere */
	return(-1);
}

/*
**	donasync() - do a NO auto-sync if a testcase or user exec fails
**
**	since this function may block, it can only be called from a
**	child process; so it can't be used on WIN32
*/

#ifdef TET_LITE	/* -LITE-CUT-LINE- */
/* ARGSUSED */
#endif /* TET_LITE */	/* -LITE-CUT-LINE- */

static void donasync(snid)
long snid;
{
#ifndef TET_LITE	/* -START-LITE-CUT- */

	/* log on to the syncd */
	if (tet_sdlogon() < 0)
		return;

	if (tet_sdasync(snid, -1L, SV_EXEC_SPNO, SV_NO, SV_EXEC_TIMEOUT, (struct synreq *) 0, (int *) 0) < 0)
		error(0, "after failed exec, autosync(NO) failed:",
			tet_ptrepcode(tet_sderrno));

	SLEEP(2);
	(void) tet_sdlogoff(0);

#endif /* !TET_LITE */	/* -END-LITE-CUT- */
}

#endif /* !_WIN32 */	/* -WIN32-CUT-LINE- */


#ifdef _WIN32		/* -START-WIN32-CUT- */

/*
**	do_spawn_redirect() - spawn a new process with stdout and stderr
**		redirected to a file
**
**	return the pid (HANDLE) of the new process if successful
**	or -1 on error
**
**	the current stdout and stderr are saved and the output file
**	is dup()'d onto stdout and stderr;
**	then the spawn() is done;
**	then the current stdout and stderr are restored
**
**	failure to restore stdout and stderr after the spawn() is fatal
**
**	diagnostics which are generated when stderr is closed should
**	appear on the console - see log.c for details
*/

static int do_spawn_redirect(path, argv, outfile)
char *path, **argv, *outfile;
{
	int fd, rc;

	TRACE2(Ttcclib, 4, "send output to \"%s\"", outfile);

	/* open the output capture file */
	if ((fd = _open(outfile, O_WRONLY | O_CREAT | O_TRUNC | O_NOINHERIT, MODEANY)) < 0) {
		error(errno, "can't open", outfile);
		return(-1);
	}

	rc = dsr2(path, argv, fd);

	(void) _close(fd);
	return(rc);
}

/*
**	dsr2() - extend the do_spawn_redirect() processing
**
**	return the pid (HANDLE) of the new process if successful
**	or -1 on error
*/

static int dsr2(path, argv, fd)
char *path, **argv;
int fd;
{
	int fd1save, rc;

	/* save the current stdout */
	if ((fd1save = _dup(1)) < 0) {
		error(errno, "can't dup stdout", (char *) 0);
		return(-1);
	}

	/* attach stdout to the output capture file */
	if (_dup2(fd, 1) < 0) {
		error(errno, "can't attach stdout to outfile before spawn()",
			(char *) 0);
		rc = -1;
	}
	else
		rc = dsr3(path, argv, fd);

	/* restore the stdout */
	if (_dup2(fd1save, 1) < 0)
		fatal(errno, "can't restore stdout after spawn()", (char *) 0);

	(void) _close(fd1save);
	return(rc);
}

/*
**	dsr3() - extend the do_spawn_redirect() processing some more
**
**	return the pid (HANDLE) of the new process if successful
**	or -1 on error
*/

static int dsr3(path, argv, fd)
char *path, **argv;
int fd;
{
	int fd2save, rc;

	/* save the current stderr */
	if ((fd2save = _dup(2)) == -1) {
		error(errno, "can't dup stderr", (char *) 0);
		return(-1);
	}

	/* attach stderr to the output capture file */
	if (_dup2(fd, 2) < 0) {
		error(errno, "can't attach stderr to outfile before spawn()",
			(char *) 0);
		rc = -1;
	}
	else
		rc = do_spawn(path, argv);

	/* restore the stderr */
	if (_dup2(fd2save, 2) < 0)
		fatal(errno, "can't restore stderr after spawn()", (char *) 0);

	(void) _close(fd2save);
	return(rc);
}

/*
**	do_spawn() - spawn a new process
**
**	return the pid (HANDLE) of the new process if successful
**	or -1 on error
**
**	note that any TRACE or diagnostics go to the output capture
**	file if output is redirected when this function is called
*/

static int do_spawn(path, argv)
char *path, **argv;
{
	register int n;
	struct _stat stbuf;

	/* ensure that the new process only inherits stdin, stdout and stderr */
	for (n = tet_getdtablesize() - 1; n > 2; n--) {
		if (_fstat(n, &stbuf) == 0)
			(void) tet_fioclex(n);
	}

	/*
	** do the spawn()
	**
	** for some reason, environ does NOT get passed to the new process
	** if _spawnvp() is used, so we must use _spawnvpe() and pass it
	** explicitly
	*/
	(void) _flushall();
	if ((n = tet_spawnvpe(path, argv, ENVIRON)) == -1)
		error(errno, "spawn failed, path =", path);

	return(n);
}

#endif /* _WIN32 */	/* -END-WIN32-CUT- */

