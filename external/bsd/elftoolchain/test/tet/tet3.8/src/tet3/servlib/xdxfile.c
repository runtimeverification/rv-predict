/*
 *      SCCS:  @(#)xdxfile.c	1.7 (03/03/26) 
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
static char sccsid[] = "@(#)xdxfile.c	1.7 (03/03/26) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)xdxfile.c	1.7 03/03/26 TETware release 3.8
NAME:		xdxfile.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	May 1992

DESCRIPTION:
	function to transfer a save file to the master system

MODIFICATIONS:
	Andrew Dingwall, The Open Group, March 2003
	Added support for binary file transfer.


************************************************************************/

#include <stdio.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "avmsg.h"
#include "btmsg.h"
#include "valmsg.h"
#include "error.h"
#include "bstring.h"
#include "servlib.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* static function declarations */
static int xd_tfclose PROTOLIST((int));
static long xd_tfopen PROTOLIST((char *, int, int));
static int xd_tfwrite PROTOLIST((FILE *, int, char *));
static int xd_wrloop PROTOLIST((FILE *, int, char *));


/*
**	tet_xdxfile() - transfer a save file to the xresd on the
**		master system
**
**	return 0 if successful or -1 on error
*/

int tet_xdxfile(ifile, ofile, binflag)
char *ifile, *ofile;
int binflag;
{
	register int rc;
	register int xfid;
	register FILE *ifp;
	struct STAT_ST stbuf;
	char *mode;

	/* make sure that file names are non-null */
	if (!ifile || !*ifile || !ofile || !*ofile) {
		tet_xderrno = ER_INVAL;
		return(-1);
	}

	/* get the file mode */
	if (STAT(ifile, &stbuf) < 0) {
		error(errno, "stat failed on", ifile);
		tet_xderrno = ER_ERR;
		return(-1);
	}

	/* open the file */
	mode = binflag ? "rb" : "r";
	if ((ifp = fopen(ifile, mode)) == NULL) {
		error(errno, "can't open", ifile);
		tet_xderrno = ER_ERR;
		return(-1);
	}

	/* open the transfer file on the master system */
	if ((xfid = xd_tfopen(ofile, (int) stbuf.st_mode, binflag)) < 0) {
		(void) fclose(ifp);
		return(-1);
	}

	/* transfer the data to the master system */
	rc = xd_wrloop(ifp, xfid, ifile);

	/* close the local file and the transfer file on the master system */
	(void) fclose(ifp);
	return(xd_tfclose(xfid) < 0 ? -1 : rc);
}

/*
**	xd_tfopen() - open a transfer file on the master system
**
**	return the transfer file id if successful or -1 on error
*/

static long xd_tfopen(ofile, mode, binflag)
char *ofile;
int mode, binflag;
{
	register char *dp;
	extern char tet_xderrmsg[];

	/* get the xresd message buffer */
	if ((dp = tet_xdmsgbuf(avmsgsz(OP_TFOPEN_ARGC))) == (char *) 0) {
		tet_xderrno = ER_ERR;
		return(-1L);
	}

#define mp	((struct avmsg *) dp)

	/* set up the request message */
	mp->av_argc = OP_TFOPEN_ARGC;
	AV_TFNAME(mp) = ofile;
	AV_FLAG(mp) = binflag;
	AV_MODE(mp) = (long) mode;

#undef mp

	/* send the request and receive the reply */
	dp = tet_xdtalk(OP_TFOPEN, TALK_DELAY);

#define rp	((struct valmsg *) dp)

	/* handle the return codes */
	switch (tet_xderrno) {
	case ER_OK:
		return((int) VM_XFID(rp));
	case ER_ERR:
		if (!dp)
			break;
		/* else fall through */
	default:
		error(0, tet_xderrmsg, tet_ptrepcode(tet_xderrno));
		break;
	}

#undef rp

	/* here for server error return */
	return(-1L);
}

/*
**	xd_tfclose() - close a transfer file on the master system
**
**	return 0 if successful or -1 on error
*/

static int xd_tfclose(xfid)
int xfid;
{
	register struct valmsg *mp;
	extern char tet_xderrmsg[];

	/* get the xresd message buffer */
	if ((mp = (struct valmsg *) tet_xdmsgbuf(valmsgsz(OP_TFCLOSE_NVALUE))) == (struct valmsg *) 0) {
		tet_xderrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->vm_nvalue = OP_TFCLOSE_NVALUE;
	VM_XFID(mp) = (long) xfid;

	/* send the request and receive the reply */
	mp = (struct valmsg *) tet_xdtalk(OP_TFCLOSE, TALK_DELAY);

	/* handle the return codes */
	switch (tet_xderrno) {
	case ER_OK:
		return(0);
	case ER_ERR:
		if (!mp)
			break;
		/* else fall through */
	default:
		error(0, tet_xderrmsg, tet_ptrepcode(tet_xderrno));
		break;
	}

	/* here for server error return */
	return(-1);
}

/*
**	xd_wrloop() - write a transfer file on the master system
**
**	return 0 if successful or -1 on error
*/

static int xd_wrloop(ifp, xfid, ifile)
FILE *ifp;
int xfid;
char *ifile;
{
	register int rc;

	while ((rc = xd_tfwrite(ifp, xfid, ifile)) > 0)
		;

	return(rc);
}

/*
**	xd_tfwrite() - perform a single transfer file i/o operation
**
**	return	1 if successful
**		0 on EOF
**		-1 on error
*/

static int xd_tfwrite(ifp, xfid, ifile)
FILE *ifp;
int xfid;
char *ifile;
{
	register struct btmsg *mp;
	register int n;
	extern char tet_xderrmsg[];

	/* get the XRESD message buffer */
	if ((mp = (struct btmsg *) tet_xdmsgbuf(BT_BTMSGSZ)) == (struct btmsg *) 0)
		return(-1);

	/* read the input file */
	if ((n = fread(mp->bt_data, sizeof mp->bt_data[0], sizeof mp->bt_data, ifp)) < 0) {
		error(errno, "read error on", ifile);
		tet_xderrno = ER_ERR;
		return(-1);
	}
	else if (n == 0)
		return(0);

	/* set up the request message */
	mp->bt_fid = (unsigned short) xfid;
	mp->bt_count = (unsigned short) n;
	if (n < sizeof mp->bt_data)
		bzero(&mp->bt_data[n], (int) (sizeof mp->bt_data - n));

	/* send the request and recieve the reply */
	mp = (struct btmsg *) tet_xdtalk(OP_TFWRITE, TALK_DELAY);

	/* handle the return codes */
	switch (tet_xderrno) {
	case ER_OK:
		return(1);
	case ER_ERR:
		if (!mp)
			break;
		/* else fall through */
	default:
		error(0, tet_xderrmsg, tet_ptrepcode(tet_xderrno));
		break;
	}

	/* here for server error return */
	return(-1);
}

