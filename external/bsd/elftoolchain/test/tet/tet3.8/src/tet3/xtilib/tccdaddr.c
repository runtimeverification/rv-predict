/*
 *      SCCS:  @(#)tccdaddr.c	1.9 (99/09/03) 
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
static char sccsid[] = "@(#)tccdaddr.c	1.9 (99/09/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tccdaddr.c	1.9 99/09/03 TETware release 3.8
NAME:		tccdaddr.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Ltd.
DATE CREATED:	April 1993

DESCRIPTION:

	function to determine tccd XTI address

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., December 1993
	added malloc tracing

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.

************************************************************************/

#include <stdlib.h>
#include <string.h>
#include <xti.h>
#include <errno.h>
#include <sys/types.h>
#include <time.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "tptab_xt.h"
#include "sysent.h"
#include "error.h"
#include "ltoa.h"
#include "xtilib_xt.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_gettccdaddr() - look up the XTI address for a TCCD
**		and store it in the related tptab entry
**
**	return 0 if successful or -1 on error
*/

TET_IMPORT int tet_gettccdaddr(pp)
struct ptab *pp;
{
	register struct sysent *sp;
	register struct netbuf *np;
	register struct tptab *tp = (struct tptab *) pp->pt_tdata;

	/* look up the host name in the systems file */
	errno = 0;
	if ((sp = tet_libgetsysbyid(pp->ptr_sysid)) == (struct sysent *) 0) {
		error(errno, "can't get systems file entry for sysid",
			tet_i2a(pp->ptr_sysid));
		return(-1);
	}

	if ((np = tet_lname2addr(sp->sy_tccd)) == (struct netbuf *)0) {
		error(0, "can't convert STCC address for host", sp->sy_name);
		return (-1);
	}

	errno = 0;
	if ((tp->tp_call.buf = (char *)malloc((size_t)np->maxlen)) == (char *)0) {
		error(errno, "can't malloc STCC address buffer", (char *)0);
		return (-1);
	}
	TRACE2(tet_Tbuf, 6, "allocate STCC address buffer = %s",
		tet_i2x(tp->tp_call.buf));

	/* all ok so fill in the address details */
	tp->tp_call.maxlen	= np->maxlen;
	tp->tp_call.len		= np->len;
	(void) memcpy(tp->tp_call.buf, np->buf, (size_t)np->len);

	return(0);
}

