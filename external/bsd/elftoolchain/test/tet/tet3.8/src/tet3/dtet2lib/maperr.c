/*
 *      SCCS:  @(#)maperr.c	1.10 (02/01/18)
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
static char sccsid[] = "@(#)maperr.c	1.10 (02/01/18) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)maperr.c	1.10 02/01/18 TETware release 3.8
NAME:		maperr.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	May 1992

DESCRIPTION:
	function to convert local errno value to DTET message reply code

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., November 1992
	AIX-specific modifications.

	Andrew Dingwall, UniSoft Ltd., January 1993
	Rewritten to use the errmap structure -
	this avoids compiler "duplicate case in switch" messages.

	Andrew Dingwall, UniSoft Ltd., March 2000
	Handle errnum == 0 correctly.

************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "error.h"
#include "dtmsg.h"
#include "dtetlib.h"
#include "errmap.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/*
**	tet_maperrno() - map errno value to DTET message reply code
*/

int tet_maperrno(errnum)
register int errnum;
{
	register struct errmap *ep, *ee;

	for (ep = tet_errmap, ee = &tet_errmap[tet_Nerrmap]; ep < ee; ep++)
		if (errnum == ep->em_errno) {
			if (ep->em_repcode <= 0)
				return(ep->em_repcode);
			else
				break;
		}

	error(errnum, ep < ee ? ep->em_errname : tet_errname(errnum),
		"has no equivalent DTET message reply code");
	return(ER_ERR);
}

