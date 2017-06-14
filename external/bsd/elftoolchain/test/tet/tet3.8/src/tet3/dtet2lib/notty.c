/*
 *      SCCS:  @(#)notty.c	1.12 (05/06/30) 
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
static char sccsid[] = "@(#)notty.c	1.12 (05/06/30) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)notty.c	1.12 05/06/30 TETware release 3.8
NAME:		notty.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to dissociate from control terminal and start a new process
	group

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., January 1994
	added setsid() call to support POSIX-only systems
	(mainly for the FIFO implementation)

	Geoff Clare, The Open Group, June 2005
	Also use setsid() if _XOPEN_SOURCE is defined.

************************************************************************/

#include <stdio.h>

#ifdef _WIN32		/* -START-WIN32-CUT- */
#  include <signal.h>
#else /* _WIN32 */	/* -END-WIN32-CUT- */
#  if defined(_POSIX_SOURCE) || defined(_POSIX_C_SOURCE) || defined(_XOPEN_SOURCE)
#    define HAS_SETSID
#  endif /* _POSIX_SOURCE etc. */
#  ifdef HAS_SETSID
#    include <unistd.h>
#    include <errno.h>
#  else /* HAS_SETSID */
#    include <sys/ioctl.h>
#    ifdef TIOCNOTTY
#      include <fcntl.h>
#    endif /* TIOCNOTTY */
#  endif /* HAS_SETSID */
#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */

#include "dtmac.h"
#include "dtetlib.h"

/*
**	tet_tiocnotty() - dissociate from control terminal
**		and start a new process group
*/

void tet_tiocnotty()
{

#ifdef _WIN32		/* -START-WIN32-CUT- */

	/*
	** there is no control terminal on WIN32;
	** the best that we can do is to ignore keyboard interrupts
	*/

	(void) signal(SIGINT, SIG_IGN);
	(void) signal(SIGBREAK, SIG_IGN);
	(void) signal(SIGTERM, SIG_IGN);

#else /* _WIN32 */	/* -END-WIN32-CUT- */

#  ifdef HAS_SETSID

	/* easy - use setsid() to start a new session */
	(void) setsid();

#  else /* HAS_SETSID */

	/* harder - must use setpgrp() and possibly TIOCNOTTY */

#    ifdef TIOCNOTTY
	int ttyfd;
#    endif

#    if defined(SVR2) || defined(SVR3) || defined(SVR4) || defined(__hpux) || defined(_AIX)
	(void) setpgrp();
#    else
	int pid = getpid();
	(void) setpgrp(pid, pid);
#    endif


#    ifdef TIOCNOTTY
	/*
	** this for BSD systems where setpgrp() does not change the
	** control terminal
	*/
	if ((ttyfd = open("/dev/tty", O_RDONLY | O_NDELAY)) >= 0) {
		(void) ioctl(ttyfd, TIOCNOTTY, 0);
		(void) close(ttyfd);
	}
#    endif /* TIOCNOTTY */

#  endif /* HAS_SETSID */

#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */

}

