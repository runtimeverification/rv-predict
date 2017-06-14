/*
 *      SCCS:  @(#)error.h	1.7 (98/08/28) 
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

/************************************************************************

SCCS:   	@(#)error.h	1.7 98/08/28 TETware release 3.8
NAME:		error.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	error message printing macros
	avoids cluttering source files with long function call lines

	each .c file that includes this file needs a line of the form

		static char srcFile[] = __FILE__;

	the file containing main() should contain pointers called
	(*tet_liberror)() and (*tet_libfatal)() which should be initialised
	to point to the error handler and fatal error handler routines to use

	the functions tet_generror() and tet_genfatal() are provided in
	dtet2lib and are sufficient for use where no special error
	processing is required

MODIFICATIONS:
	Geoff Clare, UniSoft Ltd., Oct 1996
	Add prototypes, so this can be included in C++ source.

	Andrew Dingwall, UniSoft Ltd., January 1998
	Use ANSI-style macro syntax on Win32 systems and C++ as well.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared libraries.


************************************************************************/


TET_IMPORT_FUNC_PTR(void, tet_liberror,
	PROTOLIST((int, char *, int, char *, char *)));
TET_IMPORT_FUNC_PTR(void, tet_libfatal,
	PROTOLIST((int, char *, int, char *, char *)));

#define error(errnum, s1, s2) \
	(*tet_liberror)(errnum, srcFile, __LINE__, s1, s2)
#define fatal(errnum, s1, s2) \
	(*tet_libfatal)(errnum, srcFile, __LINE__, s1, s2)

#ifndef NEEDsrcFile
#define NEEDsrcFile
#endif

/* test an assertion, exit if it fails */
TET_IMPORT_ARRAY(char, tet_assertmsg, []);
#if defined(__STDC__) || defined(_WIN32) || defined(__cplusplus)
/* assume cpp understands #x whatever the value of __STDC__ */
#  define ASSERT(x)	if (!(x)) fatal(0, tet_assertmsg, #x); else
#else
#  define ASSERT(x)	if (!(x)) fatal(0, tet_assertmsg, "x"); else
#endif

/* test an assertion only in TETware-Lite */
#ifdef TET_LITE	/* -LITE-CUT-LINE- */
#  define ASSERT_LITE(x)	ASSERT(x)
#else	/* -START-LITE-CUT- */
#define ASSERT_LITE(x)
#endif	/* -END-LITE-CUT- */

