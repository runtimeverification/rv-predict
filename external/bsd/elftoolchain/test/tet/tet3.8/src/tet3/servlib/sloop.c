/*
 *      SCCS:  @(#)sloop.c	1.11 (03/08/28) 
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
static char sccsid[] = "@(#)sloop.c	1.11 (03/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)sloop.c	1.11 03/08/28 TETware release 3.8
NAME:		sloop.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	generic server loop processing functions

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., January 1994
	added ptab tracing

	Andrew Dingwall, UniSoft Ltd., December 1996
	removed (time_t == unsigned long) assumption for n-bit portability

	Matthew Hails, The Open Group, August 2003
	Replaced INFINITY macro with the use of new MAX_TIME_INTERVAL macro
	defined in dtmac.h

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <limits.h>
#include <sys/types.h>
#include <time.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "error.h"
#include "server.h"
#include "servlib.h"
#include "tslib.h"

#ifndef NOTRACE
#include "ltoa.h"
#include "dtetlib.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* static function declarations */
static void si_procrun PROTOLIST((void));
static int si_timeouts PROTOLIST((void));


/*
**	tet_si_serverloop() - perform a single server process loop iteration
**
**	poll for incoming requests, then service any resulting events;
**	do timeout processing, then service any resulting events
**
**	the tet_ts_poll() routine should perform a poll of all connections to
**	processes in states PS_IDLE, PS_RCVMSG, PS_SNDMSG and PS_CONNECT
**	if data can be read (PS_IDLE or PS_RCVMSG) or written (PS_SNDMSG
**	or PS_CONNECT) without blocking, raise PF_ATTENTION
**	change the state to PS_RCVMSG or PS_SNDMSG as appropriate
**	if the poll is implemented by doing a non-blocking read or write,
**	set the state and flags as described for tet_ts_rcvmsg()
**	or tet_ts_sndmsg()
**
**	if a new connection request is received, a new ptab element should be
**	allocated by tet_ptalloc() and linked to the ptab list by tet_ptadd() -
**	if a connection is accepted without reading any data, set the state to
**	PS_RCVMSG and raise PF_ATTENTION, otherwise set the state and flags as
**	described for tet_ts_rcvmsg() below
**
**	tet_ts_poll() should return 1 as soon as i/o is possible, or 0 after
**	delay seconds if no i/o is possible
**	if delay is -ve, tet_ts_poll() should not return until i/o is possible
*/

void tet_si_serverloop()
{
	register struct ptab *pp;
	register time_t timeout;
	register int delay, rc;

	/* see how long it is to the next timeout */
	timeout = MAX_TIME_INTERVAL;
	for (pp = tet_ptab; pp; pp = pp->pt_next) {
		ASSERT(pp->pt_magic == PT_MAGIC);
		if (pp->pt_timeout && pp->pt_timeout < timeout)
			timeout = pp->pt_timeout;
	}
	if (timeout == MAX_TIME_INTERVAL)
		delay = tet_ptab ? LONGDELAY : SHORTDELAY;
	else if ((delay = (int) (timeout - time((time_t *) 0))) < 0 ||
		delay > LONGDELAY)
			delay = LONGDELAY;

	TRACE3(tet_Tloop, 2,
		"tet_si_serverloop TOP: tet_ptab = %s, poll timeout = %s",
		tet_i2x(tet_ptab), tet_i2a(delay));

#ifndef NOTRACE
	if (tet_Tloop) {
		TRACE2(tet_Tloop, 10, "process table:%s",
			tet_ptab ? "" : " empty");
		for (pp = tet_ptab; pp; pp = pp->pt_next)
			TRACE6(tet_Tloop, 10,
		"pp = %s, next = %s, proc = %s, state = %s, flags = %s",
				tet_i2x(pp), tet_i2x(pp->pt_next),
				tet_r2a(&pp->pt_rid),
				tet_ptstate(pp->pt_state),
				tet_ptflags(pp->pt_flags));
	}
#endif

	/* perform the main loop */
	if ((rc = tet_ts_poll(tet_ptab, delay)) > 0)
		si_procrun();
	else if (rc < 0)
		exit(1);
	if (si_timeouts() > 0)
		si_procrun();
}

/*
**	si_procrun() - scan the process table for processes that need servicing
**		call the service routine for those that do
*/

static void si_procrun()
{
	register struct ptab *pp;
	register int done;

	TRACE2(tet_Tloop, 8, "si_procrun START: tet_ptab = %s",
		tet_i2x(tet_ptab));

	do {
		TRACE2(tet_Tloop, 8, "si_procrun RESTART: tet_ptab = %s",
			tet_i2x(tet_ptab));
		done = 1;
		for (pp = tet_ptab; pp; pp = pp->pt_next) {
			TRACE3(tet_Tloop, 8, "pp = %s, next = %s",
				tet_i2x(pp), tet_i2x(pp->pt_next));
			ASSERT(pp->pt_magic == PT_MAGIC);
			if (pp->pt_flags & PF_ATTENTION) {
				pp->pt_flags &= ~PF_ATTENTION;
				tet_si_service(pp);
				done = 0;
				break;
			}
		}
	} while (!done);

	TRACE2(tet_Tloop, 8, "si_procrun END: tet_ptab = %s",
		tet_i2x(tet_ptab));

	/* call the server-specific end-procrun routine */
	tet_ss_procrun();
}

/*
**	si_timeouts() - process event timeouts
**
**	return 1 if any events need to be serviced, 0 otherwise
*/

static int si_timeouts()
{
	register struct ptab *pp;
	register time_t now = time((time_t *) 0);
	register int found = 0;

	for (pp = tet_ptab; pp; pp = pp->pt_next) {
		ASSERT(pp->pt_magic == PT_MAGIC);
		if (pp->pt_timeout > 0 && pp->pt_timeout <= now) {
			pp->pt_flags |= PF_TIMEDOUT;
			pp->pt_timeout = 0;
			TRACE4(tet_Tloop, 4, "%s: about to call tet_ss_timeout: state = %s, flags = %s",
				tet_r2a(&pp->pt_rid),
				tet_ptstate(pp->pt_state),
				tet_ptflags(pp->pt_flags));
			tet_ss_timeout(pp);
			found = 1;
		}
	}

	return(found);
}

