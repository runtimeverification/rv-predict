/*
 *      SCCS:  @(#)valmsg.c	1.8 (98/08/28) 
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
static char sccsid[] = "@(#)valmsg.c	1.8 (98/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)valmsg.c	1.8 98/08/28 TETware release 3.8
NAME:		valmsg.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to convert DTET interprocess numeric value message between
	internal and machine-independent format

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., October 1994
	added support for sync message data on the end of a numeric
	value message

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 

************************************************************************/

#ifndef TET_LITE	/* -START-LITE-CUT- */

#include <stdio.h>
#include "dtmac.h"
#include "ldst.h"
#include "dtmsg.h"
#include "valmsg.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

static struct stdesc st[] = {
	VALMSG_DESC
};

static short nst = -1;
static short fixed = -1;

/* static function declarations */
static int bs2vsmsg PROTOLIST((char *, int, struct valmsg **, int *, int));
static void stinit PROTOLIST((void));


static void stinit()
{
	register struct valmsg *sp = (struct valmsg *) 0;
	register int n = 0;

	VALMSG_INIT(st, sp, n, nst, fixed);
}

/*
**	tet_valmsg2bs() - convert a valmsg to machine-independent format
**
**	return the number of bytes occupied by the result
*/

int tet_valmsg2bs(from, to)
register struct valmsg *from;
register char *to;
{
	register int count;
	struct stdesc tmp;

	if (nst < 0)
		stinit();

	/* convert the fixed part */
	count = tet_st2bs((char *) from, to, st, fixed);

	/* then convert the variable part */
	tmp = st[fixed];
	tmp.st_type = (tmp.st_type & ST_TYPEMASK) |
		(from->vm_nvalue & ST_COUNTMASK);
	count += tet_st2bs((char *) from, to + VM_VALUESTART, &tmp, 1);

	return(count);
}

/*
**	tet_bs2valmsg() - convert a valmsg message to internal format
**
**	return the number of bytes in the valmsg result, or -1 on error
*/

TET_IMPORT int tet_bs2valmsg(from, fromlen, to, tolen)
char *from;
int fromlen;
struct valmsg **to;
int *tolen;
{
	return(bs2vsmsg(from, fromlen, to, tolen, 0));
}

/*
**	bs2vsmsg() - common subroutine for tet_bs2valmsg() and tet_bs2synmsg()
*/

static int bs2vsmsg(from, fromlen, to, tolen, smproc)
register char *from;
register int fromlen;
register struct valmsg **to;
register int *tolen;
int smproc;
{
	struct stdesc tmp;
	register int bslen, vmlen;

	if (nst < 0)
		stinit();

	/* make sure that the buffer is big enough for a minimal message */
	if (BUFCHK((char **) to, tolen, valmsgsz(1)) < 0)
		return(-1);

	/* convert the fixed part */
	if (tet_bs2st(from, (char *) *to, st, fixed,
		TET_MIN(fromlen, VM_VALUESTART)) < 0)
			return(-1);

	/* make sure that the buffer is big enough for the actual message */
	if ((int) (*to)->vm_nvalue > 1 &&
		BUFCHK((char **) to, tolen, valmsgsz((*to)->vm_nvalue)) < 0)
			return(-1);

	/* then convert the variable part */
	tmp = st[fixed];
	tmp.st_type = (tmp.st_type & ST_TYPEMASK) |
		((*to)->vm_nvalue & ST_COUNTMASK);
	bslen = fromlen - VM_VALUESTART;
	if (smproc &&
		(vmlen = VM_VALMSGSZ((*to)->vm_nvalue) - VM_VALUESTART) < bslen)
			bslen = vmlen;
	if (tet_bs2st(from + VM_VALUESTART, (char *) *to, &tmp, 1, bslen) < 0)
		return(-1);

	/* return the number of bytes in the result */
	return(valmsgsz((*to)->vm_nvalue));
}

/*
**	tet_synmsg2bs() - convert a synmsg to machine-independent format
**
**	return the number of bytes occupied by the result
*/

int tet_synmsg2bs(from, to)
register struct valmsg *from;
register char *to;
{
	register int count;
	register int dlen;
	struct stdesc tmp;

	/* convert the valmsg part */
	count = tet_valmsg2bs(from, to);

	/* convert the sync message data (if any) */
	if ((dlen = VM_MSDLEN(from)) > 0) {
		tmp.st_type = ST_CHAR(dlen);
		tmp.st_bsoff = count;
		tmp.st_stoff = VM_MSDATA(from) - (char *) from;
		count += tet_st2bs((char *) from, to, &tmp, 1);
	}

	return(count);
}

/*
**	tet_bs2synmsg() - convert a synmsg message to internal format
**
**	return the number of bytes in the valmsg result, or -1 on error
*/

TET_IMPORT int tet_bs2synmsg(from, fromlen, to, tolen)
register char *from;
register int fromlen;
register struct valmsg **to;
register int *tolen;
{
	register int count;
	register int dlen;
	struct stdesc tmp;

	/* convert the valmsg part */
	if ((count = bs2vsmsg(from, fromlen, to, tolen, 1)) < 0)
		return(-1);

	/* return now if there is no sync message data to convert */
	if (fromlen <= (int) VM_VALMSGSZ((*to)->vm_nvalue))
		return(count);

	/* grow the buffer if necessary to accomodate the sync message data */
	dlen = (int) VM_MSDLEN(*to);
	if (BUFCHK((char **) to, tolen, synmsgsz((*to)->vm_nvalue, dlen)) < 0)
		return(-1);

	/* convert the sync message data */
	tmp.st_type = ST_CHAR(dlen);
	tmp.st_bsoff = VM_VALMSGSZ((*to)->vm_nvalue);
	tmp.st_stoff = VM_MSDATA(*to) - (char *) *to;
	if (tet_bs2st(from, (char *) *to, &tmp, 1,
		fromlen - VM_VALMSGSZ((*to)->vm_nvalue)) < 0)
			return(-1);

	return(synmsgsz((*to)->vm_nvalue, dlen));
}

#else	/* -END-LITE-CUT- */

int tet_valmsg_c_not_empty;

#endif /* !TET_LITE */	/* -LITE-CUT-LINE- */

