/*
 *	SCCS: @(#)killw.c	1.1 (99/09/02)
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
static char sccsid[] = "@(#)killw.c	1.1 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)killw.c	1.1 99/09/02 TETware release 3.8
NAME:		killw.c
PRODUCT:	TETware
AUTHOR:		Geoff Clare, UniSoft Ltd.
DATE CREATED:	26 July 1990
SYNOPSIS:

	int	tet_killw(pid_t child, unsigned timeout);

DESCRIPTION:

	Tet_killw() is not part of the API.  It is used by other functions
	in the API to kill a child process (with SIGTERM) and wait for it.
	If the wait times out it will kill the child with SIGKILL.  If the
	wait fails for any other reason -1 is returned.  If the child
	exits or was not there (ECHILD) then 0 is returned.  If the kill()
	fails and errno is ESRCH the wait is done anyway (to reap a
	possible zombie).  On return the value of errno is restored to
	the value set by the failed system call.

	Note that, because of the second wait after SIGKILL, the time spent
	in this routine may be twice the timeout given.

	This function is not implemented on WIN32 platforms.


MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1999
	Moved tet_killw() from tet_fork.c to here.

************************************************************************/

#ifndef _WIN32	/* -WIN32-CUT-LINE- */

#include <stdio.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <errno.h>
#include <signal.h>
#include "dtmac.h"
#include "dtthr.h"
#include "alarm.h"
#include "error.h"
#include "tet_api.h"	/* for TET_PID_T_DEFINED */
#include "apilib.h"

#  ifdef NEEDsrcFile
     static char srcFile[] = __FILE__;
#  endif


TET_IMPORT int tet_killw(child, timeout)
pid_t child;
unsigned int timeout;
{
	/* kill child and wait for it (with timeout) */

	pid_t	pid;
	int	sig = SIGTERM;
	int	ret = -1;
	int	err, count, status;
	struct alrmaction new_aa, old_aa; 

	new_aa.waittime = timeout; 
	new_aa.sa.sa_handler = tet_catch_alarm; 
	new_aa.sa.sa_flags = 0; 
	(void) sigemptyset(&new_aa.sa.sa_mask); 

	for (count = 0; count < 2; count++)
	{
		if (kill(child, sig) == -1 && errno != ESRCH)
		{
			err = errno;
			break;
		}

		tet_alarm_flag = 0; 
		if (tet_set_alarm(&new_aa, &old_aa) == -1)
			fatal(errno, "failed to set alarm", (char *)0);
		pid = waitpid(child, &status, 0);
		err = errno;
		(void) tet_clr_alarm(&old_aa);

		if (pid == child)
		{
			ret = 0;
			break;
		}
		if (pid == -1 && tet_alarm_flag == 0 && errno != ECHILD)
			break;
		
		sig = SIGKILL; /* use a stronger signal the second time */
	}

	errno = err;
	return ret;
}

#else			/* -START-WIN32-CUT- */

int tet_killw_not_implemented;

#endif /* !_WIN32 */	/* -END-WIN32-CUT- */


