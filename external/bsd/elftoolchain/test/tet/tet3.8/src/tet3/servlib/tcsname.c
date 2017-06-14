/*
 *      SCCS:  @(#)tcsname.c	1.6 (96/11/04) 
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
static char sccsid[] = "@(#)tcsname.c	1.6 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcsname.c	1.6 96/11/04 TETware release 3.8
NAME:		tcsname.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	May 1992

DESCRIPTION:
	function to send system name list to tccd

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
**	tet_tcsysname() - send OP_SYSNAME message to TCCD and receive a reply
**
**	return 0 if successful or -1 on error
*/

int tet_tcsysname(sysid, snames, nsname)
int sysid;
register int *snames, nsname;
{
	register struct valmsg *mp;
	register int n;
	extern char tet_tcerrmsg[];

	/* make sure that snames is non-zero and that nsname is +ve */
	if (!snames || nsname <= 0) {
		tet_tcerrno = ER_INVAL;
		return(-1);
	}

	/* get the TCCD message buffer */
	if ((mp = (struct valmsg *) tet_tcmsgbuf(sysid, valmsgsz(OP_SYSNAME_NVALUE(nsname)))) == (struct valmsg *) 0) {
		tet_tcerrno = ER_ERR;
		return(-1L);
	}

	/* set up the request message */
	mp->vm_nvalue = OP_SYSNAME_NVALUE(nsname);
	for (n = 0; n < nsname; n++)
		VM_SYSNAME(mp, n) = (long) *snames++;

	/* send the request and receive the reply */
	mp = (struct valmsg *) tet_tctalk(sysid, OP_SYSNAME, TALK_DELAY);

	/* handle the return codes */
	switch (tet_tcerrno) {
	case ER_OK:
		return(0);
	case ER_INVAL:
	case ER_DUPS:
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
	return(-1);
}

