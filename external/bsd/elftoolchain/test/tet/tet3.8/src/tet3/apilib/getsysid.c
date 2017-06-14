/*
 *	SCCS: @(#)getsysid.c	1.3 (99/11/15)
 *
 *	UniSoft Ltd., London, England
 *
 * Copyright (c) 1996 The Open Group
 * All rights reserved.
 *
 * No part of this source code may be reproduced, stored in a retrieval
 * system, or transmitted, in any form or by any means, electronic,
 * mechanical, photocopying, recording or otherwise, except as stated
 * in the end-user licence agreement, without the prior permission of
 * the copyright owners.
 * A copy of the end-user licence agreement is contained in the file
 * Licence which accompanies this distribution.
 * 
 * Motif, OSF/1, UNIX and the "X" device are registered trademarks and
 * IT DialTone and The Open Group are trademarks of The Open Group in
 * the US and other countries.
 *
 * X/Open is a trademark of X/Open Company Limited in the UK and other
 * countries.
 *
 */

#ifndef lint
static char sccsid[] = "@(#)getsysid.c	1.3 (99/11/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)getsysid.c	1.3 99/11/15 TETware release 3.8
NAME:		getsysid.c
PRODUCT:	TETware
AUTHOR:		Geoff Clare, UniSoft Ltd.
DATE CREATED:	September 1996

SYNOPSIS:
	#include "tet_api.h"
	int tet_getsysbyid(int sysid, struct tet_sysent *sysp);

DESCRIPTION:
	DTET API function

	Tet_getsysbyid() places the `systems' file entry for the
	specified system ID into the space pointed to by sysp.
	It is not supported in TETware-Lite.

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., August 1998
	Moved tet_getsysbyid() from getsys.c to here.
 
	Andrew Dingwall, UniSoft Ltd., July 1999
	added support for 'other language' APIs

	Andrew Dingwall, UniSoft Ltd., October 1999
	added support for strict POSIX threads

************************************************************************/

#ifndef TET_LITE /* -START-LITE-CUT- */

#include <stdio.h>
#include <string.h>
#include "dtmac.h"
#include "dtthr.h"
#include "globals.h"
#include "sysent.h"
#include "tet_api.h"
#include "apilib.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

TET_IMPORT int tet_getsysbyid(sysid, sysp)
int sysid;
struct tet_sysent *sysp;
{
	struct sysent *entp;

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	if (!sysp)
	{
		tet_errno = TET_ER_INVAL;
		return -1;
	}

	API_LOCK;

	entp = tet_libgetsysbyid(sysid);
	if (!entp)
	{
		tet_errno = TET_ER_SYSID;
		API_UNLOCK;
		return -1;
	}

	/* copy the sy_sysid */
	sysp->ts_sysid = entp->sy_sysid;

	/* copy the sy_name - the sizes must be the same */
#if TET_SNAMELEN != SNAMELEN
#error "TET_SNAMELEN != SNAMELEN"
#endif
	(void) strncpy(sysp->ts_name, entp->sy_name, sizeof(sysp->ts_name));
	sysp->ts_name[sizeof(sysp->ts_name) - 1] = '\0';

	/* ignore the sy_tccd */
	
	API_UNLOCK;
	return 0;
}

#else /* -END-LITE-CUT- */

int tet_getsysid_c_not_used;

#endif /* -LITE-CUT-LINE- */

