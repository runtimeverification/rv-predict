/*
 *	SCCS: @(#)errlist.c	1.1 (99/09/02)
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
static char sccsid[] = "@(#)errlist.c	1.1 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)errlist.c	1.1 99/09/02 TETware release 3.8
NAME:		errlist.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall
DATE CREATED:	July 1999
SYNOPSIS:

	char *	tet_errlist[];
	int	tet_nerr;

DESCRIPTION:

	Tet_errlist contains a short string describing each tet_errno
	value, and can be indexed by tet_errno after checking the
	condition: (tet_errno >= 0 && tet_errno < tet_nerr).

MODIFICATIONS:

 
************************************************************************/

#include "dtmac.h"
#include "tet_api.h"

/* This list must be kept in sync with tet_api.h and dtmsg.h */
TET_IMPORT char *tet_errlist[] = {
/* TET_ER_OK		 0 */ "no error",
/* TET_ER_ERR		 1 */ "general error",
/* TET_ER_MAGIC		 2 */ "bad magic number",
/* TET_ER_LOGON		 3 */ "not logged on",
/* TET_ER_RCVERR	 4 */ "receive message error",
/* TET_ER_REQ		 5 */ "unknown request code",
/* TET_ER_TIMEDOUT	 6 */ "request/call timed out",
/* TET_ER_DUPS		 7 */ "request contained duplicate IDs",
/* TET_ER_SYNCERR	 8 */ "sync completed unsuccessfully",
/* TET_ER_INVAL		 9 */ "invalid parameter",
/* TET_ER_TRACE		10 */ "tracing not configured",
/* TET_ER_WAIT		11 */ "process not terminated",
/* TET_ER_XRID		12 */ "bad xrid in xresd request",
/* TET_ER_SNID		13 */ "bad snid in syncd request",
/* TET_ER_SYSID		14 */ "bad sysid or sysid not in system name list",
/* TET_ER_INPROGRESS	15 */ "event in progress",
/* TET_ER_DONE		16 */ "event finished or already happened",
/* TET_ER_CONTEXT	17 */ "request out of context",
/* TET_ER_PERM		18 */ "privilege request/kill error",
/* TET_ER_FORK		19 */ "can't fork",
/* TET_ER_NOENT		20 */ "no such file or directory",
/* TET_ER_PID		21 */ "no such process",
/* TET_ER_SIGNUM	22 */ "bad signal number",
/* TET_ER_FID		23 */ "bad file id",
/* TET_ER_INTERN	24 */ "server internal error",
/* TET_ER_ABORT		25 */ "abort TCM on TP end",
/* TET_ER_2BIG		26 */ "argument list too long",
};
 
TET_IMPORT int tet_nerr = sizeof(tet_errlist)/sizeof(*tet_errlist);

