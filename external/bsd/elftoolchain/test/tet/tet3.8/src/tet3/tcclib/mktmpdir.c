/*
 *	SCCS: @(#)mktmpdir.c	1.4 (02/01/18)
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
static char sccsid[] = "@(#)mktmpdir.c	1.4 (02/01/18) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)mktmpdir.c	1.4 02/01/18 TETware release 3.8
NAME:		mktmpdir.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	October 1996

DESCRIPTION:
	tcc action function - make a unique temporary directory

	this function moved from tccd/stcc.c to here

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., March 1998
	Added an extra salt character to the unique directory name so as
	to enable more instances of a test case to execute in parallel.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#include <stdio.h>
#include <errno.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "error.h"
#include "globals.h"
#include "ltoa.h"
#include "dtetlib.h"
#include "tcclib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* mode for created directory */
#define MODEANY		((mode_t) (S_IRWXU | S_IRWXG | S_IRWXO))

/* static function declarations */
static int tcf_mktd2 PROTOLIST((char *));


/*
**	tcf_mktmpdir() - make a unique temporary directory
**
**	return ER_OK if successful or other ER_* code on error
**
**	if successful, the name of the temporary directory is returned
**	indirectly through *subdp
*/

int tcf_mktmpdir(dir, subdp)
char *dir, **subdp;
{
	register int needlen, rc;
	char salt1, salt2;
	char pidstr[LNUMSZ];
	static char *subdir;
	static int sdlen;
	extern int tet_mypid;

	/* do a sanity check on the input arguments */
	ASSERT(dir && *dir);

	/* format the pid string */
	(void) sprintf(pidstr, "%05u", tet_mypid);

	/* get a buffer for the tmp dir name */
	needlen = strlen(dir) + strlen(pidstr) + 4;
	if (BUFCHK(&subdir, &sdlen, needlen) < 0)
		return(ER_ERR);

	/* try to make the directory a few times */
	for (salt1 = 'a'; salt1 <= 'z'; salt1++)
		for (salt2 = 'a'; salt2 <= 'z'; salt2++) {
			(void) sprintf(subdir, "%s/%s%c%c",
				dir, pidstr, salt1, salt2);
			if ((rc = tcf_mktd2(subdir)) < 0)
				return(rc);
			else if (rc > 0) {
				*subdp = subdir;
				return(ER_OK);
			}
		}

	/* here if we are out of suffix letters */
	error(0, "out of tmp subdir names in", dir);
	return(ER_ERR);
}

/*
**	tcf_mktd2() - extend the tcf_mktmpdir() processing
**
**	try to make the specified directory
**
**	return	1 if successful
**		0 to try the next tmpdir name
**		-ve error code on error
*/

static int tcf_mktd2(subdir)
char *subdir;
{
	int errsave, rc;

	TRACE2(Ttcclib, 6, "tcf_mktmpdir(): try \"%s\"", subdir);

	/* attempt to make the directory */
	if (tet_mkdir(subdir, MODEANY) < 0) {
		if (errno == EEXIST)
			return(0);
		else {
			errsave = errno;
			if ((rc = tet_maperrno(errsave)) == ER_ERR)
				error(errsave, "can't make directory", subdir);
			errno = errsave;
			return(rc);
		}
	}

	/* here if the directory could be created successfully */
	TRACE2(Ttcclib, 4, "tcf_mktmpdir(): return \"%s\"", subdir);
	return(1);
}

