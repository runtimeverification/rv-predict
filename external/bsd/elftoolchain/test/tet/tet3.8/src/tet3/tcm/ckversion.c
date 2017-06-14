/*
 *	SCCS: @(#)ckversion.c	1.2 (99/07/20)
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
static char sccsid_ckversion[] = "@(#)ckversion.c	1.2 (99/07/20) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)ckversion.c	1.2 99/07/20 TETware release 3.8
NAME:		ckversion.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	July 1998

DESCRIPTION:
	function to check the version number of the shared API library

	this file must reside in the staticly-linked part of the TCM
	no calls to TETware library functions are allowed from this file

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., July 1999
	moved tcm code out of the api library


************************************************************************/

/*
** This file is a component of the TCM (tcm.o) and/or one of the child
** process controllers (tcmchild.o and tcmrem.o).
** On UNIX systems, these .o files are built using ld -r.
** There is no equivalent to ld -r in MSVC, so on Win32 systems each .c
** file is #included in a scratch .c or .cpp file and a single object
** file built from that.
**
** This imposes some restictions on the contents of this file:
**
**	+ Since this file might be included in a C++ program, all
**	  functions must have both ANSI C and common C definitions.
**
**	+ The only .h file that may appear in this file is tcmhdrs.h;
**	  all other .h files that are needed must be #included in there.
**
**	+ The scope of static variables and functions encompasses all
**	  the source files, not just this file.
**	  So all static variables and functions must have unique names.
*/


/*
** all the header files are included by tcmhdrs.h
** don't include any other header files directly
*/
#include "tcmhdrs.h"


/*
** the definition of TET_VERSION must be here and not in a header file
** because we want to pick up the value of the Q keyword that is defined
** in this file
**
** this definition of TET_VERSION (before expansion by SCCS) must be the same
** as the one in apilib/libvers.c
*/
#ifdef TET_LITE /* -LITE-CUT-LINE- */
#  define TET_VERSION			"3.8-lite"
#else /* -START-LITE-CUT- */
#  define TET_VERSION			"3.8"
#endif /* -END-LITE-CUT- */

#define TET_VERSION_STRINGS		expected_apilib_version
#define TET_VERSION_STORAGE_CLASS	static

/*
** apilib/version.c contains a definition of TET_VERSION_STRINGS in terms
** of the #defines supplied in this file
*/
#include "../apilib/version.c"


/* static function declarations */
static int mstrcmp PROTOLIST((char **, char **));
static void rptversion PROTOLIST((char *, char **));


/*
**	tet_check_apilib_version() - check that the version number in
**		the shared API library is what we expect
**
**	there is no return if they're different
*/

#ifdef PROTOTYPES
void tet_check_apilib_version(void)
#else
void tet_check_apilib_version()
#endif
{
	if (mstrcmp(tet_apilib_version, expected_apilib_version)) {
		(void) fprintf(stderr,
			"%s: using wrong version of the API library\n",
			tet_progname);
		rptversion("expected", expected_apilib_version);
		rptversion("found   ", tet_apilib_version);
		exit(1);
	}
}

/*
**	mstrcmp() - compare two arrays or strings
**
**	return -ve value, zero or +ve value if sp1 is found, respectively,
**	to be less than, equal to or greater than sp2
*/

#ifdef PROTOTYPES
static int mstrcmp(char **sp1, char **sp2)
#else
static int mstrcmp(sp1, sp2)
char **sp1, **sp2;
#endif
{
	int rc;

	while (*sp1 && *sp2)
		if ((rc = strcmp(*sp1++, *sp2++)) != 0)
			return(rc);

	if (!*sp1 && !*sp2)
		return(0);
	else if (!*sp1)
		return(-1);
	else
		return(1);
}

/*
**	rptversion() - report a version string array to stderr
*/

#ifdef PROTOTYPES
static void rptversion(char *s, char **sp)
#else
static void rptversion(s, sp)
char *s, **sp;
#endif
{
	(void) fprintf(stderr, "%s: %s version:", tet_progname, s);
	while (*sp)
		(void) fprintf(stderr, " %s", *sp++);
	(void) putc('\n', stderr);
	(void) fflush(stderr);
}

