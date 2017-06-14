/*
 *      SCCS:  @(#)tcunlink.c	1.5 (96/11/04) 
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
static char sccsid[] = "@(#)tcunlink.c	1.5 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcunlink.c	1.5 96/11/04 TETware release 3.8
NAME:		tcunlink.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	May 1992

DESCRIPTION:
	function to unlink a file on a remote system

MODIFICATIONS:

************************************************************************/

#include "dtmac.h"
#include "dtmsg.h"
#include "avmsg.h"
#include "servlib.h"

/*
**	tet_tcunlink() - send an OP_UNLINK message to TCCD and receive a reply
**
**	return 0 if successful or -1 on error
*/

int tet_tcunlink(sysid, file)
int sysid;
char *file;
{
	register struct avmsg *mp;

	/* make sure that file is non-null */
	if (!file || !*file) {
		tet_tcerrno = ER_INVAL;
		return(-1);
	}

	/* get the TCCD message buffer */
	if ((mp = (struct avmsg *) tet_tcmsgbuf(sysid, avmsgsz(OP_UNLINK_ARGC))) == (struct avmsg *) 0) {
		tet_tcerrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->av_argc = OP_UNLINK_ARGC;
	AV_FNAME(mp) = file;

	/* send the request and receive the reply */
	return(tet_tcrsys(sysid, OP_UNLINK));
}

