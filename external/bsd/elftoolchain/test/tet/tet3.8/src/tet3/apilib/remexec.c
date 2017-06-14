/*
 *      SCCS:  @(#)remexec.c	1.24 (00/04/03) 
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
static char sccsid[] = "@(#)remexec.c	1.24 (00/04/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)remexec.c	1.24 00/04/03 TETware release 3.8
NAME:		remexec.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

SYNOPSIS:
	#include "tet_api.h"
	int tet_remexec(int sysname, char *file, char **argv);

DESCRIPTION:
	DTET API function

	execute process on remote system
	return remoteid if successful or -1 on error

	errno is set to one of the following when tet_remexec returns -1
	(a * indicates a value not mentioned in the DTET spec)

		EINVAL	sysname does not refer to a known slave system

		ENOEXEC file cannot be executed on the remote system,
			or sync with remote process failed

		ENOMEM*	out of memory on the local system

		EFAULT*	null file or argv value

		EIO*	communication with server processes failed

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, August 1993
	fix SEGV in tet_remexec when parameter list *ap not NULL terminated.

	Andrew Dingwall, UniSoft Ltd., December 1993
	changed dapi.h to dtet2/tet_api.h

	Geoff Clare, UniSoft Ltd., July 1996
	Changes for TETWare.

	Andrew Dingwall, UniSoft Ltd., August 1996
	co-operate with tet_remtime() when logging on to TCCDs

	Geoff Clare, UniSoft Ltd., Sept 1996
	Make rtab updates signal safe.
	Changes for TETWare-Lite.

	Andrew Dingwall, UniSoft Ltd., July 1998
	As a result of changes to the way in which tcc processes
	distributed configuration variables, we no longer need to process
	tcc command-line variables here (code which does this has been
	excluded with #if 0).
	Fixed a bug which caused a SIGSEGV if there were no distributed
	configuration variables.
	Added support for shared API libraries.
	Send TET_ROOT and TET_EXECUTE environment variables to tccd
	as part of the remote configuration process.
 
	Andrew Dingwall, UniSoft Ltd., June 1999
	Look for tetexec.cfg on the remote system in the alternate
	execution directory if one is defined, before looking in the
	test suite root directory.

	Andrew Dingwall, UniSoft Ltd., July 1999
	added support for 'other language' APIs

	Andrew Dingwall, UniSoft Ltd., October 1999
	added support for strict POSIX threads

	Andrew Dingwall, UniSoft Ltd., October March 2000
	set the tccd configuration mode after performing a config
	variable exchange


************************************************************************/

#ifndef TET_LITE /* -START-LITE-CUT- */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <signal.h>
#include <time.h>
#include "dtmac.h"
#include "tet_api.h"
#include "dtmsg.h"
#include "ptab.h"
#include "rtab.h"
#include "avmsg.h"
#include "synreq.h"
#include "ltoa.h"
#include "error.h"
#include "globals.h"
#include "sysent.h"
#include "config.h"
#include "servlib.h"
#include "dtetlib.h"
#include "apilib.h"
#include "sigsafe.h"
#include "bitset.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* the maximum number of systems (0 -> 999) */
#define NUMSYS		1000

static char **econf;			/* master exec econfig variables */
static int neconf;			/* number of master exec variables */
static int leconf;			/* number of bytes in *econf */
static char **dconf;			/* master distrib config variables */
static int ndconf;			/* number of master dist variables */
static int ldconf;			/* number of bytes in *dconf */
#if 0
static char **cconf;			/* MTCC cmd line config variables */
static int ncconf;			/* number of MTCC cmd line variables */
static int lcconf;			/* number of bytes in *cconf */
#endif


/* static function declarations */
static int do_tclogon PROTOLIST((int));
static int getcf2 PROTOLIST((char *, char ***, int *, int *));
static int getcf3 PROTOLIST((int, char ***, int *, int *));
static int getconf PROTOLIST((void));
static long tet_re2 PROTOLIST((int, char *, char **));
static int xcf2 PROTOLIST((int, char **, int));
static int xconfig PROTOLIST((int));


int tet_remexec(sysname, file, argv)
int sysname;
char *file;
register char **argv;
{
	register struct rtab *rp;
	register char **ap;
	register long pid;
	register int needlen, rc;
	TET_SIGSAFE_DEF
	char thistest[LNUMSZ], activity[LNUMSZ];
	char context[LNUMSZ], block[LNUMSZ];
	static char **newargv;
	static int nalen;

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	/* do a sanity check on the arguments */
	if (sysname <= 0 || sysname >= NUMSYS || sysname == tet_mysysid) {
		errno = EINVAL;
		tet_errno = TET_ER_INVAL;
		return(-1);
	}
	if (!file || !argv) {
		errno = EFAULT;
		tet_errno = TET_ER_INVAL;
		return(-1);
	}

	/* count the arguments */
	for (ap = argv; *ap; ap++)
		;

	/*
	** build the new argv with file, thistest, activity, context and block
	** at the front
	*/
	needlen = ((ap - argv) + TET_TCMC_USER_ARGS + 1) * sizeof *newargv;
	if (BUFCHK((char **) &newargv, &nalen, needlen) < 0) {
		errno = ENOMEM;
		tet_errno = TET_ER_ERR;
		return(-1);
	}
	(void) sprintf(thistest, "%d", tet_thistest);
	(void) sprintf(activity, "%ld", tet_activity);
	(void) sprintf(context, "%ld", tet_context);
	(void) sprintf(block, "%ld", tet_block);
	*newargv = file;
	*(newargv + TET_TCMC_THISTEST) = thistest;
	*(newargv + TET_TCMC_ACTIVITY) = activity;
	*(newargv + TET_TCMC_CONTEXT) = context;
	*(newargv + TET_TCMC_BLOCK) = block;

	/* copy over the user-supplied arguments */
	for (ap = newargv + TET_TCMC_USER_ARGS; *argv; ap++, argv++)
		*ap = *argv;
	*ap = (char *) 0;

	/* allocate an rtab element for the result */
	if ((rp = tet_rtalloc()) == (struct rtab *) 0) {
		errno = ENOMEM;
		tet_errno = TET_ER_ERR;
		return(-1);
	}

	/* do the rest of the remote execution and update the rtab */

	TET_SIGSAFE_START;

	if ((pid = tet_re2(sysname, file, newargv)) < 0) {
		tet_rtfree(rp);
		/* tet_errno is set in tet_re2() */
		rc = -1;
	}
	else {
		rp->rt_sysid = sysname;
		rp->rt_pid = pid;
		tet_rtadd(rp);
		rc = rp->rt_remoteid;
	}

	TET_SIGSAFE_END;

	return(rc);
}

/*
**	tet_re2() - extend the tet_remexec() processing
**
**	return pid of exec'd process if successful or -1 (with errno set)
**	on error
*/

static long tet_re2(sysid, file, argv)
int sysid;
char *file, **argv;
{
	long pid;
	int sysnames[2];
	register long snid;

	/* check sysid is valid */
	if (!tet_libgetsysbyid(sysid))
	{
		errno = EINVAL;
		tet_errno = TET_ER_SYSID;
		return(-1L);
	}

	/* log on to the TCCD and configure it if necessary */
	if (do_tclogon(sysid) < 0) {
		errno = EINVAL;
		tet_errno = TET_ER_ERR; /* do_tclogon reports errors */
		return(-1L);
	}

	/* get a snid for use in the sync after the exec */
	if ((snid = tet_sdsnget()) < 0L) {
		errno = EIO;
		tet_errno = -tet_sderrno;
		return(-1L);
	}

	/* register our sysid and the remote sysid with SYNCD */
	sysnames[0] = tet_mysysid;
	sysnames[1] = sysid;
	if (tet_sdsnsys(snid, sysnames, 2) < 0) {
		errno = EIO;
		tet_errno = -tet_sderrno;
		return(-1L);
	}

	/* do the remote exec */
	if ((pid = tet_tcuexec(sysid, file, argv, snid, tet_xrid)) < 0L) {
		errno = ENOEXEC;
		tet_errno = -tet_tcerrno;
		return(-1L);
	}

	/* do an auto-sync with the new process, kill the process and wait
		for it a bit if this fails */
	if (tet_sdasync(snid, tet_xrid, SV_EXEC_SPNO, SV_YES, SV_EXEC_TIMEOUT, (struct synreq *) 0, (int *) 0) < 0) {
		(void) tet_tckill(sysid, pid, SIGTERM);
		(void) tet_tcwait(sysid, pid, 10, (int *) 0);
		errno = ENOEXEC;
		tet_errno = -tet_sderrno;
		return(-1L);
	}

	return(pid);
}

/*
**	do_tclogon() - log on to the TCCD and do a config variable exchange
**
**	return 0 if successful or -1 on error
*/

static int do_tclogon(sysid)
int sysid;
{
	static int been_here = 0;
	static long configured[NEEDELEM(NUMSYS)];

	/* log on to the TCCD if necessary */
	if (!tet_getptbysysptype(sysid, PT_STCC) && tet_tclogon(sysid) < 0)
		return(-1);

	/* return now if the TCCD has already been configured */
	ASSERT(sysid >= 0 && sysid < NUMSYS);
	if (ISSET(sysid, configured))
		return(0);

	/*
	** here to configure the TCCD -
	** get the config variables from XRESD first time through
	*/
	if (!been_here) {
		if (getconf() < 0)
			return(-1);
		been_here = 1;
	}

	/* send the system name list to the TCCD */
	if (tet_tcsysname(sysid, tet_snames, tet_Nsname) < 0) {
		error(0, "tet_tcsysname failed, rc =",
			tet_ptrepcode(tet_tcerrno));
		return(-1);
	}

	/* perform a config variable exchange with TCCD */
	if (xconfig(sysid) < 0)
		return(-1);

	/* all ok so return success */
	BITSET(sysid, configured);
	return(0);
}

/*
**	getconf() - get the set of master config lines from XRESD
**
**	return 0 if successful or -1 on error
*/

static int getconf()
{
	register int n;
	register char **ap;
	static char *cfname[XD_NCFNAME];

	/* get the config file names from XRESD */
	if (!cfname[XD_NCFNAME - 1]) {
		if ((ap = tet_xdrcfname()) == (char **) 0) {
			error(0, "tet_xdrcfname failed, rc =",
				tet_ptrepcode(tet_xderrno));
			return(-1);
		}
		for (n = 0; n < XD_NCFNAME; n++) {
			if ((cfname[n] = tet_strstore(*ap++)) == (char *) 0)
				return(-1);
			TRACE3(tet_Ttcm, 4,
				"master config file name %s = \"%s\"",
				tet_i2a(n), cfname[n]);
		}
	}

	/* get the master exec and distrib config lines, and the MTCC
		cmd line config lines */
	if (
		getcf2(cfname[0], &econf, &leconf, &neconf) < 0
		|| getcf2(cfname[1], &dconf, &ldconf, &ndconf) < 0
#if 0
		|| getcf2(cfname[2], &cconf, &lcconf, &ncconf) < 0
#endif
	)
		return(-1);

	/* all ok so return success */
	return(0);
}

/*
**	getcf2() - get a set of lines from a single master config file
**
**	return 0 if successful or -1 on error
*/

static int getcf2(cfname, confp, lconfp, nconfp)
char *cfname;
char ***confp;
int *lconfp, *nconfp;
{
	register int fid, rc;
	static char fmt[] = "tet_xdfopen(\"%.*s\") failed, rc =";
	char msg[sizeof fmt + MAXPATH];

	TRACE2(tet_Ttcm, 4, "get master config variables from \"%s\"", cfname);

	/* open the master config file */
	if ((fid = tet_xdfopen(cfname)) < 0) {
		(void) sprintf(msg, fmt, MAXPATH, cfname);
		error(0, msg, tet_ptrepcode(tet_xderrno));
		return(-1);
	}

	/* get the config lines and close the file */
	rc = getcf3(fid, confp, lconfp, nconfp);
	(void) tet_xdfclose(fid);

	return(rc);
}

/*
**	getcf3() - get master config lines and add them to the list pointed to
**		by *confp containing *nconfp entries
**
**	return 0 if successful or -1 on error
*/

static int getcf3(fid, confp, lconfp, nconfp)
int fid;
char ***confp;
int *lconfp, *nconfp;
{
	register char **p1, **p2;
	register int n;
	int nlines, eof;
	char *cp[AV_NLINE];

	/* read in all the config lines and store them */
	do {
		nlines = AV_NLINE;
		if ((p1 = tet_xdgetsv(fid, &nlines, &eof)) == (char **) 0) {
			error(0, "tet_xdgetsv failed, rc =",
				tet_ptrepcode(tet_xderrno));
			return(-1);
		}
		if (nlines <= 0)
			continue;
		for (p2 = cp, n = 0; n < nlines && n < AV_NLINE; n++, p1++)
			if (*p1 && **p1 && **p1 != '#') {
				TRACE2(tet_Ttcm, 4,
					"read variable \"%s\"", *p1);
				if ((*p2++ = tet_strstore(*p1)) == (char *) 0)
					return(-1);
			}
		nlines = p2 - cp;
		if (BUFCHK((char **) confp, lconfp, (int) ((*nconfp + nlines) * sizeof **confp)) < 0)
			return(-1);
		p1 = cp;
		p2 = *confp + *nconfp;
		for (n = 0; n < nlines; n++)
			*p2++ = *p1++;
		*nconfp += n;
	} while (!eof);

	/* all ok so return success */
	return(0);
}

/*
**	xconfig() - perform a config variable exchange
**
**	return 0 if successful or -1 on error
*/

static int xconfig(sysid)
int sysid;
{
	char **sconf = (char **) 0;
	int lsconf = 0, nsconf = 0;
#if 0
	char **tconf = (char **) 0;
	int ltconf = 0, ntconf = 0;
#endif
	register char **p1, **p2;
#if 0
	char **p3;
#endif
	register char *s1, *s2 = "";
	register int n, rc;
	char buf[MAXPATH + 48];
	char *envs[2];
#define Nenvs (sizeof envs / sizeof envs[0])
	char **ep;
	char *tetroot_value, *tsroot_value, *tetexec_value;
	static char tetroot_name[] = "TET_ROOT";
	static char tsroot_name[] = "TET_TSROOT";
	static char tetexec_name[] = "TET_EXECUTE";
	static char ecfname[] = "tetexec.cfg";
	static char fmt[] = "can't access exec mode configuration file %.*s, rc =";
	char msg[sizeof fmt + MAXPATH];

	if ((nsconf = neconf
#if 0
		+ ndconf
#endif
#if 0
		+ ncconf
#endif
			) <= 0)
				return(0);

	/*
	** look for TET_REMnnn_TET_ROOT, TET_REMnnn_TET_TSROOT,
	** TET_REMnnn_TET_EXECUTE
	** in the distrib config information
	*/
	tetroot_value = (char *) 0;
	tsroot_value = (char *) 0;
	tetexec_value = (char *) 0;
	if (dconf)
		for (p1 = dconf + ndconf - 1; p1 >= dconf; p1--) {
			if (tetroot_value && tsroot_value && tetexec_value)
				break;
			if (
				(s1 = tet_remvar(*p1, sysid)) == (char *) 0 ||
				(s2 = tet_equindex(*p1)) == (char *) 0
			) {
				error(0, "ignored bad format distrib config line:", *p1);
				continue;
			}

			if (
				!tetroot_value && s1 != *p1 &&
				!strncmp(s1, tetroot_name,
					sizeof tetroot_name - 1) &&
				s2 == s1 + sizeof tetroot_name - 1
			)
				tetroot_value = s2 + 1;

			if (
				!tsroot_value && s1 != *p1 &&
				!strncmp(s1, tsroot_name,
					sizeof tsroot_name - 1) &&
				s2 == s1 + sizeof tsroot_name - 1
			)
				tsroot_value = s2 + 1;

			if (
				!tetexec_value && s1 != *p1 &&
				!strncmp(s1, tetexec_name,
					sizeof tetexec_name - 1) &&
				s2 == s1 + sizeof tetexec_name - 1
			)
				tetexec_value = s2 + 1;

		}

#if 0
	/* if not found, look for TET_TSROOT without the REMnnn */
	if (!tsroot_value && dconf)
		for (p1 = dconf + ndconf - 1; p1 >= dconf; p1--)
			if (
				!tsroot_value &&
				(s2 = tet_equindex(*p1)) != (char *) 0 &&
				!strncmp(*p1, tsroot_name,
					sizeof tsroot_value - 1) &&
				s2 == p1 + sizeof tsroot_name - 1
			) {
				tsroot_value = s2 + 1;
				break;
			}
#endif

	/* if still not found, return error */
	if (!tetroot_value) {
		error(0, "no TET_ROOT in the distributed configuration for system", tet_i2a(sysid));
		return(-1);
	}
	if (!tsroot_value) {
		error(0, "no TET_TSROOT in the distributed configuration for system", tet_i2a(sysid));
		return(-1);
	}

	/* send TET_ROOT and TET_EXECUTE environment variables to tccd */
	rc = 0;
	for (ep = envs; ep < &envs[Nenvs]; ep++)
		*ep = (char *) 0;
	ep = &envs[0];
	(void) sprintf(buf, "%s=%.*s",
		tetroot_name,
		(int) sizeof buf - (int) sizeof tetroot_name - 1,
		tetroot_value);
	if ((*ep++ = tet_strstore(buf)) == (char *) 0)
		rc = -1;
	if (!rc && tetexec_value) {
		(void) sprintf(buf, "%s=%.*s",
			tetexec_name,
			(int) sizeof buf - (int) sizeof tetexec_name - 1,
			tetexec_value);
		if ((*ep++ = tet_strstore(buf)) == (char *) 0)
			rc = -1;
	}
	if (!rc && tet_tcputenvv(sysid, envs, (int) (ep - envs)) < 0) {
		error(0, "tet_putenvv failed, rc =",
			tet_ptrepcode(tet_tcerrno));
		rc = -1;
	}
	for (ep = envs; ep < &envs[Nenvs]; ep++)
		if (*ep) {
			TRACE2(tet_Tbuf, 6, "free env string = %s",
				tet_i2x(*ep));
			free(*ep);
		}
	if (rc)
		return(rc);

	/* construct target system exec config file name */
	if (tetexec_value) {
		(void) sprintf(buf, "%.*s/%s",
			(int) sizeof buf - (int) sizeof ecfname - 1,
				tetexec_value, ecfname);
		if (tet_tcaccess(sysid, buf, 0) < 0)
			(void) sprintf(buf, "%.*s/%s",
				(int) sizeof buf - (int) sizeof ecfname - 1,
					tsroot_value, ecfname);
	}
	else
		(void) sprintf(buf, "%.*s/%s",
			(int) sizeof buf - (int) sizeof ecfname - 1,
				tsroot_value, ecfname);

	/* ensure that the remote exec config file is accessible */
	if (tet_tcaccess(sysid, buf, 04) < 0) {
		(void) sprintf(msg, fmt, MAXPATH, buf);
		error(0, msg, tet_ptrepcode(tet_tcerrno));
		return(-1);
	}

	/* send exec config file name to TCCD */
	TRACE3(tet_Ttcm, 6, "send exec config file name \"%s\" to system %s",
		buf, tet_i2a(sysid));
	if (tet_tccfname(sysid, buf) < 0) {
		error(0, "tet_tccfname failed, rc =",
			tet_ptrepcode(tet_tcerrno));
		return(-1);
	}

	/* allocate storage for the list of config lines to send */
	if (BUFCHK((char **) &sconf, &lsconf, (int) (nsconf * sizeof *sconf)) < 0)
		return(-1);

	/* copy over the master exec and distrib config lines */
	p2 = sconf;
	for (p1 = econf, n = 0; n < neconf; n++)
		*p2++ = *p1++;
#if 0
	for (p1 = dconf, n = 0; n < ndconf; n++)
		*p2++ = *p1++;
#endif

#if 0
	/*
	** do two passes over the MTCC cmd line variables:
	**	in the first pass:
	**		if a variable is not a TET_REMnnn one, prepend
	**		a TET_REMnnn string so that it will override a
	**		local value on the remote system;
	**		a pointer to the new string is stored in a temporary
	**		list (tconf) so that it can be freed later
	**	in the second pass:
	**		add the TET_REMnnn variables last of all,
	**		giving them precedence over all the others
	*/

	/* first pass */
	for (p1 = cconf, n = ncconf; n > 0; p1++, n--) {
		if (tet_remvar(*p1, -1) != *p1)
			continue;
		(void) sprintf(buf, "TET_REM%03d_%.*s", sysid % 1000,
			(int) sizeof buf - 12, *p1);
		if (BUFCHK((char **) &tconf, &ltconf, (int) ((ntconf + 1) * sizeof *tconf)) < 0)
			return(-1);
		p3 = tconf + ntconf++;
		if ((*p3 = tet_strstore(buf)) == (char *) 0)
			return(-1);
		if (p2 < sconf + nsconf)
			*p2++ = *p3;
	}

	/* second pass */
	for (p1 = cconf, n = ncconf; n > 0; p1++, n--) {
		if (tet_remvar(*p1, sysid) == *p1)
			continue;
		if (p2 < sconf + nsconf)
			*p2++ = *p1;
	}
#endif

	rc = xcf2(sysid, sconf, nsconf);

	/* free memory allocated above */
#if 0
	if (tconf) {
		for (p1 = tconf + ntconf - 1; p1 >= tconf; p1--)
			if (*p1) {
				TRACE2(tet_Tbuf, 6, "free tmp conf line = %s",
					tet_i2x(*p1));
				free(*p1);
			}
		TRACE2(tet_Tbuf, 6, "free tconf = %s", tet_i2x(tconf));
		free((char *) tconf);
	}
#endif
	if (sconf) {
		TRACE2(tet_Tbuf, 6, "free sconf = %s", tet_i2x(sconf));
		free((char *) sconf);
	}

	return(rc);
}

/*
**	xcf2() - extend the xconfig processing
**
**	return 0 if successful or -1 on error
*/

static int xcf2(sysid, sconf, nsconf)
int sysid, nsconf;
char **sconf;
{
	char **rconf = (char **) 0;
	int lrconf = 0, nrconf = 0;
	int nlines, done;
	register int n, err;
	register char **p1, **p2;

	/* send the config lines */
	if (tet_tcsndconfv(sysid, sconf, nsconf) < 0) {
		error(0, "tet_tcsndconfv failed, rc =",
			tet_ptrepcode(tet_tcerrno));
		return(-1);
	}

	/* receive the merged ones back */
	err = 0;
	do {
		if ((p1 = tet_tcrcvconfv(sysid, &nlines, &done)) == (char **) 0) {
			error(0, "tet_tcrcvconfv failed, rc =",
				tet_ptrepcode(tet_tcerrno));
			return(-1);
		}
		if (nlines <= 0)
			continue;
		if (BUFCHK((char **) &rconf, &lrconf, (int) ((nrconf + nlines) * sizeof *rconf)) < 0)
			return(-1);
		p2 = rconf + nrconf;
		for (n = 0; n < nlines; n++, p1++)
			if (*p1 && (*p2++ = tet_strstore(*p1)) == (char *) 0) {
				err = 1;
				break;
			}
		nrconf = p2 - rconf;
	} while (!done && !err);

	/* assign the merged lines to the TCCD */
	if (!err && tet_tcconfigv(sysid, rconf, nrconf, TC_CONF_EXEC) < 0) {
		error(0, "tet_tcconfigv failed, rc = ",
			tet_ptrepcode(tet_tcerrno));
		err = 1;
	}
	if (!err && tet_tcsetconf(sysid, TC_CONF_EXEC) < 0) {
		error(0, "tet_tcsetconf failed, rc = ",
			tet_ptrepcode(tet_tcerrno));
		err = 1;
	}

	/* free storage occupied by the merged lines */
	if (rconf) {
		for (p1 = rconf + nrconf - 1; p1 >= rconf; p1--)
			if (*p1) {
				TRACE2(tet_Tbuf, 6,
					"free rcvd config line = %s",
					tet_i2x(*p1));
				free(*p1);
			}
		TRACE2(tet_Tbuf, 6, "free rconf = %s", tet_i2x(rconf));
		free((char *) rconf);
	}

	return(err ? -1 : 0);
}

#else /* -END-LITE-CUT- */

/* avoid "empty" file */
int tet_remexec_not_supported;

#endif /* -LITE-CUT-LINE- */

