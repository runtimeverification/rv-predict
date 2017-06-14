/*
 *      SCCS:  @(#)connect.c	1.13 (99/09/02) 
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
static char sccsid[] = "@(#)connect.c	1.13 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)connect.c	1.13 99/09/02 TETware release 3.8
NAME:		connect.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	required transport-specific library interface

	function to initiate connection to remote process

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, September 1993
	added tet_ts_disconnect() function

	Denis McConalogue, UniSoft Limited, September 1993
	make sure socket descriptor allocated is not 0, 1, or 2.

	Andrew Dingwall, UniSoft Ltd., December 1993
	moved disconnect stuff to a separate file

	Andrew Dingwall, UniSoft Ltd., February 1995
	clear sockaddr_in before using it

	Andrew Dingwall, UniSoft Ltd., March 1997
	remove #ifndef __hpux from #include <arpa/inet.h>
	since current HP-UX implementations now have this file

	Andrew Dingwall, UniSoft Ltd., July 1998
	Report destination address/port if connect fails.
	Added support for shared API libraries.
 
************************************************************************/

#include <stdio.h>
#include <errno.h>
#include <fcntl.h>
#include <time.h>
#include <sys/types.h>
#ifdef _WIN32		/* -START-WIN32-CUT- */
#  include <winsock.h>
#else			/* -END-WIN32-CUT- */
#  include <unistd.h>
#  include <sys/uio.h>
#  include <sys/socket.h>
#  include <netinet/in.h>
#  include <arpa/inet.h>
#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "tptab_in.h"
#include "error.h"
#include "ltoa.h"
#include "bstring.h"
#include "tslib.h"
#include "server_in.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_ts_connect() - make a connection to a remote process
*/

TET_IMPORT void tet_ts_connect(pp)
register struct ptab *pp;
{
	register struct tptab *tp = (struct tptab *) pp->pt_tdata;
	register int rc;
	register SOCKET sd;
#ifndef _WIN32		/* -WIN32-CUT-LINE- */
	register int nsd;
#endif /* !_WIN32 */	/* -WIN32-CUT-LINE- */
	struct sockaddr_in sin;
	int err;
	static char fmt[] = "connect to %.16s port %d failed";
	char msg[sizeof fmt + 16 + LNUMSZ];

	/* get a socket for the connection */
	if ((sd = socket(AF_INET, SOCK_STREAM, 0)) == INVALID_SOCKET) {
		error(SOCKET_ERRNO, "can't get socket", (char *) 0);
		pp->pt_state = PS_DEAD;
		pp->pt_flags |= PF_ATTENTION;
		return;
	}


#ifdef _WIN32		/* -START-WIN32-CUT- */

	/* ensure that the connection is not passed to a new process */
	if (SOCKET_FIOCLEX(sd) < 0) {
		pp->pt_state = PS_DEAD;
		pp->pt_flags |= PF_ATTENTION;
		return;
	}

#else /* _WIN32 */	/* -END-WIN32-CUT- */

	/* ensure that socket is not on stdin, stdout or stderr */
	if (sd < 3) {
		bzero((char *) &sin, sizeof sin);
		sin.sin_family = AF_INET;
		sin.sin_addr.s_addr = INADDR_ANY;
		sin.sin_port = 0;

		errno = 0;
		if (bind(sd, (struct sockaddr *) &sin, sizeof sin) < 0) {
			error(errno, "can't bind client socket", (char *) 0);
			pp->pt_state = PS_DEAD;
			pp->pt_flags |= PF_ATTENTION;
			return;
		}

        	if ((nsd = fcntl(sd, F_DUPFD, 3)) < 3) {
               		error(errno, "can't dup socket", (char *) 0);
			pp->pt_state = PS_DEAD;
			pp->pt_flags |= PF_ATTENTION;
			return;
		}
        	(void) close(sd);
		sd = nsd;
	}

#endif /* !_WIN32 */	/* -WIN32-CUT-LINE- */


	tp->tp_sd = sd;

	/* call the server-specific connect routine to massage the socket and
		fill in network address */
	if (tet_ss_tsconnect(pp) < 0) {
		pp->pt_state = PS_DEAD;
		pp->pt_flags |= PF_ATTENTION;
		return;
	}

	TRACE5(tet_Tio, 4, "connect to %s on port %s using sd %s %s",
		inet_ntoa(tp->tp_sin.sin_addr),
		tet_i2a(ntohs(tp->tp_sin.sin_port)),
		tet_i2a(tp->tp_sd), tet_r2a(&pp->pt_rid));

	/* attempt the connection */
	do {
		err = 0;
		if ((rc = connect(sd, (struct sockaddr *) &tp->tp_sin, sizeof tp->tp_sin)) == SOCKET_ERROR)
			err = SOCKET_ERRNO;
	} while (rc == SOCKET_ERROR && err == SOCKET_EINTR);

	/* handle errors */
	if (rc == SOCKET_ERROR) {
		switch (err) {
		case SOCKET_EWOULDBLOCK:
		case SOCKET_EINPROGRESS:
			if (pp->pt_flags & PF_NBIO) {
				TRACE1(tet_Tio, 6, "connect in progress");
				pp->pt_flags |= PF_INPROGRESS;
				return;
			}
			/* else fall through */
		default:
			(void) sprintf(msg, fmt,
				inet_ntoa(tp->tp_sin.sin_addr),
				(int) ntohs(tp->tp_sin.sin_port));
			error(err, msg, tet_r2a(&pp->pt_rid));
			pp->pt_state = PS_DEAD;
		}
	}
	else {
		TRACE1(tet_Tio, 6, "connect succeeded");
		pp->pt_flags |= PF_CONNECTED;
	}

	/* here if the connect call completed one way or the other */
	pp->pt_flags |= PF_ATTENTION;
}

