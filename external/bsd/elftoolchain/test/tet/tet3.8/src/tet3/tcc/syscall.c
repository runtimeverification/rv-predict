/*
 *	SCCS: @(#)syscall.c	1.5 (97/07/21)
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
static char sccsid[] = "@(#)syscall.c	1.5 (97/07/21) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)syscall.c	1.5 97/07/21 TETware release 3.8
NAME:		syscall.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	functions which look like system calls

	in TETware-Lite, each function invokes a system call on the
	local system

	in fully-featured TETware, each function invokes a server
	interface function which instructs a TCCD to perform the
	required action

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include <sys/types.h>
#include <time.h>
#include <errno.h>
#ifdef TET_LITE		/* -LITE-CUT-LINE- */
#  include <sys/stat.h>
#  ifdef _WIN32		/* -START-WIN32-CUT- */
#    include <direct.h>
#  else			/* -END-WIN32-CUT- */
#    include <unistd.h>
#    include <signal.h>
#    include <sys/wait.h>
#  endif /* _WIN32 */	/* -WIN32-CUT-LINE- */
#endif /* TET_LITE */	/* -LITE-CUT-LINE- */
#include "dtmac.h"
#include "dtmsg.h"
#include "error.h"
#include "servlib.h"
#include "dtetlib.h"
#include "tcc.h"
#include "tcclib.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif

#ifdef TET_LITE	/* -LITE-CUT-LINE- */
/* mode for created directories */
#  define MODEANY	((mode_t) (S_IRWXU | S_IRWXG | S_IRWXO))

int tet_tcerrno;	/* fake TCCD reply code */
#endif	/* -LITE-CUT-LINE- */

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	all these functions set tet_tcerrno on return
**
**	if an error is returned:
**		if the error is a system error, errno is set to indicate
**		the cause of the error;
**		otherwise, errno is set to 0 and tet_tcerrno is set to
**		indicate the cause of the error
*/


#ifdef TET_LITE	/* -LITE-CUT-LINE- */
/* ARGSUSED */
#endif /* TET_LITE */	/* -LITE-CUT-LINE- */
int tcc_access(sysid, path, mode)
int sysid, mode;
char *path;
{
	register int rc;

#ifdef TET_LITE	/* -LITE-CUT-LINE- */
	rc = tet_eaccess(path, mode);
	tet_tcerrno = rc < 0 ? tet_maperrno(errno) : ER_OK;
#else	/* -START-LITE-CUT- */
	rc = tet_tcaccess(sysid, path, mode);
	if (!IS_ER_ERRNO(tet_tcerrno))
		errno = 0;
#endif	/* -END-LITE-CUT- */

	return(rc);
}

#ifdef TET_LITE	/* -LITE-CUT-LINE- */
/* ARGSUSED */
#endif /* TET_LITE */	/* -LITE-CUT-LINE- */
int tcc_mkdir(sysid, dir)
int sysid;
char *dir;
{
	register int rc;

#ifdef TET_LITE	/* -LITE-CUT-LINE- */
	rc = tet_mkdir(dir, MODEANY);
	tet_tcerrno = rc < 0 ? tet_maperrno(errno) : ER_OK;
#else	/* -START-LITE-CUT- */
	rc = tet_tcmkdir(sysid, dir);
	if (!IS_ER_ERRNO(tet_tcerrno))
		errno = 0;
#endif	/* -END-LITE-CUT- */

	return(rc);
}

#ifdef TET_LITE	/* -LITE-CUT-LINE- */
/* ARGSUSED */
#endif /* TET_LITE */	/* -LITE-CUT-LINE- */
int tcc_rmdir(sysid, dir)
int sysid;
char *dir;
{
	register int rc;

#ifdef TET_LITE	/* -LITE-CUT-LINE- */
	rc = tet_rmdir(dir);
	tet_tcerrno = rc < 0 ? tet_maperrno(errno) : ER_OK;
#else	/* -START-LITE-CUT- */
	rc = tet_tcrmdir(sysid, dir);
	if (!IS_ER_ERRNO(tet_tcerrno))
		errno = 0;
#endif	/* -END-LITE-CUT- */

	return(rc);
}

#ifdef TET_LITE	/* -LITE-CUT-LINE- */
/* ARGSUSED */
#endif /* TET_LITE */	/* -LITE-CUT-LINE- */
int tcc_chdir(sysid, dir)
int sysid;
char *dir;
{
	register int rc;

#ifdef TET_LITE	/* -LITE-CUT-LINE- */
	rc = CHDIR(dir);
	tet_tcerrno = rc < 0 ? tet_maperrno(errno) : ER_OK;
#else	/* -START-LITE-CUT- */
	rc = tet_tcchdir(sysid, dir);
	if (!IS_ER_ERRNO(tet_tcerrno))
		errno = 0;
#endif	/* -END-LITE-CUT- */

	return(rc);
}

#ifdef TET_LITE	/* -LITE-CUT-LINE- */
/* ARGSUSED */
#endif /* TET_LITE */	/* -LITE-CUT-LINE- */
int tcc_unlink(sysid, fname)
int sysid;
char *fname;
{
	register int rc;

#ifdef TET_LITE	/* -LITE-CUT-LINE- */
	rc = UNLINK(fname);
	tet_tcerrno = rc < 0 ? tet_maperrno(errno) : ER_OK;
#else	/* -START-LITE-CUT- */
	rc = tet_tcunlink(sysid, fname);
	if (!IS_ER_ERRNO(tet_tcerrno))
		errno = 0;
#endif	/* -END-LITE-CUT- */

	return(rc);
}

int tcc_kill(sysid, pid, signum)
int sysid, signum;
long pid;
{
	register int rc;

	TRACE4(tet_Ttcc, 4, "sending signal %s to pid %s on system %s",
		tet_i2a(signum), tet_l2a(pid), tet_i2a(sysid));

#ifdef TET_LITE	/* -LITE-CUT-LINE- */
	rc = KILL(pid, signum);
	tet_tcerrno = rc < 0 ? tet_maperrno(errno) : ER_OK;
#else	/* -START-LITE-CUT- */
	rc = tet_tckill(sysid, pid, signum);
	if (!IS_ER_ERRNO(tet_tcerrno))
		errno = 0;
#endif	/* -END-LITE-CUT- */

	return(rc);
}

int tcc_waitnohang(sysid, remid, statp)
int sysid, *statp;
long remid;
{
	register int rc;
	register int errsave;

#ifdef TET_LITE		/* -LITE-CUT-LINE- */
#  ifndef _WIN32	/* -WIN32-CUT-LINE- */
	pid_t pid;
	int status;
#  endif /* !_WIN32 */	/* -WIN32-CUT-LINE- */
#endif /* TET_LITE */	/* -LITE-CUT-LINE- */

	TRACE3(tet_Ttcc, 4, "wait for pid %s on system %s",
		tet_l2a(remid), tet_i2a(sysid));

#ifdef TET_LITE		/* -LITE-CUT-LINE- */

#  ifdef _WIN32		/* -START-WIN32-CUT- */

	tet_tcerrno = tcf_win32wait((int) remid, 0, statp);
	rc = (tet_tcerrno == ER_OK) ? 0 : -1;
	if (!IS_ER_ERRNO(tet_tcerrno))
		errno = 0;

#  else /* _WIN32 */	/* -END-WIN32-CUT- */

	if ((pid = waitpid((pid_t) remid, &status, WNOHANG)) == (pid_t) 0) {
		tet_tcerrno = ER_WAIT;
		rc = -1;
	}
	else if (pid == (pid_t) -1) {
		tet_tcerrno = (errno == ECHILD) ? ER_PID : ER_ERR;
		rc = -1;
	}
	else {
		ASSERT(pid == (pid_t) remid);
		tet_tcerrno = ER_OK;
		*statp = tet_mapstatus(status);
		rc = 0;
	}

#  endif /* _WIN32 */	/* -WIN32-CUT-LINE- */

#else /* TET_LITE */	/* -START-LITE-CUT- */

	rc = tet_tcwait(sysid, remid, 0, statp);
	if (!IS_ER_ERRNO(tet_tcerrno))
		errno = 0;

#endif /* TET_LITE */	/* -END-LITE-CUT- */

	errsave = errno;

	TRACE4(tet_Ttcc, 4, "wait returns %s, %s = %s", tet_i2a(rc),
		rc < 0 ? "tet_tcerrno" : "status",
		rc < 0 ? tet_ptrepcode(tet_tcerrno) : tet_i2x(*statp));

	errno = errsave;
	return(rc);
}

