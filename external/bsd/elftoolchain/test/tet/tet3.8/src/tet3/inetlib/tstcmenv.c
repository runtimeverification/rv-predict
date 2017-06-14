/*
 *      SCCS:  @(#)tstcmenv.c	1.10 (99/09/02) 
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
static char sccsid[] = "@(#)tstcmenv.c	1.10 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tstcmenv.c	1.10 99/09/02 TETware release 3.8
NAME:		tstcmenv.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to put transport-specific arguments in the environment to be
	received by the tcm

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., March 1997
	remove #ifndef __hpux from #include <arpa/inet.h>
	since current HP-UX implementations now have this file

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <sys/types.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <winsock.h>
#else		/* -END-WIN32-CUT- */
#  include <netinet/in.h>
#  include <arpa/inet.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "tptab_in.h"
#include "ltoa.h"
#include "dtetlib.h"
#include "tslib.h"


/*
**	tet_ts_tcmputenv() - put ts args in the environment for TCM
**
**	return 0 if successful or -1 on error
*/

int tet_ts_tcmputenv()
{
	register char *p1, *p2;
	register int first;
	register struct sockaddr_in *ap;
	char envstring[1024];
	static char envname[] = "TET_TSARGS=";
	static char *laststring;

	/* start the environment string */
	first = 1;
	p1 = envstring;
	for (p2 = envname; *p2; p2++)
		*p1++ = *p2;

	/* see if there is any tsinfo for syncd */
	ap = tet_sdptab ? &((struct tptab *) tet_sdptab->pt_tdata)->tp_sin :
		(struct sockaddr_in *) 0;
	if (ap && ap->sin_port) {
		p1 += tet_mkoptarg(p1, 'y', inet_ntoa(ap->sin_addr), first);
		*p1++ = ',';
		for (p2 = tet_i2a(ntohs(ap->sin_port)); *p2; p2++)
			*p1++ = *p2;
		first = 0;
	}

	/* see if there is any tsinfo for xresd */
	ap = tet_xdptab ? &((struct tptab *) tet_xdptab->pt_tdata)->tp_sin :
		(struct sockaddr_in *) 0;
	if (ap && ap->sin_port) {
		p1 += tet_mkoptarg(p1, 'x', inet_ntoa(ap->sin_addr), first);
		*p1++ = ',';
		for (p2 = tet_i2a(ntohs(ap->sin_port)); *p2; p2++)
			*p1++ = *p2;
	}

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

