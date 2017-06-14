/*
 *      SCCS:  @(#)fcopy.c	1.14 (99/04/21)
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1993 X/Open Company Limited
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
static char sccsid[] = "@(#)fcopy.c	1.14 (99/04/21) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)fcopy.c	1.14 99/04/21 TETware release 3.8
NAME:		fcopy.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Ltd.
DATE CREATED:	June 1993

DESCRIPTION:
	Routines to copy files and directories without pattern matching.
	Used when performing the processing associated with
	TET_EXEC_IN_PLACE and TET_RUN.

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., November 1993
	removed un-needed #includes, added support for non-posix systems

	Andrew Dingwall, UniSoft Ltd., August 1996
	trap attempt to recursively copy a directory into one of its
	subdirectories

	Geoff Clare, UniSoft Ltd., August 1996
	Missing <unistd.h>.

	Andrew Dingwall, UniSoft Ltd., June 1997
	tet_pmatch() moved from here to tcclib/procdir.c.
	Changed logic so that tet_fcopy("/foo", "/bar") where /foo
	is a directory copies /foo/<all> to /bar and not to /bar/foo.

	Andrew Dingwall, UniSoft Ltd., May 1998
	On Win32 systems, fopen() doesn't always set errno on error, 
	so clear errno before calling fopen().
	Use tet_basename() instead of local static version.

	Andrew Dingwall, UniSoft Ltd., December 1998
	fixed a bug in the recursive copy detection code

************************************************************************/

#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <ctype.h>
#  include <io.h>
#else		/* -END-WIN32-CUT- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "error.h"
#include "globals.h"
#include "bstring.h"
#include "tetdir.h"
#include "dtetlib.h"

#ifndef NOTRACE
#  include "dtmsg.h"
#  ifdef TET_LITE	/* -LITE-CUT-LINE- */
#    define Tfcopy	((tet_myptype == PT_MTCC) ? tet_Ttcc : tet_Ttrace)
#  else			/* -START-LITE-CUT- */
#    define Tfcopy	((tet_myptype == PT_MTCC) ? tet_Ttcc : \
			((tet_myptype == PT_STCC) ? tet_Ttccd : tet_Ttrace))
#  endif		/* -END-LITE-CUT- */
#endif


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;       /* file name for error reporting */
#endif

/* mask used to extract the permissions bits from st_mode */
#define MODEMASK	(S_IRWXU | S_IRWXG | S_IRWXO)

/* static function declarations */
static int rdcopy  PROTOLIST((char *, char *));


/*
**	Copy a file to a specified file or directory. If the 'from'
**	file is a directory, a recursive copy is done.
**
**	return 0 if successful or -1 on error
**
**	note that functions which call this function expect that
**	errno will be set on error return.
**	error() clears errno so errno must be saved when error() is called
**	and restored afterwards
*/

int tet_fcopy(src, dest)
char *src;
char *dest;
{
	char buf[BUFSIZ];
	char destfile[MAXPATH + 1];
	struct STAT_ST st_src, st_dest;
	int dest_exists, dest_is_directory, len, nread, rc, save_errno;
	FILE *ifp, *ofp;
	static char fmt1[] = "can't copy from directory %.*s to non-directory";
	static char fmt2[] = "can't copy %.*s to %.*s";
	char msg[(MAXPATH * 2) + sizeof fmt2];

	TRACE3(Tfcopy, 8, "fcopy(): from <%s> to <%s>", src, dest);

	/* look up the src file */
	if (STAT(src, &st_src) < 0) {
		save_errno = errno;
		error(errno, "can't stat", src);
		errno = save_errno;
		return(-1);
	}

	/*
	** see if dest exists -
	**	if src is a directory, dest must be a directory also
	**	whether it exists or not
	*/
	bzero((char *) &st_dest, sizeof st_dest);
	dest_exists = (STAT(dest, &st_dest) < 0) ? 0 : 1;
	if (dest_exists) {
		dest_is_directory = S_ISDIR(st_dest.st_mode);
		if (S_ISDIR(st_src.st_mode) && !dest_is_directory) {
			(void) sprintf(msg, fmt1, MAXPATH, src);
			error(ENOTDIR, msg, dest);
			errno = ENOTDIR;
			return(-1);
		}
	}
	else
		dest_is_directory = S_ISDIR(st_src.st_mode);

	/*
	** if destination is a directory:
	**	if source is a directory:
	**		make destination directories as required and do
	**		a recursive copy to dest;
	**	otherwise, determine the destination file name
	*/
	if (dest_is_directory) {
		if (S_ISDIR(st_src.st_mode)) {
			if (!dest_exists && tet_mkalldirs(dest) < 0) {
				save_errno = errno;
				error(errno, "can't create directory", dest);
				errno = save_errno;
				return(-1);
			}
			return(rdcopy(src, dest));
		}
		else {
			len = (int) sizeof destfile - (int) strlen(dest) - 2;
			(void) sprintf(destfile, "%.*s/%.*s",
				(int) (sizeof destfile - 2), dest,
				TET_MAX(len, 0), tet_basename(src));
			dest = destfile;
		}
	}

	/*
	** here if src is not a directory -
	** ignore files other than regular files
	*/
	if (!S_ISREG(st_src.st_mode)) {
		(void) sprintf(msg, fmt2, MAXPATH, src, MAXPATH, dest);
		error(0, msg, "(source is not a plain file)");
		return(0);
	}

	/*
	** here, src and dest are both names of plain files
	** (although dest may not exist yet)
	*/

/*
** macro to test whether src and dest refer to the same file
**
** on WIN32 systems: st_dev is the drive number and st_ino is always zero;
** there are no links, so a (case-insensitive) string comparison of the
** file name component after an initial drive specifier is sufficient
**
** on other systems, we do the traditional device and inode number comparison
*/
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  define SRC_IS_DEST \
	(st_src.st_dev == st_dest.st_dev && \
	!_stricmp((isalpha(*src) && *(src + 1) == ':') ? src + 2 : src, \
		(isalpha(*dest) && *(dest + 1) == ':') ? dest + 2 : dest))
#else		/* -END-WIN32-CUT- */
#  define SRC_IS_DEST \
	(st_src.st_dev == st_dest.st_dev && st_src.st_ino == st_dest.st_ino)
#endif		/* -WIN32-CUT-LINE- */

	/*
	** make sure that we are not about to copy a file onto itself;
	** also, ensure that a destination that exists is a plain file
	**
	** if dest_exists is true, st_dest contains valid data;
	** if dest_is_directory is false, dest refers to the destination file
	** (if true, dest now refers to a file yet to be created)
	*/

	if (dest_exists && !dest_is_directory) {
		if (SRC_IS_DEST) {
			(void) sprintf(msg, fmt2, MAXPATH, src, MAXPATH, dest);
			error(0, msg, "(source and destination are identical)");
			return(-1);
		}
		if (!S_ISREG(st_dest.st_mode)) {
			(void) sprintf(msg, fmt2, MAXPATH, src, MAXPATH, dest);
			error(0, msg,
				"(destination exists and is not a plain file)");
			return(-1);
		}
	}

	/*
	**	at last we are ready to do the copy
	*/

	TRACE3(Tfcopy, 8, "FILE COPY from <%s> to <%s>", src, dest);

	errno = 0;
	if ((ifp = fopen(src, "rb")) == (FILE *) 0) {
		save_errno = errno;
		error(errno, "can't open", src);
		errno = save_errno;
		return(-1);
	}

	errno = 0;
	if ((ofp = fopen(dest, "wb")) == (FILE *) 0) {
		save_errno = errno;
		error(errno, "can't open", dest);
		(void) fclose(ifp);
		errno = save_errno;
		return(-1);
	}

	rc = 0;
	while ((nread = fread(buf, sizeof buf[0], sizeof buf, ifp)) > 0) {
		(void) fwrite(buf, sizeof buf[0], (size_t) nread, ofp);
		if (ferror(ofp)) {
			save_errno = errno;
			error(errno, "write error on", dest);
			errno = save_errno;
			rc = -1;
			break;
		}
	}

	if (ferror(ifp)) {
		save_errno = errno;
		error(errno, "read error on", src);
		errno = save_errno;
		rc = -1;
	}

	(void) fclose(ifp);
	if (fclose(ofp) < 0) {
		save_errno = errno;
		error(errno, "close error on", dest);
		errno = save_errno;
		rc = -1;
	}

	/* finally, fix up the mode of the destination file if necessary */
	if (
		rc == 0 &&
		STAT(dest, &st_dest) == 0 &&
		((st_src.st_mode & MODEMASK) != (st_dest.st_mode & MODEMASK)) &&
		CHMOD(dest, st_src.st_mode & MODEMASK) < 0
	) {
		save_errno = errno;
		error(errno, "warning: can't chmod", dest);
		errno = save_errno;
	}

	return(rc);
}

/*
**
**	rdcopy - recursively copy files from one directory to another
**
*/

static int rdcopy (from, to)
char *from;
char *to;
{
	DIR *dirp;
        struct dirent *dp;
        char fromname[MAXPATH + 1], toname[MAXPATH + 1];
	int errcount = 0;
	int len1, len2, save_errno;
	static char fmt[] = "recursive directory copy from %.*s to %.*s";
	char msg[sizeof fmt + (MAXPATH * 2)];

	TRACE3(Tfcopy, 8, "rdcopy(): src = <%s>, dest = <%s>", from, to);

	/* ensure that the destination is not below the source tree */
	len1 = (int) strlen(from);
	len2 = (int) strlen(to);
	if (
#ifdef _WIN32		/* -START-WIN32-CUT- */
		!_strnicmp(from, to, len1)
#else			/* -END-WIN32-CUT- */
		!strncmp(from, to, len1)
#endif			/* -WIN32-CUT-LINE- */
		&&
		(len1 == len2 || isdirsep(*(to + len1)))
	) {
		(void) sprintf(msg, fmt, MAXPATH, from, MAXPATH, to);
		error(0, msg, "would never return!");
		return(-1);
	}

        if ((dirp = OPENDIR(from)) == (DIR *) 0) {
		save_errno = errno;
		error(errno, "can't open directory", from);
		errno = save_errno;
                return (-1);
        }

	/*
	** read each directory entry in turn and copy it to the destination;
	** if 'fromname' has been deleted by another process, it is not
	** treated as an error
	*/
	len1 = (int) sizeof fromname - (int) strlen(from) - 2;
	len2 = (int) sizeof toname - (int) strlen(to) - 2;
	while ((dp = READDIR(dirp)) != (struct dirent *) 0) {
                if (!strcmp(dp->d_name, ".") || !strcmp(dp->d_name, ".."))
                        continue;
		(void) sprintf(fromname, "%.*s/%.*s",
			(int) (sizeof fromname - 2), from,
			TET_MAX(len1, 0), dp->d_name);
		(void) sprintf(toname, "%.*s/%.*s",
			(int) (sizeof toname - 2), to,
			TET_MAX(len2, 0), dp->d_name);
		if ((tet_fcopy(fromname, toname) != 0) && (errno != ENOENT))
                	errcount++;
        }

	save_errno = errno;
	(void) CLOSEDIR(dirp);
	errno = save_errno;
	return (errcount > 0 ? -1 : 0);
}

