/*
 *      SCCS:  @(#)genfatal.c	1.9 (99/09/02) 
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
static char sccsid[] = "@(#)genfatal.c	1.9 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)genfatal.c	1.9 99/09/02 TETware release 3.8
NAME:		genfatal.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	generic fatal error handler function

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <sys/types.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "error.h"
#include "ptab.h"
#include "dtetlib.h"
#include "tslib.h"


/*
**	tet_genfatal() - generic fatal error handler
*/

TET_IMPORT void tet_genfatal(errnum, file, line, s1, s2)
int errnum, line;
char *file, *s1, *s2;
{
	(*tet_liberror)(errnum, file, line, s1, s2);

#ifndef TET_LITE	/* -START-LITE-CUT- */
	tet_ts_cleanup();
#endif	/* -END-LITE-CUT- */

	exit(1);
}

