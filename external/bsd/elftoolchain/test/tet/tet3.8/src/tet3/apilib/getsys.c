/*
 *      SCCS:  @(#)getsys.c	1.14 (99/11/15) 
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
static char sccsid[] = "@(#)getsys.c	1.14 (99/11/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)getsys.c	1.14 99/11/15 TETware release 3.8
NAME:		getsys.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

SYNOPSIS:
	#include "tet_api.h"
	int tet_remgetsys(void);

DESCRIPTION:
	DTET API function

	Tet_remgetsys() returns the (numeric) system name of the local
	system.  The TETware-Lite version always returns zero.

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., December 1993
	changed dapi.h to dtet2/tet_api.h

	Geoff Clare, UniSoft Ltd., July 1996
	Changes for TETWare.

	Geoff Clare, UniSoft Ltd., Sept 1996
	Changes for TETWare-Lite.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
	Moved tet_getsysbyid() to a separate file.
 
	Andrew Dingwall, UniSoft Ltd., July 1999
	added support for 'other language' APIs

	Andrew Dingwall, UniSoft Ltd., October 1999
	added support for strict POSIX threads

************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "globals.h"
#include "tet_api.h"
#include "apilib.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


TET_IMPORT int tet_remgetsys()
{
	tet_check_api_status(TET_CHECK_API_INITIALISED);

#ifndef TET_LITE /* -START-LITE-CUT- */
	return(tet_mysysid);
#else /* -END-LITE-CUT- */
	return(0);
#endif /* -LITE-CUT-LINE- */
}

