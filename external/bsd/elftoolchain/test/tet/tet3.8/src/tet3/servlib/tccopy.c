/*
 *      SCCS:  @(#)tccopy.c	1.5 (96/11/04) 
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
static char sccsid[] = "@(#)tccopy.c	1.5 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tccopy.c	1.5 96/11/04 TETware release 3.8
NAME:		tccopy.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Ltd.
DATE CREATED:	August 1993

DESCRIPTION:
	functions to request TCCD to copy files from source to destination.
	A recursive copy is done if necessary.

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "avmsg.h"
#include "error.h"
#include "servlib.h"
#include "dtetlib.h"


/*
**	tet_tcrcopy() - send an OP_RCOPY message to TCCD and receive a reply
**
**	return 0 if successful or -1 on error
*/

int tet_tcrcopy(sysid, from, to)
int sysid;
char *from, *to;
{
	register struct avmsg *mp;

	/* make sure that from and to are non-null */
	if (!from || !*from || !to || !*to) {
		tet_tcerrno = ER_INVAL;
		return(-1);
	}

	/* get the TCCD message buffer */
	if ((mp = (struct avmsg *) tet_tcmsgbuf(sysid, avmsgsz(OP_RCOPY_ARGC))) == (struct avmsg *) 0) {
		tet_tcerrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->av_argc = OP_RCOPY_ARGC;
	AV_XFROM(mp) = from;
	AV_XTO(mp)   = to;

	/* send the request and receive the reply */
	return(tet_tcrsys(sysid, OP_RCOPY));
}

