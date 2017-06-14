/*
 *      SCCS:  @(#)codesfile.c	1.9 (99/09/03) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1992 X/Open Company Limited
 * (C) Copyright 1994 UniSoft Limited
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
static char sccsid[] = "@(#)codesfile.c	1.9 (99/09/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)codesfile.c	1.9 99/09/03 TETware release 3.8
NAME:		codesfile.c
PRODUCT:	TETware
AUTHOR:		David Sawyer, UniSoft Ltd.
DATE CREATED:	August 1992

DESCRIPTION:
	tet result codes file name store and return

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., November 1993
	enhancements for FIFO transport interface

	Andrew Dingwall, UniSoft Ltd., August 1996
	moved rescode stuff to dtet2lib


************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <time.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "avmsg.h"
#include "ltoa.h"
#include "xresd.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

static char *codesfile;			/* the stored config file name */


/*
**	op_codesf() - store a tet result codes file name
*/

void op_codesf(pp)
register struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* make sure that client is MTCC */
	if (pp->ptr_ptype != PT_MTCC) {
		pp->ptm_rc = ER_PERM;
		return;
	}

	/* make sure that we only do this once */
	if (codesfile) {
		pp->ptm_rc = ER_DONE;
		return;
	}

	/* do some sanity checks on the request message */
	if (OP_CODESF_NCODESF(mp) != 1) {
		pp->ptm_rc = ER_INVAL;
		return;
	}
	if (!AV_CODESF(mp, 0) || !*AV_CODESF(mp, 0)) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	/* free the existing tet result codes file and store the new one */
	if ((codesfile = tet_strstore(AV_CODESF(mp, 0))) == (char *) 0) {
		pp->ptm_rc = ER_ERR;
		return;
	}
	TRACE2(tet_Txresd, 4, "receive result codes file name = \"%s\"",
		codesfile);

	/* read in the results codes */
	if (tet_readrescodes(codesfile) < 0) {
		pp->ptm_rc = ER_ERR;
		return;
	}

	/* all ok so return success */
	pp->ptm_rc = ER_OK;
}

