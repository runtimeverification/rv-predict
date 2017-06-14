/*
 *	SCCS: @(#)globals.c	1.1 (98/09/01)
 *
 *	UniSoft Ltd., London, England
 *
 * Copyright (c) 1998 The Open Group
 * All rights reserved.
 *
 * No part of this source code may be reproduced, stored in a retrieval
 * system, or transmitted, in any form or by any means, electronic,
 * mechanical, photocopying, recording or otherwise, except as stated
 * in the end-user licence agreement, without the prior permission of
 * the copyright owners.
 * A copy of the end-user licence agreement is contained in the file
 * Licence which accompanies this distribution.
 * 
 * Motif, OSF/1, UNIX and the "X" device are registered trademarks and
 * IT DialTone and The Open Group are trademarks of The Open Group in
 * the US and other countries.
 *
 * X/Open is a trademark of X/Open Company Limited in the UK and other
 * countries.
 *
 */

#ifndef lint
static char sccsid[] = "@(#)globals.c	1.1 (98/09/01) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)globals.c	1.1 98/09/01 TETware release 3.8
NAME:		globals.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	July 1998

DESCRIPTION:
	global variables that must be supplied by all programs that
	use the API library

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <process.h>
#else		/* -END-WIN32-CUT- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtmsg.h"
#include "error.h"
#include "globals.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* global data items */
TET_IMPORT char *tet_progname = "<unknown>";
					/* my program name */
TET_IMPORT int tet_mypid = -1;		/* my program ID */
TET_IMPORT int tet_myptype = PT_NOPROC;	/* my process type */
TET_IMPORT int tet_mysysid = -1;	/* my system ID */
TET_IMPORT char tet_root[MAXPATH];	/* TET_ROOT from the environment */
TET_IMPORT void (*tet_liberror) PROTOLIST((int, char *, int, char *, char *));
					/* ptr to error handler function */
TET_IMPORT void (*tet_libfatal) PROTOLIST((int, char *, int, char *, char *));
					/* ptr to fatal error handler */

/* static function declarations */
static void minfatal PROTOLIST((int, char *, int, char *, char *));


/*
**	tet_init_globals() - initialise the global data items
**
**	a call to this function should be the very first thing in a
**	program's main() function
**
**	note that initialisation of all the data items is not guaranteed
**	after a call to this function;
**	in particular, tet_root might not be initialised
**	(not all programs need it)
**	and some programs don't know all the information at startup time;
**	they have to fill in the information by hand later on
*/

TET_IMPORT void tet_init_globals(progname, ptype, sysid, liberror, libfatal)
char *progname;
int ptype, sysid;
void (*liberror) PROTOLIST((int, char *, int, char *, char *));
void (*libfatal) PROTOLIST((int, char *, int, char *, char *));
{
	char *p;

	if (progname && *progname)
		tet_progname = progname;

	tet_mypid = GETPID();

	if (ptype > 0)
		tet_myptype = ptype;

	if (sysid >= 0)
		tet_mysysid = sysid;

	if ((p = getenv("TET_ROOT")) != (char *) 0)
		(void) sprintf(tet_root, "%.*s", (int) sizeof tet_root - 1, p);

	if (!tet_libfatal)
		tet_libfatal = minfatal;
	ASSERT(liberror);
	tet_liberror = liberror;
	ASSERT(libfatal);
	tet_libfatal = libfatal;
}

/*
**	minfatal() - minimal fatal error reporting function
**
**	sufficient for use by the ASSERT() macro calls above
*/

static void minfatal(err, file, line, s1, s2)
int err, line;
char *file, *s1, *s2;
{
	if (tet_liberror)
		(*tet_liberror)(err, file, line, s1, s2);
	else
		(void) fprintf(stderr, "%s (%s, %d): %s %s\n",
			tet_progname, file, line, s1, s2 ? s2 : "");
	exit(1);
}

