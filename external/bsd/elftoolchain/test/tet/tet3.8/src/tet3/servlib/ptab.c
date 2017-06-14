/*
 *      SCCS:  @(#)ptab.c	1.7 (99/09/02) 
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
static char sccsid[] = "@(#)ptab.c	1.7 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)ptab.c	1.7 99/09/02 TETware release 3.8
NAME:		ptab.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	process table management functions

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 

************************************************************************/

#include <stdlib.h>
#include <time.h>
#include <sys/types.h>
#include <errno.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "error.h"
#include "bstring.h"
#include "llist.h"
#include "server.h"
#include "tslib.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

struct ptab *tet_ptab;			/* ptr to start of process table */


/*
**	tet_ptalloc() - allocate a process table entry and return a pointer
**		thereto, or (struct ptab *) 0 on error
**
**
*/

TET_IMPORT struct ptab *tet_ptalloc()
{
	register struct ptab *pp;

	/* allocate transport-independent data area */
	errno = 0;
	if ((pp = (struct ptab *) malloc(sizeof *pp)) == (struct ptab *) 0) {
		error(errno, "can't malloc ptab entry", (char *) 0);
		return((struct ptab *) 0);
	}
	TRACE2(tet_Tbuf, 6, "allocate ptab = %s", tet_i2x(pp));
	bzero((char *) pp, sizeof *pp);

	/* call the routine to allocate transport-specific data */
	if (tet_ts_ptalloc(pp) < 0) {
		tet_ptfree(pp);
		return((struct ptab *) 0);
	}

	/* call the routine to allocate server-specific data space */
	if (tet_ss_ptalloc(pp) < 0) {
		tet_ptfree(pp);
		return((struct ptab *) 0);
	}

	/* initialise variables */
	pp->pt_next = pp->pt_last = (struct ptab *) 0;
	pp->pt_magic = PT_MAGIC;
	pp->ptr_sysid = -1;
	pp->ptr_pid = -1L;
	pp->ptr_ptype = PT_NOPROC;
	pp->pt_state = PS_IDLE;

	return(pp);
}

/*
**	tet_ptfree() - free ptab element memory
*/

void tet_ptfree(pp)
register struct ptab *pp;
{
	TRACE2(tet_Tbuf, 6, "free ptab = %s", tet_i2x(pp));

	if (pp) {
		tet_ts_ptfree(pp);
		tet_ss_ptfree(pp);
		if (pp->ptm_data) {
			TRACE2(tet_Tbuf, 6, "free ptmdata = %s",
				tet_i2x(pp->ptm_data));
			free(pp->ptm_data);
		}
		free((char *) pp);
	}
}

/*
**	tet_ptadd() - insert an element in the ptab list
*/

void tet_ptadd(pp)
struct ptab *pp;
{
	tet_listinsert((struct llist **) &tet_ptab, (struct llist *) pp);
}

/*
**	tet_ptrm() - remove an element from the ptab list
*/

void tet_ptrm(pp)
struct ptab *pp;
{
	tet_listremove((struct llist **) &tet_ptab, (struct llist *) pp);
}

