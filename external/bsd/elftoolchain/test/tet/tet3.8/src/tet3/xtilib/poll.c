/*
 *      SCCS:  @(#)poll.c	1.11 (05/07/05) 
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
static char sccsid[] = "@(#)poll.c	1.11 (05/07/05) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)poll.c	1.11 05/07/05 TETware release 3.8
NAME:		poll.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Ltd.
DATE CREATED:	May 1993

DESCRIPTION:
	required transport-specific library interface

	poll remote process connections for the possibility of i/o

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., December 1993
	added malloc tracing

	Denis McConalogue, UniSoft Ltd., December 1993
	t_free() not always called after t_alloc() in doaccept()
	timeout not handled correctly when SVID3_POLL undefined

	Andrew Dingwall, UniSoft Ltd., November 1994
	updated t_alloc() structure type names in line with latest XTI spec

	Andrew Dingwall, UniSoft Ltd., February 1995
	report t_errno when t_look() returns an error

	Geoff Clare, UniSoft Ltd., Oct 1996
	Changes for TETware.

	Andrew Dingwall, UniSoft Ltd., March 1998
	When SVID3_POLL is not defined, return from poll loop as soon
	as a connection is accepted without waiting for the remaining
	timeout to expire.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.

	Geoff Clare, The Open Group, July 2005
	Use poll() if SUSv1/2/3 feature test macros are defined.
 
************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/types.h>
#ifndef SVID3_POLL
#  if (defined(_XOPEN_SOURCE) && defined(_XOPEN_SOURCE_EXTENDED)) || \
      (defined(_XOPEN_SOURCE) && _XOPEN_SOURCE+0 >= 500)
#    define SVID3_POLL
#  endif
#endif
#ifdef SVID3_POLL
#  include <stropts.h>
#  include <poll.h>
#else
#  include <time.h>
#endif
#include <xti.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "error.h"
#include "dtetlib.h"
#include "ltoa.h"
#include "xtilib_xt.h"
#include "tptab_xt.h"
#include "server_xt.h"
#include "tslib.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* declarations for static functions */
static int doaccept PROTOLIST((void));
#ifndef SVID3_POLL
   static int ts_poll2 PROTOLIST((struct ptab *));
#endif

extern int tet_listen_fd;

/*
**	tet_ts_poll() - poll for connection attempts, and pending i/o to/from
**		connected processes in the ptab list at *pp
**
**	return	1 if there is at least one i/o request to service
**		0 if no requests arrive before the timeout expires
**		-1 on error
*/

int tet_ts_poll(pp, timeout)
struct ptab *pp;
int timeout;
{

#ifndef SVID3_POLL
	int     rc = 0;
	time_t	start = time((time_t *) 0);
	
	TRACE2(tet_Tio, 4, "tet_ts_poll: timeout = %s", tet_i2a(timeout));

	while ((rc = ts_poll2(pp)) == 0) {
		if (timeout >= 0 && time((time_t *) 0) >= start + timeout)
			break;
		(void) sleep((unsigned) 1);
	}
	return (rc);
#else
	register struct ptab *q;
	register struct tptab *tp;
	int i, rc, nfound, rfds=0, wfds=0, nfds=0, wait;
	size_t pdtsize;

	static struct pollfd *pollfds = (struct pollfd *)0;
	static int maxfds = -1;

	TRACE2(tet_Tio, 4, "tet_ts_poll: timeout = %s", tet_i2a(timeout));

	if (!pollfds) {
		maxfds  = tet_getdtablesize();
		pdtsize = maxfds * sizeof (struct pollfd);
		errno = 0;
		if ((pollfds = (struct pollfd *) malloc(pdtsize)) == (struct pollfd *) 0) {
			error(errno, "can't malloc pollfd array",
				tet_i2a(pdtsize));
			return (-1);
		}
		TRACE2(tet_Tbuf, 6, "allocate pollfd array = %s",
			tet_i2x(pollfds));
	}

	/* accept a new connection if one is pending on the listen fd */

	if (tet_listen_fd >= 0) {
		TRACE2(tet_Tio, 6, "poll listen fd %s for reading",
			tet_i2a(tet_listen_fd));
		pollfds[nfds].fd = tet_listen_fd;
		pollfds[nfds].events = POLLIN;
		rfds++;
		nfds++;
	}
	
	/* set rest of pollfd structure according to process state */

	for (q = pp; q && (nfds < maxfds); q = q->pt_next) {
		ASSERT(q->pt_magic == PT_MAGIC);
		tp = (struct tptab *) q->pt_tdata;
		if (tp->tp_fd < 0)
			continue;

		switch (q->pt_state) {
		case PS_IDLE:
			q->pt_state = PS_RCVMSG;
			/* fall through */
		case PS_RCVMSG:
			TRACE2(tet_Tio, 6, "poll fd %s for reading",
				tet_i2a(tp->tp_fd));
			pollfds[nfds].fd = tp->tp_fd;
			pollfds[nfds].events = POLLIN;
			rfds++;
			nfds++;
			break;
		case PS_CONNECT:
		case PS_SNDMSG:
			TRACE2(tet_Tio, 6, "poll fd %s for writing",
				tet_i2a(tp->tp_fd));
			pollfds[nfds].fd = tp->tp_fd;
			pollfds[nfds].events = POLLOUT;
			wfds++;
			nfds++;
			break;
		}
	}

	/* set select timeout */
	if (timeout >= 0)
		wait = timeout * 1000;
	else
#ifdef INFTIM
		wait = INFTIM;
#else
		wait = -1;
#endif

	/* do the poll operation */

	do {
		TRACE3(tet_Tio, 8, "poll: rfds = %s, wfds = %s",
			tet_i2a(rfds), tet_i2a(wfds));
		errno = 0;
		nfound = poll(pollfds, (unsigned long) nfds, wait);
	} while (nfound < 0 && errno == EINTR);

	if (nfound >= 0)
		TRACE2(tet_Tio, 6, "poll returns %s", tet_i2a(nfound));

	if (nfound < 0) {
		error(errno, "poll() failed", (char *) 0);
		return(-1);
	}
	else if (nfound == 0)
		return(0);
	
	/* accept a connection if one is found pending on the listen 
	   file descriptor */
	if (tet_listen_fd >= 0 && (pollfds[0].revents & POLLIN)) {
		TRACE1(tet_Tio, 6, "i/o possible on listen fd");
		if ((rc = doaccept()) < 0)
			return(-1);
		nfound--;
	}

	/* arrange to service processes where i/o is possible */
	for (q = pp; q && nfound >= 0; q = q->pt_next) {
		ASSERT(q->pt_magic == PT_MAGIC);
		tp = (struct tptab *) q->pt_tdata;

		for (i = 0; i < nfds; i++)
			if (pollfds[i].fd == tp->tp_fd)
				break;

		if (i >= nfds)
			continue;

		if (pollfds[i].revents & POLLOUT) {	
			TRACE2(tet_Tio, 6, "i/o possible on write fd %s",
				tet_i2a(tp->tp_fd));
			nfound--;
			if (q->pt_state == PS_CONNECT) {
				q->pt_state = PS_SNDMSG;
				q->pt_flags = (q->pt_flags & ~PF_INPROGRESS) | PF_CONNECTED;
			}
			q->pt_flags |= PF_ATTENTION;
		}
		if (pollfds[i].revents & POLLIN) {	
			TRACE2(tet_Tio, 6, "i/o possible on read fd %s",
				tet_i2a(tp->tp_fd));
			nfound--;
			q->pt_flags |= PF_ATTENTION;
		}
	}

	return(1);

#endif /* SVID3_POLL */

}

/*
** 	doaccept() - accept a new connection if one is pending on the 
**		listen file descriptor.
**
**	return	 1 if a connection was accepted
**		 0 if no connection was pending
**		-1 on error
*/

static int doaccept()
{
	int i, rc, c_event, err, found = 0;
	struct t_discon *discon;

	err = 0;
	switch (c_event = t_look(tet_listen_fd)) {
	case T_LISTEN:
		TRACE1(tet_Tio, 6, "connect received on listen fd");

		for (i=0; i < MAX_CONN_IND; i++)
			if (tet_calls[i] == (struct t_call *)0)
				break;

		/* make sure we haven't overflowed the connect indication
		   array. This shouldn't happen since the listen fd can't
		   queue more than MAX_CONN_IND indications */

		if (i >= MAX_CONN_IND) {
			error(0, "exceeded connection limit", (char *) 0);
			return (-1);
		}
		
		if ((tet_calls[i] = T_ALLOC_CALL(tet_listen_fd)) == (struct t_call *) 0) {
			xt_error(t_errno, "can't alloc T_CALL", (char *) 0);
			return (-1);
		}
		TRACE3(tet_Tbuf, 6, "allocate tet_calls[%s] = %s",
			tet_i2a(i), tet_i2x(tet_calls[i]));

		if (t_listen(tet_listen_fd, tet_calls[i]) < 0) {
			if (t_errno != TLOOK) {
				xt_error(t_errno, "unexpected event",
					tet_xtev2a(c_event));
				rc = -1;
			}
			else {
				/* process event on next poll */
				rc = 0;
			}
			TRACE3(tet_Tbuf, 6, "t_free tet_calls[%s] = %s",
				tet_i2a(i), tet_i2x(tet_calls[i]));
			(void) t_free((char *) tet_calls[i], T_CALL);
			tet_calls[i] = (struct t_call *) 0;
			return (rc);
		}
		tet_ss_tsaccept();
		found = 1;
	
		break;

	case T_DISCONNECT:
		TRACE1(tet_Tio, 6, "disconnect received on listen fd");

		if ((discon = T_ALLOC_DIS(tet_listen_fd)) == (struct t_discon *) 0) {
			xt_error(t_errno, "t_alloc() failed", (char *) 0);
			return (-1);
		}
		TRACE2(tet_Tbuf, 6, "allocate discon = %s", tet_i2x(discon));
		if (t_rcvdis(tet_listen_fd, discon) < 0) {
			xt_error(t_errno, "t_rcvdis() failed", (char *) 0);
			TRACE2(tet_Tbuf, 6, "free discon = %s", tet_i2x(discon));
			(void) t_free((char *) discon, T_DIS);
			return(-1);
		}

		for (i = 0; i < MAX_CONN_IND; i++) {
			if (
				tet_calls[i] &&
				tet_calls[i]->sequence == discon->sequence
			) {
				TRACE3(tet_Tbuf, 6, "t_free tet_calls[%s] = %s",
					tet_i2a(i), tet_i2x(tet_calls[i]));
				(void) t_free((char *) tet_calls[i], T_CALL);
				TRACE2(tet_Tbuf, 6, "free discon = %s",
					tet_i2x(discon));
				(void) t_free((char *) discon, T_DIS);
				tet_calls[i] = (struct t_call *) 0;
			}
		}
		break;

	case 0:
		/* no event on this endpoint */
		break;

	case -1:
		err = t_errno;
		/* fall through */

	default:
		xt_error(err, "Unexpected event on tet_listen_fd:",
			tet_i2a(c_event));
		return (-1);
	}

	return (found);
}


/*
**	ts_poll2() - poll processing when no asynchronous event mechanism is
**		available.
**
**	return   1 if i/o is possible on at least one fd
**		 0 if no i/o is possible
**		-1 on error
*/

#ifndef SVID3_POLL

static int ts_poll2(pp)
struct ptab *pp;
{
	register struct ptab *q;
	register struct tptab *tp;
	int i, nfds, err, nfound = 0, rc;
	int c_event;

	/* accept a new connection if one is pending on the listen fd */
	if (tet_listen_fd >= 0) {
		TRACE2(tet_Tio, 6, "poll listen fd %s for reading",
			tet_i2a(tet_listen_fd));
		if ((rc = doaccept()) < 0)
			return (-1);
		else if (rc > 0)
			nfound++;
	}

	/* scan the process table at pp examining each fd */
	for (q = pp; q; q = q->pt_next) {
		ASSERT(q->pt_magic == PT_MAGIC);
		tp = (struct tptab *) q->pt_tdata;
		if (tp->tp_fd < 0)
			continue;
	
		err = 0;
		switch (c_event = t_look(tp->tp_fd)) {
		case T_DATA:
		case T_GODATA:
			break;

		case T_DISCONNECT:
		case T_ORDREL:
			if (q->pt_flags & PF_LOGGEDON) 
				error(0, "unexpected EOF",
					tet_r2a(&pp->pt_rid));
			q->pt_state = PS_DEAD;
			break;

		case 0:
			break;

		case -1:
			err = t_errno;
			/* fall through */

		default:
			xt_error(err, "unexpected event", tet_xtev2a(c_event));
			q->pt_state = PS_DEAD;
			break;
		}

		switch (q->pt_state) {
		case PS_IDLE:
			q->pt_state = PS_RCVMSG;
			/* fall through */
		case PS_RCVMSG:
			TRACE2(tet_Tio, 6, "poll fd %s for reading",
				tet_i2a(tp->tp_fd));
			if (c_event != 0) {
				TRACE2(tet_Tio, 6, "i/o possible on read fd %s",
					tet_i2a(tp->tp_fd));
				q->pt_flags |= PF_ATTENTION;
				nfound++;
			}
			break;

		case PS_CONNECT:
		case PS_SNDMSG:
			TRACE2(tet_Tio, 6, "poll fd %s for writing",
				tet_i2a(tp->tp_fd));
			if ((c_event == T_GODATA) || (c_event == 0)) {
				TRACE2(tet_Tio, 6,
					"i/o possible on write fd %s",
					tet_i2a(tp->tp_fd));
				if (q->pt_state == PS_CONNECT) {
					q->pt_state = PS_SNDMSG;
					q->pt_flags = (q->pt_flags & ~PF_INPROGRESS) | PF_CONNECTED;
				}	
				q->pt_flags |= PF_ATTENTION;
				nfound++;

			}
			break;

		case PS_DEAD:
			q->pt_flags |= PF_ATTENTION;
			nfound++;
			break;
			
		}
	}
	return(nfound > 0 ? 1 : 0);
}

#endif /* !SVID3_POLL */

