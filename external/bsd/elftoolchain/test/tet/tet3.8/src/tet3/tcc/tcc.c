/*
 *	SCCS: @(#)tcc.c	1.17 (03/03/26)
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
static char sccsid[] = "@(#)tcc.c	1.17 (03/03/26) TETware release 3.8";
static char *copyright[] = {
	"(C) Copyright 1996 X/Open Company Limited",
	"All rights reserved"
};
#endif

/************************************************************************

SCCS:   	@(#)tcc.c	1.17 03/03/26 TETware release 3.8
NAME:		tcc.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	tcc main function

MODIFICATIONS:
	Geoff Clare, UniSoft Ltd., August-Sept 1996
	Missing <string.h>
	Define and initialise tet_blockable_sigs.
	Change getcwd() call to pass a buffer instead of null.

	Andrew Dingwall, UniSoft Ltd., March 1997
	Write a Uname line to the journal immediately after the
	TCC Start line

	Andrew Dingwall, UniSoft Ltd., May 1997
	port to Windows 95

	Andrew Dingwall, UniSoft Ltd., June 1997
	Moved the position of rtrcopy() to before the first time that
	anything is written to the test suite root directory.
	Added -I command-line option to print certain journal lines
	to stderr - used (at least) by the GUI/HTML interface to tcc
	in the Network Computer Reference Profile test suite.

	Andrew Dingwall, UniSoft Ltd., March 1998
	Added -V command-line option to print out version information.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
	Andrew Dingwall, The Open Group, March 2003
	Enhancement to copy source files to remote systems.


************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <time.h>
#include <string.h>
#include <errno.h>
#include <signal.h>
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtmsg.h"
#include "error.h"
#include "globals.h"
#include "dtetlib.h"
#include "sigsafe.h"
#include "tcc.h"
#include "keys.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


#ifndef _WIN32	/* -WIN32-CUT-LINE- */
sigset_t tet_blockable_sigs;	/* signals blocked by tet_sigsafe_start() */
#endif		/* -WIN32-CUT-LINE- */

/* valid command-line options */
char tcc_options[] = "IT:Va:bcef:g:i:j:l:m:n:pr:s:t:v:x:y:";

/* static function declarations */
static void badusage PROTOLIST((void));
static void prversioninfo PROTOLIST((void));


int main(argc, argv)
int argc;
char **argv;
{
	char fname[MAXPATH];
	char cwd[MAXPATH];
	int c, len;
	extern char *optarg;
	extern int optind;
	char *p;
	int argcsave = argc;
	char **argvsave = argv;
	int Vflag = 0;
	char *aopt = (char *) 0;
	char *fopt = (char *) 0;
	char *gopt = (char *) 0;
	char *iopt = (char *) 0;
	char *jopt = (char *) 0;
	char *sopt = (char *) 0;
	char *xopt = (char *) 0;
	char *tsname = (char *) 0;
	char *codelist = (char *) 0;
	char *old_journal_file = (char *) 0;
	char *scenario = "all";
	int errors = 0;
	/* -START-KEYS-CUT- */
	static char keybuf[] = KEY_DEF;
	unsigned char *key = (unsigned char *) &keybuf[1];
	time_t exptime;
	/* -END-KEYS-CUT- */

	/*
	** initialise the global variables for use by the library -
	** must be first
	*/
	tet_init_globals("tcc", PT_MTCC, 0, tcc_error, tcc_fatal);
	tet_init_blockable_sigs();

#ifndef NOTRACE
	/* initialise the trace subsystem from -T command-line options */
	tet_traceinit(argc, argv);
#endif

#ifdef _WIN32		/* -START-WIN32-CUT- */
#  ifndef TET_LITE	/* -START-LITE-CUT- */
	if (!tet_iswinNT()) {
		(void) fprintf(stderr,
			"%s: Distributed TETware needs Windows NT to run\n",
			tet_progname);
		tcc_exit(0);
	}
#  endif		/* -END-LITE-CUT- */
#endif			/* -END-WIN32-CUT- */

	/* determine the current directory */
	errno = 0;
	if ((GETCWD(cwd, (size_t) MAXPATH)) == (char *) 0)
		fatal(errno, "getcwd() failed", (char *) 0);

	/* determine the tet_root directory */
	if ((p = getenv("TET_ROOT")) == (char *) 0 || !*p) {
		fatal(0, "TET_ROOT environment variable NULL or not set",
			(char *) 0);
	}
	else
		fullpath(cwd, p, tet_root, sizeof tet_root, 0);
	TRACE2(tet_Ttcc, 1, "tet_root = %s", tet_root);
	TRACE2(tet_Ttcc, 1, "cwd = %s", cwd);

	/* determine the tet_suite_root directory */
	if ((p = getenv("TET_SUITE_ROOT")) != (char *) 0 && *p) {
		fullpath(cwd, p, fname, sizeof fname, 0);
		tet_suite_root = rstrstore(fname);
	}
	else
		tet_suite_root = tet_root;
	TRACE2(tet_Ttcc, 1, "tet_suite_root = %s", tet_suite_root);

	/* see if tet_execute has been specified */
	if ((p = getenv("TET_EXECUTE")) != (char *) 0 && *p)
		aopt = p;

	/* see if tet_run has been specified */
	if ((p = getenv("TET_RUN")) != (char *) 0 && *p) {
		fullpath(cwd, p, fname, sizeof fname, 0);
		tet_run = rstrstore(fname);
		TRACE2(tet_Ttcc, 1, "tet_run = %s", tet_run);
	}

	/* see if tet_tmp_dir has been specified */
	if ((p = getenv("TET_TMP_DIR")) != (char *) 0 && *p) {
		fullpath(cwd, p, fname, sizeof fname, 0);
		tet_tmp_dir = rstrstore(fname);
		TRACE2(tet_Ttcc, 1, "tet_tmp_dir = %s", tet_tmp_dir);
	}

	/* process the command-line arguments */
	while ((c = GETOPT(argc, argv, tcc_options)) != EOF)
		switch (c) {
		case 'I':
			tcc_Iflag = 1;
			break;
		case 'T':	/* trace options -
				   already processed by tet_traceinit() */
			break;
		case 'V':	/* print version information */
			Vflag = 1;
			break;
		case 'a':	/* alternate execution directory -
				   overrides TET_EXECUTE in the environment */
			aopt = optarg;
			break;
		case 'b':	/* build mode */
			tcc_modes |= TCC_BUILD;
			break;
		case 'c':	/* clean mode */
			tcc_modes |= TCC_CLEAN;
			break;
		case 'e':	/* exec mode */
			tcc_modes |= TCC_EXEC;
			break;
		case 'f':	/* clean config file name */
			fopt = optarg;
			break;
		case 'g':	/* build config file name */
			gopt = optarg;
			break;
		case 'i':	/* results directory */
			iopt = optarg;
			break;
		case 'j':	/* journal file name */
			jopt = optarg;
			break;
		case 'l':	/* scenario line */
			proclopt(optarg);
			break;
		case 'm':	/* resume mode */
			tcc_modes |= TCC_RESUME;
			codelist = optarg;
			break;
		case 'n':	/* "no" string for scenario processing */
			if (*optarg)
				nostr(optarg, YN_CMDLINE);
			else {
				(void) fprintf(stderr,
					"%s: no-string may not be empty\n",
					tet_progname);
				errors++;
			}
			break;
		case 'p':	/* report test case processing */
			report_progress = 1;
			break;
		case 'r':	/* rerun mode */
			tcc_modes |= TCC_RERUN;
			codelist = optarg;
			break;
		case 's':	/* scenario file name */
			sopt = optarg;
			break;
		case 't':	/* timeout for test case processing and
				   lock acqusition */
			if ((tcc_timeout = atoi(optarg)) <= 0) {
				(void) fprintf(stderr,
					"%s: timeout must be +ve\n", tet_progname);
				errors++;
			}
			break;
		case 'v':	/* config variable assignment */
			if (procvopt(optarg) < 0)
				errors++;
			break;
		case 'x':	/* exec config file name */
			xopt = optarg;
			break;
		case 'y':	/* "yes" string for scenario processing */
			if (*optarg)
				yesstr(optarg, YN_CMDLINE);
			else {
				(void) fprintf(stderr,
					"%s: yes-string may not be empty\n",
					tet_progname);
				errors++;
			}
			break;
		default:
			errors++;
			break;
		}

	argc -= optind - 1;
	argv += optind - 1;

	/* print out version info and exit if so required */
	if (Vflag)
		prversioninfo();

	/*
	** ensure that at least one of build, exec or clean mode has been
	** specfied
	*/
	TRACE2(tet_Ttcc, 1, "tcc_modes = %s", prtccmode(tcc_modes));
	if ((tcc_modes & (TCC_BUILD | TCC_EXEC | TCC_CLEAN)) == 0) {
		(void) fprintf(stderr,
			"%s: at least one of -b, -c, -e and -V must be specified\n",
			tet_progname);
		errors++;
	}

	/* ensure that only one of rerun and resume mode has been specified */
	if ((tcc_modes & TCC_RERUN) && (tcc_modes & TCC_RESUME)) {
		(void) fprintf(stderr,
			"%s: only one of -m and -r may be specified\n",
			tet_progname);
		errors++;
	}

	/* pick up the name of the old journal file in rerun or resume mode */
	if (tcc_modes & (TCC_RERUN | TCC_RESUME)) {
		if (--argc > 0) {
			fullpath(cwd, *++argv, fname, sizeof fname, 0);
			old_journal_file = rstrstore(fname);
			TRACE3(tet_Ttcc, 1,
				"codelist = %s, old journal file = %s",
				codelist, old_journal_file);
		}
		else {
			(void) fprintf(stderr,
	"%s: need an old journal file name when -m or -r is specified\n",
				tet_progname);
			errors++;
		}
	}

	/* exit now if there have been command-line syntax errors */
	if (errors)
		badusage();

	/* pick up the test suite name and the scenario name
		if they have been specified on the command line */
	if (--argc > 0)
		tsname = *++argv;
	if (--argc > 0)
		scenario = *++argv;

	/* determine the test suite root */
	if (!tsname) {
		len = strlen(tet_suite_root);
		if (
#ifdef _WIN32	/* -START-WIN32-CUT- */
			!_strnicmp(cwd, tet_suite_root, len)
#else		/* -END-WIN32-CUT- */
			!strncmp(cwd, tet_suite_root, len)
#endif		/* -WIN32-CUT-LINE- */
			&&
			isdirsep(*(cwd + len))
		) {
			tsname = cwd + strlen(tet_suite_root);
			while (isdirsep(*tsname))
				tsname++;
			(void) sprintf(fname, "%.*s",
				(int) sizeof fname - 1, tsname);
			for (p = fname; *p; p++)
				if (isdirsep(*p)) {
					*p = '\0';
					break;
				}
			tsname = rstrstore(fname);
		}
	}
	if (tsname && *tsname) {
		fullpath(tet_suite_root, tsname, fname, sizeof fname, 0);
		tet_tsroot = rstrstore(fname);
	}
	else
		fatal(0, "can't determine test suite name", (char *) 0);
	TRACE3(tet_Ttcc, 1, "test suite name = %s, test suite root = %s",
		tsname, tet_tsroot);

	/* fix up tet_execute */
	if (aopt && *aopt) {
		fullpath(cwd, aopt, fname, sizeof fname, 0);
		tet_execute = rstrstore(fname);
		TRACE2(tet_Ttcc, 1, "alternate execution directory = %s",
			tet_execute);
	}

	/* -START-KEYS-CUT- check the licence key */
	if (!(KEY_ISMAGIC(key) && (KEY_ISMAGIC(key + KEY_MLEN) ||
		((exptime = (time_t) tet_keydecode(key + KEY_MLEN)) != 0 &&
			time((time_t *) 0) <= exptime)))) {
		(void) printf("%s: the licence for this software has expired\n",
			tet_progname);
		tcc_exit(0);
	}
	/* -END-KEYS-CUT- */


	/*
	** here we have finished processing the command-line arguments
	** and environment variables
	**
	** now we are ready to call some heavyweight functions to
	** process the test suite
	*/


	/*
	** perform the initial configuration processing;
	** this stage reads in the master set of configuration variables
	** for each of the selected modes of operation, then fixes up
	** TET_COMPAT which is needed for scenario processing
	*/
	initcfg(fopt, gopt, xopt, cwd);

	/*
	** process the scenario;
	** this stage builds the scenario tree which is to be processed
	** by the execution engine
	*/
	procscen(scenario, sopt, cwd);

	/* allocate a systab for each system mentioned in the scenario */
	initsystab();

	/*
	** copy the test suite to the run-time directory on the local
	** system if so required;
	** after this is done, tet_tsroot is updated to refer to the test
	** suite root below the runtime directory
	*/
	if (tet_run && *tet_run)
		rtlcopy();

	/*
	** process the distributed configuration;
	** this stage puts tet_root, tet_tsroot etc into the distributed
	** configuration as TET_REM000_ variables
	**
	** if there are remote systems or the transport uses distributed
	** configuration variables, the distributed config file is read in
	** during this stage
	**
	** after this stage it is OK to use getdcfg() to access the
	** distributed config variables for each system
	*/
	distcfg();

	/*
	** create the results directory - the default is below tet_tsroot
	** or may be specified on the command-line
	*/
	initresdir(iopt, cwd);

	/*
	** none of the previous functions have created temporary files;
	** install signal traps for use before the execution engine starts
	*/
	initsigtrap();

#ifndef TET_LITE	/* -START-LITE-CUT- */
	/*
	** initialise the transport stuff and start all the servers;
	** this stage logs on to each of the remote systems
	** (xresd needs to know the name of the results directory
	** that is created by initresdir())
	*/
	initdtcc();

	/* initialise the "transfer file types" subsystem */
	inittft();

	/*
	** for each remote system for which TET_RUN has been specified,
	** copy the test suite to the runtime directory
	**
	** after this stage the path name returned by get_runtime_tsroot()
	** is usable on remote systems
	*/
	rtrcopy();

	/* create a saved files directory on each remote system */
	initsfdir();
#endif	/* -END-LITE-CUT- */

	/*
	** locate the results code file or create a default one;
	** propagate the results code file to each remote system,
	** using the saved files directory just created
	*/
	initrescode();

	/*
	** if rerun or resume mode has been selected, prune the scenario
	** tree so that it contains only those test cases which are to be
	** processed during this run;
	** this function does not return if there is nothing left to process
	**
	** this stage needs to use the rescode table set up by initrescode()
	*/
	if (tcc_modes & (TCC_RERUN | TCC_RESUME))
		rrproc(codelist, old_journal_file);

	/* prune the scenario tree using the -y and -n command-line options */
	ynproc(YN_CMDLINE);

	/*
	** check for timed loops with nothing to stop them from
	** continuous looping
	*/
	if ((tcc_modes & TCC_RERUN) == 0)
		check_empty_timed_loops();

	/* create TET_TMP_DIR for each system if so required */
	if (getmcflag("TET_EXEC_IN_PLACE", TCC_EXEC) == 0)
		inittmpdir();

	/* open the journal file and write the start messages to it */
	jnl_init(jopt, cwd);
	(void) printf("%s: journal file is %s\n", tet_progname, jnl_jfname());
	(void) fflush(stdout);
	jnl_tcc_start(argcsave, argvsave);
	jnl_uname();

	/*
	** fix up all the configurations and write them to the journal;
	** this stage sets up all the per-system configurations and
	** performs a config variable exchange with each remote system
	*/
	doconfig();

	/* send environment variables to each system */
	initenviron();

	/* fire up the execution engine and process the scenario */
	execscen();

	/* here if all has been successful */
	tcc_exit(0);
	/* NOTREACHED */
	return(1);
}

/*
**	badusage() - print a usage message and exit with exit code 2
*/

static void badusage()
{
	static char *options[] = {
		(char *) 0,	/* placeholder for tet_progname */
		"-{bec}",
		(char *) 0,	/* placeholder for "-{m|r} codelist" */
		"[-I]",
		"[-a alt-exec-dir]",
		"[-f clean-config-file]",
		"[-g build-config-file]",
		"[-i results-dir]",
		"[-j journal]",
		"[-l scenario-line]",
		"[-n string]",
		"[-p]",
		"[-s scenario]",
		"[-t timeout]",
		"[-v variable=value]",
		"[-x exec-config-file]",
		"[-y string]",
		(char *) 0,	/* placeholder for "old-journal-file" */
		"[test-suite [scenario]]"
	};
	int n, pos;
	char *opt, *sep;

	options[0] = tet_progname;

	(void) fprintf(stderr, "\nusage:\t");
	pos = 8;
	for (n = 0; n < sizeof options / sizeof options[0]; n++) {
		if ((opt = options[n]) == (char *) 0)
			continue;
		if (pos + (int) strlen(opt) > 78) {
			sep = "\n\t";
			pos = 7;
		}
		else
			sep = n ? " " : "";
		(void) fprintf(stderr, "%s%s", sep, opt);
		pos += strlen(opt) + 1;
	}
	(void) fprintf(stderr, "\n\nor:\t");
	pos = 8;
	for (n = 0; n < sizeof options / sizeof options[0]; n++) {
		if ((opt = options[n]) == (char *) 0)
			switch (n) {
			case 2:
				opt = "-{m|r} codelist";
				break;
			default:
				opt = "old-journal-file";
				break;
			}
		if (pos + (int) strlen(opt) > 78) {
			sep = "\n\t";
			pos = 7;
		}
		else
			sep = n ? " " : "";
		(void) fprintf(stderr, "%s%s", sep, opt);
		pos += strlen(opt) + 1;
	}
	(void) fprintf(stderr, "\n\nor:\ttcc -V\n\n");

	tcc_exit(2);
	/* NOTREACHED */
}

/*
**	prversioninfo() - print out version information and exit
*/

static void prversioninfo()
{
	static char product_name[] =
#ifdef KEY_DIGITS	/* -START-KEYS-CUT- */
						"TETware";
#else			/* -END-KEYS-CUT- */
						"TET3";
#endif			/* -KEYS-CUT-LINE- */

	(void) fprintf(stderr, "%s: %s%s Release %s\n",
		tet_progname,
#ifdef TET_LITE		/* -LITE-CUT-LINE- */
		product_name, "-Lite",
#else			/* -START-LITE-CUT- */
		"Distributed ", product_name,
#endif			/* -END-LITE-CUT- */
		"3.8");

	tcc_exit(0);
	/* NOTREACHED */
}

