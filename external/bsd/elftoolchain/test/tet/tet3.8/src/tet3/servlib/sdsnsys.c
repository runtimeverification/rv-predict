/*
 *      SCCS:  @(#)sdsnsys.c	1.6 (96/11/04) 
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
static char sccsid[] = "@(#)sdsnsys.c	1.6 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)sdsnsys.c	1.6 96/11/04 TETware release 3.8
NAME:		sdsnsys.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to send system name list to syncd

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
**	tet_sdsnsys() - send system name list to SYNCD
**
**	return 0 if successful or -1 on error
*/

int tet_sdsnsys(snid, snames, nsname)
long snid;
register int *snames, nsname;
{
	register struct valmsg *mp;
	register int n;
	extern char tet_sderrmsg[];

	/* check snames and nsname */
	if (!snames || nsname <= 0) {
		tet_sderrno = ER_INVAL;
		return(-1);
	}

	/* get the SYNCD message buffer */
	if ((mp = (struct valmsg *) tet_sdmsgbuf(valmsgsz(OP_SNSYS_NVALUE(nsname)))) == (struct valmsg *) 0) {
		tet_sderrno = ER_ERR;
		return(-1);
	}

	/* construct the message */
	mp->vm_nvalue = OP_SNSYS_NVALUE(nsname);
	VM_SNID(mp) = snid;
	for (n = 0; n < nsname; n++)
		VM_SSYSID(mp, n) = (long) *snames++;

	/* perform the conversation and handle the reply codes */
	mp = (struct valmsg *) tet_sdtalk(OP_SNSYS, TALK_DELAY);
	switch (tet_sderrno) {
	case ER_OK:
		return(0);
	case ER_INVAL:
	case ER_DUPS:
	case ER_SNID:
	case ER_INPROGRESS:
		break;
	case ER_ERR:
		if (!mp)
			break;
		/* else fall through */
	default:
		error(0, tet_sderrmsg, tet_ptrepcode(tet_sderrno));
		break;
	}

	/* here for server error return */
	return(-1);
}

