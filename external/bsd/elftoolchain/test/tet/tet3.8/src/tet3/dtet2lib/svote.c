/*
 *      SCCS:  @(#)svote.c	1.6 (96/11/04) 
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
static char sccsid[] = "@(#)svote.c	1.6 (96/11/04) TETware release 3.8";
#endif


/************************************************************************

SCCS:   	@(#)svote.c	1.6 96/11/04 TETware release 3.8
NAME:		svote.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	June 1992

DESCRIPTION:
	function to return printable representation of a sync vote

MODIFICATIONS:

************************************************************************/

#ifndef TET_LITE	/* -START-LITE-CUT- */

#include <stdio.h>
#include <sys/types.h>
#include "dtmac.h"
#include "synreq.h"
#include "ltoa.h"
#include "dtetlib.h"

/*
**	tet_ptsvote() - return a printable representation of a sync vote
*/

char *tet_ptsvote(vote)
int vote;
{
	static char text[] = "unknown sync-vote ";
	static char msg[sizeof text + LNUMSZ];

	switch (vote) {
	case SV_YES:
		return("YES");
	case SV_NO:
		return("NO");
	default:
		(void) sprintf(msg, "%s%d", text, vote);
		return(msg);
	}
}

#else	/* -END-LITE-CUT- */

int tet_svote_c_not_empty;

#endif /* !TET_LITE */	/* -LITE-CUT-LINE- */

