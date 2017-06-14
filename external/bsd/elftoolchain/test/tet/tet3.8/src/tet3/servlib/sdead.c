/*
 *      SCCS:  @(#)sdead.c	1.7 (99/09/02) 
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
static char sccsid[] = "@(#)sdead.c	1.7 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)sdead.c	1.7 99/09/02 TETware release 3.8
NAME:		sdead.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	dead process handler function

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <sys/types.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "server.h"
#include "servlib.h"
#include "tslib.h"
#include "dtetlib.h"


/*
**	tet_so_dead() - server-only dead process routine
**
**	called when a client dies and we don't need the proc table entry
**	any more
*/

void tet_so_dead(pp)
struct ptab *pp;
{
	TRACE2(tet_Tserv, 4, "%s process dead", tet_r2a(&pp->pt_rid));

	/* remove the ptab entry and free it */
	tet_ptrm(pp);
	tet_ptfree(pp);

	/* if no connected processes, wait a bit just in case anyone
		still wants to connect - exit if not */
	if (!tet_ptab && tet_ts_poll(tet_ptab, SHORTDELAY) <= 0) {
		TRACE1(tet_Tserv, 2, "no more connected processes - exiting");
		tet_ss_cleanup();
	}
}

