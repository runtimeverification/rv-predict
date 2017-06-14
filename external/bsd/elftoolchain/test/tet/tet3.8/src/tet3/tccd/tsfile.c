/*
 *      SCCS:  @(#)tsfile.c	1.14 (03/03/26) 
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
static char sccsid[] = "@(#)tsfile.c	1.14 (03/03/26) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tsfile.c	1.14 03/03/26 TETware release 3.8
NAME:		tsfile.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	June 1992

DESCRIPTION:
	transfer save files functions

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, September 1993
	extract save files path from OP_TSFILES request message

	Denis McConalogue, UniSoft Limited, September 1993
	allow shell wild cards in file name matching

	Andrew Dingwall, UniSoft Ltd., November 1993
	enhancements for FIFO transport interface
	removed/re-coded non-POSIX stuff

	Geoff Clare, UniSoft Ltd., August 1996
	Missing <unistd.h>.

	Andrew Dingwall, UniSoft Ltd., October 1996
	changes for TETware

	Andrew Dingwall, UniSoft Ltd., March 1998
	Avoid passing a -ve precision value to sprintf().

	Andrew Dingwall, The Open Group, March 2003
	Updated the OP_MKSDIR and OP_TSFILES functionality to fulfill
	the requirements of the TETware tcc.
 

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <time.h>
#include <sys/types.h>
#include <sys/stat.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "avmsg.h"
#include "error.h"
#include "dtetlib.h"
#include "tetdir.h"
#include "tccd.h"
#include "tcclib.h"

#ifndef NOTRACE
#  include "ltoa.h"
#endif


/* creation mode for diretories */
#define DIRMODE		((mode_t) (S_IRWXU | S_IRWXG | S_IRWXO))


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif



/*
**	op_mksdir() - make a new save files directory
*/

void op_mksdir(pp)
register struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;
	register int n, nmax, rc;
	register struct dirent *dp;
	DIR *dirp;
	char savdir[MAXPATH + 1], lokdir[sizeof savdir];
	char dir[sizeof savdir - 8]; /* the 8 is strlen("/NNNNbec") */
	static char errmsg[] = "can't make directory";
	static char *sfdir;
	static int sfdlen;
	int needlen;

	/* do a sanity check on the request */
	if (mp->av_argc != OP_MKSDIR_ARGC ||
		!AV_DIR(mp) || !*AV_DIR(mp) ||
		!AV_SUFFIX(mp) || !*AV_SUFFIX(mp)) {
			pp->ptm_rc = ER_INVAL;
			pp->ptm_mtype = MT_NODATA;
			pp->ptm_len = 0;
			return;
	}

	TRACE3(tet_Ttccd, 4, "OP_MKSDIR: dir = \"%s\", suffix = \"%s\"",
		AV_DIR(mp), AV_SUFFIX(mp));

	/* construct the specified directory path name */
	(void) sprintf(dir, "%.*s", (int) sizeof dir - 1, AV_DIR(mp));

	/* open the directory */
	if ((dirp = OPENDIR(dir)) == (DIR *) 0) {
		error(errno, "can't open directory", dir);
		pp->ptm_rc = ER_NOENT;
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
		return;
	}

	/* read each slot in turn, looking for the highest numeric prefix */
	nmax = 0;
	while ((dp = READDIR(dirp)) != (struct dirent *) 0)
		if ((n = atoi(dp->d_name)) > nmax)
			nmax = n;
	(void) CLOSEDIR(dirp);

	/* make the directory first without the suffix (try a few times) */
	for (n = 0; n < 5; n++) {
		if (++nmax > 9999) {
			error(0, "out of subdirectory sequence numbers in",
				dir);
			pp->ptm_rc = ER_ERR;
			pp->ptm_mtype = MT_NODATA;
			pp->ptm_len = 0;
			return;
		}
		(void) sprintf(lokdir, "%s/%04d", dir, nmax);
		TRACE2(tet_Ttccd, 6, "mksdir: try lokdir = \"%s\"", lokdir);
		if (tet_mkdir(lokdir, DIRMODE) == 0 || errno != EEXIST)
			break;
	}
	if (n >= 5) {
		error(errno, errmsg, lokdir);
		pp->ptm_rc = ER_ERR;
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
		return;
	}

	/* then make the directory with the suffix
		and remove the other one */
	(void) sprintf(savdir, "%s%.3s", lokdir, AV_SUFFIX(mp));
	TRACE2(tet_Ttccd, 6, "mksdir: make savdir = \"%s\"", savdir);
	if ((rc = tet_mkdir(savdir, DIRMODE)) < 0)
		error(errno, errmsg, savdir);
	(void) tet_rmdir(lokdir);
	if (rc < 0) {
		pp->ptm_rc = ER_ERR;
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
		return;
	}

	/* remember the savedir name */
	needlen = strlen(savdir) + 1;
	if (BUFCHK(&sfdir, &sfdlen, needlen) < 0) {
		(void) tet_rmdir(savdir);
		pp->ptm_rc = ER_ERR;
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
		return;
	}
	else
		sprintf(sfdir, "%.*s", sfdlen - 1, savdir);

	/* all ok so return success */
	mp->av_argc = OP_MKSDIR_ARGC;
	AV_DIR(mp) = sfdir;
	AV_SUFFIX(mp) = (char *) 0;
	pp->ptm_rc = ER_OK;
	pp->ptm_mtype = MT_AVMSG;
	pp->ptm_len = avmsgsz(OP_MKSDIR_ARGC);
}

/*
**	op_tsfiles() - perform a transfer save files operation
*/

void op_tsfiles(pp)
struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;
	register char *todir;
	register int flag;
	int len;
	char dir[MAXPATH];

#ifndef NOTRACE
	int n;
	static char null[] = "NULL";

	if (tet_Ttccd > 0) {
		TRACE5(tet_Ttccd, 4, "OP_TSFILES: nfiles = %s, flag = %s, savedir = \"%s\", subdir = \"%s\"",
			tet_i2a(OP_TSFILES_NFILES(mp)), tet_l2a(AV_FLAG(mp)),
			AV_SAVEDIR(mp) ? AV_SAVEDIR(mp) : null,
			AV_SUBDIR(mp) ? AV_SUBDIR(mp) : null);
		for (n = 0; n < OP_TSFILES_NFILES(mp); n++)
			TRACE2(tet_Ttccd, 4, "file = \"%s\"", AV_TSFILE(mp, n));
	}
#endif

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the request */
	if (
		OP_TSFILES_NFILES(mp) == 0 ||
		(
			AV_FLAG(mp) == AV_TS_LOCAL && (
				!AV_SAVEDIR(mp) ||
				!*AV_SAVEDIR(mp) ||
				!isabspathloc(AV_SAVEDIR(mp))
			)
		) || (
			AV_FLAG(mp) == AV_TS_MASTER &&
			AV_SAVEDIR(mp)
		)
	) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	/*
	** translate the flag value for the tcf_procdir() call
	**
	** for a local transfer, make sure that we have a saved files directory
	**
	** determine the name of the destination directory -
	** for a local transfer it is specified in the request;
	** for a transfer to the master system it is relative to the saved
	** files directory on the master system which is known by XRESD
	*/
	switch (AV_FLAG(mp)) {
	case AV_TS_LOCAL:
		flag = TCF_TS_LOCAL;
		if (!AV_SUBDIR(mp) || !*AV_SUBDIR(mp))
			todir = AV_SAVEDIR(mp);
		else {
			len = (int) sizeof dir -
				(int) strlen(AV_SAVEDIR(mp)) - 2;
			(void) sprintf(dir, "%.*s/%.*s",
				(int) sizeof dir - 2, AV_SAVEDIR(mp),
				TET_MAX(len, 0), AV_SUBDIR(mp));
			todir = dir;
		}
		break;
	case AV_TS_MASTER:
		todir = AV_SUBDIR(mp);
		flag = TCF_TS_MASTER;
		break;
	default:
		pp->ptm_rc = ER_INVAL;
		return;
	}

	/* process each save file (or directory) in turn */
	pp->ptm_rc = tcf_procdir(".", todir, &AV_TSFILE(mp, 0),
		OP_TSFILES_NFILES(mp), flag);
}

