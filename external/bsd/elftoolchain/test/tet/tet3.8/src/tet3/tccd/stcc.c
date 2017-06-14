/*
 *      SCCS:  @(#)stcc.c	1.17 (03/03/26) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1992 X/Open Company Limited
 * (C) Copyright 1994 UniSoft Limited
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
static char sccsid[] = "@(#)stcc.c	1.17 (03/03/26) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)stcc.c	1.17 03/03/26 TETware release 3.8
NAME:		stcc.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	May 1992

DESCRIPTION:
	MTCC request processing routines

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, August 1993
	added op_rcopy() routine.

	Denis McConalogue, UniSoft Limited, September 1993
	don't use errno after tet_fcopy().		

	Andrew Dingwall, UniSoft Ltd., November 1993
	enhancements for FIFO transport interface

	Andrew Dingwall, UniSoft Ltd., August 1996
	changes for TETware

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
	Andrew Dingwall, The Open Group, March 2003
	Added support for OP_UTIME and OP_FTIME.
 

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <time.h>
#include <errno.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <direct.h>
#  include <sys/utime.h>
#else		/* -END-WIN32-CUT- */
#  include <unistd.h>
#  include <utime.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "error.h"
#include "ltoa.h"
#include "avmsg.h"
#include "valmsg.h"
#include "servlib.h"
#include "dtetlib.h"
#include "tccd.h"
#include "tcclib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* mode for created directories */
#define DIRMODE	((mode_t) (S_IRWXU | S_IRWXG | S_IRWXO))


/*
**	op_putenv() - put strings in the environment
**
**	strings put in the environment by OP_PUTENV are stored in memory
**	obtained from malloc()
**	if an OP_PUTENV request overwrites an existing environment string that
**	is itself stored in malloc'd memory, there is no provision for freeing
**	that memory
**
**	this is not usually a problem, however, for the typical use of this
**	request to set up an environment to be passed to a STCM via exec()
*/

void op_putenv(pp)
struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;
	register char *p1, *p2;
	register int n;
	char buf[128];

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the environment strings */
	for (n = 0; n < OP_PUTENV_NLINE(mp); n++) {
		if ((p1 = AV_ENVAR(mp, n)) == (char *) 0 || !*p1) {
			pp->ptm_rc = ER_INVAL;
			return;
		}
		TRACE2(tet_Ttccd, 4, "receive env string = \"%s\"", p1);
		if (!tet_equindex(p1)) {
			pp->ptm_rc = ER_INVAL;
			return;
		}
	}

	/*
	** examine each string:
	**
	**	remember a value for TET_ROOT
	**
	**	if this variable is already in the environment with the
	**	same value, skip it;
	**	otherwise, put the string in the environment
	*/
	for (n = 0; n < OP_PUTENV_NLINE(mp); n++) {
		p1 = tet_equindex(AV_ENVAR(mp, n));
		ASSERT(p1);
		(void) sprintf(buf, "%.*s",
			TET_MIN((int) (p1 - AV_ENVAR(mp, n)), (int) sizeof buf - 1),
			AV_ENVAR(mp, n));
		if (!strcmp(buf, "TET_ROOT") && tetrootset(p1 + 1) < 0)
			break;
		if ((p2 = getenv(buf)) != (char *) 0 && !strcmp(p1 + 1, p2))
			continue;
		if ((p1 = tet_strstore(AV_ENVAR(mp, n))) == (char *) 0 ||
			tet_putenv(p1) < 0)
				break;
	}

	pp->ptm_rc = (n == OP_PUTENV_NLINE(mp)) ? ER_OK : ER_ERR;
}

/*
**	op_access() - determine the accessibility of a file wrt euid and egid
*/

void op_access(pp)
struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the request message */
	if (mp->av_argc != OP_ACCESS_ARGC || !AV_PATH(mp) || !*AV_PATH(mp)) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	TRACE2(tet_Ttccd, 4, "OP_ACCESS: path = \"%s\"", AV_PATH(mp));

	/* check the access permissions */
	if (tet_eaccess(AV_PATH(mp), (int) AV_MODE(mp)) < 0) {
		pp->ptm_rc = tet_maperrno(errno);
		return;
	}

	pp->ptm_rc = ER_OK;
}

/*
**	op_mkdir() - make a directory
*/

void op_mkdir(pp)
struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the request message */
	if (mp->av_argc != OP_DIR_ARGC || !AV_DIR(mp) || !*AV_DIR(mp)) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	TRACE2(tet_Ttccd, 4, "OP_MKDIR: dir = \"%s\"", AV_DIR(mp));

	/* make the directory */
	if (tet_mkdir(AV_DIR(mp), DIRMODE) < 0) {
		pp->ptm_rc = tet_maperrno(errno);
		return;
	}

	pp->ptm_rc = ER_OK;
}

/*
**	op_mkalldirs() - make directories recursively
*/

void op_mkalldirs(pp)
struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the request message */
	if (mp->av_argc != OP_DIR_ARGC || !AV_DIR(mp) || !*AV_DIR(mp)) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	TRACE2(tet_Ttccd, 4, "OP_MKALLDIRS: dir = \"%s\"", AV_DIR(mp));

	/* make the directory */
	if (tet_mkalldirs(AV_DIR(mp)) < 0) {
		pp->ptm_rc = tet_maperrno(errno);
		return;
	}

	pp->ptm_rc = ER_OK;
}

/*
**	op_rmdir() - remove a directory
*/

void op_rmdir(pp)
struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the request message */
	if (mp->av_argc != OP_DIR_ARGC || !AV_DIR(mp) || !*AV_DIR(mp)) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	TRACE2(tet_Ttccd, 4, "OP_RMDIR: dir = \"%s\"", AV_DIR(mp));

	/* remove the directory */
	if (tet_rmdir(AV_DIR(mp)) < 0) {
		pp->ptm_rc = tet_maperrno(errno);
		return;
	}

	pp->ptm_rc = ER_OK;
}

/*
**	op_rmalldirs() - remove a directory subtree
*/

void op_rmalldirs(pp)
struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the request message */
	if (mp->av_argc != OP_DIR_ARGC || !AV_DIR(mp) || !*AV_DIR(mp)) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	TRACE2(tet_Ttccd, 4, "OP_RMALLDIRS: dir = \"%s\"", AV_DIR(mp));

	/* remove the directory subtree */
	if (tcf_rmrf(AV_DIR(mp)) < 0) {
		pp->ptm_rc = tet_maperrno(errno);
		return;
	}

	pp->ptm_rc = ER_OK;
}

/*
**	op_rcopy() - recursively copy files
*/

void op_rcopy(pp)
struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the request message */
	if (mp->av_argc != OP_RCOPY_ARGC || !AV_XFROM(mp) || !*AV_XFROM(mp)
					 || !AV_XTO(mp)   || !*AV_XTO(mp))  {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	TRACE3(tet_Ttccd, 4, "OP_RCOPY: from = \"%s\", to = \"%s\"",
		AV_XFROM(mp), AV_XTO(mp));

	/* do the copy*/
	if (tet_fcopy(AV_XFROM(mp), AV_XTO(mp)) < 0) {
		pp->ptm_rc = ER_ERR;
		return;
	}

	pp->ptm_rc = ER_OK;
}
/*
**	op_chdir() - change directory
*/

void op_chdir(pp)
struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;
	register int errsave;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the request message */
	if (mp->av_argc != OP_DIR_ARGC || !AV_DIR(mp) || !*AV_DIR(mp)) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	/* change directory */
	TRACE2(tet_Ttccd, 4, "OP_CHDIR: chdir to \"%s\"", AV_DIR(mp));
	if (CHDIR(AV_DIR(mp)) < 0) {
		errsave = errno;
		if ((pp->ptm_rc = tet_maperrno(errsave)) == ER_ERR)
			error(errsave, "can't chdir to", AV_DIR(mp));
		return;
	}

	pp->ptm_rc = ER_OK;
}

/*
**	op_unlink() - unlink a file
*/

void op_unlink(pp)
struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;
	register int errsave;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the request message */
	if (mp->av_argc != OP_UNLINK_ARGC || !AV_FNAME(mp) || !*AV_FNAME(mp)) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	TRACE2(tet_Ttccd, 4, "OP_UNLINK: file = \"%s\"", AV_FNAME(mp));

	/* unlink the file */
	if (UNLINK(AV_FNAME(mp)) < 0) {
		errsave = errno;
		if ((pp->ptm_rc = tet_maperrno(errsave)) == ER_ERR)
			error(errsave, "can't unlink", AV_FNAME(mp));
		return;
	}

	pp->ptm_rc = ER_OK;
}

/*
**	op_utime() - set file access and mod times
*/

void op_utime(pp)
struct ptab *pp;
{
	struct avmsg *mp = (struct avmsg *) pp->ptm_data;
	struct UTIMBUF ftimes;
	int errsave;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the request message */
	if (
		mp->av_argc != OP_UTIME_ARGC ||
		!AV_FNAME(mp) ||
		!*AV_FNAME(mp) ||
		!AV_ATIME(mp) ||
		!AV_MTIME(mp)
	) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	TRACE4(tet_Ttccd, 4, "OP_UTIME: file = \"%s\", atime = %s, mtime = %s",
		AV_FNAME(mp), tet_l2a(AV_ATIME(mp)), tet_l2a(AV_MTIME(mp)));

	/* set the file times */
	ftimes.actime = AV_ATIME(mp);
	ftimes.modtime = AV_MTIME(mp);
	if (UTIME(AV_FNAME(mp), &ftimes) < 0) {
		errsave = errno;
		if ((pp->ptm_rc = tet_maperrno(errsave)) == ER_ERR)
			error(errsave, "can't set file times on", AV_FNAME(mp));
		return;
	}

	pp->ptm_rc = ER_OK;
}

/*
**	op_ftime() - get file access and mod times
*/

void op_ftime(pp)
struct ptab *pp;
{
	register char *dp = pp->ptm_data;
	struct STAT_ST stbuf;
	int errsave;

#define mp	((struct avmsg *) dp)

	/* all error replies have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the request message */
	if (
		mp->av_argc != OP_FTIME_ARGC ||
		!AV_FNAME(mp) ||
		!*AV_FNAME(mp)
	) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	TRACE2(tet_Ttccd, 4, "OP_FTIME: file = \"%s\"", AV_FNAME(mp));

	/* get the file times */
	if (STAT(AV_FNAME(mp), &stbuf) < 0) {
		errsave = errno;
		if ((pp->ptm_rc = tet_maperrno(errsave)) == ER_ERR)
			error(errsave, "can't stat", AV_FNAME(mp));
		return;
	}

#undef mp

#define rp	((struct valmsg *) dp)

	/* make sure that the message buffer is big enough for the reply */
	if (BUFCHK(&pp->ptm_data, &pp->pt_mdlen, valmsgsz(OP_FTIME_NVALUE)) < 0) {
		pp->ptm_rc = ER_ERR;
		return;
	}
	dp = pp->ptm_data;

	/* all OK so fill in the reply data and return */
	VM_ATIME(rp) = (long) stbuf.st_atime;
	VM_MTIME(rp) = (long) stbuf.st_mtime;
	rp->vm_nvalue = OP_FTIME_NVALUE;
	pp->ptm_mtype = MT_VALMSG;
	pp->ptm_len = valmsgsz(OP_FTIME_NVALUE);
	pp->ptm_rc = ER_OK;

#undef rp

}

/*
**	op_time() - return system time
*/

void op_time(pp)
struct ptab *pp;
{
	register struct valmsg *mp;

	TRACE1(tet_Ttccd, 4, "OP_TIME");

	/* make sure that the message buffer is big enough for the reply */
	if (BUFCHK(&pp->ptm_data, &pp->pt_mdlen, valmsgsz(OP_TIME_NVALUE)) < 0) {
		pp->ptm_rc = ER_ERR;
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
		return;
	}
	mp = (struct valmsg *) pp->ptm_data;

	/* all OK so fill in the reply message and return */
	VM_TIME(mp) = time((time_t *) 0);
	pp->ptm_rc = ER_OK;
	pp->ptm_mtype = MT_VALMSG;
	pp->ptm_len = valmsgsz(OP_TIME_NVALUE);
}

/*
**	op_lockfile() - create a lock file (exclusive lock)
**
**	a zero timeout means return immediately if the operation fails
**	a -ve timeout means try indefinately until a lock is obtained or an
**	error occurs
*/

void op_lockfile(pp)
struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the request message */
	if (mp->av_argc != OP_LOCKFILE_ARGC || !AV_FNAME(mp) || !*AV_FNAME(mp)) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	TRACE3(tet_Ttccd, 4, "OP_LOCKFILE: file = %s, timeout = %s",
		AV_FNAME(mp), tet_l2a(AV_TIMEOUT(mp)));

	/* call the tcc action function to create the lock file */
	pp->ptm_rc = tcf_lockfile(AV_FNAME(mp), AV_TIMEOUT(mp));
}

/*
**	op_sharelock() - create a file in a lock directory (shared lock)
**
**	a zero timeout means return immediately if the operation fails
**	a -ve timeout means try indefinately until a lock is obtained or an
**	error occurs
*/

void op_sharelock(pp)
struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;
	char *fname;

	/* do a sanity check on the request message */
	if (mp->av_argc != OP_SHARELOCK_ARGC || !AV_DIR(mp) || !*AV_DIR(mp)) {
		pp->ptm_rc = ER_INVAL;
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
		return;
	}

	TRACE3(tet_Ttccd, 4, "OP_SHARELOCK: dir = %s, timeout = %s",
		AV_DIR(mp), tet_l2a(AV_TIMEOUT(mp)));

	/* call the tcc action function to create the shared lock file */
	if ((pp->ptm_rc = tcf_sharelock(AV_DIR(mp), pp->ptr_pid, AV_TIMEOUT(mp), &fname)) == ER_OK) {
		TRACE2(tet_Ttccd, 4, "OP_SHARELOCK: return \"%s\"", fname);
		AV_FNAME(mp) = fname;
		mp->av_argc = OP_SHARELOCK_ARGC;
		pp->ptm_mtype = MT_AVMSG;
		pp->ptm_len = avmsgsz(OP_SHARELOCK_ARGC);
	}
	else {
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
	}
}

/*
**	op_mktmpdir() - make a unique temporary directory
*/

void op_mktmpdir(pp)
struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;
	char *dir;

	/* do a sanity check on the request message */
	if (mp->av_argc != OP_DIR_ARGC || !AV_DIR(mp) || !*AV_DIR(mp)) {
		pp->ptm_rc = ER_INVAL;
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
		return;
	}

	TRACE2(tet_Ttccd, 4, "OP_MKTMPDIR: dir = \"%s\"", AV_DIR(mp));

	/* call the tcc action function to make the directory */
	if ((pp->ptm_rc = tcf_mktmpdir(AV_DIR(mp), &dir)) == ER_OK) {
		AV_DIR(mp) = dir;
		mp->av_argc = OP_DIR_ARGC;
		pp->ptm_mtype = MT_AVMSG;
		pp->ptm_len = avmsgsz(OP_DIR_ARGC);
	}
	else {
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
	}
}

/*
**	op_rxfile() - transfer a file to the master system
*/

void op_rxfile(pp)
struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* perform a sanity check on the request message */
	if (mp->av_argc != OP_RXFILE_ARGC ||
		!AV_XFROM(mp) || !*AV_XFROM(mp) ||
		!AV_XTO(mp) || !*AV_XTO(mp)) {
			pp->ptm_rc = ER_INVAL;
			return;
	}

	TRACE3(tet_Ttccd, 4, "OP_RXFILE: from = \"%s\", to = \"%s\"",
		AV_XFROM(mp), AV_XTO(mp));

	/* log on to the xresd if necessary */
	ASSERT(tet_xdptab);
	if ((tet_xdptab->pt_flags & PF_LOGGEDON) == 0 && tet_xdlogon() < 0) {
		pp->ptm_rc = ER_ERR;
		return;
	}

	pp->ptm_rc = (tet_xdxfile(AV_XFROM(mp), AV_XTO(mp), 0) < 0) ? ER_ERR : ER_OK;
}

