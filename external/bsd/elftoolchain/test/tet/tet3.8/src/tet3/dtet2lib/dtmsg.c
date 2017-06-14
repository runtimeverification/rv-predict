/*
 *      SCCS:  @(#)dtmsg.c	1.6 (96/11/04) 
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
static char sccsid[] = "@(#)dtmsg.c	1.6 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)dtmsg.c	1.6 96/11/04 TETware release 3.8
NAME:		dtmsg.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	functions to convert a DTET interprocess message header between
	internal and machine-independent format

MODIFICATIONS:

************************************************************************/

#ifndef TET_LITE	/* -START-LITE-CUT- */

#include "dtmac.h"
#include "ldst.h"
#include "dtmsg.h"

static struct stdesc st[] = {
	DTMHDR_DESC
};

static short nst = -1;
static short stlen = -1;


/* static function declarations */
static void stinit PROTOLIST((void));


static void stinit()
{
	register struct dtmhdr *hp = (struct dtmhdr *) 0;
	register int n = 0;

	DTMHDR_INIT(st, hp, n, nst, stlen);
}

int tet_dtmhdr2bs(from, to)
struct dtmhdr *from;
char *to;
{
	if (nst < 0)
		stinit();

	return(tet_st2bs((char *) from, to, st, nst));
}

int tet_dmlen2bs(from, to)
int from;
char *to;
{
	struct dtmsg dummy;

	if (nst < 0)
		stinit();

	dummy.dm_len = (short) from;
	return(tet_st2bs((char *) &dummy, to, &st[stlen], 1));
}

int tet_bs2dtmhdr(from, to, len)
char *from;
struct dtmhdr *to;
int len;
{
	if (nst < 0)
		stinit();

	return(tet_bs2st(from, (char *) to, st, nst, len));
}

#else	/* -END-LITE-CUT- */

int tet_dtmsg_c_not_empty;

#endif /* !TET_LITE */	/* -LITE-CUT-LINE- */

