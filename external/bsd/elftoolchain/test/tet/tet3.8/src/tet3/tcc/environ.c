/*
 *	SCCS: @(#)environ.c	1.4 (05/12/07)
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
static char sccsid[] = "@(#)environ.c	1.4 (05/12/07) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)environ.c	1.4 05/12/07 TETware release 3.8
NAME:		environ.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	functions to manipulate environment variables

MODIFICATIONS:
	Geoff Clare, UniSoft Ltd., August 1996
	Missing <string.h>.

	Neil Moses, The Open Group, November 2005
	Made init1environ() public for use in tool.c for tcreconnect().

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <time.h>
#include <string.h>
#include "dtmac.h"
#include "error.h"
#include "ltoa.h"
#include "servlib.h"
#include "dtetlib.h"
#include "config.h"
#include "systab.h"
#include "tcc.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	initenviron() - initialise the environment on each system
*/

void initenviron()
{
	register int sysid, sysmax;
	register struct systab *sp;

	/* do this once for each connected system */
	for (sysid = 0, sysmax = symax(); sysid <= sysmax; sysid++)
		if ((sp = syfind(sysid)) != (struct systab *) 0)
			init1environ(sp);
}

/*
**	init1environ() - initialise the environment on a single system
*/

void init1environ(sp)
struct systab *sp;
{
	/*
	** distributed config variables that are to be passed as
	** communication variables in the environment to each test case
	*/
	static char *comvar[] = {
		"TET_ROOT",
		"TET_EXECUTE",
		"TET_SUITE_ROOT",
		"TET_RUN"
	};

#define Ncomvar	(sizeof comvar / sizeof comvar[0])

	char buf[MAXPATH + 40];
	char *envstr[Ncomvar + 1];
	register char **cvp, *val;
	register char **ep = envstr;

#define Nenvstr	(sizeof envstr / sizeof envstr[0])


	ASSERT_LITE(sp->sy_sysid == 0);

	/* build the list of environment strings */
	for (cvp = comvar; cvp < comvar + Ncomvar; cvp++) {
		if ((val = getdcfg(*cvp, sp->sy_sysid)) == (char *) 0)
			val = "";
		(void) sprintf(buf, "%s=%.*s", *cvp,
			(int) sizeof buf - (int) strlen(*cvp) - 2, val);
		ASSERT(ep < &envstr[Nenvstr]);
		*ep++ = rstrstore(buf);
	}

	/* then add in TET_CODE */
	(void) sprintf(buf, "TET_CODE=%.*s", MAXPATH, sp->sy_rcfname);
	ASSERT(ep < &envstr[Nenvstr]);
	*ep++ = rstrstore(buf);

	/* put the strings in the environment on the specified system */
	if (tcc_putenvv(sp->sy_sysid, envstr, Nenvstr) < 0)
		fatal(0, "can't put communication variables in the environment on system",
			tet_i2a(sp->sy_sysid));

#ifndef TET_LITE	/* -START-LITE-CUT- */
	/* finally, free the storage allocated here */
	for (ep = envstr; ep < envstr + Nenvstr; ep++) {
		TRACE2(tet_Tbuf, 6, "free envstr = %s", tet_i2x(*ep));
		free(*ep);
	}
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
}

/*
**	tcc_putenv() - put a single string into the environment that is
**		passed to test cases and tools
**
**	return 0 if successful or -1 on error
**
**	note that in TETware-Lite, the strings are put directly into the
**	environment of the current process and so must be in static storage
*/

int tcc_putenv(sysid, str)
int sysid;
char *str;
{
	return(tcc_putenvv(sysid, &str, 1));
}

/*
**	tcc_putenv() - strings into the environment that is passed to
**		test cases and tools
**
**	return 0 if successful or -1 on error
**
**	note that in TETware-Lite, the strings are put directly into the
**	environment of the current process and so must be in static storage
*/

int tcc_putenvv(sysid, str, nstr)
int sysid;
char **str;
int nstr;
{
	ASSERT_LITE(sysid == 0);

#ifdef TET_LITE	/* -LITE-CUT-LINE- */

	while (--nstr >= 0)
		if (tet_putenv(*str++) < 0)
			return(-1);

#else	/* -START-LITE-CUT- */

	if (tet_tcputenvv(sysid, str, nstr) < 0) {
		error(tet_tcerrno, "tet_tcputenvv() failed on system",
			tet_i2a(sysid));
		return(-1);
	}

#endif /* TET_LITE */	/* -END-LITE-CUT- */

	return(0);
}

