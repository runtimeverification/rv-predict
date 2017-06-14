/*
 *      SCCS:  @(#)tctsfile.c	1.9 (96/11/04) 
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
static char sccsid[] = "@(#)tctsfile.c	1.9 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tctsfile.c	1.9 96/11/04 TETware release 3.8
NAME:		tctsfile.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	June 1992

DESCRIPTION:
	TCCD save file and transfer save file functions

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, September 1993
	added savedir (save files directory) parameter to
	tet_tctslfiles() and tet_tctsmfiles()

	Denis McConalogue, UniSoft Limited, September 1993
	fix prototype for tc_tsfiles().

	Andrew Dingwall, UniSoft Ltd., August 1996
	removed savedir parameter from tet_tctsmfiles()
	(it doesn't do anything!)

************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "avmsg.h"
#include "error.h"
#include "servlib.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* static function declarations */
static int tc_tsfiles PROTOLIST((int, char **, int, char *, char *, int));


/*
**	tet_tctslfiles() - save files locally on a remote system
**
**	return 0 if successful or -1 on error
*/

int tet_tctslfiles(sysid, files, nfile, subdir, savedir)
int sysid, nfile;
char **files, *subdir, *savedir;
{
	return(tc_tsfiles(sysid, files, nfile, subdir, savedir, AV_TS_LOCAL));
}

/*
**	tet_tctsmfiles() - copy files on a remote system to subdir in the saved
**		files directory on the master system
**
**	return 0 if successful or -1 on error
*/

int tet_tctsmfiles(sysid, files, nfile, subdir)
int sysid, nfile;
char **files, *subdir;
{
	return(tc_tsfiles(sysid, files, nfile, subdir, (char *) 0,
		AV_TS_MASTER));
}

/*
**	tc_tsfiles() - send an OP_TSFILES message to TCCD and receive a reply
**
**	return 0 if successful or -1 on error
*/

static int tc_tsfiles(sysid, files, nfile, subdir, savedir, flag)
int sysid, flag;
register char **files, *subdir, *savedir;
register int nfile;
{
	register struct avmsg *mp;
	register int n;
	extern char tet_tcerrmsg[];

	/* make sure that files is non-zero and that nfile is +ve */
	if (!files || nfile <= 0) {
		tet_tcerrno = ER_INVAL;
		return(-1);
	}

	/* get the TCCD message buffer */
	if ((mp = (struct avmsg *) tet_tcmsgbuf(sysid, avmsgsz(OP_TSFILES_ARGC(nfile)))) == (struct avmsg *) 0) {
		tet_tcerrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->av_argc = OP_TSFILES_ARGC(nfile);
	AV_FLAG(mp) = flag;
	AV_SUBDIR(mp) = subdir;
	AV_SAVEDIR(mp) = savedir;
	for (n = 0; n < nfile; n++)
		AV_TSFILE(mp, n) = *files++;

	/* send the request and receive the reply */
	mp = (struct avmsg *) tet_tctalk(sysid, OP_TSFILES, TALK_DELAY);

	/* handle the return codes */
	switch (tet_tcerrno) {
	case ER_OK:
		return(0);
	case ER_INVAL:
	case ER_CONTEXT:
		break;
	case ER_ERR:
		if (!mp)
			break;
		/* else fall through */
	default:
		error(0, tet_tcerrmsg, tet_ptrepcode(tet_tcerrno));
		break;
	}

	/* here for server error return */
	return(-1);
}

