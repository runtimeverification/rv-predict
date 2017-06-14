/*
 *      SCCS:  @(#)avmsg.c	1.8 (98/08/28) 
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
static char sccsid[] = "@(#)avmsg.c	1.8 (98/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)avmsg.c	1.8 98/08/28 TETware release 3.8
NAME:		avmsg.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	functions to convert a DTET interprocess character string message
	between internal and machine-independent format

MODIFICATIONS:
	Geoff Clare, UniSoft Ltd., August 1996
	Missing <string.h>.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#ifndef TET_LITE	/* -START-LITE-CUT- */

#include <stdio.h>
#include <string.h>
#include "dtmac.h"
#include "ldst.h"
#include "dtmsg.h"
#include "avmsg.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

static struct stdesc st[] = {
	AVMSG_DESC
};

static struct stdesc offst[] = {
	OFFSET_DESC
};

static short fixed = -1;


/* static function declarations */
static int bs2avmargv PROTOLIST((char *, char **, int, int));
static void stinit PROTOLIST((void));


static void stinit()
{
	register struct avmsg *ap = (struct avmsg *) 0;
	register int n = 0;

	AVMSG_INIT(st, ap, n, fixed);
	OFFSET_INIT(offst);
}

/*
**	tet_avmsg2bs() - put an avmsg and associated strings on a byte stream
*/

TET_IMPORT int tet_avmsg2bs(from, to)
register struct avmsg *from;
register char *to;
{
	register int n;
	register char *p, *sp;
	unsigned short offset;

	if (fixed < 0)
		stinit();

	/* convert the fixed part */
	sp = to + tet_st2bs((char *) from, to, st, fixed);

	/* allocate space on the byte stream for the argv list */
	offset = 0;
	for (n = 0; n < (int) from->av_argc; n++) {
		sp += tet_st2bs((char *) &offset,
			to + AV_AVMSGSZ(n), &offst[0], 1);
	}

	/* sp points to the place to store the strings themselves -
		copy the strings and fill in the offsets */
	for (n = 0; n < (int) from->av_argc; n++)
		if (from->av_argv[n]) {
			offset = sp - to;
			(void) tet_st2bs((char *) &offset,
				to + AV_AVMSGSZ(n), &offst[0], 1);
			for (p = from->av_argv[n]; *p; p++)
				*sp++ = *p;
			*sp++ = '\0';
		}

	return(sp - to);
}

/*
**	tet_bs2avmsg() - convert an avmsg to internal format
**
**	return number of bytes in the avmsg result
**
**	on return, av_argv contains the string pointers
**	the strings themselves are not copied from the machine-independent
**	area
*/

TET_IMPORT int tet_bs2avmsg(from, fromlen, to, tolen)
register char *from;
register int fromlen;
register struct avmsg **to;
register int *tolen;
{
	if (fixed < 0)
		stinit();

	/* make sure the buffer is big enough for a minimal message */
	if (BUFCHK((char **) to, tolen, avmsgsz(1)) < 0)
		return(-1);

	/* convert the fixed part */
	if (tet_bs2st(from, (char *) *to, st, fixed,
		TET_MIN(fromlen, AV_ARGVSTART)) < 0)
			return(-1);

	/* make sure the buffer is big enough for the actual message */
	if ((int) (*to)->av_argc > 1 &&
		BUFCHK((char **) to, tolen, avmsgsz((*to)->av_argc)) < 0)
			return(-1);

	/* then convert the variable part */
	fromlen -= AV_ARGVSTART;
	if (bs2avmargv(from, (*to)->av_argv, (int) (*to)->av_argc,
		TET_MIN((int) (*to)->av_argc * AV_ARGVSZ, fromlen)) < 0)
			return(-1);

	/* return the number of bytes in the result */
	return(avmsgsz((*to)->av_argc));
}

/*
**	bs2avmargv() - convert argv byte-stream offsets to internal
**		addresses
**
**	note it is only the offsets that are copied to the internal format
**	area, not the strings themselves
*/

static int bs2avmargv(from, to, nargv, len)
register char *from;
register char **to;
register int nargv, len;
{
	register int n;
	unsigned short offset;

	if (fixed < 0)
		stinit();

	for (n = 0; n < nargv && len > 0; n++) {
		if (tet_bs2st(from + AV_AVMSGSZ(n), (char *) &offset,
			&offst[0], 1, TET_MIN(AV_ARGVSZ, len)) < 0)
				break;
		*(to + n) = offset ? from + offset : (char *) 0;
		len -= AV_ARGVSZ;
	}

	return((len == 0 && n == nargv) ? 0 : -1);
}

/*
**	tet_avmsgbslen() - return length of an avmsg on a byte stream
*/

TET_IMPORT int tet_avmsgbslen(mp)
register struct avmsg *mp;
{
	register int len, n;

	len = AV_AVMSGSZ(mp->av_argc);
	for (n = 0; n < (int) mp->av_argc; n++)
		if (mp->av_argv[n])
			len += strlen(mp->av_argv[n]) + 1;

	return(len);
}

#else	/* -END-LITE-CUT- */

int tet_avmsg_c_not_used;

#endif /* !TET_LITE */	/* -LITE-CUT-LINE- */

