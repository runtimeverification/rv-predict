/*
 *      SCCS:  @(#)tcaccess.c	1.6 (96/11/04) 
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
static char sccsid[] = "@(#)tcaccess.c	1.6 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcaccess.c	1.6 96/11/04 TETware release 3.8
NAME:		tcaccess.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	June 1992

DESCRIPTION:
	function to request TCCD to check the accessibility of a file

MODIFICATIONS:

************************************************************************/

#include "dtmac.h"
#include "dtmsg.h"
#include "avmsg.h"
#include "servlib.h"

/*
**	tet_tcaccess() - send an OP_ACCESS message to TCCD and receive a reply
**
**	return 0 if successful or -1 on error
*/

int tet_tcaccess(sysid, path, mode)
int sysid, mode;
char *path;
{
	register struct avmsg *mp;

	/* make sure that path is non-null */
	if (!path || !*path) {
		tet_tcerrno = ER_INVAL;
		return(-1);
	}

	/* get the TCCD message buffer */
	if ((mp = (struct avmsg *) tet_tcmsgbuf(sysid, avmsgsz(OP_ACCESS_ARGC))) == (struct avmsg *) 0) {
		tet_tcerrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->av_argc = OP_ACCESS_ARGC;
	AV_MODE(mp) = (long) mode;
	AV_PATH(mp) = path;

	/* send the request and receive the reply */
	return(tet_tcrsys(sysid, OP_ACCESS));
}

