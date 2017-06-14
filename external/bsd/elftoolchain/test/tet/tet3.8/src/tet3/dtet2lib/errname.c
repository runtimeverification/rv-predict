/*
 *      SCCS:  @(#)errname.c	1.7 (96/09/30)
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
static char sccsid[] = "@(#)errname.c	1.7 (96/09/30) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)errname.c	1.7 96/09/30 TETware release 3.8
NAME:		errname.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to return printable representation of error number

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., November 1992
	AIX-specific modifications.

	Andrew Dingwall, UniSoft Ltd., January 1993
	Re-written to use the errmap structure.

************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "dtetlib.h"
#include "ltoa.h"
#include "errmap.h"

/*
**	tet_errname() - return printable representation of error number
*/

char *tet_errname(errnum)
register int errnum;
{
	register struct errmap *ep, *ee;
	static char fmt[] = "Error %d";
	static char text[sizeof fmt + LNUMSZ];

	for (ep = tet_errmap, ee = &tet_errmap[tet_Nerrmap]; ep < ee; ep++)
		if (errnum == ep->em_errno)
			return(ep->em_errname);

	(void) sprintf(text, fmt, errnum);
	return(text);
}
