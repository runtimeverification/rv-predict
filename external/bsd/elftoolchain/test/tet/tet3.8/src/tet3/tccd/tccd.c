/*
 *      SCCS:  @(#)tccd.c	1.23 (05/06/30) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1992 X/Open Company Limited
 *
 * All rights reserved.  No part of this source code may be reproduced,
 * stored in a retrieval system, or transmitted, in any form or by any
 * means, electronic, mechanical, photocopying, recording or otherwise,
 * except as stated in the end-user licence agreement, without the prior
 * permission of the copyright owners.
 *
 * X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
 * the UK and other countries.
 */

#ifndef lint
static char sccsid[] = "@(#)tccd.c	1.23 (05/06/30) TETware release 3.8";
static char *copyright[] = {
	"(C) Copyright 1996 X/Open Company Limited",
	"All rights reserved"
};
#endif

/************************************************************************

SCCS:   	@(#)tccd.c	1.23 05/06/30 TETware release 3.8
NAME:		tccd.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	server-specific functions for tccd server

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, August 1993
	added support for OP_RCOPY request message

	Denis McConalogue, UniSoft Limited, September 1993
	added ss_disconnect() function

	Andrew Dingwall, UniSoft Ltd., December 1993
	removed disconnect stuff
	moved tet_sdptab and tet_xdptab to here

	Andrew Dingwall, UniSoft Ltd., February 1994
	added -e command-line option

	Andrew Dingwall, UniSoft Ltd., February 1995
	provide default PATH if one is not present in the environment
	(mainly for the INETD version)

	Andrew Dingwall, UniSoft Ltd., August 1996
	added OP_SETCONF, OP_MKALLDIRS and OP_TIME
	changed for tetware-style configuration

	Geoff Clare, UniSoft Ltd., August-Sept 1996
	Missing <unistd.h> and <sys/stat.h>.
	Define and initialise tet_blockable_sigs.

	Andrew Dingwall, UniSoft Ltd., September 1996
	added OP_RMALLDIRS

	Andrew Dingwall, UniSoft Ltd., July 1997
	added support the MT DLL version of the C runtime support library
	on Win32 systems

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
	Andrew Dingwall, UniSoft Ltd., August 1999
	check for missing option arguments

	Andrew Dingwall, The Open Group, March 2003
	Added support for OP_FWRITE, OP_UTIME, OP_TSFTYPE and OP_FTIME.

	Geoff Clare, The Open Group, June 2005
	Removed bogus endpwent() call.

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <time.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <direct.h>
#  include <io.h>
#else		/* -END-WIN32-CUT- */
#  include <pwd.h>
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include <signal.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "sptab.h"
#include "ltoa.h"
#include "error.h"
#include "globals.h"
#include "ftype.h"
#include "avmsg.h"
#include "valmsg.h"
#include "server.h"
#include "servlib.h"
#include "dtetlib.h"
#include "sigsafe.h"
#include "tslib.h"
#include "tccd.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* global variables */
struct ptab *tet_sdptab;		/* ptr to SYNCD ptab element */
struct ptab *tet_xdptab;		/* ptr to XRESD ptab element */

#ifdef _WIN32	/* -START-WIN32-CUT- */
char *Logfile = "c:/tmp/tccdlog";	/* file name for stdout and stderr */
#else		/* -END-WIN32-CUT- */
char *Myusername = "tet";		/* user name to run as */
char *Logfile = "/tmp/tccdlog";		/* file name for stdout and stderr */
sigset_t tet_blockable_sigs;		/* for tet_sigsafe_start() */
#endif		/* -WIN32-CUT-LINE- */


/* static function declarations */
static void op_sysid PROTOLIST((struct ptab *));
static void op_sysname PROTOLIST((struct ptab *));
static void op_tsftype PROTOLIST((struct ptab *));
static SIG_FUNC_T terminate PROTOLIST((int));


/************************************************************************
**									*
**	MAIN ROUTINES							*
**									*
************************************************************************/

int main(argc, argv)
int argc;
char **argv;
{
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	struct sigaction sa, oldsa;
#endif		/* -WIN32-CUT-LINE- */

	/* ignore certain signals */
#ifdef _WIN32	/* -START-WIN32-CUT- */
	(void) signal(SIGINT, SIG_IGN);
	(void) signal(SIGBREAK, SIG_IGN);
#else		/* -END-WIN32-CUT- */
	sa.sa_handler = SIG_IGN;
	sa.sa_flags = 0;
	(void) sigemptyset(&sa.sa_mask);
	(void) sigaction(SIGHUP, &sa, (struct sigaction *) 0);
	sa.sa_handler = SIG_IGN;
	sa.sa_flags = 0;
	(void) sigemptyset(&sa.sa_mask);
	(void) sigaction(SIGINT, &sa, (struct sigaction *) 0);
	sa.sa_handler = SIG_IGN;
	sa.sa_flags = 0;
	(void) sigemptyset(&sa.sa_mask);
	(void) sigaction(SIGQUIT, &sa, (struct sigaction *) 0);
#endif		/* -WIN32-CUT-LINE- */


	/* initialise the global variables for the library code */
	tet_init_globals("tccd", PT_STCC, -1, logerror, tet_genfatal);
	tet_root[0] = '\0';

	/* initialise signal set to be blocked by tet_sigsafe_start() */
	tet_init_blockable_sigs();

	/* open the error log file */
#ifdef _WIN32	/* -START-WIN32-CUT- */
	(void) _umask(~(_S_IREAD | _S_IWRITE) & 0777);
#else		/* -END-WIN32-CUT- */
	(void) umask((mode_t) 022);
#endif		/* -WIN32-CUT-LINE- */
	loginit();

	/* catch SIGTERM */
#ifdef _WIN32	/* -START-WIN32-CUT- */
	if (signal(SIGTERM, SIG_IGN) != SIG_IGN)
		(void) signal(SIGTERM, terminate);
#else		/* -END-WIN32-CUT- */
	if (
		sigaction(SIGTERM, (struct sigaction *) 0, &oldsa) == 0 &&
		oldsa.sa_handler != SIG_IGN
	) {
		sa.sa_handler = terminate;
		sa.sa_flags = 0;
		(void) sigemptyset(&sa.sa_mask);
		(void) sigaction(SIGTERM, &sa, (struct sigaction *) 0);
	}
#endif		/* -WIN32-CUT-LINE- */

	/* call the generic server */
	return(tet_si_main(argc, argv, 0));
}

/*
**	tet_ss_argproc() - tccd command-line argument processing
**
**	return 0 if only firstarg was used or 1 if both args were used
*/

int tet_ss_argproc(firstarg, nextarg)
char *firstarg, *nextarg;
{
	register int rc = 0;
	register int mask;
	register char *p;
	static char optmsg[] = "option needs an argument";

	switch (*(firstarg + 1)) {
#ifndef _WIN32		/* -WIN32-CUT-LINE- */
	case 'u':
		if (*(firstarg + 2))
			Myusername = firstarg + 2;
		else {
			if (!nextarg)
				fatal(0, "-u", optmsg);
			Myusername = nextarg;
			rc = 1;
		}
		break;
	case 'm':
		if (*(firstarg + 2))
			p = firstarg + 2;
		else {
			if (!nextarg)
				fatal(0, "-m", optmsg);
			p = nextarg;
			rc = 1;
		}
		mask = 0;
		while (*p >= '0' && *p <= '7')
			mask = (mask << 3) | (*p++ & 07);
		(void) umask((mode_t)(mask & 077));
		break;
#endif			/* -WIN32-CUT-LINE- */
	case 'l':
		if (*(firstarg + 2))
			Logfile = firstarg + 2;
		else {
			if (!nextarg)
				fatal(0, "-l", optmsg);
			Logfile = nextarg;
			rc = 1;
		}
		loginit();
		break;
	case 'e':
		if (*(firstarg + 2))
			p = firstarg + 2;
		else {
			if (!nextarg)
				fatal(0, "-e", optmsg);
			p = nextarg;
			rc = 1;
		}
		if (!tet_equindex(p))
			error(0, "bad format environment variable:", p);
		else if (tet_putenv(p) < 0)
			error(0, "can't add variable to environment:", p);
		break;
	default:
		return(ss_tsargproc(firstarg, nextarg));
	}

	return(rc);
}

/*
**	tet_ss_initdaemon() - tccd daemon initialisation
*/

void tet_ss_initdaemon()
{
#ifdef _WIN32	/* -START-WIN32-CUT- */
	static char defpath[] = "c:/mksnt;c:/msdev/bin;c:/windows/system32;c:/windows;c:/";
	static char null[] = "nul";
#else		/* -END-WIN32-CUT- */
	static char defpath[] = "/bin:/usr/bin";
	static char null[] = "/dev/null";
	static char msg[] = "can't find passwd entry for";
	register struct passwd *pw;
	uid_t euid;
	gid_t egid;
#endif		/* -WIN32-CUT-LINE- */
	register char *p;
	static char home[] = "HOME";
	static char path[] = "PATH";
	char buf[sizeof home + MAXPATH + 1];

#ifdef _WIN32		/* -START-WIN32-CUT- */

	/* fix up HOME environment variable if necessary */
	if ((p = getenv(home)) == (char *) 0) {
		TRACE1(tet_Ttccd, 4, "no HOME env var, using c:/");
		(void) sprintf(buf, "%s=c:/", home);
		if ((p = tet_strstore(buf)) == (char *) 0 || tet_putenv(p) < 0)
			exit(1);
	}

#else /* _WIN32 */	/* -END-WIN32-CUT- */

	/* get effecive ids */
	euid = geteuid();
	egid = getegid();

	/* get password file entry for the specified user name */
	errno = 0;
	if ((pw = getpwnam(Myusername)) == (struct passwd *) 0) {
		error(errno != ENOTTY ? errno : 0, msg, Myusername);
		if (euid < 100)
			exit(1);
		errno = 0;
		if ((pw = getpwuid(euid)) == (struct passwd *) 0)
			fatal(errno, msg, tet_i2a(euid));
	}

	/* change uid and gid if necessary */
	if (getgid() != pw->pw_gid || egid != pw->pw_gid)
		if (setgid(pw->pw_gid) < 0) {
			error(errno, "can't set gid to", tet_i2a(pw->pw_gid));
			if (egid < 100)
				exit(1);
		}
	if (getuid() != pw->pw_uid || euid != pw->pw_uid)
		if (setuid(pw->pw_uid) < 0) {
			error(errno, "can't set uid to", tet_i2a(pw->pw_uid));
			if (euid < 100)
				exit(1);
		}

	/* fix up HOME environment variable if necessary */
	if ((p = getenv(home)) == (char *) 0 || strcmp(p, pw->pw_dir)) {
		(void) sprintf(buf, "%s=%.*s", home,
			(int) sizeof buf - (int) sizeof home - 2, pw->pw_dir);
		if ((p = tet_strstore(buf)) == (char *) 0 || tet_putenv(p) < 0)
			exit(1);
	}
#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */

	/* provide a default PATH variable if necessary */
	if ((p = getenv(path)) == (char *) 0) {
		(void) sprintf(buf, "%s=%.*s", path,
			(int) sizeof buf - (int) sizeof path - 2, defpath);
		TRACE2(tet_Ttccd, 4, "no PATH env var, using default: \"%s\"",
			buf);
		if ((p = tet_strstore(buf)) == (char *) 0 || tet_putenv(p) < 0)
			exit(1);
	}

	/* remember home directory for later and chdir to there */
	p = getenv(home);
	ASSERT(p && *p);
	if (tetrootset(p) < 0)
		exit(1);
#ifdef _WIN32		/* -START-WIN32-CUT- */
	TRACE2(tet_Ttccd, 4, "chdir to HOME: \"%s\"", tet_root);
#else			/* -END-WIN32-CUT- */
	TRACE3(tet_Ttccd, 4, "chdir to %s HOME: \"%s\"", pw->pw_name, tet_root);
#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */
	if (CHDIR(tet_root) < 0)
		fatal(errno, "can't chdir to", tet_root);

	/* perform syncd transport-specific initialisation */
	ss_tsinitb4fork();

	/* attach stdin to /dev/null and stdout to stderr (which goes
		to the log file */
	(void) CLOSE(0);
	errno = 0;
	if (OPEN(null, O_RDONLY, 0) != 0)
		fatal(errno, "can't open", null);
	(void) CLOSE(1);
	errno = 0;
	if (FCNTL_F_DUPFD(FILENO(stderr), 1) != 1)
		fatal(errno, "can't attach stdout to stderr", (char *) 0);

#ifndef INITTAB
	/* start the daemon */
	ts_forkdaemon();
#endif

	/* detach from the control terminal (if any) */
	tet_tiocnotty();

	/* make a log file entry */
	ts_logstart();

}

/*
**	tet_ss_serverloop() - tccd daemon main processing loop
**
**	always return 1 because we always want to come back
*/

int tet_ss_serverloop()
{
	tet_si_serverloop();
	return(1);
}

/************************************************************************
**									*
**	SERVER-SPECIFIC PARTS OF GENERIC SERVICE ROUTINES		*
**									*
************************************************************************/

/*
**	tet_ss_dead() - server-specific routine to handle a dead process
*/

void tet_ss_dead(pp)
register struct ptab *pp;
{
	static char fmt[] = "%s connection closed";
	static char cl[] = "client";
	static char se[] = "server";
	char msg[sizeof fmt + sizeof cl];

	/* emit a diagnostic message if this is unexpected */
	if ((pp->pt_flags & PF_LOGGEDOFF) == 0) {
		(void) sprintf(msg, fmt, (pp->pt_flags & PF_SERVER) ? se : cl);
		error(0, msg, tet_r2a(&pp->pt_rid));
	}

	pp->pt_flags = (pp->pt_flags & ~PF_LOGGEDON) | PF_LOGGEDOFF;

	/*
	** for clients:
	**	remove any related file and exec table elements
	**	remove ptab entry
	**	exit if it was the last one
	*/
	if ((pp->pt_flags & PF_SERVER) == 0) {
		tet_fiodead(pp);
		etdead(pp);
		tet_ptrm(pp);
		tet_ptfree(pp);
		if (!tet_ptab)
			tet_ss_cleanup();
	}
}

/*
**	tet_ss_connect() - connect to remote process
*/

void tet_ss_connect(pp)
struct ptab *pp;
{
	tet_ts_connect(pp);
}

/************************************************************************
**									*
**	FULL REQUEST PROCESSING ROUTINES				*
**									*
************************************************************************/

/*
**	tet_ss_process() - request processing routine
*/

void tet_ss_process(pp)
struct ptab *pp;
{
	/* a server ptab might get here via tet_si_servwait() */
	if ((pp->pt_flags & PF_SERVER) == 0)
		tet_si_serverproc(pp);
}

/*
**	tet_ss_serverproc() - request processing as a server
*/

void tet_ss_serverproc(pp)
register struct ptab *pp;
{
	switch (pp->ptm_req) {
	case OP_SYSID:
		op_sysid(pp);
		break;
	case OP_TSINFO:
		op_tsinfo(pp);
		break;
	case OP_SYSNAME:
		op_sysname(pp);
		break;
	case OP_EXEC:
		op_exec(pp);
		break;
	case OP_WAIT:
		op_wait(pp);
		break;
	case OP_KILL:
		op_kill(pp);
		break;
	case OP_CFNAME:
		op_cfname(pp);
		break;
	case OP_SNDCONF:
		op_sndconf(pp);
		break;
	case OP_RCVCONF:
		op_rcvconf(pp);
		break;
	case OP_PUTENV:
		op_putenv(pp);
		break;
	case OP_ACCESS:
		op_access(pp);
		break;
	case OP_MKDIR:
		op_mkdir(pp);
		break;
	case OP_RMDIR:
		op_rmdir(pp);
		break;
	case OP_CHDIR:
		op_chdir(pp);
		break;
	case OP_FOPEN:
		tet_op_fopen(pp);
		break;
	case OP_FCLOSE:
		tet_op_fclose(pp);
		break;
	case OP_PUTS:
		tet_op_puts(pp);
		break;
	case OP_LOCKFILE:
		op_lockfile(pp);
		break;
	case OP_SHARELOCK:
		op_sharelock(pp);
		break;
	case OP_MKTMPDIR:
		op_mktmpdir(pp);
		break;
	case OP_UNLINK:
		op_unlink(pp);
		break;
	case OP_CONFIG:
		op_config(pp);
		break;
	case OP_RXFILE:
		op_rxfile(pp);
		break;
	case OP_RCOPY:
		op_rcopy(pp);
		break;
	case OP_MKSDIR:
		op_mksdir(pp);
		break;
	case OP_TSFILES:
		op_tsfiles(pp);
		break;
	case OP_SETCONF:
		op_setconf(pp);
		break;
	case OP_MKALLDIRS:
		op_mkalldirs(pp);
		break;
	case OP_RMALLDIRS:
		op_rmalldirs(pp);
		break;
	case OP_TIME:
		op_time(pp);
		break;
	case OP_FWRITE:
		tet_op_fwrite(pp);
		break;
	case OP_UTIME:
		op_utime(pp);
		break;
	case OP_TSFTYPE:
		op_tsftype(pp);
		break;
	case OP_FTIME:
		op_ftime(pp);
		break;
	default:
		pp->ptm_rc = ER_REQ;
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
		break;
	}

	/* here to send a reply message */
	pp->pt_state = PS_SNDMSG;
	pp->pt_flags |= PF_ATTENTION;
}

/*
**	op_sysid() - assign system id
*/

static void op_sysid(pp)
register struct ptab *pp;
{
	register struct valmsg *mp = (struct valmsg *) pp->ptm_data;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the request message */
	if ((int) mp->vm_nvalue != OP_SYSID_NVALUE) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	if (tet_mysysid >= 0) {
		error(0, "sysid already assigned:", tet_i2a(tet_mysysid));
		pp->ptm_rc = ER_ERR;
		return;
	}

	tet_mysysid = (int) VM_SYSID(mp);
	logent("my sysid =", tet_i2a(tet_mysysid));
	pp->ptm_rc = ER_OK;
}

/*
**	op_sysname() - receive a system name list
**
**	in the request message:
**		VM_SYSNAME(0) .. VM_SYSNAME(op_sysname_nsname() - 1) =
**			list of system names
**
*/

static void op_sysname(pp)
register struct ptab *pp;
{
	register struct valmsg *mp = (struct valmsg *) pp->ptm_data;
	register struct sptab *sp = (struct sptab *) pp->pt_sdata;
	register int *snp;
	register int i, j, needlen;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the request message -
		make sure that there are no duplicates in the system
		name list */
	if (OP_SYSNAME_NSNAME(mp) < 1) {
		pp->ptm_rc = ER_INVAL;
		return;
	}
	for (i = 0; i < OP_SYSNAME_NSNAME(mp); i++) {
		if (VM_SYSNAME(mp, i) < 0) {
			pp->ptm_rc = ER_INVAL;
			return;
		}
		for (j = 0; j < i; j++)
			if (VM_SYSNAME(mp, j) == VM_SYSNAME(mp, i)) {
				pp->ptm_rc = ER_DUPS;
				return;
			}
	}

	/* allocate storage for the system name list */
	needlen = OP_SYSNAME_NSNAME(mp) * sizeof *sp->sp_snames;
	if (BUFCHK((char **) &sp->sp_snames, &sp->sp_snlen, needlen) < 0) {
		pp->ptm_rc = ER_ERR;
		return;
	}

	/* copy over the system name list */
	sp->sp_nsname = OP_SYSNAME_NSNAME(mp);
	for (i = 0, snp = sp->sp_snames; i < OP_SYSNAME_NSNAME(mp); i++, snp++)
		*snp = (int) VM_SYSNAME(mp, i);

	pp->ptm_rc = ER_OK;
}

/*
**	op_tsftype() - receive a set of file types
*/
static void op_tsftype(pp)
register struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;
	char line[1024];
	char *flds[3];
	int ftype, n, nflds;

#ifndef NOTRACE
	if (tet_Ttccd > 0) {
		register int n;
		TRACE2(tet_Ttccd, 4, "OP_TSFTYPE: nlines = %s",
			tet_i2a(OP_TSFTYPE_NTSFTYPE(mp)));
		for (n = 0; n < OP_TSFTYPE_NTSFTYPE(mp); n++)
			TRACE2(tet_Ttccd, 4, "ftype line = \"%s\"",
				AV_TSFTYPE(mp, n));
	}
#endif

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* decode each file type line and store it */
	pp->ptm_rc = ER_OK;
	for (n = 0; n < OP_TSFTYPE_NTSFTYPE(mp); n++) {
		/* split the line into fields */
		sprintf(line, "%.*s",
			(int) sizeof line - 1, AV_TSFTYPE(mp, n));
		nflds = tet_getargs(line, flds, sizeof flds / sizeof flds[0]);
		if (nflds < 2) {
			error(0,
				"wrong number of fields in OP_TSFTYPE request:",
				AV_TSFTYPE(mp, n));
			pp->ptm_rc = ER_INVAL;
			continue;
		}
		/* decode the ftype field */
		ftype = atoi(flds[1]);
		switch (ftype) {
		case TET_FT_ASCII:
		case TET_FT_BINARY:
			/* OK - store it */
			if (tet_addftype(flds[0], ftype) < 0)
				pp->ptm_rc = ER_ERR;
			break;
		default:
			pp->ptm_rc = ER_INVAL;
			error(0, "bad ftype in OP_TSFTYPE request:",
				tet_i2a(ftype));
			break;
		}
	}
}

/************************************************************************
**									*
**	SERVER-SPECIFIC PARTS OF GENERIC REQUEST PROCESSING		*
**									*
************************************************************************/

/*
**	tet_ss_logon() - server-specific logon processing routine
**
**	return ER_OK if successful or other ER_* error code on error
*/

int tet_ss_logon(pp)
struct ptab *pp;
{
	switch (pp->ptr_ptype) {
	case PT_MTCC:
	case PT_MTCM:
	case PT_STCM:
		return(ss_tslogon());
	default:
		error(0, "unexpected connected process type",
			tet_r2a(&pp->pt_rid));
		return(ER_ERR);
	}
}

/*
**	tet_ss_logoff() - server-specific logoff processing routine
*/

void tet_ss_logoff(pp)
struct ptab *pp;
{
	etdead(pp);
}

/*
**	tet_ss_cleanup() - clean up and exit
*/

void tet_ss_cleanup()
{
	config_cleanup();
	(void) tet_xdlogoff();
	tet_ts_cleanup();
	exit(0);
}

/************************************************************************
**									*
**	PUBLIC SUBROUTINES						*
**									*
************************************************************************/

/*
**	tet_ss_newptab() - server-specific new ptab entry handler
*/

void tet_ss_newptab(pp)
struct ptab *pp;
{
	/* add the entry to the process table */
	tet_ptadd(pp);
}

/************************************************************************
**									*
**	PRIVATE SUBROUTINES						*
**									*
************************************************************************/

/*
**	terminate() - SIGTERM signal handler
*/

static SIG_FUNC_T terminate(sig)
int sig;
{
	logent("going down on signal", tet_i2a(sig));
	tet_ss_cleanup();
	/* NOTREACHED */
}

/*
**	tetrootset() - assign a new value to tet_root
**
**	return 0 if successful or -1 on error
*/

int tetrootset(s)
char *s;
{
	(void) sprintf(tet_root, "%.*s", (int) sizeof tet_root - 1, s);
	return(0);
}

