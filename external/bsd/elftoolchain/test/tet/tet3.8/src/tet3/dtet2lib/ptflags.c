/*
 *      SCCS:  @(#)ptflags.c	1.9 (02/05/15) 
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
static char sccsid[] = "@(#)ptflags.c	1.9 (02/05/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)ptflags.c	1.9 02/05/15 TETware release 3.8
NAME:		ptflags.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to return a printable representation of process table flags

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., November 1993
	enhancements for use with FIFO transport interface

	Andrew Dingwall, UniSoft Ltd., July 1998
	Only compile code in this file when building Distributed TETware.
 
************************************************************************/

#ifndef TET_LITE	/* -START-LITE-CUT- */

#include <time.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "ftoa.h"

/*
**	tet_ptflags() - return printable representation of ptab pt_flags value
*/

char *tet_ptflags(fval)
int fval;
{
	static struct flags flags[] = {
		{ PF_ATTENTION, "ATTENTION" },
		{ PF_INPROGRESS, "INPROGRESS" },
		{ PF_IODONE, "IODONE" },
		{ PF_IOERR, "IOERR" },
		{ PF_TIMEDOUT, "TIMEDOUT" },
		{ PF_CONNECTED, "CONNECTED" },
		{ PF_LOGGEDON, "LOGGEDON" },
		{ PF_LOGGEDOFF, "LOGGEDOFF" },
		{ PF_RCVHDR, "RCVHDR" },
		{ PF_SNDHDR, "SNDHDR" },
		{ PF_SERVER, "SERVER" },
		{ PF_NBIO, "NBIO" },
		{ PF_SERVWAIT, "SERVWAIT" }
	};

	return(tet_f2a(fval, flags, sizeof flags / sizeof flags[0]));
}

#else		/* -END-LITE-CUT- */

int tet_ptflags_c_not_used;

#endif		/* -LITE-CUT-LINE- */

