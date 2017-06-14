/*
 *      SCCS:  @(#)rtab.c	1.7 (96/11/04) 
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
static char sccsid[] = "@(#)rtab.c	1.7 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)rtab.c	1.7 96/11/04 TETware release 3.8
NAME:		rtab.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	DTET API remote execution support routines

MODIFICATIONS:
	Geoff Clare, UniSoft Ltd., Sept 1996
	Changes for TETWare-Lite.

************************************************************************/

#ifndef TET_LITE /* -START-LITE-CUT- */

#include <stdlib.h>
#include <errno.h>
#include "dtmac.h"
#include "error.h"
#include "bstring.h"
#include "llist.h"
#include "rtab.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

static struct rtab *rtab;	/* ptr to head of remote execution table */


/*
**	tet_rtalloc() - allocate a remote execution table element and return
**		a pointer thereto
**
**	return (struct rtab *) 0 on error
*/

struct rtab *tet_rtalloc()
{
	register struct rtab *rp;
	static int remoteid;

	errno = 0;
	if ((rp = (struct rtab *) malloc(sizeof *rp)) == (struct rtab *) 0) {
		error(errno, "can't allocate rtab element", (char *) 0);
		return((struct rtab *) 0);
	}
	TRACE2(tet_Tbuf, 6, "allocate rtab element = %s", tet_i2x(rp));
	bzero((char *) rp, sizeof *rp);

	rp->rt_sysid = -1;
	rp->rt_pid = -1L;
	rp->rt_magic = RT_MAGIC;
	rp->rt_remoteid = ++remoteid;

	return(rp);
}

/*
**	tet_rtfree() - free remote execution table element
*/

void tet_rtfree(rp)
struct rtab *rp;
{
	TRACE2(tet_Tbuf, 6, "free rtab = %s", tet_i2x(rp));

	if (rp)
		free((char *) rp);
}

/*
**	tet_rtadd() - add an element to the remote execution table
*/

void tet_rtadd(rp)
struct rtab *rp;
{
	tet_listinsert((struct llist **) &rtab, (struct llist *) rp);
}

/*
**	tet_rtrm() - remove an element from the remote execution list
*/

void tet_rtrm(rp)
struct rtab *rp;
{
	tet_listremove((struct llist **) &rtab, (struct llist *) rp);
}

/*
**	tet_rtfind() - find remote execution table element matching remoteid
**		and return a pointer thereto
**
**	return (struct rtab *) 0 if not found
*/

struct rtab *tet_rtfind(remoteid)
register int remoteid;
{
	register struct rtab *rp;

	TRACE3(tet_Ttcm, 6, "tet_rtfind(%s): rtab = %s",
		tet_i2a(remoteid), tet_i2x(rtab));

	for (rp = rtab; rp; rp = rp->rt_next) {
		ASSERT(rp->rt_magic == RT_MAGIC);
		TRACE5(tet_Ttcm, 8,
			"rtab: addr = %s, remoteid = %s, sysid = %s, pid = %s",
			tet_i2x(rp), tet_i2a(rp->rt_remoteid),
			tet_i2a(rp->rt_sysid), tet_l2a(rp->rt_pid));
		if (rp->rt_remoteid == remoteid)
			break;
	}

	TRACE2(tet_Ttcm, 6, "tet_rtfind() returns %s", tet_i2x(rp));

	return(rp);
}

#else /* -END-LITE-CUT- */

/* avoid "empty" file */
int tet_rtab_not_needed;

#endif /* -LITE-CUT-LINE- */

