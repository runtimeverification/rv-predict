/*
 *	SCCS: @(#)sdsnrm.c	1.1 (96/11/04)
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
static char sccsid[] = "@(#)sdsnrm.c	1.1 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)sdsnrm.c	1.1 96/11/04 TETware release 3.8
NAME:		sdsnrm.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	October 1996

DESCRIPTION:
	function to remove a sync ID (i.e., a sequence of auto-sync events)

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
**	tet_sdsnrm() - remove a sync ID
**
**	return 0 if successful or -1 on error
*/

int tet_sdsnrm(snid)
long snid;
{
	register struct valmsg *mp;
	extern char tet_sderrmsg[];

	/* get the SYNCD message buffer */
	if ((mp = (struct valmsg *) tet_sdmsgbuf(valmsgsz(OP_SNRM_NVALUE))) == (struct valmsg *) 0) {
		tet_sderrno = ER_ERR;
		return(-1);
	}

	/* construct the message */
	mp->vm_nvalue = OP_SNRM_NVALUE;
	VM_SNID(mp) = snid;

	/* perform the conversation and handle the reply codes */
	mp = (struct valmsg *) tet_sdtalk(OP_SNRM, TALK_DELAY);
	switch (tet_sderrno) {
	case ER_OK:
		return(0);
	case ER_SNID:
	case ER_PERM:
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

