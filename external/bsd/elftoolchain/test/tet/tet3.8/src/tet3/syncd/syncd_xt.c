/*
 *      SCCS:  @(#)syncd_xt.c	1.7 (99/09/02) 
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
static char sccsid[] = "@(#)syncd_xt.c	1.7 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)syncd_xt.c	1.7 99/09/02 TETware release 3.8
NAME:		syncd_xt.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Ltd.
DATE CREATED:	April 1993

DESCRIPTION:
	server-specific functions for syncd  XTI version. These functions
	are based on the INET equivalents in syncd_in.c

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., November 1993
	added transport-specific command line argument processing

	Andrew Dingwall, UniSoft Ltd., February 1995
	added t_sync() call to refresh the TP endpoint after the
	dup/fork/exec in the parent


************************************************************************/


#include <sys/types.h>
#include <time.h>
#include <xti.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "error.h"
#include "server_xt.h"
#include "xtilib_xt.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;       /* file name for error reporting */
#endif

int tet_listen_fd = 0;			/* descriptor on which to listen  */
char *tet_tpname = (char *)0;		/* Transport provider name */
struct t_call *tet_calls[MAX_CONN_IND];	/* to hold connection indications */

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
**	ss_tsinitb4fork() - syncd transport-specific initialisation
*/

void ss_tsinitb4fork()
{
	/* make sure that we have a transport provider name -
		should be passed on the command line */
	if (!tet_tpname || !*tet_tpname)
		fatal(0, "need a transport provider name", (char *) 0);

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

