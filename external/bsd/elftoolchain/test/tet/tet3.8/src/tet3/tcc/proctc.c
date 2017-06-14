/*
 *	SCCS: @(#)proctc.c	1.14 (03/03/26)
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1996 X/Open Company Limited
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
static char sccsid[] = "@(#)proctc.c	1.14 (03/03/26) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)proctc.c	1.14 03/03/26 TETware release 3.8
NAME:		proctc.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	testcase processing functions used by the execution engine

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., March 1997
	When TET_EXEC_IN_PLACE is false and there is an alternate
	execution directory, copy files from the alt-exec-dir instead
	of from the test case source directory.

	Andrew Dingwall, UniSoft Ltd., December 1997
	Replaced SCF_DIST scenario flag with pr_distflag proctab flag.

	Andrew Dingwall, UniSoft Ltd., March 1998
	Skip exec mode processing if the lock stage fails in build mode.
	Set pr_jnlstatus after each failure that skips to the end of the
	current mode.

	Andrew Dingwall, UniSoft Ltd., March 2000
	Reset toolstate to PTS_IDLE at the start of each testcase
	processing cycle.

	Andrew Dingwall, The Open Group, March 2003
	Enhancement to copy test case source files to remote systems.


************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <sys/types.h>
#include <errno.h>
#include "dtmac.h"
#include "error.h"
#include "scentab.h"
#include "proctab.h"
#include "tcc.h"
#include "tet_jrnl.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* static function declarations */
static int nexttcstate PROTOLIST((struct proctab *, int));
static int tcs1_bec PROTOLIST((struct proctab *));
static int tcs1_buildfail PROTOLIST((struct proctab *));
static int tcs1_copy PROTOLIST((struct proctab *));
static int tcs1_cp2 PROTOLIST((struct proctab *));
static int tcs1_freetcedir PROTOLIST((struct proctab *));
static int tcs1_freetetxres PROTOLIST((struct proctab *));
static int tcs1_jnlend PROTOLIST((struct proctab *));
static int tcs1_jnlstart PROTOLIST((struct proctab *));
static int tcs1_lock PROTOLIST((struct proctab *));
static int tcs1_lk2 PROTOLIST((struct proctab *));
static int tcs1_rmtmpdir PROTOLIST((struct proctab *));
static int tcs1_save PROTOLIST((struct proctab *));
static int tcs1_unlock PROTOLIST((struct proctab *));
static void tcs_bec PROTOLIST((struct proctab *));
static void tcs_buildfail PROTOLIST((struct proctab *));
static void tcs_copy PROTOLIST((struct proctab *));
static void tcs_end PROTOLIST((struct proctab *));
static void tcs_endproc PROTOLIST((struct proctab *));
static void tcs_journal PROTOLIST((struct proctab *));
static void tcs_lock PROTOLIST((struct proctab *));
static void tcs_prebuild PROTOLIST((struct proctab *));
static int tcs_rc2 PROTOLIST((struct proctab *));
static void tcs_remcopy PROTOLIST((struct proctab *));
static void tcs_save PROTOLIST((struct proctab *));
static void tcs_start PROTOLIST((struct proctab *));
static int tcs_startproc PROTOLIST((struct proctab *));
static void tcs_unlock PROTOLIST((struct proctab *));


/*
**	proc_testcase() - process a test case on the run queue whose
**		state is PRS_PROCESS
*/

void proc_testcase(prp)
register struct proctab *prp;
{
	TRACE5(tet_Texec, 4,
		"proc_testcase(%s): tcname = %s, ref = %s, currmode = %s",
		tet_i2x(prp), prp->pr_scen->sc_tcname,
		tet_l2a(prp->pr_scen->sc_ref), prtccmode(prp->pr_currmode));

#ifndef NOTRACE
	if ((tcc_modes & TCC_RESUME) && !resume_found) {
		TRACE1(tet_Texec, 4, "RESUME point not yet found");
	}
#endif

	/* step on to the next processing mode first time through */
	switch (prp->pr_currmode) {
	case TCC_START:
		prp->pr_currmode = nextmode(prp->pr_modes, prp->pr_currmode);
		prp->pr_tcstate = TCS_START;
		break;
	case TCC_BUILD:
	case TCC_EXEC:
	case TCC_CLEAN:
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected mode", prtccmode(prp->pr_currmode));
		/* NOTREACHED */
	}

	TRACE3(tet_Texec, 4,
		"after initial switch, currmode = %s, tcstate = %s",
		prtccmode(prp->pr_currmode), prtcstate(prp->pr_tcstate));

	/*
	** if this is the first time through in this processing mode:
	**
	**	install the default EXEC IC list in EXEC mode
	**
	**	in RESUME mode, see if we should switch on test case
	**	processing
	**
	**	return now if we don't want to process this testcase
	*/
	if (prp->pr_tcstate == TCS_START) {
		if (prp->pr_currmode == TCC_EXEC)
			prp->pr_exiclist = prp->pr_scen->sc_exiclist;
		if (
			(tcc_modes & TCC_RESUME) &&
			!resume_found &&
			is_resume_point(prp)
		) {
			TRACE1(tet_Texec, 4, "found RESUME point");
			resume_found = 1;
			if (resume_iclist && prp->pr_currmode == TCC_EXEC)
				prp->pr_exiclist = resume_iclist;
		}
		if ((tcc_modes & TCC_RESUME) && !resume_found) {
			TRACE1(tet_Texec, 4, "proc_testcase() RETURN without processing this test case");
			prp->pr_currmode = nextmode(prp->pr_modes, prp->pr_currmode);
			prp->pr_state = PRS_NEXT;
			prp->pr_flags |= PRF_ATTENTION;
			return;
		}
	}

	/*
	** if currmode is END, this means that abort has been called
	** just as we were about to start processing this testcase
	*/
	switch (prp->pr_currmode) {
	case TCC_BUILD:
	case TCC_EXEC:
	case TCC_CLEAN:
		break;
	case TCC_END:
		ASSERT(tcc_modes & TCC_ABORT);
		prp->pr_state = PRS_NEXT;
		prp->pr_flags |= PRF_ATTENTION;
		return;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected mode", prtccmode(prp->pr_currmode));
		/* NOTREACHED */
	}

	/* perform the next action for the current processing mode */
	switch (prp->pr_tcstate) {
	case TCS_START:
		tcs_start(prp);
		break;
	case TCS_LOCK:
		tcs_lock(prp);
		break;
	case TCS_UNLOCK:
		tcs_unlock(prp);
		break;
	case TCS_COPY:
		tcs_copy(prp);
		break;
	case TCS_PREBUILD:
		tcs_prebuild(prp);
		break;
	case TCS_REMCOPY:
		tcs_remcopy(prp);
		break;
	case TCS_BUILDFAIL:
		tcs_buildfail(prp);
		break;
	case TCS_BUILD:
	case TCS_EXEC:
	case TCS_CLEAN:
		tcs_bec(prp);
		break;
	case TCS_JOURNAL:
		tcs_journal(prp);
		break;
	case TCS_SAVE:
		tcs_save(prp);
		break;
	case TCS_END:
		tcs_end(prp);
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected state", prtcstate(prp->pr_tcstate));
		/* NOTREACHED */
	}

	TRACE6(tet_Texec, 4, "proc_testcase(%s) RETURN: state = %s, flags = %s, currmode = %s, tcstate = %s",
		tet_i2x(prp), prpstate(prp->pr_state), prpflags(prp->pr_flags),
		prtccmode(prp->pr_currmode), prtcstate(prp->pr_tcstate));
}

/*
**	proc_tcwait() - process a test case on the run queue whose
**		state is PRS_WAIT
*/

void proc_tcwait(prp)
register struct proctab *prp;
{
#ifndef TET_LITE	/* -START-LITE-CUT- */
	register struct proctab *child;
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
	int rc;

	TRACE2(tet_Texec, 6, "proc_tcwait(%s)", tet_i2x(prp));

	/*
	** wait for each currently running tool -
	** return if there is at least one tool still running
	*/
#ifndef TET_LITE	/* -START-LITE-CUT- */
	if (prp->pr_child) {
		rc = run_child_proctabs(prp, toolwait);
		if (child_proctabs_tstate(prp, PTS_EXITED) +
			child_proctabs_tstate(prp, PTS_IDLE) < prp->pr_nsys) {
				prp->pr_state = PRS_WAIT;
				return;
		}
	}
	else
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
	{
		rc = toolwait(prp);
		if (prp->pr_toolstate != PTS_EXITED &&
			prp->pr_toolstate != PTS_IDLE) {
				prp->pr_state = PRS_WAIT;
				return;
		}
	}

#ifndef TET_LITE	/* -START-LITE-CUT- */
	/*
	** here when all tools have exited -
	** if there is a single journal and child proctabs, gather each
	** child proctab journal status into the journal status at this level
	*/
	if ((prp->pr_flags & PRF_JNL_CHILD) == 0 && prp->pr_child) {
		prp->pr_jnlstatus = 0;
		for (child = prp->pr_child; child; child = child->pr_lforw)
			if (!prp->pr_jnlstatus && child->pr_jnlstatus) {
				prp->pr_jnlstatus = child->pr_jnlstatus;
				break;
			}
	}
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	/*
	** for a PREBUILD tool with non-zero exit code, arrange to
	** skip an exec stage
	**
	** for a BUILD, EXEC or CLEAN tool, let the JOURNAL stage drive
	** the state machine - for other tools, use the exit code to drive
	** the state machine
	*/
	switch (prp->pr_tcstate) {
	case TCS_PREBUILD:
		if (rc < 0)
			prp->pr_scen->sc_flags |= SCF_SKIP_EXEC;
		break;
	case TCS_BUILD:
	case TCS_EXEC:
	case TCS_CLEAN:
		rc = 0;
		break;
	}

	/* step on to the next TC state and return */
	prp->pr_tcstate = nexttcstate(prp, rc);
	prp->pr_state = PRS_PROCESS;
	prp->pr_flags |= PRF_ATTENTION;
}

/*
**	tcs_start() - actions to be performed at the start of processing
**		a test case in the current mode of operation
*/

static void tcs_start(prp)
register struct proctab *prp;
{
	int rc;

	TRACE3(tet_Texec, 6, "tcs_start(%s), skipflags = %s", tet_i2x(prp),
		prscflags(prp->pr_scen->sc_flags & SCF_SKIP_ALL));

	/* perform start processing for a non-skipped testcase */
	switch (prp->pr_currmode) {
	case TCC_BUILD:
		if ((prp->pr_scen->sc_flags & SCF_SKIP_BUILD) == 0)
			rc = tcs_startproc(prp);
		else
			rc = -1;
		break;
	case TCC_EXEC:
		if ((prp->pr_scen->sc_flags & SCF_SKIP_EXEC) == 0)
			rc = tcs_startproc(prp);
		else
			rc = -1;
		break;
	case TCC_CLEAN:
		if ((prp->pr_scen->sc_flags & SCF_SKIP_CLEAN) == 0)
			rc = tcs_startproc(prp);
		else
			rc = -1;
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected mode", prtccmode(prp->pr_currmode));
		/* NOTREACHED */
		return;
	}

	prp->pr_tcstate = nexttcstate(prp, rc);
	prp->pr_toolstate = PTS_IDLE;
	prp->pr_state = PRS_PROCESS;
	prp->pr_flags |= PRF_ATTENTION;
}

/*
**	tcs_startproc() - actions to be performed at the start of
**		processing a test case in the current mode of operation
**		when this processing mode is not to be skipped
**
**	return 0 if successful or -1 on error
*/

static int tcs_startproc(prp)
register struct proctab *prp;
{
#ifndef TET_LITE	/* -START-LITE-CUT- */
	register struct proctab *child;
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
	int rc;

	TRACE2(tet_Texec, 6, "tcs_startproc(%s)", tet_i2x(prp));

#ifndef TET_LITE	/* -START-LITE-CUT- */
	/*
	** if there is more than one system,
	** allocate child proctabs for each system
	*/
	if (prp->pr_nsys > 1)
		setup_child_proctabs(prp);
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	/*
	** if there is only one system or this is a distributed,
	** API-conforming testcase, there is only one journal:
	**	use the journal at this level and get a snid and xrid
	** otherwise there is a journal for each system:
	**	for each child, set up a tmp journal file and get a
	**	snid and xrid
	*/
#ifndef TET_LITE	/* -START-LITE-CUT- */
	if (prp->pr_child == (struct proctab *) 0 ||
		(
			prp->pr_distflag &&
			getmcflag("TET_API_COMPLIANT", prp->pr_currmode)
		)
	) {
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
		rc = tcs1_jnlstart(prp);
#ifndef TET_LITE	/* -START-LITE-CUT- */
		for (child = prp->pr_child; child; child = child->pr_lforw) {
			child->pr_activity = prp->pr_activity;
			child->pr_snid = prp->pr_snid;
			child->pr_xrid = prp->pr_xrid;
		}
		prp->pr_flags &= ~PRF_JNL_CHILD;
	}
	else {
		rc = (run_child_proctabs(prp, jnl_tmpfile) < 0 ||
			run_child_proctabs(prp, tcs1_jnlstart) < 0) ? -1 : 0;
		prp->pr_flags |= PRF_JNL_CHILD;
	}
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	/* if all OK so far ... */
	if (!rc) {
#ifdef TET_LITE	/* -LITE-CUT-LINE- */
		/* ... put TET_CONFIG in the environment if necessary */
		rc = tet_config_putenv(prp->pr_currmode);
#else	/* -START-LITE-CUT- */
		/* ... re-configure the TCCDs if necessary */
		rc = configure_tccd(prp);
#endif /* TET_LITE */	/* -END-LITE-CUT- */
	}

	return(rc);
}

/*
**	tcs1_jnlstart() - start a single journal and obtain a snid and
**		xrid for use by a single non-distributed test case or by
**		all the parts of a distributed test case
**
**	return 0 if successful or -1 on error
*/

static int tcs1_jnlstart(prp)
struct proctab *prp;
{
	static int activity;
	char *action, *timestr;
	struct scentab *ep;
#ifndef TET_LITE	/* -START-LITE-CUT- */
	int *ip;
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	TRACE3(tet_Texec, 6, "tcs1_jnlstart(%s), flags = %s",
		tet_i2x(prp), prpflags(prp->pr_flags));

	ASSERT_LITE(*prp->pr_sys == 0);

	/* emit the progress message if so required */
	if (report_progress) {
		switch (prp->pr_currmode) {
		case TCC_BUILD:
			action = "Build";
			break;
		case TCC_EXEC:
			action = "Execute";
			break;
		case TCC_CLEAN:
			action = "Clean";
			break;
		default:
			/* this "can't happen" */
			fatal(0, "unexpected mode",
				prtccmode(prp->pr_currmode));
			/* NOTREACHED */
			return(-1);
		}
		timestr = jnl_time(time((time_t *) 0));
		ep = prp->pr_scen;
#ifdef TET_LITE	/* -LITE-CUT-LINE- */
		(void) printf("%s  %-7s %s\n", timestr, action, ep->sc_tcname);
#else	/* -START-LITE-CUT- */
		for (ip = prp->pr_sys; ip < prp->pr_sys + prp->pr_nsys; ip++)
			(void) printf("%s  %-7s %s on system %03d\n",
				timestr, action, ep->sc_tcname, *ip);
#endif /* TET_LITE */	/* -END-LITE-CUT- */
		(void) fflush(stdout);
	}

	/* start a new activity */
	prp->pr_activity = activity++;

	/* emit the start message to the journal */
	switch (prp->pr_currmode) {
	case TCC_BUILD:
		jnl_build_start(prp);
		break;
	case TCC_EXEC:
		jnl_tc_start(prp);
		break;
	case TCC_CLEAN:
		jnl_clean_start(prp);
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected mode", prtccmode(prp->pr_currmode));
		/* NOTREACHED */
	}

	/* set a default journal exit status */
	prp->pr_jnlstatus = TET_ESTAT_ERROR;

#ifdef TET_LITE	/* -LITE-CUT-LINE- */
	return(0);
#else	/* -START-LITE-CUT- */
	/* get the snid and xrid */
	return(get_snid_xrid(prp));
#endif /* TET_LITE */	/* -END-LITE-CUT- */
}

/*
**	tcs_lock() - lock a test case
*/

static void tcs_lock(prp)
register struct proctab *prp;
{
	int rc;

	TRACE2(tet_Texec, 6, "tcs_lock(%s)", tet_i2x(prp));

	/*
	** lock this test case on each system
	**
	** if the operation fails in build mode, arrange to skip the
	** EXEC stage of processing
	*/
	if ((rc = RUN_PROCTABS(prp, tcs1_lock)) < 0) {
		prp->pr_jnlstatus = TET_ESTAT_LOCK;
		if (prp->pr_currmode == TCC_BUILD)
			prp->pr_scen->sc_flags |= SCF_SKIP_EXEC;
	}
	prp->pr_tcstate = nexttcstate(prp, rc);
	prp->pr_state = PRS_PROCESS;
	prp->pr_flags |= PRF_ATTENTION;
}

/*
**	tcs1_lock() - lock a single test case or test case part
**
**	return 0 if successful or -1 on error
*/

static int tcs1_lock(prp)
register struct proctab *prp;
{
	register int rc;

	TRACE4(tet_Texec, 6, "tcs1_lock(%s), sysid = %s, flags = %s",
		tet_i2x(prp), tet_i2a(*prp->pr_sys), prpflags(prp->pr_flags));

	ASSERT(prp->pr_nsys == 1);
	ASSERT_LITE(*prp->pr_sys == 0);

	if ((rc = tcs1_lk2(prp)) < 0)
		prp->pr_jnlstatus = TET_ESTAT_LOCK;

	return(rc);
}

/*
**	tcs1_lk2() - extend the tcs1_lock() processing
**
**	return 0 if successful or -1 on error
*/

static int tcs1_lk2(prp)
register struct proctab *prp;
{
	char lkdir[MAXPATH], lkname[MAXPATH];
	char *altexecdir;
	int shared;

	/* see if we should create shared or exclusive locks */
	switch (prp->pr_currmode) {
	case TCC_EXEC:
		if (!getmcflag("TET_EXEC_IN_PLACE", prp->pr_currmode)) {
			shared = 1;
			prp->pr_flags |= PRF_SHLOCK;
			break;
		}
		/* else fall through */
	default:
		shared = 0;
		prp->pr_flags &= ~PRF_SHLOCK;
		break;
	}

	/* get the value of TET_EXECUTE for this system */
	altexecdir = getdcfg("TET_EXECUTE", *prp->pr_sys);
	if (altexecdir && !*altexecdir)
		altexecdir = (char *) 0;

	/*
	** get the source directory lock:
	**
	**	in BUILD and CLEAN mode: always
	**	in EXEC mode: only if TET_EXECUTE is not set
	*/
	if (prp->pr_currmode != TCC_EXEC || !altexecdir) {
		tcsrcdir(prp, lkdir, sizeof lkdir);
		if (tcc_lock(prp, shared, lkdir, lkname, sizeof lkname) < 0)
			return(-1);
		else if (lkname[0]) {
			TRACE3(tet_Ttcc, 6,
				"%s source directory lock name = %s",
				shared ? "shared" : "exclusive", lkname);
			prp->pr_srclock = rstrstore(lkname);
		}
	}

	/* return now if we don't want an exec directory lock */
	if (!altexecdir)
		return(0);

	/* get the execution directory lock */
	tcexecdir(prp, altexecdir, lkdir, sizeof lkdir);
	if (tcc_lock(prp, shared, lkdir, lkname, sizeof lkname) < 0)
		return(-1);
	else if (lkname[0]) {
		TRACE3(tet_Ttcc, 6, "%s execution directory lock name = %s",
			shared ? "shared" : "exclusive", lkname);
		prp->pr_execlock = rstrstore(lkname);
	}

	/* all OK so return success */
	return(0);
}

/*
**	tcs_unlock() - unlock a test case
*/

static void tcs_unlock(prp)
register struct proctab *prp;
{
	int rc;

	TRACE2(tet_Texec, 6, "tcs_unlock(%s)", tet_i2x(prp));

	/* unlock the test case on each system */
	rc = RUN_PROCTABS(prp, tcs1_unlock);
	prp->pr_tcstate = nexttcstate(prp, rc);
	prp->pr_state = PRS_PROCESS;
	prp->pr_flags |= PRF_ATTENTION;
}

/*
**	tcs1_unlock() - unlock a single test case or test case part
**
**	return 0 if successful or -1 on error
**
**	note that this function can be called when the test case
**	in not actually locked
*/

static int tcs1_unlock(prp)
register struct proctab *prp;
{
	TRACE4(tet_Texec, 6, "tcs1_unlock(%s), sysid = %s, flags = %s",
		tet_i2x(prp), tet_i2a(*prp->pr_sys), prpflags(prp->pr_flags));

	ASSERT(prp->pr_nsys == 1);
	ASSERT_LITE(*prp->pr_sys == 0);

	/* unlock the execution directory */
	if (prp->pr_execlock) {
		(void) tcc_unlock(prp, prp->pr_flags & PRF_SHLOCK,
			prp->pr_execlock);
		TRACE2(tet_Tbuf, 6, "free pr_execlock = %s",
			tet_i2x(prp->pr_execlock));
		free(prp->pr_execlock);
		prp->pr_execlock = (char *) 0;
	}

	/* unlock the source directory */
	if (prp->pr_srclock) {
		(void) tcc_unlock(prp, prp->pr_flags & PRF_SHLOCK,
			prp->pr_srclock);
		TRACE2(tet_Tbuf, 6, "free pr_srclock = %s",
			tet_i2x(prp->pr_srclock));
		free(prp->pr_srclock);
		prp->pr_srclock = (char *) 0;
	}

	prp->pr_flags &= ~PRF_SHLOCK;
	return(0);
}

/*
**	tcs_copy() - copy the contents of the test case directory to the
**		temporary execution directory on each system
*/

static void tcs_copy(prp)
register struct proctab *prp;
{
	int rc = 0;

	TRACE2(tet_Texec, 6, "tcs_copy(%s)", tet_i2x(prp));

	/* copy this test case to the temporary directory on each system */
	if ((rc = RUN_PROCTABS(prp, tcs1_copy)) < 0)
		prp->pr_jnlstatus = TET_ESTAT_ERROR;
	prp->pr_tcstate = nexttcstate(prp, rc);
	prp->pr_state = PRS_PROCESS;
	prp->pr_flags |= PRF_ATTENTION;
}

/*
**	tcs1_copy() - copy the contents of a single test case directory to
**		the temporary execution directory on a particular system
**
**	return 0 if successful or -1 on error
*/

static int tcs1_copy(prp)
register struct proctab *prp;
{
	register int rc;

	TRACE4(tet_Texec, 6, "tcs1_copy(%s), sysid = %s, flags = %s",
		tet_i2x(prp), tet_i2a(*prp->pr_sys), prpflags(prp->pr_flags));

	ASSERT(prp->pr_nsys == 1);
	ASSERT_LITE(*prp->pr_sys == 0);

	if ((rc = tcs1_cp2(prp)) < 0)
		prp->pr_jnlstatus = TET_ESTAT_ERROR;

	return(rc);
}

/*
**	tcs1_cp2() - extend the tcs1_copy() processing
**
**	return 0 if successful or -1 on error
*/

static int tcs1_cp2(prp)
register struct proctab *prp;
{
	char fromdir[MAXPATH], todir[MAXPATH]; 
	char *altexecdir, *tmproot, *tmpdir;

	/*
	** determine the name of the directory from which the files are
	** to be copied
	*/
	if ((altexecdir = getdcfg("TET_EXECUTE", *prp->pr_sys)) == (char *) 0)
		tcsrcdir(prp, fromdir, sizeof fromdir);
	else
		tcexecdir(prp, altexecdir, fromdir, sizeof fromdir);

	/* create a temporary directory below TET_TMP_DIR */
	tmproot = getdcfg("TET_TMP_DIR", *prp->pr_sys);
	ASSERT(tmproot && *tmproot);
	if (tcc_mktmpdir(prp, tmproot, &tmpdir) < 0)
		return(-1);

	/* remember the directory name so that it can be removed later */
	prp->pr_tmpdir = rstrstore(tmpdir);

	/* determine the destination name and create it */
	tcexecdir(prp, prp->pr_tmpdir, todir, sizeof todir);
	if (tcc_mkalldirs(prp, todir) < 0)
		return(-1);

	/* now do the copy */
	if (tccopy(prp, fromdir, todir) < 0)
		return(-1);

	return(0);
}

/*
**	tcs_prebuild() - execute the prebuild tool
*/

static void tcs_prebuild(prp)
register struct proctab *prp;
{
	struct proctab *q;
	char tcname[MAXPATH];
	char ocfname[MAXPATH];
	char **argv;
	int rc;

	TRACE2(tet_Texec, 6, "tcs_prebuild(%s)", tet_i2x(prp));

	/*
	** prepare to exec the prebuild tool
	** we only do this on the first (i.e., "master") system in the list;
	** if there are child proctabs we use the first one and mark
	** the rest as IDLE with success exit codes
	**
	** it's OK if the prebuild tool is not defined so if toolprep()
	** fails we simply move on to the next TC state
	*/
#ifndef TET_LITE	/* -START-LITE-CUT- */
	if (prp->pr_child) {
		for (q = prp->pr_child->pr_lforw; q; q = q->pr_lforw) {
			q->pr_toolstate = PTS_IDLE;
			q->pr_exitcode = 0;
		}
		q = prp->pr_child;
		q->pr_tcstate = prp->pr_tcstate;
	}
	else
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
		q = prp;
	if ((argv = toolprep(q, tcname, sizeof tcname)) == (char **) 0) {
		prp->pr_tcstate = nexttcstate(prp, 0);
		prp->pr_state = PRS_PROCESS;
		prp->pr_flags |= PRF_ATTENTION;
		return;
	}

	/* get the output capture file name */
	ocfilename(tcname, ocfname, sizeof ocfname);

	/* execute the tool */
	if ((rc = toolexec(q, tcname, argv, ocfname)) < 0) {
#ifndef TET_LITE	/* -START-LITE-CUT- */
		if (prp->pr_flags & PRF_JNL_CHILD) {
			ASSERT(prp->pr_child);
			prp->pr_child->pr_jnlstatus = TET_ESTAT_EXEC_FAILED;
		}
		else
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
			prp->pr_jnlstatus = TET_ESTAT_EXEC_FAILED;
		prp->pr_scen->sc_flags |= SCF_SKIP_EXEC;
		prp->pr_tcstate = nexttcstate(prp, rc);
		prp->pr_state = PRS_PROCESS;
		prp->pr_flags |= PRF_ATTENTION;
	}
	else {
		q->pr_outfile = rstrstore(ocfname);
		prp->pr_state = PRS_WAIT;
	}

	/* free the argv */
	toolpfree(argv);
}

/*
**	tcs_remcopy() - copy source files to remote systems
*/

static void tcs_remcopy(prp)
register struct proctab *prp;
{
	int rc = 0;

	TRACE2(tet_Texec, 6, "tcs_remcopy(%s)", tet_i2x(prp));

#ifndef TET_LITE	/* -START-LITE-CUT- */
	rc = tcs_rc2(prp);
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	prp->pr_tcstate = nexttcstate(prp, rc);
	prp->pr_state = PRS_PROCESS;
	prp->pr_flags |= PRF_ATTENTION;
}

#ifndef TET_LITE	/* -START-LITE-CUT- */

static int tcs_rc2(prp)
register struct proctab *prp;
{
	static char tsifile[] = "tet_transfer_source_files";
	static char cant_open[] = "can't open";
	int *ip;
	int rc;
	struct proctab proctmp;
	int sys0 = 0;
	char srcdir[MAXPATH];
	char tsipath[MAXPATH];
	FILE *fp;

	/*
	** see if there is at least one remote system;
	** return now if there isn't
	*/
	for (ip = prp->pr_sys; ip < prp->pr_sys + prp->pr_nsys; ip++)
		if (*ip > 0)
			break;
	if (ip >= prp->pr_sys + prp->pr_nsys)
		return(0);

	/*
	** here if there is at least one remote system -
	** see if TET_TRANSFER_SOURCE_FILES has been
	** specified;
	** return now if not
	*/
	if (!getmcflag("TET_TRANSFER_SOURCE_FILES", TCC_BUILD))
		return(0);

	/*
	** see if there is a per-testcase file in the
	** test case source directory
	*/
	proctmp = *prp;
	proctmp.pr_sys = &sys0;
	proctmp.pr_nsys = 1;
	tcsrcdir(&proctmp, srcdir, sizeof srcdir);
	fullpath(srcdir, tsifile, tsipath, sizeof tsipath, 0);
	if ((fp = fopen(tsipath, "r")) == (FILE *) 0) {
		if (errno != ENOENT) {
			error(errno, cant_open, tsipath);
			return(-1);
		}
		fullpath(tet_tsroot, tsifile, tsipath, sizeof tsipath, 0);
		if ((fp = fopen(tsipath, "r")) == (FILE *) 0) {
			if (errno != ENOENT) {
				error(errno, cant_open, tsipath);
				return(-1);
			}
		}
	}

	/* report a (contrived) pair of error messages if no
	** tet_transfer_source_files exists either at the
	** test case or the test suite level
	*/
	if (fp == (FILE *) 0) {
		fullpath(srcdir, tsifile, tsipath, sizeof tsipath, 0);
		error(ENOENT, cant_open, tsipath);
		fullpath(tet_tsroot, tsifile, tsipath, sizeof tsipath, 0);
		error(ENOENT, cant_open, tsipath);
		error(0, "no TET_TRANSFER_SOURCE_FILES instruction file,",
			"either at test case or test suite level");
		return(-1);
	}

	/* intrepret the transfer source files instruction file */
	if ((rc = copy_sfiles2rmt(prp, fp, tsipath)) < 0) {
		error(0, "transfer source files operation was unsuccessful",
			(char *) 0);
		prp->pr_jnlstatus = TET_ESTAT_ERROR;
	}

	fclose(fp);
	return(rc);
}

#endif /* !TET_LITE */	/* -END-LITE-CUT- */

/*
**	tcs_buildfail() - execute the build fail tool on each system
*/

static void tcs_buildfail(prp)
register struct proctab *prp;
{
	int rc;

	TRACE2(tet_Texec, 6, "tcs_buildfail(%s)", tet_i2x(prp));

	/*
	** execute the build fail tool on each system:
	** if there is at least one tool running, arrange to wait
	** for it; otherwise, step on to the next TC state
	*/
#ifndef TET_LITE	/* -START-LITE-CUT- */
	if (prp->pr_child) {
		rc = run_child_proctabs(prp, tcs1_buildfail);
		if (child_proctabs_tstate(prp, PTS_RUNNING) > 0) {
			prp->pr_state = PRS_WAIT;
			return;
		}
	}
	else
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
	{
		rc = tcs1_buildfail(prp);
		if (prp->pr_toolstate == PTS_RUNNING) {
			prp->pr_state = PRS_WAIT;
			return;
		}
	}

	/*
	** here if (all of the) exec(s) failed -
	** arrange to step on to the next TC state
	*/
	prp->pr_tcstate = nexttcstate(prp, rc);
	prp->pr_state = PRS_PROCESS;
	prp->pr_flags |= PRF_ATTENTION;
}

/*
**	tcs1_buildfail() - execute the build fail tool on a single system
**
**	return 0 if successful or -1 on error
*/

static int tcs1_buildfail(prp)
register struct proctab *prp;
{
	char tcname[MAXPATH];
	char ocfname[MAXPATH];
	char **argv;
	int rc;

	TRACE4(tet_Texec, 6, "tcs1_buildfail(%s), sysid = %s, flags = %s",
		tet_i2x(prp), tet_i2a(*prp->pr_sys), prpflags(prp->pr_flags));

	ASSERT(prp->pr_nsys == 1);
	ASSERT_LITE(*prp->pr_sys == 0);

	/*
	** prepare to exec the build fail tool
	** it's OK if the prebuild tool is not defined so if toolprep()
	** fails we still return success
	*/
	if ((argv = toolprep(prp, tcname, sizeof tcname)) == (char **) 0)
		return(0);

	/* get the output capture file name */
	ocfilename(tcname, ocfname, sizeof ocfname);

	/* execute the tool and free the argv */
	rc = toolexec(prp, tcname, argv, ocfname);
	toolpfree(argv);
	if (rc < 0)
		return(-1);

	/* all OK so store the output capture file name and return */
	prp->pr_outfile = rstrstore(ocfname);
	return(0);
}

/*
**	tcs_bec() - execute the build, exec or clean tool on each system
*/

static void tcs_bec(prp)
register struct proctab *prp;
{
	int rc = 0;

	TRACE3(tet_Texec, 6, "tcs_bec(%s), currmode = %s",
		tet_i2x(prp), prtccmode(prp->pr_currmode));

	/*
	** for a non-API conforming test case in EXEC mode,
	** generate a START message if the journal is at this level
	*/
	if (
#ifndef TET_LITE	/* -START-LITE-CUT- */
		(prp->pr_flags & PRF_JNL_CHILD) == 0 &&
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
		prp->pr_currmode == TCC_EXEC &&
		getmcflag("TET_API_COMPLIANT", prp->pr_currmode) == 0) {
			jnl_tcm_start(prp);
			jnl_ic_start(prp);
			jnl_tp_start(prp);
			prp->pr_flags |= PRF_AUTORESULT;
	}

	/*
	** execute the tool on each system 
	**
	** if any of the execs failed in build mode, arrange to skip 
	** the EXEC stage of processing
	*/
	prp->pr_jnlstatus = 0;
	if ((rc = RUN_PROCTABS(prp, tcs1_bec)) < 0) {
		prp->pr_jnlstatus = TET_ESTAT_EXEC_FAILED;
		if (prp->pr_currmode == TCC_BUILD)
			prp->pr_scen->sc_flags |= SCF_SKIP_EXEC;
	}

	/* if there is at least one tool running, arrange to wait for it */
	if (
		(
#ifndef TET_LITE	/* -START-LITE-CUT- */
			prp->pr_child &&
			child_proctabs_tstate(prp, PTS_RUNNING) > 0
		) || (
			!prp->pr_child &&
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
			prp->pr_toolstate == PTS_RUNNING
		)
	) {
		prp->pr_state = PRS_WAIT;
		return;
	}

	/*
	** here if (all of) the tool exec(s) failed:
	** step on to the next TC state
	*/
	prp->pr_tcstate = nexttcstate(prp, rc);
	prp->pr_state = PRS_PROCESS;
	prp->pr_flags |= PRF_ATTENTION;
}

/*
**	tcs1_bec() - execute the specified tool on a single system
**
**	return 0 if successful or -1 on error
*/

static int tcs1_bec(prp)
register struct proctab *prp;
{
	char tcname[MAXPATH];
	char buf[MAXPATH];
	char **argv, *ocfnp;
	int rc;

	TRACE5(tet_Texec, 6,
		"tcs1_bec(%s), currmode = %s, sysid = %s, flags = %s",
		tet_i2x(prp), prtccmode(prp->pr_currmode),
		tet_i2a(*prp->pr_sys), prpflags(prp->pr_flags));

	ASSERT(prp->pr_nsys == 1);
	ASSERT_LITE(*prp->pr_sys == 0);

#ifndef TET_LITE	/* -START-LITE-CUT- */
	/*
	** for a non-API conforming test case in EXEC mode,
	** generate a START message if the journal is at this level
	*/
	if ((prp->pr_flags & PRF_TC_CHILD) &&
		(prp->pr_parent->pr_flags & PRF_JNL_CHILD) &&
		prp->pr_currmode == TCC_EXEC &&
		getmcflag("TET_API_COMPLIANT", prp->pr_currmode) == 0) {
			jnl_tcm_start(prp);
			jnl_ic_start(prp);
			jnl_tp_start(prp);
			prp->pr_flags |= PRF_AUTORESULT;
	}
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	/* prepare to exec the tool */
	if ((argv = toolprep(prp, tcname, sizeof tcname)) == (char **) 0) {
		prp->pr_jnlstatus = TET_ESTAT_ERROR;
		return(-1);
	}

	/* get the output capture file name if we want one */
	if (getcflag("TET_OUTPUT_CAPTURE", *prp->pr_sys, prp->pr_currmode)) {
		ocfilename(tcname, buf, sizeof buf);
		ocfnp = buf;
	}
	else
		ocfnp = (char *) 0;

	/* execute the tool and free the argv */
	if ((rc = toolexec(prp, tcname, argv, ocfnp)) < 0)
		prp->pr_jnlstatus = TET_ESTAT_EXEC_FAILED;
	toolpfree(argv);
	if (rc < 0)
		return(-1);

	/*
	** here if the exec is successful -
	** remember the output capture file name if there is one
	*/
	if (ocfnp)
		prp->pr_outfile = rstrstore(ocfnp);

	/*
	** in exec mode when saved files processing is to be done,
	** remember the testcase execution directory name
	*/
	if (prp->pr_currmode == TCC_EXEC &&
		getcfg("TET_SAVE_FILES", *prp->pr_sys, prp->pr_currmode)) {
			tcc_dirname(tcname, buf, sizeof buf);
			prp->pr_tcedir = rstrstore(buf);
	}

	return(0);
}

/*
**	tcs_journal() - process the journal on each system
*/

static void tcs_journal(prp)
register struct proctab *prp;
{
	int rc = 0;
	int (*func) PROTOLIST((struct proctab *));

	TRACE2(tet_Texec, 6, "tcs_journal(%s)", tet_i2x(prp));

	/*
	** for an API-conforming test case:
	**	if there is a journal at this level:
	**		if the tool used XRESD:
	**			process the XRESD file;
	**		otherwise:
	**			the tool should have used tet_xres;
	**			if there are child proctabs:
	**				process each child's tet_xres file;
	**			otherwise:
	**				process this level's tet_xres file;
	**	otherwise:
	**		for each child journal:
	**			if the tool used XRESD:
	**				process the XRESD file;
	**			otherwise:
	**				the tool should have used tet_xres;
	**				process the tet_xres file;
	** otherwise:
	**	if there is a journal at this level:
	**		if PRF_AUTORESULT is set (i.e., non-API in EXEC mode):
	**			generate a TP result from pr_exitcode;
	**	otherwise:
	**		for each child journal:
	**			if PRF_AUTORESULT is set:
	**				generate a TP result from pr_exitcode;
	**
	*/
	if (getmcflag("TET_API_COMPLIANT", prp->pr_currmode))
		func = jnlproc_api;
	else
		func = jnlproc_nonapi;
#ifndef TET_LITE	/* -START-LITE-CUT- */
	if (prp->pr_flags & PRF_JNL_CHILD) {
		ASSERT(prp->pr_child);
		rc = run_child_proctabs(prp, func);
	}
	else
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
		rc = (*func)(prp);

	/*
	** in build mode, arrange to skip the exec if (any of)
	** the tools failed (ie, API-conforming TC emits at least one
	** non-pass result, or non-API-conforming TC exits with non-zero
	** status
	*/
	if (rc < 0 && prp->pr_currmode == TCC_BUILD)
		prp->pr_scen->sc_flags |= SCF_SKIP_EXEC;

	prp->pr_tcstate = nexttcstate(prp, rc);
	prp->pr_state = PRS_PROCESS;
	prp->pr_flags |= PRF_ATTENTION;
}

/*
**	tcs_save() - perform save files processing on each system
*/

static void tcs_save(prp)
register struct proctab *prp;
{
	int rc;

	TRACE2(tet_Texec, 6, "tcs_save(%s)", tet_i2x(prp));

	/* perform save files processing on each system */
	rc = RUN_PROCTABS(prp, tcs1_save);
	prp->pr_tcstate = nexttcstate(prp, rc);
	prp->pr_state = PRS_PROCESS;
	prp->pr_flags |= PRF_ATTENTION;
}

/*
**	tcs1_save() - perform save files processing on a single system
**
**	return 0 if successful or -1 on error
*/

static int tcs1_save(prp)
struct proctab *prp;
{
	register char *p;
	char *buf;
	char **sfiles = (char **) 0;
	int lsfiles = 0, nsfiles;
	int rc;

	TRACE4(tet_Texec, 6, "tcs1_save(%s), sysid = %s, flags = %s",
		tet_i2x(prp), tet_i2a(*prp->pr_sys), prpflags(prp->pr_flags));

	ASSERT(prp->pr_nsys == 1);
	ASSERT_LITE(*prp->pr_sys == 0);

	/* return now if there are no save files to process */
	p = getcfg("TET_SAVE_FILES", *prp->pr_sys, prp->pr_currmode);
	if (!p || !*p)
		return(0);

	/* generate the list of save files */
	buf = rstrstore(p);
	nsfiles = 1;
	for (p = buf; *p; p++)
		if (*p == ',')
			nsfiles++;
	RBUFCHK((char **) &sfiles, &lsfiles, (int) (nsfiles * sizeof *sfiles));
	nsfiles = split(buf, sfiles, nsfiles, ',');

	/* do the save */
	rc = sfproc(prp, sfiles, nsfiles);

	/* free the storage allocated here and return */
	TRACE2(tet_Tbuf, 6, "free tcs1_save list = %s", tet_i2x(sfiles));
	free((char *) sfiles);
	TRACE2(tet_Tbuf, 6, "free tcs1_save buf = %s", tet_i2x(buf));
	free(buf);
	return(rc);
}

/*
**	tcs_end() - perform test case end processing
*/

static void tcs_end(prp)
register struct proctab *prp;
{
	TRACE3(tet_Texec, 6, "tcs_end(%s), skipflags = %s", tet_i2x(prp),
		prscflags(prp->pr_scen->sc_flags & SCF_SKIP_ALL));

	/* perform end processing for a non-skipped testcase */
	switch (prp->pr_currmode) {
	case TCC_BUILD:
		if ((prp->pr_scen->sc_flags & SCF_SKIP_BUILD) == 0)
			tcs_endproc(prp);
		break;
	case TCC_EXEC:
		if ((prp->pr_scen->sc_flags & SCF_SKIP_EXEC) == 0)
			tcs_endproc(prp);
		break;
	case TCC_CLEAN:
		if ((prp->pr_scen->sc_flags & SCF_SKIP_CLEAN) == 0)
			tcs_endproc(prp);
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected mode", prtccmode(prp->pr_currmode));
		/* NOTREACHED */
	}

	/*
	** move on to the next processing mode -
	** if this is the last mode, proc_next() will move on to
	** the next scenario element
	*/
	prp->pr_currmode = nextmode(prp->pr_modes, prp->pr_currmode);
	prp->pr_tcstate = TCS_START;
	prp->pr_state = PRS_NEXT;
	prp->pr_flags |= PRF_ATTENTION;
}

/*
**	tcs_endproc() - perform test case end processing for a test case
**		which has not been skipped
*/

static void tcs_endproc(prp)
register struct proctab *prp;
{
	TRACE2(tet_Texec, 6, "tcs_endproc(%s)", tet_i2x(prp));

	/* perform per-proctab journal end processing */
#ifndef TET_LITE	/* -START-LITE-CUT- */
	if (prp->pr_flags & PRF_JNL_CHILD) {
		(void) run_child_proctabs(prp, tcs1_jnlend);
		jnl_consolidate(prp);
		prp->pr_flags &= ~PRF_JNL_CHILD;
	}
	else
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
		(void) tcs1_jnlend(prp);

	/* remove the temporary directory if there is one */
	(void) RUN_PROCTABS(prp, tcs1_rmtmpdir);

	/* ensure that all locks are removed */
	(void) RUN_PROCTABS(prp, tcs1_unlock);

	/* free the tet_xres file names */
	(void) RUN_PROCTABS(prp, tcs1_freetetxres);

	/* free the execution directory names */
	(void) RUN_PROCTABS(prp, tcs1_freetcedir);

#ifdef TET_LITE	/* -LITE-CUT-LINE- */
	ASSERT(prp->pr_child == (struct proctab *) 0);
#else	/* -START-LITE-CUT- */
	/* free any child proctabs */
	prcfree(prp);
#endif /* TET_LITE */	/* -END-LITE-CUT- */

}

/*
**	tcs1_jnlend() - emit a test case end message to the journal
*/

static int tcs1_jnlend(prp)
struct proctab *prp;
{
	TRACE3(tet_Texec, 6, "tcs1_jnlend(%s), flags = %s",
		tet_i2x(prp), prpflags(prp->pr_flags));

#ifndef TET_LITE	/* -START-LITE-CUT- */
	/* make sure that the xres file is closed and unlinked */
	rm_snid_xrid(prp);
	unlink_xres(prp);
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	/* emit a user abort message to the journal if so required */
	if (
		(
#ifndef TET_LITE	/* -START-LITE-CUT- */
			(prp->pr_flags & PRF_TC_CHILD) ?
			prp->pr_parent->pr_modes :
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
			prp->pr_modes
		) & TCC_ABORT
	) {
		jnl_user_abort(prp);
	}

	/* emit an end message for the current mode to the journal */
	switch (prp->pr_currmode) {
	case TCC_BUILD:
		jnl_build_end(prp);
		break;
	case TCC_EXEC:
		jnl_tc_end(prp);
		break;
	case TCC_CLEAN:
		jnl_clean_end(prp);
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected mode", prtccmode(prp->pr_currmode));
		/* NOTREACHED */
	}

	prp->pr_flags &= ~PRF_AUTORESULT;
	return(0);
}

/*
**	tcs1_rmtmpdir() - remove the temporary directory on a single system
**
**	always returns 0
*/

static int tcs1_rmtmpdir(prp)
struct proctab *prp;
{
	TRACE3(tet_Texec, 6, "tcs1_rmtmpdir(%s), flags = %s",
		tet_i2x(prp), prpflags(prp->pr_flags));

	if (prp->pr_tmpdir) {
		(void) tcc_rmtmpdir(prp, prp->pr_tmpdir);
		TRACE2(tet_Tbuf, 6, "free pr_tmpdir = %s",
			tet_i2x(prp->pr_tmpdir));
		free(prp->pr_tmpdir);
		prp->pr_tmpdir = (char *) 0;
	}

	return(0);
}

/*
**	tcs1_freetetxres() - free pr_tetxres in a single proctab
**
**	always returns 0
*/

static int tcs1_freetetxres(prp)
struct proctab *prp;
{
	TRACE3(tet_Texec, 6, "tcs1_freetetxres(%s), flags = %s",
		tet_i2x(prp), prpflags(prp->pr_flags));

	if (prp->pr_tetxres) {
		TRACE2(tet_Tbuf, 6, "free pr_tetxres = %s",
			tet_i2x(prp->pr_tetxres));
		free(prp->pr_tetxres);
		prp->pr_tetxres = (char *) 0;
	}

	return(0);
}

/*
**	tcs1_freetcedir() - free pr_tcedir in a single proctab
**
**	always returns 0
*/

static int tcs1_freetcedir(prp)
struct proctab *prp;
{
	TRACE3(tet_Texec, 6, "tcs1_freetcedir(%s), flags = %s",
		tet_i2x(prp), prpflags(prp->pr_flags));

	if (prp->pr_tcedir) {
		TRACE2(tet_Tbuf, 6, "free pr_tcedir = %s",
			tet_i2x(prp->pr_tcedir));
		free(prp->pr_tcedir);
		prp->pr_tcedir = (char *) 0;
	}

	return(0);
}

/*
**	nexttcstate() - return the next value of tc_state for the current
**		mode of operation
**
**	status should be zero if the current stage was successful
**	or non-zero otherwise
**
**	in the case of TC states PREBUILD, BUILD, BUILDFAIL, EXEC and
**	CLEAN, "successful" means that the toolexec() call was successful
**	and not that the tool returned zero (success) exit status
*/

static int nexttcstate(prp, status)
register struct proctab *prp;
register int status;
{
	static char s[] = "TET_EXEC_IN_PLACE";

	/*
	** if test case abort is required,
	** end processing now if we haven't got too far
	*/
	if (prp->pr_modes & TCC_ABORT)
		switch (prp->pr_tcstate) {
		case TCS_BUILD:
		case TCS_CLEAN:
			if (!status)
				break;
			/* else fall through */
		case TCS_START:
		case TCS_LOCK:
		case TCS_PREBUILD:
		case TCS_COPY:
		case TCS_UNLOCK:
			return(TCS_END);
		}

	/* give the state machine one turn of the handle */
	switch (prp->pr_tcstate) {
	case TCS_START:
		return(status ? TCS_END : TCS_LOCK);
	case TCS_LOCK:
		switch (prp->pr_currmode) {
		case TCC_BUILD:
			return(status ? TCS_END : TCS_REMCOPY);
		case TCC_EXEC:
			if (status)
				return(TCS_END);
			else if (getmcflag(s, TCC_EXEC))
				return(TCS_EXEC);
			else
				return(TCS_COPY);
		case TCC_CLEAN:
			return(status ? TCS_END : TCS_CLEAN);
		}
		break;
	case TCS_UNLOCK:
		switch (prp->pr_currmode) {
		case TCC_BUILD:
		case TCC_CLEAN:
			return(TCS_END);
		case TCC_EXEC:
			if (status || getmcflag(s, TCC_EXEC))
				return(TCS_END);
			else
				return(TCS_EXEC);
		}
		break;
	case TCS_COPY:
		switch (prp->pr_currmode) {
		case TCC_EXEC:
			return(status ? TCS_END : TCS_UNLOCK);
		}
		break;
	case TCS_REMCOPY:
		switch (prp->pr_currmode) {
		case TCC_BUILD:
			return(status ? TCS_BUILDFAIL : TCS_PREBUILD);
		}
		break;
	case TCS_PREBUILD:
		switch (prp->pr_currmode) {
		case TCC_BUILD:
			return(status ? TCS_BUILDFAIL : TCS_BUILD);
		}
		break;
	case TCS_BUILD:
		return(status ? TCS_BUILDFAIL : TCS_JOURNAL);
	case TCS_EXEC:
	case TCS_CLEAN:
		return(status ? TCS_END : TCS_JOURNAL);
	case TCS_BUILDFAIL:
		switch (prp->pr_currmode) {
		case TCC_BUILD:
			return(TCS_END);
		}
		break;
	case TCS_JOURNAL:
		switch (prp->pr_currmode) {
		case TCC_BUILD:
			return(status ? TCS_BUILDFAIL : TCS_END);
		case TCC_CLEAN:
			return(TCS_END);
		case TCC_EXEC:
			return(TCS_SAVE);
		}
		break;
	case TCS_SAVE:
		switch (prp->pr_currmode) {
		case TCC_EXEC:
			return(TCS_END);
		}
		break;
	default:
		/* this "can't happen" */
		break;
	}

	/* here if the tcstate is invalid for currmode - this "can't happen" */
	error(0, "unexpected state", prtcstate(prp->pr_tcstate));
	fatal(0, "in operating mode", prtccmode(prp->pr_currmode));
	/* NOTREACHED */
	return(0);
}

