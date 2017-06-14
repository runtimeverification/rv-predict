/*
 *	SCCS: @(#)scen2.c	1.9 (02/01/18)
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
static char sccsid[] = "@(#)scen2.c	1.9 (02/01/18) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)scen2.c	1.9 02/01/18 TETware release 3.8
NAME:		scen2.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	scenario parser stage 2 - build the scenario tree

MODIFICATIONS:
	Geoff Clare, UniSoft Ltd., August 1996
	Missing <string.h>.

	Andrew Dingwall, UniSoft Ltd., December 1997
	removed SCF_DIST scenario flag - the "distributed" attribute
	is now part of a test case's execution context
	(see struct proctab in proctab.h)

	Andrew Dingwall, UniSoft Ltd., November 1999
	disallow .. in a test case name


************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <time.h>
#include <string.h>
#include <ctype.h>
#include "dtmac.h"
#include "error.h"
#include "tcc.h"
#include "scentab.h"
#include "dirtab.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* static function declarations */
static void check_nesting PROTOLIST((struct scentab *, struct scentab **));
static int check_timed_loops PROTOLIST((struct scentab *));
static void check_tc_sys PROTOLIST((struct scentab *));
static void check_valid_scen_name PROTOLIST((struct scentab *));
static void chn2 PROTOLIST((struct scentab *, struct scentab **));
static void etet_fix PROTOLIST((struct scentab *));
static void etf2 PROTOLIST((struct scentab *));
static int etf2_compat PROTOLIST((struct scentab *));
static int etf3 PROTOLIST((struct scentab *));
static void etf4 PROTOLIST((struct scentab *));
static struct scentab *find2scen PROTOLIST((char *));
static int proc2scdir PROTOLIST((struct scentab *, struct scentab *,
	struct scentab **));
static int proc2scen PROTOLIST((struct scentab *));
static void report_unmatched PROTOLIST((int, int, char *));
static void resolv_scenptr PROTOLIST((struct scentab *, struct scentab **));
static void rsc2 PROTOLIST((struct scentab *, struct scentab **));

#ifndef TET_LITE	/* -START-LITE-CUT- */
static void cts2 PROTOLIST((struct scentab *));
#endif /* !TET_LITE */	/* -END-LITE-CUT- */


/*
**	proc2sclist() - perform scenario processing pass 2
**
**	input is the linear list of scenario elements at *sclist which
**	have been gathered during pass 1
**
**	output is a tree of scenario elements at *sctree
**
**	return 0 if successful or -1 on error
**	(at present - always returns 0)
*/

int proc2sclist()
{
	struct scentab *sctmp;
	register struct scentab *ep;
	struct scentab *q;
	register int skip;

	/*
	** sclist is a LIFO stack -
	** it is in reverse order so we must invert it
	*/
	TRACE1(tet_Tscen, 1, "proc2sclist(): inverting the element list");
	sctmp = (struct scentab *) 0;
	while ((ep = scpop(&sclist)) != (struct scentab *) 0)
		scpush(ep, &sctmp);
	sclist = sctmp;

	/* build the tree */
	TRACE1(tet_Tscen, 1, "proc2sclist(): building the scenario tree");
	skip = 0;
	while ((ep = scpop(&sclist)) != (struct scentab *) 0) {
		if (skip) {
			if (ep->sc_type == SC_SCENARIO)
				skip = 0;
			else
				continue;
		}
		ASSERT(ep->sc_type == SC_SCENARIO);
		if (find2scen(ep->sc_scenario)) {
			scenerror(ep->sc_scenario, "is multiply defined",
				ep->sc_lineno, ep->sc_fname);
			skip = 1;
			continue;
		}
		if (!sctree)
			sctree = ep;
		else { 
			ASSERT(sctmp);
			sctmp->sc_forw = ep; 
			ep->sc_back = sctmp; 
		} 
		sctmp = ep ; 
		if (proc2scen(sctmp) < 0) 
			return(-1);
	}
	if (scenerrors)
		return(0);

	/*
	**	now, perform various checks and manipulations on the tree
	*/

	/* check for valid scenario names in the top row of the tree */
	TRACE1(tet_Tscen, 1, "proc2sclist(): checking scenario names");
	for (ep = sctree; ep; ep = ep->sc_forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		ASSERT(ep->sc_type == SC_SCENARIO);
		check_valid_scen_name(ep);
	}
	if (scenerrors)
		return(0);

	/*
	** traverse the tree:
	**	check for valid test case names
	**	check for duplicate sysids in SD_REMOTE and SD_DISTRIBUTED
	*/
	TRACE1(tet_Tscen, 1,
		"proc2sclist(): checking test case names and sysids");
	for (ep = sctree; ep; ep = ep->sc_forw)
		check_tc_sys(ep->sc_child);
	if (scenerrors)
		return(0);

	/*
	** traverse the tree, resolving referenced scenario names
	** and checking for scenario reference loops
	**
	** each time we pass through a scenario header it is pushed on to
	** the stack at sctmp
	**
	** initially, the stack only contains the header for the scenario
	** that we are checking
	**
	** each time we encounter a referenced scenario name we look
	** back up the stack to see if we have been there before -
	** if we have we report a loop, otherwise we push the curent
	** header on to the stack and continue down the referenced scenario
	** tree
	**
	** the stack is unwound as we come back up the tree
	*/
	TRACE1(tet_Tscen, 1,
		"proc2sclist(): resolving referenced scenario names");
	for (ep = sctree; ep; ep = ep->sc_forw)
		ep->sc_flags &= ~SCF_PROCESSED;
	sctmp = (struct scentab *) 0;
	for (ep = sctree; ep; ep = ep->sc_forw) {
		if (ep->sc_flags & SCF_PROCESSED)
			continue;
		TRACE3(tet_Tscen, 4, "resolve loop TOP: descend tree below scenario %s at %s",
			ep->sc_scenario, tet_i2x(ep));
		scpush(ep, &sctmp);
		resolv_scenptr(ep->sc_child, &sctmp);
		q = scpop(&sctmp);
		ASSERT(q == ep);
		q = scpop(&sctmp);
		ASSERT(q == (struct scentab *) 0);
		ep->sc_flags |= SCF_PROCESSED;
	}
	if (scenerrors)
		return(0);

	/* traverse the tree, fixing up PARALLEL directives in etet mode */
	if (tet_compat != COMPAT_DTET) {
		TRACE1(tet_Tscen, 1,
			"proc2sclist(): fixing up etet-parallel directives");
		for (ep = sctree; ep; ep = ep->sc_forw) {
			TRACE3(tet_Tscen, 4, "etet fix loop TOP: descend tree below scenario %s at %s",
				ep->sc_scenario, tet_i2x(ep));
			etet_fix(ep->sc_child);
		}
		if (scenerrors)
			return(0);
	}

	/*
	** traverse the tree, checking for violations of the
	** directive nesting rules
	**
	** each time we pass through a directive it is pushed on to
	** the stack at sctmp
	**
	** initially the stack is empty
	**
	** each time we find a directive we check to see if the current
	** directive is valid within each of the enclosing directives
	** which are on the stack, then the current directive is pushed
	** on to the stack and we continue down the tree below the
	** current directive
	**
	** the stack is unwound as we come back up the tree
	*/
	TRACE1(tet_Tscen, 1, "proc2sclist(): checking directive nesting rules");
	for (ep = sctree; ep; ep = ep->sc_forw)
		ep->sc_flags &= ~SCF_PROCESSED;
	sctmp = (struct scentab *) 0;
	for (ep = sctree; ep; ep = ep->sc_forw) {
		if (ep->sc_flags & SCF_PROCESSED)
			continue;
		TRACE3(tet_Tscen, 4, "nesting loop TOP: descend tree below scenario %s at %s",
			ep->sc_scenario, tet_i2x(ep));
		check_nesting(ep->sc_child, &sctmp);
		q = scpop(&sctmp);
		ASSERT(q == (struct scentab *) 0);
		ep->sc_flags |= SCF_PROCESSED;
	}
	if (scenerrors)
		return(0);

	/*
	** traverse the tree, checking for empty timed_loops -
	** we do this check otherwise we could use up a LOT of cpu
	** time later
	*/
	TRACE1(tet_Tscen, 1, "proc2sclist(): checking for empty timed loops");
	for (ep = sctree; ep; ep = ep->sc_forw)
		ep->sc_flags &= ~SCF_PROCESSED;
	for (ep = sctree; ep; ep = ep->sc_forw) {
		if (ep->sc_flags & SCF_PROCESSED)
			continue;
		TRACE3(tet_Tscen, 4, "tloop check loop TOP: descend tree below scenario %s at %s",
			ep->sc_scenario, tet_i2x(ep));
		(void) check_timed_loops(ep->sc_child);
	}
	if (scenerrors)
		return(0);

	return(0);
}

/*
**	proc2scen() - build a level in the scenario tree
**
**	return 0 if successful or -1 on error
**	(at present - always returns 0)
*/

#ifdef NOTRACE
#define TRACE_ENTER
#define TRACE_RETURN(RC)	return(RC)
#else

#define TRACE_ENTER \
	level++; \
	TRACE2(tet_Tscen, 9, \
		"proc2scen(): process scenario level %s", tet_i2a(level));

#define TRACE_RETURN(RC) \
	TRACE3(tet_Tscen, 9, "proc2scen(): return %s from level %s", \
		tet_i2a(RC), tet_i2a(level)); \
	--level; \
	return(RC)

#endif /* NOTRACE */

static int proc2scen(parent)
struct scentab *parent;
{
	struct scentab *sctmp;
	register struct scentab *ep;
	int done, rc;

#ifndef NOTRACE
	static int level;
#endif

	TRACE_ENTER;

	/* get the next element from the input and add it to the tree */
	sctmp = (struct scentab *) 0;
	done = 0;
	while (!done && (ep = scpop(&sclist)) != (struct scentab *) 0) {
		TRACE2(tet_Tscen, 4, "proc2scen(): process %s element",
			prsctype(ep->sc_type));
		switch (ep->sc_type) {
		/* tree nodes */
		case SC_SCENARIO:
			/* a new scenario - push it back on the input
			   stream and return */
			scpush(ep, &sclist);
			done = 1;
			continue;
		case SC_DIRECTIVE:
			/* a directive - process elements until the matching
			   end directive is found */
			if ((rc = proc2scdir(ep, parent, &sctmp)) <= 0) {
				TRACE_RETURN(rc);
			}
			break;
		/* leaf nodes */
		case SC_TESTCASE:
		case SC_SCENINFO:
		case SC_SCEN_NAME:
			/* store other elements at this level */
			scstore(ep, parent, &sctmp);
			break;
		default:
			/* this "can't happen" */
			fatal(0, "unexpected type", prsctype(ep->sc_type));
			/* NOTREACHED */
		}
	}

	/*
	** here when we have reached the end of the scenario -
	** report an un-matched directive at the parent level
	*/
	switch (parent->sc_type) {
	case SC_DIRECTIVE:
		report_unmatched(parent->sc_directive,
			parent->sc_lineno, parent->sc_fname);
		break;
	case SC_SCENARIO:
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected type", prsctype(parent->sc_type));
		/* NOTREACHED */
	}

	TRACE_RETURN(0);
}

#undef TRACE_ENTER
#undef TRACE_RETURN


/*
**	proc2scdir() - scenario directive processing in proc2scen()
**
**	return	 1 to continue processing in proc2scen() at
**			the current level
**		 0 to return from proc2scen(), causing processing to
**			go up one level
**		-1 on error
**
**	(at present - always returns 0 or 1)
*/

#ifdef NOTRACE
#define TRACE_ENTER(DIRECTIVE)
#define TRACE_RETURN(RC, TEXT, DIRECTIVE)	return(RC)
#else

#define TRACE_ENTER(DIRECTIVE) \
	TRACE2(tet_Tscen, 4, "proc2scdir(): process scenario directive %s", \
		prscdir(DIRECTIVE));

#define TRACE_RETURN(RC, TEXT, DIRECTIVE) \
	TRACE4(tet_Tscen, 4, \
		"proc2scdir(): return %s after processing %s%s directive", \
		tet_i2a(RC), TEXT, prscdir(DIRECTIVE)); \
	return(RC)

#endif /* NOTRACE */

static int proc2scdir(ep, parent, sctp)
register struct scentab *ep, *parent, **sctp;
{
	register struct dirtab *dp;
	struct scentab *q;
	int directive, rc;

	TRACE_ENTER(ep->sc_directive);

	/* process the directive */
	switch (ep->sc_directive) {
	case SD_PARALLEL:
	case SD_REPEAT:
	case SD_RANDOM:
	case SD_TIMED_LOOP:
	case SD_VARIABLE:
#ifndef TET_LITE	/* -START-LITE-CUT- */
	case SD_REMOTE:
	case SD_DISTRIBUTED:
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
		/* a start directive - store it and go down a level */
		scstore(ep, parent, sctp);
		rc = proc2scen(ep) < 0 ? -1 : 1;
		/* then continue on this level */
		TRACE_RETURN(rc, "", ep->sc_directive);
	case SD_END_PARALLEL:
	case SD_END_REPEAT:
	case SD_END_RANDOM:
	case SD_END_TIMED_LOOP:
	case SD_END_VARIABLE:
#ifndef TET_LITE	/* -START-LITE-CUT- */
	case SD_END_REMOTE:
	case SD_END_DISTRIBUTED:
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
		/* an end directive - if it matches the parent,
			discard it and go up a level */
		dp = getdirbyvalue(ep->sc_directive);
		ASSERT(dp);
		TRACE3(tet_Tscen, 6,
			"checking %s directive for matching %s directive",
			prscdir(ep->sc_directive), prscdir(dp->dt_match));
		if (parent->sc_directive == dp->dt_match) {
			scfree(ep);
			TRACE_RETURN(0, "matched ", dp->dt_match);
		}
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected directive", prscdir(ep->sc_directive));
		/* NOTREACHED */
		return(-1);
	}

	/*
	** here if an end directive does not match the last start directive -
	** see if there is a match further up the tree
	**
	** if there is, push the end directive back on the input list
	** and go up one level - we will find the match next time through
	*/
	TRACE3(tet_Tscen, 6,
		"%s directive not matched by most recent START directive (%s)",
		prscdir(ep->sc_directive), prscdir(parent->sc_directive));
	TRACE1(tet_Tscen, 6, "searching for match");
	for (q = parent->sc_parent; q; q = q->sc_parent) {
		ASSERT(q->sc_magic == SC_MAGIC);
		switch (q->sc_type) {
		case SC_SCENARIO:
			/* reached top of tree */
			ASSERT(q->sc_parent == (struct scentab *) 0);
			continue;
		case SC_DIRECTIVE:
			/* check for match */
			if (q->sc_directive == dp->dt_match) {
				TRACESCELEM(tet_Tscen, 6, q, "match found at");
				report_unmatched(parent->sc_directive,
					parent->sc_lineno, parent->sc_fname);
				scpush(ep, &sclist);
				TRACE_RETURN(0, "un-matched ",
					ep->sc_directive);
			}
			break;
		default:
			/* this "can't happen" */
			fatal(0, "unexpected type", prsctype(ep->sc_type));
			/* NOTREACHED */
		}
	}

	/*
	** here if we have an un-matched end directive -
	** discard it and continue processing at this level
	*/
	TRACE1(tet_Tscen, 6, "match not found");
	directive = ep->sc_directive;
	report_unmatched(directive, ep->sc_lineno, ep->sc_fname);
	scfree(ep);
	TRACE_RETURN(1, "un-matched ", directive);
}

#undef TRACE_ENTER
#undef TRACE_RETURN

/*
**	report_unmatched() - report an un-matched scenario directive
*/

static void report_unmatched(directive, lineno, fname)
int directive, lineno;
char *fname;
{
	static char fmt[] = "found unmatched %.20s directive";
	char msg[sizeof fmt + 20];

	(void) sprintf(msg, fmt, prscdir(directive));
	scenerror(msg, (char *) 0, lineno, fname);
}

/*
**	resolv_scenptr() - resolve referenced scenario names at
**		the current level in the scenario tree
**
**	for each element of type SC_SCEN_NAME, an attempt is made
**	to determine the value of the sc_scenptr member which points
**	to the start of the referenced scenario
**
**	ep points to the first element on this level
**
**	sctp points to a stack containing SC_SCENARIO elements which
**	we have already visited - this is used to detect potential
**	infinite loops in the scenario tree
*/

#ifdef NOTRACE
#define TRACE_ENTER
#define TRACE_RETURN	return
#else

#define TRACE_ENTER \
	level++; \
	TRACE2(tet_Tscen, 6, "resolv_scenptr(): enter at level %s", \
		tet_i2a(level));

#define TRACE_RETURN \
	TRACE2(tet_Tscen, 6, "resolv_scenptr(): return from level %s", \
		tet_i2a(level)); \
	--level; \
	return

#endif /* NOTRACE */

static void resolv_scenptr(ep, sctp)
struct scentab *ep, **sctp;
{

#ifndef NOTRACE
	static int level;
#endif

	TRACE_ENTER;

	/*
	** traverse the tree at this level, resolving referenced
	** scenario names and descending directive trees
	*/
	for (; ep; ep = ep->sc_forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		switch (ep->sc_type) {
		case SC_SCEN_NAME:
			rsc2(ep, sctp);
			break;
		case SC_DIRECTIVE:
			TRACE3(tet_Tscen, 5,
				"descend tree below %s directive at %s",
				prscdir(ep->sc_directive), tet_i2x(ep));
			resolv_scenptr(ep->sc_child, sctp);
			break;
		}
	}

	TRACE_RETURN;
}

#undef TRACE_ENTER
#undef TRACE_RETURN

/*
**	rsc2() - extend the resolv_scenptr() processing for an element
**		which contains a referenced scenario name
*/

static void rsc2(ep, sctp)
register struct scentab *ep;
struct scentab **sctp;
{
	register struct scentab *q;

	/*
	** see if we can resolve the referenced scenario name if
	** this is the first time that we have visited this element
	*/
	if ((ep->sc_flags & SCF_RESOLVED) == 0 && !ep->sc_scenptr) {
		TRACE2(tet_Tscen, 4, "resolve scenario name %s",
			ep->sc_scen_name);
		ep->sc_scenptr = find2scen(ep->sc_scen_name);
		ep->sc_flags |= SCF_RESOLVED;
		if (!ep->sc_scenptr)
			scenerror("unreferenced scenario name",
				ep->sc_scen_name, ep->sc_lineno, ep->sc_fname);
	}

	/* return now if the scenario name is unresolved */
	if (!ep->sc_scenptr)
		return;

	TRACE3(tet_Tscen, 6, "check reference to %s (%s) for a loop",
		tet_i2x(ep->sc_scenptr), ep->sc_scenptr->sc_scenario);

	/* see if we have already followed this scenario reference */
	for (q = *sctp; q; q = q->sc_next) {
		ASSERT(q->sc_magic == SC_MAGIC);
		if (q == ep->sc_scenptr)
			break;
	}

	/*
	** if we have aleady been here:
	**	report a loop and return;
	** otherwise:
	**	push the header of the referenced scenario on the stack
	**	and continue down the referenced scenario
	*/
	if (q) {
		if (q->sc_flags & SCF_ERROR) {
			TRACE2(tet_Tscen, 6, "loop found at %s",
				ep->sc_scen_name);
		}
		else {
			scenerror("referenced scenario loop", ep->sc_scen_name,
				ep->sc_lineno, ep->sc_fname);
			q->sc_flags |= SCF_ERROR;
		}
	}
	else {
		ASSERT(ep->sc_scenptr->sc_type == SC_SCENARIO);
		TRACE3(tet_Tscen, 5, "descend tree below scenario %s at %s",
			ep->sc_scenario, tet_i2x(ep));
		scpush(ep->sc_scenptr, sctp);
		resolv_scenptr(ep->sc_scenptr->sc_child, sctp);
		ep->sc_scenptr->sc_flags |= SCF_PROCESSED;
		q = scpop(sctp);
		ASSERT(q == ep->sc_scenptr);
	}
}

/*
**	find2scen() - return a pointer to the named scenario
**		once the scenario tree has been built,
**		or (struct scentab *) 0 if the scenario is not in the tree
*/

static struct scentab *find2scen(scen_name)
char *scen_name;
{
	register struct scentab *ep;

	for (ep = sctree; ep; ep = ep->sc_forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		ASSERT(ep->sc_type == SC_SCENARIO);
		if (!strcmp(scen_name, ep->sc_scenario))
			break;
	}

	TRACE3(tet_Tscen, 8, "find2scen(%s) returns %s",
		scen_name, tet_i2x(ep));
	return(ep);
}

/*
**	check_valid_scen_name() - check that a scenario name is valid
*/

static void check_valid_scen_name(ep)
struct scentab *ep;
{
	register char *p;
	register int ok;

	TRACE2(tet_Tscen, 6, "check scenrio name %s", ep->sc_scenario);

	/*
	** look for invalid characters -
	** the first character should be one of [A-Za-z_]
	** other characters should be one of [0-9A-Za-z_/.-]
	*/
	p = ep->sc_scenario;
	ok = (isalpha(*p) || *p == '_') ? 1 : 0;
	while (ok && *++p)
		ok = (isalnum(*p) || *p == '_' || *p == '-' || *p == '/' ||
			*p == '.') ? 1 : 0;

	/*
	** report an error if at least one of the characters is not valid;
	** then check for a name that is too long
	*/
	if (!ok)
		scenerror(ep->sc_scenario, "is not a valid scenario name",
			ep->sc_lineno, ep->sc_fname);
	else if ((int) strlen(ep->sc_scenario) > 31)
		scenerror(ep->sc_scenario,
			"is too long for a scenario name (max 31 characters)",
			ep->sc_lineno, ep->sc_fname);
}

/*
**	check_tc_sys() - check test case names and sysid lists at
**		the current level in the tree
*/

#ifdef NOTRACE
#define TRACE_ENTER
#define TRACE_RETURN	return
#else

#define TRACE_ENTER \
	level++; \
	TRACE3(tet_Tscen, 6, "check_tc_sys(%s): enter at level %s", \
		tet_i2x(ep), tet_i2a(level));

#define TRACE_RETURN \
	TRACE2(tet_Tscen, 6, "check_tc_sys(): return from level %s", \
		tet_i2a(level)); \
	--level; \
	return

#endif /* NOTRACE */

static void check_tc_sys(ep)
register struct scentab *ep;
{
	static char *buf, **flds;
	static int buflen, fldlen;
	int n;
	char *p, **fldp;
	static char msg[] = "not valid in test case name";

#ifndef NOTRACE
	static int level;
#endif

	TRACE_ENTER;

	/*
	** traverse the tree at this level, checking testcase names
	** and sysids and descending directive trees
	*/
	for (; ep; ep = ep->sc_forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		switch (ep->sc_type) {
		case SC_TESTCASE:
			if (*ep->sc_tcname != '/') {
				scenerror("badly formed test case name:",
					ep->sc_tcname, ep->sc_lineno,
					ep->sc_fname);
				break;
			}
			n = (int) strlen(ep->sc_tcname) + 1;
			RBUFCHK(&buf, &buflen, n);
			(void) strcpy(buf, ep->sc_tcname);
#if !defined(TET_LITE) || defined(_WIN32)
			for (p = buf; *p; p++)
				if (*p == '\\') {
					scenerror("'\\'", msg,
						ep->sc_lineno, ep->sc_fname);
					break;
				}
#endif
			n = 1;
			for (p = buf; *p; p++)
				if (*p == '/')
					n++;
			RBUFCHK((char **) &flds, &fldlen,
				n * (int) sizeof *flds);
			n = split(buf, flds, n, '/');
			for (fldp = flds; n > 0; fldp++, n--)
				if (!strcmp(*fldp, "..")) {
					scenerror("\"..\"", msg,
						ep->sc_lineno, ep->sc_fname);
					break;
				}
			break;
		case SC_DIRECTIVE:
#ifndef TET_LITE	/* -START-LITE-CUT- */
			switch (ep->sc_directive) {
			case SD_DISTRIBUTED:
			case SD_REMOTE:
				cts2(ep);
				break;
			}
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
			TRACE3(tet_Tscen, 5,
				"descend tree below %s directive at %s",
				prscdir(ep->sc_directive), tet_i2x(ep));
			check_tc_sys(ep->sc_child);
			break;
		}
	}

	/*
	** free any memory allocated here
	** (note that the combination of static variables and
	** recursion can cause some slightly odd behaviour here
	** when a scenario contains test cases on more than one level;
	** but all the memory gets freed on return from the last call)
	*/
	if (buf) {
		TRACE2(tet_Tbuf, 6, "check_tc_sys(): free buf = %s",
			tet_i2x(buf));
		free((void *) buf);
		buf = (char *) 0;
		buflen = 0;
	}

	if (flds) {
		TRACE2(tet_Tbuf, 6, "check_tc_sys(): free flds = %s",
			tet_i2x(flds));
		free((void *) flds);
		flds = (char **) 0;
		fldlen = 0;
	}

	TRACE_RETURN;
}

#undef TRACE_ENTER
#undef TRACE_RETURN


#ifndef TET_LITE	/* -START-LITE-CUT- */

/*
**	cts2() - extend the check_tc_sys() processing when a SD_REMOTE
**		or SD_DISTRIBUTED directive is found
*/

static void cts2(ep)
register struct scentab *ep;
{
	register int *ip1, *ip2;
	register int dups;

	TRACESCELEM(tet_Tscen, 6, ep, "check remote or distributed directive");

	/* look for duplicate sysids and sysid 0 */
	dups = 0;
	for (ip1 = ep->sc_sys; ip1 < ep->sc_sys + ep->sc_nsys; ip1++) {
		for (ip2 = ep->sc_sys; ip2 < ip1; ip2++)
			if (*ip2 == *ip1) {
				dups = 1;
				break;
			}
		if (dups)
			break;
	}

	if (dups)
		scenerror(prscdir(ep->sc_directive),
			"directive contains duplicate system ids",
			ep->sc_lineno, ep->sc_fname);
}

#endif /* !TET_LITE */	/* -END-LITE-CUT- */


/*
**	etet_fix() - fix up etet-parallel directives in the current level
**		in the tree
*/

#ifdef NOTRACE
#define TRACE_ENTER
#define TRACE_RETURN	return
#else

#define TRACE_ENTER \
	level++; \
	TRACE2(tet_Tscen, 6, \
		"etet_fix(): enter at level %s", tet_i2a(level));

#define TRACE_RETURN \
	TRACE2(tet_Tscen, 6, "etet_fix(): return from level %s", \
		tet_i2a(level)); \
	--level; \
	return

#endif /* NOTRACE */

static void etet_fix(ep)
register struct scentab *ep;
{
#ifndef NOTRACE
	static int level;
#endif

	TRACE_ENTER;

	/*
	** traverse the tree at this level, fixing up etet-parallel
	** directives and descending directive trees
	*/
	for (; ep; ep = ep->sc_forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		switch (ep->sc_type) {
		case SC_DIRECTIVE:
			switch (ep->sc_directive) {
			case SD_PARALLEL:
				ep->sc_flags &= ~SCF_ERROR;
				etf2(ep);
				/* fall through */
			default:
				TRACE3(tet_Tscen, 5,
					"descend tree below %s directive at %s",
					prscdir(ep->sc_directive),
					tet_i2x(ep));
				etet_fix(ep->sc_child);
			}
			break;
		}
	}

	TRACE_RETURN;
}

#undef TRACE_ENTER
#undef TRACE_RETURN

/*
**	etf2() - extend the etet_fix() processing for a parallel directive
*/

static void etf2(ep)
struct scentab *ep;
{
	register struct scentab *child;
	register int done;

	TRACE2(tet_Tscen, 6,
		"etf2(): examine the level below the PARALLEL directive at %s",
		tet_i2x(ep));

	/* clear all the PROCESSED flags in the nodes below this directive */
	for (child = ep->sc_child; child; child = child->sc_forw) {
		ASSERT(child->sc_magic == SC_MAGIC);
		child->sc_flags &= ~SCF_PROCESSED;
	}

	/*
	** traverse the tree at the next level down -
	** for each REPEAT, TIMED_LOOP or RANDOM directive or referenced
	** scenario node (i.e., all the ETET directives), push the node
	** down a level and replace the node at this level with an
	** implied SEQUENTIAL directive
	**
	** since a push-down changes the forward pointer chain, we must
	** restart each time the chain is modified;
	** the PROCESSED flag avoids a node being considered for
	** push-down more than once
	*/
	done = 0;
	do {
		done = 1;
		for (child = ep->sc_child; child; child = child->sc_forw) {
			ASSERT(child->sc_magic == SC_MAGIC);
			if (child->sc_flags & SCF_PROCESSED)
				continue;
			switch (child->sc_type) {
			case SC_DIRECTIVE:
				switch (child->sc_directive) {
				case SD_REPEAT:
				case SD_TIMED_LOOP:
				case SD_RANDOM:
					if (etf2_compat(ep)) {
						etf4(child);
						done = 0;
						break;
					}
					/* else fall through */
				default:
					continue;
				}
				break;
			case SC_SCEN_NAME:
				if (etf3(child)) {
					done = 0;
					break;
				}
				/* else fall through */
			default:
				continue;
			}
			if (!done)
				break;
		}
	} while (!done);
}

/*
**	etf3() - extend the etet_fix() processing some more for a referenced
**		scenario node below a parallel directive
**
**	return 1 if the forward chain at this level has been modified
**	or 0 if it hasn't
*/

static int etf3(ep1)
register struct scentab *ep1;
{
	struct scentab *parent = ep1->sc_parent;
	struct scentab *forw, *back;
	register struct scentab *ep2;
	register int found, done;

	TRACE4(tet_Tscen, 6, "etf3(): examine the top level of referenced scenario node %s which points to %s (%s)",
		tet_i2x(ep1), tet_i2x(ep1->sc_scenptr),
		ep1->sc_scenptr->sc_scenario);

	/*
	** search the top level of the referenced scenario for nodes
	** containing ETET directives and referenced scenario names
	*/
	found = 0;
	for (ep2 = ep1->sc_scenptr->sc_child; ep2; ep2 = ep2->sc_forw) {
		ASSERT(ep2->sc_magic == SC_MAGIC);
		switch (ep2->sc_type) {
		case SC_DIRECTIVE:
			switch (ep2->sc_directive) {
			case SD_REPEAT:
			case SD_TIMED_LOOP:
			case SD_RANDOM:
				found = 1;
				break;
			}
			break;
		case SC_SCEN_NAME:
			found = 1;
			break;
		}
		if (found)
			break;
	}

	/*
	** return without changing the forward chain if no such nodes
	** have been found or we are in DTET mode
	*/
	if (!found || !etf2_compat(parent)) {
		TRACE1(tet_Tscen, 6,
			"etf3(): RETURN 0 without changing forward chain");
		return(0);
	}

	/*
	** here if we are in ETET compatibility mode and at least one ETET
	** directive or referenced scenario node appears at the top of the
	** referenced scenario -
	** remember the neighbours of this node and replace it with a copy
	** of the referenced scenario
	*/
	back = ep1->sc_back;
	forw = ep1->sc_forw;
	copy_refscen(ep1, parent);

	/*
	** then, push down each ETET directive and referenced scenario
	** node within the scope of the replaced referenced scenario node
	** and replace it with an implied SEQUENTIAL directive
	**
	** since a push-down changes the forward pointer chain, we must
	** restart each time the chain is modified;
	** the PROCESSED flag avoids a node being considered for
	** push-down more than once
	*/
	done = 0;
	do {
		done = 1;
		for (ep2 = back ? back->sc_forw : parent->sc_child; ep2 && ep2 != forw; ep2 = ep2->sc_forw) {
			ASSERT(ep2->sc_magic == SC_MAGIC);
			if (ep2->sc_flags & SCF_PROCESSED)
				continue;
			switch (ep2->sc_type) {
			case SC_DIRECTIVE:
				switch (ep2->sc_directive) {
				case SD_REPEAT:
				case SD_TIMED_LOOP:
				case SD_RANDOM:
					break;
				default:
					continue;
				}
				/* fall through */
			case SC_SCEN_NAME:
				etf4(ep2);
				done = 0;
				break;
			default:
				continue;
			}
			if (!done)
				break;
		}
	} while (!done);

	TRACE1(tet_Tscen, 6, "etf3(): RETURN 1 after performing ETET fix");
	return(1);
}

/*
**	etf4() - extend the etet_fix() processing even more
**
**	ep1 points to an ETET directive or a referenced scenario node
**
**	the node is pushed down a level and is replaced by a node containing
**	an implied SEQUENTIAL directive
*/

static void etf4(ep1)
register struct scentab *ep1;
{
	register struct scentab *ep2;
#ifndef NOTRACE
	static char fmt[] = "etf4(): inserting implied sequential directive above the %.32s %.16s at";
	char msg[sizeof fmt + 32 + 16];
	char *s1, *s2;
#endif

	/*
	** see if a parallel directive has more than one element within
	** its scope and emit a warning if it hasn't
	*/
	if (
		ep1->sc_parent->sc_type == SC_DIRECTIVE &&
		ep1->sc_parent->sc_directive == SD_PARALLEL &&
		ep1->sc_parent->sc_count < 2 &&
		ep1->sc_forw == (struct scentab *) 0 &&
		ep1->sc_back == (struct scentab *) 0
	) {
		scenermsg("warning: parallel directive has no effect",
			(char *) 0, ep1->sc_parent->sc_lineno,
			ep1->sc_parent->sc_fname);
	}

#ifndef NOTRACE
	if (ep1->sc_type == SC_DIRECTIVE) {
		s1 = prscdir(ep1->sc_directive);
		s2 = "directive";
	}
	else {
		s1 = prsctype(ep1->sc_type);
		s2 = "element";
	}
	(void) sprintf(msg, fmt, s1, s2);
	TRACESCELEM(tet_Tscen, 6, ep1, msg);
#endif

	/* add the implied sequential directive */
	ep2 = scalloc();
	ep2->sc_type = SC_DIRECTIVE;
	ep2->sc_directive = SD_SEQUENTIAL;
	ep2->sc_flags |= SCF_IMPLIED | SCF_PROCESSED;
	ep2->sc_fname = ep1->sc_fname;
	ep2->sc_lineno = ep1->sc_lineno;
	ep2->sc_forw = ep1->sc_forw;
	ep2->sc_back = ep1->sc_back;
	ep2->sc_parent = ep1->sc_parent;
	ep2->sc_child = ep1;
	if (ep1->sc_forw) {
		ep1->sc_forw->sc_back = ep2;
		ep1->sc_forw = (struct scentab *) 0;
	}
	if (ep1->sc_back) {
		ep1->sc_back->sc_forw = ep2;
		ep1->sc_back = (struct scentab *) 0;
	}
	if (ep1->sc_parent->sc_child == ep1)
		ep1->sc_parent->sc_child = ep2;
	ep1->sc_flags |= SCF_PROCESSED;

	TRACESCELEM(tet_Tscen, 6, ep2, "etf4(): new directive is at");
}

/*
**	eft2_compat() - see if a PARALLEL directive should be fixed up
**		by etet_fix()
**
**	return 1 if it should or 0 if it should not
**
**	a diagnostic is printed if the compatibility mode has not been set
*/

static int etf2_compat(ep)
struct scentab *ep;
{
	switch (tet_compat) {
	case COMPAT_DTET:
		return(0);
	case COMPAT_ETET:
		return(1);
	default:
		if ((ep->sc_flags & SCF_ERROR) == 0) {
			scenerror("TET_COMPAT must be set to a valid value",
				"in order to interpret parallel directive correctly",
				ep->sc_lineno, ep->sc_fname);
			ep->sc_flags |= SCF_ERROR;
		}
		return(0);
	}
}

/*
**	check_nesting() - enforce the nesting rules at the current level
**		in the tree
*/

#ifdef NOTRACE
#define TRACE_ENTER
#define TRACE_RETURN	return
#else

#define TRACE_ENTER \
	level++; \
	TRACE2(tet_Tscen, 6, \
		"check_nesting(): enter at level %s", tet_i2a(level));

#define TRACE_RETURN \
	TRACE2(tet_Tscen, 6, "check_nesting(): return from level %s", \
		tet_i2a(level)); \
	--level; \
	return

#endif /* NOTRACE */

static void check_nesting(ep, sctp)
register struct scentab *ep;
struct scentab **sctp;
{
	struct scentab *q;

#ifndef NOTRACE
	static int level;
#endif

	TRACE_ENTER;

	/*
	** traverse the tree at this level, checking for nested directives
	** and descending directive and referenced scenario trees;
	**
	** it's OK to jump straight into a referenced scenario tree
	** because we have already checked for scenario loops
	*/
	for (; ep; ep = ep->sc_forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		switch (ep->sc_type) {
		case SC_SCEN_NAME:
			TRACE3(tet_Tscen, 5, "descend tree below referenced scenario %s at %s",
				ep->sc_scenario, tet_i2x(ep->sc_scenptr));
			check_nesting(ep->sc_scenptr->sc_child, sctp);
			ep->sc_scenptr->sc_flags |= SCF_PROCESSED;
			break;
		case SC_DIRECTIVE:
			chn2(ep, sctp);
			TRACE3(tet_Tscen, 5,
				"descend tree below %s directive at %s",
				prscdir(ep->sc_directive), tet_i2x(ep));
			scpush(ep, sctp);
			check_nesting(ep->sc_child, sctp);
			q = scpop(sctp);
			ASSERT(q == ep);
			break;
		}
	}

	TRACE_RETURN;
}

#undef TRACE_ENTER
#undef TRACE_RETURN

static void chn2(ep, sctp)
struct scentab *ep;
struct scentab **sctp;
{
	register struct scentab *q;
	register struct dirtab *dp;
	register int *edp;
	struct scentab *seq;
	static char fmt[] = "\tmay not enclose %.32s";
	char msg[sizeof fmt + 32];

	TRACE2(tet_Tscen, 6, "check nesting rules for %s directive",
		prscdir(ep->sc_directive));

	/*
	** look back up the stack of enclosing directives;
	** report an error if a directive is found which is NOT in the
	** list of valid enclosing directives for this directive
	**
	** note that we will report an error more than once if:
	**	scenarios scen1 and scen2 appear (in this order)
	**	in the scenario file, AND
	**	the directive foo is not valid within the scope of
	**	the directive bar, AND
	**	scen1 contains foo within the scope of bar, AND
	**	scen2 contains a reference to scen1
	**
	** a fix might be to maintain a list of directive pairs which we
	** have already reported, but this seems a bit over-the-top :-(
	*/
	seq = (struct scentab *) 0;
	for (q = *sctp; q; q = q->sc_next) {
		ASSERT(q->sc_magic == SC_MAGIC);
		switch (q->sc_directive) {
		case SD_PARALLEL:
			if (seq) {
				dp = getdirbyvalue(seq->sc_directive);
				seq = (struct scentab *) 0;
			}
			else
				dp = getdirbyvalue(q->sc_directive);
			break;
		case SD_SEQUENTIAL:
			seq = q;
			/* fall through */
		default:
			dp = getdirbyvalue(q->sc_directive);
			break;
		}
		ASSERT(dp);
		ASSERT(dp->dt_enc || !dp->dt_nenc);
		for (edp = dp->dt_enc; edp < dp->dt_enc + dp->dt_nenc; edp++)
			if (ep->sc_directive == *edp)
				break;
		if (edp >= dp->dt_enc + dp->dt_nenc) {
			scenermsg(prscdir(q->sc_directive), "directive",
				q->sc_lineno, q->sc_fname);
			(void) sprintf(msg, fmt, prscdir(ep->sc_directive));
			scenerror(msg, "directive", ep->sc_lineno,
				ep->sc_fname);
		}
	}
}

/*
**	check_empty_timed_loops() - call check_timed_loop() from main()
**		after the scenario tree has been reduced to a single scenario
*/

void check_empty_timed_loops()
{
	(void) check_timed_loops(sctree->sc_child);
	if (scenerrors)
		scengiveup();
}

/*
**	check_timed_loops() - check for empty timed loops
**
**	return the number of test cases found at this level and below
*/

#ifdef NOTRACE
#define TRACE_ENTER
#define TRACE_RETURN(RC)	return(RC)
#else

#define TRACE_ENTER \
	level++; \
	TRACE2(tet_Tscen, 6, \
		"check_timed_loops(): enter at level %s", tet_i2a(level));

#define TRACE_RETURN(RC) \
	TRACE3(tet_Tscen, 6, "check_timed_loops(): return %s from level %s", \
		tet_i2a(RC), tet_i2a(level)); \
	--level; \
	return(RC)

#endif /* NOTRACE */

static int check_timed_loops(ep)
register struct scentab *ep;
{
	register int n, count = 0;

#ifndef NOTRACE
	static int level;
#endif

	TRACE_ENTER;

	/*
	** traverse the tree at this level, counting test cases, checking
	** for empty timed loops and descending directive and
	** referenced scenario trees
	**
	** it's OK to jump straight into a referenced scenario tree
	** because we have already checked for scenario loops
	*/
	for (; ep; ep = ep->sc_forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		switch (ep->sc_type) {
		case SC_SCEN_NAME:
			TRACE3(tet_Tscen, 5, "descend tree below referenced scenario %s at %s",
				ep->sc_scenario, tet_i2x(ep->sc_scenptr));
			count += check_timed_loops(ep->sc_scenptr->sc_child);
			ep->sc_scenptr->sc_flags |= SCF_PROCESSED;
			break;
		case SC_DIRECTIVE:
			TRACE3(tet_Tscen, 5,
				"descend tree below %s directive at %s",
				prscdir(ep->sc_directive), tet_i2x(ep));
			if ((n = check_timed_loops(ep->sc_child)) == 0 &&
				ep->sc_directive == SD_TIMED_LOOP)
					scenerror("no test cases found within",
						"the scope of a timed loop",
					ep->sc_lineno, ep->sc_fname);
			count += n;
			break;
		case SC_TESTCASE:
			count++;
			break;
		}
	}

	TRACE_RETURN(count);
}

#undef TRACE_ENTER
#undef TRACE_RETURN

