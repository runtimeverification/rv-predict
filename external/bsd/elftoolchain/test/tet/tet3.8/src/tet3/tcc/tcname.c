/*
 *	SCCS: @(#)tcname.c	1.6 (97/07/15)
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
static char sccsid[] = "@(#)tcname.c	1.6 (97/07/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcname.c	1.6 97/07/15 TETware release 3.8
NAME:		tcname.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	functions to determine the full path names of test cases and
	their directories

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., June 1997
	use get_runtime_tsroot() instead of getdcfg("TET_TSROOT") to
	determine the location of the runtime test suite root directory

************************************************************************/

#include <stdio.h>
#include <ctype.h>
#include <sys/types.h>
#include <time.h>
#include "dtmac.h"
#include "error.h"
#include "scentab.h"
#include "proctab.h"
#include "tcc.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tcsrcname() - return the full path name of a testcase below the
**		source directory
**
**	on return up to namelen bytes are copied into the name array
*/

void tcsrcname(prp, name, namelen)
struct proctab *prp;
char name[];
int namelen;
{
	register char *tcname = prp->pr_scen->sc_tcname;
	register char *tsroot;

	ASSERT(prp->pr_nsys == 1);

	tsroot = get_runtime_tsroot(*prp->pr_sys);
	ASSERT(tsroot && *tsroot);

	while (isdirsep(*tcname))
		tcname++;
	fullpath(tsroot, tcname, name, namelen, 1);
}

/*
**	tcexecname() - return the full path name of a testcase below the
**		execution directory
**
**	on return up to namelen bytes are copied into the name array
*/

void tcexecname(prp, altexecdir, name, namelen)
struct proctab *prp;
char *altexecdir, name[];
int namelen;
{
	register char *tcname = prp->pr_scen->sc_tcname;

	while (isdirsep(*tcname))
		tcname++;
	fullpath(altexecdir, tcname, name, namelen, 1);
}

/*
**	tcsrcdir() - return the full path name of the testcase source directory
**
**	on return up to dirlen bytes are copied into the dir array
*/

void tcsrcdir(prp, dir, dirlen)
struct proctab *prp;
char dir[];
int dirlen;
{
	char name[MAXPATH];

	tcsrcname(prp, name, (int) sizeof name);
	tcc_dirname(name, dir, dirlen);
}

/*
**	tcexecdir() - return the full path name of the testcase execution
**		directory
**
**	on return up to dirlen bytes are copied into the dir array
*/

void tcexecdir(prp, altexecdir, dir, dirlen)
struct proctab *prp;
char *altexecdir, dir[];
int dirlen;
{
	char name[MAXPATH];

	tcexecname(prp, altexecdir, name, (int) sizeof name);
	tcc_dirname(name, dir, dirlen);
}

