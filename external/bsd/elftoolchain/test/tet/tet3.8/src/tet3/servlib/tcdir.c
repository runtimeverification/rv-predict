/*
 *      SCCS:  @(#)tcdir.c	1.7 (96/11/04) 
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
static char sccsid[] = "@(#)tcdir.c	1.7 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcdir.c	1.7 96/11/04 TETware release 3.8
NAME:		tcdir.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	May 1992

DESCRIPTION:
	functions to request TCCD to make, remove and change directories

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., August 1996
	added tet_tcmkalldirs(), tet_tcrmalldirs()

************************************************************************/

#include "dtmac.h"
#include "dtmsg.h"
#include "avmsg.h"
#include "servlib.h"

/* static function declarations */
static int tc_dir PROTOLIST((int, char *, int));


/*
**	tet_tcmkdir() - send an OP_MKDIR message to TCCD and receive a reply
**
**	return 0 if successful or -1 on error
*/

int tet_tcmkdir(sysid, dir)
int sysid;
char *dir;
{
	return(tc_dir(sysid, dir, OP_MKDIR));
}

/*
**	tet_tcmkalldirs() - send an OP_MKALLDIRS message to TCCD and receive
**		a reply
**
**	return 0 if successful or -1 on error
*/

int tet_tcmkalldirs(sysid, dir)
int sysid;
char *dir;
{
	return(tc_dir(sysid, dir, OP_MKALLDIRS));
}

/*
**	tet_tcrmdir() - send an OP_RMDIR message to TCCD and receive a reply
**
**	return 0 if successful or -1 on error
*/

int tet_tcrmdir(sysid, dir)
int sysid;
char *dir;
{
	return(tc_dir(sysid, dir, OP_RMDIR));
}

/*
**	tet_tcrmalldirs() - send an OP_RMALLDIRS message to TCCD and receive
**		a reply
**
**	return 0 if successful or -1 on error
*/

int tet_tcrmalldirs(sysid, dir)
int sysid;
char *dir;
{
	return(tc_dir(sysid, dir, OP_RMALLDIRS));
}

/*
**	tet_tcchdir() - send an OP_CHDIR message to TCCD and receive a reply
**
**	return 0 if successful or -1 on error
*/

int tet_tcchdir(sysid, dir)
int sysid;
char *dir;
{
	return(tc_dir(sysid, dir, OP_CHDIR));
}

/*
**	tc_dir() - common routine for TCCD directory requests
**
**	return 0 if successful or -1 on error
*/

static int tc_dir(sysid, dir, request)
int sysid, request;
char *dir;
{
	register struct avmsg *mp;

	/* make sure that dir is non-null */
	if (!dir || !*dir) {
		tet_tcerrno = ER_INVAL;
		return(-1);
	}

	/* get the TCCD message buffer */
	if ((mp = (struct avmsg *) tet_tcmsgbuf(sysid, avmsgsz(OP_DIR_ARGC))) == (struct avmsg *) 0) {
		tet_tcerrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->av_argc = OP_DIR_ARGC;
	AV_DIR(mp) = dir;

	/* send the request and receive the reply */
	return(tet_tcrsys(sysid, request));
}

