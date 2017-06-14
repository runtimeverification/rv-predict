/*
 *      SCCS:  @(#)tsinfo.c	1.7 (99/09/03) 
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
static char sccsid[] = "@(#)tsinfo.c	1.7 (99/09/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tsinfo.c	1.7 99/09/03 TETware release 3.8
NAME:		tsinfo.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Ltd.
DATE CREATED:	May 1993

DESCRIPTION:
	functions to convert DTET interprocess XTI transport-specific
	information messages between internal and machine-independent format

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#include <stdio.h>
#include <sys/types.h>
#include <time.h>
#include <xti.h>
#include "dtmac.h"
#include "error.h"
#include "ltoa.h"
#include "ldst.h"
#include "dtmsg.h"
#include "ptab.h"
#include "xtilib_xt.h"
#include "tsinfo_xt.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

static struct stdesc *st = (struct stdesc *)0;
#ifdef TCPTPI
static struct stdesc st_in[] = {
	TSINFO_INET_DESC
};
#endif
#ifdef OSITPI
static struct stdesc st_osi[] = {
	TSINFO_OSICO_DESC
};
#endif

static short nst = -1;


/* static function declarations */
static void stinit PROTOLIST((void));


static void stinit()
{
	register struct tsinfo *sp = (struct tsinfo *) 0;
	register int n = 0;

	switch (tet_tpi_mode) {
#ifdef TCPTPI
	case TPI_TCP:
		TSINFO_INET_INIT(st_in, sp, n, nst);
		st = st_in;
		break;
#endif
#ifdef OSITPI
	case TPI_OSICO:
		TSINFO_OSICO_INIT(st_osi, sp, n, nst);
		st = st_osi;
		break;
#endif

	default:
		fatal(0, "invalid TPI mode, or not defined", tet_i2a(tet_tpi_mode));
		break;

	}
}

TET_IMPORT int tet_tsinfo2bs(from, to)
register struct tsinfo *from;
register char *to;
{
	if (nst < 0)
		stinit();

	return(tet_st2bs((char *) from, to, st, nst));
}

/*
**	tet_bs2tsinfo() - convert a tsinfo message to internal format
*/

int tet_bs2tsinfo(from, fromlen, to, tolen)
register char *from;
register int fromlen;
register struct tsinfo **to;
register int *tolen;
{
	if (nst < 0)
		stinit();

	if (BUFCHK((char **) to, tolen, sizeof **to) < 0)
		return(-1);

	if (tet_bs2st(from, (char *) *to, st, nst, fromlen) < 0)
		return(-1);

	return(sizeof **to);
}

