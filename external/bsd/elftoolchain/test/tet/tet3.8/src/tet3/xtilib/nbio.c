/*
 *      SCCS:  @(#)nbio.c	1.7 (99/09/03) 
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
static char sccsid[] = "@(#)nbio.c	1.7 (99/09/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)nbio.c	1.7 99/09/03 TETware release 3.8
NAME:		nbio.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Ltd.
DATE CREATED:	May 1993

DESCRIPTION:
	function to establish non-blocking i/o on an XTI file descriptor
	endpoint.

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., February 1994
	moved the ioctl() call from dtet2lib/fionbio.c to here
	(this is to make dtet2lib POSIX-clean)

	Andrew Dingwall, UniSoft Ltd., February 1995
	changed to use O_NONBLOCK since XTI may not support FIONBIO


************************************************************************/

#include <fcntl.h>
#include <sys/types.h>
#include <time.h>
#include <xti.h>
#include <errno.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "tptab_xt.h"
#include "xtilib_xt.h"
#include "error.h"
#include "ltoa.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_ts_nbio() - establish non-blocking i/o on an XTI fd
**
**	return 0 if successful or -1 on error
*/

int tet_ts_nbio(pp)
register struct ptab *pp;
{
	register int fd = ((struct tptab *) pp->pt_tdata)->tp_fd;
	int flags;

	if ((flags = fcntl(fd, F_GETFL, 0)) < 0) {
		error(errno, "can't get file status flags on fd", tet_i2a(fd));
		return(-1);
	}

	flags |= O_NONBLOCK;

	if (fcntl(fd, F_SETFL, flags) < 0) {
		error(errno, "can't set file status flags on fd", tet_i2a(fd));
		return(-1);
	}

	pp->pt_flags |= PF_NBIO;
	return(0);
}

