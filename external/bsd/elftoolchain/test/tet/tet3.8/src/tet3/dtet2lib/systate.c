/*
 *      SCCS:  @(#)systate.c	1.7 (98/08/28) 
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
static char sccsid[] = "@(#)systate.c	1.7 (98/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)systate.c	1.7 98/08/28 TETware release 3.8
NAME:		systate.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to return printable representation of a sync state

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#ifndef TET_LITE	/* -START-LITE-CUT- */

#include <stdio.h>
#include <sys/types.h>
#include "dtmac.h"
#include "synreq.h"
#include "ltoa.h"
#include "dtetlib.h"

/*
**	tet_systate() - return a printable representation of a synreq sy_state
**		value
*/

TET_IMPORT char *tet_systate(state)
int state;
{
	static char text[] = "sync-state ";
	static char msg[sizeof text + LNUMSZ];

	switch (state) {
	case SS_SYNCYES:
		return("SYNC-YES");
	case SS_SYNCNO:
		return("SYNC-NO");
	case SS_NOTSYNCED:
		return("NOT-SYNCED");
	case SS_TIMEDOUT:
		return("TIMED-OUT");
	case SS_DEAD:
		return("DEAD");
	default:
		(void) sprintf(msg, "%s%d", text, state);
		return(msg);
	}
}

#else	/* -END-LITE-CUT- */

int tet_systate_c_not_empty;

#endif /* !TET_LITE */	/* -LITE-CUT-LINE- */

