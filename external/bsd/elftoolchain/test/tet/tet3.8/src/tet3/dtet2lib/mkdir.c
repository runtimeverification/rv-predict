/*
 *      SCCS:  @(#)mkdir.c	1.11 (98/08/28) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1992 X/Open Company Limited
 *
 * All rights reserved.  No part of this source code may be reproduced,
 * stored in a retrieval system, or transmitted, in any form or by any
 * means, electronic, mechanical, photocopying, recording or otherwise,
 * except as stated in the end-user licence agreement, without the prior
 * permission of the copyright owners.
 *
 * X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
 * the UK and other countries.
 */

#ifndef lint
static char sccsid[] = "@(#)mkdir.c	1.11 (98/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)mkdir.c	1.11 98/08/28 TETware release 3.8
NAME:		mkdir.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	functions to create and remove a directory

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., May 1997
	port to Windows 95

	Andrew Dingwall, UniSoft Ltd., July 1998
	Extended the Windows 95 errno band-aid code to Windows NT as well.
 

************************************************************************/

#include <stdio.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include "dtmac.h"
#include "dtetlib.h"

#ifdef NOMKDIR
#  include "error.h"
#  ifdef NEEDsrcFile
     static char srcFile[] = __FILE__;	/* file name for error reporting */
#  endif /* NEEDsrcFile */
#else
#  ifdef _WIN32	/* -START-WIN32-CUT- */
#    include <direct.h>
#    include <string.h>
#    include "tetdir.h"
#    define MKDIR(A, B)		_mkdir(A)
#    define RMDIR(A)		_rmdir(A)
#  else		/* -END-WIN32-CUT- */
#    include <unistd.h>
#    define MKDIR(A, B)		mkdir((A), (mode_t) (B))
#    define RMDIR(A)		rmdir(A)
#  endif	/* -WIN32-CUT-LINE- */
#endif /* NOMKDIR */

/* extern function declarations */
#ifdef NOMKDIR
#  ifdef LOCAL_FUNCTION_DECL
     extern void _exit();
#  endif
#endif

/* static function declarations */
#ifdef NOMKDIR
   static int hardmkrmdir PROTOLIST((char *, char *, int));
#else
#  ifdef _WIN32	/* -START-WIN32-CUT- */
     static int ised2 PROTOLIST((char *));
     static int isemptydir PROTOLIST((char *));
#  endif	/* -END-WIN32-CUT- */
#endif /* NOMKDIR */

/*
**	tet_mkdir() - create a directory
**
**	return 0 if successful, -1 otherwise
*/

int tet_mkdir(path, mode)
char *path;
int mode;
{
	register int rc;

#ifdef NOMKDIR

	register char *p;
	register int n;
	struct stat stbuf;
	char dir[MAXPATH + 1];

	/* see if the directory exists already */
	if (stat(path, &stbuf) == 0) {
		errno = EEXIST;
		return(-1);
	}

	/* see if a mkdir is likely to succeed */
	for (p = path + strlen(path) - 1; p >= path; p--)
		if (*p == '/')
			break;
	if (p > path) {
		n = p - path;
		(void) sprintf(dir, "%.*s",
			TET_MIN(n, (int) sizeof dir - 1), path);
		if (stat(dir, &stbuf) < 0)
			return(-1);
		if ((stbuf.st_mode & S_IFMT) != S_IFDIR) {
			errno = ENOTDIR;
			return(-1);
		}
		if (tet_eaccess(dir, 02) < 0)
			return(-1);
	}

	/* we must make it the hard way */
	if ((rc = hardmkrmdir("mkdir", path, mode)) < 0)
		if (rc == -1)
			errno = EEXIST;
		else
			rc = -1;

#else /* NOMKDIR */

	/* easy - we have mkdir(2) */
	rc = MKDIR(path, mode);

#endif /* NOMKDIR */

	return(rc);
}

/*
**	tet_rmdir() - remove a directory
**
**	return 0 if successful, -1 otherwise
*/

int tet_rmdir(path)
char *path;
{
	register int rc;

#ifdef NOMKDIR

	register char *p;
	register int n;
	struct stat stbuf;
	char dir[MAXPATH + 1];

	/* see if the path exists and is a directory */
	if (stat(path, &stbuf) < 0)
		return(-1);
	if ((stbuf.st_mode & S_IFMT) != S_IFDIR) {
		errno = ENOTDIR;
		return(-1);
	}

	/* see if a rmdir is likely to succeed */
	for (p = path + strlen(path) - 1; p >= path; p--)
		if (*p == '/')
			break;
	if (p > path) {
		n = p - path;
		(void) sprintf(dir, "%.*s",
			TET_MIN(n, (int) sizeof dir - 1), path);
		if (tet_eaccess(dir, 02) < 0)
			return(-1);
	}

	/* we must remove it the hard way */
	if ((rc = hardmkrmdir("rmdir", path, 0777)) < 0)
		if (rc == -1)
			errno = ENOENT;
		else
			rc = -1;

#else /* NOMKDIR */

	/* easy - we have rmdir(2) */
	rc = RMDIR(path);

	/* band-aid for non-POSIX systems */
#  ifdef ENOTEMPTY
	if (rc < 0 && errno == ENOTEMPTY)
		errno = EEXIST;
#  endif /* ENOTEMPTY */

	/* band-aid for Windows 95 (and Windows NT using SMB) */
#  ifdef _WIN32	/* -START-WIN32-CUT- */
	if (rc < 0 && errno == EACCES && !isemptydir(path))
		errno = EEXIST;
#  endif	/* -END-WIN32-CUT- */

#endif /* NOMKDIR */

	return(rc);
}


#ifndef NOMKDIR
#  ifdef _WIN32	/* -START-WIN32-CUT- */

/*
**	isemptydir() - see if a directory is empty
**
**	return 1 if it is, 0 if it isn't or -1 on error
**
**	we need this function because rmdir() returns EACCES
**	instead of ENOTEMPTY on Windows 95
*/

static int isemptydir(path)
char *path;
{
	int errsave, rc;

	errsave = errno;
	rc = ised2(path);
	errno = errsave;

	return(rc);
}

static int ised2(path)
char *path;
{
	DIR *dirp;
	struct dirent *dp;
	int rc;

	if ((dirp = OPENDIR(path)) == (DIR *) 0)
		return(-1);

	rc = 1;
	while ((dp = READDIR(dirp)) != (struct dirent *) 0)
		if (strcmp(dp->d_name, ".") && strcmp(dp->d_name, "..")) {
			rc = 0;
			break;
		}

	(void) CLOSEDIR(dirp);
	return(rc);
}

#  endif /* _WIN32 */	/* -END-WIN32-CUT- */
#endif /* !NOMKDIR */



#ifdef NOMKDIR

/*
**	hardmkrmdir() - fork and exec mkdir or rmdir
**
**	return   0 if the program succeeded
**		-1 if it failed
**		-2 for some other error
*/

static int hardmkrmdir(prog, path, mode)
char *prog, *path;
int mode;
{
	char *argv[3];
	register int pid, rc;
	int status, save_errno;
	char msg[32];

	switch (pid = tet_dofork()) {
	case 0:
		argv[0] = prog;
		argv[1] = path;
		argv[2] = (char *) 0;
		(void) umask(~(mode & ~umask(0)) & 077);
		(void) execvp(prog, argv);
		(void) sprintf(msg, "can't exec: %s", prog);
		error(errno, msg, path);
		_exit(~0);
		/* NOTREACHED */
	case -1:
		save_errno = errno;
		(void) sprintf(msg, "can't fork: %s", prog);
		error(errno, msg, path);
		errno = save_errno;
		return(-2);
	default:
		status = 0;
		while ((rc = wait(&status)) > 0)
			if (rc == pid)
				break;
		if (rc < 0) {
			save_errno = errno;
			(void) sprintf(msg, "wait failed: %s", prog);
			error(errno, prog, msg);
			errno = save_errno;
			return(-2);
		}
		if (status & 0xffff) {
			if ((status & 0xff00) != 0xff00) {
				(void) sprintf(msg, "%s failed:", prog);
				error(0, msg, path);
			}
			return(-1);
		}
		break;
	}

	return(0);
}

#endif /* NOMKDIR */

