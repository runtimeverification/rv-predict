/*
 *	SCCS: @(#)procdir.c	1.8 (05/12/07)
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
static char sccsid[] = "@(#)procdir.c	1.8 (05/12/07) TETware release 3.8";
#endif

/************************************************************************

SCCS:		@(#)procdir.c	1.8 05/12/07 TETware release 3.8
NAME:		procdir.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	directive processing functions used by the execution engine

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., June 1997
	Changes to enable parallel remote and distributed test cases
	to work correctly.

	Andrew Dingwall, UniSoft Ltd., December 1997
	Replaced SCF_DIST scenario flag (which is per scenario element)
	with pr_distflag proctab flag (which is part of a test case's
	execution context).

	Andrew Dingwall, UniSoft Ltd., March 1998
	Don't iterate round a timed loop in EXEC mode when there are no
	test cases to process.
	When processing a RANDOM directive, don't choose
	a test case that marked to be skipped.
	Interrupt looping directives on abort.

	Neil Moses, The Open Group, December 2005
	In proc_rdist() initialise the proctab reconnect system list 
	using values from the scentab entries.

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <time.h>
#include "dtmac.h"
#include "error.h"
#include "ltoa.h"
#include "scentab.h"
#include "dirtab.h"
#include "proctab.h"
#include "tcc.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* static function declarations */
static int count_tc PROTOLIST((struct scentab *, int));
static struct scentab *get_tc PROTOLIST((struct scentab *, int *));
static int loop_test PROTOLIST((struct proctab *));
static void proc_par_link PROTOLIST((struct proctab *, struct proctab *,
	struct proctab **));
static struct proctab *proc_par_s1 PROTOLIST((struct proctab *,
	struct scentab *));
static void proc_par_s2 PROTOLIST((struct proctab *, struct proctab **,
	struct proctab *));
static void proc_par_simple PROTOLIST((struct proctab *, struct scentab *,
	int, struct proctab **));
static int proc_rtl2 PROTOLIST ((struct proctab *));

#ifndef TET_LITE	/* -START-LITE-CUT- */
static int is_tcdist PROTOLIST((struct scentab *));
static void proc_par_rdist PROTOLIST((struct proctab *, struct scentab *, int,
	struct proctab **));
static void proc_par_rdjnl PROTOLIST((struct proctab *, struct scentab *,
	struct proctab **, void (*) PROTOLIST((struct proctab *))));
#endif /* !TET_LITE */	/* -END-LITE-CUT- */


/*
**	proc_parallel() - process a PARALLEL directive on the run queue
**		whose state is PRS_PROCESS
**
**	PARALLEL sets up a separate processing thread (proctab) for
**	each scenario element below it.
**	Then the execution engine is turned over once for each of the
**	selected modes of operation; i.e., everything is built in
**	parallel, then executed in parallel, then cleaned in parallel.
**	The pr_currmode element in the proctab at this level is used
**	to control this process.
**	The pr_modes element in each child proctab is set to the current
**	mode of operation, thus the children of this directive all think
**	that the current mode is the only mode of operation.
**	The execution engine's single step facility is used to ensure that
**	control is returned to here when each child proctab's state changes
**	to NEXT, rather than to the next scenario element on the child
**	level as would normally be done.
**
**	The only things that we expect below PARALLEL are the leaf scenario
**	nodes or a SEQUENTIAL, REMOTE or DISTRIBUTED directive.
*/

void proc_parallel(prp)
register struct proctab *prp;
{
	register struct scentab *ep;
	register int count;
	struct proctab *lback;

	TRACE3(tet_Texec, 6, "proc_parallel(%s): currmode = %s",
		tet_i2x(prp), prtccmode(prp->pr_currmode));

	/* free child proctabs left after processing the previous mode */
	switch (prp->pr_currmode) {
	case TCC_START:
		break;
	case TCC_BUILD:
	case TCC_EXEC:
	case TCC_CLEAN:
		jnl_consolidate(prp);
		prcfree(prp);
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected mode", prtccmode(prp->pr_currmode));
		/* NOTREACHED */
		return;
	}

	/* step on to the next processing mode */
	prp->pr_currmode = nextmode(prp->pr_modes, prp->pr_currmode);

	TRACE2(tet_Texec, 6, "after initial switch, currmode = %s",
		prtccmode(prp->pr_currmode));

	/*
	** if we have finished processing, move on to the next scenario
	** element at this level;
	** otherwise, determine how many copies of each of the child
	** scenario elements to process
	*/
	switch (prp->pr_currmode) {
	case TCC_BUILD:
	case TCC_CLEAN:
		count = 1;
		break;
	case TCC_EXEC:
		count = prp->pr_scen->sc_count;
		break;
	case TCC_END:
		prp->pr_state = PRS_NEXT;
		prp->pr_flags |= PRF_ATTENTION;
		return;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected mode", prtccmode(prp->pr_currmode));
		/* NOTREACHED */
		return;
	}

	/* in RESUME mode, see if we should switch on test case processing */
	if ((tcc_modes & TCC_RESUME) && !resume_found && is_resume_point(prp)) {
		TRACE1(tet_Texec, 6, "proc_parallel(): found RESUME point");
		resume_found = 1;
	}

	/*
	** allocate a child proctab for each required instance of each
	** child scenario element
	**
	** if a child scenario element is a REMOTE or DISTRIBUTED directive,
	** a child proctab is not allocated for the directive itself;
	** instead, proctabs are allocated for the required number of
	** instances of each scenario element below the directive so that
	** they all get processed in parallel
	**
	** the group of proctabs thus allocated is preceded by a proctab
	** whose journal contains the Start line and followed by a proctab
	** whose journal contains the End line
	**
	** these journal proctabs are not put on the run queue,
	** so they don't cause any processing to be performed
	*/
	lback = (struct proctab *) 0;
	for (ep = prp->pr_scen->sc_child; ep; ep = ep->sc_forw) {
#ifndef TET_LITE	/* -START-LITE-CUT- */
		if (ep->sc_type == SC_DIRECTIVE)
			switch (ep->sc_directive) {
			case SD_REMOTE:
			case SD_DISTRIBUTED:
				proc_par_rdist(prp, ep, count, &lback);
				continue;
			}
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
		proc_par_simple(prp, ep, count, &lback);
	}

	/* wait for all of the children to finish processing */
	prp->pr_state = PRS_SLEEP;
}


#ifndef TET_LITE	/* -START-LITE-CUT- */

/*
**	proc_par_rdist() - extend the proc_parallel() processing for
**		a REMOTE or DISTRIBUTED directive
**
**	here, prp points to the proctab element for the PARALLEL directive
**	and ep1 points to the scentab element for the enclosed REMOTE or
**	DISTRIBUTED directive
*/

static void proc_par_rdist(prp, ep1, count, lbp)
struct proctab *prp, **lbp;
struct scentab *ep1;
int count;
{
	register struct scentab *ep2;
	register struct proctab *child;
	register int n;
	void (*jnlstart) PROTOLIST((struct proctab *));
	void (*jnlend) PROTOLIST((struct proctab *));

	/* determine the journal functions for directive start and end */
	ASSERT(ep1->sc_type == SC_DIRECTIVE);
	switch (ep1->sc_directive) {
	case SD_REMOTE:
		jnlstart = jnl_rmt_start;
		jnlend = jnl_rmt_end;
		break;
	case SD_DISTRIBUTED:
		jnlstart = jnl_dist_start;
		jnlend = jnl_dist_end;
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected directive", prscdir(ep1->sc_directive));
		/* NOTREACHED */
		return;
	}

	/* arrange for a directive start line to be written to the journal */
	proc_par_rdjnl(prp, ep1, lbp, jnlstart);

	/*
	** allocate a proctab element for each required instance of each
	** scenario element below this directive
	*/
	for (ep2 = ep1->sc_child; ep2; ep2 = ep2->sc_forw)
		for (n = 0; n < count; n++) {
			child = proc_par_s1(prp, ep2);
			child->pr_sys = ep1->sc_sys;
			child->pr_nsys = ep1->sc_nsys;
			child->pr_distflag = is_tcdist(ep1);
			proc_par_s2(prp, lbp, child);
		}

	/* arrange for a directive end line to be written to the journal */
	proc_par_rdjnl(prp, ep1, lbp, jnlend);
}

/*
**	proc_par_rdjnl() - arrange for a directive start/end line to be
**		written to the journal
**
**	the line is written to its own child journal file and gathered
**	into the journal at this level by jnl_consolidate() when
**	proc_parallel() is next called
*/

static void proc_par_rdjnl(prp, ep, lbp, jnlfunc)
struct proctab *prp, **lbp;
struct scentab *ep;
void (*jnlfunc) PROTOLIST((struct proctab *));
{
	register struct proctab *child;

	child = pralloc();
	child->pr_parent = prp;
	child->pr_scen = ep;
	child->pr_level = prp->pr_level + 1;
	child->pr_context = prp->pr_context;
	child->pr_sys = ep->sc_sys;
	child->pr_nsys = ep->sc_nsys;
	child->pr_distflag = is_tcdist(ep);
	child->pr_modes = prp->pr_currmode;
	if (jnl_tmpfile(child) < 0) {
		prfree(child);
		return;
	}
	(*jnlfunc)(child);
	proc_par_link(prp, child, lbp);
}

#endif /* !TET_LITE */	/* -END-LITE-CUT- */


/*
**	proc_par_simple() - allocate a child proctab for each required
**		instance of each child scenario element
**
**	the code is divided into two parts so as to enable it to be shared
**	between this function and proc_par_rdist() above
*/

static void proc_par_simple(prp, ep, count, lbp)
struct proctab *prp, **lbp;
struct scentab *ep;
register int count;
{
	register struct proctab *child;

	while (--count >= 0) {
		child = proc_par_s1(prp, ep);
		proc_par_s2(prp, lbp, child);
	}
}

/*
**	proc_par_s1() - part 1 of the proc_par_simple() processing
**
**	allocate a child proctab and fill part of it in
**
**	return a pointer to the allocated child proctab
*/

static struct proctab *proc_par_s1(prp, ep)
struct proctab *prp;
struct scentab *ep;
{
	register struct proctab *child;

	child = pralloc();
	child->pr_parent = prp;
	child->pr_scen = ep;
	child->pr_currmode = TCC_START;
	child->pr_level = prp->pr_level + 1;
	child->pr_context = prp->pr_context;

	return(child);
}

/*
**	proc_par_s2() - part 2 of the proc_par_simple() processing
**
**	fill the rest in, link it below the parent and add it to the runq
*/

static void proc_par_s2(prp, lbp, child)
struct proctab *prp, **lbp, *child;
{
	child->pr_modes = prp->pr_currmode;
	if (jnl_tmpfile(child) < 0) {
		prfree(child);
		return;
	}
	child->pr_state = PRS_PROCESS;
	child->pr_flags |= (PRF_ATTENTION | PRF_STEP);
	proc_par_link(prp, child, lbp);
	runqadd(child);
}

/*
**	proc_par_link() - link a child proctab below the proctab at *prp
**		and to the right of the proctab at **lbp
**
**	on return the pointer at *lbp is updated to point to the
**	proctab just linked in
*/

static void proc_par_link(prp, child, lbp)
struct proctab *prp, *child, **lbp;
{
	if (*lbp)
		(*lbp)->pr_lforw = child;
	else {
		ASSERT(prp->pr_child == (struct proctab *) 0);
		prp->pr_child = child;
	}
	child->pr_lback = *lbp;
	*lbp = child;
}

/*
**	proc_sequential() - process a SEQUENTIAL directive on the run queue
**		whose state is PRS_PROCESS
**
**	SEQUENTIAL sets up a single thread of control (proctab) to process
**	all the scenario elements below it.
**	The execution engine transfers control to the next
**	scenario element on the child level when the child proctab's state
**	changes to NEXT.
**
**	Any directive or leaf scenario node may appear below SEQUENTIAL.
*/

void proc_sequential(prp)
register struct proctab *prp;
{
	register struct proctab *child;

	TRACE3(tet_Texec, 6, "proc_sequential(%s): currmode = %s",
		tet_i2x(prp), prtccmode(prp->pr_currmode));

	/* allocate a child proctab and fill it in */
	child = pralloc();
	child->pr_parent = prp;
	child->pr_scen = prp->pr_scen->sc_child;
	child->pr_currmode = TCC_START;
	child->pr_level = prp->pr_level + 1;
	child->pr_context = prp->pr_context;
	child->pr_state = PRS_PROCESS;
	child->pr_flags = PRF_ATTENTION;
	runqadd(child);
	prp->pr_child = child;

	/* wait for the child to finish processing */
	prp->pr_state = PRS_SLEEP;
}

/*
**	proc_variable() - process a VARIABLE directive on the run queue
**		whose state is PRS_PROCESS
**
**	VARIABLE is just like SEQUENTIAL, except that it saves the current
**	configuration for each system and installs new ones.
**	The saved configurations are restored when this proctab's state
**	changes to NEXT.
**
**	Any directive or leaf scenario node may appear below VARIABLE.
*/

void proc_variable(prp)
register struct proctab *prp;
{
	TRACE3(tet_Texec, 6, "proc_variable(%s): currmode = %s",
		tet_i2x(prp), prtccmode(prp->pr_currmode));

	/* XXX install new configurations here */

	proc_sequential(prp);
}

/*
**	proc_random() - process a RANDOM directive on the run queue
**		whose state is PRS_PROCESS
**
**	RANDOM sets up a single thread of control (proctab).
**	It may either execute a single testcase scenario chosen at random
**	from anywhere below here, or may process the whole tree below
**	here through the child proctab.
**
**	RANDOM only expects to find leaf scenario nodes or a SEQUENTIAL,
**	VARIABLE, REMOTE or DISTRIBUTED directive below here.
*/

void proc_random(prp)
register struct proctab *prp;
{
	register struct proctab *child;
	register struct scentab *ep1, *ep2;
	int choose, flags, skip, skip_tmp;

	TRACE3(tet_Texec, 6, "proc_random(%s): currmode = %s",
		tet_i2x(prp), prtccmode(prp->pr_currmode));

	/*
	** decide if we should choose a single test case to process,
	** or if we should process the whole tree below here
	**
	** if we are below a looping directive, loopcount is non-zero and
	** modes is set to currmode, so:
	**
	**	if we are looping in exec mode, choose a single test case
	**	and process it
	**
	**	if we are looping in build or clean mode, we only loop
	**	once so build or clean the whole tree
	**
	** if we are not below a looping directive, loopcount is zero, so:
	**
	**	if exec mode has been selected, choose a single test case
	** 	and process it, otherwise process the whole tree
	*/
	if (prp->pr_loopcount > 0)
		switch (prp->pr_modes & (TCC_BUILD | TCC_EXEC | TCC_CLEAN)) {
		case TCC_BUILD:
		case TCC_CLEAN:
			choose = 0;
			break;
		case TCC_EXEC:
			choose = 1;
			break;
		default:
			/* this "can't happen" */
			fatal(0, "unexpected mode", prtccmode(prp->pr_modes));
			/* NOTREACHED */
			return;
		}
	else
		choose = prp->pr_modes & TCC_EXEC;

	/* in RESUME mode, see if we should switch on test case processing */
	if ((tcc_modes & TCC_RESUME) && !resume_found && is_resume_point(prp)) {
		TRACE1(tet_Texec, 6, "proc_random(): found RESUME point");
		resume_found = 1;
	}

	/*
	** choose a single test case below here if so required;
	** otherwise, process the whole tree
	**
	** if we are choosing a single test case which is not immediately
	** below this directive, we must create an alternate scenario subtree
	** containing all the subordinate directives but with only the chosen
	** test case at the bottom
	**
	** the alternate scenario subtree is freed when the state of this
	** proctab changes to PROC_NEXT
	*/
	if (choose) {
		if (prp->pr_numtc == 0)
			prp->pr_numtc = count_tc(prp->pr_scen->sc_child, 0);
		if (prp->pr_numtc == 0) {
			TRACE1(tet_Texec, 8,
				"proc_random() RETURN: no TCs to choose from");
			prp->pr_state = PRS_NEXT;
			prp->pr_flags |= PRF_ATTENTION;
			return;
		}
		do {
			skip = rand() % prp->pr_numtc;
			skip_tmp = skip;
			ep1 = get_tc(prp->pr_scen->sc_child, &skip_tmp);
		} while (ep1->sc_flags & SCF_SKIP_ALL);
		ASSERT(ep1 && ep1->sc_type == SC_TESTCASE);
		TRACE4(tet_Texec, 8, "proc_random(): choosing a random test case (%s) after skipping %s out of %s TCs",
			ep1->sc_tcname, tet_i2a(skip), tet_i2a(prp->pr_numtc));
		if (ep1->sc_parent == prp->pr_scen) {
			TRACE1(tet_Texec, 8, "proc_random(): no directives between this level and the chosen test case");
			flags = PRF_STEP;
		}
		else {
			TRACE1(tet_Texec, 8, "proc_random(): building alternate scenario tree to contain the chosen test case");
			ASSERT(prp->pr_altscen == (struct scentab *) 0);
			for (; ep1 != prp->pr_scen; ep1 = ep1->sc_parent) {
				ASSERT(ep1 && ep1->sc_magic == SC_MAGIC);
				ep2 = scalloc();
				*ep2 = *ep1;
				ep2->sc_flags |= SCF_DATA_USED;
				ep2->sc_forw = (struct scentab *) 0;
				ep2->sc_back = (struct scentab *) 0;
				ep2->sc_child = prp->pr_altscen;
				if (prp->pr_altscen)
					prp->pr_altscen->sc_parent = ep2;
				prp->pr_altscen = ep2;
			}
			ep1 = prp->pr_altscen;
			flags = 0;
		}
	}
	else {
		ep1 = prp->pr_scen->sc_child;
		flags = 0;
		TRACE1(tet_Texec, 8, "proc_random(): processing whole subtree");
	}

	/* allocate a proctab element and fill it in */
	child = pralloc();
	child->pr_parent = prp;
	child->pr_scen = ep1;
	child->pr_currmode = TCC_START;
	child->pr_level = prp->pr_level + 1;
	child->pr_context = prp->pr_context;
	child->pr_state = PRS_PROCESS;
	child->pr_flags |= (flags | PRF_ATTENTION);
	runqadd(child);
	prp->pr_child = child;

	/* wait for the child to finish processing */
	prp->pr_state = PRS_SLEEP;
}

/*
**	count_tc() - count the number of test cases on this level
**		and below
*/

static int count_tc(ep, flagmask)
register struct scentab *ep;
register int flagmask;
{
	register int count = 0;

	/*
	** traverse the tree at this level, counting test cases and
	** descending directive trees
	*/
	for (; ep; ep = ep->sc_forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		switch (ep->sc_type) {
		case SC_DIRECTIVE:
			count += count_tc(ep->sc_child, flagmask);
			break;
		case SC_TESTCASE:
			if ((ep->sc_flags & flagmask) == 0)
				count++;
			break;
		case SC_SCENINFO:
			break;
		default:
			/* this "can't happen" */
			fatal(0, "unexpected type", prsctype(ep->sc_type));
			/* NOTREACHED */
		}
	}

	return(count);
}

/*
**	get_tc() - return a pointer to the (*skp + 1)'th test case
**		on this level in the scenario or below
**
**	return (struct scentab *) 0 if we have not skipped enough
**	test cases yet
*/

static struct scentab *get_tc(ep1, skp)
register struct scentab *ep1;
int *skp;
{
	register struct scentab *ep2;

	/*
	** traverse the tree until either:
	**	a) we have skipped enough test cases, or
	**	b) we come to the end of this level
	**
	** if we find a tree node, we descend the tree below that as well
	*/
	for (; ep1; ep1 = ep1->sc_forw) {
		ASSERT(ep1->sc_magic == SC_MAGIC);
		switch (ep1->sc_type) {
		case SC_DIRECTIVE:
			if ((ep2 = get_tc(ep1->sc_child, skp)) != (struct scentab *) 0)
				return(ep2);
			break;
		case SC_TESTCASE:
			if (--*skp < 0)
				return(ep1);
			break;
		case SC_SCENINFO:
			break;
		default:
			/* this "can't happen" */
			fatal(0, "unexpected type", prsctype(ep1->sc_type));
			/* NOTREACHED */
		}
	}

	return((struct scentab *) 0);
}


/*
**	proc_rtloop() - common processing for a REPEAT or TIMED_LOOP
**		directive on the run queue whose state is PRS_PROCESS
**
**	REPEAT and TIMED_LOOP each set up a single thread of control (proctab).
**	The scenario tree below here is processed using this proctab.
**	Then the execution engine is turned over once for each of the
**	selected modes of operation; i.e., everything is built once,
**	executed the specified number of times, and cleaned once.
**	The pr_currmode element in the proctab at this level is used
**	to control this process.
**	The pr_modes element in the child proctab is set to the current
**	mode of operation, thus the children of this directive all think
**	that the current mode is the only mode of operation.
**
**	The execution engine transfers control to the next
**	scenario element on the child level when the child proctab's state
**	changes to NEXT.
**	Control is returned to here when the last child scenario element
**	has finished processing.
**
**	Any directive or leaf scenario node may appear below REPEAT
**	or TIMED_LOOP.
*/

void proc_rtloop(prp)
register struct proctab *prp;
{
	TRACE4(tet_Texec, 6, "proc_rtloop(%s): currmode = %s, starttime = %s",
		tet_i2x(prp), prtccmode(prp->pr_currmode),
		tet_l2a(prp->pr_starttime));

	/* determine the current mode first time through */
	switch (prp->pr_currmode) {
	case TCC_START:
		prp->pr_currmode = nextmode(prp->pr_modes, prp->pr_currmode);
		prp->pr_loopcount = 0;
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

	TRACE3(tet_Texec, 6,
		"after initial switch, currmode = %s, loopcount = %s",
		prtccmode(prp->pr_currmode), tet_i2a(prp->pr_loopcount));

	/*
	** if currmode is END, this means that abort has been called
	** just as we were about to start processing this directive
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

	/* see if we should perform the next iteration of the loop */
	if (proc_rtl2(prp))
		return;

	/*
	** here for the end of the loop -
	**	free the child proctab and move on to the next mode
	*/
	prcfree(prp);
	prp->pr_currmode = nextmode(prp->pr_modes, prp->pr_currmode);
	if (prp->pr_currmode == TCC_END) {
		prp->pr_loopcount = prp->pr_parent ? prp->pr_parent->pr_loopcount : 0;
		prp->pr_state = PRS_NEXT;
	}
	else {
		prp->pr_loopcount = 0;
		prp->pr_state = PRS_PROCESS;
	}
	prp->pr_flags |= PRF_ATTENTION;

	TRACE4(tet_Texec, 6, "proc_rtloop(%s) RETURN for loop end, currmode = %s, state = %s",
		tet_i2x(prp), prtccmode(prp->pr_currmode),
		prpstate(prp->pr_state));
}

/*
**	proc_rtl2() - extend the proc_rtloop() processing
**
**	return 1 if it's OK to continue the loop or 0 to end the loop
*/

static int proc_rtl2(prp)
register struct proctab *prp;
{
	register struct proctab *child;

	/* end the loop unconditionally if abort has been called */
	if (prp->pr_modes & TCC_ABORT)
		return(0);

	/*
	** if this is the first iteration of the loop in the current mode:
	**
	**	avoid thrashing if this is the first iteration in EXEC mode;
	**	otherwise, allocate a child proctab and fill it in
	**
	*/
	if (prp->pr_loopcount == 0) {
		if (
			prp->pr_currmode == TCC_EXEC &&
			prp->pr_scen->sc_directive == SD_TIMED_LOOP &&
			count_tc(prp->pr_scen->sc_child, SCF_SKIP_EXEC) == 0
		) {
			jnl_tcc_prpmsg(prp, "processing of empty timed loop suppressed in EXEC mode");
			return(0);
		}
		child = pralloc();
		child->pr_parent = prp;
		child->pr_level = prp->pr_level + 1;
		child->pr_context = prp->pr_context;
		child->pr_modes = prp->pr_currmode;
		prp->pr_child = child;
		prp->pr_starttime = time((time_t *) 0);
	}
	else
		child = prp->pr_child;

	/*
	** see if we must perform another iteration of the loop -
	**	if so, put the child proctab on the run queue and sleep;
	** the child proctab comes off the runq when its state changes
	** to PRS_NEXT
	*/
	if (loop_test(prp)) {
		child->pr_scen = prp->pr_scen->sc_child;
		child->pr_currmode = TCC_START;
		child->pr_loopcount = ++prp->pr_loopcount;
		child->pr_state = PRS_PROCESS;
		child->pr_flags |= PRF_ATTENTION;
		runqadd(child);
		prp->pr_state = PRS_SLEEP;
		TRACE4(tet_Texec, 6, "proc_rtloop(%s) RETURN for loop iteration %s, state = %s",
			tet_i2x(prp), tet_i2a(prp->pr_loopcount),
			prpstate(prp->pr_state));
		return(1);
	}

	return(0);
}

/*
**	loop_test() - perform a test at the start of a REPEAT or
**		TIMED_LOOP loop
**
**	return 1 if it's OK to go round the loop again, or 0 if it isn't
*/

static int loop_test(prp)
register struct proctab *prp;
{
	time_t now, maxtime;

	/*
	** we only loop in EXEC mode
	**
	** in RERUN mode, or in RESUME mode before the resume point,
	** the rerun/resume processing has supplied a loopcount for use in a
	** TIMED_LOOP so for the purposes of this test we treat a TIMED_LOOP
	** under these conditions as if it is a REPEAT
	*/
	if (prp->pr_currmode == TCC_EXEC) {
		switch (prp->pr_scen->sc_directive) {
		case SD_REPEAT:
			return(prp->pr_loopcount < prp->pr_scen->sc_count);
		case SD_TIMED_LOOP:
			now = time((time_t *) 0);
			maxtime = prp->pr_starttime + prp->pr_scen->sc_seconds;
			if (maxtime <= (time_t) 0)
				error(0, "TIMED_LOOP time is too far away:",
					tet_i2a(prp->pr_scen->sc_seconds));
			if ((tcc_modes & TCC_RERUN) || ((tcc_modes & TCC_RESUME) && !resume_found))
				return(now < maxtime && prp->pr_loopcount < prp->pr_scen->sc_count);
			else
				return(now < maxtime);
		default:
			/* this "can't happen" */
			fatal(0, "unexpected directive",
				prscdir(prp->pr_scen->sc_directive));
			/* NOTREACHED */
			return(0);
		}
	}
	else
		return(prp->pr_loopcount < 1);
}


#ifndef TET_LITE	/* -START-LITE-CUT- */

/*
**	proc_rdist() - common processing for a REMOTE or DISTRIBUTED
**		directive on the run queue whose state is PRS_PROCESS
**
**	REMOTE and DISTRIBUTED each set up a single thread of control
**	(proctab).
**	The scenario tree below here is processed using this proctab.
**
**	The system list and number of systems in the child proctab's
**	context is updated to reflect the system list and number of systems
**	associated with this directive.
**
**	The execution engine transfers control to the next
**	scenario element on the child level when the child proctab's state
**	changes to NEXT.
**	Control is returned to here when the last child scenario element
**	has finished processing.
**
**	Any directive or leaf scenario node may appear below REMOTE
**	or DISTRIBUTED, except anoter REMOTE or DISTRIBUTED directive.
*/

void proc_rdist(prp)
register struct proctab *prp;
{
	register struct proctab *child;
	register struct scentab *ep = prp->pr_scen;

	TRACE3(tet_Texec, 6, "proc_rdist(%s): currmode = %s",
		tet_i2x(prp), prtccmode(prp->pr_currmode));

	/* allocate a child proctab and fill it in */
	child = pralloc();
	child->pr_parent = prp;
	child->pr_scen = ep->sc_child;
	child->pr_currmode = TCC_START;
	child->pr_level = prp->pr_level + 1;
	child->pr_context = prp->pr_context;
	child->pr_sys = ep->sc_sys;
	child->pr_nsys = ep->sc_nsys;
	child->pr_recon = ep->sc_recon;
	child->pr_nrecon = ep->sc_nrecon;
	child->pr_distflag = is_tcdist(ep);
	child->pr_state = PRS_PROCESS;
	child->pr_flags |= PRF_ATTENTION;
	runqadd(child);
	prp->pr_child = child;

	/* wait for the child to finish processing */
	prp->pr_state = PRS_SLEEP;
}

/*
**	is_tcdist() - return 1 if test cases are distributed, 0 if not
*/

static int is_tcdist(ep)
register struct scentab *ep;
{
	register int *ip;

	ASSERT(ep->sc_type == SC_DIRECTIVE);
	switch (ep->sc_directive) {
	case SD_REMOTE:
		for (ip = ep->sc_sys; ip < ep->sc_sys + ep->sc_nsys; ip++)
			if (*ip == 0)
				return(1);
		return(0);
	case SD_DISTRIBUTED:
		return(1);
	default:
		/* this "can't happen" */
		fatal(0, "unexpected directive", prscdir(ep->sc_directive));
		/* NOTREACHED */
		return(0);
	}
}

#endif /* !TET_LITE */	/* -END-LITE-CUT- */

