/*
 *	SCCS: @(#)tctime.c	1.5 (96/11/04)
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
static char sccsid[] = "@(#)tctime.c	1.5 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tctime.c	1.5 96/11/04 TETware release 3.8
NAME:		tctime.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	function to obtain system time from TCCD

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "valmsg.h"
#include "error.h"
#include "servlib.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_tctime() - send OP_TIME message to TCCD and receive a reply
**
**	return 0 if successful or -1 on error
*/

int tet_tctime(sysid, tp)
int sysid;
long *tp;
{
	register struct valmsg *mp;
	extern char tet_tcerrmsg[];

	/* make sure that tp is non-null */
	if (!tp) {
		tet_tcerrno = ER_INVAL;
		return(-1);
	}

	/* send the request and receive the reply */
	mp = (struct valmsg *) tet_tctalk(sysid, OP_TIME, TALK_DELAY);

	/* handle the return codes */
	switch (tet_tcerrno) {
	case ER_OK:
		*tp = VM_TIME(mp);
		return(0);
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

