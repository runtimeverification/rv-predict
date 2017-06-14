/*
 *	SCCS: @(#)scenario.c	1.9 (03/08/28)
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
static char sccsid[] = "@(#)scenario.c	1.9 (03/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)scenario.c	1.9 03/08/28 TETware release 3.8
NAME:		scenario.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	top-level interfaces to the scenario parser and the execution engine

MODIFICATIONS:
	Geoff Clare, UniSoft Ltd., August 1996
	Changed sys/time.h to time.h.

	Andrew Dingwall, UniSoft Ltd., April 1999
	When the delay in the main loop in execscen() is out-of-range,
	moderate it to be within range instead of exiting with an ASSERT
	error.
	This is to cater for tests that alter the system time, and might
	make tcc more robust on a heavily-loaded system as well.

	Matthew Hails, The Open Group, August 2003
	Replaced bitshifted time_t value with the use of new
	MAX_TIME_INTERVAL macro defined in dtmac.h.

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <limits.h>
#include <sys/types.h>
#include <time.h>
#include <errno.h>
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "error.h"
#include "scentab.h"
#include "dirtab.h"
#include "proctab.h"
#include "tcc.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* list of scenario lines specified by -l command-line options */
static char **cmdscen;
static int lcmdscen;
static int ncmdscen;

/* scenario file name */
static char *scenario_file = "tet_scen";


/*
**	proclopt() - store a -l command-line option for later processing
*/

void proclopt(line)
char *line;
{
	if (!line || !*line)
		return;

	RBUFCHK((char **) &cmdscen, &lcmdscen,
		(int) (++ncmdscen * sizeof *cmdscen));

	*(cmdscen + ncmdscen - 1) = rstrstore(line);
}

/*
**	procscen() - perform the top-level scenario processing
**
**	scenario is the name of the scenario to be processed
**	sopt is the -s command-line option
**	cwd is tcc's initial working directory
*/

void procscen(scenario, sopt, cwd)
char *scenario, *sopt, *cwd;
{
	char fname[MAXPATH];
	register char **csp;
	FILE *fp;

	/* fix up the scenario file name */
	if (sopt && *sopt) {
		fullpath(cwd, sopt, fname, sizeof fname, 0);
		scenario_file = rstrstore(fname);
	}
	else if (ncmdscen)
		scenario_file = (char *) 0;
	else {
		fullpath(tet_tsroot, scenario_file, fname, sizeof fname, 0);
		scenario_file = rstrstore(fname);
	}

	if (scenario_file)
		TRACE2(tet_Ttcc, 1, "scenario file = %s", scenario_file);
	else
		TRACE1(tet_Ttcc, 1, "no scenario file specified");

	/* process the -l command-line options */
	if (ncmdscen) {
		for (csp = cmdscen; csp < cmdscen + ncmdscen; csp++) {
			if (proc1cmdline(*csp) < 0)
				tcc_exit(2);
			TRACE2(tet_Tbuf, 6, "free -l command-line option = %s",
				tet_i2x(*csp));
			free(*csp);
		}
		if (scenerrors)
			scengiveup();
		TRACE2(tet_Tbuf, 6, "free command-line scenario list = %s",
			tet_i2x(cmdscen));
		free((char *) cmdscen);
		cmdscen = (char **) 0;
		lcmdscen = ncmdscen = 0;
	}

	/*
	** if we have a scenario file name,
	** read in the file and tokenise it
	*/
	if (scenario_file && *scenario_file) {
		if ((fp = fopen(scenario_file, "r")) == (FILE *) 0)
			fatal(errno, "can't open", scenario_file);
		else {
			if (proc1scfile(fp, scenario_file) < 0)
				tcc_exit(1);
			(void) fclose(fp);
		}
		if (scenerrors)
			scengiveup();
	}

	/* build the scenario tree */
	if (proc2sclist() < 0)
		tcc_exit(1);
	if (scenerrors)
		scengiveup();

	/* prune the scenario tree */
	if (proc3sctree(scenario) < 0)
		tcc_exit(1);
	if (scenerrors)
		scengiveup();
}

/*
**	execscen() - execute (process) the scenario
*/

void execscen()
{
	register struct proctab *prp, *q;
	static int sys0 = 0;
	int delay, ndelay;
	time_t now, next;

	/* return now if the scenario is empty */
	if (!sctree || !sctree->sc_child)
		return;

	TRACE2(tet_Ttcc, 1, "about to execute scenario '%s'",
		sctree->sc_scenario);

	/* install the signal traps for the execution engine */
	execsigtrap();

	/* prime the execution engine */
	prp = pralloc();
	prp->pr_scen = sctree->sc_child;
	prp->pr_modes = tcc_modes & (TCC_BUILD | TCC_EXEC | TCC_CLEAN);
	prp->pr_currmode = TCC_START;
	prp->pr_sys = &sys0;
	prp->pr_nsys = 1;
	prp->pr_jfp = jnl_jfp();
	prp->pr_jfname = jnl_jfname();
	prp->pr_level = 1;
	prp->pr_state = PRS_PROCESS;
	prp->pr_flags |= PRF_ATTENTION;
	runqadd(prp);

	/* crank the execution engine until the scenario finishes */
	delay = 0;
	while (prp->pr_state != PRS_IDLE) {
		if (delay) {
			if (delay > WAITINTERVAL_MAX)
				ndelay = WAITINTERVAL_MAX;
			else if (delay < 1)
				ndelay = 1;
			else
				ndelay = delay;
#ifndef NOTRACE
			if (ndelay == delay)
				TRACE2(tet_Texec, 2,
					"execscen(): sleep for %s seconds",
					tet_i2a(ndelay));
			else
				TRACE3(tet_Texec, 2,
	"execscen(): wanted to sleep for %s seconds, moderated to %s seconds",
					tet_i2a(delay), tet_i2a(ndelay));
#endif /* NOTRACE */
			SLEEP((unsigned) ndelay);
		}
		tcc_sloop();
		if (prp->pr_state == PRS_IDLE)
			break;
		now = time((time_t *) 0);
		if (tcc_timeouts(now) > 0) {
			delay = 0;
			continue;
		}
		next = MAX_TIME_INTERVAL;
		for (q = runq; q; q = q->pr_rqforw)
			if (q->pr_nextattn > 0 && q->pr_nextattn < next)
				next = q->pr_nextattn;
		delay = (int) (next - now);
	}

	/* all finished so free the proctab and return */
	prfree(prp);
}

/*
**	is_resume_point() - see if the current position in the scenario
**		matches the resume point stored by rrproc()
**
**	return 1 if it does or 0 if it doesn't
*/

int is_resume_point(prp)
register struct proctab *prp;
{
	TRACE5(tet_Texec, 8, "is_resume_point(): scen = %s, resume_scen = %s, currmode = %s, resume_mode = %s",
		tet_i2x(prp->pr_scen), tet_i2x(resume_scen),
		prtccmode(prp->pr_currmode), prtccmode(resume_mode));

	/*
	** see if this scenario element and current mode match the ones
	** stored by rrproc()
	*/
	if (prp->pr_scen != resume_scen || prp->pr_currmode != resume_mode)
		return(0);

	/* we've found it if resume_mode is not EXEC */
	if (resume_mode != TCC_EXEC)
		return(1);

	/*
	** check that all the enclosing loops' counts match the resume counts
	** stored by rrproc()
	*/
	for (prp = prp->pr_parent; prp; prp = prp->pr_parent) {
		if (prp->pr_scen->sc_type != SC_DIRECTIVE)
			continue;
		switch (prp->pr_scen->sc_directive) {
		case SD_REPEAT:
		case SD_TIMED_LOOP:
			TRACE4(tet_Texec, 8, "is_resume_point(): enclosing directive = %s, loopcount = %s, resume count = %s",
				prscdir(prp->pr_scen->sc_directive),
				tet_i2a(prp->pr_loopcount),
				tet_i2a(prp->pr_scen->sc_rescount));
			if (prp->pr_loopcount != prp->pr_scen->sc_rescount)
					return(0);
			break;
		}
	}

	/* everything matches so we've found it! */
	return(1);
}

