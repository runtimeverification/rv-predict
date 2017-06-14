/*
 *	SCCS: @(#)fake.c	1.4 (99/09/02)
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1996 X/Open Company Limited
 *
 * All rights reserved.  No part of this source code may be reproduced,
 * stored in a retrieval system, or transmitted, in any form or by any
 * means, electronic, mechanical, photocopying, recording or otherwise,
 * except as stated in the end-user licence agreement, without the prior
 * permission of the copyright owners.
 * A copy of the end-user licence agreement is contained in the file
 * Licence which accompanies this distribution.
 * 
 * X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
 * the UK and other countries.
 */

#ifndef lint
static char sccsid[] = "@(#)fake.c	1.4 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)fake.c	1.4 99/09/02 TETware release 3.8
NAME:		fake.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	dummy functions needed by the generic client/server code

MODIFICATIONS:

************************************************************************/

#ifndef TET_LITE	/* -START-LITE-CUT- */

#include <sys/types.h>
#include <time.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "error.h"
#include "server.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_ss_serverloop() - server-specific server loop
**
**	this may be called from tet_si_servwait() if non-blocking message i/o
**	would block
**
**	tcc does not do non-blocking i/o, so this should never occur
*/

int tet_ss_serverloop()
{
	error(0, "internal error - serverloop called!", (char *) 0);
	return(-1);
}

/*
**	tet_ss_process() - server-specific request process routine
**
**	would be called from tet_si_service() when state is PS_PROCESS
**
**	tcc only uses tet_si_clientloop() which itself returns as soon as a
**	process reaches this state, so tet_ss_process() should never be called
**/

void tet_ss_process(pp)
struct ptab *pp;
{
	error(0, "internal error - tet_ss_process called!",
		tet_r2a(&pp->pt_rid));
}

#else	/* -END-LITE-CUT- */

int tet_fake_c_not_empty;

#endif /* !TET_LITE */	/* -LITE-CUT-LINE- */

