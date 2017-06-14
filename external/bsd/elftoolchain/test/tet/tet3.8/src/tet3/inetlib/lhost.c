/*
 *      SCCS:  @(#)lhost.c	1.8 (97/07/21) 
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
static char sccsid[] = "@(#)lhost.c	1.8 (97/07/21) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)lhost.c	1.8 97/07/21 TETware release 3.8
NAME:		lhost.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	INET localhost address lookup function

MODIFICATIONS:

************************************************************************/

#include <sys/types.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <winsock.h>
#else		/* -END-WIN32-CUT- */
#  include <netinet/in.h>
#endif		/* -WIN32-CUT-LINE- */
#include <time.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "inetlib_in.h"

/*
**	tet_getlocalhostaddr() - return the INET address of localhost
*/

struct in_addr *tet_getlocalhostaddr()
{
	return(tet_gethostaddr("localhost"));
}

