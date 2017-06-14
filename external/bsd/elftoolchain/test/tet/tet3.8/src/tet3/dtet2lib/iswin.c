/*
 *	SCCS: @(#)iswin.c	1.2 (97/07/21)
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1997 X/Open Company Limited
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
static char sccsid[] = "@(#)iswin.c	1.2 (97/07/21) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)iswin.c	1.2 97/07/21 TETware release 3.8
NAME:		iswin.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	May 1997

DESCRIPTION:
	functions to determine the type of WIN32 platform
	on which we are running

MODIFICATIONS:

************************************************************************/

#ifdef _WIN32	/* -START-WIN32-CUT- */

#include <stdio.h>
#include <windows.h>
#include "dtmac.h"
#include "bstring.h"
#include "error.h"
#include "ltoa.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* static function declarations */
static void iswininit PROTOLIST((void));


/* the WIN32 platform ID */
static int platformid = -1;


/*
**	tet_iswin95() - see if we are running on Windows 95
**
**	return 1 if we are, 0 otherwise
*/

int tet_iswin95()
{
	if (platformid < 0)
		iswininit();

	return(platformid == VER_PLATFORM_WIN32_WINDOWS ? 1 : 0);
}

/*
**	tet_iswinNT() - see if we are running on Windows NT
**
**	return 1 if we are, 0 otherwise
*/

int tet_iswinNT()
{
	if (platformid < 0)
		iswininit();

	return(platformid == VER_PLATFORM_WIN32_NT ? 1 : 0);
}

/*
**	iswininit() - get the platform name from the O/S
*/

static void iswininit()
{
	OSVERSIONINFO osinf;

	bzero((char *) &osinf, sizeof osinf);
	osinf.dwOSVersionInfoSize = sizeof osinf;
	if (GetVersionEx(&osinf) != TRUE)
		fatal(0, "GetVersionEx() failed, error =",
			tet_i2a(GetLastError()));

	platformid = osinf.dwPlatformId;
}

#else		/* -END-WIN32-CUT- */

int tet_iswin_c_not_used;

#endif		/* -WIN32-CUT-LINE- */

