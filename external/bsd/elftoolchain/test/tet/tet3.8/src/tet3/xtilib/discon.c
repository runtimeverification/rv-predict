/*
 *      SCCS:  @(#)discon.c	1.4 (99/09/03) 
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
static char sccsid[] = "@(#)discon.c	1.4 (99/09/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)discon.c	1.4 99/09/03 TETware release 3.8
NAME:		disc.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, Unisoft Ltd.
DATE CREATED:	May 1993

DESCRIPTION:
	required transport-specific library interface

	function to support tet_disconnect() for the XTI transport interface

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., December 1993
	moved disconnect stuff from connect.c to here


************************************************************************/

#include <sys/types.h>
#include <time.h>
#include <xti.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "tptab_xt.h"
#include "tslib.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif

/*
**      tet_ts_disconnect() - transport-specific disconnect routine -
**		called from tet_disconnect()
**
**	note that this function can be called even when the calling process
**	is not connected to the specified server
*/

void tet_ts_disconnect(pp)
struct ptab *pp;
{
        register struct tptab *tp = (struct tptab *) pp->pt_tdata;

	TRACE2(tet_Tio, 4, "tet_ts_disconnect: close fd %s",
		tet_i2a(tp->tp_fd));

	if (tp->tp_fd >= 0) {
		(void) t_close(tp->tp_fd);
		tp->tp_fd = -1;
	}

	pp->pt_flags = (pp->pt_flags & ~(PF_LOGGEDON | PF_CONNECTED)) | PF_LOGGEDOFF;
	pp->pt_state = PS_IDLE;
}

