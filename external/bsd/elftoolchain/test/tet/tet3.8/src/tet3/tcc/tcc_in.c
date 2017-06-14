/*
 *	SCCS: @(#)tcc_in.c	1.7 (03/08/28)
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
static char sccsid[] = "@(#)tcc_in.c	1.7 (03/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcc_in.c	1.7 03/08/28 TETware release 3.8
NAME:		tcc_in.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	client-specific functions for tcc INET version

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., December 1993
	added ptm_mtype assignment

	Andrew Dingwall, UniSoft Ltd., February 1995
	clear sockaddr_in before using it

	Andrew Dingwall, UniSoft Ltd., August 1996
	Changes for TETware.
	This file is derived from d_tcc_in.c in dTET2 R2.3.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for TET_LOCALHOST distributed configuration variable.
	(This must refer to an external interface - not "localhost"!)
	Always use localhost when connecting to tccd on the local system.

	Matthew Hails, The Open Group, August 2003
	Modified address_len argument to getsockname() to use SOCKLEN_T type.

************************************************************************/

#ifndef TET_LITE	/* -START-LITE-CUT- */

#include <stdio.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/types.h>
#include <time.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <winsock.h>
#  include <process.h>
#else		/* -END-WIN32-CUT- */
#  include <unistd.h>
#  include <sys/uio.h>
#  include <sys/socket.h>
#  include <netinet/in.h>
#  include <netdb.h>
#  include <arpa/inet.h>
#  include <sys/wait.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "ltoa.h"
#include "error.h"
#include "globals.h"
#include "dtmsg.h"
#include "ptab.h"
#include "tptab_in.h"
#include "tsinfo_in.h"
#include "bstring.h"
#include "server.h"
#include "server_in.h"
#include "inetlib_in.h"
#include "sysent.h"
#include "dtetlib.h"
#include "config.h"
#include "tslib.h"
#include "tcc.h"
#include "dtcc.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* static function declarations */
static int ts_ss2 PROTOLIST((struct ptab *, char **, SOCKET));


/*
**	ts_tccinit() - transport-specific tcc initialisation
**
**	return 0 if successful or -1 on error
*/

int ts_tccinit()
{
	/* get localhost net address */
	return(tet_getlocalhostaddr() ? 0 : -1);
}

/*
**	ts_needdist() - return 1 if we need the distributed config
**		or 0 if we don't
*/

int ts_needdist()
{
	return(0);
}

/*
**	ts_stserver() - start an INET server
**
**	return 0 if successful or -1 on error
*/

int ts_stserver(pp, argv)
struct ptab *pp;
char **argv;
{
	register int rc;
	register SOCKET sd;

	/* get a socket for the server */
	if ((sd = socket(AF_INET, SOCK_STREAM, 0)) == INVALID_SOCKET) {
		error(SOCKET_ERRNO, "can't create server socket", (char *) 0);
		return(-1);
	}

	rc = ts_ss2(pp, argv, sd);
	(void) SOCKET_CLOSE(sd);

	return(rc);
}

/*
**	ts_ss2() - extend ts_stserver() processing
**
**	return 0 if successful or -1 on error
*/

static int ts_ss2(pp, argv, sd)
struct ptab *pp;
char **argv;
SOCKET sd;
{
	register struct tptab *tp = (struct tptab *) pp->pt_tdata;
	register int fd, pid, rc;
	struct sockaddr_in sin;
	SOCKLEN_T len;
	int status;
	char path[MAXPATH];
#ifdef _WIN32	/* -START-WIN32-CUT- */
	static char *larg[] = { "-l", (char *) 0, (char *) 0 };
	char **newargv;
#endif		/* -END-WIN32-CUT- */
#ifndef NOTRACE
	char **ap;
#endif

	/* bind the socket to an ephemeral port */
	bzero((char *) &sin, sizeof sin);
	sin.sin_family = AF_INET;
	sin.sin_addr.s_addr = INADDR_ANY;
	sin.sin_port = 0;
	if (bind(sd, (struct sockaddr *) &sin, sizeof sin) == SOCKET_ERROR) {
		error(SOCKET_ERRNO, "can't bind to server socket", (char *) 0);
		return(-1);
	}

	/* determine the port number */
	len = (SOCKLEN_T)(sizeof sin);
	if (getsockname(sd, (struct sockaddr *) &sin, &len) < 0) {
		error(SOCKET_ERRNO, "can't get server socket name", (char *) 0);
		return(-1);
	}

	/*
	** on UNIX systems we fork and exec the server in the child;
	** the listen socket is passed as standard input to the server;
	** on startup the server immediately forks and the main path exit
	** thus spawning the daemon
	**
	** on WIN32 systems we spawn the daemon directly;
	** the value of the listen socket's handle is passed to the daemon
	** using the -l command-line option
	*/
	(void) sprintf(path, "%.*s/bin/%s",
		(int) sizeof path - (int) strlen(*argv) - 6,
		tet_root, *argv);
#ifdef _WIN32	/* -START-WIN32-CUT- */
	/* add in the listen socket argument */
	larg[1] = tet_i2a(sd);
	newargv = tet_addargv(argv, larg);
	argv = newargv;
#endif		/* -END-WIN32-CUT- */

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	/* fork and exec the daemon */
	if ((pid = tet_dofork()) < 0) {
		error(errno, "can't fork", (char *) 0);
		return(-1);
	}
	else if (pid == 0) {
		/* in child on UNIX systems */
#endif		/* -WIN32-CUT-LINE- */

#ifndef NOTRACE
		TRACE3(tet_Ttcc, 2, "start server \"%s\", port = %s",
			path, tet_i2a(ntohs(sin.sin_port)));
		if (tet_Ttcc) {
			for (ap = argv; *ap; ap++) {
				TRACE2(tet_Ttcc, 6, "argv = \"%s\"", *ap);
			}
		}
#endif /* !NOTRACE */

#ifdef _WIN32		/* -START-WIN32-CUT- */
		pid = _spawnv(_P_NOWAIT, path, argv);
#else /* _WIN32 */	/* -END-WIN32-CUT- */

		/*
		** still in child on UNIX systems -
		** dup the socket on to fd 0 and close all other files
		** except 1 and 2
		*/
		(void) close(0);
		if ((rc = fcntl(sd, F_DUPFD, 0)) != 0) {
			error(errno, "server socket: fcntl(F_DUPFD) returned",
				tet_i2a(rc));
			_exit(~0);
		}
		for (fd = tet_getdtablesize() - 1; fd > 2; fd--)
			(void) close(fd);
		(void) execv(path, argv);
		error(errno, "can't exec", path);
		_exit(~0);
	}
	else {
		/* in parent on UNIX systems - wait for daemon to start */
		status = 0;
		while ((rc = wait(&status)) != pid)
			if (rc < 0) {
				error(errno, "wait failed:", *argv);
				return(-1);
			}
	}

#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */

#ifdef _WIN32		/* -START-WIN32-CUT- */
	TRACE2(tet_Tbuf, 6, "free newargv = %s", tet_i2x(newargv));
	free((char *) newargv);
	if (pid == -1) {
		error(errno, "can't exec", path);
#else			/* -END-WIN32-CUT- */
	if (status) {
		TRACE4(tet_Ttcc, 2, "%s: exit status %s, signal %s", path,
			tet_i2x((status >> 8) & 0xff), tet_i2a(status & 0xff));
#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */
		return(-1);
	}
	else
		TRACE2(tet_Ttcc, 8, "%s started ok", path);

	/* remember the server's inet address and port no for later */
	tp->tp_sin = sin;
	tp->tp_sin.sin_addr = *tet_getlocalhostaddr();

	return(0);
}

/*
**	tet_ss_tsconnect() - tcc transport-specific connect routine
**
**	return 0 if successful or -1 on error
*/

int tet_ss_tsconnect(pp)
struct ptab *pp;
{
	/*
	** work out where the process is on the network -
	**	the addresses for SYNCD and XRESD were stored when they
	**	were started up
	**	TCCD address must be looked up in the DTET systems file,
	**	and in the hosts and services files
	*/
	switch (pp->ptr_ptype) {
	case PT_SYNCD:
	case PT_XRESD:
		return(0);
	case PT_STCC:
		if (tet_gettccdaddr(pp) < 0)
			return(-1);
		if (pp->ptr_sysid == 0)
			((struct tptab *) pp->pt_tdata)->tp_sin.sin_addr = *tet_getlocalhostaddr();
		return(0);
	}

	error(0, "don't know how to connect to", tet_ptptype(pp->ptr_ptype));
	return(-1);
}

/*
**	tet_ss_tsinfo() - construct a tsinfo message relating to a
**		server process
**
**	this message tells processes (on this machine or another machine)
**	how to connect to servers running on this machine
**
**	return 0 if successful or -1 on error
*/

int tet_ss_tsinfo(pp, ptype)
struct ptab *pp;
register int ptype;
{
	register struct tptab *tp;
	register struct tsinfo *mp;
	struct in_addr addr, *ap;
	struct sysent *sp;
	char hostname[SNAMELEN];
	char *p;

	if ((mp = (struct tsinfo *) tet_ti_msgbuf(pp, sizeof *mp)) == (struct tsinfo *) 0)
		return(-1);

	/* make tp point to the tptab for the server */
	switch (ptype) {
	case PT_SYNCD:
		if (!tet_sdptab) {
			error(0, "must start syncd first", (char *) 0);
			return(-1);
		}
		tp = (struct tptab *) tet_sdptab->pt_tdata;
		break;
	case PT_XRESD:
		if (!tet_xdptab) {
			error(0, "must start xresd first", (char *) 0);
			return(-1);
		}
		tp = (struct tptab *) tet_xdptab->pt_tdata;
		break;
	default:
		error(0, "no tsinfo for", tet_ptptype(ptype));
		return(-1);
	}

	/* all ok so copy over the data and return */
	mp->ts_ptype = ptype;
	mp->ts_port = ntohs(tp->tp_sin.sin_port);

	/*
	** we talk to syncd and xresd using the loopback address, so that
	** is what is stored in the tptab structure;
	** if the message destination is also localhost:
	**	simply copy the stored (localhost) address
	** otherwise:
	**	(the destination is on another machine)
	**	if TET_LOCALHOST is defined in the master distributed
	**	configuration:
	**		use the address specified by TET_LOCALHOST
	**	otherwise:
	**		if there is a systems file entry for the local system:
	**			use the address specified in the systems file
	**		otherwise:
	**			find our (external) Internet address
	**			and use that
	*/
	if (((struct tptab *) pp->pt_tdata)->tp_sin.sin_addr.s_addr == tp->tp_sin.sin_addr.s_addr)
		mp->ts_addr = ntohl(tp->tp_sin.sin_addr.s_addr);
	else {
		p = getmcfg("TET_LOCALHOST", CONF_DIST);
		if (!p || !*p) {
			if ((sp = tet_libgetsysbyid(0)) != (struct sysent *) 0)
				p = sp->sy_name;
		}
		if (p && *p) {
			if ((addr.s_addr = inet_addr(p)) != -1)
				ap = &addr;
			else if ((ap = tet_gethostaddr(p)) == (struct in_addr *) 0)
				return(-1);
		}
		else {
			if (gethostname(hostname, sizeof hostname) < 0) {
				error(errno, "gethostname() failed", (char *) 0);
				return(-1);
			}
			if ((ap = tet_gethostaddr(hostname)) == (struct in_addr *) 0)
				return(-1);
		}
		mp->ts_addr = ntohl(ap->s_addr);
	}

	pp->ptm_mtype = MT_TSINFO_IN;
	pp->ptm_len = sizeof *mp;
	return(0);
}

/*
**	ts_tsinfolen() - return length of a machine-independent tsinfo
**		structure
*/

int ts_tsinfolen()
{
	return(TS_TSINFOSZ);
}

/*
**	ts_tsinfo2bs() - call tet_tsinfo2bs()
*/

int ts_tsinfo2bs(from, to)
char *from, *to;
{
	return(tet_tsinfo2bs((struct tsinfo *) from, to));
}

#else	/* -END-LITE-CUT- */

int tet_tcc_in_c_not_used;

#endif /* !TET_LITE */	/* -LITE-CUT-LINE- */

