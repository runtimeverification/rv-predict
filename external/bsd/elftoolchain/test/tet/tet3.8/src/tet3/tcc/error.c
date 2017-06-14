/*
 *	SCCS: @(#)error.c	1.7 (98/09/01)
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
static char sccsid[] = "@(#)error.c	1.7 (98/09/01) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)error.c	1.7 98/09/01 TETware release 3.8
NAME:		error.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	tcc error handlers

MODIFICATIONS:
	Geoff Clare, UniSoft Ltd., August 1996
	Missing <string.h>.

	Andrew Dingwall, UniSoft Ltd., July 1997
	added support the MT DLL version of the C runtime support library
	on Win32 systems

	Andrew Dingwall, UniSoft Ltd., March 1998
	replaced references to sys_errlist[] and sys_nerr with
	a call to strerror()

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <time.h>
#ifndef TET_LITE	/* -START-LITE-CUT- */
#  ifdef _WIN32		/* -START-WIN32-CUT- */
#    include <winsock.h>
#  endif /* _WIN32 */	/* -END-WIN32-CUT- */
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
#include "dtmac.h"
#include "ltoa.h"
#include "globals.h"
#include "dtetlib.h"
#include "scentab.h"
#include "proctab.h"
#include "tcc.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

#define MSGSIZE	((MAXPATH * 2) + 128)	/* size of a message buffer */

/* static function declarations */
static void errfmt PROTOLIST((int, char *, int, char *, char *, char *,
	char []));


/*
**	tcc_error() - TCC error handler
*/

void tcc_error(errnum, file, line, s1, s2)
int errnum, line;
char *file, *s1, *s2;
{
	char msg[MSGSIZE];

	/* format the error message */
	errfmt(errnum, file, line, s1, s2, (char *) 0, msg);

	/* print the message to the journal if possible, otherwise to stderr */
	if (jnl_usable())
		jnl_tcc_msg(msg);
	else {
		(void) fprintf(stderr, "%s %s\n", tet_progname, msg);
		(void) fflush(stderr);
	}
}

/*
**	tcc_fatal() - TCC fatal error handler
*/

void tcc_fatal(errnum, file, line, s1, s2)
int errnum, line;
char *file;
char *s1, *s2;
{
	tcc_error(errnum, file, line, s1, s2);
	tcc_exit(1);
}

/*
**	tcc_prperror() - TCC context-dependent error handler
**		for use during test case execution
*/

void tcc_prperror(prp, sysid, errnum, file, line, s1, s2)
struct proctab *prp;
int sysid, errnum, line;
char *file, *s1, *s2;
{
	static char fmt[] = "on system %03d";
	char text[sizeof fmt + LNUMSZ];
	char msg[MSGSIZE];

	/* format the message */
	if (sysid >= 0)
		(void) sprintf(text, fmt, sysid);
	else
		text[0] = '\0';
	errfmt(errnum, file, line, s1, s2, text, msg);

	/* punt the message for printing to the current journal */
	jnl_tcc_prpmsg(prp, msg);
}

/*
**	errfmt() - format an error message into the msg buffer
*/

static void errfmt(errnum, file, line, s1, s2, s3, msg)
int errnum, line;
char *file, *s1, *s2, *s3, msg[];
{
	register char *p = msg;

	/* generate the source file and line number */
	(void) sprintf(p, "(%s, %d): ", file, line);
	p += strlen(p);

	/* append the first message string */
	while (*s1 && p < &msg[MSGSIZE - 1])
		*p++ = *s1++;

	/* append the second message string if there is one */
	if (s2 && *s2 && p < &msg[MSGSIZE - 2]) {
		*p++ = ' ';
		while (*s2 && p < &msg[MSGSIZE - 1])
			*p++ = *s2++;
	}

	/* append the third message string if there is one */
	if (s3 && *s3 && p < &msg[MSGSIZE - 2]) {
		*p++ = ' ';
		while (*s3 && p < &msg[MSGSIZE - 1])
			*p++ = *s3++;
	}

#ifdef TET_LITE		/* -LITE-CUT-LINE- */
#  define REPLY_CODE_STRING	", reply code = "
#else			/* -START-LITE-CUT- */
#  define REPLY_CODE_STRING	", server reply code = "
#endif /* TET_LITE */	/* -END-LITE-CUT- */

	/*
	** generate the errnum information - errnum can be
	** an errno (+ve values), a server reply code (-ve values) or
	** zero (meaning don't print anything)
	*/
	if (errnum) {
		if (errnum > 0) {
			s1 = ": ";
#ifndef TET_LITE	/* -START-LITE-CUT- */
#  ifdef _WIN32		/* -START-WIN32-CUT- */
			if (errnum >= WSABASEERR)
				s2 = tet_wsaerrmsg(errnum);
			else
#  endif /* _WIN32 */	/* -END-WIN32-CUT- */
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
				if ((s2 = strerror(errnum)) == (char *) 0) {
					s1 = ", errno = ";
					s2 = tet_errname(errnum);
				}
		}
		else {
			s1 = REPLY_CODE_STRING;
			s2 = tet_ptrepcode(errnum);
		}
		while (*s1 && p < &msg[MSGSIZE - 1])
			*p++ = *s1++;
		while (*s2 && p < &msg[MSGSIZE - 1])
			*p++ = *s2++;
	}
	*p = '\0';

#if 0
	TRACE2(tet_Ttcc + tet_Tscen + tet_Texec + tet_Tbuf + tet_Tio +
		tet_Tserv + tet_Tloop, 1, "ERROR: %s", msg);
#endif

}

