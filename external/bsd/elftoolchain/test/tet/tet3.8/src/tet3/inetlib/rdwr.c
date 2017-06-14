/*
 *      SCCS:  @(#)rdwr.c	1.17 (02/01/18) 
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
static char sccsid[] = "@(#)rdwr.c	1.17 (02/01/18) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)rdwr.c	1.17 02/01/18 TETware release 3.8
NAME:		rdwr.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	required transport-specific library interfaces

	functions to perform i/o on remote process connections

MODIFICATIONS:
	Geoff Clare, UniSoft Ltd., August 1996
	Changes for TETware.

	Geoff Clare, UniSoft Ltd., Sept 1996
	Ignore SIGPIPE instead of catching it.

	Andrew Dingwall, UniSoft Ltd., February 1998
	Use TETware-specific macros to access threads functions and
	data items.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.

	Geoff Clare, UniSoft Ltd., May 2001
	Use sigaction() instead of signal().
 
************************************************************************/

#include <stdio.h>
#include <sys/types.h>
#include <errno.h>
#include <time.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <winsock.h>
#else		/* -END-WIN32-CUT- */
#  include <signal.h>
#  include <unistd.h>
#  include <sys/socket.h>
#  include <netinet/in.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtmsg.h"
#include "dtthr.h"
#include "ptab.h"
#include "tptab_in.h"
#include "bstring.h"
#include "error.h"
#include "ltoa.h"
#include "dtetlib.h"
#include "tslib.h"
#include "server_bs.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

#ifndef NOTRACE
static char msghdr[] = "message header";
static char msgdata[] = "message data";
#endif

/* declarations for static functions */
static int doread PROTOLIST((struct ptab *));
static int bs2md PROTOLIST((char *, struct ptab *));
static int md2bs PROTOLIST((struct ptab *));


/*
**	tet_ts_rcvmsg() - recieve an inet message on a socket with
**		at least some data to read
**
**	set one of PF_INPROGRESS, PF_IODONE or PF_IOERR on return
**	change connected process state to PS_DEAD on EOF
**
**	return an ER_ code to show the state of the local receive processing
*/

int tet_ts_rcvmsg(pp)
register struct ptab *pp;
{
	register struct tptab *tp = (struct tptab *) pp->pt_tdata;
	register int rc;

	/* set up variables if first attempt to read this message */
	if ((pp->pt_flags & PF_INPROGRESS) == 0) {
		tp->tp_cnt = DM_HDRSZ;
		tp->tp_ptr = tp->tp_buf;
		pp->pt_flags |= (PF_INPROGRESS | PF_RCVHDR);
		TRACE2(tet_Tio, 4, "rcvmsg: read %s bytes header",
			tet_i2a(tp->tp_cnt));
	}

	/* process the next bit of the message */
	switch (pp->pt_flags & PF_RCVHDR) {
	case PF_RCVHDR:
		/* read the message header */
		if ((rc = doread(pp)) <= 0) {
			if (rc < 0)
				pp->pt_flags &= ~PF_RCVHDR;
			return(ER_OK);
		}
		TDUMP(tet_Tio, 10, tp->tp_buf, DM_HDRSZ, msghdr);
		pp->pt_flags &= ~PF_RCVHDR;
		(void) tet_bs2dtmhdr(tp->tp_buf, &pp->ptm_hdr, DM_HDRSZ);
		if (pp->ptm_magic != DTM_MAGIC && pp->ptm_len) {
			/* we are probably out of sync with sender */
			error(0, "received bad message header,",
				"ignored message data");
			pp->ptm_len = 0;
		}
		if (pp->ptm_len <= 0) {
			/* no data portion */
			pp->pt_flags |= PF_IODONE;
			return(ER_OK);
		}
		if (BUFCHK(&tp->tp_buf, &tp->tp_len, pp->ptm_len) < 0) {
			/* can't grow data buffer */
			pp->pt_flags |= PF_IODONE;
			return(ER_ERR);
		}
		pp->pt_flags |= PF_INPROGRESS;
		tp->tp_ptr = tp->tp_buf;
		tp->tp_cnt = pp->ptm_len;
		TRACE2(tet_Tio, 4, "rcvmsg: read %s bytes data",
			tet_i2a(tp->tp_cnt));
		/* fall through */
	default:
		/* read the message data */
		if (doread(pp) <= 0)
			return(ER_OK);
		TDUMP(tet_Tio, 10, tp->tp_buf, pp->ptm_len, msgdata);
		if ((rc = bs2md(tp->tp_buf, pp)) != ER_OK) {
			pp->pt_flags |= PF_IODONE;
			return(rc);
		}
		pp->pt_flags |= PF_IODONE;
		break;
	}

	return(ER_OK);
}

/*
**	doread() - common read processing for both header and data
**
**	return	1 when done
**		0 for not done
**		-1 on error
*/

static int doread(pp)
register struct ptab *pp;
{
	register struct tptab *tp = (struct tptab *) pp->pt_tdata;
	register int cnt, rc = -1;
	int err;

	TRACE3(tet_Tio, 4, "doread: expect %s bytes on sd %s",
		tet_i2a(tp->tp_cnt), tet_i2a(tp->tp_sd));

	do {
		err = 0;
		if ((cnt = recv(tp->tp_sd, (void *) tp->tp_ptr, tp->tp_cnt, 0)) < 0)
			err = SOCKET_ERRNO;
	} while (cnt < 0 && err == SOCKET_EINTR);

	if (cnt < 0) {
		/* read error */
		TRACE2(tet_Tio, 4, "read error, errno = %s", tet_errname(err));
		switch (err) {
		case SOCKET_ECONNRESET:
			error(0, "unexpected EOF", tet_r2a(&pp->pt_rid));
			pp->pt_state = PS_DEAD;
			break;
		case SOCKET_EWOULDBLOCK:
			if (pp->pt_flags & PF_NBIO)
				return(0);
			/* else fall through */
		default:
			pp->pt_flags |= PF_IOERR;
			break;
		}
	}
	else if (cnt == 0) {
		/* end-of-file */
		TRACE1(tet_Tio, 4, "encountered EOF");
		pp->pt_state = PS_DEAD;
	}
	else if ((tp->tp_cnt -= cnt) > 0) {
		/* partial read - come back for more later */
		TRACE3(tet_Tio, 4, "partial read: wanted %s, received %s",
			tet_i2a(tp->tp_cnt + cnt), tet_i2a(cnt));
		tp->tp_ptr += cnt;
		return(0);
	}
	else {
		/* all done for now */
		TRACE1(tet_Tio, 4, "this read complete");
		rc = 1;
	}

	if (pp->pt_state == PS_DEAD)
		pp->pt_flags |= PF_IODONE;

	if (pp->pt_flags & PF_IOERR)
		error(err, "socket read error", tet_r2a(&pp->pt_rid));

	/* all non-zero return codes come here */
	pp->pt_flags &= ~PF_INPROGRESS;
	return(rc);
}

/*
**	tet_ts_sndmsg() - send a message to a connected process
**
**	set one of PF_INPROGRESS, PF_IODONE or PF_IOERR on return
**
**	return an ER_ code to show the state of the local send processing
*/

int tet_ts_sndmsg(pp)
register struct ptab *pp;
{
	register struct tptab *tp = (struct tptab *) pp->pt_tdata;
	register int rc, err;

#ifndef _WIN32		/* -WIN32-CUT-LINE- */
#  ifdef TET_THREADS
	sigset_t newset, oldset;
#  endif /* TET_THREADS */
	struct sigaction ignsa, oldsa;

	ignsa.sa_handler = SIG_IGN;
	ignsa.sa_flags = 0;
	(void) sigemptyset(&ignsa.sa_mask);
#endif /* !_WIN32 */	/* -WIN32-CUT-LINE- */

	/* set up variables for this message on first time through */
	if ((pp->pt_flags & PF_INPROGRESS) == 0) {
		do {
			err = 0;
			tp->tp_cnt = tet_dtmhdr2bs(&pp->ptm_hdr, tp->tp_buf);
			if (pp->ptm_len <= 0)
				rc = 0;
			else if ((rc = md2bs(pp)) < 0) {
				if (pp->pt_flags & PF_SERVER) {
					pp->pt_flags |= PF_IODONE;
					return(rc);
				}
				else {
					pp->ptm_rc = ER_ERR;
					pp->ptm_len = 0;
					TRACE2(tet_Tserv, 3,
						"md2bs failed, new rc = %s",
						tet_ptrepcode(pp->ptm_rc));
					err = 1;
					continue;
				}
			}
			TDUMP(tet_Tio, 10, tp->tp_buf, tp->tp_cnt, msghdr);
			if (rc > 0) {
				TDUMP(tet_Tio, 10, tp->tp_buf + tp->tp_cnt,
					rc, msgdata);
				tp->tp_cnt += rc;
			}
			(void) tet_dmlen2bs(rc, tp->tp_buf);
		} while (err);
		tp->tp_ptr = tp->tp_buf;
		pp->pt_flags |= PF_INPROGRESS;
	}

	TRACE3(tet_Tio, 4, "sendmsg: want to write %s bytes on sd %s",
		tet_i2a(tp->tp_cnt), tet_i2a(tp->tp_sd));

#ifndef _WIN32		/* -WIN32-CUT-LINE- */
	/* write out the message -
		Ignore SIGPIPE and detect broken connections via errno.
		Signals just cause the write to be restarted (in an attempt
		to cope with systems without restartable system calls) */
#  ifndef TET_THREADS
	(void) sigaction(SIGPIPE, &ignsa, &oldsa);
#  else
	/* just block the signal for now so as to avoid interfering with
	   another thread's usage of SIGPIPE */
	(void) sigemptyset(&newset);
	(void) sigaddset(&newset, SIGPIPE);
	(void) TET_THR_SIGSETMASK(SIG_BLOCK, &newset, &oldset);
#  endif /* TET_THREADS */
#endif /* !_WIN32 */	/* -WIN32-CUT-LINE- */

	do {
		err = 0;
		if ((rc = send(tp->tp_sd, (void *) tp->tp_ptr, tp->tp_cnt, 0)) < 0)
			err = SOCKET_ERRNO;
	} while (rc < 0 && err == SOCKET_EINTR);

#ifndef _WIN32		/* -WIN32-CUT-LINE- */
#  ifndef TET_THREADS
	(void) sigaction(SIGPIPE, &oldsa, (struct sigaction *) 0);
#  else
	if (rc < 0 && (err == EPIPE || err == ECONNRESET))
	{
		/* a SIGPIPE is pending: get rid of it by setting it to be
		   ignored then unblocking it.  This could interfere
		   with another thread's usage of SIGPIPE, but the
		   chances are very small.  Note that we unblock SIGPIPE
		   explicitly, since it may be a member of "oldset". */
		(void) sigaction(SIGPIPE, &ignsa, &oldsa);
		(void) TET_THR_SIGSETMASK(SIG_UNBLOCK, &newset, (sigset_t *) 0);

		/* now restore the original mask and handler */
		(void) TET_THR_SIGSETMASK(SIG_SETMASK, &oldset, (sigset_t *) 0);
		(void) sigaction(SIGPIPE, &oldsa, (struct sigaction *) 0);
	}
	else
		(void) TET_THR_SIGSETMASK(SIG_SETMASK, &oldset, (sigset_t *) 0);
#  endif /* TET_THREADS */
#endif /* !_WIN32 */	/* -WIN32-CUT-LINE- */

	/* set state and flags according to results */
	if (rc < 0) {
		/* write error */
		TRACE2(tet_Tio, 4, "write error, errno = %s", tet_errname(err));
		switch (err) {
#ifndef _WIN32		/* -WIN32-CUT-LINE- */
		case EPIPE:
#endif			/* -WIN32-CUT-LINE- */
		case SOCKET_ECONNRESET:
			error(0, "process connection broken",
				tet_r2a(&pp->pt_rid));
			pp->pt_flags |= PF_IODONE;
			pp->pt_state = PS_DEAD;
			break;
		case SOCKET_EWOULDBLOCK:
			if (pp->pt_flags & PF_NBIO)
				return(ER_OK);
			/* else fall through */
		default:
			error(err, "socket write error", tet_r2a(&pp->pt_rid));
			pp->pt_flags |= PF_IOERR;
			break;
		}
	}
	else if ((tp->tp_cnt -= rc) > 0) {
		/* partial write - try again later */
		TRACE3(tet_Tio, 4, "partial write: requested %s, only wrote %s",
			tet_i2a(tp->tp_cnt + rc), tet_i2a(rc));
		tp->tp_ptr += rc;
		return(ER_OK);
	}
	else {
		/* all done */
		TRACE1(tet_Tio, 4, "write complete");
		pp->pt_flags |= PF_IODONE;
	}

	/* here if no more data to write now */
	pp->pt_flags &= ~PF_INPROGRESS;
	return(ER_OK);
}

/*
**	bs2md() - convert machine-independent message data to internal format
**		depending on message type
**
**	return ER_OK if successful, or -ve error code on error
*/

static int bs2md(bp, pp)
char *bp;
struct ptab *pp;
{
	register int rc;

	if ((pp->pt_flags & PF_SERVER) == 0)
		pp->pt_savreq = pp->ptm_req;

	switch (pp->pt_savreq) {
	case OP_NULL:
		pp->ptm_len = 0;
		rc = ER_OK;
		break;
	default:
		if ((rc = tet_ss_bs2md(bp, pp)) < 0)
			pp->ptm_len = 0;
		else {
			pp->ptm_len = (short) rc;
			rc = ER_OK;
		}
		break;
	}

	return(rc);
}

/*
**	md2bs() - convert message data to machine-independent format
**		depending on message type
**
**	return the number of bytes occupied by the message, or -ve error code
**	on error
*/

static int md2bs(pp)
register struct ptab *pp;
{
	register struct tptab *tp = (struct tptab *) pp->pt_tdata;
	register int len = 0;

	/* work out the required buffer size */
	switch (pp->ptm_req) {
	case OP_NULL:
		len = pp->ptm_len;
		break;
	}

	/* check that the buffer is big enough */
	if (len > 0 && BUFCHK(&tp->tp_buf, &tp->tp_len, tp->tp_cnt + len) < 0)
		return(ER_ERR);

	/* copy the internal data into the machine-independent buffer */
	switch (pp->ptm_req) {
	case OP_NULL:
		bcopy(pp->ptm_data, tp->tp_buf + tp->tp_cnt, len);
		break;
	default:
		len = tet_ss_md2bs(pp, &tp->tp_buf, &tp->tp_len, tp->tp_cnt);
		break;
	}

	return(len);
}

/*
**	tet_ts_dead() - perform transport-specific actions for a
**		dead connected process
*/

void tet_ts_dead(pp)
register struct ptab *pp;
{
	register struct tptab *tp = (struct tptab *) pp->pt_tdata;

	TRACE2(tet_Tio, 4, "tet_ts_dead: close sd %s", tet_i2a(tp->tp_sd));

	(void) SOCKET_CLOSE(tp->tp_sd);
	tp->tp_sd = INVALID_SOCKET;
	pp->pt_flags &= ~(PF_CONNECTED | PF_INPROGRESS);
}

/*
**	tet_ts_startup() - perform transport-specific actions on startup
**
**	there is no return on error
*/

TET_IMPORT void tet_ts_startup()
{
#ifdef _WIN32	/* -START-WIN32-CUT- */

	WORD version = MAKEWORD(1, 1);
	WSADATA wsadata;
	int rc;
	static int been_here;

	if (been_here++)
		return;

	if ((rc = WSAStartup(version, &wsadata)) != 0)
		fatal(rc, "WSAStartup() failed", (char *) 0);

#else		/* -END-WIN32-CUT- */

	/* nothing */

#endif		/* -WIN32-CUT-LINE- */
}

/*
**	tet_ts_cleanup() - perform transport-specific actions before exiting
*/

void tet_ts_cleanup()
{
#ifdef _WIN32	/* -START-WIN32-CUT- */

	static int been_here;

	if (been_here++)
		return;

	(void) WSACleanup();

#else		/* -END-WIN32-CUT- */

	/* nothing */

#endif		/* -WIN32-CUT-LINE- */
}

