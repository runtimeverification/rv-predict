/*
 *      SCCS:  @(#)connect.c	1.12 (99/09/03) 
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
static char sccsid[] = "@(#)connect.c	1.12 (99/09/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)connect.c	1.12 99/09/03 TETware release 3.8
NAME:		connect.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, Unisoft Ltd.
DATE CREATED:	May 1993

DESCRIPTION:
	required transport-specific library interface

	function to initiate connection to remote process

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, September 1993
	make sure client fd endpoint is not 0, 1 or 2.

	Denis McConalogue, UniSoft Limited, September 1993
	added additional error reporting code.

	Andrew Dingwall, UniSoft Ltd., December 1993
	moved disconnect stuff to a separate file

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 

************************************************************************/

#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/types.h>
#include <time.h>
#include <xti.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "tptab_xt.h"
#include "error.h"
#include "ltoa.h"
#include "tslib.h"
#include "server_xt.h"
#include "xtilib_xt.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_ts_connect() - make a connection to a remote process
*/

void tet_ts_connect(pp)
register struct ptab *pp;
{
	register struct tptab *tp = (struct tptab *) pp->pt_tdata;
	register int rc, fd, nfd, c_event;
	char buf[256], *event;
	struct t_info info;
	struct t_call sndcall;

	/*	get a file descriptor for the transport endpoint */
	if (!tet_tpname || !*tet_tpname) {
		error(0, "no transport provider interface defined", (char *)0);
		pp->pt_state  = PS_DEAD;
		pp->pt_flags |= PF_ATTENTION;
		return;
	}
	if ((fd = t_open(tet_tpname, O_RDWR, &info)) < 0) {
		xt_error(t_errno, "can't open transport provider", tet_tpname); 
		pp->pt_state = PS_DEAD;
		pp->pt_flags |= PF_ATTENTION;
		return;
	}
	
	/* make sure the file descriptor is not 0, 1, or 2 */
	if (fd < 3) {
		if ((nfd = fcntl(fd, F_DUPFD, 3)) < 3) {
			error(errno, "can't dup fd", tet_i2a(fd));
			pp->pt_state = PS_DEAD;
			pp->pt_flags |= PF_ATTENTION;
			return;
		}
		(void) t_close(fd);
		fd = nfd;
	}

	/* bind this endpoint to an arbitrary protocol address */
	if (t_bind(fd, (struct t_bind *)0, (struct t_bind *)0) < 0) {
		xt_error(t_errno, "can't bind fd", tet_i2a(fd));
		(void) t_close(fd);
		pp->pt_state = PS_DEAD;
		pp->pt_flags |= PF_ATTENTION;
		return;
	}
              
	tp->tp_fd = fd;


	/*	call the server-specific connect routine to massage the 
		file descriptor to fill in the the network address */
	if (tet_ss_tsconnect(pp) < 0) {
		pp->pt_state = PS_DEAD;
		pp->pt_flags |= PF_ATTENTION;
		return;
	}

	TRACE2(tet_Tio, 4, "connect to %s", tet_addr2lname(&tp->tp_call));

	sndcall.addr.maxlen	= tp->tp_call.maxlen;
	sndcall.addr.len	= tp->tp_call.len;
	sndcall.addr.buf	= tp->tp_call.buf;
	sndcall.opt.len		= 0;
	sndcall.udata.len	= 0;

	/*	attempt the connection  */
	do {
		errno   = 0;
		t_errno = 0;
		rc = t_connect(fd, &sndcall, (struct t_call *)0);
	} while (rc < 0 && (t_errno==TSYSERR && errno==EINTR));

	if (rc < 0) {
		switch(t_errno) {
		case TNODATA:
			if (pp->pt_flags & PF_NBIO) {
				TRACE1(tet_Tio, 6, "connect in progress");
				pp->pt_flags |= PF_INPROGRESS;
				return;
			}
			/* else just fall through */

		default:
			event = tet_xtev2a(t_look(fd));
			TRACE2(tet_Tio, 6, "connect failed:%s", event);
			(void) sprintf(buf, "%s(%.*s)", "connect failed",
				(int) sizeof buf - (int) strlen(event) - 17,
				event);
			xt_error(t_errno, buf, tet_r2a(&pp->pt_rid));
			(void) t_close(fd);
			pp->pt_state  = PS_DEAD;
			break;	
		}
	}
	else {
		TRACE1(tet_Tio, 6, "connect succeeded");
		pp->pt_flags |= PF_CONNECTED;
	}
	
	/* here if the connect call completed one way or the other */
	pp->pt_flags |= PF_ATTENTION;
}
