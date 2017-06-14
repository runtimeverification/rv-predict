/*
 *	SCCS: @(#)utils.c	1.7 (98/09/01)
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
static char sccsid[] = "@(#)utils.c	1.7 (98/09/01) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)utils.c	1.7 98/09/01 TETware release 3.8
NAME:		utils.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	tcc utility functions

MODIFICATIONS:
	Geoff Clare, UniSoft Ltd., August 1996
	Missing <string.h>.

	Andrew Dingwall, UniSoft Ltd., May 1998
	Use tet_basename() in dtet2lib instead of tcc_basename().

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <time.h>
#include <ctype.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <direct.h>
#endif		/* -END-WIN32-CUT- */
#include <string.h>
#include "dtmac.h"
#include "error.h"
#include "globals.h"
#include "ftoa.h"
#include "dtetlib.h"
#include "tcc.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/*
**	rstrstore() - reliable strstore() call
**
**	there is no return on error
*/

char *rstrstore(s)
char *s;
{
	char *p;

	if ((p = tet_strstore(s)) == (char *) 0)
		fatal(0, "can't continue", (char *) 0);

	return(p);
}

#ifdef NOTRACE

/*
**	rbufchk() - reliable tet_bufchk() call
*/

void rbufchk(bpp, lp, newlen)
char **bpp;
int *lp, newlen;
{
	if (tet_bufchk(bpp, lp, newlen) < 0)
		fatal(0, "can't continue", (char *) 0);
}

#else /* NOTRACE */

/*
**	rbuftrace() - reliable tet_buftrace() call
*/

void rbuftrace(bpp, lp, newlen, file, line)
char **bpp, *file;
int *lp, newlen, line;
{
	if (tet_buftrace(bpp, lp, newlen, file, line) < 0)
		fatal(0, "can't continue", (char *) 0);
}

#endif /* NOTRACE */

/*
**	split() - split a string up into at most maxargs fields,
**		discarding excess fields
**
**	return the number of fields found
*/

int split(s, argv, maxargs, delim)
register char *s, **argv;
register int maxargs, delim;
{
	register int argc, new;

	if (isspace(delim))
		return(tet_getargs(s, argv, maxargs));

	for (argc = 0, new = 1; *s; s++)
		if (*s == (char) delim) {
			*s = '\0';
			new = 1;
			if (argc >= maxargs)
				break;
		}
		else if (new && argc++ < maxargs) {
			*argv++ = s;
			new = 0;
		}

	return(argc);
}

/*
**	scenerror() - report an error in a scenario file
*/

void scenerror(s1, s2, lineno, fname)
char *s1, *s2, *fname;
int lineno;
{
	scenermsg(s1, s2, lineno, fname);
	if (++scenerrors >= MAXSCENERRORS)
		scengiveup();
}

/*
**	scenermsg() - print a scenario file error message
*/

void scenermsg(s1, s2, lineno, fname)
char *s1, *s2, *fname;
int lineno;
{
	(void) fprintf(stderr, "%s: %s", tet_progname, s1);
	if (s2 && *s2)
		(void) fprintf(stderr, " %s", s2);
	(void) fprintf(stderr, " at line %d in file %s\n", lineno, fname);
	(void) fflush(stderr);
}

/*
**	scengiveup() - report scenario error total and exit
*/

void scengiveup()
{
	(void) fprintf(stderr,
		"%s: giving up after finding %d scenario error%s\n",
		tet_progname, scenerrors, scenerrors == 1 ? "" : "s");
	tcc_exit(1);
}

/*
**	tcc_dirname() - return all but the last part of a path name
**
**	up to dirlen bytes of the return value is copied into the dir array
*/

void tcc_dirname(path, dir, dirlen)
char *path, dir[];
int dirlen;
{
	register int len;

	if ((len = tet_basename(path) - path - 1) == 0 && isdirsep(*path))
		len++;

	if (len <= 0) {
		path = ".";
		len = 1;
	}

	(void) sprintf(dir, "%.*s", TET_MIN(len, dirlen - 1), path);
}

/*
**	fullpath() - return the absolute path name of file
**		relative to dir
**
**	on return up to pathlen bytes are copied to the path array
*/

void fullpath(dir, file, path, pathlen, remote)
char *dir, *file, path[];
int pathlen, remote;
{
	register char *p = path;
	register int len;

#ifndef NOTRACE
	static char null[] = "NULL";
#endif

	TRACE4(tet_Ttcc, 10, "fullpath(\"%s\", \"%s\", %s)",
		dir ? dir : null, file ? file : null,
		remote ? "REMOTE" : "LOCAL");

	ASSERT(file && *file);

#ifdef _WIN32		/* -START-WIN32-CUT- */

	if (remote) {
		/* if not absolute, prepend directory name */
		if (!isabspathrem(file)) {
			ASSERT(dir && isabspathrem(dir));
			(void) sprintf(p, "%.*s", pathlen - 2, dir);
			len = strlen(p);
			p += len;
			pathlen -= len;
			if (!isdirsep(*(p - 1))) {
				*p++ = '/';
				*p = '\0';
				--pathlen;
			}
		}
	}
	else if (isabsondrv(file)) {
		/* absolute path - convert drive letter to lcase */
		if (isupper(*file)) {
			*p++ = tolower(*file);
			pathlen--;
			file++;
		}
	}
	else if (isabsoncur(file)) {
		/* prepend current drive letter */
		(void) sprintf(p, "%c:", dno2dl(_getdrive()));
		p += 2;
		pathlen -= 2;
	}
	else if (isrelondrv(file)) {
		/* prepend cwd on specified drive */
		if (_getdcwd(dl2dno(*file), p, pathlen - 2) == (char *) 0)
			fatal(errno, "_getdcwd() failed", (char *) 0);
		len = strlen(p);
		p += len;
		pathlen -= len;
		if (!isdirsep(*(p - 1))) {
			*p++ = '/';
			*p = '\0';
			--pathlen;
		}
		file += 2;
	}
	else {
		/* must be relative - prepend cwd on current drive */
		ASSERT(isreloncur(file));
		ASSERT(dir && (isabsondrv(dir) || isabsoncur(dir)));
		if (isabsoncur(dir)) {
			(void) sprintf(p, "%c:", dno2dl(_getdrive()));
			p += 2;
			pathlen -= 2;
		}
		(void) sprintf(p, "%.*s", pathlen - 2, dir);
		len = strlen(p);
		p += len;
		pathlen -= len;
		if (!isdirsep(*(p - 1))) {
			*p++ = '/';
			*p = '\0';
			--pathlen;
		}
	}

#else /* _WIN32 */	/* -END-WIN32-CUT- */

	if (
		(remote && !isabspathrem(file)) ||
		(!remote && !isabspathloc(file))
	) {
		ASSERT(dir);
		if (remote)
			ASSERT(isabspathrem(dir));
		else
			ASSERT(isabspathloc(dir));
		(void) sprintf(p, "%.*s", pathlen - 2, dir);
		len = strlen(p);
		p += len;
		pathlen -= len;
		if (
			(remote && !ispcdirsep(*(p - 1))) ||
			(!remote && !isdirsep(*(p - 1)))
		) {
			*p++ = '/';
			*p = '\0';
			--pathlen;
		}
	}

#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */

	(void) sprintf(p, "%.*s", pathlen - 1, file);

	if ((dir ? (int) strlen(dir) : 0) + (int) strlen(file) > pathlen - 2)
		error(0, "path name is too long and has been truncated:",
			path);

#ifdef _WIN32	/* -START-WIN32-CUT- */
	for (p = path; *p; p++)
		if (*p == '\\')
			*p = '/';
#endif		/* -END-WIN32-CUT- */

	TRACE2(tet_Ttcc, 10, "fullpath(): return \"%s\"", path);
}

/*
**	prtccmode() - return printable representation of tcc modes value
*/

char *prtccmode(fval)
int fval;
{
	static struct flags flags[] = {
		{ TCC_START,	"START" },
		{ TCC_BUILD,	"BUILD" },
		{ TCC_EXEC,	"EXEC" },
		{ TCC_CLEAN,	"CLEAN" },
		{ TCC_RESUME,	"RESUME" },
		{ TCC_RERUN,	"RERUN" },
		{ TCC_ABORT,	"ABORT" },
		{ TCC_END,	"END" }
	};

	return(tet_f2a(fval, flags, sizeof flags / sizeof flags[0]));
}

