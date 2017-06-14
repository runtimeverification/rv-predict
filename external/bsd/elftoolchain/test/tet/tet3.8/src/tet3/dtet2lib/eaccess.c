/*
 *      SCCS:  @(#)eaccess.c	1.13 (05/07/08) 
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
static char sccsid[] = "@(#)eaccess.c	1.13 (05/07/08) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)eaccess.c	1.13 05/07/08 TETware release 3.8
NAME:		eaccess.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	June 1992

DESCRIPTION:
	function to check access permissions wrt effective user and group IDs

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., January 1994
	use S_ISDIR instead of S_IFMT for strict posix conformance

	Geoff Clare, UniSoft Ltd., August 1996
	Missing <unistd.h>.

	Andrew Dingwall, UniSoft Ltd., March 1999
	On UNIX systems, check group permissions w.r.t. supplementary
	group IDs as well as against the egid.

	Geoff Clare, The Open Group, July 2005
	Allocate buffer for NGROUPS_MAX+1 groups.
	Avoid non-portable mode value assumptions.

************************************************************************/

#include <stdio.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <io.h>
#else		/* -END-WIN32-CUT- */
#  include <limits.h>
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "error.h"
#include "ltoa.h"
#include "dtetlib.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* static function declarations */
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
static int check_grouplist PROTOLIST((struct STAT_ST *, int));
#endif		/* -WIN32-CUT-LINE- */


/*
**	tet_eaccess() - like access() but checks permissions wrt
**		effective user and group IDs
**
**	Note: this routine uses the traditional encoding for its
**	mode argument, ie: 4 = read, 2 = write, 1 = exec
**	because it may have been sent from a remote system
**	where R_OK etc. could have different values.
**	(The translation to a native access mode would probably be
**	better done in tet_tcaccess(), but is done here instead for
**	historical reasons.)
*/

int tet_eaccess(path, mode)
char *path;
register int mode;
{
	struct STAT_ST stbuf;

#ifdef _WIN32	/* -START-WIN32-CUT- */

	/* check that the file exists and is readable and/or writable */
	if (ACCESS(path, mode & 06) < 0)
		return(-1);

	/* perform an execute check if so required */
	if (mode & 01) {
		if (STAT(path, &stbuf) < 0)
			return(-1);
		mode &= 07;
		if (((stbuf.st_mode >> 6) & mode) != mode) {
			errno = EACCES;
			return(-1);
		}
	}

	return(0);

#else /* _WIN32 */	/* -END-WIN32-CUT- */

	int amode;
	uid_t euid;
	int rc, rc2;

	/*
	** translate the traditional access mode to a native "amode"
	*/
	if (mode == 0)
		amode = F_OK;
	else {
		amode = 0;
		if (mode & 01)
			amode |= X_OK;
		if (mode & 02)
			amode |= W_OK;
		if (mode & 04)
			amode |= R_OK;
	}

	/*
	** first check for things like non-existent file,
	** read-only file system etc.
	*/
	if (ACCESS(path, amode) < 0) {
		if (errno != EACCES)
			return(-1);
	}
	else
		if (amode == F_OK)
			return(0);

	/*
	** here if access() succeeded, or failed because of wrong permissions;
	** first get the file permissions
	*/
	if (STAT(path, &stbuf) < 0)
		return(-1);

	/*
	** check the permissions wrt the euid, the egid and the
	** supplementary groups list;
	** treating root specially (like the kernel does)
	*/
	rc = 0;
	if ((euid = geteuid()) == 0) {
		if (!S_ISDIR(stbuf.st_mode) &&
		    (amode & X_OK) != 0 &&
		    (stbuf.st_mode & (S_IXUSR|S_IXGRP|S_IXOTH)) == 0)
			rc = -1;
	}
	else if (stbuf.st_uid == euid) {
		if ((amode & R_OK) != 0 && (stbuf.st_mode & S_IRUSR) == 0 ||
		    (amode & W_OK) != 0 && (stbuf.st_mode & S_IWUSR) == 0 ||
		    (amode & X_OK) != 0 && (stbuf.st_mode & S_IXUSR) == 0)
			rc = -1;
	}
	else if (stbuf.st_gid == getegid()) {
		if ((amode & R_OK) != 0 && (stbuf.st_mode & S_IRGRP) == 0 ||
		    (amode & W_OK) != 0 && (stbuf.st_mode & S_IWGRP) == 0 ||
		    (amode & X_OK) != 0 && (stbuf.st_mode & S_IXGRP) == 0)
			rc = -1;
	}
	else {
		rc2 = check_grouplist(&stbuf, amode);
		switch (rc2) {
		case 2:
			break;
		case 1:
			rc = -1;
			break;
		case 0:
			if ((amode & R_OK) != 0 &&
				(stbuf.st_mode & S_IROTH) == 0 ||
			    (amode & W_OK) != 0 &&
				(stbuf.st_mode & S_IWOTH) == 0 ||
			    (amode & X_OK) != 0 &&
				(stbuf.st_mode & S_IXOTH) == 0)
				rc = -1;
			break;
		case -1:
			return(-1);
		default:
			/* "can't happen" */
			fatal(0, "check_grouplist() returned unexpected value",
				tet_i2a(rc2));
			/* NOTREACHED */
			return(-1);
		}
	}

	if (rc < 0)
		errno = EACCES;
	return(rc);

#endif /* !_WIN32 */	/* -WIN32-CUT-LINE- */

}


#ifndef _WIN32		/* -WIN32-CUT-LINE- */

/*
**	check_grouplist() - check the requested access mode against
**		the process's supplementary grouplist
**
**	return	 2 if a supplementary group matched and group access is allowed
**		 1 if a supplementary group matched but group access is
**		   not allowed
**		 0 if no supplementary groups matched
**		-1 on error (with errno set)
*/

static int check_grouplist(stp, amode)
struct STAT_ST *stp;
int amode;
{
	int errsave, ngids, ngmax;
	gid_t *gidp;
	static gid_t *gids = (gid_t *) 0;
	static int lgids = 0;

	/*
	** allocate a buffer to hold the supplementary group list;
	** we only evaluate NGROUPS_MAX once because on some systems it
	** can be a call to sysconf()
	*/
	ngmax = (int) NGROUPS_MAX;
	if (ngmax <= 0)
		return 0;

	/* in SUSv3/POSIX.1-2001 getgroups() can return NGROUPS_MAX+1 groups */
	ngmax++;

	if (BUFCHK((char **) &gids, &lgids, ngmax * (int) sizeof *gidp) < 0) {
		errno = ENOMEM;
		return(-1);
	}

	/*
	** get the supplementary group list from the kernel;
	** it probably won't change from one invocation of tet_eaccess() to
	** the next, but we get it on each call just to be on the safe side
	**/
	if ((ngids = getgroups(ngmax, gids)) < 0) {
		errsave = errno;
		error(errno, "can't get supplementary group list", (char *) 0);
		errno = errsave;
		return(-1);
	}

	/*
	** check the file's group id against each supplementary group;
	** if the groups match, see if the requested access permission(s)
	** will be granted
	*/
	for (gidp = gids; gidp < gids + ngids; gidp++)
		if (stp->st_gid == *gidp) {
			if ((amode & R_OK) != 0 &&
				(stp->st_mode & S_IRGRP) == 0 ||
			    (amode & W_OK) != 0 &&
				(stp->st_mode & S_IWGRP) == 0 ||
			    (amode & X_OK) != 0 &&
				(stp->st_mode & S_IXGRP) == 0)
				return 1;
			else
				return 2;
		}

	return 0;
}

#endif /* !_WIN32 */	/* -WIN32-CUT-LINE- */

