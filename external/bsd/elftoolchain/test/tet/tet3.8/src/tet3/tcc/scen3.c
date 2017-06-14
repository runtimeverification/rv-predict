/*
 *	SCCS: @(#)scen3.c	1.4 (97/03/27)
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
static char sccsid[] = "@(#)scen3.c	1.4 (97/03/27) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)scen3.c	1.4 97/03/27 TETware release 3.8
NAME:		scen3.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	scenario parser stage 3 - prune and manipulate the scenario tree

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., March 1997
	in remove_unneeded_scenarios(), restart the "for" loop after
	removing a scenario

************************************************************************/

#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <time.h>
#include "dtmac.h"
#include "error.h"
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
static void add_scenrefs PROTOLIST((struct scentab *));
static void copy_r2 PROTOLIST((struct scentab *, struct scentab *,
	struct scentab **));
static void mark_needed_scenarios PROTOLIST((struct scentab *));
static void proc3refscen PROTOLIST((struct scentab *));
static void remove_unneeded_scenarios PROTOLIST((void));
static void rus2 PROTOLIST((struct scentab *));
static void ynp2 PROTOLIST((struct scentab *, int));


/*
**	proc3sctree() - perform scenario processing pass 3
**
**	the scenario tree at *sctree is manipulated in various ways
**	during this stage of processing
**
**	return 0 if successful or -1 on error
**	(at present - always returns 0)
*/

int proc3sctree(scenario)
char *scenario;
{
	register struct scentab *ep;
	struct scentab *myscen = (struct scentab *) 0;

	/* set the SCF_NEEDED flag in the chosen scenario */
	TRACE1(tet_Tscen, 1, "proc3sctree(): marking needed scenarios");
	for (ep = sctree; ep; ep = ep->sc_forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		ASSERT(ep->sc_type == SC_SCENARIO);
		if (!strcmp(ep->sc_scenario, scenario)) {
			TRACE2(tet_Tscen, 6, "chosen scenario %s is needed",
				ep->sc_scenario);
			ep->sc_flags |= SCF_NEEDED;
			myscen = ep;
			break;
		}
	}

	if (!myscen) {
		scenerrors++;
		fatal(0, "chosen scenario not defined:", scenario);
	}

	/* decend the tree below the chosen scenario, 
		identifying other needed scenarios */
	TRACE2(tet_Tscen, 4, "mark TOP: descend tree below scenario %s",
		myscen->sc_scenario);
	mark_needed_scenarios(myscen->sc_child);

	/* remove un-needed scenario trees */
	TRACE1(tet_Tscen, 1,
		"proc3sctree(): removing un-needed scenario trees 1");
	remove_unneeded_scenarios();

	/* copy referenced scenarios into the chosen tree */
	TRACE1(tet_Tscen, 1, "proc3sctree(): copying referenced scenarios");
	proc3refscen(myscen);

	/* mark all the other scenarios as un-needed and remove them */
	for (ep = sctree; ep; ep = ep->sc_forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		ASSERT(ep->sc_type == SC_SCENARIO);
		if (ep != myscen)
			ep->sc_flags &= ~SCF_NEEDED;
	}
	TRACE1(tet_Tscen, 1,
		"proc3sctree(): removing un-needed scenario trees 2");
	remove_unneeded_scenarios();

	/* here there should only be one scenario (the chosen scenario) */
	ASSERT(sctree->sc_magic == SC_MAGIC);
	ASSERT(sctree->sc_type == SC_SCENARIO);
	ASSERT(sctree->sc_forw == (struct scentab *) 0);
	ASSERT(sctree == myscen);

	/* add the scenario reference numbers */
	TRACE1(tet_Tscen, 1,
		"proc3sctree(): adding scenario reference numbers");
	sctree->sc_ref = 0L;
	add_scenrefs(sctree->sc_child);
	

	return(0);
}

/*
**	mark_needed_scenarios() - set the NEEDED flag in the element at the
**		top of each of the needed scenarios
*/

static void mark_needed_scenarios(ep)
register struct scentab *ep;
{
	/*
	** traverse the tree at this level, descending referenced scenario
	** and directive trees
	*/
	for (; ep; ep = ep->sc_forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		switch (ep->sc_type) {
		case SC_SCEN_NAME:
			ASSERT(ep->sc_scenptr->sc_magic == SC_MAGIC);
			TRACE2(tet_Tscen, 6, "scenario %s is needed",
				ep->sc_scenptr->sc_scenario);
			ep->sc_scenptr->sc_flags |= SCF_NEEDED;
			TRACE2(tet_Tscen, 5, "descend tree below scenario %s",
				ep->sc_scenptr->sc_scenario);
			mark_needed_scenarios(ep->sc_scenptr->sc_child);
			break;
		case SC_DIRECTIVE:
			TRACE2(tet_Tscen, 5, "descend tree below %s directive",
				prscdir(ep->sc_directive));
			mark_needed_scenarios(ep->sc_child);
			break;
		}
	}
}

/*
**	remove_unneeded_scenarios() - remove all unneeded scenarios
**		from the scenario tree
*/

static void remove_unneeded_scenarios()
{
	register struct scentab *ep;
	register int done;

	/*
	** remove each scenario which does not have the NEEDED flag set;
	** we must restart when a scenario is removed because the act
	** of removing a scenario breaks the forward pointer chain
	*/
	done = 0;
	do {
		done = 1;
		for (ep = sctree; ep; ep = ep->sc_forw) {
			ASSERT(ep->sc_magic == SC_MAGIC);
			if (ep->sc_flags & SCF_NEEDED)
				continue;
			done = 0;
			TRACE2(tet_Tscen, 6, "remove unneeded scenario %s",
				ep->sc_scenario);
			if (ep->sc_forw)
				ep->sc_forw->sc_back = ep->sc_back;
			if (ep->sc_back)
				ep->sc_back->sc_forw = ep->sc_forw;
			else {
				ASSERT(ep == sctree);
				sctree = ep->sc_forw;
			}
			if (ep->sc_child)
				rus2(ep->sc_child);
			scfree(ep);
			break;
		}
	} while (!done);
}

/*
**	rus2() - extend the remove_unneeded_scenario() processing
**
**	remove the tree at this level and below
*/

static void rus2(ep)
register struct scentab *ep;
{
	register struct scentab *forw;

	/* remove this level of the tree */
	while (ep) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		forw = ep->sc_forw;
		switch (ep->sc_type) {
		case SC_SCENARIO:
		case SC_DIRECTIVE:
			if (ep->sc_child)
				rus2(ep->sc_child);
			break;
		}
		scfree(ep);
		ep = forw;
	}
}

/*
**	proc3refscen() - replace scenario references with the scenarios
**		themselves
*/ 

static void proc3refscen(parent)
struct scentab *parent;
{
	register struct scentab *ep;
	register int done;

	/*
	** traverse the tree at this level, copying in referenced
	** scenarios and descending directive trees;
	** we must restart the process after a scenario is copied in
	** because the act of copying breaks the forward pointer chain
	*/
	done = 0;
	do {
		done = 1;
		ASSERT(parent->sc_magic == SC_MAGIC);
		for (ep = parent->sc_child; ep; ep = ep->sc_forw) {
			ASSERT(ep->sc_magic == SC_MAGIC);
			switch (ep->sc_type) {
			case SC_SCEN_NAME:
				copy_refscen(ep, parent);
				done = 0;
				break;
			case SC_DIRECTIVE:
				TRACE2(tet_Tscen, 5,
					"descend tree below %s directive",
					prscdir(ep->sc_directive));
				proc3refscen(ep);
				break;
			}
			if (!done)
				break;
		}
	} while (!done);
}

/*
**	copy_refscen() - replace a referenced scenario node with a copy
**		of the referenced scenario
*/

void copy_refscen(ep, parent)
struct scentab *ep, *parent;
{
	register struct scentab *scenptr, *forw;
	struct scentab *back;

	/* remember essential information from this node, then free it */
	back = ep->sc_back;
	forw = ep->sc_forw;
	scenptr = ep->sc_scenptr;
	if (parent->sc_child == ep)
		parent->sc_child = (struct scentab *) 0;
	scfree(ep);

	/* replace the node with the scenario that it referenced */
	ASSERT(scenptr->sc_magic == SC_MAGIC);
	TRACE4(tet_Tscen, 6, "copy_refscen(): replacing referenced scenario node at %s with a copy of scenario %s at %s",
		tet_i2x(ep), scenptr->sc_scenario, tet_i2x(scenptr));
	if (scenptr->sc_child)
		copy_r2(scenptr->sc_child, parent, &back);
	else if (!parent->sc_child)
		parent->sc_child = forw;
	if (back)
		back->sc_forw = forw;
	if (forw)
		forw->sc_back = back;
}

/*
**	copy_r2() - extend the copy_refscen() processing for a referenced
**		scenario
**
**	from points to the first element in the scenario to be copied in
**
**	scenario elements are copied in below the element at *parent and to
**	the right of the element at **sctp
**
**	*sctp is updated as copying progresses - on return, *sctp contains
**	the address of the last element copied in
*/

static void copy_r2(from, parent, sctp)
register struct scentab *from;
struct scentab *parent, **sctp;
{
	register struct scentab *ep;
	struct scentab *sctmp;

	TRACE3(tet_Tscen, 6, "copy_r2(): from = %s, parent = %s",
		tet_i2x(from), tet_i2x(parent));

	for (; from; from = from->sc_forw) {
		ASSERT(from->sc_magic == SC_MAGIC);
		ep = scalloc();
		*ep = *from;
		from->sc_flags |= SCF_DATA_USED;
		ep->sc_flags |= SCF_DATA_USED;
		scstore(ep, parent, sctp);
		if (from->sc_type == SC_DIRECTIVE) {
			TRACE3(tet_Tscen, 6, "copy_r2(): descend tree below %s directive at %s",
				prscdir(from->sc_directive), tet_i2x(from));
			sctmp = (struct scentab *) 0;
			copy_r2(from->sc_child, ep, &sctmp);
		}
	}

	TRACE1(tet_Tscen, 6, "copy_r2() RETURN");
}

/*
**	ynproc() - prune test cases from the scenario tree that are not
**		selected by -y and -n options
*/

void ynproc(flag)
int flag;
{
	TRACE2(tet_Tscen, 1,
		"ynproc(%s): prune scenario tree w.r.t -y/-n options",
		tet_i2a(flag));

	ynp2(sctree, flag);
}

/*
**	ynp2() - extend the ynproc() processing
*/

static void ynp2(ep, flag)
register struct scentab *ep;
int flag;
{
	register struct scentab *forw;

	for (; ep; ep = forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		forw = ep->sc_forw;
		switch (ep->sc_type) {
		case SC_SCENARIO:
		case SC_DIRECTIVE:
			ynp2(ep->sc_child, flag);
			break;
		case SC_TESTCASE:
			if (!okstr(ep->sc_tcname, flag)) {
				TRACE2(tet_Tscen, 4,
					"-{y|n} removes this test case: %s",
					ep->sc_tcname);
				scrm_lnode(ep);
			}
			break;
		}
	}
}

/*
**	add_scenrefs() - add reference numbers to the scenario tree
*/

static void add_scenrefs(ep)
register struct scentab *ep;
{
	static long ref;

	for (; ep; ep = ep->sc_forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		ASSERT(ep->sc_ref == 0L);
		ep->sc_ref = ++ref;
		switch (ep->sc_type) {
		case SC_DIRECTIVE:
			add_scenrefs(ep->sc_child);
			break;
		}
	}
}

