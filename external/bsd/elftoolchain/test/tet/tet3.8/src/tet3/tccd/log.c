/*
 *      SCCS:  @(#)log.c	1.13 (99/09/02) 
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
static char sccsid[] = "@(#)log.c	1.13 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)log.c	1.13 99/09/02 TETware release 3.8
NAME:		log.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	tccd error reporting functions

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., July 1997
	added support the MT DLL version of the C runtime support library
	on Win32 systems

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
	Andrew Dingwall, UniSoft Ltd., August 1999
	Removed static initialisation of lfp for compatibility with glibc.

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <errno.h>
#include <time.h>
#include <sys/types.h>
#include <sys/stat.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <io.h>
#endif		/* -END-WIN32-CUT- */
#include "dtmac.h"
#include "error.h"
#include "globals.h"
#include "dtmsg.h"
#include "ptab.h"
#include "tccd.h"
#include "dtetlib.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* the log file stream pointer */
static FILE *lfp;


/* static function declarations */
static FILE *getlogfp PROTOLIST((void));
static char *loghdr PROTOLIST((void));


/*
**	loginit() - open the TCCD log file
*/

void loginit()
{
	FILE *fp;
	int n;
	struct STAT_ST stbuf;
#ifdef _WIN32	/* -START-WIN32-CUT- */
	static char devnull[] = "nul";
#else		/* -END-WIN32-CUT- */
	static char devnull[] = "/dev/null";
#endif		/* -WIN32-CUT-LINE- */
	static char buf[BUFSIZ];
	extern char *Logfile;

	/* make sure that the logfile open will get a fd > 2, otherwise
		all kinds of trouble breaks out when we dup fds later */
	for (n = 0; n < 3; n++)
		if (FSTAT(n, &stbuf) < 0 && OPEN(devnull, O_RDONLY, 0) < 0)
			fatal(errno, "can't open", devnull);
	errno = 0;

	/* open the log file */
	if ((fp = fopen(Logfile, "a")) == NULL)
		fatal(errno, "can't open log file", Logfile);

	/* allow anyone to read/write to the logfile */
	(void) CHMOD(Logfile, (mode_t)S_IRUSR|S_IWUSR|S_IRGRP|S_IWGRP|S_IROTH|S_IWOTH);

	/* attach stderr to the logfile */
	errno = 0;
	if ((lfp = freopen(Logfile, "a", stderr)) != stderr) {
		lfp = fp;
		fatal(errno, "can't reopen stderr as", Logfile);
	}

	(void) fclose(fp);

	setbuf(lfp, buf);

	if (tet_fappend(FILENO(lfp)) < 0)
		exit(1);
}

/*
**	logent() - make an entry in the TCCD log file
*/

void logent(s1, s2)
char *s1, *s2;
{
	register FILE *fp;

	fp = getlogfp();

	(void) fprintf(fp, "%s: %s", loghdr(), s1);
	if (s2 && *s2)
		(void) fprintf(fp, " %s", s2);
	(void) putc('\n', fp);
	(void) fflush(fp);
}

/*
**	logerror() - TCCD error printing routine
*/

void logerror(errnum, file, line, s1, s2)
int errnum, line;
char *file, *s1, *s2;
{
	tet_prerror(getlogfp(), errnum, loghdr(), file, line, s1, s2);
}

/*
**	loghdr() - construct a TCCD error message preamble
**		and return a pointer thereto
*/

static char *loghdr()
{
	static char *month[] = { "Jan", "Feb", "Mar", "Apr", "May", "Jun",
		"Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
	time_t t;
	register struct tm *tp;
	static char header[50];

	t = time((time_t *) 0);
	tp = localtime(&t);
	(void) sprintf(header, "%.16s (%d) %d %s %d:%02d:%02d",
		tet_progname, tet_mypid, tp->tm_mday, month[tp->tm_mon],
		tp->tm_hour, tp->tm_min, tp->tm_sec);

	return(header);
}

/*
**	getlogfp() - return the logfile stream pointer
*/

static FILE *getlogfp()
{
	struct STAT_ST stbuf;
	static FILE *cfp;
	static int cfd = -1;
#ifdef _WIN32	/* -START-WIN32-CUT- */
	static char console[] = "con";
#else		/* -END-WIN32-CUT- */
	static char console[] = "/dev/console";
#endif		/* -WIN32-CUT-LINE- */

	if (lfp == (FILE *) 0)
		lfp = stderr;

	if (!lfp || FSTAT(FILENO(lfp), &stbuf) < 0) {
		if (!cfp) {
			if (cfd < 0)
				cfd = OPEN(console, O_WRONLY | O_NOCTTY, 0);
			if (cfd >= 0)
				cfp = FDOPEN(cfd, "w");
		}
		return(cfp);
	}
	else
		return(lfp);
}

