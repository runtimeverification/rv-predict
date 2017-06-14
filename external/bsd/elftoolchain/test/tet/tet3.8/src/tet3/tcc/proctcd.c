/*
 *	SCCS: @(#)proctcd.c	1.11 (05/12/07)
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1996 X/Open Company Limited
 * (C) Copyright 2005 The Open Group
 *
 * All rights reserved.  No part of this source code may be reproduced,
 * stored in a retrieval system, or transmitted, in any form or by any
 * means, electronic, mechanical, photocopying, recording or otherwise,
 * except as stated in the end-user licence agreement, without the prior
 * permission of the copyright owners.
 * A copy of the end-user licence agreement is contained in the file
 * Licence which accompanies this distribution.
 * 
 * X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
 * the UK and other countries.
 */

#ifndef lint
static char sccsid[] = "@(#)proctcd.c	1.11 (05/12/07) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)proctcd.c	1.11 05/12/07 TETware release 3.8
NAME:		proctcd.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	testcase processing support functions called from proctc.c

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., March 2000
	In run_child_proctabs(), only propagate pr_tcstate and pr_currmode
	to child proctabs when proctab contains a test case.
	Per-system configurations are now private to config.c rather
	than being available in the systab.

	Geoff Clare, The Open Group, June 2005
	Added support for full timestamps.

	Neil Moses, The Open Group, December 2005
	In setup_child_proctabs() need to check whether the child proctab
	system id is in the list of reconnectable systems. If it is then
	set the PRF_RECONNECT flag.

************************************************************************/

#ifndef TET_LITE	/* -START-LITE-CUT- */

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <time.h>
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "error.h"
#include "ltoa.h"
#include "servlib.h"
#include "config.h"
#include "scentab.h"
#include "proctab.h"
#include "systab.h"
#include "tcc.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* static function declarations */
static int conf1tccd PROTOLIST((struct proctab *, int, int));
static void rm_snid PROTOLIST((struct proctab *));


/*
**	get_snid_xrid() - get a snid and an xrid for this proctab
**
**	return 0 if successful or -1 on error
*/

int get_snid_xrid(prp)
register struct proctab *prp;
{
	char *fname;
	int full_time_flag;

	TRACE2(tet_Ttcc, 6, "get_snid_xrid(%s)", tet_i2x(prp));

	/* get a sync ID from SYNCD */
	if ((prp->pr_snid = tet_sdsnget()) < 0L) {
		prperror(prp, -1, tet_sderrno, "can't get sync ID", (char *) 0);
		return(-1);
	}

	/* assign the system name list to it */
	if (tet_sdsnsys(prp->pr_snid, prp->pr_sys, prp->pr_nsys) < 0) {
		prperror(prp, -1, tet_sderrno,
			"can't send system list to SYNCD", (char *) 0);
		rm_snid(prp);
		return(-1);
	}

	/* generate a unique xres file name */
	if ((fname = jnl_tfname(resdirname(), "xr")) == (char *) 0) {
		rm_snid(prp);
		return(-1);
	}

	/* determine whether XRESD should generate full timestamps */
	full_time_flag = getmcflag("TET_FULL_TIMESTAMPS", prp->pr_currmode);

	/* tell XRESD to open the file */
	if ((prp->pr_xrid = tet_xdxropen(fname, full_time_flag)) < 0L) {
		prperror(prp, -1, tet_xderrno, "XRESD can't open", fname);
		rm_snid(prp);
		return(-1);
	}

	/* assign the system list to the xres file */
	if (tet_xdxrsys(prp->pr_xrid, prp->pr_sys, prp->pr_nsys) < 0) {
		prperror(prp, -1, tet_xderrno,
			"can't send system list to XRESD", (char *) 0);
		rm_snid_xrid(prp);
		(void) UNLINK(fname);
		return(-1);
	}

	/* all OK so store the xres file name and return */
	prp->pr_xfname = rstrstore(fname);
	TRACE5(tet_Ttcc, 6, "get_snid_xrid(%s) successful RETURN: snid = %s, xrid = %s, xfname = %s",
		tet_i2x(prp), tet_l2a(prp->pr_snid), tet_l2a(prp->pr_xrid),
		prp->pr_xfname);
	return(0);
}

/*
**	rm_snid_xrid() - remove a sync ID, close an XRES file
*/

void rm_snid_xrid(prp)
register struct proctab *prp;
{
	TRACE4(tet_Ttcc, 6, "rm_snid_xrid(%s): snid = %s, xrid = %s",
		tet_i2x(prp), tet_i2a(prp->pr_snid), tet_i2a(prp->pr_xrid));

	/* remove the sync ID */
	rm_snid(prp);

	/* close the XRES file */
	if (prp->pr_xrid > 0L && tet_xdxrclose(prp->pr_xrid) < 0)
		prperror(prp, -1, tet_xderrno, "tet_xdxrclose() failed on",
			prp->pr_xfname);
	prp->pr_xrid = -1L;
}

/*
**	rm_snid() - remove a sync ID
*/

static void rm_snid(prp)
register struct proctab *prp;
{
	if (prp->pr_snid > 0L && tet_sdsnrm(prp->pr_snid) < 0)
		prperror(prp, -1, tet_sderrno,
			"tet_sdsnrm() failed for sync ID",
			tet_l2a(prp->pr_snid));
	prp->pr_snid = -1L;
}

/*
**	setup_child_proctabs() - allocate a set of child proctabs for use
**		when processing a remote or distributed test case which
**		is to run on more than one system
**
**	note that these child proctabs never go on the runq
*/

void setup_child_proctabs(prp)
register struct proctab *prp;
{
	register int i;
	register struct proctab *child;
	register int *ip;
	struct proctab *lback = (struct proctab *) 0;

	ASSERT(prp->pr_nsys > 1);

	for (ip = prp->pr_sys; ip < prp->pr_sys + prp->pr_nsys; ip++) {
		child = pralloc();
		child->pr_parent = prp;
		child->pr_scen = prp->pr_scen;
		child->pr_exiclist = prp->pr_exiclist;
		child->pr_context = prp->pr_context;
		child->pr_sys = ip;
		child->pr_nsys = 1;
		if (lback)
			lback->pr_lforw = child;
		else
			prp->pr_child = child;
		lback = child;
		child->pr_flags |= PRF_TC_CHILD;
		/* Check if the child can reconnect */
		for (i = 0; i < prp->pr_nrecon; i++)
			if (*ip == prp->pr_recon[i])
				child->pr_flags |= PRF_RECONNECT;

	}
}

/*
**	run_child_proctabs() - invoke the named function on each of
**		the child protcabs in turn
**
**	return 0 if successful or -1 if at least one function failed
*/

int run_child_proctabs(prp, func)
register struct proctab *prp;
int (*func) PROTOLIST((struct proctab *));
{
	register struct proctab *child;
	register int rc = 0;

	ASSERT(prp->pr_magic == PR_MAGIC);
	ASSERT((prp->pr_flags & PRF_TC_CHILD) == 0);

	for (child = prp->pr_child; child; child = child->pr_lforw) {
		ASSERT(child->pr_magic == PR_MAGIC);
		if (prp->pr_scen->sc_type == SC_TESTCASE) {
			child->pr_tcstate = prp->pr_tcstate;
			child->pr_currmode = prp->pr_currmode;
		}
		if ((*func)(child) < 0)
			rc = -1;
	}

	return(rc);
}

/*
**	child_proctabs_tstate() - return the number of this proctab's
**		children whose toolstate is in the specified state
*/

int child_proctabs_tstate(prp, state)
struct proctab *prp;
register int state;
{
	register struct proctab *child;
	register int count = 0;

	for (child = prp->pr_child; child; child = child->pr_lforw) {
		ASSERT(child->pr_magic == PR_MAGIC);
		if (child->pr_toolstate == state)
			count++;
	}

	return(count);
}

/*
**	unlink_xres() - unlink the xres file if nexessary
*/

void unlink_xres(prp)
struct proctab *prp;
{
	if (prp->pr_xfname) {
		(void) UNLINK(prp->pr_xfname);
		TRACE2(tet_Tbuf, 6, "free prp->pr_xfname = %s",
			tet_i2x(prp->pr_xfname));
		free(prp->pr_xfname);
		prp->pr_xfname = (char *) 0;
	}
}

/*
**	configure_tccd() - configure all the TCCDs for the current
**		mode of operation
**
**	return 0 if successful or -1 on error
*/

int configure_tccd(prp)
register struct proctab *prp;
{
	register int *ip;

	for (ip = prp->pr_sys; ip < prp->pr_sys + prp->pr_nsys; ip++)
		if (conf1tccd(prp, *ip, prp->pr_currmode) < 0)
			return(-1);

	return(0);
}

/*
**	conf1tccd() - configure a single TCCDs for a particular mode
**		of operation
**
**	return 0 if successful or -1 on error
*/

static int conf1tccd(prp, sysid, opmode)
struct proctab *prp;
int sysid, opmode;
{
	register struct systab *sp;
	register int cfmode, tc_cfmode;
	struct cflist *lp;

	/*
	** determine the config mode and TCCD config mode from the
	** current mode of operation
	*/
	cfmode = tcc2cfmode(opmode);
	switch (opmode) {
	case TCC_BUILD:
		tc_cfmode = TC_CONF_BUILD;
		break;
	case TCC_EXEC:
		tc_cfmode = TC_CONF_EXEC;
		break;
	case TCC_CLEAN:
		tc_cfmode = TC_CONF_CLEAN;
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected mode", prtccmode(opmode));
		/* NOTREACHED */
		return(-1);
	}

	/* find the systab entry for this system */
	sp = syfind(sysid);
	ASSERT(sp);

	/*
	** configure the TCCD for the required mode if we haven't already
	** done so
	*/
	if ((sp->sy_cfmodes & opmode) == 0) {
		lp = per_system_config(sysid, cfmode);
		if (tet_tcconfigv(sysid, lp->cf_conf, lp->cf_nconf, tc_cfmode) < 0) {
			prperror(prp, sysid, tet_tcerrno,
				"can't assign config lines to TCCD for mode",
				tet_i2a(cfmode));
			return(-1);
		}
		sp->sy_cfmodes |= opmode;
		sp->sy_currcfmode = -1;
	}

	/* set TCCD's current configuration mode if necessary */
	if (sp->sy_currcfmode != tc_cfmode) {
		if (tet_tcsetconf(sysid, tc_cfmode) < 0) {
			prperror(prp, sysid, tet_tcerrno,
				"can't set TCCD config mode", (char *) 0);
			return(-1);
		}
		sp->sy_currcfmode = tc_cfmode;
	}

	/* all OK so return success */
	return(0);
}

#else	/* -END-LITE-CUT- */

int tet_proctcd_c_not_used;

#endif /* !TET_LITE */	/* -LITE-CUT-LINE- */

