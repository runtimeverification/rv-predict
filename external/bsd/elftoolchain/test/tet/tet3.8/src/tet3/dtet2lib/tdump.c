/*
 *      SCCS:  @(#)tdump.c	1.5 (96/11/04) 
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
static char sccsid[] = "@(#)tdump.c	1.5 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tdump.c	1.5 96/11/04 TETware release 3.8
NAME:		tdump.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	trace interface to tet_hexdump()

MODIFICATIONS:

************************************************************************/

#ifndef NOTRACE

#include <stdio.h>
#include <errno.h>
#include "dtmac.h"
#include "trace.h"
#include "dtetlib.h"

/*
**	tet_tdump() - print a memory dump to the trace file
*/

void tet_tdump(from, len, title)
char *from, *title;
int len;
{
	register int save_errno = errno;
	extern FILE *tet_tfp;

	if (!tet_tfp)
		tet_tfopen();

	(void) fprintf(tet_tfp, "%s:\n",
		title && *title ? title : "data dump:");

	tet_hexdump(from, len, tet_tfp);

	errno = save_errno;
}

#else

int tet_tdump_c_not_empty;

#endif /* NOTRACE */

