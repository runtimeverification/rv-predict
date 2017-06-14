/*
 *	SCCS: @(#)tool.c	1.14 (05/12/08)
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
static char sccsid[] = "@(#)tool.c	1.14 (05/12/08) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tool.c	1.14 05/12/08 TETware release 3.8
NAME:		tool.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	functions to execute and wait for testcases and tools

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., June 1997
	Attempt to copy the output capture file to the journal after a
	failed exec.

	Andrew Dingwall, UniSoft Ltd., December 1997
	When sending the system ID list to tccd before executing a
	remote (non-distributed) test case, only send a list containing
	the single system ID to each system instead of a list
	containing all the system IDs.

	Andrew Dingwall, UniSoft Ltd., May 1998
	Use tet_basename() instead of tcc_basename().
 
	Neil Moses, The Open Group, November 2005
	Systems that are allowed to reconnect are handled in toolw2()
	when a disconnect error is detected. If the proctab entry has
	the flag PRF_RECONNECT set, then tsreconnect() is called.

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <ctype.h>
#include <errno.h>
#include <signal.h>
#include <sys/types.h>
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtmsg.h"
#include "error.h"
#include "ltoa.h"
#include "servlib.h"
#include "dtetlib.h"
#include "config.h"
#include "systab.h"
#include "scentab.h"
#include "proctab.h"
#include "tet_jrnl.h"
#include "tcc.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* static function declarations */
static void ocf2jnl2 PROTOLIST((struct proctab *, char *));
static void ocfile2jnl PROTOLIST((struct proctab *, char *));
static void tcdirfname PROTOLIST((char *, char *, char *, int));
static void tooladdargv PROTOLIST((char ***, int *, int *, char *, int));
static int toolrun2 PROTOLIST((struct proctab *));
static int toolrunning PROTOLIST((struct proctab *));
static int toolw2 PROTOLIST((struct proctab *));
static void xresfilename PROTOLIST((char *, char *, int));
#ifndef TET_LITE	/* -START-LITE-CUT- */
static int tcreconnect PROTOLIST((struct proctab *prp));
#endif /* !TET_LITE */	/* -END-LITE-CUT- */


/*
**	toolprep() - prepare to execute a tool or testcase on a particular
**		system
**
**	return a pointer to the argv to use if successful or (char **) 0 if
**	not in EXEC mode and the tool is not defined
**
**	the argv array is contained in memory obtained from malloc
**	which should be freed when no longer required by a call to
**	toolpfree()
*/

char **toolprep(prp, tcname, tcnamelen)
register struct proctab *prp;
char *tcname;
int tcnamelen;
{
	register char *p;
	char *toolvar, *filevar, *edir;
	int pass_tcname, pass_iclist;
	char **argv = (char **) 0;
	int argvlen = 0, argc = 0;

	/* determine the names of the tool and file variables */
	switch (prp->pr_tcstate) {
	case TCS_BUILD:
		toolvar = "TET_BUILD_TOOL";
		filevar = "TET_BUILD_FILE";
		break;
	case TCS_EXEC:
		toolvar = "TET_EXEC_TOOL";
		filevar = "TET_EXEC_FILE";
		break;
	case TCS_CLEAN:
		toolvar = "TET_CLEAN_TOOL";
		filevar = "TET_CLEAN_FILE";
		break;
	case TCS_PREBUILD:
		toolvar = "TET_PREBUILD_TOOL";
		filevar = "TET_PREBUILD_FILE";
		break;
	case TCS_BUILDFAIL:
		toolvar = "TET_BUILD_FAIL_TOOL";
		filevar = "TET_BUILD_FAIL_FILE";
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected TC state", prtcstate(prp->pr_tcstate));
		/* NOTREACHED */
		return((char **) 0);
	}

	/*
	** tool variables
	**
	** in BUILD and CLEAN modes a tool must be defined
	** in EXEC mode the tool is optional; if it is not defined the
	** test case is executed directly
	**
	** the PREBUILD and BUILD_FAIL tools are optional; these tools only
	** get executed if they are defined
	*/

	/*
	** get the value of the tool variable:
	** in EXEC mode it doesn't matter if there isn't one;
	** in other modes, return an error if the tool is not defined
	**
	** if no tool is defined, an error message is printed unless we are
	** looking for the optional PREBUILD or BUILD_FAIL tools
	**
	** store the tool variable in the argv
	** then get and store an optional file argument
	*/
	p = getcfg(toolvar, *prp->pr_sys, prp->pr_currmode);
	if (p && *p) {
		tooladdargv(&argv, &argvlen, &argc, p, 1);
		p = getcfg(filevar, *prp->pr_sys, prp->pr_currmode);
		if (p && *p)
			tooladdargv(&argv, &argvlen, &argc, p, 1);
	}
	else {
		TRACE4(tet_Ttcc, 4,
			"%s not defined in %s configuration for system %s",
			toolvar, prtccmode(prp->pr_currmode),
			tet_i2a(*prp->pr_sys));
		if (prp->pr_currmode != TCC_EXEC) {
			switch (prp->pr_tcstate) {
			case TCS_PREBUILD:
			case TCS_BUILDFAIL:
				break;
			default:
				prperror(prp, *prp->pr_sys, 0, toolvar,
					"not defined");
				prp->pr_jnlstatus = TET_ESTAT_ERROR;
				break;
			}
			return((char **) 0);
		}
	}

	/*
	** see if we want to pass the test case name and the optional
	** iclist to the tool
	**
	** the test case name is always passed to a PREBUILD tool
	** a BUILD_FAIL tool and an EXEC tool
	**
	** if present, the iclist is only passed to an EXEC tool
	*/
	switch (prp->pr_tcstate) {
	case TCS_PREBUILD:
	case TCS_BUILDFAIL:
		pass_tcname = 1;
		pass_iclist = 0;
		break;
	case TCS_EXEC:
		pass_tcname = 1;
		pass_iclist = prp->pr_exiclist ? 1 : 0;
		break;
	default:
		pass_tcname = getcflag("TET_PASS_TC_NAME", *prp->pr_sys,
			prp->pr_currmode);
		pass_iclist = 0;
		break;
	}

	/*
	** work out the name of the test case to add to the argv
	**
	** in EXEC mode the test case name is always added:
	**	if TET_EXEC_IN_PLACE is true, the test case is located below
	**	the alternate execution directory if one has been defined,
	**	otherwise it is below the test suite root;
	**	if TET_EXEC_IN_PLACE is false, the test case has been
	**	copied to the temporary execution directory and must be
	**	executed from there
	**
	** in other modes the test case name is only added to a PREBUILD
	** or BUILD_FAIL tool, or if TET_PASS_TC_NAME is true
	*/
	if (prp->pr_currmode == TCC_EXEC) {
		if (getmcflag("TET_EXEC_IN_PLACE", TCC_EXEC)) {
			if ((edir = getdcfg("TET_EXECUTE", *prp->pr_sys)) == (char *) 0)
				tcsrcname(prp, tcname, tcnamelen);
			else
				tcexecname(prp, edir, tcname, tcnamelen);
		}
		else
			tcexecname(prp, prp->pr_tmpdir, tcname, tcnamelen);
	}
	else
		tcsrcname(prp, tcname, tcnamelen);

	/*
	** if we have a test case name, add it to the argv together with
	** the iclist if there is one;
	** if this is the first entry in the argv then we must be preparing
	** to execute a test case directly so add in the full path name;
	** otherwise we are preparing to execute a tool so just add
	** the last component of the test case path name - tcc_texec()
	** does a chdir to dirname(tcname) before exec'ing the testcase
	** or tool
	*/
	if (pass_tcname) {
		tooladdargv(&argv, &argvlen, &argc,
			argc > 0 ? tet_basename(tcname) : tcname, 0);
		if (pass_iclist)
			tooladdargv(&argv, &argvlen, &argc, prp->pr_exiclist, 0);
	}

	/* initialise the proctab */
	prp->pr_toolstate = PTS_IDLE;
	prp->pr_exitcode = 0;
	if (prp->pr_tcstate != TCS_BUILDFAIL)
		prp->pr_jnlstatus = 0;

	return(argv);
}

/*
**	tooladdargv() - split a string into fields and add each field
**		into an existing argv array, growing it if necessary
*/

static void tooladdargv(avp, alp, anp, s, splitflds)
register char ***avp;
int *alp, splitflds;
register int *anp;
char *s;
{
	register char *p, **ap;
	char buf[MAXPATH * 2];
	int nflds;

	/* count the fields in the string */
	nflds = 1;
	if (splitflds)
		for (p = s; *p; p++)
			if (isspace(*p))
				nflds++;

	/* grow the argv so as to be big enough to contain the new fields */
	RBUFCHK((char **) avp, alp, (int) ((*anp + nflds + 1) * sizeof *avp));

	/*
	** if there is more than one field, split the string into fields
	** if so required
	*/
	ap = *avp + *anp;
	if (splitflds && nflds > 1) {
		(void) sprintf(buf, "%.*s", (int) sizeof buf - 1, s);
		nflds = tet_getargs(buf, ap, nflds);
	}
	else
		*ap = s;

	/* now, store each field and replace its ptr in the argv */
	for (ap = *avp + *anp; ap < *avp + *anp + nflds; ap++)
		*ap = rstrstore(*ap);

	/* update the argc, null-terminate the argv and return */
	*anp += nflds;
	*ap = (char *) 0;
}

/*
**	toolpfree() - free up memory allocated by toolprep()
*/

void toolpfree(argv)
char **argv;
{
	register char **ap;

	for (ap = argv; *ap; ap++) {
		TRACE2(tet_Tbuf, 6, "toolpfree(): free *argv = %s",
			tet_i2x(*ap));
		free(*ap);
	}

	TRACE2(tet_Tbuf, 6, "toolpfree(): free argv = %s", tet_i2x(argv));
	free((char *) argv);
}

/*
**	toolexec() - execute a tool with a tcname and an argv that has
**		been set up by toolprep()
**
**	return 0 if successful or -1 on error
*/

int toolexec(prp, tcname, argv, ocfname)
struct proctab *prp;
char *tcname, **argv, *ocfname;
{
	char buf[MAXPATH];
#ifndef TET_LITE	/* -START-LITE-CUT- */
	int *sys, nsys;
	struct systab *sp;
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	/*
	** if this is a build, exec or clean tool, remove a tet_xres
	** file from the directory which contains the test case
	** and remember its name for use in the JOURNAL stage
	*/
	switch (prp->pr_tcstate) {
	case TCS_BUILD:
	case TCS_EXEC:
	case TCS_CLEAN:
		xresfilename(tcname, buf, sizeof buf);
		(void) tcc_unlink(*prp->pr_sys, buf);
		prp->pr_tetxres = rstrstore(buf);
		break;
	}

#ifndef TET_LITE	/* -START-LITE-CUT- */
	/* send the system name list to tccd if necessary */
	if (prp->pr_distflag && (prp->pr_flags & PRF_TC_CHILD)) {
		sys = prp->pr_parent->pr_sys;
		nsys = prp->pr_parent->pr_nsys;
	}
	else {
		sys = prp->pr_sys;
		nsys = prp->pr_nsys;
	}
	sp = syfind(*prp->pr_sys);
	ASSERT(sp);
	if (sp->sy_sys != sys || sp->sy_nsys != nsys) {
		if (tet_tcsysname(sp->sy_sysid, sys, nsys) < 0) {
			prperror(prp, sp->sy_sysid, tet_tcerrno,
				"can't send system name list to TCCD",
				(char *) 0);
			return(-1);
		}
		sp->sy_sys = sys;
		sp->sy_nsys = nsys;
	}
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	/* unlink an existing output capture file if one is specified */
	if (ocfname)
		(void) tcc_unlink(*prp->pr_sys, ocfname);

	/*
	** do the exec:
	** if the operation fails, it may be that an error message has been
	** written in the output capture file on an NT system; so, if the
	** output capture file has been created, copy it to the journal
	*/
	tcc_dirname(tcname, buf, sizeof buf);
	if ((prp->pr_remid = tcc_texec(prp, argv[0], argv, buf, ocfname)) < 0) {
		if (ocfname && tcc_access(*prp->pr_sys, ocfname, 04) == 0)
			ocfile2jnl(prp, ocfname);
		return(-1);
	}

	/* enter the exec details in the proctab */
	prp->pr_starttime = time((time_t *) 0);
	prp->pr_waitinterval = WAITINTERVAL_START;
	prp->pr_nextattn = prp->pr_starttime + prp->pr_waitinterval;
#ifndef TET_LITE	/* -START-LITE-CUT- */
	if (prp->pr_flags & PRF_TC_CHILD) {
		ASSERT(prp->pr_parent && prp->pr_parent->pr_magic == PR_MAGIC);
		prp->pr_parent->pr_starttime = prp->pr_starttime;
		prp->pr_parent->pr_nextattn = prp->pr_nextattn;
	}
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
	prp->pr_toolstate = PTS_RUNNING;

	/* all OK so return success */
	return(0);
}

/*
**	toolwait() - wait for a tool to finish executing
**
**	return 0 if the tool exited with a zero exit code or is still
**	running, or -1 if the tool exited with non-zero exit code
*/

int toolwait(prp)
register struct proctab *prp;
{
	int rc;

	TRACE5(tet_Ttcc, 6,
		"toolwait(%s): currmode = %s, tcstate = %s, toolstate = %s",
		tet_i2x(prp), prtccmode(prp->pr_currmode),
		prtcstate(prp->pr_tcstate), prtoolstate(prp->pr_toolstate));

	switch (prp->pr_toolstate) {
	case PTS_RUNNING:
	case PTS_ABORT:
	case PTS_SIGTERM:
	case PTS_SIGKILL:
		rc = toolw2(prp);
		break;
	case PTS_EXITED:
		rc = prp->pr_exitcode ? -1 : 0;
		break;
	case PTS_IDLE:
		rc = 0;
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected tool state",
			prtoolstate(prp->pr_toolstate));
		/* NOTREACHED */
		return(-1);
	}

	TRACE6(tet_Ttcc, 6, "toolwait(%s) RETURN %s: currmode = %s, tcstate = %s, toolstate = %s",
		tet_i2x(prp), tet_i2a(rc), prtccmode(prp->pr_currmode),
		prtcstate(prp->pr_tcstate), prtoolstate(prp->pr_toolstate));

	return(rc);
}

/*
**	toolw2() - extend the toolwait() processing for a tool which
**		is still running
**
**	return 0 if the tool exited with a zero exit code or is still
**	running, or -1 if the tool exited with non-zero exit code
*/

static int toolw2(prp)
register struct proctab *prp;
{
	int status;

	ASSERT(prp->pr_nsys == 1);

	errno = 0;
	if (tcc_waitnohang(*prp->pr_sys, prp->pr_remid, &status) < 0) {
		if (prp->pr_waitcount > 0)
			prp->pr_waitcount--;
		switch (tet_tcerrno) {
		case ER_WAIT:
			if (toolrunning(prp) == 0)
				return(0);
			break;
		default:
#ifndef TET_LITE	/* -START-LITE-CUT- */
			if (prp->pr_flags & PRF_RECONNECT)
				if (tcreconnect(prp) == 0)
					break;
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
			prperror(prp, *prp->pr_sys, errno ? errno : tet_tcerrno,
				"tcc_waitnohang() failed for pid",
				tet_l2a(prp->pr_remid));
			break;
		}
		status = (~0 << 8) & 017777;
	}

	/* here when the tool has finished - gather the captured output */
	if (prp->pr_outfile) {
		ocfile2jnl(prp, prp->pr_outfile);
		TRACE2(tet_Tbuf, 6, "free pr_outfile = %s",
			tet_i2x(prp->pr_outfile));
		free(prp->pr_outfile);
		prp->pr_outfile = (char *) 0;
	}

	/*
	** make an attempt to decode the exit status like the shell does
	**
	** note that tcc_waitnohang() returns a status using the traditional
	** encodings because the status might have come from another system
	** with different WIF* macros
	*/
	prp->pr_exitcode = (((unsigned) status >> 8) & 0377) | ((status & 0377) << 8);

	/*
	** copy a non-zero exit code to the journal status unless we already
	** have a status (e.g., TET_ESTAT_TIMEOUT)
	*/
	if (!prp->pr_jnlstatus && prp->pr_exitcode)
		prp->pr_jnlstatus = prp->pr_exitcode;

	/* mark the testcase as exited */
	prp->pr_toolstate = PTS_EXITED;
	return(status ? -1 : 0);
}

/*
**	toolrunning() - extend the toolwait() processig for a tool that
**
**	return 0 if this is OK or -1 if an error has occurred
*/

static int toolrunning(prp)
register struct proctab *prp;
{
	register int rc;

	rc = toolrun2(prp);

#ifndef TET_LITE	/* -START-LITE-CUT- */
	if (rc == 0 && (prp->pr_flags & PRF_TC_CHILD)) {
		ASSERT(prp->pr_parent && prp->pr_parent->pr_magic == PR_MAGIC);
		if (prp->pr_nextattn > prp->pr_parent->pr_nextattn)
			prp->pr_parent->pr_nextattn = prp->pr_nextattn;
	}
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	return(rc);
}

/*
**	toolrun2() - extend the toolrunning() processing
**
**	return 0 if this is OK or -1 if an error has occurred
*/

static int toolrun2(prp)
register struct proctab *prp;
{
	time_t now = time((time_t *) 0);
	time_t next;
	int n;
	int signum = 0;

	/*
	** decide what to do next, depending on the tool's state and whether
	** a timeout has been specified
	*/
	switch (prp->pr_toolstate) {
	case PTS_RUNNING:
		if (tcc_timeout <= 0 || now <= prp->pr_starttime + tcc_timeout) {
			next = now + prp->pr_waitinterval;
			if (tcc_timeout > 0 && next > prp->pr_starttime + tcc_timeout)
				next = TET_MAX(prp->pr_starttime + tcc_timeout, now + 1);
			prp->pr_nextattn = next;
			n = ((int) (now - prp->pr_starttime) / 10) + 1;
			prp->pr_waitinterval = TET_MIN(n, WAITINTERVAL_MAX);
			return(0);
		}
		TRACE2(tet_Ttcc, 6, "toolrun2(%s): tool timed out",
			tet_i2x(prp));
		prp->pr_jnlstatus = TET_ESTAT_TIMEOUT;
		/* else fall through */
	case PTS_ABORT:
#ifdef _WIN32		/* -START-WIN32-CUT- */
		/* fall through */
#else			/* -END-WIN32-CUT- */
		prp->pr_toolstate = PTS_SIGTERM;
		prp->pr_waitcount = 5;
		signum = SIGTERM;
		break;
#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */
	case PTS_SIGTERM:
		if (prp->pr_waitcount > 0) {
			prp->pr_nextattn = now + 1;
			return(0);
		}
		prp->pr_toolstate = PTS_SIGKILL;
		prp->pr_waitcount = 2;
		signum = SIGKILL;
		break;
	case PTS_SIGKILL:
		if (prp->pr_waitcount > 0) {
			prp->pr_nextattn = now + 1;
			return(0);
		}
		prperror(prp, *prp->pr_sys, 0,
			"tool or testcase hung after SIGKILL: pid",
			tet_l2a(prp->pr_remid));
		return(-1);
	default:
		/* this "can't happen" */
		fatal(0, "unexpected tool state",
			prtoolstate(prp->pr_toolstate));
		/* NOTREACHED */
	}

	/* here to send a signal to the testcase or tool */
	ASSERT(signum);
	if (tcc_kill(*prp->pr_sys, prp->pr_remid, signum) < 0) {
		prperror(prp, *prp->pr_sys, errno ? errno : tet_tcerrno,
			"tcc_kill() failed for pid",
				tet_l2a(prp->pr_remid));
		return(-1);
	}

	prp->pr_nextattn = time((time_t *) 0) + 1;
	return(0);
}

/*
**	ocfilename() - determine the name of the output capture file
**
**	tcpath refers to a test case path name that has been generated
**	by a previous call to toolprep()
**
**	the name of the output capture file is "tet_captured" which
**	is located in the directory where the test case is to be processed
*/

void ocfilename(tcpath, ocfname, ocfnamelen)
char *tcpath, *ocfname;
int ocfnamelen;
{
	tcdirfname(tcpath, "tet_captured", ocfname, ocfnamelen);
}

/*
**	xresfilename() - determine the name of the non-C API tet_xres file
**
**	tcpath refers to a test case path name that has been generated
**	by a previous call to toolprep()
*/

static void xresfilename(tcpath, xrfname, xrfnamelen)
char *tcpath, *xrfname;
int xrfnamelen;
{
	tcdirfname(tcpath, "tet_xres", xrfname, xrfnamelen);
}

/*
**	tcdirfname() - determine the full path name of the specified file
**		in the directory which contains the named test case
*/

static void tcdirfname(tcpath, fname, path, pathlen)
char *tcpath, *fname, *path;
int pathlen;
{
	char tcdir[MAXPATH];

	tcc_dirname(tcpath, tcdir, sizeof tcdir);
	fullpath(tcdir, fname, path, pathlen, 1);
}

/*
**	ocfile2jnl() - enter the contents of an output capture file in
**		the journal
*/

static void ocfile2jnl(prp, ocfname)
struct proctab *prp;
char *ocfname;
{
#ifndef TET_LITE	/* -START-LITE-CUT- */
	char *tfname;
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	TRACE4(tet_Ttcc, 6, "ocfile2jnl(%s, %s): sysid = %s",
		tet_i2x(prp), ocfname, tet_i2a(*prp->pr_sys));

	/*
	** if this is not the local system, transfer the file to the
	** local system and process it there;
	** otherwise, just process the file straight off
	*/
#ifndef TET_LITE	/* -START-LITE-CUT- */
	if (*prp->pr_sys > 0) {
		if ((tfname = jnl_tfname(resdirname(), "oc")) == (char *) 0) {
			prperror(prp, *prp->pr_sys, 0, "can't generate file name to receive captured output in",
				ocfname);
			return;
		}
		if (getremfile(prp, ocfname, tet_basename(tfname)) == 0)
			ocf2jnl2(prp, tfname);
		(void) UNLINK(tfname);
	}
	else
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
		ocf2jnl2(prp, ocfname);
}

/*
**	ocf2jnl2() - extend the ocfile2jnl() processing for an output
**		capture file on the local system
*/

static void ocf2jnl2(prp, ocfname)
struct proctab *prp;
char *ocfname;
{
	char buf[LBUFLEN];
	FILE *fp;
	register char *p;

	TRACE4(tet_Ttcc, 6, "ocf2jnl2(%s): transfer captured output from %s to journal file %s",
		tet_i2x(prp), ocfname, prp->pr_jfname);

	if ((fp = fopen(ocfname, "r")) == (FILE *) 0) {
		prperror(prp, 0, errno, "can't open output capture file",
			ocfname);
		return;
	}

	while (fgets(buf, sizeof buf, fp) != (char *) 0) {
		for (p = buf; *p; p++)
			if (*p == '\r' || *p == '\n') {
				*p = '\0';
				break;
			}
		jnl_captured(prp, buf);
	}

	(void) fclose(fp);
}


#ifndef TET_LITE	/* -START-LITE-CUT- */

/*
**	getremfile() - retrieve a file from a remote system and put it
**		below the saved files directory on the local system
**
**	return 0 if successful or -1 on error
*/

int getremfile(prp, fromfile, tofile)
struct proctab *prp;
char *fromfile, *tofile;
{
	static char fmt[] = "can't transfer file to %.*s from";
	char msg[sizeof fmt + MAXPATH];

	TRACE6(tet_Ttcc, 6,
		"getremfile(%s): copy %s on system %s to %s/%s on local system",
		tet_i2x(prp), fromfile, tet_i2a(*prp->pr_sys), resdirname(),
		tofile);

	/*
	** get TCCD/XRESD to do the work for us -
	** tet_tcrxfile() interprets tofile relative to the saved files
	** directory on the local system
	*/
	if (tet_tcrxfile(*prp->pr_sys, fromfile, tofile) < 0) {
		(void) sprintf(msg, fmt, sizeof msg - sizeof fmt, tofile);
		prperror(prp, *prp->pr_sys, tet_tcerrno, msg, fromfile);
		return(-1);
	}

	return(0);
}

/*
**	tcreconnect() - Reconnect to a remote system.
**	
**	return 0 if successful or -1 on error
*/

static int tcreconnect(prp)
struct proctab *prp;
{
	int cfmode;
	time_t start_time;
	int timeout;
	char *cfg = "TET_RECONNECT_TIMEOUT";
	char *var;
	struct systab *sp;
	struct cflist *lp;

	TRACE3(tet_Ttcc, 6, "tcreconnect(%s): reconnect to system %d",
		tet_i2x(prp), tet_i2a(*prp->pr_sys));

	/* Make sure system is seen as logged off */
	(void) tet_tclogoff(*prp->pr_sys);

	/* Check if the user has set a timeout for the reconnect */
	var = getcfg(cfg, *prp->pr_sys, prp->pr_currmode);
        if (var == 0 || *var == '\0')
		var = TET_DEFAULT_RECONNECT_TIMEOUT; /* Default timeout */

	start_time = time((time_t *)0);
	timeout = atoi(var);

	/* Attempt to logon to the remote system within the timeout period */
	for (;;)
	{
		if (tet_tclogon(*prp->pr_sys) == 0)
			break;

		if (time((time_t *)0) > start_time + timeout)
		{
			prperror(prp, *prp->pr_sys, 0,
				"Timed out waiting to reconnect", (char *)0);
			return(-1);
		}

		SLEEP(10);
	}

	if ((sp = syfind(*prp->pr_sys)) 
			== (struct systab *) 0)
	{
		prperror(prp, *prp->pr_sys, tet_tcerrno,
			"Failed to find systab entry", (char *)0);
		return (-1);
	}

	/* Send the environment on the remote system */
	init1environ(sp);

	/* Send the list of participating systems */
	if (tet_tcsysname(*prp->pr_sys, sp->sy_sys, 
		sp->sy_nsys) < 0) 
	{
		prperror(prp, *prp->pr_sys, tet_tcerrno,
			"Failed to send system name list", (char *)0);
		return(-1);
	}

	/* Send the config variables to the remote system */
	cfmode = tcc2cfmode(TCC_EXEC);
	lp = per_system_config(*prp->pr_sys, cfmode);
	if (tet_tcconfigv(*prp->pr_sys, lp->cf_conf, lp->cf_nconf, TC_CONF_EXEC) < 0) {
		prperror(prp, *prp->pr_sys, tet_tcerrno,
			"can't assign config lines to TCCD for mode",
			tet_i2a(cfmode));
		return(-1);
	}	

	if (tet_tcsetconf(*prp->pr_sys, TC_CONF_EXEC) < 0) {
		prperror(prp, *prp->pr_sys, tet_tcerrno,
			"can't set TCCD config mode", (char *) 0);
		return(-1);
	}

	return(0);
}

#endif /* !TET_LITE */	/* -END-LITE-CUT- */

