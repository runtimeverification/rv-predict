/*
 *	SCCS: @(#)tetchild.c	1.2 (99/11/15)
 *
 *	UniSoft Ltd., London, England
 *
 * Copyright (c) 1999 The Open Group
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

/*
 * Copyright 1990 Open Software Foundation (OSF)
 * Copyright 1990 Unix International (UI)
 * Copyright 1990 X/Open Company Limited (X/Open)
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose and without fee is hereby granted, provided
 * that the above copyright notice appear in all copies and that both that
 * copyright notice and this permission notice appear in supporting
 * documentation, and that the name of OSF, UI or X/Open not be used in 
 * advertising or publicity pertaining to distribution of the software 
 * without specific, written prior permission.  OSF, UI and X/Open make 
 * no representations about the suitability of this software for any purpose.  
 * It is provided "as is" without express or implied warranty.
 *
 * OSF, UI and X/Open DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, 
 * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO 
 * EVENT SHALL OSF, UI or X/Open BE LIABLE FOR ANY SPECIAL, INDIRECT OR 
 * CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF 
 * USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR 
 * OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR 
 * PERFORMANCE OF THIS SOFTWARE.
 */

#ifndef lint
static char sccsid[] = "@(#)tetchild.c	1.2 (99/11/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tetchild.c	1.2 99/11/15 TETware release 3.8
NAME:		tetchild.c
PRODUCT:	TETware
AUTHOR:		Geoff Clare, UniSoft Ltd.
DATE CREATED:	26 July 1990
SYNOPSIS:

	pid_t	tet_child;  [ per-thread ]

	pid_t *	tet_thr_child(void);

DESCRIPTION:

	After a call to tet_fork(), tet_child in the parent process is
	set to the PID of the child process.

	Tet_thr_child() is not part of the API: it is used (in the
	threads version) in the #define of tet_child.

	This interface is not implemented on WIN32 platforms.


MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., July 1999
	tet_child moved from tet_fork.c to here.

	Andrew Dingwall, UniSoft Ltd., October 1999
	added support for strict POSIX threads

************************************************************************/

#ifndef _WIN32	/* -WIN32-CUT-LINE- */

#include <stdio.h>
#include <stdlib.h>
#include "dtmac.h"
#include "dtthr.h"
#include "error.h"
#include "tet_api.h"
#include "apilib.h"


#  ifndef TET_THREADS

TET_IMPORT pid_t tet_child;

#  else		/* TET_THREADS */


#  ifdef NEEDsrcFile
     static char srcFile[] = __FILE__;	/* file name for error reporting */
#  endif


TET_IMPORT tet_thread_key_t tet_child_key;

TET_IMPORT pid_t *tet_thr_child()
{
	/* find tet_child address for this thread */

	void *rtval;

#  ifdef TET_STRICT_POSIX_THREADS
	static pid_t child_tet_child;

	if (IS_CHILD_OF_MULTITHREAD_PARENT)
		return(&child_tet_child);
#  endif

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	rtval = 0;
	TET_THR_GETSPECIFIC(tet_child_key, &rtval);
	if (rtval == 0)
	{
		/* No tet_child has been set up for this thread - probably
		   because it was not created with tet_thr_create().
		   Try and allocate a new tet_child. */

		rtval = malloc(sizeof(pid_t));
		TET_THR_SETSPECIFIC(tet_child_key, rtval);
		rtval = 0;
		TET_THR_GETSPECIFIC(tet_child_key, &rtval);
		if (rtval == 0)
			fatal(0, "could not set up tet_child for new thread in tet_thr_child", (char *)0);
		*((pid_t *)rtval) = 0;
	}

	return (pid_t *)rtval;
}
#  endif	/* TET_THREADS */

#else		/* -START-WIN32-CUT- */

int tet_child_not_implemented;

#endif		/* -END-WIN32-CUT- */

