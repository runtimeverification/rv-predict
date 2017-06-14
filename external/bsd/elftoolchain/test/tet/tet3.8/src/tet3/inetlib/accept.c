/*
 *      SCCS:  @(#)accept.c	1.9 (03/08/28) 
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
static char sccsid[] = "@(#)accept.c	1.9 (03/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)accept.c	1.9 03/08/28 TETware release 3.8
NAME:		accept.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to accept a new INET connection

MODIFICATIONS:
	Matthew Hails, The Open Group, August 2003
	Modified address_len argument to accept() to use SOCKLEN_T type.

************************************************************************/

#include <errno.h>
#include <time.h>
#include <sys/types.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <winsock.h>
#else		/* -END-WIN32-CUT- */
#  include <sys/uio.h>
#  include <sys/socket.h>
#  include <netinet/in.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "tptab_in.h"
#include "ltoa.h"
#include "error.h"
#include "tslib.h"
#include "server.h"
#include "server_in.h"
#include "inetlib_in.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_ts_accept() - accept a new connection on a listening socket
**		and allocate a ptab entry for it
*/

void tet_ts_accept(lsd)
SOCKET lsd;
{
	struct sockaddr_in sin;
	register struct ptab *pp;
	register struct tptab *tp;
	register SOCKET nsd;
	SOCKLEN_T len;
	int err;

	TRACE2(tet_Tio, 4, "accept connection on sd %s", tet_i2a(lsd));

	/* allocate a proc table entry for this connection */
	if ((pp = tet_ptalloc()) == (struct ptab *) 0)
		return;

	/* accept the connection */
	do {
		err = 0;
		len = (SOCKLEN_T)(sizeof sin);
		if ((nsd = accept(lsd, (struct sockaddr *) &sin, &len)) == INVALID_SOCKET)
			err = SOCKET_ERRNO;
	} while (nsd < 0 && err == SOCKET_EINTR);

	if (nsd == INVALID_SOCKET) {
		error(errno, "accept() failed on sd", tet_i2a(lsd));
		tet_ptfree(pp);
		return;
	}

	TRACE2(tet_Tio, 4, "accept: new sd = %s", tet_i2a(nsd));

	/* store the remote address */
	tp = (struct tptab *) pp->pt_tdata;
	tp->tp_sd = nsd;
	tp->tp_sin = sin;

	/* call server-specific routine to massage socket */
	if (tet_ss_tsafteraccept(pp) < 0) {
		tet_ts_dead(pp);
		tet_ptfree(pp);
		return;
	}

	/* pass the new ptab entry to the server for registration */
	tet_ss_newptab(pp);

	pp->pt_flags |= PF_CONNECTED;
}

