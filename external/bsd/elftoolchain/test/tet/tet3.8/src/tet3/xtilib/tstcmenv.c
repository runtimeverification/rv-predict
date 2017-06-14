/*
 *      SCCS:  @(#)tstcmenv.c	1.7 (99/09/03) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1993 X/Open Company Limited
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
static char sccsid[] = "@(#)tstcmenv.c	1.7 (99/09/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tstcmenv.c	1.7 99/09/03 TETware release 3.8
NAME:		tstcmenv.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Ltd.
DATE CREATED:	April 1993

DESCRIPTION:
	function to put transport-specific arguments in the environment to be
	received by the tcm

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <time.h>
#include <xti.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "tptab_xt.h"
#include "ltoa.h"
#include "dtetlib.h"
#include "tslib.h"
#include "xtilib_xt.h"


/*
**	tet_ts_tcmputenv() - put ts args in the environment for TCM
**
**	return 0 if successful or -1 on error
*/

int tet_ts_tcmputenv()
{
	register char *p1, *p2;
	register int first;
	register struct netbuf *ap;
	char envstring[1024];
	char addrbuff[1024];
	static char envname[] = "TET_TSARGS=";
	static char *laststring;

	/* start the environment string */
	first = 1;
	p1 = envstring;
	for (p2 = envname; *p2; p2++)
		*p1++ = *p2;

	/* see if there is any tsinfo for syncd */
	ap = tet_sdptab ? &((struct tptab *)tet_sdptab->pt_tdata)->tp_call : (struct netbuf *)0;
	if (ap) {
		p1 += tet_mkoptarg(p1, 'y', tet_addr2lname(ap), first);
		first = 0;
	}

	/* see if there is any tsinfo for xresd */
	ap = tet_xdptab ? &((struct tptab *)tet_xdptab->pt_tdata)->tp_call : (struct netbuf *)0;
	if (ap) {
		p1 += tet_mkoptarg(p1, 'x', tet_addr2lname(ap), first);
	}

	/* Also want the transport provider name and mode */
	p1 +=  tet_mkoptarg(p1, 'P', tet_tpname, first);
	p1 +=  tet_mkoptarg(p1, 'M', tet_i2a(tet_tpi_mode), first);

	*p1 = '\0';

	/* store the string in static memory and put it in the environment */
	if ((p1 = tet_strstore(envstring)) == (char *) 0 || tet_putenv(p1) < 0)
		return(-1);

	/* free any previous one and remember the new one if successful */
	if (laststring) {
		TRACE2(tet_Tbuf, 6, "free old ts env string = %s",
			tet_i2x(laststring));
		free(laststring);
	}
	laststring = p1;

	return(0);
}

