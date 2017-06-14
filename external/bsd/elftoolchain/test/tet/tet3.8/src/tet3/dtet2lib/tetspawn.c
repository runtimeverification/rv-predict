/*
 *	SCCS: @(#)tetspawn.c	1.4 (98/08/28)
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1997 X/Open Company Limited
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
static char sccsid[] = "@(#)tetspawn.c	1.4 (98/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tetspawn.c	1.4 98/08/28 TETware release 3.8
NAME:		tetspawn.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	May 1997

DESCRIPTION:
	function to spawn a new process on a WIN32 platform;
	searching the PATH and invoking an interpreter if necessary

	this function is necessary because we want to be able to cater
	for more file name extensions than _spawnvpe() understands;
	also, we can't call _spawnvpe() directly on Windows 95 because
	it doesn't understand '/' in PATH or in the file name

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., May 1998
	Use tet_basename() instead of a local static version.

************************************************************************/

#ifdef _WIN32	/* -START-WIN32-CUT- */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <ctype.h>
#include <process.h>
#include "dtmac.h"
#include "dtetlib.h"

#ifndef NOTRACE
#  include "ltoa.h"
#endif

/* table of known filename extensions and their interpreters */
struct extensions {
	char *ex_ext;		/* file extension */
	char *ex_interp;	/* name of interpreter */
};

static struct extensions extensions[] = {
	".ksh", "sh.exe",
	".pl", "perl.exe"
};

static int Nextensions = sizeof extensions / sizeof extensions[0];


/* static function declarations */
static int tet_sp2 PROTOLIST((char *, char **, char **));
static int tet_sp3 PROTOLIST((char *, char **, char **));
static int tet_sp4 PROTOLIST((char *, char *, char **, char **));


/*
**	tet_spawnvpe() - spawn a new process
**
**	return the pid (HANDLE) of the new process if successful
**	or -1 on error with errno set
**
**	if the file name does not contain a directory separator,
**	the file is searched for using the PATH environment variable
**
**	if the file name does not end with an extension, each of a set
**	of default extensions is tried in turn
**
**	if the extension of the file name indicates that an interpreter
**	is required to execute the file, the interpreter is invoked with
**	the file name and argv[1] -> argv[n] as arguments;
**
**	note that this function may be called recursively
*/

int tet_spawnvpe(file, argv, envp)
char *file, **argv, **envp;
{
	register char *p1, *p2;
	char *path = getenv("PATH");
	char buf[MAXPATH];
	int pid;

	/*
	** if the file name starts with a drive letter or contains a
	** directory separator, try to spawn the unmodified file name
	*/
	if (isdrvspec(file))
		return(tet_sp2(file, argv, envp));
	for (p1 = file; *p1; p1++)
		if (isdirsep(*p1))
			return(tet_sp2(file, argv, envp));

	/*
	** here if there is no directory separator
	**
	** if there is no PATH, try to spawn the unmodified file name
	*/
	if (!path || !*path)
		return(tet_sp2(file, argv, envp));

	/* otherwise, try prepending each PATH component in turn */
	p1 = path;
	p2 = buf;
	do {
		if (!*p1 || *p1 == ';') {
			if (p2 > buf && p2 < &buf[sizeof buf - 2])
				*p2++ = '/';
			*p2 = '\0';
			(void) sprintf(p2, "%.*s",
				(int) (&buf[sizeof buf - 1] - p2), file);
			if ((pid = tet_sp2(buf, argv, envp)) != -1 || errno != ENOENT)
				return(pid);
			p2 = buf;
		}
		else if (p2 < &buf[sizeof buf - 2])
			*p2++ = *p1;
	} while (*p1++);

	/*
	** here if an executable file with any of the known extensions
	** cannot be found anywhere on the search path
	*/
	return(-1);
}

/*
**	tet_sp2() - entend the tet_spawnvpe() processing
**
**	return the pid of the new process if successful or -1 on error
*/

static int tet_sp2(file, argv, envp)
char *file, **argv, **envp;
{
	register char *p;
	register struct extensions *ep;
	char buf[MAXPATH];
	int pid;

	/* attempt to spawn a file name with an extension straight off */
	for (p = tet_basename(file); *p; p++)
		if (*p == '.')
			return(tet_sp3(file, argv, envp));

	/* see if the file name can be executed as is */
	if ((pid = tet_sp3(file, argv, envp)) != -1 || errno != ENOENT)
		return(pid);

	/* finally, try appending various known extensions to the file name */
	for (ep = extensions; ep < &extensions[Nextensions]; ep++) {
		(void) sprintf(buf, "%.*s%s",
			(int) sizeof buf - 5, file, ep->ex_ext);
		if ((pid = tet_sp3(buf, argv, envp)) != -1 || errno != ENOENT)
			break;
	}

	return(pid);
}

/*
**	tet_sp3() - entend the tet_spawnvpe() processing some more
**
**	return the pid of the new process if successful or -1 on error
*/

static int tet_sp3(file, argv, envp)
char *file, **argv, **envp;
{
	register char *p1, *p2;
	register struct extensions *ep;
	char *extp;
	char buf[MAXPATH];

	/* invoke an interpreter when one is indicated by an extension */
	if ((extp = strrchr(tet_basename(file), '.')) != (char *) 0)
		for (ep = extensions; ep < &extensions[Nextensions]; ep++)
			if (ep->ex_interp && !_stricmp(extp, ep->ex_ext))
				return(tet_sp4(ep->ex_interp, file, argv, envp));

	/*
	** here to attempt to spawn an executable file -
	** note that _spawnve() understands .com, .exe, .bat and .cmd
	** and will supply these extensions if necessary
	*/

	/* convert all the '/' characters in the file name to '\' characters */
	for (p1 = file, p2 = buf; *p1 && p2 < &buf[sizeof buf - 1]; p1++, p2++)
		switch (*p1) {
		case '/':
			*p2 = '\\';
			break;
		default:
			*p2 = *p1;
			break;
		}
	*p2 = '\0';

	/* finally, attempt the spawn and return */
	return(_spawnve(_P_NOWAIT, buf, argv, envp));
}

/*
**	tet_sp4() - entend the tet_spawnvpe() processing when an
**		interpreter must be invoked to execute the file
**
**	return the pid of the new process if successful or -1 on error
*/

static int tet_sp4(interp, file, argv, envp)
char *interp, *file, **argv, **envp;
{
	char *arg0[2], **argvp;
	int rc;

	/* ensure that the file exists and is readable */
	if (_access(file, 04) < 0)
		return(-1);

	/*
	** prepend the interpreter name to the argv and pass the full
	** pathname of the file to execute as argv[1] to the interpreter
	*/
	arg0[0] = interp;
	arg0[1] = (char *) 0;
	if ((argvp = tet_addargv(arg0, argv)) == (char **) 0) {
		errno = ENOMEM;
		return(-1);
	}
	*(argvp + 1) = file;

	/* spawn the interpreter */
	rc = tet_spawnvpe(interp, argvp, envp);

	/* free storage allocated here */
	TRACE2(tet_Tbuf, 6, "free interpreter argv = %s", tet_i2a(argvp));
	free((char *) argvp);

	return(rc);
}

#else		/* -END-WIN32-CUT- */

int tet_tetspawn_c_not_used;

#endif		/* -WIN32-CUT-LINE- */

