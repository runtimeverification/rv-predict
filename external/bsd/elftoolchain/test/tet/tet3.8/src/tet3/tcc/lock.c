/*
 *	SCCS: @(#)lock.c	1.5 (98/09/01)
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
static char sccsid[] = "@(#)lock.c	1.5 (98/09/01) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)lock.c	1.5 98/09/01 TETware release 3.8
NAME:		lock.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	testcase locking functions

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#include <stdio.h>
#include <sys/types.h>
#include <time.h>
#include <errno.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "error.h"
#include "globals.h"
#include "ltoa.h"
#include "servlib.h"
#include "scentab.h"
#include "proctab.h"
#include "tcc.h"
#include "tcclib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tcc_lock() - acquire a shared lock or an exclusive lock
**
**	return 0 if successful or -1 on error
**
**	if successful, the name of the created lock is returned in lkname[]
**
**	failure to acquire a lock on a read-only file system is not
**	treated as an error; in this case an empty string is returned
*/

int tcc_lock(prp, shared, dir, lkname, lknamelen)
struct proctab *prp;
int shared, lknamelen;
char *dir, lkname[];
{
	static char fmt[] = "can't acquire %s lock";
	char msg[sizeof fmt + 9];
	char lkpath[MAXPATH];
	char *lktype, *lnp;
	int err, rc;

	fullpath(dir, "tet_lock", lkpath, sizeof lkpath, *prp->pr_sys ? 1 : 0);
	errno = 0;
	if (shared) {
		lktype = "shared";
#ifdef TET_LITE	/* -LITE-CUT-LINE- */
		if ((tet_tcerrno = tcf_sharelock(lkpath, (long) tet_mypid, tcc_timeout, &lnp)) != ER_OK)
			lnp = (char *) 0;
#else	/* -START-LITE-CUT- */
		lnp = tet_tcsharelock(*prp->pr_sys, lkpath, tcc_timeout);
#endif /* TET_LITE */	/* -END-LITE-CUT- */
	}
	else {
		lktype = "exclusive";
#ifdef TET_LITE	/* -LITE-CUT-LINE- */
		tet_tcerrno = tcf_lockfile(lkpath, tcc_timeout);
		rc = (tet_tcerrno == ER_OK) ? 0 : -1;
#else	/* -START-LITE-CUT- */
		rc = tet_tclockfile(*prp->pr_sys, lkpath, tcc_timeout);
#endif /* TET_LITE */	/* -END-LITE-CUT- */
		lnp = (rc < 0) ? (char *) 0 : lkpath;
	}

	/* handle an error return */
	if (!lnp) {
		if (!IS_ER_ERRNO(tet_tcerrno))
			errno = 0;
		switch (errno) {
		case EROFS:
			lkname[0] = '\0';
			return(0);
		case ENOTDIR:
			err = 0;
			break;
		default:
			err = errno ? errno : tet_tcerrno;
			break;
		}
		(void) sprintf(msg, fmt, lktype);
		prperror(prp, *prp->pr_sys, err, msg, lkpath);
		return(-1);
	}

	TRACE4(tet_Ttcc, 4, "created %s lock %s on system %s",
		lktype, lnp, tet_i2a(*prp->pr_sys));

	(void) sprintf(lkname, "%.*s", lknamelen, lnp);
	return(0);
}

/*
**	tcc_unlock() - remove a lock
**
**	return 0 if successful or -1 on error
*/

int tcc_unlock(prp, shared, lkname)
struct proctab *prp;
int shared;
char *lkname;
{
	static char fmt[] = "can't remove %s lock%s";
	char msg[sizeof fmt + 20];
	char *lktype;
	char lkdir[MAXPATH];
	int rc = 0;

	if (shared)
		lktype = "shared";
	else
		lktype = "exclusive";

	/* remove the lock file */
	errno = 0;
	if (tcc_unlink(*prp->pr_sys, lkname) < 0) {
		(void) sprintf(msg, fmt, lktype, "");
		prperror(prp, *prp->pr_sys, errno ? errno : tet_tcerrno,
			msg, lkname);
		rc = -1;
	}
	else {
		TRACE4(tet_Ttcc, 4, "removed %s lock %s on system %s",
			lktype, lkname, tet_i2a(*prp->pr_sys));
	}

	/* return now if the lock was an exclusive lock */
	if (!shared)
		return(rc);

	/*
	** for a shared lock, attempt to remove the lock directory as well;
	** it is OK for this call to fail because the directory is not empty
	** or because someone else has removed it before we had a chance to
	*/
	tcc_dirname(lkname, lkdir, sizeof lkdir);
	errno = 0;
	if (tcc_rmdir(*prp->pr_sys, lkdir) < 0)
		switch (errno) {
		case EEXIST:
		case ENOENT:
#ifdef ENOTEMPTY	/* band-aid for non-POSIX systems */
		case ENOTEMPTY:
#endif
			break;
		default:
			(void) sprintf(msg, fmt, lktype, " directory");
			prperror(prp, *prp->pr_sys, errno ? errno : tet_tcerrno,
				msg, lkdir);
			rc = -1;
		}
	else {
		TRACE4(tet_Ttcc, 4, "removed %s lock directory %s on system %s",
			lktype, lkdir, tet_i2a(*prp->pr_sys));
	}

	return(rc);
}

