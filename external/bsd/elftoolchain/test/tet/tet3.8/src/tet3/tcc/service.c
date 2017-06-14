/*
 *	SCCS: @(#)service.c	1.6 (02/01/18)
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
static char sccsid[] = "@(#)service.c	1.6 (02/01/18) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)service.c	1.6 02/01/18 TETware release 3.8
NAME:		service.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	the tcc execution engine

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., March 2000
	Make the processing in tcc_timeouts() more robust in the face of
	system clock adjustments.

	Andrew Dingwall, The Open Group, January 2002
	Fixed an infinite loop problem when an empty directive is detected
	(really only likely in resume/rerun mode)

************************************************************************/

#include <stdio.h>
#include <sys/types.h>
#include <time.h>
#include "dtmac.h"
#include "error.h"
#include "proctab.h"
#include "scentab.h"
#include "dirtab.h"
#include "tcc.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* static function declarations */
static void proc_directive PROTOLIST((struct proctab *));
static void proc_n2 PROTOLIST((struct proctab *));
static void proc_next PROTOLIST((struct proctab *));
static void proc_process PROTOLIST((struct proctab *));
static void proc_sceninfo PROTOLIST((struct proctab *));
static void proc_wait PROTOLIST((struct proctab *));
static void tcc_service PROTOLIST((struct proctab *));
static void wakeup PROTOLIST((struct proctab *));


/*
**	tcc_sloop() - TCC service loop
**
**	service all entries in the run queue which need attention
*/

void tcc_sloop()
{
	register struct proctab *prp, *rqforw;
	register int done;

	TRACE1(tet_Texec, 2, "tcc_sloop() START");

	do {
		TRACE1(tet_Texec, 2, "tcc_sloop() RESTART");
		exec_block_signals();
		done = 1;
		for (prp = runq; prp; prp = rqforw) {
			rqforw = prp->pr_rqforw;
			if (prp->pr_flags & PRF_ATTENTION) {
				prp->pr_flags &= ~PRF_ATTENTION;
				tcc_service(prp);
				done = 0;
			}
		}
		exec_unblock_signals();
	} while (!done);

	TRACE1(tet_Texec, 2, "tcc_sloop() END");
}

/*
**	tcc_timeouts() - set the ATTENTION flag in each proctab on the runq
**		whose timeout has expired
**
**	return the number of proctabs which need attention
*/

int tcc_timeouts(now)
time_t now;
{
	register int count = 0;
	register struct proctab *prp;

	TRACE1(tet_Texec, 2, "tcc_timeouts() START");

	/*
	** look down the runq and moderate the nextattn values on proctabs
	** that are hopelessly out-of-range;
	** this is to allow for the possibility that the value was set
	** while the system clock was a long way from where it is now
	*/
	for (prp = runq; prp; prp = prp->pr_rqforw)
		if (prp->pr_nextattn > (now + (WAITINTERVAL_MAX * 2)))
			prp->pr_nextattn = now + WAITINTERVAL_MAX;
		else if (prp->pr_nextattn < 0)
			prp->pr_nextattn = now;

	/*
	** look down the runq and set the attention flag on proctabs whose
	** timeouts have expired
	*/
	for (prp = runq; prp; prp = prp->pr_rqforw)
		if (prp->pr_nextattn > 0)
			if (prp->pr_nextattn <= now) {
				prp->pr_nextattn = 0;
				prp->pr_flags |= PRF_ATTENTION;
				count++;
			}

	TRACE2(tet_Texec, 2, "tcc_timeouts() RETURN; count = %s",
		tet_i2a(count));

	return(count);
}

/*
**	tcc_service() - service a single entry in the run queue
**		whose ATTENTION flag is set
*/

static void tcc_service(prp)
struct proctab *prp;
{

	TRACE4(tet_Texec, 3, "tcc_service(%s): state = %s, flags = %s",
		tet_i2x(prp), prpstate(prp->pr_state), prpflags(prp->pr_flags));

	/* process the entry according to its state */
	switch (prp->pr_state) {
	case PRS_PROCESS:
		proc_process(prp);
		break;
	case PRS_NEXT:
		proc_next(prp);
		break;
	case PRS_SLEEP:
		wakeup(prp);
		break;
	case PRS_WAIT:
		proc_wait(prp);
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected state", prpstate(prp->pr_state));
		/* NOTREACHED */
	}

	TRACE4(tet_Texec, 3, "tcc_service(%s) RETURN: state = %s, flags = %s",
		tet_i2x(prp), prpstate(prp->pr_state), prpflags(prp->pr_flags));
}

/*
**	proc_process() - process an entry on the run queue
**		whose state is PRS_PROCESS
*/

static void proc_process(prp)
struct proctab *prp;
{
	register struct scentab *ep = prp->pr_scen;

	TRACE5(tet_Texec, 4,
		"proc_process(%s): state = %s, flags = %s, currmode = %s",
		tet_i2x(prp), prpstate(prp->pr_state), prpflags(prp->pr_flags),
		prtccmode(prp->pr_currmode));

	ASSERT(ep->sc_magic == SC_MAGIC);

	/* process the element */
	switch (ep->sc_type) {
	case SC_DIRECTIVE:
		proc_directive(prp);
		break;
	case SC_TESTCASE:
		proc_testcase(prp);
		break;
	case SC_SCENINFO:
		proc_sceninfo(prp);
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected type", prsctype(ep->sc_type));
		/* NOTREACHED */
	}

	TRACE4(tet_Texec, 4, "proc_process(%s) RETURN: state = %s, flags = %s",
		tet_i2x(prp), prpstate(prp->pr_state), prpflags(prp->pr_flags));
}

/*
**	proc_directive() - process a directive on the run queue
**		whose state is PRS_PROCESS
*/

static void proc_directive(prp)
register struct proctab *prp;
{
	register struct scentab *ep = prp->pr_scen;

	TRACE3(tet_Texec, 4, "proc_directive(%s): directive = %s",
		tet_i2x(prp), prscdir(ep->sc_directive));

	/* report a directive start */
	if (prp->pr_currmode == TCC_START)
		switch (ep->sc_directive) {
		case SD_PARALLEL:
			jnl_par_start(prp);
			break;
		case SD_SEQUENTIAL:
			jnl_seq_start(prp);
			break;
		case SD_REPEAT:
			jnl_rpt_start(prp);
			break;
		case SD_TIMED_LOOP:
			jnl_tloop_start(prp);
			break;
		case SD_RANDOM:
			jnl_rnd_start(prp);
			break;
		case SD_VARIABLE:
			jnl_var_start(prp);
			break;
#ifndef TET_LITE	/* -START-LITE-CUT- */
		case SD_REMOTE:
			jnl_rmt_start(prp);
			break;
		case SD_DISTRIBUTED:
			jnl_dist_start(prp);
			break;
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
		default:
			/* this "can't happen" */
			fatal(0, "unexpected directive",
				prscdir(ep->sc_directive));
			/* NOTREACHED */
		}

	/* move on to the next scenario element if there are no elements
		below this one */
	if (ep->sc_child)
		ASSERT(ep->sc_child->sc_magic == SC_MAGIC);
	else {
		TRACE1(tet_Texec, 4,
			"proc_directive(): empty directive RETURN");
		prp->pr_currmode = TCC_END;
		prp->pr_state = PRS_NEXT;
		prp->pr_flags |= PRF_ATTENTION;
		return;
	}

	/* otherwise, process the directive */
	switch (ep->sc_directive) {
	case SD_PARALLEL:
		proc_parallel(prp);
		break;
	case SD_SEQUENTIAL:
		proc_sequential(prp);
		break;
	case SD_REPEAT:
	case SD_TIMED_LOOP:
		proc_rtloop(prp);
		break;
	case SD_RANDOM:
		proc_random(prp);
		break;
	case SD_VARIABLE:
		proc_variable(prp);
		break;
#ifndef TET_LITE	/* -START-LITE-CUT- */
	case SD_REMOTE:
	case SD_DISTRIBUTED:
		proc_rdist(prp);
		break;
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
	default:
		/* this "can't happen" */
		fatal(0, "unexpected directive", prscdir(ep->sc_directive));
		/* NOTREACHED */
	}

	TRACE3(tet_Texec, 4, "proc_directive(%s) RETURN: directive = %s",
		tet_i2x(prp), prscdir(ep->sc_directive));
}

/*
**	proc_sceninfo() - process a scenario line on the run queue
**		whose state is PRS_PROCESS
*/

static void proc_sceninfo(prp)
register struct proctab *prp;
{
	TRACE2(tet_Texec, 4, "proc_sceninfo(%s)", tet_i2x(prp));

	/* output the line to the journal file */
	jnl_sceninfo(prp, prp->pr_scen->sc_sceninfo);

	/* move on to the next scenario element */
	prp->pr_state = PRS_NEXT;
	prp->pr_flags |= PRF_ATTENTION;

	TRACE2(tet_Texec, 4, "proc_sceninfo(%s) RETURN", tet_i2x(prp));
}

/*
**	proc_wait() - process an entry on the run queue whose state
**		is PRS_WAIT
*/

static void proc_wait(prp)
struct proctab *prp;
{
	register struct scentab *ep = prp->pr_scen;

	TRACE5(tet_Texec, 4,
		"proc_wait(%s): state = %s, flags = %s, currmode = %s",
		tet_i2x(prp), prpstate(prp->pr_state), prpflags(prp->pr_flags),
		prtccmode(prp->pr_currmode));

	ASSERT(ep->sc_magic == SC_MAGIC);

	/* process the element */
	switch (ep->sc_type) {
	case SC_TESTCASE:
		proc_tcwait(prp);
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected type", prsctype(ep->sc_type));
		/* NOTREACHED */
	}

	TRACE4(tet_Texec, 4, "proc_wait(%s) RETURN: state = %s, flags = %s",
		tet_i2x(prp), prpstate(prp->pr_state), prpflags(prp->pr_flags));
}

/*
**	proc_next() - step on to the next action when state is PRS_NEXT
*/

static void proc_next(prp)
struct proctab *prp;
{
#ifndef NOTRACE
	register struct scentab *ep = prp->pr_scen;

	TRACE5(tet_Texec, 4,
		"proc_next(%s): prflags = %s, scentype = %s, currmode = %s",
		tet_i2x(prp), prpflags(prp->pr_flags), prsctype(ep->sc_type),
		prtccmode(prp->pr_currmode));
	switch (ep->sc_type) {
	case SC_DIRECTIVE:
		TRACE2(tet_Texec, 4, "\tdirective = %s",
			prscdir(ep->sc_directive));
		break;
	case SC_TESTCASE:
		TRACE3(tet_Texec, 4, "\ttcname = %s, tcstate = %s",
			ep->sc_tcname, prtcstate(prp->pr_tcstate));
		break;
	}
#endif

	proc_n2(prp);

#ifndef NOTRACE
	ep = prp->pr_scen;
	TRACE5(tet_Texec, 4, "proc_next(%s) RETURN: prflags = %s, scentype = %s, currmode = %s",
		tet_i2x(prp), prpflags(prp->pr_flags), prsctype(ep->sc_type),
		prtccmode(prp->pr_currmode));
	switch (ep->sc_type) {
	case SC_DIRECTIVE:
		TRACE2(tet_Texec, 4, "\tdirective = %s",
			prscdir(ep->sc_directive));
		break;
	case SC_TESTCASE:
		TRACE3(tet_Texec, 4, "\ttcname = %s, tcstate = %s",
			ep->sc_tcname, prtcstate(prp->pr_tcstate));
		break;
	}
#endif

}

/*
**	proc_n2() - extend the proc_next() processing
*/

static void proc_n2(prp)
register struct proctab *prp;
{
	register struct scentab *ep = prp->pr_scen;

	/*
	** determine whether we must re-visit the current element
	** or move on to the next element
	*/
	switch (ep->sc_type) {
	case SC_DIRECTIVE:
		switch (ep->sc_directive) {
		case SD_PARALLEL:
		case SD_REPEAT:
		case SD_TIMED_LOOP:
			if (prp->pr_currmode != TCC_END) {
				prp->pr_state = PRS_PROCESS;
				prp->pr_flags |= PRF_ATTENTION;
				return;
			}
			break;
		case SD_VARIABLE:
			/* XXX restore previous configurations here */
			break;
		}
		break;
	case SC_TESTCASE:
		if (prp->pr_currmode != TCC_END) {
			prp->pr_state = PRS_PROCESS;
			prp->pr_flags |= PRF_ATTENTION;
			return;
		}
		break;
	case SC_SCENINFO:
		break;
	default:
		fatal(0, "unexpected type", prsctype(prp->pr_scen->sc_type));
		/* NOTREACHED */
	}

	/*
	** here to stop processing the current scenario element -
	** free any child proctabs
	*/
	prcfree(prp);

	/* make a directive 'end' entry in the journal */
	if (ep->sc_type == SC_DIRECTIVE)
		switch (ep->sc_directive) {
		case SD_PARALLEL:
			jnl_par_end(prp);
			break;
		case SD_SEQUENTIAL:
			jnl_seq_end(prp);
			break;
		case SD_REPEAT:
			jnl_rpt_end(prp);
			break;
		case SD_TIMED_LOOP:
			jnl_tloop_end(prp);
			break;
		case SD_RANDOM:
			jnl_rnd_end(prp);
			break;
		case SD_VARIABLE:
			jnl_var_end(prp);
			break;
#ifndef TET_LITE	/* -START-LITE-CUT- */
		case SD_REMOTE:
			jnl_rmt_end(prp);
			break;
		case SD_DISTRIBUTED:
			jnl_dist_end(prp);
			break;
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
		default:
			/* this "can't happen" */
			fatal(0, "unexpected directive",
				prscdir(ep->sc_directive));
			/* NOTREACHED */
		}

	/* free the per-proctab alternate scenario tree if there is one */
	if (prp->pr_altscen) {
		register struct scentab *ep2, *child;
		TRACE1(tet_Texec, 8,
			"proc_n2(): freeing the alternate scenario tree");
		for (ep2 = prp->pr_altscen; ep2; ep2 = child) {
			child = ep2->sc_child;
			scfree(ep2);
		}
		prp->pr_altscen = (struct scentab *) 0;
	}

	/*
	** if
	**	abort has not been called
	** and
	**	we are not single-stepping
	** and
	**	there is another scenario element at this level:
	** move on to the next scenario element;
	** otherwise, come off the run queue and wake up the parent
	*/
	if (
		(tcc_modes & TCC_ABORT) == 0 &&
		(prp->pr_flags & PRF_STEP) == 0 &&
		ep->sc_forw
	) {
		prp->pr_scen = ep->sc_forw;
		prp->pr_exiclist = (char *) 0;
		prp->pr_numtc = 0;
		prp->pr_starttime = time((time_t *) 0);
		prp->pr_tcstate = TCS_START;
		prp->pr_currmode = TCC_START;
		prp->pr_activity = -1;
		prp->pr_modes &= ~TCC_ABORT;
		prp->pr_state = PRS_PROCESS;
		prp->pr_flags |= PRF_ATTENTION;
	}
	else {
		prp->pr_state = PRS_IDLE;
		runqrm(prp);
		wakeup(prp->pr_parent);
	}
}

/*
**	wakeup() - see if any child proctab elements are still on the
**		run queue
**
**	if none of the child proctabs are still on the run queue,
**	move on to the next stage of processing
*/

static void wakeup(prp)
struct proctab *prp;
{
	register struct proctab *child;

	TRACE5(tet_Texec, 4, "wakeup(%s): child = %s, state = %s, flags = %s",
		tet_i2x(prp), tet_i2x(prp ? prp->pr_child : 0),
		prp ? prpstate(prp->pr_state) : "0",
		prpflags(prp ? prp->pr_flags : 0));

	if (prp)
		ASSERT(prp->pr_magic == PR_MAGIC);
	else
		return;

	/* see of any of the child proctabs are on the run queue */
	for (child = prp->pr_child; child; child = child->pr_lforw) {
		ASSERT(child->pr_magic == PR_MAGIC);
		if (child->pr_flags & PRF_RUNQ)
			break;
	}

	/* if there aren't, arrange to move on to the next processing stage */
	if (!child) {
		prp->pr_state = PRS_NEXT;
		prp->pr_flags |= PRF_ATTENTION;
	}

	TRACE4(tet_Texec, 4, "wakeup(%s) RETURN: state = %s, flags = %s",
		tet_i2x(prp), prpstate(prp->pr_state), prpflags(prp->pr_flags));
}

/*
**	nextmode() - return the next processing mode, given a set of modes
**		of operation and the current mode
*/

int nextmode(modes, currmode)
register int modes, currmode;
{
	if (modes & TCC_ABORT)
		return(TCC_END);

	switch (currmode) {
	case TCC_START:
		if (modes & TCC_BUILD)
			return(TCC_BUILD);
		/* else fall through */
	case TCC_BUILD:
		if (modes & TCC_EXEC)
			return(TCC_EXEC);
		/* else fall through */
	case TCC_EXEC:
		if (modes & TCC_CLEAN)
			return(TCC_CLEAN);
		/* else fall through */
	case TCC_CLEAN:
		return(TCC_END);
	default:
		/* this "can't happen" */
		fatal(0, "unexpected mode", prtccmode(currmode));
		/* NOTREACHED */
		return(TCC_END);
	}
}

