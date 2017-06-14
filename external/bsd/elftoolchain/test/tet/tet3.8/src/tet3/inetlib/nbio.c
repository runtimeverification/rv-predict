/*
 *      SCCS:  @(#)nbio.c	1.9 (05/06/30) 
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
static char sccsid[] = "@(#)nbio.c	1.9 (05/06/30) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)nbio.c	1.9 05/06/30 TETware release 3.8
NAME:		nbio.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to establish non-blocking i/o on an INET socket

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., February 1994
	moved the ioctl() call from dtet2lib/fionbio.c to here
	(this is to make dtet2lib POSIX-clean)

	Andrew Dingwall, UniSoft Ltd., July 1999
	When FIONBIO is not defined, use O_NONBLOCK instead
	for compatibility with UNIX98.

	Geoff Clare, The Open Group, June 2005
	Only include non-portable headers if no standard FTMs are defined.

************************************************************************/

#include <time.h>
#include <sys/types.h>
#ifdef _WIN32		/* -START-WIN32-CUT- */
#  include <winsock.h>
#else			/* -END-WIN32-CUT- */
#  include <netinet/in.h>
#  if !defined(_POSIX_SOURCE) && !defined(_POSIX_C_SOURCE) && \
      !defined(_XOPEN_SOURCE)
#    ifdef SVR4
#      include <sys/filio.h>
#    else
#      include <sys/ioctl.h>
#    endif /* SVR4 */
#  endif /* _POSIX_SOURCE etc. */
#  include <fcntl.h>
#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */
#include <errno.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "tptab_in.h"
#include "inetlib_in.h"
#include "ltoa.h"
#include "error.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_ts_nbio() - establish non-blocking i/o on a socket
**
**	return 0 if successful or -1 on error
*/

int tet_ts_nbio(pp)
register struct ptab *pp;
{
	register SOCKET sd = ((struct tptab *) pp->pt_tdata)->tp_sd;

#ifdef FIONBIO	/* do it the BSD way - also works with Winsock */

	int arg;

	arg = 1;
	if (SOCKET_IOCTL(sd, FIONBIO, &arg) == SOCKET_ERROR) {
		error(SOCKET_ERRNO, "ioctl(FIONBIO) failed on sd",
			tet_i2a(sd));
		return(-1);
	}

#else		/* do it the POSIX way */

	int flags;

	if ((flags = fcntl(sd, F_GETFL, 0)) < 0) {
		error(errno, "can't get file status flags on sd", tet_i2a(sd));
		return(-1);
	}

	flags |= O_NONBLOCK;

	if (fcntl(sd, F_SETFL, flags) < 0) {
		error(errno, "can't set file status flags on sd", tet_i2a(sd));
		return(-1);
	}

#endif

	pp->pt_flags |= PF_NBIO;
	return(0);
}

