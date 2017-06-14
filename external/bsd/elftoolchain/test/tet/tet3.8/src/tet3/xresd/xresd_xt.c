/*
 *      SCCS:  @(#)xresd_xt.c	1.9 (99/09/03) 
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
static char sccsid[] = "@(#)xresd_xt.c	1.9 (99/09/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)xresd_xt.c	1.9 99/09/03 TETware release 3.8
NAME:		xresd_in.c
PRODUCT:	TETware
AUTHOR:		Denis McCOnalogue, UniSoft Ltd.
DATE CREATED:	April 1993

DESCRIPTION:
	server-specific functions for XTI xresd server

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., January 1994
	added transport-specific argument processing
	moved XTI-specific stuff from xresd.c to here

	Andrew Dingwall, UniSoft Ltd., February 1995
	added t_sync() call to refresh the TP endpoint after the
	dup/fork/exec in the parent

	Andrew Dingwall, UniSoft Ltd., July 1998
	Changed tet_calls[] from (struct t_call) to (struct t_call *).
 

************************************************************************/

#include <stdio.h>
#include <xti.h>
#include <sys/types.h>
#include <time.h>
#include "dtmac.h"
#include "error.h"
#include "dtmsg.h"
#include "ptab.h"
#include "server_xt.h"
#include "xtilib_xt.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;       /* file name for error reporting */
#endif

struct t_call *tet_calls[MAX_CONN_IND];	/* to hold connection indications */
int tet_listen_fd = 0;			/* descriptor on which to listen  */
char *tet_tpname = (char *) 0;		/* Transport provider name */

/*
**	tet_ss_tsaccept() - server-specific accept() processing
*/

void tet_ss_tsaccept()
{
	/* accept the connection unless we are closing down */
	if (tet_listen_fd >= 0)
		tet_ts_accept(tet_listen_fd);
}

/*
**	tet_ss_tsafteraccept() - server-specific things to do after an accept()
*/

int tet_ss_tsafteraccept(pp)
struct ptab *pp;
{
	/* establish non-blocking i/o on the connection */
	return(tet_ts_nbio(pp));
}

/*
**	ss_tsinitb4fork() - xresd transport-specific initialisation
*/

void ss_tsinitb4fork()
{
	/* ensure we have a transport provider name -
		should have been passed on the command line */
	if (!tet_tpname || !*tet_tpname)
		fatal(0, "must specify a transport provider name", (char *) 0);

	/* refresh the transport provider state */
	if (t_sync(tet_listen_fd) < 0)
		xt_error(t_errno, "t_sync() failed on listen fd", (char *) 0);

	/* arrange to accept incoming connections */
	tet_ts_listen(tet_listen_fd);
}

/*
**	ss_tsargproc() - syncd transport-specific command-line
**		argument processing
**
**	return 0 if only firstarg was used or 1 if both args were used
*/

int ss_tsargproc(firstarg, nextarg)
char *firstarg, *nextarg;
{
	register int rc = 0;

	switch (*(firstarg + 1)) {
	case 'P':
		if (*(firstarg + 2))
			tet_tpname = firstarg + 2;
		else {
			tet_tpname = nextarg;
			rc = 1;
		}
		break;
	default:
		fatal(0, "unknown option:", firstarg);
		/* NOTREACHED */
	}

	return(rc);
}

