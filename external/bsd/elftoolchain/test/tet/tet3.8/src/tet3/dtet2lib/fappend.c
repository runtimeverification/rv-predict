/*
 *      SCCS:  @(#)fappend.c	1.7 (97/07/21) 
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
static char sccsid[] = "@(#)fappend.c	1.7 (97/07/21) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)fappend.c	1.7 97/07/21 TETware release 3.8
NAME:		fappend.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to set append mode on a file

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include <errno.h>
#include <fcntl.h>
#include "dtmac.h"
#include "error.h"
#include "ltoa.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_fappend() - set append mode on a file
**
**	return 0 if successful or -1 on error
**
**	note that this function is a no-op on WIN32
*/

int tet_fappend(fd)
int fd;
{

#ifndef _WIN32	/* -WIN32-CUT-LINE- */

	register int flags;

	if ((flags = fcntl(fd, F_GETFL, 0)) < 0) {
		error(errno, "can't get file status flags for fd", tet_i2a(fd));
		return(-1);
	}

#  ifdef FAPPEND
	/* BSD style */
	flags |= FAPPEND;
#  else
	/* SYSV style */
	flags |= O_APPEND;
#  endif /* FAPPEND */

	if (fcntl(fd, F_SETFL, flags) < 0) {
		error(errno, "can't set file status flags on fd", tet_i2a(fd));
		return(-1);
	}

#endif		/* -WIN32-CUT-LINE- */

	return(0);
}

