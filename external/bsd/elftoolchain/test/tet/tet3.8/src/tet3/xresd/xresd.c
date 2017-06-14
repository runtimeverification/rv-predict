/*
 *      SCCS:  @(#)xresd.c	1.13 (99/09/03) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1992 X/Open Company Limited
 * (C) Copyright 1994 UniSoft Limited
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
static char sccsid[] = "@(#)xresd.c	1.13 (99/09/03) TETware release 3.8";
static char *copyright[] = {
	"(C) Copyright 1996 X/Open Company Limited",
	"All rights reserved"
};
#endif

/************************************************************************

SCCS:   	@(#)xresd.c	1.13 99/09/03 TETware release 3.8
NAME:		xresd.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	server-specific routines for xresd server

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, June 1993
	XTI enhancements - fill in name of transport provider
			   interface from -P command line option.

	Denis McConalogue, UniSoft Limited, September 1993
	added support for OP_XRCLOSE message request

	Denis McConalogue, UniSoft Limited, September 1993
	tet_ss_logon() - allow more than one MTCC to log on

	Denis McConalogue, UniSoft Limited, September 1993
	added ss_disconnect() function

	Andrew Dingwall, UniSoft Ltd., November 1993
	enhancements for FIFO transport interface
	moved XTI-specific stuff to xresd_xt.c
	removed disconnect stuff

	Andrew Dingwall, UniSoft Ltd., June 1997
	changes to enable parallel remote and distributed test cases
	to work correctly

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <time.h>
#include <sys/types.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "bstring.h"
#include "ptab.h"
#include "sptab.h"
#include "xtab.h"
#include "error.h"
#include "globals.h"
#include "ltoa.h"
#include "xresd.h"
#include "server.h"
#include "servlib.h"
#include "dtetlib.h"
#include "tslib.h"

char *Tet_savedir;			/* saved files directory */

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/************************************************************************
**									*
**	MAIN ROUTINES							*
**									*
************************************************************************/

int main(argc, argv)
int argc;
char **argv;
{
	/* must be first */
	tet_init_globals("tetxresd", PT_XRESD, 0, tet_generror, tet_genfatal);
	tet_root[0] = '\0';

	return(tet_si_main(argc, argv, 1));
}

/*
**	tet_ss_argproc() - xresd command-line argument processing
**
**	return 0 if only first arg was used or 1 if both args were used
*/

int tet_ss_argproc(firstarg, nextarg)
char *firstarg, *nextarg;
{
	register int rc;

	switch (*(firstarg + 1)) {
	case 's':
		if (*(firstarg + 2)) {
			Tet_savedir = firstarg + 2;
			rc = 0;
		}
		else {
			Tet_savedir = nextarg;
			rc = 1;
		}
		break;
	default:
		rc = ss_tsargproc(firstarg, nextarg);
		break;
	}

	return(rc);
}

/*
**	tet_ss_initdaemon() - xresd daemon initialisation
*/

void tet_ss_initdaemon()
{
	/* make sure that we have a Tet_savedir from the command line */
	if (!Tet_savedir)
		fatal(0, "must specify a savefiles directory", (char *) 0);

	/* perform xresd transport-specific initialisation */
	ss_tsinitb4fork();

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	/* start the daemon */
	tet_si_forkdaemon();
#endif		/* -WIN32-CUT-LINE- */

	/* detach from the control terminal */
	tet_tiocnotty();
}

/*
**	tet_ss_serverloop() - xresd main processing loop
**
**	return 1 to be recalled, 0 otherwise
*/

int tet_ss_serverloop()
{
	/* perform the generic server loop */
	tet_si_serverloop();

	/* exit if there are no more connected processes,
		otherwise arrange to come back */
	return(tet_ptab ? 1 : 0);
}

/************************************************************************
**									*
**	SERVER-SPECIFIC PARTS OF GENERIC SERVICE ROUTINES		*
**									*
************************************************************************/

/*
**	tet_ss_dead() - server-specific routine to handle a dead process
**
**	server logoff routines should not come here
*/

void tet_ss_dead(pp)
register struct ptab *pp;
{
	static char fmt[] = "%s connection closed";
	static char cl[] = "client";
	static char se[] = "server";
	char msg[sizeof fmt + sizeof cl];

	/* emit a diagnostic message if this is unexpected */
	if ((pp->pt_flags & PF_LOGGEDOFF) == 0) {
		(void) sprintf(msg, fmt, (pp->pt_flags & PF_SERVER) ? se : cl);
		error(0, msg, tet_r2a(&pp->pt_rid));
	}

	pp->pt_flags = (pp->pt_flags & ~PF_LOGGEDON) | PF_LOGGEDOFF;

	/* for clients, remove the related table entries
		and exit if the process table is empty */
	if ((pp->pt_flags & PF_SERVER) == 0) {
		xtdead(pp);
		tfdead(pp);
		tet_so_dead(pp);
	}
}

/************************************************************************
**									*
**	FULL REQUEST PROCESSING ROUTINES				*
**									*
************************************************************************/

/*
**	tet_ss_process() - request processing routine
*/

void tet_ss_process(pp)
struct ptab *pp;
{
	/* a server ptab might get here via tet_si_servwait() */
	if ((pp->pt_flags & PF_SERVER) == 0)
		tet_si_serverproc(pp);
}

/*
**	tet_ss_serverproc() - request processing as a server
*/

void tet_ss_serverproc(pp)
register struct ptab *pp;
{
	switch (pp->ptm_req) {
	case OP_XROPEN:
		op_xropen(pp);
		break;
	case OP_XRCLOSE:
		op_xrclose(pp);
		break;
	case OP_XRSEND:
		op_xrsend(pp);
		break;
	case OP_XRSYS:
		op_xrsys(pp);
		break;
	case OP_ICSTART:
		op_icstart(pp);
		break;
	case OP_TPSTART:
		op_tpstart(pp);
		break;
	case OP_ICEND:
		op_icend(pp);
		break;
	case OP_TPEND:
		op_tpend(pp);
		break;
	case OP_XRES:
		op_xres(pp);
		break;
	case OP_RESULT:
		op_result(pp);
		break;
	case OP_TFOPEN:
		op_tfopen(pp);
		break;
	case OP_TFCLOSE:
		op_tfclose(pp);
		break;
	case OP_TFWRITE:
		op_tfwrite(pp);
		break;
	case OP_FOPEN:
		tet_op_fopen(pp);
		break;
	case OP_FCLOSE:
		tet_op_fclose(pp);
		break;
	case OP_GETS:
		tet_op_gets(pp);
		break;
	case OP_CFNAME:
		op_cfname(pp);
		break;
	case OP_CODESF:
		op_codesf(pp);
		break;
	case OP_RCFNAME:
		op_rcfname(pp);
		break;
	default:
		pp->ptm_rc = ER_REQ;
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
		break;
	}

	/* here to send a reply message */
	pp->pt_state = PS_SNDMSG;
	pp->pt_flags = (pp->pt_flags & ~PF_IODONE) | PF_ATTENTION;
}

/************************************************************************
**									*
**	SERVER-SPECIFIC PARTS OF GENERIC REQUEST PROCESSING		*
**									*
************************************************************************/

/*
**	tet_ss_logon() - server-specific logon processing routine
**
**	return ER_OK if successful or other ER_* error code on error
*/

/* ARGSUSED */
int tet_ss_logon(pp)
/* register */ struct ptab *pp;
{
	register struct ptab *pp1, *pp2;
	register int count;

	/* make sure that we only have one MTCC logged on */
	switch (pp->ptr_ptype) {
	case PT_MTCC:
		pp1 = tet_ptab;
		count = 0;
		while ((pp2 = tet_getnextptbyptype(pp->ptr_ptype, pp1)) != (struct ptab *) 0) {
			if (pp2 != pp)
				count++;
			if ((pp1 = pp2->pt_next) == (struct ptab *) 0)
				break;
		}
		if (count > 0) {
			error(0, "client of this type already logged on",
				tet_r2a(&pp->pt_rid));
			return(ER_LOGON);
		}
		break;
	}

	return(ER_OK);
}

/*
**	tet_ss_logoff() - server-specific logoff processing
*/

void tet_ss_logoff(pp)
struct ptab *pp;
{
	xtdead(pp);
	tfdead(pp);
}

/*
**	tet_ss_cleanup() - clean up and exit
*/

void tet_ss_cleanup()
{
	tet_ts_cleanup();
	exit(0);
}

/************************************************************************
**									*
**	PUBLIC SUBROUTINES						*
**									*
************************************************************************/

/*
**	tet_ss_ptalloc() - allocate server-specific data element in a ptab
**		structure
**
**	return 0 if successful or -1 on error
*/

int tet_ss_ptalloc(pp)
struct ptab *pp;
{
	register struct sptab *sp;

	errno = 0;
	if ((sp = (struct sptab *) malloc(sizeof *sp)) == (struct sptab *) 0) {
		error(errno, "can't get memory for ss data", (char *) 0);
		pp->pt_sdata = (char *) 0;
		return(-1);
	}
	TRACE2(tet_Tbuf, 6, "allocate sptab = %s", tet_i2x(sp));
	bzero((char *) sp, sizeof *sp);

	sp->sp_xrid = -1L;

	pp->pt_sdata = (char *) sp;

	return(0);
}

/*
**	tet_ss_ptfree() - free server-specific data element in a ptab structure
*/

void tet_ss_ptfree(pp)
struct ptab *pp;
{
	register struct sptab *sp = (struct sptab *) pp->pt_sdata;

	TRACE2(tet_Tbuf, 6, "free sptab = %s", tet_i2x(sp));

	if (sp) {
		free((char *) sp);
		pp->pt_sdata = (char *) 0;
	}
}

/*
**	tet_ss_newptab() - server-specific new ptab entry handler
*/

void tet_ss_newptab(pp)
struct ptab *pp;
{
	/* add the entry to the process table */
	tet_ptadd(pp);
}

