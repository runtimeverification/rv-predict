/*
 *	SCCS: @(#)scenpp.c	1.9 (02/05/15)
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
static char sccsid[] = "@(#)scenpp.c	1.9 (02/05/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)scenpp.c	1.9 02/05/15 TETware release 3.8
NAME:		scenpp.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	scenario file preprocessor

	NOTE that this file is not part of the TETware product
	but might be useful when analysing the behaviour of a
	complicated scenario

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., March 1997
	Port to NT

	Andrew Dingwall, UniSoft Ltd., October 1997
	Added -r option to print scenario references

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include <string.h>
#include <sys/types.h>
#include <time.h>
#include <errno.h>
#include <ctype.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "error.h"
#include "globals.h"
#include "ltoa.h"
#include "tcc.h"
#include "scentab.h"
#include "dirtab.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* command-line options */
static int printscrefs = 0;

/* output file name and stream pointer */
static char *ofname;
static FILE *ofp;

/* variables used by doindent() and printloc() */
#define INDENT	4
static int tabwidth = INDENT;
static int linectrl = 1;
static int lastlineno;
static char *lastfname;

/* static function declarations */
static void badusage PROTOLIST((void));
static void doindent PROTOLIST((int));
static char *printaddr PROTOLIST((struct scentab *));
static void printloc PROTOLIST((int, char *, int));
static void printscdir PROTOLIST((struct scentab *, int));
static void printsctree PROTOLIST((struct scentab *, int));
static void scpp_fatal PROTOLIST((int, char *, int, char *, char *));


int main(argc, argv)
int argc;
char **argv;
{
	int c;
	FILE *ifp;
	int rc;
	register struct scentab *ep;
	char *scenario = "all";
	char cwd[MAXPATH];
	extern int optind;
	extern char *optarg;

	/* must be first */
	tet_init_globals("tetscpp", PT_STAND, -1, tet_generror, scpp_fatal);

#ifndef NOTRACE
	tet_traceinit(argc, argv);
#endif

	while ((c = GETOPT(argc, argv, "PT:c:n:o:rs:t:y:")) != EOF)
		switch (c) {
		case 'P':
			linectrl = 0;
			break;
		case 'T':
			break;
		case 'c':
			c = *optarg;
			if (isupper(c))
				c = tolower(c);
			switch (c) {
			case 'd':
				tet_compat = COMPAT_DTET;
				break;
			case 'e':
				tet_compat = COMPAT_ETET;
				break;
			default:
				error(0, "warning: invalid -c option ignored:",
					optarg);
				break;
			}
			break;
		case 'n':
			if (*optarg)
				nostr(optarg, YN_CMDLINE);
			else
				badusage();
			break;
		case 'o':
			ofname = optarg;
			break;
		case 'r':
			printscrefs = 1;
			break;
		case 's':
			scenario = optarg;
			break;
		case 't':
			if ((tabwidth = atoi(optarg)) <= 0)
				tabwidth = INDENT;
			break;
		case 'y':
			if (*optarg)
				yesstr(optarg, YN_CMDLINE);
			else
				badusage();
			break;
		default:
			badusage();
			/* NOTREACHED */
		}

	argc -= optind - 1;
	argv += optind - 1;

	if (ofname) {
		if ((ofp = fopen(ofname, "w")) == (FILE *) 0)
			fatal(errno, "can't open", ofname);
	}
	else {
		ofname = "<standard output>";
		ofp = stdout;
	}

	/*
	** set tet_tsroot to the current directory -
	** this is a kluge to enable the scenario parser to perform 
	** include file processing
	*/
	errno = 0;
	if ((GETCWD(cwd, (size_t) MAXPATH)) == (char *) 0)
		fatal(errno, "getcwd() failed", (char *) 0);
	tet_tsroot = cwd;

	/*
	**	scenario processing pass 1 -
	**	tokenise each input file in turn
	**	result is a linear list of scenario elements
	**	pointed to by sclist
	*/
	if (argc <= 1) {
		if (proc1scfile(stdin, "<standard input>") < 0)
			tcc_exit(1);
		}
	else
		while (++argv, --argc > 0)
			if ((ifp = fopen(*argv, "r")) == (FILE *) 0)
				error(errno, "can't open", *argv);
			else {
				rc = proc1scfile(ifp, *argv);
				(void) fclose(ifp);
				if (rc < 0)
					tcc_exit(1);
			}

	if (scenerrors)
		scengiveup();

	/*
	**	scenario processing pass 2 -
	**	build the linear list of scenario elements at *sclist
	**	into a tree rooted at *sctree
	*/
	if (proc2sclist())
		tcc_exit(1);
	if (scenerrors)
		scengiveup();

	/*
	**	scenario processing pass 3 -
	**	prune the tree rooted at *sctree
	*/
	if (proc3sctree(scenario))
		tcc_exit(1);
	if (scenerrors)
		scengiveup();

	/* process any -y and -n command-line options */
	ynproc(YN_CMDLINE);

	/*
	**	print out all the scenarios in the tree
	*/
	for (ep = sctree; ep; ep = ep->sc_forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		ASSERT(ep->sc_type == SC_SCENARIO);
		printloc(ep->sc_lineno, ep->sc_fname, 0);
		(void) fprintf(ofp, "scenario%s(\"%s\")\n{\n",
			printaddr(ep), ep->sc_scenario);
		lastlineno += 2;
		printsctree(ep->sc_child, 1);
		(void) fprintf(ofp, "}\n\n");
		lastlineno += 2;
		(void) fflush(ofp);
	}

	tcc_exit(0);
	/* NOTREACHED */
	return(0);
}

static void badusage()
{
	static char *options[] = {
		"-P",
		"-c {dtet|etet}",
		"-n str",
		"-o output-file",
		"-r",
		"-s scenario",
		"-t tabwidth",
		"-y str",
		"files ..."
	};
	int len, pos, n;
	char *sep;
	char msg[1024];
	register char *p = msg;

	(void) sprintf(p, "\t%s", tet_progname);
	len = strlen(p);
	p += len;
	pos = len + 7;
	for (n = 0; n < sizeof options / sizeof options[0]; n++) {
		if (pos + (int) strlen(options[n]) > 75) {
			*p++ = '\n';
			sep = "\t\t";
			pos = 15;
		}
		else
			sep = " ";
		(void) sprintf(p, "%s[%s]", sep, options[n]);
		len = strlen(p);
		p += len;
		pos += len;
	}

	fatal(0, "usage:\n", msg);
}

static void printsctree(ep, indent)
struct scentab *ep;
int indent;
{
	for (; ep; ep = ep->sc_forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		switch (ep->sc_type) {
		case SC_DIRECTIVE:
			printscdir(ep, indent);
			break;
		case SC_TESTCASE:
			printloc(ep->sc_lineno, ep->sc_fname, indent);
			(void) fprintf(ofp,
				"testcase%s(\"%s\", \"%s\");\n",
				printaddr(ep), ep->sc_tcname,
				ep->sc_exiclist ? ep->sc_exiclist : "all");
			break;
		case SC_SCENINFO:
			printloc(ep->sc_lineno, ep->sc_fname, indent);
			(void) fprintf(ofp, "sceninfo%s(%s);\n",
				printaddr(ep), ep->sc_sceninfo);
			break;
		case SC_SCEN_NAME:
			printloc(ep->sc_lineno, ep->sc_fname, indent);
			(void) fprintf(ofp, "call%s(\"%s\");\n",
				printaddr(ep), ep->sc_scen_name);
			break;
		default:
			/* this "can't happen" */
			fatal(0, "unexpected type", prsctype(ep->sc_type));
			/* NOTREACHED */
		}
	}
}

static void printscdir(ep, indent)
register struct scentab *ep;
int indent;
{
	int *argv = (int *) 0;
	int argc = 0;
	int *ap;
	char **vp;
	char *func;

	ASSERT(ep->sc_type == SC_DIRECTIVE);

	switch (ep->sc_directive) {
	case SD_PARALLEL:
		func = "parallel";
		argv = &ep->sc_count;
		argc = 1;
		break;
	case SD_SEQUENTIAL:
		func = "sequential";
		break;
	case SD_REPEAT:
		func = "repeat";
		argv = &ep->sc_count;
		argc = 1;
		break;
	case SD_RANDOM:
		func = "random";
		break;
	case SD_TIMED_LOOP:
		func = "timed_loop";
		break;
#ifndef TET_LITE	/* -START-LITE-CUT- */
	case SD_REMOTE:
		func = "remote";
		argv = ep->sc_sys;
		argc = ep->sc_nsys;
		break;
	case SD_DISTRIBUTED:
		func = "distributed";
		argv = ep->sc_sys;
		argc = ep->sc_nsys;
		break;
#endif			/* -END-LITE-CUT- */
	case SD_VARIABLE:
		doindent(indent);
		(void) fprintf(ofp, "{\n");
		lastlineno++;
		for (vp = ep->sc_vars; vp < ep->sc_vars + ep->sc_nvars; vp++) {
			printloc(ep->sc_lineno, ep->sc_fname, indent + 1);
			(void) fprintf(ofp, "variable%s %s\n",
				printaddr(ep), *vp);
		}
		printsctree(ep->sc_child, indent + 1);
		doindent(indent);
		(void) fprintf(ofp, "}\n");
		lastlineno++;
		return;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected directive", prscdir(ep->sc_directive));
		/* NOTREACHED */
		return;
	}

	printloc(ep->sc_lineno, ep->sc_fname, indent);
	(void) fprintf(ofp, "%s%s(", func, printaddr(ep));
	switch (ep->sc_directive) {
	case SD_TIMED_LOOP:
		(void) fprintf(ofp, "%ld", ep->sc_seconds);
		break;
	default:
		for (ap = argv; ap < argv + argc; ap++) {
			if (ap > argv)
				(void) fprintf(ofp, ", ");
			(void) fprintf(ofp, "%d", *ap);
		}
		break;
	}
	(void) fprintf(ofp, ")\n");

	doindent(indent);
	(void) fprintf(ofp, "{\n");
	lastlineno++;
	printsctree(ep->sc_child, indent + 1);
	doindent(indent);
	(void) fprintf(ofp, "}\n");
	lastlineno++;
}

static void printloc(thislineno, thisfname, indent)
int thislineno, indent;
char *thisfname;
{
	register int doit = 0;

	if (linectrl) {
		if (!lastfname || strcmp(lastfname, thisfname)) {
			lastfname = thisfname;
			lastlineno = 0;
			doit = 1;
		}
		if (thislineno != ++lastlineno) {
			lastlineno = thislineno;
			doit = 1;
		}
		if (doit)
			(void) fprintf(ofp, "# %d \"%s\"\n",
				thislineno, thisfname);
	}

	doindent(indent);
}

static void doindent(indent)
int indent;
{
	register int space;

	for (space = indent * tabwidth; space > 7; space -= 8)
		(void) putc('\t', ofp);

	while (space-- > 0)
		(void) putc(' ', ofp);
}

static char *printaddr(ep)
struct scentab *ep;
{
	static char buf[LNUMSZ + LXNUMSZ + 3];
	char *p = buf;

	buf[0] = '\0';

	if (printscrefs) {
		(void) sprintf(p, "-%ld", ep->sc_ref);
		p += strlen(p);
	}

#ifndef NOTRACE
	if (tet_Tscen) {
		(void) sprintf(p, "@%#lx", (long) ep);
		p += strlen(p);
	}
#endif

	return(buf);
}

/*
**	tcc_exit() - clean up and exit
*/

void tcc_exit(status)
int status;
{
	exit(status);
	/* NOTREACHED */
}

static void scpp_fatal(errnum, file, line, s1, s2)
int errnum, line;
char *file, *s1, *s2;
{
	(*tet_liberror)(errnum, file, line, s1, s2);
	tcc_exit(1);
}

