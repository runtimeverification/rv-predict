/*
 *      SCCS:  @(#)msgbuf.c	1.8 (99/09/02) 
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
static char sccsid[] = "@(#)msgbuf.c	1.8 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)msgbuf.c	1.8 99/09/02 TETware release 3.8
NAME:		msgbuf.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	server message buffer access function

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#include <stdio.h>
#include <time.h>
#include <sys/types.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "bstring.h"
#include "servlib.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_ti_msgbuf() - return a pointer to a ptab message data buffer,
**		growing it if necessary
**
**	return (char *) 0 on error
*/

TET_IMPORT char *tet_ti_msgbuf(pp, newlen)
struct ptab *pp;
int newlen;
{
	if (BUFCHK(&pp->ptm_data, &pp->pt_mdlen, newlen) < 0)
		return((char *) 0);

	bzero(pp->ptm_data, newlen);
	return(pp->ptm_data);
}

