/*
 *	SCCS: @(#)scen1.c	1.12 (05/12/07)
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
static char sccsid[] = "@(#)scen1.c	1.12 (05/12/07) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)scen1.c	1.12 05/12/07 TETware release 3.8
NAME:		scen1.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	scenario parser stage 1 - tokenise the scenario file

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., March 1997
	allow leading white space on lines read from a scenario include file

	Andrew Dingwall, UniSoft Ltd., March 1998
	Added support for number ranges in directive arguments.
	Increased the number of arguments that a directive may take.
	Permit a number range to appear as an argument to a remote or
	distributed directive, so as to avoid the need to handle long
	scenario lines and large numbers of arguments when many system
	IDs are specified.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for generic scenario file preprocessing,
	and support for the %include scenario keyword.
	Functions previously in getline.c moved to here, and enhanced to
	support multiple input files.

	Andrew Dingwall, UniSoft Ltd., March 2000
	Use compatibility mode value when deciding how many elements
	are attached to a directive or directive group.

	Andrew Dingwall, The Open Group, March 2003
	Made isnumrange() and iszorpnum() global so that they can be used
	when parsing tet_transfer_source_files.

	Neil Moses, The Open Group, November 2005
	Added support for reconnecting systems. The remote directive
	now can have system ids suffixed by the letter 'r' to indicate
	the system can be reconnected.

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <time.h>
#include <ctype.h>
#include <errno.h>
#include <string.h>
#include "dtmac.h"
#include "error.h"
#include "ltoa.h"
#include "llist.h"
#include "bstring.h"
#include "scentab.h"
#include "dirtab.h"
#include "dtetlib.h"
#include "tcc.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* some limits which apply to directives */
/* max number of semicolon-delimited fields in group execution */
#define MAXFLDS	(LBUFLEN / 8)
#if MAXFLDS < 10
#  define MAXFLDS	10
#endif
/* max number of comma-delimited args in a single directive */
#define MAXARGS	(LBUFLEN / 4)
#if MAXARGS < 10
#  define MAXARGS	10
#endif

/* is this an attached test list or file name? */
#define isattached(s)	(*(s) && !isspace(*(s)))

/*
** structure of the line cache stack used by getline() and ungetline()
** this is a linked list
** the lc_next and lc_last elements must be 1st and 2nd so as to
** enable the stack to be manipulated by the llist functions
*/
struct lcache {
        struct lcache *lc_next;		/* ptr to next element in the list */
        struct lcache *lc_last;		/* ptr to prev element in the list */
        char *lc_line;          	/* the stored input line */
};

/*
** Structure of the input file stack.
** This is a linked list.
** The if_next and if_last elements must be 1st and 2nd so as to enable
** the stack to be manipulated by the llist functions.
**
** An input file stack element is allocated for each scenario file that
** is being read.
** Initially, an element is allocated for the tet_scen file, which is already
** open when proc1scfile() is called..
** If a %include keyword is encountered, a new element is allocated for
** the specified file and is pushed on to the stack.
** Thus the file to be %include'd becomes the current input file.
** The stack is unwound when EOF is encountered at each level.
*/
struct ifstack {
	struct ifstack *if_next;	/* ptr to next element in the list */
	struct ifstack *if_last;	/* ptr to prev element in the list */
	char *if_fname;			/* file name */
	FILE *if_fp;			/* stdio stream pointer */
	int if_lcount;			/* number of lines read from file */
	struct lcache *if_lcache;	/* line cache for this file */
};

/* the input file stack itself */
static struct ifstack *ifstack;

/*
** pointer to the currently active input file stack element;
** that is: the element in the stack where getline() and ungetline()
** fetch and store input lines
**
** normally, the currently active element is at the top of the stack;
** however, if there is more than one level in the stack and ungetline() is
** called to push back more lines than have been read from the file at any
** particular level, the next level down becomes the currently active level
** and lines are cached and retrieved from there
*/
static struct ifstack *ifstp;


/* static function declarations */
static int find1scen PROTOLIST((void));
static char *getline PROTOLIST((void));
static void includefile PROTOLIST((char *, char *, int));
static struct ifstack *ifsalloc PROTOLIST((void));
static void ifsfree PROTOLIST((struct ifstack *));
static struct ifstack *ifspop PROTOLIST((void));
static void ifspush PROTOLIST((struct ifstack *));
static struct lcache *lcalloc PROTOLIST((void));
static void lcfree PROTOLIST((struct lcache *));
static struct lcache *lcpop PROTOLIST((void));
static struct lcache *lcpop2 PROTOLIST((struct ifstack *));
static void lcpush PROTOLIST((struct lcache *));
static int ppinclude PROTOLIST((char *));
static int preprocess PROTOLIST((char *));
static int proc1dgrp PROTOLIST((char *, char *, char *, int));
static void proc1scelem PROTOLIST((char *, int, int, int, char *));
static int proc1scen PROTOLIST((void));
static int proc1scline PROTOLIST((char *, char *, int));
static void ungetline PROTOLIST((char *));
#ifndef NOTRACE
static char *firstpart PROTOLIST((char *));
#endif


/*
**	proc1scfile() - tokenise a scenario input file
**
**	return 0 if successful or -1 on error
*/

int proc1scfile(fp, fname)
FILE *fp;
char *fname;
{
	struct ifstack *ifp;
	int rc;

	TRACE2(tet_Tscen, 1, "proc1scfile(): tokenising scenario file %s",
		fname);

	/* store the top level file details on the input file stack */
	ifp = ifsalloc();
	ifp->if_fname = fname;
	ifp->if_fp = fp;
	ifspush(ifp);

	/* tokenise each scenario in turn */
	do {
		if ((rc = find1scen()) > 0)
			while ((rc = proc1scen()) > 0)
				;
	} while (rc > 0);

	/*
	** close all files except the top level one
	**
	** normally, files are closed as the input file stack is unwound -
	** files only get closed here if one of the lower level functions
	** returned abruptly after encountering an error consition
	*/
	ifstp = ifstack;
	while (ifstack) {
		if (ifstack->if_next && ifstack->if_fp)
			(void) fclose(ifstack->if_fp);
		ifsfree(ifspop());
	}

	return(rc);
}

/*
**	find1scen() - search for the start of a scenario in the input file
**
**	return	 1 if found
**		 0 on EOF
**		-1 if processing of this file must be abandoned
*/

static int find1scen()
{
	char *line;
	int n, skipstart = 0;
	static char fmt[] = "found a scenario after skipping %d line%s";
	char msg[sizeof fmt + LNUMSZ];

	TRACE1(tet_Tscen, 4, "find1scen(): find start of scenario");

	/*
	** find the start of the next scenario -
	**	skip lines until it is found -
	**	when the start is found, push the line back so that
	**	the scenario processor can find it
	*/
	for (;;) {
		if ((line = getline()) == (char *) 0)
			return(ferror(ifstp->if_fp) ? -1 : 0);
		if (!isspace(*line)) {
			/* start of new scenario */
			if (skipstart) {
				n = ifstp->if_lcount - skipstart;
				(void) sprintf(msg, fmt, n, n == 1 ? "" : "s");
				scenermsg(msg, (char *) 0, ifstp->if_lcount,
					ifstp->if_fname);
			}
			ungetline(line);
			return(1);
		}
		/*
		** here if we are skipping lines -
		**	emit a warning first time through
		*/
		if (!skipstart) {
			skipstart = ifstp->if_lcount;
			scenerror("line(s) outside of a scenario ignored",
				(char *) 0, skipstart, ifstp->if_fname);
		}
	}
}

/*
**	proc1scen() - tokenise a scenario once the input stream is
**		correctly positioned
**
**	return	 1 if successful and the input stream is still
**			correctly positioned
**		 0 if successful and the input stream is not
**			correctly positioned (in practice this means EOF)
**		-1 if processing of this file must be abandoned
*/

static int proc1scen()
{
	char *line, *next;
	register char *p;
	register struct scentab *ep;

	/* read the scenario name - starts in column 1 */
	line = getline();
	ASSERT(line);
	ASSERT(!isspace(*line));

	/* see if there are scenario elements after the name */
	for (p = line; *p; p++)
		if (isspace(*p)) {
			*p++ = '\0';
			break;
		}
	next = p;

	TRACE3(tet_Tscen, 4, "proc1scen(): new scenario = <%s>, rest = <%s>",
		line, next);

	/* store the scenario header token */
	ep = scalloc();
	ep->sc_type = SC_SCENARIO;
	ep->sc_scenario = rstrstore(line);
	ep->sc_lineno = ifstp->if_lcount;
	ep->sc_fname = ifstp->if_fname;
	scpush(ep, &sclist);

	/* process any elements on the same line as the header */
	if (proc1scline(next, ifstp->if_fname, ifstp->if_lcount) < 0)
		return(-1);

	/* process the rest of the current scenario */
	while ((line = getline()) != (char *) 0) {
		if (!isspace(*line)) {
			/* a new scenario */
			ungetline(line);
			return(1);
		}
		/* process a line in the current scenario */
		if (proc1scline(line, ifstp->if_fname, ifstp->if_lcount) < 0)
			return(-1);
	}

	return(ferror(ifstp->if_fp) ? -1 : 0);
}

/*
**	proc1cmdline() - tokenise a scenario line specified with
**		the -l command-line option
**
**	return 0 if successful or -1 if processing must be abandoned
*/

int proc1cmdline(line)
char *line;
{
	static char cmdline[] = "<command-line>";
	static int lineno;
	struct scentab *ep;

	/* start a new (default) scenario first time through */
	if (!lineno++) {
		ep = scalloc();
		ep->sc_type = SC_SCENARIO;
		ep->sc_scenario = "all";
		ep->sc_lineno = lineno;
		ep->sc_fname = cmdline;
		scpush(ep, &sclist);
	}

	/* process the line and return */
	return(proc1scline(line, cmdline, lineno));
}

/*
**	proc1scline() - tokenise a line in the current scenario
**
**	return 0 if successful or -1 if processing of this file must be
**		abandoned
*/

static int proc1scline(line, fname, lineno)
char *line, *fname;
int lineno;
{
	register char *p;
	int type, flags;
	char *next;
	char delim;
	static char msg[2];

	/* skip leading spaces */
	while (*line && isspace(*line))
		line++;

	TRACE2(tet_Tscen, 4, "proc1scline(): line = <%s>", line);

	/* determine the element type and how it is delimited */
	flags = 0;
	switch (*line) {
	case '\0':	/* an empty line */
		return(0);
	case '"':	/* a scenario information line */
		type = SC_SCENINFO;
		delim = *line++;
		break;
	case '^':	/* a referenced scenario name */
		type = SC_SCEN_NAME;
		delim = ' ';
		line++;
		break;
	case ':':	/* a scenario directive */
		type = SC_DIRECTIVE;
		delim = *line++;
		break;
	default:	/* treat everything else as a testcase for now -
				we'll check for valid testcase names later */
		type = SC_TESTCASE;
		delim = ' ';
		break;
	}

	/* search for the required delimiter */
	for (p = line; *p; p++)
		if (*p == delim || (delim == ' ' && isspace(*p)))
			break;

	/* see if it was found */
	if (!*p && !isspace(delim)) {
		msg[0] = delim;
		scenerror("unmatched delimiter", msg, lineno, fname);
		return(0);
	}

	/* terminate this element, remember start of next element */
	if (*p)
		*p++ = '\0';
	next = p;

	TRACE3(tet_Tscen, 4, "proc1scline(): element = <%s>, rest = <%s>",
		line, next);

	/* process this element */
	switch (type) {
	case SC_TESTCASE:
	case SC_SCEN_NAME:
		if (!*(line + 1)) {
			scenerror("empty test case or scenario name",
				line, lineno, fname);
			break;
		}
		/* else fall through */
	case SC_SCENINFO:
		proc1scelem(line, type, flags, lineno, fname);
		break;
	case SC_DIRECTIVE:
		return(proc1dgrp(line, next, fname, lineno));
	default:
		/* this "can't happen" .*/
		fatal(0, "unexpected type", prsctype(type));
		/* NOTREACHED */
	}

	/* process the rest of the current line */
	return(proc1scline(next, fname, lineno));
}

/*
**	proc1scelem() - process a single (non-directive) scenario element
*/

static void proc1scelem(element, type, flags, lineno, fname)
char *element, *fname;
int type, flags, lineno;
{
	register struct scentab *ep;
	register char *p;
	char *iclist, *tcname;
	int lsceninfo;

	TRACE2(tet_Tscen, 4, "proc1scelem(): element = <%s>", element);

	/* allocate a scenario element and fill it in */
	ep = scalloc();
	ep->sc_type = type;
	ep->sc_flags = flags;
	ep->sc_lineno = lineno;
	ep->sc_fname = fname;

	switch (type) {
	case SC_SCENINFO:
		lsceninfo = 0;
		RBUFCHK(&ep->sc_sceninfo, &lsceninfo,
			(int) strlen(element) + 3);
		(void) sprintf(ep->sc_sceninfo, "\"%s\"", element);
		break;
	case SC_TESTCASE:
		tcname = element;
		iclist = (char *) 0;
		for (p = element; *p; p++)
			if (*p == '{') {
				*p++ = '\0';
				for (iclist = p; *p; p++)
					if (*p == '}') {
						*p = '\0';
						break;
					}
				break;
			}
		ep->sc_tcname = rstrstore(tcname);
		if (iclist && *iclist)
			ep->sc_sciclist = rstrstore(iclist);
		ep->sc_exiclist = ep->sc_sciclist;
		break;
	case SC_SCEN_NAME:
		ep->sc_scen_name = rstrstore(element);
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected type", prsctype(type));
		/* NOTREACHED */
	}

	/* then add the element to the list */
	scpush(ep, &sclist);
}

/*
**	proc1dgrp() - process a group of scenario directives;
**		i.e., something between a pair of ':' delimiters
**
**	return 0 if successful or -1 if processing of this file must be
**		abandoned
*/

static int proc1dgrp(dgroup, next, fname, lineno)
char *dgroup, *next, *fname;
int lineno;
{
	char buf[LBUFLEN];
	char *dirs[MAXFLDS];
	int ndirs;
	char **dirp;
	char *args[MAXARGS];
	int nargs;
	register char **ap;
	register struct scentab *ep;
	register struct dirtab *dp;
	char **vp;
	char *line;
	int istart, iend, ok, rc;
	register char *p1, *p2;
	struct scentab *scstack = (struct scentab *) 0;
	char *argp;
#ifndef TET_LITE	/* -START-LITE-CUT- */
	int *ip, nsys, syslen;
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	TRACE3(tet_Tscen, 4,
		"proc1dgrp(): directive group = <%s>, rest = <%s>",
		dgroup, next);

	/* now the colons have gone - split a group-execution directive
		into semicolon-delimited fields */
	ndirs = split(dgroup, dirs, MAXFLDS, ';');

	/* trim leading spaces from each directive */
	for (dirp = dirs; dirp < &dirs[ndirs]; dirp++)
		while (**dirp && isspace(**dirp))
			++*dirp;

#ifndef NOTRACE
	TRACE2(tet_Tscen, 4, "group contains %s directive(s)", tet_i2a(ndirs));
	if (tet_Tscen >= 5) {
		for (dirp = dirs; dirp < &dirs[ndirs]; dirp++)
			TRACE3(tet_Tscen, 5, "\tdirective %s = <%s>",
				tet_i2a((dirp - dirs) + 1), *dirp);
	}
#endif

	/*
	**	check the syntax of the directive group
	*/

#ifndef NOTRACE
	if (isattached(next))
		TRACE2(tet_Tscen, 4,
			"directive group has attached items: <%s>",
			next);
	else
		TRACE1(tet_Tscen, 4, "directive group has no attached items");
#endif

	/* check the syntax of each directive in the group
		using the rules in the directives table */
	TRACE1(tet_Tscen, 4, "starting directive syntax check");
	ok = 1;
	for (dirp = dirs; dirp < &dirs[ndirs]; dirp++) {
		/* count the number of comma-separated arguments
			associated with this directive */
		(void) strcpy(buf, *dirp);
		args[0] = "";
		nargs = split(buf, args, MAXARGS, ',') - 1;

		/* here, args[0] is the directive,
			nargs is the number of arguments
			args[1] -> args[nargs] are the arguments */

		/* trim leading spaces from each argument */
		for (ap = &args[1]; ap <= &args[nargs]; ap++)
			while (**ap && isspace(**ap))
				++*ap;

#ifndef NOTRACE
		TRACE3(tet_Tscen, 6,
			"syntax checking <%s> with %s argument(s)",
			args[0], tet_i2a(nargs));
		if (tet_Tscen >= 7) {
			for (ap = &args[1]; ap <= &args[nargs]; ap++)
				TRACE3(tet_Tscen, 7, "\targ %s = <%s>",
					tet_i2a(ap - args), *ap);
		}
#endif

		/* look up the directive in the table */
		if ((dp = getdirbyname(args[0])) == (struct dirtab *) 0) {
			if (!*dirp)
				*dirp = "<empty>";
			scenerror("unknown/unsupported directive",
				*dirp, lineno, fname);
			ok = 0;
			continue;
		}

		/* check for optional and mandatory arguments */
		if (
			nargs == 0 &&
			(dp->dt_flags & (SDF_NEED_ARG | SDF_NEED_MARG))
		) {
			scenerror(args[0], "needs an argument",
				lineno, fname);
			ok = 0;
		}
		else if (
			nargs > 1 &&
			(dp->dt_flags & (SDF_NEED_ARG | SDF_OPT_ARG))
		) {
			scenerror(args[0], "has too many arguments",
				lineno, fname);
			ok = 0;
		} else if (
			nargs > 0 &&
			!(dp->dt_flags & (SDF_OPT_ARG | SDF_OPT_MARG |
				SDF_NEED_ARG | SDF_NEED_MARG))
		) {
			scenerror(args[0], "should not have arguments",
				lineno, fname);
			ok = 0;
		}

		/* check for non-numeric arguments */
		if (dp->dt_flags & SDF_NUMERIC_ARGS)
			for (ap = &args[1]; ap <= &args[nargs]; ap++)
				if (!iszorpnum(*ap)) {
					scenerror(args[0],
						"argument is not a +ve number",
						lineno, fname);
					ok = 0;
					break;
				}

		/* check for correctly formatted number range arguments */
		if (dp->dt_flags & SDF_NRANGE_ARGS)
			for (ap = &args[1]; ap <= &args[nargs]; ap++) {

				argp = rstrstore(*ap);

#ifndef TET_LITE	/* -START-LITE-CUT- */
				/* Could be a system to reconnect */
				if (argp[strlen(argp) - 1] == 'r')
					argp[strlen(argp) - 1] = '\0';
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

				istart = iend = 0;
				if (
					!iszorpnum(argp) &&
					!isnumrange(argp, &istart, &iend)
				) {
					scenerror(args[0],
				"argument is not a +ve number or number range",
						lineno, fname);
					ok = 0;
					break;
				}
				else if (istart > iend) {
					scenerror(argp,
				": start is greater than end in number range",
						lineno, fname);
					ok = 0;
				}

				free(argp);
			}

		/* check for "variable" format arguments */
		if (dp->dt_flags & SDF_VARFMT_ARGS)
			for (ap = &args[1]; ap <= &args[nargs]; ap++) {
				p1 = tet_remvar(*ap, -1);
				p2 = tet_equindex(*ap);
				if (!p1 || !p2) {
					scenerror(*ap,
			"is not a properly formatted variable assignment",
						lineno, fname);
					ok = 0;
					break;
				}
				else if (!strncmp(p1, "TET_", 4)) {
					*p2 = '\0';
					scenerror(p1,
			"value may not be assigned in a scenario file",
						lineno, fname);
					ok = 0;
					break;
				}
			}

		/* check for optional and mandatory attached elements */
		if (isattached(next)) {
			if ((dp->dt_flags & (SDF_OPT_ATTACH | SDF_NEED_ATTACH)) == 0) {
				scenerror(args[0],
					"should not have an attached element",
					lineno, fname);
				ok = 0;
			}
		}
		else {
			if (dp->dt_flags & SDF_NEED_ATTACH) {
				scenerror(args[0],
					"needs an attached element",
					lineno, fname);
				ok = 0;
			}
		}
	}

	TRACE2(tet_Tscen, 6, "directive syntax check was %ssuccessful",
		ok ? "" : "un");

	if (!ok) {
		TRACE1(tet_Tscen, 4, "proc1dgrp(): return 0");
		return(0);
	}

	/* process each of the directives in turn */
	for (dirp = dirs; dirp < &dirs[ndirs]; dirp++) {

		TRACE2(tet_Tscen, 4, "process directive <%s>", *dirp);

		/* separate the directive from its arguments */
		nargs = split(*dirp, args, MAXARGS, ',') - 1;

		/* trim leading spaces from each argument */
		for (ap = &args[1]; ap <= &args[nargs]; ap++)
			while (**ap && isspace(**ap))
				++*ap;

		/* here, args[0] is the directive,
			nargs is the number of arguments
			args[1] -> args[nargs] are the arguments */

#ifndef NOTRACE
		TRACE3(tet_Tscen, 4, "directive = <%s> has %s argument(s)",
			args[0], tet_i2a(nargs));
		if (tet_Tscen >= 5) {
			for (ap = &args[1]; ap <= &args[nargs]; ap++)
				TRACE3(tet_Tscen, 7, "\targ %s = <%s>",
					tet_i2a(ap - args), *ap);
		}
#endif

		/* look the directive up in the table */
		dp = getdirbyname(args[0]);
		ASSERT(dp);

		/* an INCLUDE directive does not go in the list -
		**	in dtet mode it is just a tag on which
		**	to hang a filename;
		**	in etet mode it is a no-op
		*/
		if (dp->dt_directive == SD_INCLUDE)
			continue;

		/* allocate a scenario element and fill it in */
		ep = scalloc();
		ep->sc_type = SC_DIRECTIVE;
		ep->sc_directive = dp->dt_directive;
		ap = &args[1];
		switch (ep->sc_directive) {
		case SD_PARALLEL:
		case SD_REPEAT:
			ASSERT(nargs == 0 || nargs == 1);
			if (nargs == 0)
				ep->sc_count = 1;
			else
				ep->sc_count = atoi(*ap);
			break;
		case SD_TIMED_LOOP:
			ASSERT(nargs == 1);
			ep->sc_seconds = atol(*ap);
			break;
		case SD_VARIABLE:
			ASSERT(nargs >= 0);
			if ((ep->sc_vars = (char **) calloc((unsigned) TET_MAX(nargs, 1), sizeof *ep->sc_vars)) == (char **) 0)
				fatal(errno, "can't allocate memory for variable list", (char *) 0);
			TRACE2(tet_Tbuf, 6, "allocate variable list = %s",
				tet_i2x(ep->sc_vars));
			ep->sc_nsys = nargs;
			for (vp = ep->sc_vars; nargs > 0; nargs--)
				*vp++ = rstrstore(*ap++);
			errno = 0;
			break;
#ifndef TET_LITE	/* -START-LITE-CUT- */
		case SD_REMOTE:
		case SD_DISTRIBUTED:
			ASSERT(nargs > 0);
			ep->sc_nsys = 0;
			ep->sc_nrecon = 0;
			if ((ep->sc_recon = (int *) malloc(sizeof(int) * nargs)) == (int *) 0)
				fatal(errno, "can't allocate memory for reconnect system list", (char *) 0);
			syslen = 0;
			for (; nargs > 0; nargs--, ap++) {
				nsys = 1;
				if (isnumrange(*ap, &istart, &iend))
					nsys += iend - istart;
				else
				{
					argp = *ap;
					istart = atoi(argp);
					if (argp[strlen(argp) - 1] == 'r')
						ep->sc_recon[ep->sc_nrecon++] = istart;
				}
				RBUFCHK((char **) &ep->sc_sys, &syslen, (int) (sizeof *ep->sc_sys * (ep->sc_nsys + nsys)));
				ip = ep->sc_sys + ep->sc_nsys;
				ep->sc_nsys += nsys;
				while (--nsys >= 0)
					*ip++ = istart++;
			}
			break;
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
		case SD_RANDOM:
		case SD_END_PARALLEL:
		case SD_END_REPEAT:
		case SD_END_RANDOM:
		case SD_END_TIMED_LOOP:
		case SD_END_VARIABLE:
#ifndef TET_LITE	/* -START-LITE-CUT- */
		case SD_END_REMOTE:
		case SD_END_DISTRIBUTED:
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
			ASSERT(nargs == 0);
			break;
		default:
			/* this "can't happen" */
			fatal(0, "unexpected directive",
				prscdir(dp->dt_directive));
			/* NOTREACHED */
		}
		ep->sc_fname = fname;
		ep->sc_lineno = lineno;

		/* store the directive */
		scpush(ep, &sclist);

		/*
		** if there is an attached element, we will need a matching
		** end* directive once the attached element is processed
		*/
		if (isattached(next) && (dp->dt_flags & SDF_ENDDIR)) {
			dp = getdirbyvalue(dp->dt_match);
			ASSERT(dp);
			ep = scalloc();
			ep->sc_type = SC_DIRECTIVE;
			ep->sc_directive = dp->dt_directive;
			ep->sc_flags = SCF_IMPLIED;
			ep->sc_fname = fname;
			ep->sc_lineno = lineno;
			scpush(ep, &scstack);
		}
	}

	/*
	** here we must decide what to do with the rest of the line -
	**
	** if the thing after the directive(s) is a space, there are no
	** implied end* directives and the rest of the line can be
	** processed separately
	*/

	if (!isattached(next)) {
		ASSERT(scstack == (struct scentab *) 0);
		TRACE1(tet_Tscen, 4,
			"no attached items - process rest of line");
		rc = proc1scline(next, fname, lineno);
		TRACE2(tet_Tscen, 4, "proc1dgrp(): RETURN %s", tet_i2a(rc));
		return(rc);
	}

	/*
	** but if the thing after the directive(s) is attached
	** we must determine the nature of the attached element
	** which could be one of the follwing:
	**
	**	/include-file-name
	**	@/test-case-name
	**	^scenario-name
	*/

	/*
	** separate the attached element(s) from the rest of the line -
	** in DTET mode there can only be one attached element,
	** whereas in ETET mode the rest of the line (or up to the next
	** directive) is considered to be attached
	*/
	line = next;
	for (p1 = line; *p1; p1++) {
		if (!isspace(*p1))
			continue;
		switch (tet_compat) {
		default:
			scenerror("TET_COMPAT must be set to a valid value in order to determine the number of elements attached to the directive",
				ndirs > 1 ? "group" : (char *) 0,
				lineno, fname);
			/* fall through */
		case COMPAT_DTET:
			p2 = ":";
			break;
		case COMPAT_ETET:
			for (p2 = p1 + 1; *p2; p2++)
				if (!isspace(*p2))
					break;
			break;
		}
		if (*p2 == ':') {
			*p1++ = '\0';
			break;
		}
	}
	next = p1;

	/*
	** now, line points to the (null-terminated) attached element(s)
	** and next points to the rest of the line
	*/
	switch (*line) {
	case '@':
		line++;
		/* fall through */
	case '^':
		if (proc1scline(line, fname, lineno) < 0) {
			while ((ep = scpop(&scstack)) != (struct scentab *) 0)
				scfree(ep);
			TRACE1(tet_Tscen, 4, "proc1dgrp(): return -1");
			return(-1);
		}
		break;
	case '/':
		includefile(line, fname, lineno);
		break;
	default:
		scenerror("badly formed include file name:",
			line, lineno, fname);
		break;
	}

	/* add the implied end* directives */
	while ((ep = scpop(&scstack)) != (struct scentab *) 0)
		scpush(ep, &sclist);

	/* finally, process the rest of the line and return */
	TRACE1(tet_Tscen, 4, "process rest of line after attached items");
	rc = proc1scline(next, fname, lineno);
	TRACE2(tet_Tscen, 4, "proc1dgrp(): return %s", tet_i2a(rc));
	return(rc);
}

/*
**	includefile() - interpolate the contents of an include file
*/

static void includefile(nextfile, currfile, currline)
char *nextfile, *currfile;
int currline;
{
	char buf[TET_MAX(LBUFLEN, MAXPATH)];
	FILE *fp;
	char *fname;
	int lineno = 0;
	char *args[1];
	int nargs;
	register char *p, *bp;
	char delim;

	TRACE2(tet_Tscen, 4, "includefile(): file = <%s>", nextfile);

	/* strip off unwanted characters */
	for (p = nextfile; *p; p++)
		if (!isdirsep(*p))
			break;
	if (!*p) {
		scenerror("empty include file name:", nextfile,
			currline, currfile);
		return;
	}

	/*
	** generate the full path name of the file relative to the
	** test suite root directory
	*/
	fullpath(tet_tsroot, p, buf, sizeof buf, 0);
	fname = rstrstore(buf);

	/* open the file */
	if ((fp = fopen(fname, "r")) == (FILE *) 0) {
		error(errno, "can't open", fname);
		scenerrors++;
		TRACE2(tet_Tbuf, 6, "free include file name = %s",
			tet_i2x(fname));
		free(fname);
		return;
	}

	/* read each line in turn */
	while (fgets(buf, sizeof buf, fp) != (char *) 0) {
		lineno++;

		/* strip out comments and the trailing newline */
		for (p = buf; *p; p++)
			if (*p == '\n' || *p == '#') {
				*p = '\0';
				break;
			}
		/* strip trailing spaces */
		while (--p >= buf)
			if (isspace(*p))
				*p = '\0';
			else
				break;

		/* strip leading spaces */
		for (bp = buf; *bp; bp++)
			if (!isspace(*bp))
				break;

		TRACE2(tet_Tscen, 10, "line after stripping spaces = <%s>", bp);

		/* ignore a blank line */
		if (!*bp)
			continue;

		/* check that the line contains a valid element -
			only file names and scenario information lines
			are valid here
		*/
		switch (*bp) {
		case '/':
			delim = ' ';
			break;
		case '"':
			delim = '"';
			break;
		default:
			nargs = split(bp, args, 1, ' ');
			ASSERT(nargs >= 1);
			scenerror(args[0], "is not valid in included file",
				lineno, fname);
			continue;
		}

		/* ensure that there is only one element on the line */
		for (p = bp + 1; *p; p++)
			if (*p == delim || (delim == ' ' && isspace(*p))) {
				if (delim == '"')
					p++;
				if (split(p, args, 1, ' ') > 0) {
					scenerror("too many elements",
						(char *) 0, lineno, fname);
				}
				*p = '\0';
				break;
			}

		/* send the element for processing */
		if (proc1scline(bp, fname, lineno) < 0)
			break;
	}

	/* finally, close the file and return (but don't free fname!) */
	(void) fclose(fp);
	TRACE1(tet_Tscen, 4, "includefile(): return");
}

/*
**	iszorpnum() - see if a string is a non-negative number
**
**	return 1 if it is or 0 if it isn't
*/

int iszorpnum(s)
register char *s;
{
	if (!*s)
		return(0);
	else
		while (*s && isdigit(*s))
			s++;

	return(*s ? 0 : 1);
}

/*
**      isnumrange() - see if a string is of the form m-n where m and n
**		are non-negative numbers
**
**	return 1 if it is or 0 if it isn't
**
**	if successful the values of m and n are returned indirectly
**		through *startp and *endp
*/

int isnumrange(s, startp, endp)
char *s;
int *startp, *endp;
{
	register char *p;
	register int rc;

	for (p = s; *p; p++)
		if (*p == '-') {
			*p = '\0';
			if ((rc = iszorpnum(s) & iszorpnum(p + 1)) != 0) {
				if (startp)
					*startp = atoi(s);
				if (endp)
					*endp = atoi(p + 1);
			}
			*p = '-';
			return(rc);
		}

	return(0);
}

/*
**	ifsalloc(), ifsfree() - functions to allocate and free an
**		input file stack element
*/

static struct ifstack *ifsalloc()
{
	struct ifstack *ifp;

	errno = 0;
	if ((ifp = (struct ifstack *) malloc(sizeof *ifp)) == (struct ifstack *) 0)
		fatal(errno, "can't allocate input file stack element",
			(char *) 0);

	TRACE2(tet_Tbuf, 6, "allocate ifstack element = %s", tet_i2x(ifp));
	bzero((char *) ifp, sizeof *ifp);
	return(ifp);
}

static void ifsfree(ifp)
struct ifstack *ifp;
{
	struct lcache *lcp;

	TRACE2(tet_Tbuf, 6, "free input file stack element = %s", tet_i2x(ifp));

	if (ifp == (struct ifstack *) 0)
		return;

	while ((lcp = lcpop2(ifp)) != (struct lcache *) 0)
		lcfree(lcp);

	free((void *) ifp);
}

/*
**	ifspush(), ifspop() - input file stack manipulation functions
**
**	the operation of these functions depends on the LIFO behaviour
**	of linked lists which are manipulated by tet_listinsert() and
**	tet_listremove()
*/

static void ifspush(ifp)
struct ifstack *ifp;
{
	TRACE2(tet_Tscen, 10, "ifspush(): push active filename %s on stack",
		ifp->if_fname);

	ASSERT(ifstp == ifstack);

	tet_listinsert((struct llist **) &ifstack, (struct llist *) ifp);
	ifstp = ifstack;
}

static struct ifstack *ifspop()
{
	register struct ifstack *ifp;

	ASSERT(ifstp == ifstack);

	if ((ifp = ifstack) != (struct ifstack *) 0) {
		tet_listremove((struct llist **) &ifstack,
			(struct llist *) ifp);
		ifstp = ifstack;
		TRACE2(tet_Tscen, 10, "ifspop(): pop filename %s from stack",
			ifp->if_fname);
		if (ifstp)
			TRACE2(tet_Tscen, 10, "ifspop(): active file is now %s",
				ifstp->if_fname);
		else
			TRACE1(tet_Tscen, 10,
				"ifspop(): that was the last active file");
	}
	else
		TRACE1(tet_Tscen, 10, "ifspop(): stack is empty");

	return(ifp);
}

/*
**	getline() - get the next non-blank, non-comment line
**		from the currently active input file
**
**	return a pointer to the line, or (char *) 0 on EOF or error
*/

static char *getline()
{
	static char buf[LBUFLEN];
	struct lcache *lcp;
	register char *p;

	/*
	** pop a line off the stack for the current input file
	** if there is one
	*/
	if ((lcp = lcpop()) != (struct lcache *) 0) {
		(void) strcpy(buf, lcp->lc_line);
		lcfree(lcp);
		TRACE2(tet_Tscen, 10, "getline(): line = <%s>", firstpart(buf));
		return(buf);
	}

	ASSERT(ifstp == ifstack);
	for (;;) {
		ASSERT(ifstp);
		/*
		** read the next line from the currently active file;
		** on EOF or error:
		**	if this is not the top level:
		**		close the file;
		**		(don't free fname - it's pointed to by each
		**		scentab element generated from the file)
		**		pop the currently active level off the
		**		input file stack;
		**		continue reading from the file at the
		**		newly uncovered level
		**	otherwise:
		**		return the EOF indication
		*/
		if (
			feof(ifstp->if_fp) ||
			fgets(buf, sizeof buf, ifstp->if_fp) == (char *) 0
		) {
			if (ferror(ifstp->if_fp)) {
				error(errno, "read error on", ifstp->if_fname);
				scenerrors++;
			}
			else
				TRACE2(tet_Tscen, 10,
					"getline(): encountered EOF on %s",
					ifstp->if_fname);
			if (ifstp->if_next) {
				(void) fclose(ifstp->if_fp);
				ifsfree(ifspop());
				continue;
			}
			TRACE1(tet_Tscen, 10, "getline(): return EOF");
			return((char *) 0);
		}

		/* here we have a line - increment the line counter */
		ifstp->if_lcount++;

		/* strip comments (only in column 1) */
		if (buf[0] == '#')
			continue;

		/* strip the trailing newline */
		for (p = buf; *p; p++)
			if (*p == '\n') {
				*p = '\0';
				break;
			}

		/* strip trailing spaces */
		while (--p >= buf)
			if (isspace(*p))
				*p = '\0';
			else
				break;

		/* punt a % line to the "preprocessor" */
		if (buf[0] == '%') {
			if (preprocess(&buf[1]) < 0)
				return((char *) 0);
			else
				continue;
		}

		/* if there is anything left, return it */
		if (p >= buf) {
			TRACE4(tet_Tscen, 10, "getline(): fname = %s, lineno = %s, line = <%s>",
				ifstp->if_fname, tet_i2a(ifstp->if_lcount),
				firstpart(buf));
			return(buf);
		}
	}
}

/*
**	ungetline() - store a line for subsequent retrieval by getline()
*/

static void ungetline(line)
char *line;
{
	struct lcache *lcp;

	TRACE2(tet_Tscen, 10, "ungetline(): line = <%s>", firstpart(line));

	/* store the line and push it on to the stack */
	lcp = lcalloc();
	lcp->lc_line = rstrstore(line);
	lcpush(lcp);
}

/*
**	preprocess() - process a line starting with a %
**
**	return 0 to continue or -1 to abandon processing the current file
**
**	(at present, always returns 0)
*/

static int preprocess(line)
char *line;
{
	/* list of keywords and their associated functions */
	static struct ppfuncs {
		char *pp_keyword;
		int (*pp_func) PROTOLIST((char *));
	} ppfuncs[] = {
		{ "include", ppinclude }
	};
#define Nppfuncs	(sizeof ppfuncs / sizeof ppfuncs[0])

	struct ppfuncs *pp;
	char *p;

	TRACE2(tet_Tscen, 10, "preprocess(): line = <%s>", line);

	/* strip leading spaces */
	while (*line && isspace(*line))
		line++;

	if (!*line) {
		scenerror("need a keyword after %", (char *) 0,
			ifstp->if_lcount, ifstp->if_fname);
		return(0);
	}

	/* separate the keyword from the rest of the line */
	for (p = line; *p; p++)
		if (isspace(*p)) {
			*p++ = '\0';
			break;
		}

	/* strip leading spaces from the rest of the line */
	while (*p && isspace(*p))
		p++;

	/*
	** hand the rest of the line off to the appropriate preprocessor
	** function
	*/
	for (pp = ppfuncs; pp < &ppfuncs[Nppfuncs]; pp++)
		if (!strcmp(line, pp->pp_keyword))
			return((*pp->pp_func)(p));

	scenerror("unknown % keyword", line, ifstp->if_lcount,
		ifstp->if_fname);
	return(0);
}

/*
**	ppinclude() - process the rest of the line after a %include keyword
**
**	always returns 0
*/

static int ppinclude(line)
char *line;
{
	char fname[TET_MAX(LBUFLEN, MAXPATH)];
	struct ifstack *ifp;
	FILE *fp;
	char *p;

	/* ignore extra words on the line */
	for (p = line; *p; p++)
		if (isspace(*p)) {
			*p = '\0';
			break;
		}

	/*
	** if the file name is not a full path name, generate the full path
	** name of the file relative to the test suite root directory
	*/
	fullpath(tet_tsroot, line, fname, sizeof fname, 0);

	/* check for a %include loop */
	for (ifp = ifstp; ifp; ifp = ifp->if_next)
		if (!strcmp(fname, ifp->if_fname)) {
			scenerror("%include file loop", fname,
				ifstp->if_lcount, ifstp->if_fname);
			return(0);
		}

	/* open the file */
	if ((fp = fopen(fname, "r")) == (FILE *) 0) {
		error(errno, "can't open", fname);
		scenerrors++;
		return(0);
	}

	/* set up a new level on the input file stack and return */
	ifp = ifsalloc();
	ifp->if_fname = rstrstore(fname);
	ifp->if_fp = fp;
	ifspush(ifp);
	return(0);
}

/*
**	lcalloc(), lcfree() - functions to allocate and free a
**		line cache element
*/

static struct lcache *lcalloc()
{
	register struct lcache *lcp;

	errno = 0;
	if ((lcp = (struct lcache *) malloc(sizeof *lcp)) == (struct lcache *) 0)
		fatal(errno, "can't allocate line cache element", (char *) 0);

	TRACE2(tet_Tbuf, 6, "allocate lcache element = %s", tet_i2x(lcp));
	bzero((char *) lcp, sizeof *lcp);
	return(lcp);
}

static void lcfree(lcp)
struct lcache *lcp;
{
	TRACE2(tet_Tbuf, 6, "free lcache element = %s", tet_i2x(lcp));

	if (lcp) {
		if (lcp->lc_line) {
			TRACE2(tet_Tbuf, 6, "free lcache line = %s",
				tet_i2x(lcp->lc_line));
			free(lcp->lc_line);
		}
		free((char *) lcp);
	}
}

/*
**	lcpush(), lcpop() - line cache stack manipulation functions
**
**	the operation of these functions depends on the LIFO behaviour
**	of linked lists which are manipulated by tet_listinsert() and
**	tet_listremove()
*/

static void lcpush(lcp)
struct lcache *lcp;
{
	/*
	** if all the lines read from the current file are already in
	** the cache, this line must have been read from the next file
	** down in the input file stack;
	** so move the currently active level down
	**
	** if we reach the bottom of the stack we are trying to push back
	** more lines than have previously been read,
	** so bail out on a programming error
	*/
	while (ifstp->if_lcount <= 0) {
		ASSERT(ifstp->if_next);
		ifstp = ifstp->if_next;
		TRACE2(tet_Tscen, 10, "lcpush(): active file is now %s",
			ifstp->if_fname);
	}

	TRACE4(tet_Tscen, 10, "lcpush(): push line %s on to %s stack = <%s>",
		tet_i2a(ifstp->if_lcount), ifstp->if_fname,
		firstpart(lcp->lc_line));

	tet_listinsert((struct llist **) &ifstp->if_lcache,
		(struct llist *) lcp);
	ifstp->if_lcount--;
}

static struct lcache *lcpop()
{
	/*
	** if there are no lines in the cache at the currently active level
	** on the input file stack and there are higher levels,
	** move the currectly active level up
	*/
	while (ifstp->if_lcache == (struct lcache *) 0 && ifstp->if_last) {
		ifstp = ifstp->if_last;
		TRACE2(tet_Tscen, 10, "lcpop(): active file is now %s",
			ifstp->if_fname);
	}

	return(lcpop2(ifstp));
}

/*
**	lcpop2() - pop an lcache element off the line cache stack which
**		belongs to the input file stack element at *ifp
*/

static struct lcache *lcpop2(ifp)
struct ifstack *ifp;
{
	register struct lcache *lcp;

	if ((lcp = ifp->if_lcache) != (struct lcache *) 0) {
		tet_listremove((struct llist **) &ifp->if_lcache,
			(struct llist *) lcp);
		ifp->if_lcount++;
		TRACE4(tet_Tscen, 10,
			"lcpop(): pop line %s from %s stack = <%s>",
			tet_i2a(ifp->if_lcount), ifp->if_fname,
			firstpart(lcp->lc_line));
	}
	else
		TRACE2(tet_Tscen, 10, "lcpop(): %s stack is empty",
			ifp->if_fname);

	return(lcp);
}

/*
**	firstpart() - deliver the first part of a string
**
**	if the string is longer than PLEN, it has an elipsis appended
**	to it
*/

#ifndef NOTRACE

#define PLEN	30
static char *firstpart(s)
char *s;
{
	static char dots[] = " ...";
	static char buf[PLEN + sizeof dots];

	(void) sprintf(buf, "%.*s%s", PLEN, s,
		(int) strlen(s) > PLEN ? dots : "");

	return(buf);
}

#endif /* NOTRACE */


