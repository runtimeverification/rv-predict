/*
 *      SCCS:  @(#)putenv.c	1.10 (99/09/02) 
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
static char sccsid[] = "@(#)putenv.c	1.10 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)putenv.c	1.10 99/09/02 TETware release 3.8
NAME:		putenv.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	environment manipulation function

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., February 1993
	allow user to modify environment between calls

	Andrew Dingwall, UniSoft Ltd., July 1997
	added support the MT DLL version of the C runtime support library
	on Win32 systems

	Andrew Dingwall, UniSoft Ltd., July 1999
	added support for shared API libraries

************************************************************************/

#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <stdlib.h>
#endif		/* -END-WIN32-CUT- */
#include <stdio.h>
#include "dtmac.h"
#include "dtetlib.h"
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include "error.h"
#endif		/* -END-WIN32-CUT- */

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_putenv() - add an environment string to the environment
**
**	return 0 if successful or -1 on error
**
**	this routine is here because not all systems have putenv(3)
*/

TET_IMPORT int tet_putenv(s)
char *s;
{
#ifdef _WIN32	/* -START-WIN32-CUT- */

	if (_putenv(s) < 0) {
		error(errno, "_putenv() failed", (char *) 0);
		return(-1);
	}

	return(0);

#else		/* -END-WIN32-CUT- */

	static char **env;
	static int envlen;
	register char *p1, *p2;
	register char **ep1, **ep2;
	extern char **environ;

	/* see if the 'name' part is already in the environment
		if so, make the ptr refer to the new string */
	for (ep1 = environ; *ep1; ep1++) {
		for (p1 = *ep1, p2 = s; *p1 && *p2; p1++, p2++)
			if (*p1 != *p2 || *p1 == '=')
				break;
		if (*p1 == '=' && *p2 == '=') {
			*ep1 = s;
			return(0);
		}
	}

	/* not there so:
		see if we have been here before -
		make ep2 point to the old environment space (if any);
		allocate a new environment space */
	ep2 = env;
	if (BUFCHK((char **) &env, &envlen, (int) (((ep1 - environ) + 2) * sizeof *env)) < 0)
		return(-1);

	/* now make ep2 point to the end of the new environment,
		copy in the old environment if env did not previously
		refer to it */
	if (ep2 && ep2 == environ)
		ep2 = env + (ep1 - environ);
	else
		for (ep1 = environ, ep2 = env; *ep1; ep1++, ep2++)
			*ep2 = *ep1;

	/* add the new string to the end of the new environment */
	*ep2++ = s;
	*ep2 = (char *) 0;
	environ = env;

	return(0);

#endif		/* -WIN32-CUT-LINE- */

}

