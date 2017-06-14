/*
 *      SCCS:  @(#)poll.c	1.11 (02/01/18) 
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
static char sccsid[] = "@(#)poll.c	1.11 (02/01/18) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)poll.c	1.11 02/01/18 TETware release 3.8
NAME:		poll.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	required transport-specific library interface

	poll remote process connections for the posibility of i/o

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., November 1992
	AIX-specific modifications.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Now that AIX is UNIX98-compliant we don't need the AIX-specific
	modifications any more.
	Provision of FD_SET macro and friends now only if _XOPEN_SOURCE
	is not defined.
 
************************************************************************/

#include <errno.h>
#include <time.h>
#include <sys/types.h>
#ifdef _WIN32		/* -START-WIN32-CUT- */
#  include <winsock.h>
#else			/* -END-WIN32-CUT- */
#  if defined(_AIX) && !defined(_XOPEN_SOURCE)
#    include <sys/select.h>
#  endif /* _AIX */
#  include <sys/time.h>
#  include <netinet/in.h>
#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "error.h"
#include "tptab_in.h"
#include "server_in.h"
#include "tslib.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/*
** compatability for systems with pre-BSD4.3 select() call -
**	only one long in fd_set
**
** in UniPlus V.2, fd_set is already typedefed in <sys/types.h>
** on systems where this is not done, you will need something like
**
**	typedef struct { long fd_bits[1]; } fd_set;
**
** here as well
*/

#ifndef _XOPEN_SOURCE
#  ifndef _WIN32		/* -WIN32-CUT-LINE- */
#    ifndef FD_SET

#      define	FD_SET(n, p)	((p)->fds_bits[0] |= (1L << (n)))
#      define	FD_CLR(n, p)	((p)->fds_bits[0] &= ~(1L << (n)))
#      define	FD_ISSET(n, p)	((p)->fds_bits[0] & (1L << (n)))
#      define	FD_ZERO(p)	((p)->fds_bits[0] = 0L)

#    endif /* FD_SET */
#  endif 			/* -WIN32-CUT-LINE- */
#endif /* !_XOPEN_SOURCE */

/*
**	tet_ts_poll() - poll for connection attempts, and pending i/o
**		to/from connected processes in the ptab list at *pp
**
**	return	1 if there is at least one i/o request to service
**		0 if no requests arrive before the timeout expires
**		-1 on error
*/

int tet_ts_poll(pp, timeout)
struct ptab *pp;
int timeout;
{
	register struct ptab *q;
	register struct tptab *tp;
	fd_set rfds, wfds, rd, wd;
	int nfds, nfound, err;
	struct timeval tv;
	register struct timeval *tvp;
	extern SOCKET tet_listen_sd;

	TRACE2(tet_Tio, 4, "tet_ts_poll: timeout = %s", tet_i2a(timeout));

	/* clear descriptor masks for select -
		arrange to receive new connection notification */
	FD_ZERO(&rfds);
	FD_ZERO(&wfds);
	if (tet_listen_sd != INVALID_SOCKET) {
		TRACE2(tet_Tio, 6, "poll listen sd %s for reading",
			tet_i2a(tet_listen_sd));
		FD_SET(tet_listen_sd, &rfds);
#ifdef _WIN32	/* -START-WIN32-CUT- */
		/* in winsock, the nfds parameter to select() is ignored */
		nfds = 0;
#else		/* -END-WIN32-CUT- */
		nfds = tet_listen_sd + 1;
#endif		/* -WIN32-CUT-LINE- */
	}
	else
		nfds = 0;

	/* set fd_set bits according to process state */
	for (q = pp; q; q = q->pt_next) {
		ASSERT(q->pt_magic == PT_MAGIC);
		tp = (struct tptab *) q->pt_tdata;
		if (tp->tp_sd == INVALID_SOCKET)
			continue;
		switch (q->pt_state) {
		case PS_IDLE:
			q->pt_state = PS_RCVMSG;
			/* fall through */
		case PS_RCVMSG:
			TRACE2(tet_Tio, 6, "poll sd %s for reading",
				tet_i2a(tp->tp_sd));
			FD_SET(tp->tp_sd, &rfds);
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
			if (tp->tp_sd >= nfds)
				nfds = tp->tp_sd + 1;
#endif		/* -WIN32-CUT-LINE- */
			break;
		case PS_CONNECT:
		case PS_SNDMSG:
			TRACE2(tet_Tio, 6, "poll sd %s for writing",
				tet_i2a(tp->tp_sd));
			FD_SET(tp->tp_sd, &wfds);
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
			if (tp->tp_sd >= nfds)
				nfds = tp->tp_sd + 1;
#endif		/* -WIN32-CUT-LINE- */
			break;
		}
	}

	/* set select timeout */
	if (timeout >= 0) {
		tv.tv_sec = timeout;
		tv.tv_usec = 0;
		tvp = &tv;
	}
	else
		tvp = (struct timeval *) 0;

	/* do the select operation -
		we use temporary copies of rfds and wfds because they
		get trashed if select returns with EINTR */
	do {
#if 0
#  ifndef _WIN32				/* -WIN32-CUT-LINE- */
		TRACE3(tet_Tio, 8, "select: rfds = %s, wfds = %s",
			tet_l2x(rfds.fds_bits[0]), tet_l2x(wfds.fds_bits[0]));
#  endif					/* -WIN32-CUT-LINE- */
#endif
		rd = rfds;
		wd = wfds;
		err = 0;
		if ((nfound = select(nfds, &rd, &wd, (fd_set *) 0, tvp)) == SOCKET_ERROR)
			err = SOCKET_ERRNO;
	} while (nfound == SOCKET_ERROR && err == SOCKET_EINTR);
	rfds = rd;
	wfds = wd;

	if (nfound >= 0) {
		TRACE2(tet_Tio, 6, "select returns %s", tet_i2a(nfound));
	}

	if (nfound == SOCKET_ERROR) {
		error(err, "select() failed", (char *) 0);
		return(-1);
	}
	else if (nfound == 0)
		return(0);

	/* accept a new connection if one is pending on the listen socket */
	if (tet_listen_sd != INVALID_SOCKET && FD_ISSET(tet_listen_sd, &rfds)) {
		TRACE1(tet_Tio, 6, "i/o possible on listen sd");
		FD_CLR(tet_listen_sd, &rfds);
		tet_ss_tsaccept();
		nfound--;
	}

	/* arrange to service processes where i/o is possible */
	for (q = pp; q && nfound >= 0; q = q->pt_next) {
		ASSERT(q->pt_magic == PT_MAGIC);
		tp = (struct tptab *) q->pt_tdata;
		if (FD_ISSET(tp->tp_sd, &wfds)) {
			TRACE2(tet_Tio, 6, "i/o possible on write sd %s",
				tet_i2a(tp->tp_sd));
			nfound--;
			if (q->pt_state == PS_CONNECT) {
				q->pt_state = PS_SNDMSG;
				q->pt_flags = (q->pt_flags & ~PF_INPROGRESS) | PF_CONNECTED;
			}
			q->pt_flags |= PF_ATTENTION;
		}
		if (FD_ISSET(tp->tp_sd, &rfds)) {
			TRACE2(tet_Tio, 6, "i/o possible on read sd %s",
				tet_i2a(tp->tp_sd));
			nfound--;
			q->pt_flags |= PF_ATTENTION;
		}
	}

	return(1);
}

