/*
 *      SCCS:  @(#)reqcode.c	1.11 (03/03/26) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1992 X/Open Company Limited
 * (C) Copyright 1994 UniSoft Limited
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
static char sccsid[] = "@(#)reqcode.c	1.11 (03/03/26) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)reqcode.c	1.11 03/03/26 TETware release 3.8
NAME:		reqcode.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to return printable representation of a DTET interprocess
	message request code

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, September 1993
	added support for OP_XRCLOSE, OP_RCOPY and OP_CODESF
	request messages.

	Andrew Dngwall, UniSoft Ltd., November 1993
	added support for FIFO transport interface

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.

	Andrew Dingwall, The Open Group, March 2003
	Added support for OP_FWRITE, OP_UTIME, OP_TSFTYPE and OP_FTIME.

 
************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ltoa.h"
#include "dtetlib.h"

/*
**	tet_ptreqcode() - return printable representation of message request
**		code
*/

TET_IMPORT char *tet_ptreqcode(request)
int request;
{
	static char text[] = "request-code ";
	static char msg[sizeof text + LNUMSZ];

	switch (request) {
	case OP_LOGON:
		return("LOGON");
	case OP_LOGOFF:
		return("LOGOFF");
	case OP_NULL:
		return("NULL");
	case OP_SNGET:
		return("SNGET");
	case OP_SNSYS:
		return("SNSYS");
	case OP_ASYNC:
		return("ASYNC");
	case OP_USYNC:
		return("USYNC");
	case OP_SYSID:
		return("SYSID");
	case OP_SYSNAME:
		return("SYSNAME");
	case OP_TSINFO:
		return("TSINFO");
	case OP_TRACE:
		return("TRACE");
	case OP_EXEC:
		return("EXEC");
	case OP_WAIT:
		return("WAIT");
	case OP_KILL:
		return("KILL");
	case OP_XROPEN:
		return("XROPEN");
	case OP_XRCLOSE:
		return("XRCLOSE");
	case OP_XRSYS:
		return("XRSYS");
	case OP_ICSTART:
		return("ICSTART");
	case OP_TPSTART:
		return("TPSTART");
	case OP_ICEND:
		return("ICEND");
	case OP_TPEND:
		return("TPEND");
	case OP_XRES:
		return("XRES");
	case OP_RESULT:
		return("RESULT");
	case OP_CFNAME:
		return("CFNAME");
	case OP_RCFNAME:
		return("RCFNAME");
	case OP_SNDCONF:
		return("SNDCONF");
	case OP_RCVCONF:
		return("RCVCONF");
	case OP_CONFIG:
		return("CONFIG");
	case OP_TFOPEN:
		return("TFOPEN");
	case OP_TFCLOSE:
		return("TFCLOSE");
	case OP_TFWRITE:
		return("TFWRITE");
	case OP_PUTENV:
		return("PUTENV");
	case OP_ACCESS:
		return("ACCESS");
	case OP_MKDIR:
		return("MKDIR");
	case OP_RMDIR:
		return("RMDIR");
	case OP_CHDIR:
		return("CHDIR");
	case OP_FOPEN:
		return("FOPEN");
	case OP_FCLOSE:
		return("FCLOSE");
	case OP_GETS:
		return("GETS");
	case OP_PUTS:
		return("PUTS");
	case OP_LOCKFILE:
		return("LOCKFILE");
	case OP_SHARELOCK:
		return("SHARELOCK");
	case OP_MKTMPDIR:
		return("MKTMPDIR");
	case OP_UNLINK:
		return("UNLINK");
	case OP_RXFILE:
		return("RXFILE");
	case OP_MKSDIR:
		return("MKSDIR");
	case OP_TSFILES:
		return("TSFILES");
	case OP_CODESF:
		return("CODESF");
	case OP_RCOPY:
		return("RCOPY");
	case OP_CONNECT:
		return("CONNECT");
	case OP_ATTENTION:
		return("ATTENTION");
	case OP_SETCONF:
		return("SETCONF");
	case OP_MKALLDIRS:
		return("MKALLDIRS");
	case OP_TIME:
		return("TIME");
	case OP_RMALLDIRS:
		return("RMALLDIRS");
	case OP_SNRM:
		return("SNRM");
	case OP_XRSEND:
		return("XRSEND");
	case OP_FWRITE:
		return("FWRITE");
	case OP_UTIME:
		return("UTIME");
	case OP_TSFTYPE:
		return("TSFTYPE");
	case OP_FTIME:
		return("FTIME");
#if TESTING
	case OP_PRINT:
		return("PRINT");
#endif
	default:
		(void) sprintf(msg, "%s%d", text, request);
		return(msg);
	}
}

