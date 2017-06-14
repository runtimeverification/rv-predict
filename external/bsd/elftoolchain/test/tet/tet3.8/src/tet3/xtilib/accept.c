/*
 *      SCCS:  @(#)accept.c	1.12 (99/09/03) 
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
static char sccsid[] = "@(#)accept.c	1.12 (99/09/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)accept.c	1.12 99/09/03 TETware release 3.8
NAME:		accept.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Ltd.
DATE CREATED:	April 1993

DESCRIPTION:
	function to service a new connection indication


MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, September 1993
	added additional error reporting code

	Andrew Dingwall, UniSoft Ltd., December 1993
	added malloc tracing

	Andrew Dingwall, UniSoft Ltd., November 1994
	updated t_alloc() structure type names in line with latest XTI spec

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 

************************************************************************/

#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <sys/types.h>
#include <time.h>
#include <fcntl.h>
#include <xti.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "tptab_xt.h"
#include "ltoa.h"
#include "error.h"
#include "tslib.h"
#include "server.h"
#include "server_xt.h"
#include "xtilib_xt.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/*
**	tet_ts_accept() - accept a new connection on a listening file  
**		descriptor and allocate a ptab entry for it
*/

void tet_ts_accept(lfd)
int lfd;
{
	register struct ptab *pp;
	register struct tptab *tp;
	register int nfd;
	int	i,len, c_event;
	char	*p;


	TRACE2(tet_Tio, 4, "accept connection on fd %s", tet_i2a(lfd));


	for (i = 0; i < MAX_CONN_IND; i++) {

		if (tet_calls[i] == (struct t_call *) 0)
			continue;

		/* allocate a proc table entry for this connection */
		if ((pp = tet_ptalloc()) == (struct ptab *) 0)
			return;
	

		/* allocate a file descriptor for the endpoint on which the
		   connection is to be accepted */
		if (!tet_tpname || !*tet_tpname) {
			error(0, "no transport provider interface defined",
				(char *) 0);
			tet_ptfree(pp);
			return;
		}
		if ((nfd = t_open(tet_tpname, O_RDWR, (struct t_info *) 0)) < 0) {
			xt_error(t_errno, "can't open", tet_tpname);
			tet_ptfree(pp);
			return;
		}
	
		/* bind this endpoint to an arbitrary protocol address */
		if (t_bind(nfd, (struct t_bind *) 0, (struct t_bind *) 0) < 0) {
			xt_error(t_errno, "can't bind fd", tet_i2a(nfd));
			(void) t_close(nfd);
			tet_ptfree(pp);
			return;
		}	
			
		/* accept the connection */
		if (t_accept(lfd, nfd, tet_calls[i]) < 0) {

			/* a return of TLOOK is presumably a T_DISCONNECT or
			   T_CONNECT indication. By just returning we process
			   this event on the next poll  */

			if (t_errno != TLOOK) {
				/* an unexpected error */
				c_event = t_look(lfd);
				xt_error(t_errno,"unexpected event",
					tet_xtev2a(c_event));
			}
			(void) t_close(nfd);
			tet_ptfree(pp);
			return;
		}
		
		/* Store the remote (callers) address */

		tp = (struct tptab *) pp->pt_tdata;

		tp->tp_fd		= nfd;
		tp->tp_call.maxlen	= tet_calls[i]->addr.maxlen;
		tp->tp_call.len		= tet_calls[i]->addr.len;

		errno = 0;
		if ((tp->tp_call.buf = (char *) malloc((size_t)tet_calls[i]->addr.maxlen)) == (char *) 0) {
			error(errno, "can't malloc caller address", (char *) 0);
			tet_ptfree(pp);
			return;
		}
		TRACE2(tet_Tbuf, 6, "allocate tp_call.buf = %s",
			tet_i2x(tp->tp_call.buf));

		(void) memcpy(tp->tp_call.buf, tet_calls[i]->addr.buf,
			(size_t)tet_calls[i]->addr.len);	

		TRACE3(tet_Tbuf, 6, "t_free tet_calls[%s] = %s",
			tet_i2a(i), tet_i2x(tet_calls[i]));
		(void) t_free((char *)tet_calls[i], T_CALL);
 		tet_calls[i] = (struct t_call *)0;


		/* call server-specific routine to massage endpoint */
		if (tet_ss_tsafteraccept(pp) < 0) {
			tet_ts_dead(pp);
			tet_ptfree(pp);
			return;
		}

		/* pass the new ptab entry to the server for registration */
		tet_ss_newptab(pp);

		pp->pt_flags |= PF_CONNECTED;
	}
}

