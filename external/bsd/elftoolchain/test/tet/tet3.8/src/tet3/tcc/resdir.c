/*
 *	SCCS: @(#)resdir.c	1.4 (96/11/04)
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
static char sccsid[] = "@(#)resdir.c	1.4 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)resdir.c	1.4 96/11/04 TETware release 3.8
NAME:		resdir.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	functions to create and administer the results directory

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <ctype.h>
#include <sys/types.h>
#include <time.h>
#include "dtmac.h"
#include "error.h"
#include "dtetlib.h"
#include "tetdir.h"
#include "tcc.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* results directory name - gets overwritten by initresdir() */
static char *results_dir = "results";

/* static function declarations */
static char *ressubdir PROTOLIST((char *));


/*
**	initresdir() - create the results directory
**
**	iopt is the -i command-line option
**	cwd is tcc's initial working directory
*/

void initresdir(iopt, cwd)
char *iopt, *cwd;
{
	char resroot[MAXPATH], resdir[MAXPATH];
	int n, rc;

	/*
	** determine the name of the results directory
	**
	** if -i has not been specified, create a default directory
	** whose name is tet_tsroot/results/NNNN{bec}
	*/
	if (iopt && *iopt) {
		fullpath(cwd, iopt, resdir, sizeof resdir, 0);
		if (tet_eaccess(resdir, 02) < 0)
			fatal(errno, "can't access", resdir);
	}
	else {
		fullpath(tet_tsroot, results_dir, resroot, sizeof resroot, 0);
		for (n = 0; n < 5; n++) {
			fullpath(resroot, ressubdir(resroot), resdir,
				sizeof resdir, 0);
			errno = 0;
			if ((rc = tet_mkalldirs(resdir)) == 0 ||
				errno != EEXIST)
					break;
		}
		if (rc < 0)
			tcc_exit(1);
	}

	results_dir = rstrstore(resdir);
	TRACE2(tet_Ttcc, 1, "results directory = %s", results_dir);
}

/*
**	resdirname() - return the name of the results directory
*/

char *resdirname()
{
	ASSERT(results_dir && isabspathloc(results_dir));
	return(results_dir);
}

/*
**	ressubdir() - return the name of the results subdirectory to use
*/

static char *ressubdir(resroot)
char *resroot;
{
	DIR *dirp;
	struct dirent *dp;
	int thisval, maxval;
	static char buf[8];

	/* if the results directory exists, open it and find the
	** highest numbered subdirectory below it
	*/
	maxval = 0;
	if ((dirp = OPENDIR(resroot)) == (DIR *) 0) {
		if (errno != ENOENT)
			fatal(errno, "can't open", resroot);
	}
	else {
		while ((dp = READDIR(dirp)) != (struct dirent *) 0)
			if ((thisval = atoi(dp->d_name)) > maxval)
				maxval = thisval;
		(void) CLOSEDIR(dirp);
	}

	/*
	** check for overflow - this should not usually occur because
	** most systems limit the number of links (i.e., subdirectories)
	** to a much lower value
	*/
	if (++maxval > 9999)
		fatal(0, "too many results directories below", resroot);

	/* construct the name of the subdirectory for the selected
		modes of operation */
	(void) sprintf(buf, "%04d%s", maxval, resdirsuffix());

	return(buf);
}

/*
**	resdirsuffix() - return pointer to the suffix to use for the
**		results directory on the local system, or the saved files
**		directory on a remote system
*/

char *resdirsuffix()
{
	static char suffix[4];
	register char *p;

	if (!suffix[0]) {
		p = suffix;
		if (tcc_modes & TCC_BUILD)
			*p++ = 'b';
		if (tcc_modes & TCC_EXEC)
			*p++ = 'e';
		if (tcc_modes & TCC_CLEAN)
			*p++ = 'c';
	}

	return(suffix);
}

