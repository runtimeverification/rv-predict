/*
 *      SCCS:  @(#)xresd_in.c	1.8 (99/09/03) 
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
static char sccsid[] = "@(#)xresd_in.c	1.8 (99/09/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)xresd_in.c	1.8 99/09/03 TETware release 3.8
NAME:		xresd_in.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	server-specific functions for INET xresd server

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., January 1994
	added transport-specific argument processing

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <time.h>
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
#include "error.h"
#include "server_in.h"
#include "inetlib_in.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* socket on which to listen */
#ifdef _WIN32	/* -START-WIN32-CUT- */
SOCKET tet_listen_sd = INVALID_SOCKET;
#else		/* -END-WIN32-CUT- */
SOCKET tet_listen_sd = 0;
#endif		/* -WIN32-CUT-LINE- */

/*
**	tet_ss_tsaccept() - server-specific accept() processing
*/

void tet_ss_tsaccept()
{
	/* accept the connection unless we are closing down */
	if (tet_listen_sd != INVALID_SOCKET)
		tet_ts_accept(tet_listen_sd);
}

/*
**	tet_ss_tsafteraccept() - server-specific things to do after an accept()
*/

int tet_ss_tsafteraccept(pp)
struct ptab *pp;
{
	/* establish non-blocking i/o on the connection */
	return(tet_ts_nbio(pp));
}

/*
**	ss_tsinitb4fork() - xresd transport-specific initialisation
*/

void ss_tsinitb4fork()
{
	/* arrange to accept incoming connections */
	tet_ts_listen(tet_listen_sd);
}

/*
**	ss_tsargproc() - xresd transport-specific command-line
**		argument processing
**
**	return 0 if only firstarg was used or 1 if both args were used
*/

int ss_tsargproc(firstarg, nextarg)
char *firstarg, *nextarg;
{
	register int rc = 0;

	switch (*(firstarg + 1)) {
#ifdef _WIN32	/* -START-WIN32-CUT- */
	case 'l':
                if (*(firstarg + 2))
                        tet_listen_sd = (SOCKET) atol(firstarg + 2);
                else {
                        tet_listen_sd = (SOCKET) atol(nextarg);
                        rc = 1;
                } 
		break;
#endif		/* -END-WIN32-CUT- */
	default:
		fatal(0, "unknown option:", firstarg);
		/* NOTREACHED */
	}

	return(rc);
}

