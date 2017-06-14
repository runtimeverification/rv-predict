/*
 *      SCCS:  @(#)sdsnget.c	1.6 (96/11/04) 
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
static char sccsid[] = "@(#)sdsnget.c	1.6 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)sdsnget.c	1.6 96/11/04 TETware release 3.8
NAME:		sdsnget.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to get a sync identifier from syncd

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
**	tet_sdsnget() - get a sync id from SYNCD
**
**	return the sync id if successful or -1 on error
*/

long tet_sdsnget()
{
	register struct valmsg *mp;
	extern char tet_sderrmsg[];

	/* perform the conversation and handle the reply codes */
	mp = (struct valmsg *) tet_sdtalk(OP_SNGET, TALK_DELAY);
	switch (tet_sderrno) {
	case ER_OK:
		return(VM_SNID(mp));
	case ER_ERR:
		if (!mp)
			break;
		/* else fall through */
	default:
		error(0, tet_sderrmsg, tet_ptrepcode(tet_sderrno));
		break;
	}

	/* here for server error return */
	return(-1L);
}

