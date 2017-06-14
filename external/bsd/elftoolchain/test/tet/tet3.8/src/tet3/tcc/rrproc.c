/*
 *	SCCS: @(#)rrproc.c	1.8 (02/01/18)
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
static char sccsid[] = "@(#)rrproc.c	1.8 (02/01/18) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)rrproc.c	1.8 02/01/18 TETware release 3.8
NAME:		rrproc.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	September 1996

DESCRIPTION:
	tcc rerun and resume processing functions

MODIFICATIONS:
	Geoff Clare, UniSoft Ltd., Oct 1996
	Added <string.h>.

	Andrew Dingwall, UniSoft Ltd., June 1997
	fixed a problem with the way in which the resume_iclist is
	extended after a resume point is found part-way through a test
	case in EXEC mode

	Andrew Dingwall, UniSoft Ltd., October 1997
	added a work-around for LINUX getopt() which scribbles on the argv
	that is passed to it

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <time.h>
#include <ctype.h>
#include <errno.h>
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "error.h"
#include "ltoa.h"
#include "dtetlib.h"
#include "scentab.h"
#include "dirtab.h"
#include "tcc.h"
#include "tet_jrnl.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* rescodes and modes of operation from the command-line */
static int *rrcodes;
static int lrrcodes, nrrcodes;
static int rrmodes;

/* name and handle for the old journal file */
static char *jfname;
static FILE *jfp;

/*
** the scenario resume point when in RESUME mode
**
** information about where to resume a loop is stored in the related
** loop directive node in the scenario tree
*/
struct scentab *resume_scen;	/* the resume point */
int resume_mode;		/* mode in which to resume */
char *resume_iclist;		/* iclist to use when resuming in EXEC mode */
int resume_found;		/* set by the execution engine when the resume
				   point has been found */

/* structure of a broken out journal line */
struct jline {
	long jl_lineno;			/* line number in the journal file */
	long jl_offset;			/* offset of line from start of file */
	int jl_id;			/* the line's ID from field 1 */
	char *jl_flds2[5];		/* subfields in field 2 */
	char *jl_fld3;			/* the text field */
	long jl_ref;			/* reference from scenario ref */
	int jl_sys;			/* sysid from scenario ref */
};


/* static function declarations */
static void badynopt PROTOLIST((int));
static void clear_exiclists PROTOLIST((struct scentab *));
static void clear_itcount PROTOLIST((struct scentab *));
static void clear_scflags PROTOLIST((struct scentab *, int));
static void exiclist_addupdate PROTOLIST((struct scentab *, int));
static void exiclist_set PROTOLIST((struct scentab *, char *));
static char *prjnlid PROTOLIST((int));
static void remove_unneeded_tcs PROTOLIST((struct scentab *));
static void rrp_checkloopstart PROTOLIST((struct scentab *, struct jline *));
static int rrp_cl2 PROTOLIST((struct scentab *, struct scentab *, int));
static struct scentab *rrp_findscen PROTOLIST((struct jline *));
static struct scentab *rrp_fs2 PROTOLIST((struct scentab *, long));
static struct jline *rrp_getline PROTOLIST((char *));
static int rrp_procendline PROTOLIST((struct scentab *, struct jline *, int));
static int rrp_procic PROTOLIST((struct scentab *, int, char *, int));
static int rrp_proctc PROTOLIST((struct scentab *, char *, int));
static int rrp_proctp PROTOLIST((char *, int));
static void rrp_tccstart PROTOLIST((struct jline *));
static void rrp_tcsync PROTOLIST((struct scentab *, struct jline *));
static char **rrp_ts2 PROTOLIST((char *, int *));
static void set_scflags PROTOLIST((struct scentab *, int));


/*
**	rrproc() - prune the scenario tree in rerun or resume mode
*/

void rrproc(codelist, old_journal_file)
char *codelist, *old_journal_file;
{
	register char *p, **ap;
	register int n;
	int rrerrors = 0;
	char **argv = (char **) 0;
	int largv = 0;
	int argc, sel;
	char buf[LBUFLEN];
	struct jline *jlp;
	struct scentab *ep;

	jfname = old_journal_file;

	TRACE4(tet_Ttcc, 4, "rrproc(): rerun/resume = %s, codelist = \"%s\", old journal file = \"%s\"",
		prtccmode(tcc_modes & (TCC_RERUN | TCC_RESUME)),
		codelist, jfname);

	/*
	**	decode the codelist from the command-line
	*/

	/* count the comma-separated fields */
	n = 1;
	for (p = codelist; *p; p++)
		if (*p == ',')
			n++;

	/*
	** get an argv big enough for all the fields
	** and split the codelist into fields
	*/
	RBUFCHK((char **) &argv, &largv, n * sizeof *argv);
	(void) sprintf(buf, "%.*s", (int) sizeof buf - 1, codelist);
	argc = split(buf, argv, n, ',');

	/*
	** process each field in the codelist -
	** if it a mode of operation, set the corresponding bit in rrmodes;
	** otherwise, if it is a valid result code name, append the
	** corresponding result code value to the list at *rrcodes
	*/
	for (ap = argv; ap < argv + argc; ap++) {
		if (**ap == 'b' && *(*ap + 1) == '\0')
			rrmodes |= TCC_BUILD;
		else if (**ap == 'e' && *(*ap + 1) == '\0')
			rrmodes |= TCC_EXEC;
		else if (**ap == 'c' && *(*ap + 1) == '\0')
			rrmodes |= TCC_CLEAN;
		else if ((n = tet_getrescode(*ap, (int *) 0)) < 0) {
			error(0, *ap, "is not a valid result code name or mode of operation");
			rrerrors++;
		}
		else {
			RBUFCHK((char **) &rrcodes, &lrrcodes,
				(int) ((nrrcodes + 1) * sizeof *rrcodes));
			*(rrcodes + nrrcodes++) = n;
		}
	}

	/* free the argv */
	TRACE2(tet_Tbuf, 6, "free codelist argv = %s", tet_i2x(argv));
	free((char *) argv);
	largv = argc = 0;
	argv = (char **) 0;

	/* exit now if there have been any codelist errors */
	if (rrerrors)
		tcc_exit(2);

#ifndef NOTRACE
	TRACE3(tet_Ttcc, 8, "rrmodes = %s, nrrcodes = %s",
		prtccmode(rrmodes), tet_i2a(nrrcodes));
	if (rrcodes && tet_Ttcc >= 8) {
		int *ip;
		for (ip = rrcodes; ip < rrcodes + nrrcodes; ip++)
			TRACE3(tet_Ttcc, 8, "rrcode %s = %s",
				tet_i2a(*ip), tet_getresname(*ip, (int *) 0));
	}
#endif

	/*
	**	process the old journal file
	*/

	/* open the old journal file */
	if ((jfp = fopen(jfname, "r")) == (FILE *) 0)
		fatal(errno, "can't open", jfname);

	/* read the first line in the file - should be a TCC Start line */
	if (
		(jlp = rrp_getline(buf)) == (struct jline *) 0 ||
		jlp->jl_id != TET_JNL_TCC_START
	) {
		(void) fprintf(stderr, "TCC: old journal file does not start with a TCC Start line: %s\n",
			jfname);
		tcc_exit(1);
	}

	/*
	** inspect the command line arguments on the TCC start line
	** in the old journal file
	*/
	rrp_tccstart(jlp);

	/* clear the NEEDED flag in each node in the scenario tree */
	clear_scflags(sctree->sc_child, SCF_NEEDED);

	/* clear the iteration counts in each looping directive in the tree */
	clear_itcount(sctree->sc_child);

	/*
	** in RERUN mode, clear the default exec IC list in each test case
	** node in the scenario tree;
	** the exec IC lists will be rebuilt later to reflect the ICs
	** which should actually be rerun
	*/
	if (tcc_modes & TCC_RERUN)
		clear_exiclists(sctree->sc_child);

	/* read each journal line in turn and process it */
	sel = 0;
	while ((jlp = rrp_getline(buf)) != (struct jline *) 0) {
		switch (jlp->jl_id) {
		case TET_JNL_BUILD_START:
		case TET_JNL_INVOKE_TC:
		case TET_JNL_CLEAN_START:
			ep = rrp_findscen(jlp);
			rrp_checkloopstart(ep, jlp);
			rrp_tcsync(ep, jlp);
			if ((tcc_modes & TCC_RESUME) == 0 || !resume_scen)
				sel += rrp_proctc(ep, buf, jlp->jl_id);
			break;
		case TET_JNL_BUILD_END:
		case TET_JNL_TC_END:
		case TET_JNL_CLEAN_END:
		case TET_JNL_IC_START:
		case TET_JNL_IC_END:
		case TET_JNL_TP_START:
		case TET_JNL_TP_RESULT:
			if ((tcc_modes & TCC_RESUME) == 0 || !resume_scen) {
				(void) fprintf(stderr, "TCC: found %s outside the scope of a Test Case at line %ld in old journal file %s\n",
					prjnlid(jlp->jl_id), jlp->jl_lineno,
					jfname);
				tcc_exit(1);
			}
			break;
		case TET_JNL_RPT_START:
		case TET_JNL_TLOOP_START:
			ep = rrp_findscen(jlp);
			clear_itcount(ep->sc_child);
			break;
		case TET_JNL_SEQ_START:
		case TET_JNL_RND_START:
		case TET_JNL_PRL_START:
		case TET_JNL_VAR_START:
#ifndef TET_LITE	/* -START-LITE-CUT- */
		case TET_JNL_RMT_START:
		case TET_JNL_DIST_START:
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
		case TET_JNL_RPT_END:
		case TET_JNL_SEQ_END:
		case TET_JNL_RND_END:
		case TET_JNL_PRL_END:
		case TET_JNL_VAR_END:
		case TET_JNL_TLOOP_END:
#ifndef TET_LITE	/* -START-LITE-CUT- */
		case TET_JNL_RMT_END:
		case TET_JNL_DIST_END:
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
			(void) rrp_findscen(jlp);
			break;
		}
	}

	(void) fclose(jfp);

	/*
	** see if any test cases have been selected (in RERUN mode) or if
	** a resume point has been found (in RESUME mode)
	*/
	if ((tcc_modes & TCC_RERUN) && !sel) {
		(void) printf("TCC: rerun codelist does not select any test cases to rerun\n");
		(void) fflush(stdout);
		tcc_exit(0);
	}
	if ((tcc_modes & TCC_RESUME) && !resume_scen) {
		(void) printf("TCC: resume codelist does not identify a resume point in the scenario\n");
		(void) fflush(stdout);
		tcc_exit(0);
	}

	/* remove all of the un-needed test cases from the scenario tree */
	TRACE1(tet_Ttcc, 4,
		"rrproc(): remove un-needed test cases from the scenario tree");
	remove_unneeded_tcs(sctree->sc_child);

	/* free allocated memory and return */
	if (rrcodes) {
		TRACE2(tet_Tbuf, 6, "free rrcodes = %s", tet_i2x(rrcodes));
		free((char *) rrcodes);
		rrcodes = (int *) 0;
		lrrcodes = nrrcodes = 0;
	}

	TRACE1(tet_Ttcc, 4, "rrproc() RETURN");
}

/*
**	rrp_tccstart() - process the TCC start line in the old journal file
*/

static void rrp_tccstart(jlp)
struct jline *jlp;
{
	int argc, c, modes;
	char **argv, **ap1, **ap2;
	char **gargv = (char **) 0;
	int lgargv = 0;
	extern int optind;
	extern char *optarg;

	/* ensure that the tcc version number matches our version number */
	if (strcmp(jlp->jl_flds2[0], tcc_version)) {
		(void) fprintf(stderr, "TCC version %s: old journal file was generated by a different version of tcc (%s)\n",
			tcc_version, jlp->jl_flds2[0]);
		tcc_exit(1);
	}

	/*
	** get the tcc command line arguments -
	** ensure that this is not a RESUME mode journal;
	** get the modes of operation and any -y or -n options from the journal
	**
	** we must pass a copy of the argv to getopt(), since getopt()
	** modifies some of the pointers in the argv on LINUX (at least);
	** also, getopt() on LINUX seems to modify *(argv - 1),
	** so the copy must include an extra element on the front of the argv
	*/
	argc = 0;
	argv = rrp_ts2(jlp->jl_fld3, &argc);
	RBUFCHK((char **) &gargv, &lgargv,
		(int) ((argc + 2) * sizeof *gargv));
	ap2 = gargv;
	*ap2++ = (char *) 0;
	for (ap1 = argv; ap1 < argv + argc; ap1++)
		*ap2++ = *ap1;
	*ap2 = (char *) 0;
	modes = 0;
	optind = 1;
	while ((c = GETOPT(argc, gargv + 1, tcc_options)) != EOF)
		switch (c) {
		case 'b':
			modes |= TCC_BUILD;
			break;
		case 'c':
			modes |= TCC_CLEAN;
			break;
		case 'e':
			modes |= TCC_EXEC;
			break;
		case 'm':
			if (tcc_modes & TCC_RESUME) {
				(void) fprintf(stderr, "TCC: can't RESUME using an old journal file which has been generated by tcc in RESUME mode\n");
				tcc_exit(1);
			}
			break;
		case 'n':
			if (*optarg)
				nostr(optarg, YN_OJFILE);
			else
				badynopt(c);
			break;
		case 'y':
			if (*optarg)
				yesstr(optarg, YN_OJFILE);
			else
				badynopt(c);
			break;
		case '?':
			(void) fprintf(stderr, "TCC: TCC Start line in old journal file contains an invalid or incomplete command-line option\n");
			tcc_exit(1);
			break;
		}

	/*
	** in RESUME mode, check that the operation modes in the journal match
	** the current set
	*/
	if (
		(tcc_modes & TCC_RESUME) &&
		(tcc_modes & (TCC_BUILD | TCC_EXEC | TCC_CLEAN)) != modes
	) {
		(void) fprintf(stderr, "TCC: set of operation modes in old journal file (%s) does not match the set selected for this run (%s)\n",
			prtccmode(modes),
			prtccmode(tcc_modes & (TCC_BUILD | TCC_EXEC | TCC_CLEAN)));
		tcc_exit(1);
	}

	/* free the storage allocated in this function */
	TRACE2(tet_Tbuf, 6, "free gargv = %s", tet_i2x(gargv));

	/* free the storage allocated by rrp_ts2() */
	for (ap1 = argv; ap1 < argv + argc; ap1++)
		if (*ap1) {
			TRACE2(tet_Tbuf, 6, "free rrp_ts2 arg = %s",
				tet_i2x(*ap1));
			free(*ap1);
		}
	TRACE2(tet_Tbuf, 6, "free rrp_ts2 argv = %s", tet_i2x(argv));
	free((char *) argv);

	/*
	** finally, prune the scenario tree using -y/-n options from the
	** old journal file
	*/
	ynproc(YN_OJFILE);
}

/*
**	rrp_ts2() - extend the rrp_tccstart() processing
**
**	fld3 points to the 3rd field of the TCC Start line
**
**	the return value points to an argv which contains the tcc
**	command-line arguments;
**	the number of arguments is returned indirectly through *argcp
*/

static char **rrp_ts2(fld3, argcp)
char *fld3;
int *argcp;
{
	static char key[] = "Command line: ";
	register char *p1, *p2;
	register int new, quotes;
	register char **ap;
	char **argv = (char **) 0;
	int largv = 0;
	int nargv;
	char buf[TET_JNL_LEN];

	/* find the start of the tcc command line */
	for (p1 = fld3; *p1; p1++)
		if (!strncmp(p1, key, sizeof key - 1))
			break;
	if (!*p1) {
		(void) fprintf(stderr, "TCC: can't find tcc command in TCC Start line in old journal file\n");
		tcc_exit(1);
	}

	/* count the number of words in the command-line */
	p1 += sizeof key - 1;
	nargv = 1;
	for (p2 = p1; *p2; p2++)
		if (isspace(*p2))
			nargv++;

	/* allocate memory for the argv */
	RBUFCHK((char **) &argv, &largv, (int) (nargv * sizeof *argv));
	ap = argv;

	/*
	** build the argv from the command line extracted from the old
	** journal file, interpreting the escape and quoting conventions
	** used when the line was written
	*/
	new = 1;
	quotes = 0;
	for (p2 = buf; *p1; p1++) {
		if (p2 >= &buf[sizeof buf - 2]) {
			if (ap < argv + nargv)
				*ap++ = rstrstore(buf);
			p2 = buf;
			new = 1;
			continue;
		}
		if (*p1 == '\\' && *(p1 + 1)) {
			*p2++ = *++p1;
			new = 0;
			continue;
		}
		if (!quotes && isspace(*p1)) {
			if (!new) {
				*p2 = '\0';
				if (ap < argv + nargv)
					*ap++ = rstrstore(buf);
				p2 = buf;
				new = 1;
			}
			continue;
		}
		new = 0;
		if (*p1 == '"')
			quotes = !quotes;
		else
			*p2++ = *p1;
	}

	if (!new) {
		*p2 = '\0';
		if (ap < argv + nargv)
			*ap++ = rstrstore(buf);
	}

	*argcp = ap - argv;
	return(argv);
}

/*
**	badynopt() - report a bad -y or -n option in the old journal file
*/

static void badynopt(c)
int c;
{
	(void) fprintf(stderr, "TCC: TCC Start line in old journal file contains a bad format -%c option\n", c);
	tcc_exit(1);
}

/*
**	rrp_findscen() - return a pointer to the scenario element
**		which matches the journal line scenario reference
*/

static struct scentab *rrp_findscen(jlp)
struct jline *jlp;
{
	register struct scentab *ep;
	int sctype, scdir;
	char *directive;

	/* find the scenario element corresponding to the scenario reference */
	if ((ep = rrp_fs2(sctree->sc_child, jlp->jl_ref)) == (struct scentab *) 0) {
		(void) fprintf(stderr, "TCC: the specified scenario does not contain an element which corresponds to reference %ld-%d at line %ld in old journal file %s\n",
			jlp->jl_ref, jlp->jl_sys, jlp->jl_lineno, jfname);
		tcc_exit(1);
	}

	/*
	** determine what type of scenario element corresponds to this
	** journal line id
	*/
	switch (jlp->jl_id) {
	case TET_JNL_INVOKE_TC:
	case TET_JNL_TC_END:
	case TET_JNL_BUILD_START:
	case TET_JNL_BUILD_END:
	case TET_JNL_CLEAN_START:
	case TET_JNL_CLEAN_END:
		sctype = SC_TESTCASE;
		scdir = -1;
		break;
	case TET_JNL_PRL_START:
	case TET_JNL_PRL_END:
		sctype = SC_DIRECTIVE;
		scdir = SD_PARALLEL;
		break;
	case TET_JNL_SEQ_START:
	case TET_JNL_SEQ_END:
		sctype = SC_DIRECTIVE;
		scdir = SD_SEQUENTIAL;
		break;
	case TET_JNL_VAR_START:
	case TET_JNL_VAR_END:
		sctype = SC_DIRECTIVE;
		scdir = SD_VARIABLE;
		break;
	case TET_JNL_RPT_START:
	case TET_JNL_RPT_END:
		sctype = SC_DIRECTIVE;
		scdir = SD_REPEAT;
		break;
	case TET_JNL_TLOOP_START:
	case TET_JNL_TLOOP_END:
		sctype = SC_DIRECTIVE;
		scdir = SD_TIMED_LOOP;
		break;
	case TET_JNL_RND_START:
	case TET_JNL_RND_END:
		sctype = SC_DIRECTIVE;
		scdir = SD_RANDOM;
		break;
#ifndef TET_LITE	/* -START-LITE-CUT- */
	case TET_JNL_RMT_START:
	case TET_JNL_RMT_END:
		sctype = SC_DIRECTIVE;
		scdir = SD_REMOTE;
		break;
	case TET_JNL_DIST_START:
	case TET_JNL_DIST_END:
		sctype = SC_DIRECTIVE;
		scdir = SD_DISTRIBUTED;
		break;
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
	default:
		/* this "can't happen" */
		fatal(0, "journal line id unexpected:", prjnlid(jlp->jl_id));
		/* NOTREACHED */
		return((struct scentab *) 0);
	}

	/*
	** ensure that the scenario element matches the journal id -
	** if it doesn't, it is likely that the old journal file was not
	** generated from this scenario
	*/
	if (
		sctype != ep->sc_type ||
		(sctype == SC_DIRECTIVE && scdir != ep->sc_directive)
	) {
		if (ep->sc_type == SC_DIRECTIVE)
			if ((ep->sc_flags & SCF_IMPLIED) &&
				ep->sc_directive == SD_SEQUENTIAL)
					directive = "implied sequential";
			else
				directive = prscdir(ep->sc_directive);
		else
			directive = "";
		(void) fprintf(stderr, "TCC: %s line %ld in old journal file %s\n",
			prjnlid(jlp->jl_id), jlp->jl_lineno, jfname);
		(void) fprintf(stderr, "does not match %s%s%s element at line %d in scenario file %s\n",
			directive, (ep->sc_type == SC_DIRECTIVE) ? " " : "",
			prsctype(ep->sc_type), ep->sc_lineno, ep->sc_fname);
		tcc_exit(1);
	}

	return(ep);
}

/*
**	rrp_fs2() - extend the rrp_findscen processing
**
**	search the scenario tree at this level and below for an element which
**	matches the specified reference
*/

static struct scentab *rrp_fs2(ep, ref)
register struct scentab *ep;
register long ref;
{
	register struct scentab *ep2;

	for (; ep; ep = ep->sc_forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		if (ep->sc_ref == ref)
			return(ep);
		switch (ep->sc_type) {
		case SC_DIRECTIVE:
			if ((ep2 = rrp_fs2(ep->sc_child, ref)) != (struct scentab *) 0)
				return(ep2);
			break;
		}
	}

	return((struct scentab *) 0);
}

/*
**	rrp_tcsync() - check that the test case name on a BUILD/TC/CLEAN
**		start line matches the test case name in the corresponding
**		scenario element
**
**	there is no return if a mismatch is found - it is likely that the
**	old journal file was not generated from the current scenario
*/

static void rrp_tcsync(ep, jlp)
struct scentab *ep;
struct jline *jlp;
{
	int is_testcase_start_id = 0;

	switch (jlp->jl_id) {
	case TET_JNL_BUILD_START:
	case TET_JNL_INVOKE_TC:
	case TET_JNL_CLEAN_START:
		is_testcase_start_id = 1;
	}

	ASSERT(ep->sc_type == SC_TESTCASE);
	ASSERT(is_testcase_start_id);

	if (strcmp(ep->sc_tcname, jlp->jl_flds2[1])) {
		(void) fprintf(stderr, "TCC: the test case name (%s) on %s line %ld in old journal file %s\n",
			jlp->jl_flds2[1], prjnlid(jlp->jl_id),
			jlp->jl_lineno, jfname);
		(void) fprintf(stderr, "does not match the test case name (%s) at line %d of scenario file %s\n",
			ep->sc_tcname, ep->sc_lineno, ep->sc_fname);
		tcc_exit(1);
	}
}

/*
**	rrp_proctc() - process journal lines associated with a TEST CASE
**		scenario element
**
**	we've just read a BUILD, TC or CLEAN start line -
**	process journal lines up to and including the matching END line
**
**	return 1 if this TC is selected by the RERUN/RESUME options,
**	zero otherwise
*/

static int rrp_proctc(ep, buf, startid)
struct scentab *ep;
char *buf;
int startid;
{
	register struct jline *jlp;
	register int done, sel;
	register struct scentab *ep2;
	int mode, endid;

	ASSERT(ep->sc_type == SC_TESTCASE);

	/* determine the matching END id */
	switch (startid) {
	case TET_JNL_BUILD_START:
		endid = TET_JNL_BUILD_END;
		mode = TCC_BUILD;
		break;
	case TET_JNL_INVOKE_TC:
		endid = TET_JNL_TC_END;
		mode = TCC_EXEC;
		break;
	case TET_JNL_CLEAN_START:
		endid = TET_JNL_CLEAN_END;
		mode = TCC_CLEAN;
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected jnl id:", prjnlid(startid));
		/* NOTREACHED */
		return(0);
	}

	TRACE4(tet_Ttcc, 8, "rrp_proctc(): examine TC %s ref %s in %s mode",
		ep->sc_tcname, tet_l2a(ep->sc_ref), prtccmode(mode));

	/*
	** process each line associated with this test case
	** until the corresponding END line is found
	*/
	done = 0;
	sel = 0;
	while (!done && (jlp = rrp_getline(buf)) != (struct jline *) 0) {

		/*
		** if this is an END line, ensure that it matches the
		** START line which defined the processing mode for this TC;
		** then process it
		**
		** punt an IC start line down a level
		**
		** check for lines out of context
		**
		** back up over other lines and return - the backed-up line
		** will be re-read at the level above
		*/
		switch (jlp->jl_id) {
		case TET_JNL_BUILD_END:
		case TET_JNL_TC_END:
		case TET_JNL_CLEAN_END:
			if (endid != jlp->jl_id) {
				(void) fprintf(stderr, "TCC: expected %s at line %ld in old journal file %s\n",
					prjnlid(endid), jlp->jl_lineno, jfname);
				(void) fprintf(stderr,
					"instead, found %s line\n",
					prjnlid(jlp->jl_id));
				tcc_exit(1);
			}
			sel += rrp_procendline(ep, jlp, mode);
			done = 1;
			break;
		case TET_JNL_IC_START:
			sel += rrp_procic(ep, atoi(jlp->jl_flds2[1]), buf, mode);
			break;
		case TET_JNL_IC_END:
		case TET_JNL_TP_START:
		case TET_JNL_TP_RESULT:
			(void) fprintf(stderr, "TCC: found %s outside the scope of an IC at line %ld in old journal file %s\n",
				prjnlid(jlp->jl_id), jlp->jl_lineno, jfname);
			tcc_exit(1);
			break;
		default:
			if (fseek(jfp, jlp->jl_offset, SEEK_SET) < 0)
				fatal(errno, "seek error on", jfname);
			done = 1;
			break;
		}
	}

	/* return now if this test case has not been selected */
	if (!sel)
		return(0);

	/*
	** here if this test case has been selected -
	** mark the test case as needed
	*/
	ep->sc_flags |= SCF_NEEDED;

	/*
	** in RESUME mode:
	**
	**	the resume mode has already been set
	**	adjust the resume mode if necessary
	**
	**	if the resume point is within the scope of a PARALLEL
	**	or RANDOM directive, move it back to the start of the
	**	outermost PARALLEL directive or innermost RANDOM directive
	**
	**	mark the rest of the scenario as needed
	**
	**	remember the iteration count in each of the enclosing
	**	REPEAT or TIMED_LOOP directives
	*/
	if (tcc_modes & TCC_RESUME) {

		if (resume_mode == TCC_EXEC && (tcc_modes & TCC_CLEAN))
			resume_mode = TCC_BUILD;

		for (ep2 = ep->sc_parent; ep2; ep2 = ep2->sc_parent) {
			ASSERT(ep2->sc_magic == SC_MAGIC);
			if (
				ep2->sc_type == SC_DIRECTIVE &&
				ep2->sc_directive == SD_RANDOM
			) {
				TRACE2(tet_Ttcc, 6, "resume point moved to start of enclosing RANDOM directive at %s",
					tet_i2x(ep2));
				resume_scen = ep2;
				if (resume_iclist) {
					TRACE2(tet_Tbuf, 6,
						"RND free resume_iclist = %s",
						tet_i2x(resume_iclist));
					free(resume_iclist);
					resume_iclist = (char *) 0;
				}
				break;
			}
		}

		for (ep2 = ep->sc_parent; ep2; ep2 = ep2->sc_parent) {
			ASSERT(ep2->sc_magic == SC_MAGIC);
			if (
				ep2->sc_type == SC_DIRECTIVE &&
				ep2->sc_directive == SD_PARALLEL
			) {
				TRACE2(tet_Ttcc, 6, "resume point moved to start of enclosing PARALLEL directive at %s",
					tet_i2x(ep2));
				resume_scen = ep2;
				if (resume_iclist) {
					TRACE2(tet_Tbuf, 6,
						"PAR free resume_iclist = %s",
						tet_i2x(resume_iclist));
					free(resume_iclist);
					resume_iclist = (char *) 0;
				}
			}
		}

		set_scflags(resume_scen->sc_child, SCF_NEEDED);
		for (ep2 = resume_scen; ep2; ep2 = ep2->sc_parent) {
			ASSERT(ep2->sc_magic == SC_MAGIC);
			if (ep2->sc_type != SC_SCENARIO)
				set_scflags(ep2->sc_forw, SCF_NEEDED);
		}

		if (resume_mode == TCC_EXEC) {
			for (ep2 = ep->sc_parent; ep2; ep2 = ep2->sc_parent) {
				ASSERT(ep2->sc_magic == SC_MAGIC);
				if (ep2->sc_type != SC_DIRECTIVE)
					continue;
				if (
					ep2->sc_directive == SD_REPEAT ||
					ep2->sc_directive == SD_TIMED_LOOP
				) {
					TRACE4(tet_Ttcc, 6, "resume execution at iteration %s of enclosing %s directive at %s",
						tet_i2a(ep2->sc_itcount),
						prscdir(ep2->sc_directive),
						tet_i2x(ep2));
					ep2->sc_rescount = ep2->sc_itcount;
				}
			}
		}

	}

	return(1);
}

/*
**	rrp_procic() - process journal lines associated with an IC
**
**	we've just read an IC start line -
**	process journal lines up to and including the matching IC end line
**
**	return 1 if a result code below this IC selects this IC, zero otherwise
*/

static int rrp_procic(ep, icno, buf, mode)
struct scentab *ep;
int icno;
char *buf;
int mode;
{
	register struct jline *jlp;
	register int done, sel;
	int len, needlen, tmplen;
	char *p;

	TRACE3(tet_Ttcc, 8, "rrp_procic(): examine IC %s in %s mode",
		tet_i2a(icno), prtccmode(mode));

	/*
	** process each line associated with this IC
	** until the corresponding END line is found
	*/
	done = 0;
	sel = 0;
	while (!done && (jlp = rrp_getline(buf)) != (struct jline *) 0) {
		switch (jlp->jl_id) {
		case TET_JNL_TP_START:
			TRACE2(tet_Ttcc, 8, "examine result of TP %s",
				jlp->jl_flds2[1]);
			sel += rrp_proctp(buf, mode);
			break;
		case TET_JNL_IC_END:
			done = 1;
			break;
		case TET_JNL_TP_RESULT:
			(void) fprintf(stderr, "TCC: found %s outside the scope of a TP at line %ld in old journal file %s\n",
				prjnlid(jlp->jl_id), jlp->jl_lineno, jfname);
			tcc_exit(1);
			break;
		default:
			if (fseek(jfp, jlp->jl_offset, SEEK_SET) < 0)
				fatal(errno, "seek error on", jfname);
			done = 1;
			break;
		}
	}

	/*
	** if we already have a resume point, append this IC number
	** to the resume_iclist and return
	*/
	if ((tcc_modes & TCC_RESUME) && resume_scen) {
		if (mode == TCC_EXEC && resume_iclist) {
			len = strlen(resume_iclist) + 1;
			tmplen = len;
			p = tet_i2a(icno);
			needlen = len + strlen(p) + 1;
			RBUFCHK(&resume_iclist, &tmplen, needlen);
			(void) sprintf(resume_iclist + len - 1, ",%s", p);
		}
		return(sel);
	}

	/* return now if this IC has not been selected */
	if (!sel)
		return(0);

	/*
	**	here if this IC is selected
	*/

	/* in RESUME mode, remember the resume point */
	if (tcc_modes & TCC_RESUME) {
		TRACE4(tet_Ttcc, 6,
			"rrp_procic(): resume at TC %s (%s) in %s mode",
			ep->sc_tcname, tet_i2x(ep), prtccmode(mode));
		resume_scen = ep;
		resume_mode = mode;
		if (mode == TCC_EXEC) {
			ASSERT(resume_iclist == (char *) 0);
			(void) sprintf(buf, "%d", icno);
			resume_iclist = rstrstore(buf);
			TRACE2(tet_Ttcc, 6, "\tresume at IC %s",
				resume_iclist);
		}
	}

	/* in RERUN mode, update the IC list if so required */
	if ((tcc_modes & TCC_RERUN) && mode == TCC_EXEC)
		exiclist_addupdate(ep, icno);

	return(1);
}

/*
**	rrp_proctp() - process journal lines associated with a TP
**
**	we've just read an TP start line -
**	the next line should be the matching TP result line
**
**	return 1 if the result selects the enclosing IC, zero otherwise
*/

static int rrp_proctp(buf, mode)
char *buf;
int mode;
{
	struct jline *jlp;
	register int *ip, result, sel;

	/* read the TP result line */
	if ((jlp = rrp_getline(buf)) == (struct jline *) 0)
		return(0);

	sel = 0;
	switch (jlp->jl_id) {
	case TET_JNL_TP_RESULT:
		break;
	default:
		if (rrmodes & mode) {
			TRACE2(tet_Ttcc, 8, "missing TP result line selects this TP in %s mode",
				prtccmode(mode));
			sel = 1;
		}
		if (fseek(jfp, jlp->jl_offset, SEEK_SET) < 0)
			fatal(errno, "seek error on", jfname);
		return(sel);
	}

	/* see if the result selects the enclosing IC */
	result = atol(jlp->jl_flds2[2]);
	if (mode == TCC_EXEC)
		for (ip = rrcodes; ip < rrcodes + nrrcodes; ip++)
			if (*ip == result) {
				TRACE2(tet_Ttcc, 8, "result code %s selects this TP in EXEC mode",
					tet_i2a(result));
				sel = 1;
				break;
			}

	if (!sel && result && (rrmodes & mode)) {
		TRACE2(tet_Ttcc, 8,
			"non-zero result code selects this TP in %s mode",
			prtccmode(mode));
		sel = 1;
	}

	return(sel);
}

/*
**	rrp_procendline() - process a BUILD/TC/CLEAN end line
**
**	return 1 if the status code selects this test case, zero otherwise
*/

static int rrp_procendline(ep, jlp, mode)
struct scentab *ep;
struct jline *jlp;
int mode;
{
	int status = atoi(jlp->jl_flds2[1]);

	TRACE5(tet_Ttcc, 8, "rrp_procendline(): examine TC %s ref %s completion status %s in %s mode",
		ep->sc_tcname, tet_l2a(jlp->jl_ref), tet_i2a(status),
		prtccmode(mode));

	/* see if the tool completion status selects this TC */
	if (!status || (rrmodes & mode) == 0)
		return(0);

	/* here if the tool completion status selects this TC */
	TRACE2(tet_Ttcc, 8,
		"non-zero completion status selects this TC in %s mode",
		prtccmode(mode));

	/* in RERUN mode, restore the original exec IC list if necessary */
	if ((tcc_modes & TCC_RERUN) && mode == TCC_EXEC)
		exiclist_set(ep, ep->sc_sciclist);

	/*
	** in RESUME mode, remember the resume point if we don't already
	** have one
	*/
	if (tcc_modes & TCC_RESUME) {
		if (resume_scen) {
			if (mode == TCC_EXEC && resume_scen == ep && resume_iclist) {
				TRACE2(tet_Tbuf, 6,
					"ENDLINE free resume_iclist = %s",
					tet_i2x(resume_iclist));
				free(resume_iclist);
				resume_iclist = (char *) 0;
			}
		}
		else {
			resume_scen = ep;
			resume_mode = mode;
		}
	}

	return(1);
}

/*
**	rrp_getline() - read a line from the old journal file
**
**	return a pointer to a structure containing the broken-out fields
**	from the journal line
**
**	return (struct jline *) 0 on EOF or error
*/

static struct jline *rrp_getline(buf)
char *buf;
{
	static char fmt1[] = "ignored badly formatted journal line number %ld in";
	static char fmt2[] = "expected %d subfields in field 2, observed";
	static long lcount;
	static struct jline jline;
	register char *p;
	char line[LBUFLEN];
	char *flds[3];
	int nflds2, expflds;

	/*
	** loop until we find a line in a valid format whose id is one
	** of the types that we want
	*/
	for (;;) {
		jline.jl_offset = ftell(jfp);
		if (fgets(line, sizeof line, jfp) == (char *) 0)
			return((struct jline *) 0);
		lcount++;
		for (p = line; *p; p++)
			if (*p == '\n') {
				*p = '\0';
				break;
			}

		/* split the line into 3 |-separated fields */
		if (jnlproc_split(line, flds, buf) < 0 || !isdigit(*flds[0])) {
			(void) sprintf(line, fmt1, lcount);
			error(0, line, jfname);
			continue;
		}

		/* store the line count and the ID from field 1 */
		jline.jl_lineno = lcount;
		jline.jl_id = atoi(flds[0]);

		/*
		** discard lines that we don't want -
		** determine how many subfields to expect in field 2
		*/
		switch (jline.jl_id) {
		case TET_JNL_SEQ_START:
		case TET_JNL_RND_START:
		case TET_JNL_VAR_START:
			expflds = 0;
			break;
		case TET_JNL_PRL_START:
		case TET_JNL_RPT_START:
		case TET_JNL_TLOOP_START:
#ifndef TET_LITE	/* -START-LITE-CUT- */
		case TET_JNL_RMT_START:
		case TET_JNL_DIST_START:
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
			expflds = 1;
			break;
		case TET_JNL_TCC_START:
		case TET_JNL_INVOKE_TC:
		case TET_JNL_TC_END:
		case TET_JNL_BUILD_START:
		case TET_JNL_BUILD_END:
		case TET_JNL_TP_START:
		case TET_JNL_CLEAN_START:
		case TET_JNL_CLEAN_END:
			expflds = 3;
			break;
		case TET_JNL_IC_START:
		case TET_JNL_IC_END:
		case TET_JNL_TP_RESULT:
			expflds = 4;
			break;
		default:
			continue;
		}

		/* break out the 2nd field into subfields */
		nflds2 = tet_getargs(flds[1], jline.jl_flds2, 5);

		/* check the number of subfields in the 2nd field */
		if (expflds != nflds2) {
			(void) sprintf(line, fmt2, expflds);
			error(0, line, tet_i2a(nflds2));
			(void) sprintf(line, fmt1, lcount);
			error(0, line, jfname);
			continue;
		}

		/* zero out excess subfield pointers */
		while (nflds2 < 5)
			jline.jl_flds2[nflds2++] = (char *) 0;

		/* check the format of some of the subfields */
		switch (jline.jl_id) {
		case TET_JNL_PRL_START:
		case TET_JNL_RPT_START:
		case TET_JNL_TLOOP_START:
#ifndef TET_LITE	/* -START-LITE-CUT- */
		case TET_JNL_RMT_START:
		case TET_JNL_DIST_START:
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
			if (!isdigit(*jline.jl_flds2[0])) {
				(void) sprintf(line, fmt1, lcount);
				error(0, line, jfname);
				continue;
			}
			break;
		case TET_JNL_BUILD_START:
		case TET_JNL_INVOKE_TC:
		case TET_JNL_CLEAN_START:
			if (
				!isdigit(*jline.jl_flds2[0]) ||
				!(
					*jline.jl_flds2[1] == '/' ||
					*jline.jl_flds2[1] == '@'
				)
			) {
				(void) sprintf(line, fmt1, lcount);
				error(0, line, jfname);
				continue;
			}
			break;
		case TET_JNL_TC_END:
		case TET_JNL_BUILD_END:
		case TET_JNL_CLEAN_END:
			if (
				!isdigit(*jline.jl_flds2[0]) ||
				!(
					isdigit(*jline.jl_flds2[1]) ||
					*jline.jl_flds2[1] == '-'
				)
			) {
				(void) sprintf(line, fmt1, lcount);
				error(0, line, jfname);
				continue;
			}
			break;
		case TET_JNL_TP_START:
			if (
				!isdigit(*jline.jl_flds2[0]) ||
				!isdigit(*jline.jl_flds2[1])
			) {
				(void) sprintf(line, fmt1, lcount);
				error(0, line, jfname);
				continue;
			}
			break;
		case TET_JNL_IC_START:
		case TET_JNL_IC_END:
		case TET_JNL_TP_RESULT:
			if (
				!isdigit(*jline.jl_flds2[0]) ||
				!isdigit(*jline.jl_flds2[1]) ||
				!isdigit(*jline.jl_flds2[2])
			) {
				(void) sprintf(line, fmt1, lcount);
				error(0, line, jfname);
				continue;
			}
			break;
		}

		/* store the 3rd field */
		jline.jl_fld3 = flds[2];

		/* decode the scenario reference on lines which have it */
		jline.jl_ref = 0L;
		jline.jl_sys = 0;
		switch (jline.jl_id) {
		case TET_JNL_INVOKE_TC:
		case TET_JNL_TC_END:
		case TET_JNL_BUILD_START:
		case TET_JNL_BUILD_END:
		case TET_JNL_CLEAN_START:
		case TET_JNL_CLEAN_END:
		case TET_JNL_SEQ_START:
		case TET_JNL_RND_START:
		case TET_JNL_PRL_START:
		case TET_JNL_VAR_START:
		case TET_JNL_RPT_START:
		case TET_JNL_TLOOP_START:
#ifndef TET_LITE	/* -START-LITE-CUT- */
		case TET_JNL_RMT_START:
		case TET_JNL_DIST_START:
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
			for (p = flds[2]; *p; p++)
				if (sscanf(p, tcc_scenref_fmt, &jline.jl_ref, &jline.jl_sys) > 0)
					break;
			break;
		}

		TRACE3(tet_Ttcc, 10,
			"rrp_getline(): return line no %s, id = %s",
			tet_l2a(jline.jl_lineno), prjnlid(jline.jl_id));
		return(&jline);
	}

}

/*
**	rrp_checkloopstart() - see if this Test Case element is at the start of
**		a loop in EXEC mode
**
**	if it is, increment the enclosing directive's iteration count
*/

static void rrp_checkloopstart(ep, jlp)
register struct scentab *ep;
struct jline *jlp;
{
	register struct scentab *parent;

	/* return now if this is not an EXEC start */
	if (jlp->jl_id != TET_JNL_INVOKE_TC)
		return;

#ifndef TET_LITE	/* -START-LITE-CUT- */
	/*
	** look back up the tree to see if we are within the scope of
	** a REMOTE or DISTRIBUTED directive - return if this line is not
	** for the first system in the list
	*/
	for (parent = ep->sc_parent; parent; parent = parent->sc_parent) {
		ASSERT(parent->sc_magic == SC_MAGIC);
		if (
			parent->sc_type == SC_DIRECTIVE && (
				parent->sc_directive == SD_REMOTE ||
				parent->sc_directive == SD_DISTRIBUTED
			)
		) {
			ASSERT(parent->sc_sys);
			if (jlp->jl_sys != *parent->sc_sys)
				return;
		}
	}
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	/*
	** look back up the tree to see if we are within the scope
	** of a looping directive
	*/
	for (parent = ep->sc_parent; parent; parent = parent->sc_parent) {
		ASSERT(parent->sc_magic == SC_MAGIC);
		if (
			parent->sc_type == SC_DIRECTIVE && (
				parent->sc_directive == SD_REPEAT ||
				parent->sc_directive == SD_TIMED_LOOP
			)
		) {
			break;
		}
	}

	/* return now if not */
	if (!parent)
		return;

	/*
	** see if this is the first TC within the looping directive's scope
	** and increment the directive's iteration count if it is
	**
	** for a TIMED_LOOP, remember the iteration count high water mark
	** in sc_count for use in loop_test() in the execution engine
	*/
	if (rrp_cl2(parent->sc_child, ep, 0)) {
		parent->sc_itcount++;
		if (parent->sc_directive == SD_TIMED_LOOP &&
			parent->sc_itcount > parent->sc_count)
				parent->sc_count = parent->sc_itcount;
	}
}

/*
**	rrp_cl2() - extend the rrp_loopcount() processing
**
**	return 1 if tc is the first Test Case node on or below the level
**	pointed to by ep, or zero otherwise
*/

static int rrp_cl2(ep, tc, random)
register struct scentab *ep, *tc;
int random;
{
	for (; ep && ep != tc; ep = ep->sc_forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		switch (ep->sc_type) {
		case SC_DIRECTIVE:
			switch (ep->sc_directive) {
			case SD_RANDOM:
				random = 1;
				/* fall through */
			default:
				if (rrp_cl2(ep->sc_child, tc, random))
					return(1);
				break;
			}
			break;
		case SC_TESTCASE:
			if (!random)
				return(0);
			break;
		}
	}

	return((ep && ep == tc) ? 1 : 0);
}

/*
**	exiclist_addupdate() - add an IC number to a scenario element's
**		EXEC IC list if it is not already there
*/

static void exiclist_addupdate(ep, icno)
struct scentab *ep;
int icno;
{
	struct ics {
		int ic_start;
		int ic_end;
	} *ics = (struct ics *) 0;
	int lics = 0;
	register struct ics *icp;
	register char **fldp;
	register char *p;
	char *iclist;
	char **flds1 = (char **) 0;
	int lflds1 = 0;
	int nflds1;
	char *flds2[2];
	int nics, len, needlen;

	/*
	** if the existing EXEC IC list is empty, just set it to the new
	** IC number and return
	*/
	if (ep->sc_exiclist == (char *) 0) {
		exiclist_set(ep, rstrstore(tet_i2a(icno)));
		return;
	}

	/*
	** here if there is an existing IC list;
	** split the list into comma-separated fields
	**
	** a scratchpad is allocated because split() trashes its input string
	*/
	iclist = rstrstore(ep->sc_exiclist);
	nflds1 = 1;
	for (p = iclist; *p; p++)
		if (*p == ',')
			nflds1++;
	RBUFCHK((char **) &flds1, &lflds1, (int) (nflds1 * sizeof *flds1));
	nflds1 = split(iclist, flds1, nflds1, ',');

	/*
	** now, each field contains an IC number or an IC number range;
	** split each field into IC start and end numbers
	*/
	RBUFCHK((char **) &ics, &lics, (int) (nflds1 * sizeof *ics));
	icp = ics;
	for (fldp = flds1; fldp < flds1 + nflds1; fldp++) {
		switch (split(*fldp, flds2, 2, '-')) {
		case 0:
			continue;
		case 1:
			icp->ic_start = atoi(flds2[0]);
			icp->ic_end = icp->ic_start;
			break;
		default:
			icp->ic_start = atoi(flds2[0]);
			icp->ic_end = atoi(flds2[1]);
			break;
		}
		icp++;
	}
	nics = icp - ics;

	/* look for the IC number in each field */
	for (icp = ics; icp < ics + nics; icp++)
		if (icno >= icp->ic_start && icno <= icp->ic_end)
			break;

	/* free the scratchpad */
	TRACE2(tet_Tbuf, 6, "free tmp iclist = %s", tet_i2x(iclist));
	free(iclist);
	iclist = (char *) 0;

	/* if icno was not found, append it to the EXEC IC list */
	if (icp >= ics + nics) {
		len = 0;
		p = tet_i2a(icno);
		needlen = strlen(ep->sc_exiclist) + strlen(p) + 2;
		RBUFCHK(&iclist, &len, needlen);
		(void) sprintf(iclist, "%s,%s", ep->sc_exiclist, p);
		exiclist_set(ep, iclist);
	}

	/* finally, free the other allocated storage and return */
	TRACE2(tet_Tbuf, 6, "free flds1 = %s", tet_i2x(flds1));
	free((char *) flds1);
	TRACE2(tet_Tbuf, 6, "free ics = %s", tet_i2x(ics));
	free((char *) ics);
}

/*
**	exiclist_set() - set the EXEC IC list in a scenario element
*/

static void exiclist_set(ep, iclist)
register struct scentab *ep;
char *iclist;
{
	if (iclist != ep->sc_exiclist) {
		if (ep->sc_exiclist && ep->sc_exiclist != ep->sc_sciclist) {
			TRACE2(tet_Tbuf, 6, "free existing sc_exiclist = %s",
				tet_i2x(ep->sc_exiclist));
			free(ep->sc_exiclist);
		}
		ep->sc_exiclist = iclist;
	}
}


/*
**	remove_unneeded_tcs() - remove all unneeded test cases
**		from the scenario tree
*/

static void remove_unneeded_tcs(ep)
register struct scentab *ep;
{
	register struct scentab *forw;

	/* remove each test case which does not have the NEEDED flag set */
	for (; ep; ep = forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		forw = ep->sc_forw;
		switch (ep->sc_type) {
		case SC_DIRECTIVE:
			remove_unneeded_tcs(ep->sc_child);
			continue;
		case SC_TESTCASE:
			if ((ep->sc_flags & SCF_NEEDED) == 0) {
				TRACE3(tet_Ttcc, 6,
					"remove unneeded test case %s ref %s",
					ep->sc_tcname, tet_l2a(ep->sc_ref));
				scrm_lnode(ep);
			}
			break;
		default:
			continue;
		}
	}
}

/*
**	set_scflags() - set the specified flags in the scenario tree
*/

static void set_scflags(ep, flags)
register struct scentab *ep;
register int flags;
{
	/*
	** traverse the tree at this level, seting the specified flags
	** and descending directive trees
	*/
	for (; ep; ep = ep->sc_forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		ep->sc_flags |= flags;
		switch (ep->sc_type) {
		case SC_DIRECTIVE:
			TRACE2(tet_Ttcc, 7, "set_scflags(): descend tree below %s directive",
				prscdir(ep->sc_directive));
			set_scflags(ep->sc_child, flags);
			break;
		}
	}
}

/*
**	clear_scflags() - clear the specified flags in the scenario tree
*/

static void clear_scflags(ep, flags)
register struct scentab *ep;
register int flags;
{
	/*
	** traverse the tree at this level, clearing the specified flags
	** and descending directive trees
	*/
	for (; ep; ep = ep->sc_forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		ep->sc_flags &= ~flags;
		switch (ep->sc_type) {
		case SC_DIRECTIVE:
			TRACE2(tet_Ttcc, 7, "clear_scflags(): descend tree below %s directive",
				prscdir(ep->sc_directive));
			clear_scflags(ep->sc_child, flags);
			break;
		}
	}
}

/*
**	clear_exiclists() - clear all the default EXEC IC lists in the
**		scenario tree
**
**	this function should only be called in RERUN mode
*/

static void clear_exiclists(ep)
register struct scentab *ep;
{
	/*
	** traverse the tree at this level, clearing the EXEC IC lists
	** and descending directive trees
	*/
	for (; ep; ep = ep->sc_forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		switch (ep->sc_type) {
		case SC_DIRECTIVE:
			TRACE2(tet_Ttcc, 7, "clear_exiclists(): descend tree below %s directive",
				prscdir(ep->sc_directive));
			clear_exiclists(ep->sc_child);
			break;
		case SC_TESTCASE:
			ep->sc_exiclist = (char *) 0;
			break;
		}
	}
}

/*
**	clear_itcount() - clear all the loop directive iteration counts in
**		the scenario tree
*/

static void clear_itcount(ep)
register struct scentab *ep;
{
	/*
	** traverse the tree at this level, clearing the loop directive
	** iteration counts and descending directive trees
	*/
	for (; ep; ep = ep->sc_forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		switch (ep->sc_type) {
		case SC_DIRECTIVE:
			switch (ep->sc_directive) {
			case SD_REPEAT:
			case SD_TIMED_LOOP:
				ep->sc_itcount = 0;
				break;
			}
			TRACE2(tet_Ttcc, 7, "clear_itcount(): descend tree below %s directive",
				prscdir(ep->sc_directive));
			clear_itcount(ep->sc_child);
			break;
		}
	}
}

/*
**	prjnlid() - return a printable representation of a journal id value
*/

static char *prjnlid(id)
int id;
{
	static char text[] = "journal-id ";
	static char msg[sizeof text + LNUMSZ];

	switch (id) {
	case TET_JNL_TCC_START:
		return("TCC Start");
	case TET_JNL_INVOKE_TC:
		return("TC Start");
	case TET_JNL_TCM_START:
		return("TCM Start");
	case TET_JNL_CFG_START:
		return("Config Start");
	case TET_JNL_CFG_VALUE:
		return("Config Assignment");
	case TET_JNL_CFG_END:
		return("Config End");
	case TET_JNL_TC_MESSAGE:
		return("TCC Message");
	case TET_JNL_SCEN_OUT:
		return("Scenario Information");
	case TET_JNL_TC_END:
		return("TC End");
	case TET_USER_ABORT:
		return("User Abort");
	case TET_JNL_CAPTURED_OUTPUT:
		return("Captured Output");
	case TET_JNL_BUILD_START:
		return("Build Start");
	case TET_JNL_BUILD_END:
		return("Build End");
	case TET_JNL_TP_START:
		return("TP Start");
	case TET_JNL_TP_RESULT:
		return("TP Result");
	case TET_JNL_CLEAN_START:
		return("Clean Start");
	case TET_JNL_CLEAN_OUTPUT:
		return("Clean Output");
	case TET_JNL_CLEAN_END:
		return("Clean End");
	case TET_JNL_IC_START:
		return("IC Start");
	case TET_JNL_IC_END:
		return("IC End");
	case TET_JNL_TCM_INFO:
		return("TCM Message");
	case TET_JNL_TC_INFO:
		return("TC Info");
	case TET_JNL_PRL_START:
		return("Parallel Start");
	case TET_JNL_PRL_END:
		return("Parallel End");
	case TET_JNL_SEQ_START:
		return("Sequential Start");
	case TET_JNL_SEQ_END:
		return("Sequential End");
	case TET_JNL_VAR_START:
		return("Variable Start");
	case TET_JNL_VAR_END:
		return("Variable End");
	case TET_JNL_RPT_START:
		return("Repeat Start");
	case TET_JNL_RPT_END:
		return("Repeat End");
	case TET_JNL_TLOOP_START:
		return("Timed-loop Start");
	case TET_JNL_TLOOP_END:
		return("Timed-loop End");
	case TET_JNL_RND_START:
		return("Random Start");
	case TET_JNL_RND_END:
		return("Random End");
	case TET_JNL_RMT_START:
		return("Remote Start");
	case TET_JNL_RMT_END:
		return("Remote End");
	case TET_JNL_DIST_START:
		return("Distributed Start");
	case TET_JNL_DIST_END:
		return("Distributed End");
	case TET_JNL_TCC_END:
		return("TCC End");
	default:
		(void) sprintf(msg, "%s%d", text, id);
		return(msg);
	}
}


