/*
 *      SCCS:  @(#)tfname.c	1.13 (05/07/08) 
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
static char sccsid[] = "@(#)tfname.c	1.13 (05/07/08) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tfname.c	1.13 05/07/08 TETware release 3.8
NAME:		tfname.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to generate a temporary file name with some guarantee that it
	will be useful

MODIFICATIONS:
	Geoff Clare, UniSoft Ltd., August 1996
	Missing <unistd.h>.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.

	Andrew Dingwall, The Open Group, January 2002
	When all names have been exhausted, re-use names of files that
	no longer exist (as a result of changes made to tet_vprintf()
	in apilib/dresfile.c v1.32, tet_mktfname() could now be called
	much more often in a single process).

	Geoff Clare, The Open Group, July 2005
	Added /var/tmp to the default tmpdirs list.
	Report an error when none of the tmpdirs are usable.
 
************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <io.h>
#else		/* -END-WIN32-CUT- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "globals.h"
#include "error.h"
#include "ltoa.h"
#include "bstring.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

#ifdef _WIN32	/* -START-WIN32-CUT- */
static char *dirs[] = { (char *) 0, "c:/temp", "c:/tmp", "c:", (char *) 0 };
#else		/* -END-WIN32-CUT- */
static char *dirs[] = { (char *) 0, "/var/tmp", "/usr/tmp", "/tmp", (char *) 0 };
#endif		/* -WIN32-CUT-LINE- */

static char **tmpdirs;
static char salt[] = "\000AAA";

/* tryone() return codes */
#define TR_OK		1	/* file could be created */
#define TR_NEXTDIR	2	/* try again in next directory */
#define TR_NEXTFNAME	3	/* try again with next file name */
#define TR_ERROR	-1	/* error - give up */

/* mode for the open() call */
#define MODEANY	\
	((mode_t) (S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH))


/* static function declarations */
static int tryone PROTOLIST((char *, char *, char **));


/*
**	tet_mktfname() - generate a temporary file name and return a pointer
**		thereto
**
**	return (char *) 0 on error
*/

char *tet_mktfname(prefix)
char *prefix;
{
	register char **tdp;
	register char *p;
	register int rc = TR_ERROR;
	char *fname;
	int pass1;

	/* do initial setup first time through */
	if (!tmpdirs) {
		if ((p = getenv("TMPDIR")) == (char *) 0 || !*p)
			tmpdirs = &dirs[1];
		else {
			dirs[0] = p;
			tmpdirs = &dirs[0];
		}
	}

	/* generate the file name */
	pass1 = 0;
	for (;;) {
		if (salt[0]) {
			if (!pass1++)
				salt[0] = '\0';
			else {
				error(0, "out of tmp file names", (char *) 0);
				break;
			}
		}
		for (tdp = tmpdirs; *tdp; tdp++)
			if ((rc = tryone(*tdp, prefix, &fname)) != TR_NEXTDIR)
				break;
		if (rc == TR_ERROR)
			break;
		if (rc == TR_NEXTDIR) {
			error(0, "all tmp dirs unusable - try setting TMPDIR in environment", (char *) 0);
			break;
		}
		/* here if rc is TR_OK or TR_NEXTFNAME */
		for (p = &salt[sizeof salt - 2]; p >= salt; p--)
			if (++*p > 'Z')
				*p = 'A';
			else
				break;
		if (rc == TR_OK)
			return(fname);
	}

	/* here to return after an error */
	return((char *) 0);
}

/*
**	tryone() - try to create a temporary file
**
**	return TR_* return code to indicate success or failure
**	if successful, the generated file name is stored indirectly through *np
**
**	we try to make sure that there is a reasonable chance of being able to
**	write some data to the file
*/

static int tryone(dir, prefix, np)
char *dir, *prefix;
char **np;
{
	register char *fname, *pidstr;
	register int fd, n, rc;
	size_t needlen;
	char buf[1024];

	/* work out how big a buffer is needed */
	pidstr = tet_i2a(tet_mypid);
	needlen = strlen(dir) + strlen(prefix) + sizeof salt + strlen(pidstr);

	/* get a buffer for the file name */
	errno = 0;
	if ((fname = malloc(needlen)) == (char *) 0) {
		error(errno, "can't get tmp file name buffer", (char *) 0);
		return(TR_ERROR);
	}
	TRACE2(tet_Tbuf, 6, "allocate tfname = %s", tet_i2x(fname));

	/* generate the file name */
	(void) sprintf(fname, "%s/%s%s%s", dir, prefix, &salt[1], pidstr);

	/* try to open the file */
	if ((fd = OPEN(fname, O_RDWR|O_CREAT|O_EXCL|O_BINARY, MODEANY)) < 0)
		switch (errno) {
		case EMFILE:
		case ENFILE:
			/* caller will have to take his chances! */
			rc = TR_OK;
			break;
		case EEXIST:
		case EISDIR:
		case ENXIO:
			rc = TR_NEXTFNAME;
			break;
		default:
			rc = TR_NEXTDIR;
			break;
		}
	else {
		/* try to write a reasonable amount of data to the file */
		bzero(buf, sizeof buf);
		rc = TR_OK;
		for (n = 0; n < 10; n++)
			if (WRITE(fd, buf, sizeof buf) != sizeof buf) {
				rc = TR_NEXTDIR;
				break;
			}
		/* close the file and unlink it */
		(void) CLOSE(fd);
		if (UNLINK(fname) < 0)
			error(errno, "can't unlink", fname);
	}

	/* store the file name if ok, otherwise free the buffer */
	if (rc == TR_OK)
		*np = fname;
	else {
		TRACE2(tet_Tbuf, 6, "free tfname = %s", tet_i2x(fname));
		free(fname);
	}

	return(rc);
}

