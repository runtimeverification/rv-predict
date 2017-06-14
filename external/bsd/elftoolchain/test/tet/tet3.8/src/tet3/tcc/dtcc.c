/*
 *	SCCS: @(#)dtcc.c	1.7 (99/11/15)
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
static char sccsid[] = "@(#)dtcc.c	1.7 (99/11/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)dtcc.c	1.7 99/11/15 TETware release 3.8
NAME:		dtcc.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	functions related to distributed processing
	interface to the generic client/server

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., June 1997
	check each environment string for valid format before sending
	it to tccd on system 0

	Andrew Dingwall, UniSoft Ltd., October 1999
	added dtcc_exit() which is like tet_exit() but doesn't need to
	worry about the strict POSIX threads stuff

************************************************************************/

#ifndef TET_LITE	/* -START-LITE-CUT- */

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <time.h>
#include <errno.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "error.h"
#include "globals.h"
#include "ltoa.h"
#include "server.h"
#include "servlib.h"
#include "dtetlib.h"
#include "tslib.h"
#include "config.h"
#include "systab.h"
#include "tcc.h"
#include "dtcc.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
extern char **ENVIRON;
#endif		/* -WIN32-CUT-LINE- */

/* ptrs to syncd and xresd ptabs */
struct ptab *tet_sdptab, *tet_xdptab;

/* static function declarations */
static int env2sys0 PROTOLIST((void));
static int sd_start PROTOLIST((void));
static int tccdlogon PROTOLIST((void));
static int ti_st2 PROTOLIST((struct ptab *, char **));
static struct ptab *ti_stserver PROTOLIST((int, char **));
static int xd_start PROTOLIST((char *));


/*
**	initdtcc() - start up all the servers and log on to them
*/

void initdtcc()
{

	TRACE1(tet_Ttcc, 1, "initdtcc(): start up the servers and log on to each remote system");

	tet_ts_startup();

	if (
		ts_tccinit() < 0 ||
		sd_start() < 0 ||
		tet_sdlogon() < 0 ||
		xd_start(resdirname()) < 0 ||
		tet_xdlogon() < 0 ||
		tccdlogon() < 0
	)
			tcc_exit(1);

	TRACE1(tet_Ttcc, 1, "initdtcc(): normal RETURN");
}

/*
**	sd_start() - start a syncd
**
**	return 0 if successful or -1 on error
*/

static int sd_start()
{
	static char *argv[] = { "tetsyncd", tet_root, (char *) 0 };

	if (tet_sdptab) {
		error(0, "syncd already started", (char *) 0);
		return(-1);
	}

	if ((tet_sdptab = ti_stserver(PT_SYNCD, argv)) == (struct ptab *) 0)
		return(-1);

	return(0);
}

/*
**	xd_start() - start an xresd
**
**	return 0 if successful or -1 on error
*/

static int xd_start(savedir)
char *savedir;
{
	static char *argv[] = {
		"tetxresd",
		"-s", (char *) 0,
		tet_root,
		(char *) 0
	};

	if (tet_xdptab) {
		error(0, "xresd already started", (char *) 0);
		return(-1);
	}

	argv[2] = savedir;
	if ((tet_xdptab = ti_stserver(PT_XRESD, argv)) == (struct ptab *) 0)
		return(-1);

	return(0);
}

/*
**	ti_stserver() - start a server
**
**	return ptr to allocated ptab entry if successful,
**	(struct ptab *) 0 otherwise
*/

static struct ptab *ti_stserver(ptype, argv)
int ptype;
char **argv;
{
	register struct ptab *pp;

	/* allocate a new ptab for the server and initialise it */
	if ((pp = tet_ptalloc()) == (struct ptab *) 0)
		return(pp);
	pp->ptr_sysid = 0;
	pp->ptr_ptype = ptype;
	pp->pt_flags = PF_SERVER;

	if (ti_st2(pp, argv) < 0) {
		tet_ptfree(pp);
		return((struct ptab *) 0);
	}

	return(pp);
}

/*
**	ti_st2() - extend ti_stserver processing
**
**	return 0 if successful, -1 otherwise
*/

static int ti_st2(pp, argv)
struct ptab *pp;
char **argv;
{
	register int rc;
	register char **avp;

#ifndef NOTRACE
	/* add trace flags to the argv list */
	if ((avp = tet_traceargs(pp->ptr_ptype, argv)) == (char **) 0)
		return(-1);
#else
	avp = argv;
#endif

	/* start the server */
	rc = ts_stserver(pp, avp);

	return(rc);
}

/*
**	tccdlogon() - log on to each tccd for which we have a systab entry
**
**	return 0 if successful or -1 on error
*/

static int tccdlogon()
{
	static char fmt[] = "can't access %.*s on system";
	char msg[sizeof fmt + MAXPATH];
	register int sysid, sysmax;
	char *dir;

	for (sysid = 0, sysmax = symax(); sysid <= sysmax; sysid++)
		if (syfind(sysid) && !tet_getptbysysptype(sysid, PT_STCC) &&
			tet_tclogon(sysid) < 0) {
				error(0, "can't log on to TCCD on system",
					tet_i2a(sysid));
				return(-1);
		}

	/* send the local system environment to tccd on system 0 */
	if (syfind(0) && env2sys0() < 0)
		return(-1);

	/* check that TET_ROOT and TET_TSROOT exist on each remote system */
	for (sysid = 1, sysmax = symax(); sysid <= sysmax; sysid++)
		if (syfind(sysid)) {
			dir = getdcfg("TET_ROOT", sysid);
			ASSERT(dir && *dir);
			if (tcc_access(sysid, dir, 04) < 0) {
				(void) sprintf(msg, fmt,
					sizeof msg - sizeof fmt, dir);
				error(errno, msg, tet_i2a(sysid));
				return(-1);
			}
			dir = getdcfg("TET_TSROOT", sysid);
			ASSERT(dir && *dir);
			if (tcc_access(sysid, dir, 04) < 0) {
				(void) sprintf(msg, fmt,
					sizeof msg - sizeof fmt, dir);
				error(errno, msg, tet_i2a(sysid));
				return(-1);
			}
		}

	return(0);
}

/*
**	env2sys0() - send the local system environment to tccd on system 0
**
**	return 0 if successful or -1 on error
*/

static int env2sys0()
{
	register char **ep;
	int bad_environ, rc;
	char **envp;
	int lenv, nenv;

	/*
	** count the number of environment strings and check each one
	**
	** if the environment contains garbage (as can happen when the
	** user's shell is csh), we must first weed out the junk before
	** sending the environment to tccd, otherwise tccd complains with
	** an ER_INVAL error
	*/

	bad_environ = 0;
	for (ep = ENVIRON; *ep; ep++)
		if (tet_equindex(*ep) == (char *) 0) {
			bad_environ = 1;
			break;
		}

	if (bad_environ) {
		TRACE1(tet_Ttcc, 8,
			"env2sys0(): environment contains bad strings");
		envp = (char **) 0;
		lenv = nenv = 0;
		for (ep = ENVIRON; *ep; ep++)
			if (tet_equindex(*ep)) {
				RBUFCHK((char **) &envp, &lenv,
					(int) ((nenv + 1) * sizeof *envp));
				*(envp + nenv++) = *ep;
			}
	}
	else {
		envp = ENVIRON;
		nenv = ep - ENVIRON;
	}

	/* send the environment strings to tccd on system 0 */
	if ((rc = tet_tcputenvv(0, envp, nenv)) < 0)
		error(tet_tcerrno,
			"can't send local environment to tccd on system 0",
			(char *) 0);

	/* free up any storage allocated here */
	if (bad_environ) {
		TRACE2(tet_Tbuf, 6, "env2sys0(): free envp = %s",
			tet_i2x(envp));
		free((char *) envp);
	}
	
	return(rc);
}

/*
**	dtcc_cleanup() - log off all the servers and close the connections
*/

void dtcc_cleanup()
{
	int sysid, sysmax;

	TRACE1(tet_Ttcc, 1, "dtcc_cleanup(): log off all the servers and close the network connections");

	for (sysid = 0, sysmax = symax(); sysid <= sysmax; sysid++)
		if (syfind(sysid) && tet_getptbysysptype(sysid, PT_STCC))
			(void) tet_tclogoff(sysid);

	(void) tet_sdlogoff(0);
	(void) tet_xdlogoff();

	tet_ts_cleanup();
}

/*
**	tet_ss_dead() - server-specific dead process handler
**
**	should only be called from tet_si_service() when a server dies
**	unexpectedly
**
**	server logoff routines do not come here
*/

void tet_ss_dead(pp)
struct ptab *pp;
{
	/* emit a diagnostic if this is unexpected */
	if ((pp->pt_flags & PF_LOGGEDOFF) == 0)
		error(0, "server connection closed", tet_r2a(&pp->pt_rid));

	pp->pt_flags = (pp->pt_flags & ~PF_LOGGEDON) | PF_LOGGEDOFF;
}

/*
**	tet_ss_connect() - connect to remote process
*/

void tet_ss_connect(pp)
struct ptab *pp;
{
	tet_ts_connect(pp);
}

/*
**	tet_ss_ptalloc(), tet_ss_ptfree()  - allocate and free server-specific
**		ptab data area
**
**	tcc does not make use of server-specific data
*/

int tet_ss_ptalloc(pp)
struct ptab *pp;
{
	pp->pt_sdata = (char *) 0;
	return(0);
}

/* ARGSUSED */
void tet_ss_ptfree(pp)
struct ptab *pp;
{
	/* nothing */
}

#else	/* -END-LITE-CUT- */

int tet_dtcc_c_not_used;

#endif /* !TET_LITE */	/* -LITE-CUT-LINE- */

