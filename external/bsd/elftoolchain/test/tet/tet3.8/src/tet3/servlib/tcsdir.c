/*
 *      SCCS:  @(#)tcsdir.c	1.6 (96/11/04) 
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
static char sccsid[] = "@(#)tcsdir.c	1.6 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcsdir.c	1.6 96/11/04 TETware release 3.8
NAME:		tcsdir.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	June 1992

DESCRIPTION:
	function to make a saved files directory on a remote system

MODIFICATIONS:

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


/*
**	tet_tcmksdir() - send an OP_MKSDIR message to TCCD and receive a reply
**
**	return pointer to new directory name if successful or (char *) 0
**	on error
*/

char *tet_tcmksdir(sysid, dir, suffix)
int sysid;
char *dir, *suffix;
{
	register struct avmsg *mp;
	extern char tet_tcerrmsg[];

	/* make sure that dir and suffix are non-null */
	if (!dir | !*dir || !suffix || !*suffix) {
		tet_tcerrno = ER_INVAL;
		return((char *) 0);
	}

	/* get the TCCD message buffer */
	if ((mp = (struct avmsg *) tet_tcmsgbuf(sysid, avmsgsz(OP_MKSDIR_ARGC))) == (struct avmsg *) 0) {
		tet_tcerrno = ER_ERR;
		return((char *) 0);
	}

	/* set up the request message */
	mp->av_argc = OP_MKSDIR_ARGC;
	AV_DIR(mp) = dir;
	AV_SUFFIX(mp) = suffix;

	/* send the request and receive the reply */
	mp = (struct avmsg *) tet_tctalk(sysid, OP_MKSDIR, TALK_DELAY);

	/* handle the return codes */
	switch (tet_tcerrno) {
	case ER_OK:
		return(AV_DIR(mp));
	case ER_CONTEXT:
	case ER_NOENT:
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
	return((char *) 0);
}

