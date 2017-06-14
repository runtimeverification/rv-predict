/*
 *	SCCS: @(#)errno.c	1.9 (99/11/15)
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
static char sccsid[] = "@(#)errno.c	1.9 (99/11/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)errno.c	1.9 99/11/15 TETware release 3.8
NAME:		'C' API error reporting mechanism
PRODUCT:	TETware
AUTHOR:		Geoff Clare, UniSoft Ltd.
DATE CREATED:	July 1996
SYNOPSIS:

	int	tet_errno;
	int *	tet_thr_errno(void);

DESCRIPTION:

	Tet_errno is set to a non-zero value by API functions when
	an error is indicated by the function's return value.
	In the threads version of the API, tet_errno is really
	a #define, enabling each thread to have a separate tet_errno.

	Tet_thr_errno() is not part of the API: it is used (in the
	threads version) in the #define of tet_errno.

	Where API functions call others, and wish to use tet_errno in
	reporting errors, tet_errno must be passed NEGATED to the
	internal error reporting functions such as tet_error(), error()
	and fatal().

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., February 1998
	Use TETware-specific macros to access threads functions and
	data items.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
	Andrew Dingwall, UniSoft Ltd., July 1999
	tet_errlist and tet_nerr moved to errlist.c
	added support for 'other language' APIs

	Andrew Dingwall, UniSoft Ltd., October 1999
	added support for strict POSIX threads


************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include "dtmac.h"
#include "dtthr.h"
#include "tet_api.h"
#include "error.h"
#include "apilib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;
#endif

#ifndef TET_THREADS
TET_IMPORT int tet_errno;
#else /* !TET_THREADS */

TET_IMPORT tet_thread_key_t tet_errno_key;

TET_IMPORT int *tet_thr_errno()
{
	/* find tet_errno address for this thread */

	void *rtval;

#  ifdef TET_STRICT_POSIX_THREADS
	static int child_tet_errno;

	if (IS_CHILD_OF_MULTITHREAD_PARENT)
		return(&child_tet_errno);
#  endif

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	rtval = 0;
	TET_THR_GETSPECIFIC(tet_errno_key, &rtval);
	if (rtval == 0)
	{
		/* No tet_errno has been set up for this thread - probably
		   because it was not created with tet_thr_create().
		   Try and allocate a new tet_errno. */

		rtval = malloc(sizeof(int));
		TET_THR_SETSPECIFIC(tet_errno_key, rtval);
		rtval = 0;
		TET_THR_GETSPECIFIC(tet_errno_key, &rtval);
		if (rtval == 0)
			fatal(0, "could not set up tet_errno for new thread in tet_thr_errno", (char *)0);
		*((int *)rtval) = 0;
	}

	return (int *)rtval;
}
#endif /* !TET_THREADS */

