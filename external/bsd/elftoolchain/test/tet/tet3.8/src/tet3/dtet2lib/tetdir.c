/*
 *	SCCS: @(#)tetdir.c	1.3 (97/07/21)
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
static char sccsid[] = "@(#)tetdir.c	1.3 (97/07/21) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tetdir.c	1.3 97/07/21 TETware release 3.8
NAME:		tetdir.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	September 1996

DESCRIPTION:
	emulation of the directory(3) functions for WIN32

MODIFICATIONS:

************************************************************************/

#ifdef _WIN32	/* -START-WIN32-CUT- */

#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <io.h>
#include <errno.h>
#include "dtmac.h"
#include "error.h"
#include "bstring.h"
#include "llist.h"
#include "tetdir.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
** tetdir information table structure - this is a linked list
**
** one of these structures is allocated for each directory opened by
** a call to tet_opendir()
**
** the next and last pointers must be first so as to allow the use of
** the llist routines to manipulate the list
*/
struct drtab {
	struct dirtab *dr_next;		/* ptr to next element in the list */
	struct dirtab *dr_last;		/* ptr to last element in the list */
	long dr_magic;			/* magic number */
	int dr_flags;			/* flags - see below */
	long dr_handle;			/* return value from _findfirst() */
	struct _finddata_t dr_data;	/* data returned by _findfirst()
					   and _findnext() */
	struct dirent dr_dirent;	/* tet_readdir() return value */
};

/* dirtab magic number */
#define DR_MAGIC	0x64495274

/* values for dr_flags (a bit field) */
#define DRF_FIRST	01	/* set when _findfirst() is called,
				   cleared when _findnext() is called */
#define DRF_EOF		02	/* directory is at EOF */


/* the tetdir information table itself */
static struct drtab *drtab;

/* static function declarations */
static void dradd PROTOLIST((struct drtab *));
static struct drtab *dralloc PROTOLIST((void));
static void drfree PROTOLIST((struct drtab *));
static void drrm PROTOLIST((struct drtab *));


/*
**	tet_opendir() - open a directory for reading
**
**	return a (DIR *) pointer suitable for use with tet_readdir() and
**	tet_closedir()
**
**	return (DIR *) 0 on error
*/

DIR *tet_opendir(dir)
char *dir;
{
	static char fmt[] = "%.*s/*.*";
	char pattern[MAXPATH + sizeof fmt];
	struct _stat stbuf;
	struct drtab *dp;

	if (_stat(dir, &stbuf) < 0)
		return((DIR *) 0);

	if ((stbuf.st_mode & _S_IFMT) != _S_IFDIR) {
		errno = ENOTDIR;
		return((DIR *) 0);
	}

	/* set up the pattern for _findfirst() */
	(void) sprintf(pattern, fmt, MAXPATH, dir);

	if ((dp = dralloc()) == (struct drtab *) 0) {
		errno = ENOMEM;
		return((DIR *) 0);
	}

	if ((dp->dr_handle = _findfirst(pattern, &dp->dr_data)) == -1L) {
		if (errno == ENOENT) {
			errno = 0;
			dp->dr_flags |= DRF_EOF;
		}
		else {
			drfree(dp);
			return((DIR *) 0);
		}
	}

	dp->dr_flags |= DRF_FIRST;
	dradd(dp);
	return((DIR *) dp);
}

/*
**	tet_readdir() - read a directory entry and return a pointer thereto
**
**	return (struct dirent *) 0 on EOF or error
*/

struct dirent *tet_readdir(dirp)
DIR *dirp;
{
	register struct drtab *dp = (struct drtab *) dirp;

	if (!dp || dp->dr_magic != DR_MAGIC) {
		errno = EBADF;
		return((struct dirent *) 0);
	}

	if (dp->dr_flags & DRF_EOF)
		return((struct dirent *) 0);
	else if (dp->dr_flags & DRF_FIRST)
		dp->dr_flags &= ~DRF_FIRST;
	else {
		if (_findnext(dp->dr_handle, &dp->dr_data) == -1) {
			if (errno == ENOENT) {
				errno = 0;
				dp->dr_flags |= DRF_EOF;
			}
			return((struct dirent *) 0);
		}
	}

	dp->dr_dirent.d_name = dp->dr_data.name;
	return(&dp->dr_dirent);
}

/*
**	tet_closedir() - close a directory
**
**	return 0 if successful or -1 on error
*/

int tet_closedir(dirp)
DIR *dirp;
{
	register struct drtab *dp = (struct drtab *) dirp;
	register int rc;

	if (!dp || dp->dr_magic != DR_MAGIC) {
		errno = EBADF;
		return(-1);
	}

	if ((rc = _findclose(dp->dr_handle)) == -1 && errno == ENOENT) {
		rc = 0;
		errno = 0;
	}

	drrm(dp);
	drfree(dp);
	return(rc);
}

static struct drtab *dralloc()
{
	register struct drtab *dp;

	errno = 0;
	if ((dp = (struct drtab *) malloc(sizeof *dp)) == (struct drtab *) 0) {
		error(errno, "can't allocate drtab entry", (char *) 0);
		return((struct drtab *) 0);
	}
	TRACE2(tet_Tbuf, 6, "allocate drtab = %s", tet_i2x(dp));
	bzero((char *) dp, sizeof *dp);

	dp->dr_magic = DR_MAGIC;
	return(dp);
}

static void drfree(dp)
struct drtab *dp;
{

	TRACE2(tet_Tbuf, 6, "free drtab = %s", tet_i2x(dp));

	if (dp) {
		bzero((char *) dp, sizeof *dp);
		free((char *) dp);
	}
}

static void dradd(dp)
struct drtab *dp;
{
	tet_listinsert((struct llist **) &drtab, (struct llist *) dp);
}

static void drrm(dp)
struct drtab *dp;
{
	tet_listremove((struct llist **) &drtab, (struct llist *) dp);
}

#else		/* -END-WIN32-CUT- */

int tet_tetdir_c_not_used;

#endif		/* -WIN32-CUT-LINE- */

