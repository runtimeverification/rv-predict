/*
 *	SCCS: @(#)tet_getpid.c	1.3 (05/12/07)
 *
 *	UniSoft Ltd., London, England
 *
 * Copyright (c) 1999 UniSoft Ltd.
 * All rights reserved.
 *
 * No part of this source code may be reproduced, stored in a retrieval
 * system, or transmitted, in any form or by any means, electronic,
 * mechanical, photocopying, recording or otherwise, except as stated
 * in the end-user licence agreement, without the prior permission of
 * the copyright owners.
 * A copy of the end-user licence agreement is contained in the file
 * Licence which accompanies this distribution.
 */

#ifndef lint
static char sccsid[] = "@(#)tet_getpid.c	1.3 (05/12/07) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tet_getpid.c	1.3 05/12/07 TETware release 3.8
NAME:		tet_getpid.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	June 1999

DESCRIPTION:
	Helper process for the distributed Korn Shell API.
	Prints the PID of the parent process on stdout.
	This enable a (sub)shell to determine its process ID.

	(Recall that in the shell $$ always refers to the PID of the top
	level shell.)

	This program uses getppid() to determine the PID of the calling
	process.
	There doesn't seem to be an equivalent function in Win32, so I
	don't think this program can be ported to Win32 systems.

MODIFICATIONS:
	Geoff Clare, The Open Group, December 2005
	On Win32 systems return an error at runtime instead of
	disallowing compilation.

************************************************************************/

#ifdef _WIN32	/* -START-WIN32-CUT- */

int main()
{
	/*
	 * Not implemented for Win32 - return an error.
	 *
	 * (The tet_getpid helper is only used by the undocumented
	 * tet_fork() function, so this just means tet_fork() can't be
	 * used on Win32 systems.)
	 */

	return 1;
}

#else		/* -END-WIN32-CUT- */

#include <stdio.h>
#include <unistd.h>

int main()
{
	(void) printf("%lu\n", (unsigned long) getppid());
	return 0;
}

#endif		/* -WIN32-CUT-LINE- */

