/*
 *	SCCS: @(#)wsaerr.c	1.4 (97/07/21)
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
static char sccsid[] = "@(#)wsaerr.c	1.4 (97/07/21) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)wsaerr.c	1.4 97/07/21 TETware release 3.8
NAME:		wsaerr.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	September 1996

DESCRIPTION:
	function to return a printable representation of a
	Winsock error code

	note that although this function is only relevant to the INET
	network transport on WIN32 platforms, it is included in
	dtet2lib (rather than inetlib) because it is referenced by
	error handler code that is itself not specific to any particular
	network transport

MODIFICATIONS:

************************************************************************/

#if defined(_WIN32) && !defined(TET_LITE) /* -START-LITE-CUT- -START-WIN32-CUT- */

#include <stdio.h>
#include <winsock.h>
#include "dtmac.h"
#include "ltoa.h"
#include "dtetlib.h"

/*
**	tet_wsaerrmsg() - return a printable representation of a
**		Winsock error code
*/

char *tet_wsaerrmsg(wsa_errnum)
int wsa_errnum;
{
	static char fmt[] = "Error %d";
	static char msg[sizeof fmt + LNUMSZ];

	switch (wsa_errnum) {
	case WSAEACCES:
		return("Permission denied");
	case WSAEADDRINUSE:
		return("Address already in use");
	case WSAEADDRNOTAVAIL:
		return("Can't assign requested address");
	case WSAEAFNOSUPPORT:
		return("Address family not supported by protocol family");
	case WSAEALREADY:
		return("Operation already in progress");
	case WSAEBADF:
		return("Bad file number");
	case WSAECONNABORTED:
		return("Software caused connection abort");
	case WSAECONNREFUSED:
		return("Connection refused");
	case WSAECONNRESET:
		return("Connection reset by peer");
	case WSAEDESTADDRREQ:
		return("Destination address required");
	case WSAEDISCON:
		return("Connection has been disconnected gracefully");
	case WSAEDQUOT:
		return("Disk quota exceeded");
	case WSAEFAULT:
		return("Bad address");
	case WSAEHOSTDOWN:
		return("Host is down");
	case WSAEHOSTUNREACH:
		return("Host is unreachable");
	case WSAEINPROGRESS:
		return("Operation now in progress");
	case WSAEINTR:
		return("Interrupted system call");
	case WSAEINVAL:
		return("Invalid argument");
	case WSAEISCONN:
		return("Socket is already connected");
	case WSAELOOP:
		return("Too many levels of symbolic links");
	case WSAEMFILE:
		return("Too many open files");
	case WSAEMSGSIZE:
		return("Message too long");
	case WSAENAMETOOLONG:
		return("File name too long");
	case WSAENETDOWN:
		return("Network is down");
	case WSAENETRESET:
		return("Network dropped connection on reset");
	case WSAENETUNREACH:
		return("Network is unreachable");
	case WSAENOBUFS:
		return("No buffer space available");
	case WSAENOPROTOOPT:
		return("Option not supported by protocol");
	case WSAENOTCONN:
		return("Socket is not connected");
	case WSAENOTEMPTY:
		return("Directory not empty");
	case WSAENOTSOCK:
		return("Socket operation on non-socket");
	case WSAEOPNOTSUPP:
		return("Operation not supported on socket");
	case WSAEPFNOSUPPORT:
		return("Protocol family not supported");
	case WSAEPROCLIM:	/* not documented anywhere ?? */
		return("WSAEPROCLIM");
	case WSAEPROTONOSUPPORT:
		return("Protocol not supported");
	case WSAEPROTOTYPE:
		return("Protocol wrong type for socket");
	case WSAEREMOTE:
		return("Too many levels of remote path");
	case WSAESHUTDOWN:
		return("Can't send after socket shutdown");
	case WSAESOCKTNOSUPPORT:
		return("Socket type not supported");
	case WSAESTALE:
		return("Stale NFS file handle");
	case WSAETIMEDOUT:
		return("Connection timed out");
	case WSAETOOMANYREFS:
		return("Too many references: can't splice");
	case WSAEUSERS:
		return("Too many users");
	case WSAEWOULDBLOCK:
		return("Operation would block");
	case WSAHOST_NOT_FOUND:
		return("Host not found");
	case WSANOTINITIALISED:
		return("Winsock not initialised");
	case WSANO_DATA:
		return("Valid name but no data");
	case WSANO_RECOVERY:
		return("Unrecoverable error");
	case WSASYSNOTREADY:
		return("Network subsystem is not usable");
	case WSATRY_AGAIN:
		return("Try again");
	case WSAVERNOTSUPPORTED:
		return("Winsock version not supported");
	default:
		(void) sprintf(msg, fmt, wsa_errnum);
		return(msg);
	}
}

#else					/* -END-LITE-CUT- -END-WIN32-CUT- */

int tet_wsaerr_c_not_used;

#endif /* _WIN32 && !TET_LITE */	/* -LITE-CUT-LINE- -WIN32-CUT-LINE- */

