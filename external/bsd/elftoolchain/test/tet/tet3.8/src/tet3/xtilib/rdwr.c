/*
 *      SCCS:  @(#)rdwr.c	1.8 (99/09/03) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1993 X/Open Company Limited
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
static char sccsid[] = "@(#)rdwr.c	1.8 (99/09/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)rdwr.c	1.8 99/09/03 TETware release 3.8
NAME:		rdwr.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Limited.
		(based on inetlib/rdwr.c)
DATE CREATED:	May 1993

DESCRIPTION:
	required transport-specific library interfaces

	functions to perform i/o on remote process connections

MODIFICATIONS:
	Geoff Clare, UniSoft Ltd., August 1996
	Changes for TETware.

	Geoff Clare, UniSoft Ltd., Oct 1996
	Added tet_ts_startup().

************************************************************************/

#include <stdio.h>
#include <errno.h>
#include <sys/types.h>
#include <time.h>
#include <unistd.h>
#include <xti.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "tptab_xt.h"
#include "bstring.h"
#include "error.h"
#include "ltoa.h"
#include "dtetlib.h"
#include "tslib.h"
#include "server_bs.h"
#include "xtilib_xt.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

#ifndef NOTRACE
static char msghdr[] = "message header";
static char msgdata[] = "message data";
#endif


/* static function declarations */
static int bs2md PROTOLIST((char *, struct ptab *));
static int doread PROTOLIST((struct ptab *));
static int md2bs PROTOLIST((struct ptab *));


/*
**	tet_ts_rcvmsg() - recieve an XTI message on a file descriptor with
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

	int 	flags;

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
	int flags, save_errno;

	TRACE3(tet_Tio, 4, "doread: expect %s bytes on fd %s",
		tet_i2a(tp->tp_cnt), tet_i2a(tp->tp_fd));

	do {
		errno = 0;
		cnt = t_rcv(tp->tp_fd, tp->tp_ptr, (unsigned)tp->tp_cnt, &flags);
	} while (cnt < 0 && (t_errno == TSYSERR && errno == EINTR));
	save_errno = t_errno;

	if (cnt < 0) {
		/* read error */
		TRACE3(tet_Tio, 4, "read error, t_errno = %s, read event = %s",
			tet_xterrno2a(save_errno),
			tet_xtev2a(t_look(tp->tp_fd)));
		switch (save_errno) {
		case TLOOK:
			/* T_DISCONNECT or T_ORDREL (presumably) */
			if (pp->pt_flags & PF_LOGGEDON)
				error(0, "unexpected EOF",
					tet_r2a(&pp->pt_rid));
			pp->pt_state = PS_DEAD;
			break;
		case TNODATA:
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
		error(save_errno, "t_rcv() error", tet_r2a(&pp->pt_rid));

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
	int save_errno;
	SIG_T (*pipe_save)();

	int	flags;

	/* set up variables for this message on first time through */
	if ((pp->pt_flags & PF_INPROGRESS) == 0) {
		do {
			err = 0;
			tp->tp_cnt = tet_dtmhdr2bs(&pp->ptm_hdr, tp->tp_buf);
			if (pp->ptm_len <= 0)
				rc = 0;
			else if ((rc = md2bs(pp)) < 0)
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

	TRACE3(tet_Tio, 4, "sendmsg: want to write %s bytes on fd %s",
		tet_i2a(tp->tp_cnt), tet_i2a(tp->tp_fd));

	/* write out the message -
		signals cause the write to be restarted (in an attempt
		to cope with systems without restartable system calls) */
	do {
		errno = 0;
		flags = 0;
		rc = t_snd(tp->tp_fd, tp->tp_ptr,
			(unsigned) tp->tp_cnt, flags);
	} while (rc < 0 && (t_errno == TSYSERR && errno == EINTR));
	save_errno = t_errno;

	/* set state and flags according to results */
	if (rc < 0) {
		/* write error */
		TRACE2(tet_Tio, 4, "t_snd error, t_errno = %s",
			tet_xterrno2a(save_errno));
		switch (save_errno) {
		case TLOOK:
			if (t_look(tp->tp_fd) != T_DISCONNECT) {
				/* return value unexpected */
				xt_error(save_errno, "t_snd() write error",
					tet_r2a(&pp->pt_rid));
				pp->pt_flags |= PF_IOERR;
				break;
			}
			/* else fall through */
				
		case TSYSERR:
		
			xt_error(save_errno, "process connection broken",
				tet_r2a(&pp->pt_rid));
			pp->pt_flags |= PF_IODONE;
			pp->pt_state = PS_DEAD;
			break;

		case TFLOW:
			if (pp->pt_flags & PF_NBIO)
				return(ER_OK);
			/* else fall through */
		default:
			xt_error(save_errno, "t_snd() write error",
				tet_r2a(&pp->pt_rid));
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

	TRACE2(tet_Tio, 4, "tet_ts_dead: close fd %s", tet_i2a(tp->tp_fd));

	(void) t_close(tp->tp_fd);
	tp->tp_fd = -1;
	pp->pt_flags &= ~(PF_CONNECTED | PF_INPROGRESS);
}

/*
**	tet_ts_startup() - perform transport-specific actions on startup
*/

void tet_ts_startup()
{
	/* nothing */
}

/*
**	tet_ts_cleanup() - perform transport-specific actions before exiting
*/

void tet_ts_cleanup()
{
	/* nothing */
}

