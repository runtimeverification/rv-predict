/*
 *      SCCS:  @(#)fork.c	1.9 (98/08/28) 
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
static char sccsid[] = "@(#)fork.c	1.9 (98/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)fork.c	1.9 98/08/28 TETware release 3.8
NAME:		fork.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to fork a new process

	this function  is not implemented on WIN32

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, September 1993
	make sure tet_mypid is updated after fork()

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#ifndef _WIN32		/* -WIN32-CUT-LINE- */

#include <stdio.h>
#include <unistd.h>
#include "dtmac.h"
#include "globals.h"
#include "dtetlib.h"


/*
**	tet_dofork() - try to fork a few times until successful
**
**	return fork() return value
*/

int tet_dofork()
{
	register int rc, try;

	for (try = 0; (rc = fork()) < 0 && try < 5; try++)
		(void) sleep((unsigned) TET_MAX(1 << try, 2));

	if (rc == 0)
		tet_mypid = (int) getpid();

	return(rc);
}

#endif /* !_WIN32 */	/* -WIN32-CUT-LINE- */

