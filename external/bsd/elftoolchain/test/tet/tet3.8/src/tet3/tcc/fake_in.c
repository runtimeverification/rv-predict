/*
 *	SCCS: @(#)fake_in.c	1.5 (99/09/02)
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
static char sccsid[] = "@(#)fake_in.c	1.5 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)fake_in.c	1.5 99/09/02 TETware release 3.8
NAME:		fake_in.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	dummy functions referenced by the INET transport/specific code

MODIFICATIONS:

************************************************************************/

#include <sys/types.h>
#include <time.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <winsock.h>
#endif		/* -END-WIN32-CUT- */
#include "dtmac.h"
#include "error.h"
#include "dtmsg.h"
#include "ptab.h"
#include "server_in.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* tcc does not listen for or accept incoming connections */
SOCKET tet_listen_sd = INVALID_SOCKET;

void tet_ss_tsaccept()
{
	error(0, "internal error - tet_ss_tsaccept called!", (char *) 0);
}

