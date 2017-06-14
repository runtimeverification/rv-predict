/*
 *      SCCS:  @(#)tptab.c	1.6 (99/09/03) 
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
static char sccsid[] = "@(#)tptab.c	1.6 (99/09/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tptab.c	1.6 99/09/03 TETware release 3.8
NAME:		tptab.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Ltd.
DATE CREATED:	April 1993

DESCRIPTION:
	functions to administer XTI transport-specific process table data

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <xti.h>
#include <errno.h>
#include <sys/types.h>
#include <time.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ltoa.h"
#include "ptab.h"
#include "tptab_xt.h"
#include "error.h"
#include "bstring.h"
#include "dtetlib.h"
#include "tslib.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/*
**	tet_ts_ptalloc() - allocate transport-specific data element in a ptab
**		structure
**
**	return 0 if successful or -1 on error
*/

int tet_ts_ptalloc(pp)
struct ptab *pp;
{
	register struct tptab *tp;

	errno = 0;
	if ((tp = (struct tptab *) malloc(sizeof *tp)) == (struct tptab *) 0) {
		error(errno, "can't get memory for ts data", (char *) 0);
		pp->pt_tdata = (char *) 0;
		return(-1);
	}
	TRACE2(tet_Tbuf, 6, "allocate tptab = %s", tet_i2x(tp));
	bzero((char *) tp, sizeof *tp);

	tp->tp_fd = -1;
	if (BUFCHK(&tp->tp_buf, &tp->tp_len, DM_HDRSZ) < 0) {
		tet_ts_ptfree(pp);
		return(-1);
	}
	tp->tp_ptr = tp->tp_buf;

	pp->pt_tdata = (char *) tp;
	return(0);
}

/*
**	tet_ts_ptfree() - free transport-specific data element in a
**		ptab structure
*/

void tet_ts_ptfree(pp)
struct ptab *pp;
{
	register struct tptab *tp = (struct tptab *) pp->pt_tdata;

	TRACE2(tet_Tbuf, 6, "free tptab = %s", tet_i2x(tp));

	if (tp) {
		if (tp->tp_buf) {
			TRACE2(tet_Tbuf, 6, "free tpbuf = %s",
				tet_i2x(tp->tp_buf));
			free(tp->tp_buf);
		}
		if (tp->tp_call.buf) {
			TRACE2(tet_Tbuf, 6, "free tp call buf = %s",
				tet_i2x(tp->tp_call.buf));
			free(tp->tp_call.buf);
		}
		free((char *) tp);
		pp->pt_tdata = (char *) 0;
	}
}

