/*
 *      SCCS:  @(#)fake.c	1.8 (99/09/02) 
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
static char sccsid[] = "@(#)fake.c	1.8 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)fake.c	1.8 99/09/02 TETware release 3.8
NAME:		fake.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	fake server-specific functions for tccd

MODIFICATIONS:

************************************************************************/

#include <sys/types.h>
#include <time.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "tccd.h"

/*
**	tet_ss_timeout() - server-specific timeout processing
**
**	tccd does not set timeouts
*/

/* ARGSUSED */
void tet_ss_timeout(pp)
struct ptab *pp;
{
	/* nothing */
}

/*
**	tet_ss_procrun() - server-specific end-procrun processing
*/

void tet_ss_procrun()
{
	/* nothing */
}

