/*
 *      SCCS:  @(#)getlist.c	1.17 (99/11/15) 
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
static char sccsid[] = "@(#)getlist.c	1.17 (99/11/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)getlist.c	1.17 99/11/15 TETware release 3.8
NAME:		getlist.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

SYNOPSIS:
	#include "tet_api.h"
	int tet_remgetlist(int **sysnames);

DESCRIPTION:
	DTET API function

	Return the number of other systems in a distributed test case,
	or -1 on error.

	If successful, a pointer to a list of the (numeric) system
	names is returned indirectly through *sysnames.
	A zero terminator is added to the list for
	backwards compatibility.

	The TETware-Lite version always returns zero and sets (*sysnames)
	to point to an empty list.

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, September 1993
	terminate the list of system names returned with integer
	zero (0) (not included in the count).

	Andrew Dingwall, UniSoft Ltd., December 1993
	changed dapi.h to dtet2/tet_api.h

	Geoff Clare, UniSoft Ltd., July 1996
	Changes for TETWare.

	Geoff Clare, UniSoft Ltd., Sept 1996
	Changes for TETWare-Lite.

	Andrew Dingwall, June 1997
	Fixed a bug whereby the number of systems returned always
	included sysid 0 even when that system is not participating
	in a distributed test;
	the system list is always zero-terminated but the zero is only
	included in the count if sysid 0 is one of the participating
	systems.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
	Andrew Dingwall, UniSoft Ltd., July 1999
	added support for 'other language' APIs

	Andrew Dingwall, UniSoft Ltd., October 1999
	added support for strict POSIX threads


************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "dtthr.h"
#include "globals.h"
#include "tet_api.h"
#include "apilib.h"
#include "dtetlib.h"

#ifndef TET_LITE /* -START-LITE-CUT- */

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


TET_IMPORT int tet_remgetlist(sysnames)
int **sysnames;
{
	register int n, nsys, *ip1, *ip2;
	static int *snames;
	static int slen;
	static int nsname = -1;

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	API_LOCK;

	/*
	** initialise the systems list first time through -
	**
	** the list is always terminated by a 0
	**
	** the return value (i.e., the number of systems in the list)
	** includes the terminating 0 if sysid 0 is in the list;
	** otherwise the return value excludes the terminating 0
	*/
	if (nsname < 0) {
		nsys = 0;
		for (n = 0, ip1 = tet_snames; n < tet_Nsname; n++, ip1++)
			if (*ip1 != tet_mysysid)
				nsys++;
		n = (nsys + 1) * sizeof *snames;
		if (BUFCHK((char **) &snames, &slen, n) < 0)
		{
			tet_errno = TET_ER_ERR;
			API_UNLOCK;
			return(-1);
		}
		ip2 = snames;
		for (n = 0, ip1 = tet_snames; n < tet_Nsname; n++, ip1++)
			if (*ip1 > 0 && *ip1 != tet_mysysid)
				*ip2++ = *ip1;
		*ip2 = 0;
		nsname = nsys;
	}

	if (sysnames)
		*sysnames = snames;

	API_UNLOCK;
	return(nsname);
}

#else /* -END-LITE-CUT- */

TET_IMPORT int tet_remgetlist(sysnames)
int **sysnames;
{
	static int snames[1] = { 0 };

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	if (sysnames)
		*sysnames = snames;

	return 0;
}

#endif /* -LITE-CUT-LINE- */

