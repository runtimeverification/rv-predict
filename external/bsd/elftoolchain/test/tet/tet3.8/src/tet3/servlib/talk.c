/*
 *      SCCS:  @(#)talk.c	1.7 (99/09/02) 
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
static char sccsid[] = "@(#)talk.c	1.7 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)talk.c	1.7 99/09/02 TETware release 3.8
NAME:		talk.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to talk to a server

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include <time.h>
#include <sys/types.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "error.h"
#include "servlib.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_ti_talk() - talk to a server and receive a reply
**
**	return 0 if successful or -1 on error
*/

int tet_ti_talk(pp, delay)
struct ptab *pp;
int delay;
{
	register int rc;

	tet_si_clientloop(pp, delay);

	if (pp->pt_state == PS_DEAD || (pp->pt_flags & PF_IOERR))
		rc = -1;
	else if (pp->pt_flags & PF_TIMEDOUT) {
		pp->pt_flags &= ~PF_TIMEDOUT;
		error(0, "server timed out", tet_r2a(&pp->pt_rid));
		rc = -1;
	}
	else
		rc = 0;

	return(rc);
}

