/*
 *      SCCS:  @(#)repcode.c	1.9 (98/08/28) 
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
static char sccsid[] = "@(#)repcode.c	1.9 (98/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)repcode.c	1.9 98/08/28 TETware release 3.8
NAME:		repcode.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to return printable representation of a DTET interprocess
	message reply code

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ltoa.h"
#include "dtetlib.h"

/*
**	tet_ptrepcode() - return printable representation of message reply code
*/

TET_IMPORT char *tet_ptrepcode(rc)
int rc;
{
	static char text[] = "reply-code ";
	static char msg[sizeof text + LNUMSZ];

	switch (rc) {
	case ER_OK:
		return("OK");
	case ER_ERR:
		return("ER_ERR");
	case ER_MAGIC:
		return("ER_MAGIC");
	case ER_LOGON:
		return("ER_LOGON");
	case ER_RCVERR:
		return("ER_RCVERR");
	case ER_REQ:
		return("ER_REQ");
	case ER_TIMEDOUT:
		return("ER_TIMEDOUT");
	case ER_DUPS:
		return("ER_DUPS");
	case ER_SYNCERR:
		return("ER_SYNCERR");
	case ER_INVAL:
		return("ER_INVAL");
	case ER_TRACE:
		return("ER_TRACE");
	case ER_WAIT:
		return("ER_WAIT");
	case ER_XRID:
		return("ER_XRID");
	case ER_SNID:
		return("ER_SNID");
	case ER_SYSID:
		return("ER_SYSID");
	case ER_INPROGRESS:
		return("ER_INPROGRESS");
	case ER_DONE:
		return("ER_DONE");
	case ER_CONTEXT:
		return("ER_CONTEXT");
	case ER_PERM:
		return("ER_PERM");
	case ER_FORK:
		return("ER_FORK");
	case ER_NOENT:
		return("ER_NOENT");
	case ER_PID:
		return("ER_PID");
	case ER_SIGNUM:
		return("ER_SIGNUM");
	case ER_FID:
		return("ER_FID");
	case ER_INTERN:
		return("ER_INTERN");
	case ER_ABORT:
		return("ER_ABORT");
	case ER_2BIG:
		return("ER_2BIG");
	case ER_EPERM:
		return("ER_EPERM");
	case ER_ENOENT:
		return("ER_ENOENT");
	case ER_ESRCH:
		return("ER_ESRCH");
	case ER_EINTR:
		return("ER_EINTR");
	case ER_EIO:
		return("ER_EIO");
	case ER_ENXIO:
		return("ER_ENXIO");
	case ER_E2BIG:
		return("ER_E2BIG");
	case ER_ENOEXEC:
		return("ER_ENOEXEC");
	case ER_EBADF:
		return("ER_EBADF");
	case ER_ECHILD:
		return("ER_ECHILD");
	case ER_EAGAIN:
		return("ER_EAGAIN");
	case ER_ENOMEM:
		return("ER_ENOMEM");
	case ER_EACCES:
		return("ER_EACCES");
	case ER_EFAULT:
		return("ER_EFAULT");
	case ER_ENOTBLK:
		return("ER_ENOTBLK");
	case ER_EBUSY:
		return("ER_EBUSY");
	case ER_EEXIST:
		return("ER_EEXIST");
	case ER_EXDEV:
		return("ER_EXDEV");
	case ER_ENODEV:
		return("ER_ENODEV");
	case ER_ENOTDIR:
		return("ER_ENOTDIR");
	case ER_EISDIR:
		return("ER_EISDIR");
	case ER_EINVAL:
		return("ER_EINVAL");
	case ER_ENFILE:
		return("ER_ENFILE");
	case ER_EMFILE:
		return("ER_EMFILE");
	case ER_ENOTTY:
		return("ER_ENOTTY");
	case ER_ETXTBSY:
		return("ER_ETXTBSY");
	case ER_EFBIG:
		return("ER_EFBIG");
	case ER_ENOSPC:
		return("ER_ENOSPC");
	case ER_ESPIPE:
		return("ER_ESPIPE");
	case ER_EROFS:
		return("ER_EROFS");
	case ER_EMLINK:
		return("ER_EMLINK");
	case ER_EPIPE:
		return("ER_EPIPE");
	case ER_ENOTEMPTY:
		return("ER_ENOTEMPTY");
	default:
		(void) sprintf(msg, "%s%d", text, rc);
		return(msg);
	}
}

