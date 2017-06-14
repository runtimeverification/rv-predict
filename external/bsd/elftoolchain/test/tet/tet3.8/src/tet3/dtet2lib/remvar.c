/*
 *      SCCS:  @(#)remvar.c	1.8 (02/08/09) 
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
static char sccsid[] = "@(#)remvar.c	1.8 (02/08/09) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)remvar.c	1.8 02/08/09 TETware release 3.8
NAME:		remvar.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	June 1992

DESCRIPTION:
	function to strip TET_REMnnn_ prefix from remote config variable
	assignment

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., July 1998
	added tet_remvar_sysid() function

************************************************************************/

#include <stdio.h>
#include <ctype.h>
#include <string.h>
#include "dtmac.h"
#include "dtetlib.h"

/* static function declarations */
static int rvs2 PROTOLIST((char *, char **));

/*
**	tet_remvar() - process remote config variable assignment
**
**	if s starts with a TET_REMnnn_ prefix and nnn matches sysid,
**	tet_remvar() returns a pointer to the start of the rest of the
**	assignment string following the TET_REMnnn_ prefix
**
**	if the name part does not start with the prefix TET_REMnnn_ or nnn
**	does not match sysid, tet_remvar() returns its first argument
**
**	if sysid is -1, any sysid is matched
**
**	tet_remvar() returns (char *) 0 if the variable name is malformed
*/

char *tet_remvar(s, sysid)
char *s;
int sysid;
{
	char *var;
	int rc;

	if ((rc = rvs2(s, &var)) < 0)
		return(rc == -1 ? s : (char *) 0);

	return((sysid == rc || sysid == -1) ? var : s);
}

/*
**	tet_remvar_sysid() - parse remote configuration variable name
**
**	return	nnn if s starts with a TET_REMnnn_ prefix
**	return	-1 if the name doesn't start with a TET_REMnnn_ prefix
**	return	-2 for a malformed TET_REMnnn_ prefix
*/

int tet_remvar_sysid(s)
char *s;
{
	char *var;
	return(rvs2(s, &var));
}

/*
**	rvs2() - common function for tet_remvar() and tet_remvar_sysid()
**
**	return nnn if s starts with a TET_REMnnn_ prefix;
**	a pointer to the first character after the prefix is returned
**	indirectly through *vp
**
**	return -1 if the name doesn't start with a TET_REMnnn_ prefix
**	return -2 for a malformed TET_REMnnn_ prefix
*/

static int rvs2(s, vp)
char *s, **vp;
{
	register char *p;
	register int sysid;
	static char fmt[] = "TET_REM";

	/* see if this is a TET_REM variable */
	if (strncmp(s, fmt, sizeof fmt - 1))
		return(-1);

	/*
	** make p point past the "TET_REM" and extract the nnn part -
	** we don't really mind how many digits there are
	*/
	sysid = 0;
	for (p = s + sizeof fmt - 1; *p && isdigit(*p); p++)
		sysid = (sysid * 10) + (*p & 017);

	/* next char should be '_'; skip over it */
	if (*p++ != '_')
		return(-2);

	*vp = p;
	return(sysid);
}

