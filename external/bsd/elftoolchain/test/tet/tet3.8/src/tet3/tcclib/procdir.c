/*
 *	SCCS: @(#)procdir.c	1.6 (03/03/26)
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
static char sccsid[] = "@(#)procdir.c	1.6 (03/03/26) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)procdir.c	1.6 03/03/26 TETware release 3.8
NAME:		procdir.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	October 1996

DESCRIPTION:
	tcc action function - perform a SAVE FILES operation

	this function moved from tccd/tsfile.c to here

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., June 1997
	pmatch() moved from dtet2lib/fcopy.c to here

	Andrew Dingwall, UniSoft Ltd., March 1998
	Corrected memory leaks in tcf_procdir() and copydir().
	Avoid passing a -ve precision value to sprintf().

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.

	Andrew Dingwall, The Open Group, March 2003
	Moved pmatch() to a separate file in dtet2lib and renamed
	it to tet_pmatch().
	Added support for binary file transfer mode.
 
************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <time.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <io.h>
#else		/* -END-WIN32-CUT- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtmsg.h"
#include "error.h"
#include "globals.h"
#include "ftype.h"
#include "ltoa.h"
#include "ptab.h"
#include "servlib.h"
#include "dtetlib.h"
#include "tcclib.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* static function declarations */
static int copydir PROTOLIST((char *, char *, int));
static int copyfile PROTOLIST((char *, char *, struct STAT_ST *, int));
static int procfile PROTOLIST((char *, char *, char *, char *[], int, int));
static int tsave PROTOLIST((char *, char *, int));
static int tsc2 PROTOLIST((int, char *, char *));
static int tscopy PROTOLIST((char *, char *));


/*
**	tcf_procdir() - search a directory for save files and
**		process them
**
**	return ER_OK if successful or other ER_* error code on error
*/

int tcf_procdir(fromdir, todir, sfiles, nsfile, flag)
char *fromdir, *todir, *sfiles[];
int nsfile, flag;
{
	register char **fip, **fromfiles;
	register int rc, rctmp;

	TRACE3(Ttcclib, 8, "procdir(): fromdir = \"%s\", todir = \"%s\"",
		fromdir, todir);

	/* get a directory list */
	if ((fromfiles = tet_lsdir(fromdir)) == (char **) 0)
		return(ER_ERR);

	/* process each entry in turn */
	rc = ER_OK;
	for (fip = fromfiles; *fip; fip++) {
		if ((rctmp = procfile(fromdir, todir, *fip, sfiles, nsfile, flag)) != ER_OK)
			rc = rctmp;
		TRACE2(tet_Tbuf, 6, "free file name = %s", tet_i2x(*fip));
		free(*fip);
	}

	TRACE2(tet_Tbuf, 6, "free file list = %s", tet_i2x(fromfiles));
	free((char *) fromfiles);
	return(rc);
}

/*
**	procfile() - see if this file name is in the list of save files
**		and save it if it is
**
**	return ER_OK if successful or other ER_* error code on error
*/

static int procfile(fromdir, todir, fromfile, sfiles, nsfile, flag)
register char *fromdir, *fromfile;
char *todir;
register char *sfiles[];
register int nsfile;
int flag;
{
	register int n;
	struct STAT_ST stbuf;
	int len;
	char path[MAXPATH + 1];

	TRACE3(Ttcclib, 8, "procfile(): fromdir = \"%s\", fromfile = \"%s\"",
		fromdir, fromfile);

	/* strip off an initial "./" from dir */
	if (*fromdir == '.' && isdirsep(*(fromdir + 1)))
		fromdir += 2;

	/* construct the path name to this file */
	if (!*fromdir || (*fromdir == '.' && !*(fromdir + 1)))
		(void) sprintf(path, "%.*s", (int) sizeof path - 1, fromfile);
	else {
		len = (int) sizeof path - (int) strlen(fromdir) - 2;
		(void) sprintf(path, "%.*s/%.*s",
			(int) sizeof path - 2, fromdir,
			TET_MAX(len, 0), fromfile);
	}

	TRACE2(Ttcclib, 9, "procfile(): path name = \"%s\"", path);

	/* get the file stats */
	if (STAT(path, &stbuf) < 0) {
		error(errno, "warning: can't stat", path);
		return(ER_OK);
	}

	/* see if the file is in the save files list */
	for (n = 0; n < nsfile; n++)
		if (tet_pmatch(fromfile, sfiles[n]))
			return(copyfile(path, todir, &stbuf, flag));

	/* not in the list - if the file is a directory, search that as well */
	if (S_ISDIR(stbuf.st_mode) && !(*fromfile == '.' && !*(fromfile + 1)))
		return(tcf_procdir(path, todir, sfiles, nsfile, flag));

	return(ER_OK);
}

/*
**	copyfile() - process a transfer save file (or directory)
**
**	return ER_OK if successful or other ER_* error code on error
*/

#ifdef S_IFMT
#define FILE_TYPE	S_IFMT
#else
#define FILE_TYPE	~(S_IRWXU | S_IRWXG | S_IRWXO)
#endif

static int copyfile(fromfile, todir, stp, flag)
char *fromfile, *todir;
struct STAT_ST *stp;
int flag;
{
	TRACE2(Ttcclib, 8, "copyfile(): fromfile = \"%s\"", fromfile);

	/* see if it is a file or a directory and process it accordingly */
	if (S_ISDIR(stp->st_mode))
		return(copydir(fromfile, todir, flag));
	else if (S_ISREG(stp->st_mode))
		return(tsave(fromfile, todir, flag));
	else {
		error(0, "ignored save file", fromfile);
		error(0, "type", tet_i2o(stp->st_mode & FILE_TYPE));
		return(ER_OK);
	}
}

/*
**	copydir() - process a transfer save file that is a directory
**
**	return ER_OK if successful or other ER_* error code on error
*/

static int copydir(fromdir, todir, flag)
char *fromdir, *todir;
int flag;
{
	register char **fip, **fromfiles;
	register int rc, rctmp;
	struct STAT_ST stbuf;
	int len;
	char path[MAXPATH + 1];

	TRACE2(Ttcclib, 8, "copydir(): fromdir = \"%s\"", fromdir);

	/* get a directory list */
	if ((fromfiles = tet_lsdir(fromdir)) == (char **) 0)
		return(ER_ERR);

	/* process each file name in turn */
	rc = ER_OK;
	len = (int) sizeof path - (int) strlen(fromdir) - 2;
	for (fip = fromfiles; *fip; fip++) {
		(void) sprintf(path, "%.*s/%.*s",
			(int) sizeof path - 2, fromdir,
			TET_MAX(len, 0), *fip);
		if (STAT(path, &stbuf) < 0) {
			error(errno, "warning: can't stat", path);
			continue;
		}
		if ((rctmp = copyfile(path, todir, &stbuf, flag)) != ER_OK)
			rc = rctmp;
		TRACE2(tet_Tbuf, 6, "free tsfile name = %s", tet_i2x(*fip));
		free(*fip);
	}

	TRACE2(tet_Tbuf, 6, "free tsfile list = %s", tet_i2x(fromfiles));
	free((char *) fromfiles);
	return(rc);
}

/*
**	tsave() - process a single transfer save file
**
**	return ER_OK if successful or other ER_* error code on error
*/

static int tsave(fromfile, todir, flag)
char *fromfile, *todir;
int flag;
{
#ifndef TET_LITE	/* -START-LITE-CUT- */
	register char *p;
	char tofile[MAXPATH + 1];
	int binflag;
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	TRACE3(Ttcclib, 8, "tsave(): fromfile = \"%s\", flag = %s",
		fromfile, tet_i2a(flag));

	switch (flag) {
	case TCF_TS_LOCAL:
		return(tscopy(fromfile, todir));
#ifndef TET_LITE	/* -START-LITE-CUT- */
	case TCF_TS_MASTER:
		ASSERT(tet_xdptab);
		if ((tet_xdptab->pt_flags & PF_LOGGEDON) == 0 &&
			tet_xdlogon() < 0)
				return(ER_ERR);
		p = tofile;
		if (todir) {
			(void) sprintf(tofile, "%.*s/",
				(int) sizeof tofile - 2, todir);
			p += strlen(tofile);
		}
		(void) sprintf(p, "%.*s",
			(int) sizeof tofile - (int) (p - tofile) - 1, fromfile);
		binflag = (tet_getftype(fromfile) == TET_FT_BINARY) ? 1 : 0;
		TRACE4(Ttcclib, 8,
		"call xd_rxfile: from = \"%s\", to = \"%s\", binflag = %s",
			fromfile, tofile, tet_i2a(binflag));
		if (tet_xdxfile(fromfile, tofile, binflag) < 0) {
			error(0, "transfer save file failed, rc =",
				tet_ptrepcode(tet_xderrno));
			return(ER_ERR);
		}
		return(ER_OK);
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
	default:
		error(0, "unexpected flag value:", tet_i2a(flag));
		return(ER_ERR);
	}
}

/*
**	tscopy() - copy a save file on the local system
**
**	return ER_OK if successful or other ER_* error code on error
*/

static int tscopy(fromfile, todir)
char *fromfile, *todir;
{
	register char *p;
	register int ifd, rc;
	int len;
	char tofile[MAXPATH + 1];

	/* construct the destination file name */
	len = (int) sizeof tofile - (int) strlen(todir) - 2;
	(void) sprintf(tofile, "%.*s/%.*s",
		(int) sizeof tofile - 2, todir, TET_MAX(len, 0), fromfile);

	TRACE3(Ttcclib, 8, "tscopy(): fromfile = \"%s\", tofile = \"%s\"",
		fromfile, tofile);

	/* make any required directories for the destination file */
	for (p = tofile + strlen(tofile) - 1; p >= tofile; p--)
		if (isdirsep(*p))
			break;
	if (p > tofile) {
		*p = '\0';
		TRACE2(Ttcclib, 9, "tscopy(): about to call tet_mkalldirs(%s)",
			tofile);
		if (tet_mkalldirs(tofile) < 0)
			return(ER_ERR);
		*p = '/';
	}

	TRACE3(Ttcclib, 9, "tscopy(): about to copy \"%s\" to \"%s\"",
		fromfile, tofile);

	/* open the source file, copy it if successful */
	if ((ifd = OPEN(fromfile, O_RDONLY | O_BINARY, 0)) < 0) {
		error(errno, "can't open", fromfile);
		rc = ER_ERR;
	}
	else {
		rc = tsc2(ifd, fromfile, tofile);
		(void) CLOSE(ifd);
	}

	TRACE2(Ttcclib, 9, "tscopy() return %s", tet_ptrepcode(rc));
	return(rc);
}

/*
**	tsc2() - extend the tscopy() processing
**
**	return ER_OK if successful or other ER_* error code on error
*/

#define MODEMASK	(S_IRWXU | S_IRWXG | S_IRWXO)
#define MODEANY		((mode_t) MODEMASK)

static int tsc2(ifd, fromfile, tofile)
int ifd;
char *fromfile, *tofile;
{
	register int n, ofd, rc;
	char buf[BUFSIZ];
	struct STAT_ST stbuf;

	/* open the destination file */
	if ((ofd = OPEN(tofile, O_WRONLY | O_CREAT | O_TRUNC | O_BINARY, MODEANY)) < 0) {
		error(errno, "can't open", tofile);
		return(ER_ERR);
	}

	/* copy the file */
	rc = ER_OK;
	while ((n = READ(ifd, buf, sizeof buf)) > 0)
		if (WRITE(ofd, buf, (unsigned) n) != n) {
			error(errno, "write error on", tofile);
			rc = ER_ERR;
			break;
		}
	if (n < 0) {
		error(errno, "read error on", fromfile);
		rc = ER_ERR;
	}

	/* set the mode on the destination file */
	if (FSTAT(ifd, &stbuf) < 0)
		error(errno, "warning: can't stat", fromfile);
	else if (CHMOD(tofile, (mode_t) (stbuf.st_mode & MODEMASK)) < 0)
		error(errno, "warning: can't chmod", tofile);

	(void) CLOSE(ofd);
	return(rc);
}

