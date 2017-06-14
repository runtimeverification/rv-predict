/*
 *	SCCS: @(#)lsdir.c	1.5 (03/03/26)
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
static char sccsid[] = "@(#)lsdir.c	1.5 (03/03/26) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)lsdir.c	1.5 03/03/26 TETware release 3.8
NAME:		lsdir.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	October 1996

DESCRIPTION:
	function to generate a directory listing

	this function moved from tccd/tsfile.c to here

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., March 1998
	Excluded "." from the list of directory entries returned by
	tcf_lsdir() - it's not used anywhere.

	Andrew Dingwall, UniSoft Ltd., November 1998
	added <sys/types.h> for the benefit of FreeBSD

	Andrew Dingwall, UniSoft Ltd., May 1999
	If the directory is empty (except for . and ..), return an empty
	list rather than an error condition.

	Andrew Dingwall, The Open Group, March 2003
	Moved from tcclib to dtet2lib and renamed from tcf_lsdir()
	to tet_lsdir().


************************************************************************/

#include <stdio.h>
#include <sys/types.h>
#include <string.h>
#include <errno.h>
#include "dtmac.h"
#include "tetdir.h"
#include "error.h"
#include "dtetlib.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/*
**	tet_lsdir() - return a pointer to a list of pointers to directory
**		entries
**
**	return (char **) 0 on error
**
**	the return list excludes "." and ".."
*/

char **tet_lsdir(dir)
char *dir;
{
	register DIR *dirp;
	register struct dirent *dp;
	register int n, nfiles;
	register char **fip;
	char **files = (char **) 0;
	int flen = 0;

	/* open the directory */
	if ((dirp = OPENDIR(dir)) == (DIR *) 0) {
		error(errno, "can't open", dir);
		return((char **) 0);
	}

	/* create an empty list */
	if (BUFCHK((char **) &files, &flen, (int) sizeof *files) < 0)
		return((char **) 0);
	*files = (char *) 0;
	

	/* count the files in the directory and store their names */
	nfiles = 0;
	while ((dp = READDIR(dirp)) != (struct dirent *) 0) {
		if (!strcmp(dp->d_name, ".") || !strcmp(dp->d_name, ".."))
			continue;
		n = (nfiles + 2) * sizeof *files;
		if (BUFCHK((char **) &files, &flen, n) < 0) {
			break;
		}
		fip = files + nfiles;
		if ((*fip = tet_strstore(dp->d_name)) == (char *) 0) {
			break;
		}
		*++fip = (char *) 0;
		nfiles++;
	}
	(void) CLOSEDIR(dirp);

	return(files);
}

