/*
 *	SCCS: @(#)config.c	1.15 (02/01/18)
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
static char sccsid[] = "@(#)config.c	1.15 (02/01/18) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)config.c	1.15 02/01/18 TETware release 3.8
NAME:		config.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	functions which deal with configuration variables

MODIFICATIONS:
	Geoff Clare, UniSoft Ltd., August 1996
	Missing <string.h>.

	Andrew Dingwall, UniSoft Ltd., June 1997.
	added get_runtime_tsroot() to return the runtime value of
	TET_TSROOT - required to support the correct operation of TET_RUN
	when a remote system's test suite root directory resides on
	a read-only file system

	Andrew Dingwall, UniSoft Ltd., June 1998
	Don't try to do a config variable exchange with a remote system
	when TET_TSROOT is undefined or when remote config file is
	inaccessible.
	Give TET_REMnnn variables in the master configurations priority
	over generic ones when performing a configuration variable exchange.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for configuration variable expansion.

	Andrew Dingwall, UniSoft Ltd., December 1998
	In TETware-Lite, write the local system's per-system configurations
	to the temporary files instead of the master configurations.

	Andrew Dingwall, UniSoft Ltd., May 1999
	Fixed a bug which left the value of TET_TSROOT in dvar[]
	incorrect when the TET_RUN environment variable was specified
	AND tcc was invoked with a -v command-line option.

	Andrew Dingwall, UniSoft Ltd., March 2000
	We now keep the per-system configuration lists separate from the
	systab.
	This is to permit a remote system not mentioned in the chosen
	scenario to be the target of a tet_remexec() request.
	Although configuration variable expansion still doesn't work
	properly for such systems (and to make it work would be rather
	difficult), this change is a step in the right direction!
	

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <sys/types.h>
#include <ctype.h>
#include <errno.h>
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "error.h"
#include "bstring.h"
#include "globals.h"
#include "ltoa.h"
#include "llist.h"
#include "config.h"
#include "servlib.h"
#include "dtetlib.h"
#include "systab.h"
#include "tcc.h"
#include "dtcc.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* default config file names on each system */
static char *defcfname[] = {
	"tetbuild.cfg",
	"tetexec.cfg",
	"tetclean.cfg",
#ifndef TET_LITE	/* -START-LITE-CUT- */
	"tetdist.cfg"
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
};

/* the default config file name for a particular mode */
#define DEFCFNAME(mode)	(defcfname[TC_CONF_MODE(mode)])

/* config file names - these get filled in by initcfg() */
static char *cfname[sizeof defcfname / sizeof defcfname[0]];

/* the actual config file name for a particular mode */
#define CFNAME(mode)	(cfname[TC_CONF_MODE(mode)])


/*
** the master configuration lists
**
** there are master lists for: build mode, execute mode, clean mode,
** the so-called distributed variables and variables specified with -v
** on the command-line
**
** for each list there is a pointer, a length and the number of entries in
** the list
*/
static struct cflist mcflist[TC_NCONF_MODES + 1];
static struct cflist vopts;

/* the master config list for a particular mode */
#define MCFLIST(mode)	(mcflist[TC_CONF_MODE(mode)])


/*
** the per-system configuration lists
**
** there is a set of configuration variable lists for each system
** mentioned in the chosen scenario,
** and also for each system nnn mentioned by a TET_REMnnn_ variable
**
** each set of configuration variable lists is held in a table
*/
/*
** structure of a configuration list table element
** the cft_next and cft_last elements must be first and second so as to
** enable the table to be manipulated by the llist routines
*/
struct cftab {
	struct cftab *cft_next;		/* ptr to next entry in the table */
	struct cftab *cft_last;		/* ptr to previous entry in the table */
	int cft_magic;			/* magic number */
	int cft_sysid;			/* system id */
	struct cflist cft_list[TC_NCONF_MODES + 1];
					/* configuration lists */
	int cft_setup;			/* flags to say which lists have been
					   set up */
};
/* pointer to the start of the table */
static struct cftab *cftab;

#define CFT_MAGIC		0x63466C54
#define CFLIST(tp, mode)	((tp)->cft_list[TC_CONF_MODE(mode)])
#define Ncftlist(tp)		(sizeof (tp)->cft_list / \
					sizeof (tp)->cft_list[0])

/* macros to test and set the cft_setup flags */
#define SET_CFSETUP(tp, mode)	((tp)->cft_setup |= 1 << TC_CONF_MODE(mode))
#define IS_CFSETUP(tp, mode)	((tp)->cft_setup & (1 << TC_CONF_MODE(mode)))


/* config error counter */
static int conferrors;


#ifdef TET_LITE /* -LITE-CUT-LINE- */

/*
** file names for the temporary master config files which are passed
** to test cases via the TET_CONFIG communication variable
*/
static char *tcfname[sizeof defcfname / sizeof defcfname[0]];
#define Ntcfname	(sizeof tcfname / sizeof tcfname[0])
#define TCFNAME(mode)	(tcfname[TC_CONF_MODE(mode)])

#else /* -START-LITE-CUT- */

/*
** file names for the temporary master exec, distrib and command-line
** config files which are passed to XRESD in order to support tet_remexec()
*/
static char *ecfname, *dcfname, *ccfname;

#endif /* TET_LITE */	/* -END-LITE-CUT- */


/*
** names of defined distributed variables
**
** environment variables with these names on the master system are added
** to the distributed configuration
**
** note that the number and order of these variables must
** correspond to the initialisation code in initdvar()
*/
struct dvar {
	char *dv_name;		/* variable name */
	int dv_needed;		/* variable is needed on each system */
	int dv_len;		/* strlen(mv_name) */
	char **dv_vp;		/* ptr to value on master system */
};
static struct dvar dvar[] = {
	{ "TET_ROOT",		1 },
	{ "TET_SUITE_ROOT",	0 },
	{ "TET_TSROOT",		1 },
	{ "TET_EXECUTE",	0 },
	{ "TET_TMP_DIR",	0 },
	{ "TET_RUN",		0 }
};
#define Ndvar	(sizeof dvar / sizeof dvar[0])

/*
** names of variables that may appear in the master distributed configuration
** but should not be copied to the per-system distributed configurations
*/
static struct dvar mdvar[] = {
	{ "TET_LOCALHOST", 	0 },
	{ "TET_XTI_MODE",	0 },
	{ "TET_XTI_TPI",	0 }
};
#define Nmdvar	(sizeof mdvar / sizeof mdvar[0])


/*
** a simple stack structure -
** used to detect configuration variable substitution loops
*/
struct cfstack {
	struct cfstack *cs_last;	/* pointer to the previous element */
	char *cs_name;			/* variable name */
};


/* static function declarations */
static void addvopts PROTOLIST((struct cflist *));
static int cfix2 PROTOLIST((int));
static int cflag2bool PROTOLIST((char *, char *));
static void cflcopy PROTOLIST((struct cflist *, struct cflist *, int));
static struct cftab *cftalloc PROTOLIST((void));
static struct cftab *cftcheck PROTOLIST((int));
static struct cftab *cftfind PROTOLIST((int));
static void cftadd PROTOLIST((struct cftab *));
static int cftmax PROTOLIST((void));
static void checkbvar PROTOLIST((struct cflist *, int, int));
static void compat_fix PROTOLIST((void));
static void confgiveup PROTOLIST((void));
static void config_variable_dollar PROTOLIST((void));
static void cvd2 PROTOLIST((int));
static void cvd3 PROTOLIST((struct cflist *, int, int));
static void cvd4 PROTOLIST((char *));
static void config_variable_expand PROTOLIST((struct cflist *, int, int));
static char *cp2value PROTOLIST((char **, int, int, struct cfstack *));
static void cve2 PROTOLIST((char **, int, int, struct cfstack *));
static void cve3 PROTOLIST((char **, char *, char *, int, int, int *,
	struct cfstack *));
static char *cve3_getvalue PROTOLIST((char *, char *, int, int,
	struct cfstack *));
static char *cve3_dist PROTOLIST((char *, char *, int, int, struct cfstack *));
static char *cve3_opmode PROTOLIST((char *, char *, int, int, int,
	struct cfstack *));
static void cve_error PROTOLIST((char *, int, int, char *));
static int docff2 PROTOLIST((char *, struct cflist *, char *));
static char *docffile PROTOLIST((struct cflist *, char *));
static void docfl2 PROTOLIST((struct cftab *, int));
static void docfloc PROTOLIST((struct cftab *));
static char **findcfg PROTOLIST((char *, struct cflist *));
static char **finddcfg PROTOLIST((char *, int));
static void fix_tet_api_compliant PROTOLIST((int));
static void fix_tet_pass_tc_name PROTOLIST((int));
static void initdvar PROTOLIST((void));
static void initmdvar PROTOLIST((void));
static int is_dist_var PROTOLIST((char *));
static int is_mdist_var PROTOLIST((char *));
static void proccfl2 PROTOLIST((char *, struct cflist *));
static void proccfline PROTOLIST((char *, struct cflist *, int, char *));
static void readmconf PROTOLIST((char *, struct cflist *));
static void reportcfg PROTOLIST((struct cflist *, int, int));
#ifndef TET_LITE	/* -START-LITE-CUT- */
static void cftfree PROTOLIST((struct cftab *));
static void cftrm PROTOLIST((struct cftab *));
static void cftsetup PROTOLIST((int));
static void docfr2 PROTOLIST((struct cftab *, struct cflist *, int));
static void docfr3 PROTOLIST((struct systab *, struct cflist *,
	struct cflist *, struct cflist *, int));
static void docfrem PROTOLIST((struct cftab *));
static void reportdcfg PROTOLIST((void));
#endif /* !TET_LITE */	/* -END-LITE-CUT- */


/************************************************************************
*									*
*	configuration file processing functions				*
*									*
************************************************************************/


/*
**	initcfg() - perform initial configuration
**
**	fopt is the -f command-line option
**	gopt is the -g command-line option
**	xopt is the -x command-line option
**	cwd is tcc's initial working directory
*/

void initcfg(fopt, gopt, xopt, cwd)
char *fopt, *gopt, *xopt, *cwd;
{
	static char tet_version[] =
#ifdef TET_LITE	/* -LITE-CUT-LINE- */
					"TET_VERSION=3.8-lite";
#else		/* -START-LITE-CUT- */
					"TET_VERSION=3.8";
#endif		/* -END-LITE-CUT- */
	char fname[MAXPATH];

	TRACE5(tet_Ttcc, 2, "initcfg(): fopt = \"%s\", gopt = \"%s\", xopt = \"%s\", cwd = \"%s\"",
		fopt ? fopt : "", gopt ? gopt : "", xopt ? xopt : "", cwd);

	/* read in the configuration variables for each of the chosen
		modes of operation */
	if (tcc_modes & TCC_BUILD) {
		if (gopt && *gopt)
			fullpath(cwd, gopt, fname, sizeof fname, 0);
		else
			fullpath(tet_tsroot, DEFCFNAME(CONF_BUILD), fname,
				sizeof fname, 0);
		readmconf(fname, &MCFLIST(CONF_BUILD));
		CFNAME(CONF_BUILD) = rstrstore(fname);
		TRACE2(tet_Ttcc, 1, "build config file = %s",
			CFNAME(CONF_BUILD));
	}
	if (tcc_modes & TCC_EXEC) {
		if (xopt && *xopt)
			fullpath(cwd, xopt, fname, sizeof fname, 0);
		else if (tet_execute && *tet_execute) {
			fullpath(tet_execute, DEFCFNAME(CONF_EXEC), fname,
				sizeof fname, 0);
			if (tet_eaccess(fname, 0) < 0)
				fullpath(tet_tsroot, DEFCFNAME(CONF_EXEC),
					fname, sizeof fname, 0);
		}
		else
			fullpath(tet_tsroot, DEFCFNAME(CONF_EXEC), fname,
				sizeof fname, 0);
		readmconf(fname, &MCFLIST(CONF_EXEC));
		CFNAME(CONF_EXEC) = rstrstore(fname);
		TRACE2(tet_Ttcc, 1, "exec config file = %s",
			CFNAME(CONF_EXEC));
	}
	if (tcc_modes & TCC_CLEAN) {
		if (fopt && *fopt)
			fullpath(cwd, fopt, fname, sizeof fname, 0);
		else
			fullpath(tet_tsroot, DEFCFNAME(CONF_CLEAN), fname,
				sizeof fname, 0);
		readmconf(fname, &MCFLIST(CONF_CLEAN));
		CFNAME(CONF_CLEAN) = rstrstore(fname);
		TRACE2(tet_Ttcc, 1, "clean config file = %s",
			CFNAME(CONF_CLEAN));
	}

	if (conferrors)
		confgiveup();

	/*
	** add in the command-line variables
	** supply default TET_API_COMPLIANT and TET_PASS_TC_NAME
	** add in TET_VERSION
	*/
	if (tcc_modes & TCC_BUILD) {
		addvopts(&MCFLIST(CONF_BUILD));
		fix_tet_api_compliant(CONF_BUILD);
		fix_tet_pass_tc_name(CONF_BUILD);
		proccfl2(tet_version, &MCFLIST(CONF_BUILD));
	}
	if (tcc_modes & TCC_EXEC) {
		addvopts(&MCFLIST(CONF_EXEC));
		fix_tet_api_compliant(CONF_EXEC);
		fix_tet_pass_tc_name(CONF_EXEC);
		proccfl2(tet_version, &MCFLIST(CONF_EXEC));
	}
	if (tcc_modes & TCC_CLEAN) {
		addvopts(&MCFLIST(CONF_CLEAN));
		fix_tet_api_compliant(CONF_CLEAN);
		fix_tet_pass_tc_name(CONF_CLEAN);
		proccfl2(tet_version, &MCFLIST(CONF_CLEAN));
	}

	/* fix up tet_compat */
	compat_fix();

	if (conferrors)
		confgiveup();

	TRACE1(tet_Ttcc, 2, "initcfg() RETURN");
}

/*
**	readmconf() - read in a set of master configuration variables
**		from a configuration file
*/

static void readmconf(fname, lp)
char *fname;
struct cflist *lp;
{
	FILE *fp;
	char buf[MAXPATH * 2];
	register char *p;
	int lcount = 0;

	/* open the config file */
	if ((fp = fopen(fname, "r")) == (FILE *) 0) {
		error(errno, "can't open", fname);
		conferrors++;
		return;
	}

	/* process each line in turn -
	** ignore blank lines and comments
	** trim trailing spaces
	*/
	while (fgets(buf, sizeof buf, fp) != (char *) 0) {
		lcount++;
		for (p = buf; *p; p++)
			if (*p == '\n' || *p == '#') {
				*p = '\0';
				break;
			}
		while (--p >= buf)
			if (isspace(*p))
				*p = '\0';
			else
				break;
		if (p >= buf)
			proccfline(buf, lp, lcount, fname);
	}

	(void) fclose(fp);
}

/*
**	proccfline() - process a single configuration line
*/

static void proccfline(line, lp, lineno, fname)
char *line, *fname;
struct cflist *lp;
int lineno;
{
	static char fmt[] =
		"bad format config variable assignment at line %d in file";
	char msg[sizeof fmt + LNUMSZ];

	/* check the format of the config line */
	if (!tet_equindex(line) || !tet_remvar(line, -1)) {
		(void) sprintf(msg, fmt, lineno);
		error(0, msg, fname);
		conferrors++;
		return;
	}

	/* perform common variable assignment processing */
	proccfl2(line, lp);
}

/*
**	proccfl2() - common config line processing
*/

static void proccfl2(line, lp)
char *line;
register struct cflist *lp;
{
	register char **cp;

	/* add a new value or update an existing one */
	if ((cp = findcfg(line, lp)) == (char **) 0) {
		RBUFCHK((char **) &lp->cf_conf, &lp->cf_lconf,
			(int) ((lp->cf_nconf + 1) * sizeof *lp->cf_conf));
		*(lp->cf_conf + lp->cf_nconf++) = rstrstore(line);;
	}
	else {
		ASSERT(*cp);
		if (strcmp(line, *cp)) {
			TRACE2(tet_Tbuf, 6, "proccfl2(): free config line = %s",
				tet_i2x(*cp));
			free((void *) *cp);
			*cp = rstrstore(line);
		}
	}
}

/*
**	addvopts() - add the command-line variables (except distributed
**		variables) to a configuration variable list
**
**	this function should not be called for the master distributed
**	configuration list
*/

static void addvopts(lp)
struct cflist *lp;
{
	register char **cp;

	TRACE1(tet_Ttcc, 6, "addvopts()");

	ASSERT(lp != &MCFLIST(CONF_DIST));

	for (cp = vopts.cf_conf; cp < vopts.cf_conf + vopts.cf_nconf; cp++)
		if (!is_mdist_var(*cp))
			proccfl2(*cp, lp);

	TRACE1(tet_Ttcc, 6, "addvopts() RETURN");
}

/*
**	fix_tet_api_compliant() - provide a default value for TET_API_COMPLIANT
*/

static void fix_tet_api_compliant(mode)
int mode;
{
	static char name[] = "TET_API_COMPLIANT";
	char line[sizeof name + 6];

	TRACE2(tet_Ttcc, 6, "fix_tet_api_compliant(%s)", prcfmode(mode));

	ASSERT(CONF_MODE_OK(mode, mcflist));

	/* return now if TET_API_COMPLIANT is already defined */
	if (getmcfg(name, mode))
		return;

	/* here to add a default value of !TET_OUTPUT_CAPTURE */
	(void) sprintf(line, "%s=%s", name,
		getmcflag("TET_OUTPUT_CAPTURE", mode) ? "False" : "True");
	proccfl2(line, &MCFLIST(mode));
}

/*
**	fix_tet_pass_tc_name() - provide a default value for TET_PASS_TC_NAME
*/

static void fix_tet_pass_tc_name(mode)
int mode;
{
	static char name[] = "TET_PASS_TC_NAME";
	char line[sizeof name + 6];

	TRACE2(tet_Ttcc, 6, "fix_pass_tc_name(%s)", prcfmode(mode));

	ASSERT(CONF_MODE_OK(mode, mcflist));

	/* return now if TET_PASS_TC_NAME is already defined */
	if (getmcfg(name, mode))
		return;

	/* here to add a default value of TET_OUTPUT_CAPTURE */
	(void) sprintf(line, "%s=%s", name,
		getmcflag("TET_OUTPUT_CAPTURE", mode) ? "True" : "False");
	proccfl2(line, &MCFLIST(mode));
}

/*
**	compat_fix() - fix up the value of tet_compat
**
**	the value of TET_COMPAT is extracted from the master config list
**	for each of the selected modes of operation
*/

static void compat_fix()
{
	int compat;
	int conflict;

	TRACE1(tet_Ttcc, 6, "compat_fix()");

	/*
	** get the value of TET_COMPAT from each of the master
	** configurations and check for conflicts
	*/
	tet_compat = cfix2(CONF_BUILD);
	conflict = 0;

	if ((compat = cfix2(CONF_EXEC)) != 0) {
		if (tet_compat && compat != tet_compat)
			conflict++;
		tet_compat = compat;
	}

	if ((compat = cfix2(CONF_CLEAN)) != 0) {
		if (tet_compat && compat != tet_compat)
			conflict++;
		tet_compat = compat;
	}

	/* report a conflict if one has been detected */
	if (conflict) {
		error(0, "conflicting values for TET_COMPAT",
			"have been specified in the per-mode configurations");
		conferrors++;
	}

#ifndef NOTRACE
	if (tet_compat)
		TRACE2(tet_Tscen, 1, "running in %sTET compatibility mode",
			tet_compat == COMPAT_ETET ? "E" : "D");
	else
		TRACE1(tet_Tscen, 1, "no compatibility mode specified");
#endif
}

/*
**	cfix2() - extend the compat_fix() processing for a particular
**		master configuration list
**
**	return COMPAT_DTET or COMPAT_ETET if TET_COMPAT has been so
**	defined, or 0 if TET_COMPAT has not been defined with a valid value
*/

static int cfix2(mode)
int mode;
{
	static char compatname[] = "TET_COMPAT";
	register char *p;

	if ((p = getmcfg(compatname, mode)) != (char *) 0)
		switch (*p) {
		case 'D':
		case 'd':
			return(COMPAT_DTET);
		case 'E':
		case 'e':
			return(COMPAT_ETET);
		default:
			error(0, compatname, "variable has ambiguous value");
			conferrors++;
			break;
		}

	return(0);
}

/*
**	distcfg() - process the distributed configuration
*/

void distcfg()
{
	struct dvar *dvp;
	int cfmax, sysid, sysmax;
	char *p;
	struct cftab *tp;
	char buf[MAXPATH + 40];
	static char fmt1[] =
		"%s distributed configuration variable for system %d";
	static char fmt2[] =
		"%s distributed configuration variable not defined for system";
#ifndef TET_LITE	/* -START-LITE-CUT- */
	char **cp;
	struct cflist *lp;
	char fname[MAXPATH];
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	TRACE1(tet_Ttcc, 2, "distcfg(): process distributed configuration");

	initdvar();

	/*
	** copy the (non-empty) defined environment variables to the
	** distributed configuration for the local system
	*/
	tp = cftcheck(0);
	for (dvp = dvar; dvp < dvar + Ndvar; dvp++) {
		if (!dvp->dv_vp || !*dvp->dv_vp || !**dvp->dv_vp)
			continue;
		(void) sprintf(buf, "%s=%.*s", dvp->dv_name,
			(int) sizeof buf - dvp->dv_len - 1,
			*dvp->dv_vp);
		proccfl2(buf, &CFLIST(tp, CONF_DIST));
	}
	SET_CFSETUP(tp, CONF_DIST);


	sysmax = symax();
	ASSERT_LITE(sysmax == 0);

	/*
	** set up a configuration list table entry for each system
	** mentioned in the chosen scenario
	*/
	for (sysid = 0; sysid <= sysmax; sysid++)
		if (syfind(sysid))
			(void) cftcheck(sysid);


#ifndef TET_LITE	/* -START-LITE-CUT- */

	/*
	** if we need the distributed config file, read it in; then add in
	** distributed variables with nnn > 0 from the tcc command line set
	*/
	if (sysmax > 0 || ts_needdist()) {
		fullpath(tet_tsroot, DEFCFNAME(CONF_DIST), fname,
			sizeof fname, 0);
		readmconf(fname, &MCFLIST(CONF_DIST));
		CFNAME(CONF_DIST) = rstrstore(fname);
		TRACE2(tet_Ttcc, 1, "distributed config file = %s",
			CFNAME(CONF_DIST));
		for (cp = vopts.cf_conf; cp < vopts.cf_conf + vopts.cf_nconf; cp++)
			if (is_mdist_var(*cp) && tet_remvar_sysid(*cp) != 0)
				proccfl2(*cp, &MCFLIST(CONF_DIST));
	}

	/*
	** scan the master configurations for TET_REMnnn_ variables;
	** ensure that we have a configuration list table entry for
	** each system nnn
	*/
	if (tcc_modes & TCC_BUILD)
		cftsetup(CONF_BUILD);
	if (tcc_modes & TCC_EXEC)
		cftsetup(CONF_EXEC);
	if (tcc_modes & TCC_CLEAN)
		cftsetup(CONF_CLEAN);

	/*
	** copy the defined distributed variables to the appropriate
	** per-system distributed configuration(s)
	**
	** if a defined distributed variable has a TET_REMnnn_ prefix:
	**	then the unadorned variable assignment is copied to the
	**	distributed configuration for that system
	** otherwise:
	**	the variable assignment is copied to the distributed
	**	configuration for each sysid
	**
	** the first stage also creates a configuration list table entry
	** if a system is not mentioned in the chosen scenario but a
	** TET_REMnnn_ variable is defined for it in the master
	** distributed configuration
	*/
	lp = &MCFLIST(CONF_DIST);
	for (cp = lp->cf_conf; cp < lp->cf_conf + lp->cf_nconf; cp++) {
		if (!is_dist_var(*cp) || (sysid = tet_remvar_sysid(*cp)) <= 0)
			continue;
		p = tet_remvar(*cp, -1);
		ASSERT(p && p != *cp);
		tp = cftcheck(sysid);
		proccfl2(p, &CFLIST(tp, CONF_DIST));
	}
	cfmax = cftmax();
	for (cp = lp->cf_conf; cp < lp->cf_conf + lp->cf_nconf; cp++) {
		if (!is_dist_var(*cp) || (sysid = tet_remvar_sysid(*cp)) > 0)
			continue;
		ASSERT(sysid == -1);
		for (sysid = 1; sysid <= cfmax; sysid++) {
			if ((tp = cftfind(sysid)) != (struct cftab *) 0)
				proccfl2(*cp, &CFLIST(tp, CONF_DIST));
		}
	}

	/* mark the destination lists as being set up */
	for (sysid = 1; sysid <= cfmax; sysid++)
		if ((tp = cftfind(sysid)) != (struct cftab *) 0)
			SET_CFSETUP(tp, CONF_DIST);


	/*
	** perform variable expansion on the distributed configurations 
	** for remote systems
	*/
	config_variable_expand(&MCFLIST(CONF_DIST), -1, CONF_DIST);
	for (sysid = 1; sysid <= cfmax; sysid++)
		if ((tp = cftfind(sysid)) != (struct cftab *) 0)
			config_variable_expand(&CFLIST(tp, CONF_DIST),
				sysid, CONF_DIST);

#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	/* ensure that we have all the dist config variables that we need */
	for (sysid = 0; sysid <= sysmax; sysid++) {
		if (!syfind(sysid))
			continue;
		for (dvp = dvar; dvp < dvar + Ndvar; dvp++) {
			if (!dvp->dv_needed)
				continue;
			p = getdcfg(dvp->dv_name, sysid);
			if (!p || !*p) {
				(void) sprintf(buf, fmt2, dvp->dv_name);
				error(0, buf, tet_i2a(sysid)); 
				conferrors++;
			}
		}
	}

	/* ensure that all the dist config variables refer to full path names */
	cfmax = cftmax();
	for (sysid = 0; sysid <= cfmax; sysid++) {
		if (!cftfind(sysid))
			continue;
		for (dvp = dvar; dvp < dvar + Ndvar; dvp++) {
			p = getdcfg(dvp->dv_name, sysid);
			if (p && *p) {
				if (sysid == 0)
					ASSERT(isabspathloc(p));
				else if (!isabspathrem(p)) {
					(void) sprintf(buf, fmt1,
						dvp->dv_name, sysid);
					error(0, buf,
						"is not an absolute path name");
					conferrors++;
				}
			}
		}
	}

	if (conferrors)
		confgiveup();

	TRACE1(tet_Ttcc, 2, "distcfg() RETURN");
}


#ifndef TET_LITE	/* -START-LITE-CUT- */

/*
**	cftsetup() - scan a master configuration for TET_REMnnn_ variables;
**		ensure that we have a configuration list table entry for each
**		system nnn
*/

static void cftsetup(mode)
int mode;
{
	struct cflist *lp;
	char **cp;
	int sysid;

	TRACE2(tet_Ttcc, 6, "cftsetup(): look for TET_REMnnn_ variables in the %s configuration",
		prcfmode(mode));

	ASSERT(CONF_MODE_OK(mode, mcflist));

	lp = &MCFLIST(mode);

	for (cp = lp->cf_conf; cp < lp->cf_conf + lp->cf_nconf; cp++)
		if ((sysid = tet_remvar_sysid(*cp)) > 0)
			(void) cftcheck(sysid);

	TRACE1(tet_Ttcc, 6, "cftsetup() RETURN");
}

/*
**	procvopt() - process a -v command-line option
**
**	return 0 if successful or -1 on error
*/

#endif /* !TET_LITE */	/* -END-LITE-CUT- */


int procvopt(line)
char *line;
{
	/* check the format of the config line */
	if (!tet_equindex(line) || !tet_remvar(line, -1)) {
		error(0, "-v option is badly formatted:", line);
		return(-1);
	}

	/* perform common variable assignment processing */
	proccfl2(line, &vopts);
	return(0);
}

/************************************************************************
*									*
*	configuration fixup and reporting functions			*
*									*
************************************************************************/

/*
**	doconfig() - fix up all the configurations and write them to the
**		journal
*/

void doconfig()
{
	struct cftab *tp;
	struct cflist *blp, *elp, *clp;
#ifndef TET_LITE	/* -START-LITE-CUT- */
	struct systab *sp;
	struct cflist tmp;
	struct cflist *lp;
	char **cp;
	int sysid, sysmax, cfmax;
	static char fmt[] = "TET_REM%03d_%.*s";
	char buf[MAXPATH + sizeof fmt];
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	TRACE1(tet_Ttcc, 2, "doconfig(): fix up all the configurations and write them to the journal");

	/*
	** if we are processing on the local system, fix up the local configs
	** and arrange to report the local configs to the journal;
	** otherwise, arrange to report the master configs to the journal
	*/
	if (syfind(0) != (struct systab *) 0) {
		tp = cftfind(0);
		ASSERT(tp);
		docfloc(tp);
		blp = &CFLIST(tp, CONF_BUILD);
		elp = &CFLIST(tp, CONF_EXEC);
		clp = &CFLIST(tp, CONF_CLEAN);
	}
	else {
		blp = &MCFLIST(CONF_BUILD);
		elp = &MCFLIST(CONF_EXEC);
		clp = &MCFLIST(CONF_CLEAN);
	}


#ifndef TET_LITE	/* -START-LITE-CUT- */

	/* fix up the remote configs */
	cfmax = cftmax();
	for (sysid = 1; sysid <= cfmax; sysid++)
		if ((tp = cftfind(sysid)) != (struct cftab *) 0) 
			docfrem(tp);

#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	/* give up if any of the above failed */
	if (conferrors)
		confgiveup();

	/*
	** now that all the config variable expansion is done,
	** condense each occurrence of $$ to a single $
	*/
	config_variable_dollar();

	/* report the local/master configs to the journal */
	if (tcc_modes & TCC_BUILD)
		reportcfg(blp, 0, CONF_BUILD);
	if (tcc_modes & TCC_EXEC)
		reportcfg(elp, 0, CONF_EXEC);
	if (tcc_modes & TCC_CLEAN)
		reportcfg(clp, 0, CONF_CLEAN);


#ifdef TET_LITE		/* -LITE-CUT-LINE- */

	/*
	** write out the temporary config files for the local system which are
	** passed to test cases via the TET_CONFIG communication variable
	*/
	if (
		(tcc_modes & TCC_BUILD) &&
		(TCFNAME(CONF_BUILD) = docffile(blp, "build")) == (char *) 0
	)
		conferrors++;
	if (
		(tcc_modes & TCC_EXEC) &&
		(TCFNAME(CONF_EXEC) = docffile(elp, "exec")) == (char *) 0
	)
		conferrors++;
	if (
		(tcc_modes & TCC_CLEAN) &&
		(TCFNAME(CONF_CLEAN) = docffile(clp, "clean")) == (char *) 0
	)
		conferrors++;

#else			/* -START-LITE-CUT- */

	/* report the dist config */
	sysmax = symax();
	if (sysmax > 0 || ts_needdist())
		reportdcfg();

	/* report the remote configs */
	for (sysid = 1; sysid <= sysmax; sysid++) {
		if ((sp = syfind(sysid)) == (struct systab *) 0) 
			continue;
		tp = cftfind(sysid);
		ASSERT(tp);
		if (tcc_modes & TCC_BUILD)
			reportcfg(&CFLIST(tp, CONF_BUILD), sysid, CONF_BUILD);
		if (tcc_modes & TCC_EXEC)
			reportcfg(&CFLIST(tp, CONF_EXEC), sysid, CONF_EXEC);
		if (tcc_modes & TCC_CLEAN)
			reportcfg(&CFLIST(tp, CONF_CLEAN), sysid, CONF_CLEAN);
	}

	/*
	** write the master exec, distributed and command-line config
	** variables to temporary files for use with tet_remexec()
	*/
	if ((ecfname = docffile(&MCFLIST(CONF_EXEC), "master exec")) == (char *) 0)
		conferrors++;

#if 0
	if ((dcfname = docffile(&MCFLIST(CONF_DIST), "distributed")) == (char *) 0)
		conferrors++;
#endif

	tmp.cf_conf = (char **) 0;
	tmp.cf_lconf = 0;
	tmp.cf_nconf = 0;
	for (sysid = 0; sysid <= cfmax; sysid++) {
		if ((tp = cftfind(sysid)) == (struct cftab *) 0) 
			continue;
		lp = &CFLIST(tp, CONF_DIST);
		for (cp = lp->cf_conf; cp < lp->cf_conf + lp->cf_nconf; cp++) {
			if (!*cp)
				continue;
			(void) sprintf(buf, fmt, sysid % 1000,
				(int) (sizeof buf - sizeof fmt), *cp);
			RBUFCHK((char **) &tmp.cf_conf, &tmp.cf_lconf,
				(int) ((tmp.cf_nconf + 1) * sizeof *tmp.cf_conf));
			*(tmp.cf_conf + tmp.cf_nconf++) = rstrstore(buf);
		}
	}
	if ((dcfname = docffile(&tmp, "distributed")) == (char *) 0)
		conferrors++;
	for (cp = tmp.cf_conf; cp < tmp.cf_conf + tmp.cf_nconf; cp++) {
		TRACE2(tet_Tbuf, 6, "free tmp dist config var = %s",
			tet_i2x(*cp));
		free((void *) *cp);
	}
	TRACE2(tet_Tbuf, 6, "free tmp dist config var list = %s",
		tet_i2x(tmp.cf_conf));
	free((void *) tmp.cf_conf);

	if ((ccfname = docffile(&vopts, "command-line")) == (char *) 0)
		conferrors++;

	/* give up if any of the above failed */
	if (conferrors)
		confgiveup();

	/* then send the file names to XRESD */
	if (tet_xdcfname(ecfname, dcfname, ccfname) < 0) {
		error(tet_xderrno, "can't send config file names to XRESD",
			(char *) 0);
		conferrors++;
	}

	/*
	** free up configuration list table entries that refer
	** to systems not in the chosen scenario
	*/
	for (sysid = 0; sysid <= cfmax; sysid++)
		if (
			(tp = cftfind(sysid)) != (struct cftab *) 0 &&
			syfind(sysid) == (struct systab *) 0
		) {
			TRACE2(tet_Ttcc, 4, "doconfig(): removing un-needed configuration list table entry for system %s",
				tet_i2a(sysid));
			cftrm(tp);
			cftfree(tp);
		}

#endif /* TET_LITE */	/* -END-LITE-CUT- */


	/* give up if any of the above failed */
	if (conferrors)
		confgiveup();
}

/*
**	docfloc() - fix up the local configs for each of the selected
**		modes of operation
*/

static void docfloc(tp)
struct cftab *tp;
{
	TRACE1(tet_Ttcc, 2, "docfloc(): fix up all the local configurations");

	if (tcc_modes & TCC_BUILD)
		docfl2(tp, CONF_BUILD);

	if (tcc_modes & TCC_EXEC)
		docfl2(tp, CONF_EXEC);

	if (tcc_modes & TCC_CLEAN)
		docfl2(tp, CONF_CLEAN);
}

/*
**	docfl2() - extend the docfloc() processing for a particular mode
*/

static void docfl2(tp, mode)
struct cftab *tp;
int mode;
{
	struct cflist *from, *to;

	TRACE2(tet_Ttcc, 3, "docfl2(): fix up the local %s configuration",
		prcfmode(mode));

	/*
	** determine the source (master config list)
	** and destination (per-system config list for the local system),
	** and copy the variables across
	*/
	ASSERT(CONF_MODE_OK(mode, tp->cft_list));
	from = &MCFLIST(mode);
	to = &CFLIST(tp, mode);
	cflcopy(from, to, 0);

	/* mark the destination list as being set up */
	SET_CFSETUP(tp, mode);

	/* perform variable expansion if so required */
	if (getcflag("TET_EXPAND_CONF_VARS", 0, mode))
		config_variable_expand(to, 0, mode);

	/* finally, check all of the boolean variables */
	checkbvar(to, 0, mode);
}


#ifndef TET_LITE	/* -START-LITE-CUT- */

/*
**	docfrem() - fix up the remote configs for each of the selected
**		modes of operation
*/

static void docfrem(tp)
struct cftab *tp;
{
	struct cflist tmp;

	TRACE2(tet_Ttcc, 2,
		"docfrem() - fix up all the configurations on system %s",
		tet_i2a(tp->cft_sysid));

	ASSERT(tp->cft_sysid > 0);

	/* initialise the scratchpad */
	tmp.cf_conf = (char **) 0;
	tmp.cf_lconf = 0;
	tmp.cf_nconf = 0;

	/* fix up the configs for each mode */
	if (tcc_modes & TCC_BUILD)
		docfr2(tp, &tmp, CONF_BUILD);
	if (tcc_modes & TCC_EXEC)
		docfr2(tp, &tmp, CONF_EXEC);
	if (tcc_modes & TCC_CLEAN)
		docfr2(tp, &tmp, CONF_CLEAN);

	/* free the scratchpad area */
	if (tmp.cf_conf) {
		TRACE2(tet_Tbuf, 6, "free tmp config list = %s",
			tet_i2x(tmp.cf_conf));
		free((void *) tmp.cf_conf);
	}
}

/*
**	docfr2() - extend the docfrem() processing for a particular mode
*/

static void docfr2(tp, tcfp, mode)
struct cftab *tp;
struct cflist *tcfp;
int mode;
{
	struct cflist *from, *to;
	struct systab *sp;

	TRACE3(tet_Ttcc, 3,
		"docfr2(): fix up the %s configuration on system %s",
		prcfmode(mode), tet_i2a(tp->cft_sysid));

	/*
	** determine the source (master config list)
	** and destination (per-system config list for this system)
	*/
	ASSERT(CONF_MODE_OK(mode, tp->cft_list));
	from = &MCFLIST(mode);
	to = &CFLIST(tp, mode);

	/*
	** perform the configuration variable exchange if this system
	** is mentioned in the chosen scenario;
	** otherwise, just copy the variables to their destination
	*/
	if ((sp = syfind(tp->cft_sysid)) != (struct systab *) 0)
		docfr3(sp, from, to, tcfp, mode);
	else
		cflcopy(from, to, tp->cft_sysid);

	/* mark the destination list as being set up */
	SET_CFSETUP(tp, mode);

	/* perform variable expansion if so required */
	if (getcflag("TET_EXPAND_CONF_VARS", tp->cft_sysid, mode))
		config_variable_expand(to, tp->cft_sysid, mode);

	/* finally, check all of the boolean variables */
	checkbvar(to, tp->cft_sysid, mode);
}

static void docfr3(sp, from, to, tcfp, mode)
struct systab *sp;
struct cflist *from, *to, *tcfp;
int mode;
{
	char fname[MAXPATH];
	char *texec, *tsroot;
	register char **cp;
	static char fmt1[] = "can't access %s mode configuration file %.*s on system";
	static char fmt2[] = "tet_tcxconfig() failed when performing %s mode configuration variable exchange with system";
	char msg[sizeof fmt1 + MAXPATH + 6];

	/* determine the config file name on the remote system */
	if ((tsroot = getdcfg("TET_TSROOT", sp->sy_sysid)) == (char *) 0) {
		error(0, "TET_TSROOT not defined for system",
			tet_i2a(sp->sy_sysid));
		conferrors++;
		return;
	}
	ASSERT(CONF_MODE_OK(mode, defcfname));
	if (mode == CONF_EXEC &&
		(texec = getdcfg("TET_EXECUTE", sp->sy_sysid)) != (char *) 0) {
			fullpath(texec, DEFCFNAME(mode), fname,
				sizeof fname, 1);
			if (tet_tcaccess(sp->sy_sysid, fname, 0) < 0)
				fullpath(tsroot, DEFCFNAME(mode), fname,
					sizeof fname, 1);
	}
	else
		fullpath(tsroot, DEFCFNAME(mode), fname, sizeof fname, 1);

	/* make sure that the remote config file is accessible */
	if (tcc_access(sp->sy_sysid, fname, 04) < 0) {
		(void) sprintf(msg, fmt1, prcfmode(mode), MAXPATH, fname);
		error(errno ? errno : tet_tcerrno, msg, tet_i2a(sp->sy_sysid));
		conferrors++;
		return;
	}

	/*
	** build a list of configuration variables to send to the remote
	** system which consists of all the generic variables plus all
	** the TET_REMnnn variables for that system;
	** the generic variables go first so as to give TET_REMnnn variables
	** priority over them
	*/
	tcfp->cf_nconf = 0;
	for (cp = from->cf_conf; cp < from->cf_conf + from->cf_nconf; cp++) {
		if (tet_remvar(*cp, -1) != *cp)
			continue;
		RBUFCHK((char **) &tcfp->cf_conf, &tcfp->cf_lconf,
			(int) ((tcfp->cf_nconf + 1) * sizeof *tcfp->cf_conf));
		*(tcfp->cf_conf + tcfp->cf_nconf++) = *cp;
	}
	for (cp = from->cf_conf; cp < from->cf_conf + from->cf_nconf; cp++) {
		if (tet_remvar(*cp, sp->sy_sysid) == *cp)
			continue;
		RBUFCHK((char **) &tcfp->cf_conf, &tcfp->cf_lconf,
			(int) ((tcfp->cf_nconf + 1) * sizeof *tcfp->cf_conf));
		*(tcfp->cf_conf + tcfp->cf_nconf++) = *cp;
	}

	/* perform a config variable exchange with the remote system */
	if (tet_tcxconfig(sp->sy_sysid, fname, tcfp, &vopts, to) < 0) {
		(void) sprintf(msg, fmt2, prcfmode(mode));
		error(tet_tcerrno, msg, tet_i2a(sp->sy_sysid));
		conferrors++;
	}
}

#endif /* !TET_LITE */	/* -END-LITE-CUT- */


/*
**	per_system_config()
**
**	return a pointer to a per-system configuration list for the
**		specified mode
*/

struct cflist *per_system_config(sysid, mode)
int sysid, mode;
{
	struct cftab *tp;

	tp = cftfind(sysid);
	ASSERT(tp);
	ASSERT(CONF_MODE_OK(mode, tp->cft_list));
	ASSERT(IS_CFSETUP(tp, mode));

	return(&CFLIST(tp, mode));
}


/*
**	cflcopy() - copy configuration variables from a master list to
**		a per-system list
*/

static void cflcopy(from, to, sysid)
struct cflist *from, *to;
int sysid;
{
	char *p, **cp;

	/*
	** do two passes down the source list;
	**
	** in the first pass we add/update all the non TET_REMnnn_
	** variables into the destination list
	**
	** in the second pass we add/update all the TET_REMnnn_
	** variables for the local system into the destination list without
	** the TET_REMnnn_ prefix
	*/

	/* first pass */
	for (cp = from->cf_conf; cp < from->cf_conf + from->cf_nconf; cp++)
		if (tet_remvar(*cp, -1) == *cp)
			proccfl2(*cp, to);

	/* second pass */
	for (cp = from->cf_conf; cp < from->cf_conf + from->cf_nconf; cp++)
		if ((p = tet_remvar(*cp, sysid)) != *cp) {
			ASSERT(p);
			proccfl2(p, to);
		}
}

/*
**	checkbvar() - check that all the boolean variables are
**		either "True" or "False" in a particular configuration
*/

static void checkbvar(lp, sysid, mode)
struct cflist *lp;
int sysid;
int mode;
{
	/* list of boolean configuration variables */
	static struct bvar {
		char *bv_name;
		int bv_len;
	} bvar[] = {
		{ "TET_EXEC_IN_PLACE" },
		{ "TET_OUTPUT_CAPTURE" },
		{ "TET_API_COMPLIANT" },
		{ "TET_PASS_TC_NAME" },
		{ "TET_TRANSFER_SAVE_FILES" },
		{ "TET_EXPAND_CONF_VARS" }
	};

#define Nbvar	(sizeof bvar / sizeof bvar[0])

	static char fmt[] = "bad value for boolean variable %s in %s configuration on system";
	char msg[sizeof fmt + 40];
	register char *p1, *p2;
	register char **cp;
	register struct bvar *vp;

	TRACE3(tet_Ttcc, 2, "checkbvar(): check the boolean variables in the %s configuration for system %s",
		prcfmode(mode), tet_i2a(sysid));

	/* initialise the name lengths first time through */
	if (!bvar[0].bv_len)
		for (vp = bvar; vp < bvar + Nbvar; vp++)
			vp->bv_len = strlen(vp->bv_name);

	/*
	** search the list for boolean variables and check that each one is
	** either "true" or "false" (actually we only check for 't' and 'f')
	*/
	for (cp = lp->cf_conf; cp < lp->cf_conf + lp->cf_nconf; cp++) {
		if ((p1 = tet_remvar(*cp, -1)) == (char *) 0 ||
			(p2 = tet_equindex(p1)) == (char *) 0)
				continue;
		for (vp = bvar; vp < bvar + Nbvar; vp++) {
			if (vp->bv_len != p2 - p1 ||
				strncmp(p1, vp->bv_name, vp->bv_len))
					continue;
			switch (*++p2) {
			case 'T':
			case 'F':
			case 't':
			case 'f':
				break;
			default:
				(void) sprintf(msg, fmt, vp->bv_name,
					prcfmode(mode));
				error(0, msg, tet_i2a(sysid));
				conferrors++;
				break;
			}
			break;
		}
	}
}

/*
**	reportcfg() - report a per-mode configuration to the journal
*/

static void reportcfg(lp, sysid, mode)
struct cflist *lp;
int sysid, mode;
{
	char **cp;

	TRACE3(tet_Ttcc, 3,
		"reportcfg(): report %s mode configuration for system %s",
		prcfmode(mode), tet_i2a(sysid));

	ASSERT_LITE(sysid == 0);
	ASSERT(mode != CONF_DIST);

	/* emit the config start message */
	if (sysid == 0) {
		ASSERT(CONF_MODE_OK(mode, cfname));
		jnl_mcfg_start(CFNAME(mode), mode);
	}
#ifndef TET_LITE	/* -START-LITE-CUT- */
	else
		jnl_scfg_start(sysid, mode);
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	/* emit the config lines */
	for (cp = lp->cf_conf; cp < lp->cf_conf + lp->cf_nconf; cp++)
		jnl_cfg(*cp);

	/* emit the config end message */
	jnl_cfg_end();
}


#ifndef TET_LITE	/* -START-LITE-CUT- */

/*
**	reportdcfg() - report the distributed configuration to the journal
*/

static void reportdcfg()
{
	struct systab *sp;
	struct cflist *lp;
	char **cp;
	int sysid, sysmax;
	char *buf = (char *) 0;
	int buflen = 0;

	TRACE1(tet_Ttcc, 3,
		"reportdcfg(): report the distributed configuration");

	/* emit the config start message */
	jnl_mcfg_start(CFNAME(CONF_DIST), CONF_DIST);

	/*
	** first, report all the defined distributed configuration
	** variables in sysid order
	*/
	for (sysid = 0, sysmax = symax(); sysid <= sysmax; sysid++) {
		if ((sp = syfind(sysid)) == (struct systab *) 0)
			continue;
		lp = per_system_config(sysid, CONF_DIST);
		for (cp = lp->cf_conf; cp < lp->cf_conf + lp->cf_nconf; cp++) {
			RBUFCHK(&buf, &buflen, (int) strlen(*cp) + 12);
			(void) sprintf(buf, "TET_REM%03d_%s",
				sysid % 1000, *cp);
			jnl_cfg(buf);
		}
	}

	if (buf) {
		TRACE2(tet_Tbuf, 6, "reportdcfg(): free buf = %s",
			tet_i2x(buf));
		free((void *) buf);
		buf = (char *) 0;
		buflen = 0;
	}

	/*
	** then, report the other variables (if any) that are in
	** the master distributed configuration
	*/
	lp = &MCFLIST(CONF_DIST);
	for (cp = lp->cf_conf; cp < lp->cf_conf + lp->cf_nconf; cp++)
		if (!is_dist_var(*cp))
			jnl_cfg(*cp);

	/* emit the config end message */
	jnl_cfg_end();
}

#endif /* !TET_LITE */	/* -END-LITE-CUT- */


/*
**	docffile() - write out a configuration to a temporary file
**		and return a pointer to the file name which is stored
**		in malloc'd memory
**
**	return (char *) 0 on error
*/

static char *docffile(lp, type)
struct cflist *lp;
char *type;
{
	char *fname;

	/* get a temporary file name */
	if ((fname = tet_mktfname("tcc")) == (char *) 0)
		return(fname);

	/* write out the variables to the file */
	if (docff2(fname, lp, type) < 0) {
		TRACE2(tet_Tbuf, 6, "free tmp config file name = %s",
			tet_i2x(fname));
		free((void *) fname);
		fname = (char *) 0;
	}

	return(fname);
}

/*
**	docff2() - extend the docffile() processing
**
**	return 0 if successful or -1 on error
*/

static int docff2(fname, lp, type)
register struct cflist *lp;
char *fname, *type;
{
	register char **cp;
	FILE *fp;

	/* open the file */
	if ((fp = fopen(fname, "w")) == (FILE *) 0) {
		error(errno, "can't open", fname);
		return(-1);
	}

	/* write out the variables */
	(void) fprintf(fp, "# %s configuration variables\n\n", type);
	for (cp = lp->cf_conf; cp < lp->cf_conf + lp->cf_nconf; cp++)
		if (fprintf(fp, "%s\n", *cp) < 0) {
			error(errno, "write error on", fname);
			(void) fclose(fp);
			return(-1);
		}

	/* close the file */
	if (fclose(fp) < 0) {
		error(errno, "close error on", fname);
		return(-1);
	}

	/* all OK so return success */
	return(0);
}

/************************************************************************
*									*
*	configuration variable expansion				*
*									*
************************************************************************/


/*
**	config_variable_expand() - expand variables in the specified
**		configuration
**
**	if a value contains a ${variable}, then the variable's value is
**	interpolated
**
**	a $$ in the value is left in place for now -
**	later it will be condensed to a single $
**
**	lp points to the configuration list to process
**	if the list is for a per-system configuration, sysid is the
**	system number;
**	otherwise, if the list is for the distributed configuration,
**	sysid should be -1
*/

static void config_variable_expand(lp, sysid, mode)
struct cflist *lp;
int sysid, mode;
{
	char **cp;

#ifndef NOTRACE
	if (mode == CONF_DIST && sysid < 0)
		TRACE1(tet_Ttcc, 3, "config_variable_expand(): expand master distributed configuration variables");
	else
		TRACE3(tet_Ttcc, 3, "config_variable_expand(): expand %s mode configuration variables for system %s",
			prcfmode(mode), tet_i2a(sysid));

	TRACE1(tet_Ttcc, 10, "configuration list before expansion:");
	if (tet_Ttcc > 0) {
		for (cp = lp->cf_conf; cp < lp->cf_conf + lp->cf_nconf; cp++) {
			TRACE4(tet_Ttcc, 10, "\tcp = %s, *cp = %s -> \"%s\"",
				tet_i2x(cp), tet_i2x(*cp),
				*cp ? *cp : "<NULL>");
		}
	}
#endif

	ASSERT_LITE(sysid == 0 || mode == CONF_DIST);

	/* process each configuration variable assignment in turn */
	for (cp = lp->cf_conf; cp < lp->cf_conf + lp->cf_nconf; cp++)
		cve2(cp, sysid, mode, (struct cfstack *) 0);

#ifndef NOTRACE
	TRACE1(tet_Ttcc, 10, "configuration list after expansion:");
	if (tet_Ttcc > 0) {
		for (cp = lp->cf_conf; cp < lp->cf_conf + lp->cf_nconf; cp++) {
			TRACE4(tet_Ttcc, 10, "\tcp = %s, *cp = %s -> \"%s\"",
				tet_i2x(cp), tet_i2x(*cp),
				*cp ? *cp : "<NULL>");
		}
	}
	TRACE1(tet_Ttcc, 3, "config_variable_expand() RETURN");
#endif

}

/*
**	cve2() - extend the config_variable_expand() processing
**		for a particular variable assignment
**
**	this function can be called recursively -
**	the stack at *stp is used to detect a configuration variable
**	substitution loop
*/

#ifdef NOTRACE
#  define TRACE_ENTER
#  define TRACE_RETURN
#else
#  define TRACE_ENTER \
	level++; \
	TRACE6(tet_Ttcc, 8, "cve2() ENTER at level %s, cp = %s, sysid = %s, mode = %s, assignment = \"%s\"", \
		tet_i2a(level), tet_i2x(cp), tet_i2a(sysid), \
		prcfmode(mode), *cp)
#  define TRACE_RETURN \
	TRACE2(tet_Ttcc, 8, "cve2() RETURN from level %s", tet_i2a(level)); \
	--level; \
	return
#endif


static void cve2(cp, sysid, mode, stp)
char **cp;
int sysid, mode;
struct cfstack *stp;
{
	char *p, *s;
	int syntax_errors;
	static char fmt[] = "ignoring useless %.50s";
	char msg[sizeof fmt + 50];
#ifndef NOTRACE
	static int level;
#endif

	TRACE_ENTER;

	/*
	** if this variable is in a distributed configuration:
	**
	**	if it is a master distributed configuration variable:
	**		ignore defined distributed variables
	*/
	if (mode == CONF_DIST) {
		if (sysid < 0) {
			if (is_dist_var(*cp)) {
				TRACE1(tet_Ttcc, 8, "cve2(): ignoring a defined distributed variable in the master distributed configuration");
				TRACE_RETURN;
			}
			if (tet_remvar_sysid(*cp) >= 0) {
				p = tet_equindex(*cp);
				ASSERT(*p == '=');
				*p = '\0';
				(void) sprintf(msg, fmt, *cp);
				*p = '=';
				error(0, msg,
		"variable assignment in the distributed configuration");
				TRACE_RETURN;
			}
		}
		else
			ASSERT(tet_remvar_sysid(*cp) == -1);
	}

	/*
	** loop until all the ${variable}s have been expanded
	** but leave all the $$s in place for now
	*/
	syntax_errors = 0;
	for (;;) {
		p = tet_equindex(*cp);
		ASSERT(p);
		for (p += 1; *p; p++)
			if (*p == '$') {
				if (*(p + 1) == '$')
					p++;
				else
					break;
			}
		if (!*p)
			break;
		s = rstrstore(*cp);
		p = tet_equindex(s);
		ASSERT(p);
		*p++ = '\0';
		cve3(cp, s, p, sysid, mode, &syntax_errors, stp);
		TRACE2(tet_Tbuf, 6, "cve2(): free config line = %s",
			tet_i2x(s));
		free((void *) s);
	}

	TRACE_RETURN;
}

/*
**	cve3() - extend the config_variable_expand() processing some more
**		when the variable assignment at *cp is known to contain
**		a ${variable}
**
**	name and value should point to null-terminated COPIES of the original
**	assignment
**	these copies can get modified below here
*/

static void cve3(cp, name, value, sysid, mode, errp, stp)
char **cp, *name, *value;
int sysid, mode, *errp;
struct cfstack *stp;
{
	static char badsyntax[] = "bad variable substitution syntax";
	char *p, *head, *tail, *fullvar;
	int buflen, len;

	TRACE2(tet_Ttcc, 8, "cve3(): expand variable assignment \"%s\"", *cp);

	/*
	** split the value into three null-terminated parts:
	**	head
	**	variable name (fullvar)
	**	tail
	**
	** again, a $$ is left in place for now
	*/
	head = value;
	for (p = value; *p; p++)
		if (*p == '$') {
			if (*(p + 1) == '$')
				p++;
			else
				break;
		}
	ASSERT(*p == '$');
	*p++ = '\0';
	if (*p == '{') {
		fullvar = tail = ++p;
		for (; *p; p++)
			if (*p == '}') {
				*p = '\0';
				tail = p + 1;
				break;
			}
			else if (*p == '$') {
				fullvar = (char *) 0;
				tail = p;
				break;
			}
	}
	else {
		fullvar = (char *) 0;
		tail = p;
	}

	/*
	** here:
	**	fullvar points to the name of the variable to be interpolated
	**	or is (char *) 0 to indicate a syntax error
	**	head points to the part of the value before the variable name
	**	tail points to the part of the value after the variable name
	**
	** each of these strings are null-terminated
	*/

	/* interpolate the value and return */
	if (!fullvar || !*fullvar) {
		if ((*errp)++ == 0)
			cve_error(name, mode, sysid, badsyntax);
		p = (char *) 0;
	}
	else
		p = cve3_getvalue(name, fullvar, sysid, mode, stp);
	len = (int) (strlen(name) + strlen(head) + strlen(tail)) + 2;
	if (p)
		len += (int) strlen(p);
	buflen = (int) strlen(*cp) + 1;
	RBUFCHK(cp, &buflen, len);
	(void) sprintf(*cp, "%s=%s%s%s", name, head, p ? p : "", tail);

	TRACE2(tet_Ttcc, 8, "cve3(): assignment after expansion: \"%s\"", *cp);
}

/*
**	cve3_getvalue() - extend the cve3 processing
**
**	return pointer to the value to be interpolated,
**	or (char *) 0 on error
*/

static char *cve3_getvalue(name, fullvar, sysid, mode, stp1)
char *name, *fullvar;
int sysid, mode;
struct cfstack *stp1;
{
	static char subloop[] = "variable substitution loop";
	static char fmt[] = "can't find a value to substitute for ${%.40s}";
	char msg[sizeof fmt + 40];
	struct cfstack *stp2;
	char *p, *var;
	int mmm;

	/*
	** if this is a recursive call, stp1 points to a stack containing the
	** names of all the variables currently in the process of
	** being interpolated
	**
	** check for a variable substitution loop by looking back up the
	** stack to see if we are already trying to interpolate the
	** current variable name
	*/
	for (stp2 = stp1; stp2; stp2 = stp2->cs_last)
		if (!strcmp(name, stp2->cs_name)) {
			cve_error(name, mode, sysid, subloop);
			return((char *) 0);
		}

	/*
	** set var to the short (unadorned) name of the variable whose value
	** is to be looked up
	*/
	if ((mmm = tet_remvar_sysid(fullvar)) >= 0) {
		var = tet_remvar(fullvar, mmm);
		ASSERT(fullvar != var);
	}
	else {
		ASSERT(mmm == -1);
		var = fullvar;
	}

	/* look up the value of the variable that is to be interpolated */
	if (mode == CONF_DIST)
		p = cve3_dist(name, var, mmm, sysid, stp1);
	else
		p = cve3_opmode(name, var, mmm, sysid, mode, stp1);

	if (!p) {
		(void) sprintf(msg, fmt, fullvar);
		cve_error(name, mode, sysid, msg);
	}

	return(p);
}

/*
**	cve3_dist() - extend the config_variable_expand() processing for a
**		distributed configuration variable assignment
**
**	return a pointer to the value to be interpolated,
**	or (char *) 0 if no value can be found
**
** variable assignment can take one of these forms:
**
**	FOO=head${TET_REMmmm_BAR}tail
**	FOO=head${BAR}tail
**
** TET_REMnnn_FOO variables don't appear in a per-system distributed
** configuration
** TET_REMnnn_FOO variables in the master distributed configuration
** get filtered out in cve2() above
**
** the following strategy is used:
**
** if ${fullvar} is TET_REMmmm_BAR:
**	look for BAR in the distributed configuration for system mmm;
**	if not found:
**		look for BAR in the distributed configuration for system sysid;
**		if not found:
**			report an error
** otherwise:
**	(${fullvar} is BAR)
**	look for BAR in the distributed configuration for system sysid;
**	if not found:
**		report an error
*/

static char *cve3_dist(name, var, mmm, sysid, stp)
char *name, *var;
int mmm, sysid;
struct cfstack *stp;
{
	struct cfstack stack;
	char **cp;
	int n;

	/*
	** make cp point to the address of the configuration variable
	** assignment whose value we want to interpolate
	**
	** most of the work is done by finddcfg() which looks for
	** var in the per-system distributed configuration first, then
	** drops back to looking in the master distributed configuration
	*/
	if ((n = (mmm >= 0) ? mmm : sysid) >= 0)
		cp = finddcfg(var, n);
	else
		cp = findcfg(var, &MCFLIST(CONF_DIST));

	/*
	** perform recursive varible expansion on the chosen variable
	** and return its value
	*/
	stack.cs_name = name;
	stack.cs_last = stp;
	return(cp2value(cp, n, CONF_DIST, &stack));
}

/*
**	cve3_opmode() - extend the config_variable_expand() processing for a
**		build, exec or clean mode configuration variable assignment
**
**	return a pointer to the value to be interpolated,
**	or (char *) 0 if no value can be found
**
** variable assignment can take one of these forms:
**
**	FOO=head${TET_REMmmm_BAR}tail
**	FOO=head${BAR}tail
**
** (there are no TET_REMnnn_ variables in a per-system configuration)
**
** the following strategy is used:
**
** if ${fullvar} is one of the known distributed variables:
**	if ${fullvar} is TET_REMmmm_BAR:
**		get the value of BAR for system mmm from the
**		distributed configuration;
**		if not defined:
**			report an error
**	otherwise:
**		(${fullvar} is BAR)
**		get the value of BAR for system sysid from the
**		distributed configuration;
**		if not defined:
**			report an error
** otherwise:
**	(${fullvar} is not a distributed variable)
**	if ${fullvar} is TET_REMmmm_BAR:
**		look for BAR in the configuration for system mmm;
**		if not found:
**			report an error
**	otherwise:
**		(${fullvar} is BAR)
**		look for BAR in the configuration for system sysid;
**		if not found:
**			report an error
*/

static char *cve3_opmode(name, var, mmm, sysid, mode, stp)
char *name, *var;
int mmm, sysid, mode;
struct cfstack *stp;
{
	struct cftab *tp;
	struct cfstack stack;
	int n;

	n = (mmm >= 0) ? mmm : sysid;

	/*
	** if var is one of the defined distributed configuration variables,
	** return its value
	**
	** we don't need to perform recursive variable expansion on a
	** distributed configuration variable because all such variables
	** have already been fully expanded
	*/
	if (is_dist_var(var))
		return(cp2value(finddcfg(var, n), n, CONF_DIST,
			(struct cfstack *) 0));

	/*
	** not a defined distributed configuration variable, so look for
	** the variable in the configuration for the current mode
	**
	** perform recursive variable expansion on it before returning
	** its value
	*/
	if ((tp = cftfind(n)) == (struct cftab *) 0)
		return((char *) 0);
	ASSERT(CONF_MODE_OK(mode, tp->cft_list));
	stack.cs_name = name;
	stack.cs_last = stp;
	return(cp2value(findcfg(var, &CFLIST(tp, mode)), n, mode, &stack));
}

/*
**	cp2value() - return pointer to the value part of a
**		configuration variable assignment whose address is
**		pointed to by cp
*/

static char *cp2value(cp, sysid, mode, stp)
char **cp;
int sysid, mode;
struct cfstack *stp;
{
	char *p;

	/* return now if we have been passed a NULL pointer */
	if (cp == (char **) 0)
		return((char *) 0);

	/*
	** if required, perform recursive configuration variable expansion on
	** the value that we are about to return
	*/
	if (stp)
		cve2(cp, sysid, mode, stp);

	/* locate the value and return it */
	p = tet_equindex(*cp);
	ASSERT(p);
	return(p + 1);
}

/*
**	cve_error() - common error reporting function for
**		configuration variable expansion errors
*/

static void cve_error(name, mode, sysid, text)
char *name, *text;
int mode, sysid;
{
	static char fmt1[] = "%.80s in %.40s assignment in the %s";
	static char fmt2[] = "%.14s for system %d";
	static char conf[] = "configuration";
	char msg1[sizeof fmt1 + 80 + 40 + 14 + LNUMSZ];
	char msg2[sizeof fmt2 + 14 + LNUMSZ];
	char *p;

	if (mode == CONF_DIST) {
		p = "Distributed";
		(void) sprintf(msg2, conf);
	}
	else {
		p = prcfmode(mode);
		if (sysid < 0) {
			(void) sprintf(msg2, "%s %s", p, conf);
			p = "master";
		}
		else
			(void) sprintf(msg2, fmt2, conf, sysid);
	}
	(void) sprintf(msg1, fmt1, text, name, p);
	error(0, msg1, msg2);

	conferrors++;
}

/*
**	config_variable_dollar() - condense each occurrence of $$ to $
**		in variable assignments
*/

static void config_variable_dollar()
{
	if (tcc_modes & TCC_BUILD)
		cvd2(CONF_BUILD);
	if (tcc_modes & TCC_EXEC)
		cvd2(CONF_EXEC);
	if (tcc_modes & TCC_CLEAN)
		cvd2(CONF_CLEAN);
	cvd2(CONF_DIST);
}

/*
**	cvd2() - extend the config_variable_dollar() processing for
**		a particular mode of operation
*/

static void cvd2(mode)
int mode;
{
	struct cftab *tp;
	int sysid, cfmax;

#ifndef TET_LITE	/* -START-LITE-CUT- */

	if (mode == CONF_DIST)
		cvd3(&MCFLIST(CONF_DIST), -1, CONF_DIST);

#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	/*
	** for each system:
	**	process the list if:
	**		mode is DIST and sysid > 0, or
	**		variable expansion is enabled
	*/
	for (sysid = 0, cfmax = cftmax(); sysid <= cfmax; sysid++)
		if (
			(mode != CONF_DIST || sysid > 0) &&
			(tp = cftfind(sysid)) != (struct cftab *) 0 &&
			(
				mode == CONF_DIST ||
				getcflag("TET_EXPAND_CONF_VARS", sysid, mode)
			)
		)
			cvd3(&CFLIST(tp, mode), sysid, mode);
}

/*
**	cvd3() - extend the config_variable_dollar() processing for
**		a particular configuration list
*/

static void cvd3(lp, sysid, mode)
struct cflist *lp;
int sysid, mode;
{
	char **cp;

	/*
	** process each variable assigment in the list
	**
	** entries in build, exec and clean configurations are
	** always processed
	**
	** entries in a per-system distributed configurations are
	** always processed
	** (these lists contain only defined distributed variables)
	**
	** in the master distributed configuration, only entries that are
	** not defined distributed variables are processed
	*/
	for (cp = lp->cf_conf; cp < lp->cf_conf + lp->cf_nconf; cp++)
		if (mode != CONF_DIST || sysid >= 0 || !is_dist_var(*cp))
			cvd4(*cp);
}

/*
**	cvd4() - extend the config_variable_dollar() processing
**		for an individual configuration variable assignment
*/

static void cvd4(s)
char *s;
{
	char *p1, *p2;

	p1 = tet_equindex(s);
	ASSERT(p1);

	/* replace each $$ with a $ */
	while (*++p1)
		if (*p1 == '$') {
			p2 = p1 + 1;
			ASSERT(*p2 == '$');
			do {
				*p2 = *(p2 + 1);
			} while (*++p2);
		}
}

/*
**	is_dist_var() - see if name is a defined distributed variable
**		which may appear in a per-system distributed configuration
**
**	return 1 if name is FOO or TET_REMnnn_FOO, where FOO is a
**		defined distributed variable
**
**	otherwise, return 0
**
**	name may be delimited either by '=' or '\0'
*/

static int is_dist_var(name)
char *name;
{
	struct dvar *dvp;
	char *p, *shortname;
	int len;

	initdvar();

	shortname = tet_remvar(name, -1); 
	ASSERT(shortname);

	if ((p = tet_equindex(shortname)) == (char *) 0)
		len = (int) strlen(shortname);
	else
		len = (int) (p - shortname);

	for (dvp = dvar; dvp < dvar + Ndvar; dvp++)
		if (len == dvp->dv_len &&
			!strncmp(shortname, dvp->dv_name, len))
				return(1);

	return(0);
}

/*
**	is_mdist_var() - see if name is a variable which may appear in
**		the master distributed configuration
**
**	return 1 if name is FOO or TET_REMnnn_FOO, where FOO is a
**		distributed variable
**
**	otherwise, return 0
**
**	name may be delimited either by '=' or '\0'
*/

static int is_mdist_var(name)
char *name;
{
	struct dvar *dvp;
	char *p, *shortname;
	int len;

	initmdvar();

	shortname = tet_remvar(name, -1); 
	ASSERT(shortname);

	if ((p = tet_equindex(shortname)) == (char *) 0)
		len = (int) strlen(shortname);
	else
		len = (int) (p - shortname);

	for (dvp = mdvar; dvp < mdvar + Nmdvar; dvp++)
		if (len == dvp->dv_len &&
			!strncmp(shortname, dvp->dv_name, len))
				return(1);

	return(is_dist_var(shortname));
}


/************************************************************************
*									*
*	configuration variable lookup functions				*
*									*
************************************************************************/

/*
**	getmcfg() - return pointer to value of config variable "name"
**		in the master configuration for the specified mode
**
**	return (char *) 0 if no entry appears for name
*/

char *getmcfg(name, mode)
char *name;
int mode;
{
	register char **cp, *p;

	mode = tcc2cfmode(mode);
	ASSERT(CONF_MODE_OK(mode, mcflist));

	if ((cp = findcfg(name, &MCFLIST(mode))) == (char **) 0)
		p = (char *) 0;
	else {
		p = tet_equindex(*cp);
		ASSERT(p);
		p++;
	}

	TRACE4(tet_Ttcc, 10, "getmcfg(\"%s\", %s) returns %s",
		name, prcfmode(mode), p ? p : "NULL");

	return(p);
}

/*
**	getcfg() - return pointer to value of config variable "name"
**		in the per-system configuration for the specified mode
**
**	return (char *) 0 if no entry appears for name
*/

char *getcfg(name, sysid, mode)
char *name;
int sysid, mode;
{
	register char **cp, *p;
	struct cftab *tp;

	ASSERT_LITE(sysid == 0);

	/* get the config list table entry for this system */
	tp = cftfind(sysid);

	/*
	** if there is no config list table entry for this system:
	**	if this is the local system, return the master config value;
	**	otherwise bail out on a programming error
	*/
	if (tp == (struct cftab *) 0 && sysid == 0)
		return(getmcfg(name, mode));
	ASSERT(tp);

	/* convert mode if necessary and check it */
	mode = tcc2cfmode(mode);
	ASSERT(CONF_MODE_OK(mode, tp->cft_list));

	/* look up the variable in the per-system configuration */
	if ((cp = findcfg(name, &CFLIST(tp, mode))) == (char **) 0)
		p = (char *) 0;
	else {
		p = tet_equindex(*cp);
		ASSERT(p);
		p++;
	}

	TRACE5(tet_Ttcc, 10, "getcfg(\"%s\", %s, %s) returns %s",
		name, tet_i2a(sysid), prcfmode(mode), p ? p : "NULL");

	return(p);
}

/*
**	getdcfg() - return pointer to value of config variable "name"
**		in the per-system distributed configuration for sysid
**
**	return (char *) 0 if no entry appears for name
**
**	this function must not be called before the per-system distributed
**	configurations have been set up
*/

char *getdcfg(name, sysid)
char *name;
int sysid;
{
	char *p, **cp;

	if ((cp = finddcfg(name, sysid)) == (char **) 0)
		p = (char *) 0;
	else {
		p = tet_equindex(*cp);
		ASSERT(p);
		p++;
	}

	TRACE4(tet_Ttcc, 10, "getdcfg(\"%s\", %s) returns %s",
		name, tet_i2a(sysid), p ? p : "NULL");
	return(p);
}

/*
**	putdcfg() - add/update a distributed configuration variable
**		for a particular system
**
**	this function  must not be called before the per-system
**	distributed configurations have been set up
*/

void putdcfg(name, sysid, value)
char *name, *value;
int sysid;
{
	char buf[MAXPATH + 40];

	ASSERT_LITE(sysid == 0);

	(void) sprintf(buf, "%s=%.*s", name,
		(int) sizeof buf - (int) strlen(name) - 2, value);
	proccfl2(buf, per_system_config(sysid, CONF_DIST));
}

/*
**	getmcflag() - get the value of a configuration variable from the
**		specified master configuration list whose value can be
**		True or False
**
**	return 1 if the variable is set and its value is True, 0 otherwise
**
**	it is assumed that all flags default to False if unset
**	(currently this is the case)
*/

int getmcflag(name, mode)
char *name;
int mode;
{
	register char *p;

	if ((p = getmcfg(name, mode)) == (char *) 0)
		return(0);
	
	return(cflag2bool(name, p));
}

/*
**	getcflag() - get the value of a configuration variable from the
**		per-system configuration for the specified mode
**		whose value can be True or False
**
**	return 1 if the variable is set and its value is True, 0 otherwise
**
**	it is assumed that all flags default to False if unset
**	(currently this is the case)
*/

int getcflag(name, sysid, mode)
char *name;
int sysid, mode;
{
	register char *p;

	ASSERT_LITE(sysid == 0);

	if ((p = getcfg(name, sysid, mode)) == (char *) 0)
		return(0);
	
	return(cflag2bool(name, p));
}

/*
**	findcfg() - find a config list entry in a configuration list
**		for the specified name and return a pointer to the
**		entry's address
**
**	name may be delimited either by '=' or '\0'
**
**	return (char **) 0 if no match can be found
*/

static char **findcfg(name, lp)
char *name;
register struct cflist *lp;
{
	register int len;
	register char **cp, *p;

	/* make len equal to the number of bytes in the name */
	if ((p = tet_equindex(name)) == (char *) 0)
		len = strlen(name);
	else
		len = p - name;

	/*
	** search the list for an entry whose initial part matches the
	** specified name
	*/
	for (cp = lp->cf_conf; cp < lp->cf_conf + lp->cf_nconf; cp++)
		if (*cp && !strncmp(*cp, name, len) && *(*cp + len) == '=')
			return(cp);

	return((char **) 0);
}

/*
**	finddcfg() - find a config variable assignment for name on sysid
**		in the distributed configuration and return a pointer
**		to the entry's address
**
**	name may be delimited either by '=' or '\0'
**
**	return (char **) 0 if no match can be found
**
**	this function must not be called before the per-system distributed
**	configurations have been set up
*/

static char **finddcfg(name, sysid)
char *name;
int sysid;
{
	struct cftab *tp;
	char **cp;

	ASSERT(sysid >= 0);
	ASSERT_LITE(sysid == 0);
	ASSERT(tet_remvar_sysid(name) == -1);

	/*
	** first look for a defined distributed variable in the per-system
	** distributed configuration
	*/
	if (
		is_dist_var(name) &&
		(tp = cftfind(sysid)) != (struct cftab *) 0
	) {
		ASSERT(IS_CFSETUP(tp, CONF_DIST));
		if ((cp = findcfg(name, &CFLIST(tp, CONF_DIST))) != (char **) 0)
			return(cp);
	}

        /*
        ** not found
	** if sysid > 0, look for the variable in
	** the master distributed configuration
	**
	** we don't look for system 0 variables in the master config -
	** this is because distributed variables for system 0 are are derived
	** from environment variables and we don't want an undadorned variable
	** in tetdist.cfg supplying a default value when the corresponding
	** environment variable has not been specified on the local system
        */
	return(sysid > 0 ? findcfg(name, &MCFLIST(CONF_DIST)) : (char **) 0);
}


/************************************************************************
*									*
*	configuration list table manipulation functions			*
*									*
************************************************************************/

/*
**	cftalloc(), cftfree() - functions to allocate and free a
**		configuration list table entry
*/

static struct cftab *cftalloc()
{
	struct cftab *tp;

	errno = 0;
	if ((tp = (struct cftab *) malloc(sizeof *tp)) == (struct cftab *) 0)
		fatal(errno, "can't allocate configuration list table entry",
			(char *) 0);

	TRACE2(tet_Tbuf, 6,
		"cftalloc(): allocate configuration list table entry = %s",
		tet_i2x(tp));

	bzero((char *) tp, sizeof *tp);
	tp->cft_magic = CFT_MAGIC;
	tp->cft_sysid = -1;

	return(tp);
}

#ifndef TET_LITE	/* -START-LITE-CUT- */

static void cftfree(tp)
struct cftab *tp;
{
	struct cflist *lp;
	char **cp;

	TRACE2(tet_Tbuf, 6,
		"cftfree(): free configuration list table entry = %s",
		tet_i2x(tp));

	if (!tp)
		return;

	ASSERT(tp->cft_magic == CFT_MAGIC);
	for (lp = tp->cft_list; lp < &tp->cft_list[Ncftlist(tp)]; lp++) {
		if (!lp->cf_conf)
			continue;
		for (cp = lp->cf_conf; cp < lp->cf_conf + lp->cf_nconf; cp++) {
			if (!*cp)
				continue;
			TRACE2(tet_Tbuf, 6, "free config variable = %s",
				tet_i2x(*cp));
			free((void *) *cp);
		}
		TRACE4(tet_Tbuf, 6,
			"system %s: free %s config variable list = %s",
			tet_i2a(tp->cft_sysid),
			prcfmode((int) (lp - tp->cft_list) + 1),
			tet_i2x(lp->cf_conf));
		free((void *) lp->cf_conf);
	}

	bzero((char *) tp, sizeof *tp);
	free((void *) tp);
}

#endif /* !TET_LITE */	/* -END-LITE-CUT- */


/*
**	cftadd() - add an entry to the configuration list table
*/

static void cftadd(tp)
struct cftab *tp;
{
	tet_listinsert((struct llist **) &cftab, (struct llist *) tp);
}


#ifndef TET_LITE	/* -START-LITE-CUT- */

/*
**	cftrm() - remove an entry from the configuration list table
*/

static void cftrm(tp)
struct cftab *tp;
{
	tet_listremove((struct llist **) &cftab, (struct llist *) tp);
}

#endif /* !TET_LITE */	/* -END-LITE-CUT- */


/*
**	cftfind() - find the configuration list table entry for the
**		specified system and return a pointer thereto
**
**	return (struct cftab *) 0 if there is no entry for sysid
*/

static struct cftab *cftfind(sysid)
int sysid;
{
	struct cftab *tp;

	for (tp = cftab; tp; tp = tp->cft_next) {
		ASSERT(tp->cft_magic == CFT_MAGIC);
		if (tp->cft_sysid == sysid)
			break;
	}

	return(tp);
}

/*
**	cftmax() - return the highest sysid in the configuration list table
*/

static int cftmax()
{
	struct cftab *tp;
	int max = -1;

	for (tp = cftab; tp; tp = tp->cft_next) {
		ASSERT(tp->cft_magic == CFT_MAGIC);
		if (tp->cft_sysid > max)
			max = tp->cft_sysid;
	}

	return(max);
}

/*
**	cftcheck() - see if a configuration list table entry exists
**		for this sysid and add one if it doesn't
**
**	either way, return a pointer to the entry
*/

static struct cftab *cftcheck(sysid)
int sysid;
{
	struct cftab *tp;

	if ((tp = cftfind(sysid)) == (struct cftab *) 0) {
		TRACE2(tet_Ttcc, 6, "cftcheck(): about to allocate configuration list table entry for system %s",
			tet_i2a(sysid));
		tp = cftalloc();
		tp->cft_sysid = sysid;
		cftadd(tp);
	}

	return(tp);
}

/************************************************************************
*									*
*	miscellaneous functions						*
*									*
************************************************************************/

/*
**	tcc2cfmode() - convert a mode of operation to a config mode
**
**	returns mode if mode is not a valid tcc mode of operation
*/

int tcc2cfmode(mode)
int mode;
{
	switch (mode) {
	case TCC_BUILD:
		return(CONF_BUILD);
	case TCC_EXEC:
		return(CONF_EXEC);
	case TCC_CLEAN:
		return(CONF_CLEAN);
	case CONF_BUILD:
	case CONF_EXEC:
	case CONF_CLEAN:
	case CONF_DIST:
		return(mode);
	default:
		/* this "can't happen" */
		fatal(0, "unexpected mode", tet_i2o(mode));
		/* NOTREACHED */
		return(mode);
	}
}

/*
**	prcfmode() - return printable representation of a configuration
**		mode
*/

char *prcfmode(mode)
int mode;
{
	static char text[] = "conf-mode ";
	static char msg[sizeof text + LNUMSZ];

	switch (mode) {
	case CONF_BUILD:
		return("BUILD");
	case CONF_EXEC:
		return("EXEC");
	case CONF_CLEAN:
		return("CLEAN");
	case CONF_DIST:
		return("DIST");
	default:
		(void) sprintf(msg, "%s%d", text, mode);
		return(msg);
	}
}

/*
**	cflag2bool() - return the boolean value of a string which should
**		either be "True" or "False"
*/

static int cflag2bool(name, val)
char *name, *val;
{
	switch (*val) {
	case 'T':
	case 't':
		return(1);
	case 'F':
	case 'f':
		break;
	default:
		error(0, name, "variable has ambiguous value - False assumed");
		break;
	}

	return(0);
}

#ifdef TET_LITE	/* -LITE-CUT-LINE- */

/*
**	tet_config_putenv() - set up the value of the TET_CONFIG environment
**		variable which corresponds to the current mode of operation
**
**	return 0 if successful or -1 on error
*/

int tet_config_putenv(opmode)
int opmode;
{
	static int currmode = -1;
	static char *var;
	static int lvar;
	static char envname[] = "TET_CONFIG";
	int cfmode;

	cfmode = tcc2cfmode(opmode);
	ASSERT(CONF_MODE_OK(cfmode, cfname));

	/* return now if the current value has already been set */
	if (cfmode == currmode)
		return(0);

	/*
	** here to change the setting of TET_CONFIG in the environment
	**
	** ensure that the environment string is big enough,
	** then format the variable
	*/
	ASSERT(TCFNAME(cfmode));
	if (BUFCHK(&var, &lvar, (int) (sizeof envname + strlen(TCFNAME(cfmode)) + 1)) < 0)
		return(-1);
	(void) sprintf(var, "%s=%s", envname, TCFNAME(cfmode));

	/* put the variable in the environment */
	if (tet_putenv(var) < 0)
		return(-1);

	/* all ok so remember the current mode and return success */
	currmode = cfmode;
	return(0);
}

#endif /* TET_LITE */	/* -LITE-CUT-LINE- */



/*
**	confgiveup() - exit after finding configuration errors
*/

static void confgiveup()
{
	(void) fprintf(stderr,
		"%s: giving up after %d configuration error%s\n",
		tet_progname, conferrors, conferrors == 1 ? "" : "s");
	tcc_exit(1);
}

/*
**	config_cleanup() - remove temporary config files before exit
*/

void config_cleanup()
{

#ifdef TET_LITE		/* -LITE-CUT-LINE- */

	char **fname;

	for (fname = tcfname; fname < &tcfname[Ntcfname]; fname++)
		if (*fname)
			(void) UNLINK(*fname);

#else 	/* -START-LITE-CUT- */

	if (ecfname)
		(void) UNLINK(ecfname);
	if (dcfname)
		(void) UNLINK(dcfname);
	if (ccfname)
		(void) UNLINK(ccfname);

#endif /* TET_LITE */	/* -END-LITE-CUT- */

}

/*
**	get_runtime_tsroot() - return the name of the test suite root
**		directory to be used at runtime on a particular system
**
**	this function should not be called before distcfg() has set
**	up the distributed configurations for each system
**
**	the value returned by this function may not be useful before any
**	required runtime copy operations have been performed
**	(see the calls to rtlcopy() and rtrcopy() in tcc.c)
*/

char *get_runtime_tsroot(sysid)
int sysid;
{
	char *retval, *rtdir, *tsroot;
	static char rtsroot[MAXPATH];

	/*
	** if there is no TET_RUN, just return the system's TET_TSROOT;
	** otherwise we must construct the name of the tsroot directory
	** below TET_RUN and return that
	*/
	tsroot = getdcfg("TET_TSROOT", sysid);
	ASSERT(tsroot);
	if ((rtdir = getdcfg("TET_RUN", sysid)) == (char *) 0)
		retval = tsroot;
	else {
		fullpath(rtdir, tet_basename(tsroot), rtsroot, sizeof rtsroot,
			sysid == 0 ? 0 : 1);
		retval = rtsroot;
	}

	TRACE3(tet_Ttcc, 10, "get_runtime_tsroot(%s) returns %s",
		tet_i2a(sysid), retval);
	return(retval);
}

/*
**	initdvar() - initialise elements of the dvar array
*/

static void initdvar()
{
	register struct dvar *dvp = dvar;
	static char *tet_root_p = tet_root;
	static int been_here;

	if (been_here)
		return;
	else
		been_here = 1;

	/*
	** set up dvar first time through -
	** the number and order of these assignments must
	** correspond to the variables in the dvar array
	*/
	(dvp++)->dv_vp = &tet_root_p;
	(dvp++)->dv_vp = &tet_suite_root;
	(dvp++)->dv_vp = &tet_tsroot;
	(dvp++)->dv_vp = &tet_execute;
	(dvp++)->dv_vp = &tet_tmp_dir;
	(dvp++)->dv_vp = &tet_run;
	for (dvp = dvar; dvp < dvar + Ndvar; dvp++)
		dvp->dv_len = strlen(dvp->dv_name);
}

/*
**	initmdvar() - initialise elements of the mdvar array
*/

static void initmdvar()
{
	register struct dvar *dvp = mdvar;
	static int been_here;

	if (been_here)
		return;
	else
		been_here = 1;

	/* set up mdvar first time through - */
	for (dvp = mdvar; dvp < mdvar + Nmdvar; dvp++)
		dvp->dv_len = strlen(dvp->dv_name);
}

