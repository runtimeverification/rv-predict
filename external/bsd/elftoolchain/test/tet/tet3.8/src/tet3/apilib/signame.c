/*
 *	SCCS: @(#)signame.c	1.1 (99/09/02)
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
static char sccsid[] = "@(#)signame.c	1.1 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)signame.c	1.1 99/09/02 TETware release 3.8
NAME:		signame.c
PRODUCT:	TETware
AUTHOR:		Geoff Clare, UniSoft Ltd.
DATE CREATED:	26 July 1990
SYNOPSIS:

	char *	tet_signame(int signum);

DESCRIPTION:

	tet_signame() is not part of the API.
	It is used by API functions to obtain names for the standard 
	signal names.

	This function is not implemented on WIN32 platforms.


MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1999
	tet_signame() moved from tet_fork.c to here.

 
************************************************************************/

#ifndef _WIN32	/* -WIN32-CUT-LINE- */

#include <stdio.h>
#include <signal.h>
#include "dtmac.h"
#include "apilib.h"


TET_IMPORT char *tet_signame(sig)
int sig;
{
	/* look up name for given signal number */

	/* the table must contain standard signals only - a return
	   value not starting with "SIG" is taken to indicate a
	   non-standard signal */

	int	i;
	static	struct {
		int num;
		char *name;
	} sig_table[] = {
		{ SIGABRT,	"SIGABRT" },
		{ SIGALRM,	"SIGALRM" },
		{ SIGCHLD,	"SIGCHLD" },
		{ SIGCONT,	"SIGCONT" },
		{ SIGFPE,	"SIGFPE" },
		{ SIGHUP,	"SIGHUP" },
		{ SIGILL,	"SIGILL" },
		{ SIGINT,	"SIGINT" },
		{ SIGKILL,	"SIGKILL" },
		{ SIGPIPE,	"SIGPIPE" },
		{ SIGQUIT,	"SIGQUIT" },
		{ SIGSEGV,	"SIGSEGV" },
		{ SIGSTOP,	"SIGSTOP" },
		{ SIGTERM,	"SIGTERM" },
		{ SIGTSTP,	"SIGTSTP" },
		{ SIGTTIN,	"SIGTTIN" },
		{ SIGTTOU,	"SIGTTOU" },
		{ SIGUSR1,	"SIGUSR1" },
		{ SIGUSR2,	"SIGUSR2" },
		{ 0,		(char *) 0 }
	};


	for (i = 0; sig_table[i].name != (char *) 0; i++)
	{
		if (sig_table[i].num == sig)
			return sig_table[i].name;
	}

	return "unknown signal";
}

#else			/* -START-WIN32-CUT- */

int tet_signame_not_implemented;

#endif /* !_WIN32 */	/* -END-WIN32-CUT- */


