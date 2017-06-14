/*
 *      SCCS:  @(#)listn.c	1.11 (02/01/18) 
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
static char sccsid[] = "@(#)listn.c	1.11 (02/01/18) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)listn.c	1.11 02/01/18 TETware release 3.8
NAME:		listn.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to listen on an INET socket

MODIFICATIONS:

************************************************************************/

#include <errno.h>
#include <time.h>
#include <sys/types.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <winsock.h>
#else		/* -END-WIN32-CUT- */
#  include <sys/socket.h>
#  include <netinet/in.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "error.h"
#include "ltoa.h"
#include "time.h"
#include "dtmsg.h"
#include "ptab.h"
#include "inetlib_in.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/*
**	tet_ts_listen() - arrange to listen on the incoming message socket
*/

void tet_ts_listen(sd)
SOCKET sd;
{
	TRACE2(tet_Tio, 4, "listen on sd %s", tet_i2a(sd));

	if (listen(sd, 10) == SOCKET_ERROR)
		fatal(SOCKET_ERRNO, "listen() failed on sd", tet_i2a(sd));
}

