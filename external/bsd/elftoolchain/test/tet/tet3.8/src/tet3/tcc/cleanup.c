/*
 *	SCCS: @(#)cleanup.c	1.7 (99/11/15)
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
static char sccsid[] = "@(#)cleanup.c	1.7 (99/11/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)cleanup.c	1.7 99/11/15 TETware release 3.8
NAME:		cleanup.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	tcc exit function

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1999
	implemented -j - and -j |shell-command

	Andrew Dingwall, UniSoft Ltd., October 1999
	following the changes to support strict POSIX threads in the API,
	we can no longer exit via tet_exit()

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <sys/types.h>
#include "dtmac.h"
#include "tcc.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/*
**	tcc_exit() - clean up and exit
*/

void tcc_exit(status)
int status;
{
	static int been_here;

	TRACE2(tet_Ttcc, 1, "tcc_exit(): status = %s", tet_i2a(status));

	/*
	** guard against recursive calls; e.g., because one of the
	** functions called here itself calls the fatal error handler
	*/
	if (been_here++)
		exit(status);

	/* shut down the execution engine if it is running */
	engine_shutdown();

	/* remove temporary files */
	rescode_cleanup();
	config_cleanup();

	/* emit the end line to the journal */
	if (jnl_usable())
		jnl_tcc_end();
	jnl_close();

	/* log off servers, close connections and exit */
#ifndef TET_LITE	/* -START-LITE-CUT- */
	dtcc_cleanup();
#endif			/* -END-LITE-CUT- */
	exit(status);
}

