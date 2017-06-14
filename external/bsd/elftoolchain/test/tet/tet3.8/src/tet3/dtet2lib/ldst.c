/*
 *      SCCS:  @(#)ldst.c	1.7 (96/11/04) 
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
static char sccsid[] = "@(#)ldst.c	1.7 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)ldst.c	1.7 96/11/04 TETware release 3.8
NAME:		ldst.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	low-level functions to convert data structures between internal and
	machine-independent format

MODIFICATIONS:

************************************************************************/

#ifndef TET_LITE	/* -START-LITE-CUT- */

#include <stdio.h>
#include "dtmac.h"
#include "ltoa.h"
#include "ldst.h"
#include "dtmsg.h"
#include "error.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* static function declarations */
static int bs2char PROTOLIST((char *, char *, int));
static int bs2long PROTOLIST((char *, long *, int));
static int bs2short PROTOLIST((char *, short *, int));
static int bs2ushort PROTOLIST((char *,  unsigned short *, int));
static int char2bs PROTOLIST((char *, char *, int));
static int long2bs PROTOLIST((long *, char *, int));
static int short2bs PROTOLIST((short *, char *, int));


/*
**	tet_bs2st() - convert a structure from machine-independent to
**		internal format
**
**	return 0 if the expected number of bytes was received, -1 otherwise
**	(this is mainly a defence against inconsistencies between the
**	structures and their stdesc descriptions)
*/

int tet_bs2st(from, to, st, nst, len)
char *from, *to;
register struct stdesc *st;
register int nst, len;
{
	register char *fp, *tp;
	register int n, count;

	for (count = 0; nst > 0 && count < len; nst--, st++) {
		fp = from + st->st_bsoff;
		tp = to + st->st_stoff;
		n = st->st_type & ST_COUNTMASK;
		switch (st->st_type & ST_TYPEMASK) {
		case ST_CHARTYPE:
			count += bs2char(fp, tp, n);
			break;
		case ST_SHORTTYPE:
			count += bs2short(fp, (short *) tp, n);
			break;
		case ST_USHORTTYPE:
			count += bs2ushort(fp, (unsigned short *) tp, n);
			break;
		case ST_LONGTYPE:
			count += bs2long(fp, (long *) tp, n);
			break;
		default:
			error(0, "unexpected type", tet_i2o(st->st_type));
			break;
		}
	}

	if (len != count) {
		error(0, "internal error: tet_bs2st() wanted", tet_i2a(count));
		error(0, "tet_bs2st() received", tet_i2a(len));
		return(-1);
	}

	return(0);
}

/*
**	tet_st2bs() - convert a structure to machine-independent format
**
**	return the number of bytes occupied by the result
*/

int tet_st2bs(from, to, st, nst)
char *from, *to;
register struct stdesc *st;
register int nst;
{
	register char *fp, *tp;
	register int n, count;

	for (count = 0; nst > 0; nst--, st++) {
		fp = from + st->st_stoff;
		tp = to + st->st_bsoff;
		n = st->st_type & ST_COUNTMASK;
		switch (st->st_type & ST_TYPEMASK) {
		case ST_CHARTYPE:
			count += char2bs(fp, tp, n);
			break;
		case ST_SHORTTYPE:
		case ST_USHORTTYPE:
			count += short2bs((short *) fp, tp, n);
			break;
		case ST_LONGTYPE:
			count += long2bs((long *) fp, tp, n);
			break;
		default:
			error(0, "unexpected type", tet_i2o(st->st_type));
			break;
		}
	}

	return(count);
}

/*
**	bs2char(), bs2short(), bs2ushort, bs2long() - convert arrays of
**		objects from machine-independent to internal format
**
**	return the number of bytes of machine-independent data converted
*/

static int bs2char(from, to, n)
register char *from;
register char *to;
register int n;
{
	register int count = n;

	while (--n >= 0)
		*to++ = *from++;

	return(count);
}

static int bs2short(from, to, n)
register char *from;
register short *to;
register int n;
{
	register int count = n * SHORTSIZE;

	while (--n >= 0) {
		*to++ = ld16(from);
		from += SHORTSIZE;
	}

	return(count);
}

static int bs2ushort(from, to, n)
register char *from;
register unsigned short *to;
register int n;
{
	register int count = n * SHORTSIZE;

	while (--n >= 0) {
		*to++ = ld16u(from);
		from += SHORTSIZE;
	}

	return(count);
}

static int bs2long(from, to, n)
register char *from;
register long *to;
register int n;
{
	register int count = n * LONGSIZE;

	while (--n >= 0) {
		*to++ = ld32(from);
		from += LONGSIZE;
	}

	return(count);
}

/*
**	char2bs(), short2bs(), long2bs() - convert arrays of
**		objects from internal to machine-independent format
**
**	return the number of bytes occupied by the machine-independent data
*/

static int char2bs(from, to, n)
register char *from, *to;
register int n;
{
	register int count = n;

	while (--n >= 0)
		*to++ = *from++;

	return(count);
}

static int short2bs(from, to, n)
register short *from;
register char *to;
register int n;
{
	register int count = n * SHORTSIZE;

	while (--n >= 0) {
		st16(*from, to);
		from++;
		to += SHORTSIZE;
	}

	return(count);
}

static int long2bs(from, to, n)
register long *from;
register char *to;
register int n;
{
	register int count = n * LONGSIZE;

	while (--n >= 0) {
		st32(*from, to);
		from++;
		to += LONGSIZE;
	}

	return(count);
}

#else	/* -END-LITE-CUT- */

int tet_ldst_c_not_empty;

#endif /* !TET_LITE */	/* -LITE-CUT-LINE- */

