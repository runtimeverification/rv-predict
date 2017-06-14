/*
 *	SCCS: @(#)proctab.c	1.7 (03/03/26)
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
static char sccsid[] = "@(#)proctab.c	1.7 (03/03/26) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)proctab.c	1.7 03/03/26 TETware release 3.8
NAME:		proctab.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	tcc process table management functions

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., March 2000
	Added support for the extra toolstate flag values used in
	engine_shutdown().

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <time.h>
#include <errno.h>
#include "dtmac.h"
#include "bstring.h"
#include "error.h"
#include "ftoa.h"
#include "ltoa.h"
#include "scentab.h"
#include "proctab.h"
#include "tcc.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* the execution engine's run queue */
struct proctab *runq;


/*
**	pralloc(), prfree() - functions to allocate and free a
**		tcc process table element
*/

struct proctab *pralloc()
{
	register struct proctab *prp;

	errno = 0;
	if ((prp = (struct proctab *) malloc(sizeof *prp)) == (struct proctab *) 0)
		fatal(errno, "can't allocate process table element",
			(char *) 0);

	TRACE2(tet_Tbuf, 6, "allocate proctab element = %s", tet_i2x(prp));

	bzero((char *) prp, sizeof *prp);
	prp->pr_magic = PR_MAGIC;
	prp->pr_state = PRS_IDLE;
	prp->pr_toolstate = PTS_IDLE;
	prp->pr_activity = -1;
#ifndef TET_LITE	/* -START-LITE-CUT- */
	prp->pr_snid = -1L;
	prp->pr_xrid = -1L;
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	return(prp);
}

void prfree(prp)
struct proctab *prp;
{
	TRACE2(tet_Tbuf, 6, "free proctab element = %s", tet_i2x(prp));

	if (prp) {
		ASSERT(prp->pr_magic == PR_MAGIC);
		ASSERT(prp->pr_srclock == (char *) 0);
		ASSERT(prp->pr_execlock == (char *) 0);
		ASSERT(prp->pr_tmpdir == (char *) 0);
		ASSERT(prp->pr_outfile == (char *) 0);
		ASSERT(prp->pr_tetxres == (char *) 0);
		ASSERT(prp->pr_tcedir == (char *) 0);
		prcfree(prp);
		bzero((char *) prp, sizeof *prp);
		free((char *) prp);
	}
}

/*
**	prcfree() - free all of this proctab's children
*/

void prcfree(prp)
struct proctab *prp;
{
	register struct proctab *child, *lforw;

	for (child = prp->pr_child; child; child = lforw) {
		ASSERT(child->pr_magic == PR_MAGIC);
		lforw = child->pr_lforw;
		prfree(child);
	}

	prp->pr_child = (struct proctab *) 0;
}

/*
**	runqadd() - add a proctab element to the end of the run queue
*/

void runqadd(prp)
register struct proctab *prp;
{
	register struct proctab *q;

	TRACE4(tet_Texec, 10,
		"runqadd(): add proctab %s to runq (%s), flags = %s",
		tet_i2x(prp), tet_i2x(runq), prpflags(prp->pr_flags));

	ASSERT(prp->pr_magic == PR_MAGIC);

#ifdef TET_LITE	/* -LITE-CUT-LINE- */
	ASSERT((prp->pr_flags & PRF_RUNQ) == 0);
#else	/* -START-LITE-CUT- */
	ASSERT((prp->pr_flags & (PRF_RUNQ | PRF_TC_CHILD)) == 0);
#endif /* TET_LITE */	/* -END-LITE-CUT- */

	if (!runq)
		runq = prp;
	else {
		for (q = runq; q->pr_rqforw; q = q->pr_rqforw)
			ASSERT(q->pr_magic == PR_MAGIC);
		q->pr_rqforw = prp;
		prp->pr_rqback = q;
	}

	prp->pr_flags |= PRF_RUNQ;
}

/*
**	runqrm() - remove a proctab element from the run queue
*/

void runqrm(prp)
register struct proctab *prp;
{
	TRACE3(tet_Texec, 10, "runqrm(): remove proctab %s from runq (%s)",
		tet_i2x(prp), tet_i2x(runq));

	ASSERT(prp->pr_magic == PR_MAGIC);
	ASSERT(prp->pr_flags & PRF_RUNQ);

	if (prp->pr_rqforw) {
		ASSERT(prp->pr_rqforw->pr_magic == PR_MAGIC);
		ASSERT(prp->pr_rqforw->pr_rqback == prp);
		prp->pr_rqforw->pr_rqback = prp->pr_rqback;
	}

	if (prp->pr_rqback) {
		ASSERT(prp->pr_rqback->pr_magic == PR_MAGIC);
		ASSERT(prp->pr_rqback->pr_rqforw == prp);
		prp->pr_rqback->pr_rqforw = prp->pr_rqforw;
	}
	else {
		ASSERT(prp == runq);
		runq = prp->pr_rqforw;
	}

	prp->pr_rqforw = prp->pr_rqback = (struct proctab *) 0;
	prp->pr_flags &= ~PRF_RUNQ;

	TRACE1(tet_Texec, 10, "runqrm() RETURN");
}

/*
**	prpstate() - return a printable representation of a proctab
**		pr_state value
*/

char *prpstate(state)
int state;
{
	static char text[] = "proctab-state ";
	static char msg[sizeof text + LNUMSZ];

	switch (state) {
	case PRS_IDLE:
		return("IDLE");
	case PRS_PROCESS:
		return("PROCESS");
	case PRS_NEXT:
		return("NEXT");
	case PRS_SLEEP:
		return("SLEEP");
	case PRS_WAIT:
		return("WAIT");
	default:
		(void) sprintf(msg, "%s%d", text, state);
		return(msg);
	}
}

/*
**	prtcstate() - return a printable representation of a proctab
**		pr_tcstate value
*/

char *prtcstate(state)
int state;
{
	static char text[] = "testcase-state ";
	static char msg[sizeof text + LNUMSZ];

	switch (state) {
	case TCS_START:
		return("START");
	case TCS_LOCK:
		return("LOCK");
	case TCS_UNLOCK:
		return("UNLOCK");
	case TCS_COPY:
		return("COPY");
	case TCS_PREBUILD:
		return("PREBUILD");
	case TCS_REMCOPY:
		return("REMCOPY");
	case TCS_BUILD:
		return("BUILD");
	case TCS_BUILDFAIL:
		return("BUILDFAIL");
	case TCS_EXEC:
		return("EXEC");
	case TCS_CLEAN:
		return("CLEAN");
	case TCS_JOURNAL:
		return("JOURNAL");
	case TCS_SAVE:
		return("SAVE");
	case TCS_END:
		return("END");
	default:
		(void) sprintf(msg, "%s%d", text, state);
		return(msg);
	}
}

/*
**	prpflags() - return a printable representation of a proctab
**		pr_flags value
*/

char *prpflags(fval)
int fval;
{
	static struct flags flags[] = {
		{ PRF_ATTENTION,	"ATTENTION" },
		{ PRF_RUNQ,		"RUNQ" },
		{ PRF_STEP,		"STEP" },
		{ PRF_SHLOCK,		"SHLOCK" },
		{ PRF_AUTORESULT,	"AUTORESULT" },
#ifndef TET_LITE	/* -START-LITE-CUT- */
		{ PRF_TC_CHILD,		"TC_CHILD" },
		{ PRF_JNL_CHILD,	"JNL_CHILD" }
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
	};

	return(tet_f2a(fval, flags, sizeof flags / sizeof flags[0]));
}

/*
**	prtoolstate() - retur a printable representation of a tool's state
*/

char *prtoolstate(state)
int state;
{
	static char text[] = "tool-state ";
	static char msg[sizeof text + LNUMSZ];

	switch (state) {
	case PTS_IDLE:
		return("IDLE");
	case PTS_RUNNING:
		return("RUNNING");
	case PTS_EXITED:
		return("EXITED");
	case PTS_ABORT:
		return("ABORT");
	case PTS_SIGTERM:
		return("SIGTERM");
	case PTS_SIGKILL:
		return("SIGKILL");
	case PTS_UNWAITEDFOR:
		return("UN-WAITED-FOR");
	case PTS_KILLED_AND_UNWAITEDFOR:
		return("KILLED-AND-UNWAITED-FOR");
	default:
		(void) sprintf(msg, "%s%d", text, state);
		return(msg);
	}
}

