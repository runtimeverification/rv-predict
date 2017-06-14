/*
 *	SCCS: @(#)tcc_xt.c	1.6 (99/09/02)
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
static char sccsid[] = "@(#)tcc_xt.c	1.6 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcc_xt.c	1.6 99/09/02 TETware release 3.8
NAME:		tcc_xt.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Ltd.
DATE CREATED:	April 1993

DESCRIPTION:
	client-specific functions for tcc XTI version

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., December 1993
	Added malloc tracing.
	Added ptm_mtype assignment.
	Moved transport-specific stuff from tcc.c, d_tccfuncs.c and
	tcc_env.h to here.

	Andrew Dingwall, UniSoft Ltd., May 1994
	clear malloc'd memory if it is not otherwise filled in

	Andrew Dingwall, UniSoft Ltd., November 1994
	updated t_alloc() structure type names in line with latest XTI spec

	Geoff Clare, UniSoft Ltd., October 1996
	Changes for TETware.
	This file is derived from d_tcc_xt.c in dTET2 R2.3.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>
#include <fcntl.h>
#include <errno.h>
#include <time.h>
#include <sys/types.h>
#include <unistd.h>
/*#include <sys/uio.h>*/
#include <xti.h>
#ifdef TCPTPI
#  include <netdb.h>
#endif
#include "dtmac.h"
#include "ltoa.h"
#include "error.h"
#include "globals.h"
#include "dtmsg.h"
#include "ptab.h"
#include "sysent.h"
#include "tptab_xt.h"
#include "tcc.h"
#include "dtcc.h"
#include "config.h"
#include "server.h"
#include "server_xt.h"
#include "xtilib_xt.h"
#include "tsinfo_xt.h"
#include "dtetlib.h"
#include "tslib.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

char *tet_tpname = "/dev/tcp";	/* Transport provider name for XTI based tcc */
int  tet_tpi_mode = -1;		/* Transport provider mode */


/* static function declarations */
static int ts_ss2 PROTOLIST((struct ptab *, char **));
static int ts_ss3 PROTOLIST((struct ptab *, char **, int));


/*
**	ts_tccinit() - transport-specific tcc initialisation
**
**	return 0 if successful or -1 on error
*/

int	ts_tccinit()
{
	register char *p;

	/*
	 * Initialise the transport provider name from the TET_XTI_TPI
	 * configuration variable. Establish "tcp" as the default transport
	 * provider.
	 */
	if ((p = getmcfg("TET_XTI_TPI", CONF_DIST)) != (char *) 0) {
		if (!*p) {
			error(0, "TET_XTI_TPI is null", (char *) 0);
			return(-1);
		}
		if ((tet_tpname = tet_strstore(p)) == (char *) 0)
			return(-1);
	}

	if ((p = getmcfg("TET_XTI_MODE", CONF_DIST)) == (char *) 0)
		p = "tcp";
	if ((tet_tpi_mode = tet_mode2i(p)) < 0) {
		error(0, "invalid TET_XTI_MODE:", p);
		return(-1);
	}

	return(0);
}

/*
**	ts_needdist() - return 1 if we need the distributed config
**		or 0 if we don't
*/

int ts_needdist()
{
	return(1);
}

/*
**	ts_stserver() - start an XTI based server
**
**	return 0 if successful or -1 on error
*/

int ts_stserver(pp, argv)
struct ptab *pp;
char **argv;
{
	register int rc;
	register char **newargv;
	static char *tsargv[] = { "-P", (char *) 0, (char *) 0 };

	/* make sure that we have a transport provider name */
	if (!tet_tpname || !*tet_tpname) {
		error(0, "no transport provider interface defined", (char *)0);
		return(-1);
	}

	/* add the transport-specific arguments to argv */
	tsargv[1] = tet_tpname;
	if ((newargv = tet_addargv(argv, tsargv)) == (char **) 0)
		return(-1);

	rc = ts_ss2(pp, newargv);

	TRACE2(tet_Tbuf, 6, "free newargv = %s", tet_i2x(newargv));
	free((char *) newargv);

	return(rc);
}

/*
**	ts_ss2() - extend the ts_stserver() processing
**
**	return 0 inf successful or -1 on error
*/

static int ts_ss2(pp, argv)
struct ptab *pp;
char **argv;
{
	register int rc, fd;

	/* get a file descriptor for the server */
	if ((fd = t_open(tet_tpname, O_RDWR, (struct t_info *) 0)) < 0) {
		xt_error(t_errno, "can't open transport provider", tet_tpname);
		return(-1);
	}

	rc = ts_ss3(pp, argv, fd);

	(void) t_close(fd);

	return(rc);
}

/*
**	ts_ss3() - extend ts_stserver() processing some more
**
**	return 0 if successful or -1 on error
*/

static int ts_ss3(pp, argv, fd)
struct ptab *pp;
char **argv;
int fd;
{
	register struct tptab *tp = (struct tptab *) pp->pt_tdata;
	register pid, rc;

	struct t_bind  req;
	struct t_bind *ret;

#ifdef TCPTPI
	struct	sockaddr_in *s;
	char	*ip;
#endif

	int status;
	char path[MAXPATH +1];

#ifndef NOTRACE
	char **ap;
#endif

	/* bind the fd to an ephemeral address */
	req.addr.maxlen = MAX_ADDRL;
	req.addr.len    = 0;
	req.addr.buf    = (char *)0;
	req.qlen        = MAX_CONN_IND;

	if ((ret = T_ALLOC_BIND(fd)) == (struct t_bind *)0) {
		xt_error(t_errno, "can't allocate T_BIND", (char *) 0);
		(void) t_close(fd);
		return(-1);
	}
	TRACE2(tet_Tbuf, 6, "t_alloc() = %s", tet_i2x(ret));

	if (t_bind(fd, &req, ret) < 0) {
		xt_error(t_errno, "can't bind to server fd", (char *)0);

		TRACE2(tet_Tbuf, 6, "t_free ret = %s", tet_i2x(ret));
		(void) t_free((char *)ret, T_BIND);
		(void) t_close(fd);
		return (-1);
	}

	/* see if an address was actually generated */
	if (ret->addr.len == 0) {
		xt_error(t_errno, "address not generated", (char *)0);
		TRACE2(tet_Tbuf, 6, "t_free ret = %s", tet_i2x(ret));
		(void) t_free((char *)ret, T_BIND);
		(void) t_close(fd);
		return (-1);
	}
		
#ifdef TCPTPI
	/* Make sure the bound address is not the `wildcard'
	   address INADDR_ANY and fill in the inet address of this machine */
	if (tet_tpi_mode == TPI_TCP) {
		if ((ip = getmcfg("TET_LOCALHOST", CONF_DIST)) == (char *) 0 || !*ip) {
			error(0, "configuration variable TET_LOCALHOST",
				"null or not set");
			return(-1);
		}
	   	s = (struct sockaddr_in *)ret->addr.buf;
	   	if (!(s->sin_addr.s_addr = tet_inetoul(ip))) {
			error(0, "TET_LOCALHOST: invalid address format", ip);
			return(-1);
		}
	}
#endif

	/* fork and exec the daemon */
	if ((pid = tet_dofork()) < 0) {
		error(errno, "can't fork", (char *) 0);
		return(-1);
	}
	else if (pid == 0) {
		/* in child */
		(void) sprintf(path, "%.*s/bin/%s",
			(int) sizeof path - (int) strlen(*argv) - 6,
			tet_root, *argv);
#ifndef NOTRACE
		TRACE3(tet_Ttcc, 2, "start server \"%s\", address = %s",
			path, tet_addr2lname(&ret->addr));
		if (tet_Ttcc)
			for (ap = argv; *ap; ap++)
				TRACE2(tet_Ttcc, 6, "argv = \"%s\"", *ap);
#endif
		/* dup the connection on to fd 0 and close all other
			files except 1 and 2 */
		(void) close(0);
		TRACE2(tet_Tbuf, 6, "t_free ret = %s", tet_i2x(ret));
		(void) t_free((char *)ret, T_BIND);

		if ((rc = fcntl(fd, F_DUPFD, 0)) != 0) {
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
		/* in parent - wait for daemon to start */
		status = 0;
		while ((rc = wait(&status)) != pid)
			if (rc < 0) {
				error(errno, "wait failed:", *argv);
				return(-1);
			}
	}

	if (status) {
		TRACE4(tet_Ttcc, 2, "%s: exit status %s, signal %s", *argv,
			tet_i2x((status >> 8) & 0xff), tet_i2a(status & 0xff));
		return(-1);
	}
	else
		TRACE2(tet_Ttcc, 8, "%s started ok", *argv);

	/* remember the server's address for later */

	tp->tp_call.maxlen = ret->addr.maxlen;
	tp->tp_call.len    = ret->addr.len;

	errno = 0;
	if ((tp->tp_call.buf = (char *) malloc(tp->tp_call.maxlen)) == (char *) 0) {
		error(errno, "can't malloc server address", (char *)0);
		TRACE2(tet_Tbuf, 6, "t_free ret = %s", tet_i2x(ret));
		(void) t_free((char *)ret, T_BIND);	
		return (-1);
	}
	TRACE2(tet_Tbuf, 6, "allocate tp_call.buf = %s",
		tet_i2x(tp->tp_call.buf));

	(void) memcpy(tp->tp_call.buf, ret->addr.buf, ret->addr.len);
	if (ret->addr.maxlen > ret->addr.len)
		(void) memset(tp->tp_call.buf + ret->addr.len, '\0',
			ret->addr.maxlen - ret->addr.len);

	(void) t_free((char *)ret, T_BIND);	
	return (0);
}

/*
**	tet_ss_tsconnect() - tcc transport-specific connect routine
**
**	return 0 if successful or -1 on error
*/

int tet_ss_tsconnect(pp)
struct ptab *pp;
{
	/* work out where the process is on the network -
		the addresses for SYNCD and XRESD were stored when they
		were started up
		TCCD address must be looked up in the DTET systems file,
		and in the hosts and services files */
	switch (pp->ptr_ptype) {
	case PT_SYNCD:
	case PT_XRESD:
		return(0);
	case PT_STCC:
		return(tet_gettccdaddr(pp));
	}

	error(0, "don't know how to connect to", tet_ptptype(pp->ptr_ptype));
	return(-1);
}

/*
**	tet_ss_tsinfo() - construct a tsinfo message relating to a
**		server process
**
**	return 0 if successful or -1 on error
*/

int tet_ss_tsinfo(pp, ptype)
struct ptab *pp;
register int ptype;
{
	register struct tptab 	*tp;
	register struct tsinfo 	*mp;

	if ((mp = (struct tsinfo *)tet_ti_msgbuf(pp, sizeof *mp)) == (struct tsinfo *) 0)
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

	switch (tet_tpi_mode) {
#ifdef TCPTPI
	case TPI_TCP:
	
		mp->ts.inet.ts_port
		    = ntohs(((struct sockaddr_in *)tp->tp_call.buf)->sin_port);
	  	mp->ts.inet.ts_addr
		    = ntohl(((struct sockaddr_in *)tp->tp_call.buf)->sin_addr.s_addr);

		break;
#endif
#ifdef OSITPI
	case TPI_OSICO:

		if (tp->tp_call.len > sizeof (mp->ts.osico.ts_nsap)) {
			error(0, "address too big for tsinfo buffer",
				(char *)0);
			return (-1);
		}

		mp->ts.osico.ts_len = tp->tp_call.len;
		(void) memcpy(mp->ts.osico.ts_nsap, tp->tp_call.buf,
			tp->tp_call.len);

		break;
#endif
	default:

		error(0, "unsupported transport mode", tet_i2a(tet_tpi_mode));
		return(-1);
	}

	pp->ptm_mtype = MT_TSINFO_XT;
	pp->ptm_len = sizeof *mp;
	return(0);
}

/*
**	ts_tsinfolen() - return length of a machine-independent tsinfo
**		structure
*/

int ts_tsinfolen()
{
	switch (tet_tpi_mode) {
#ifdef TCPTPI
	case TPI_TCP:
		return(TS_INET_TSINFOSZ);
#endif
#ifdef OSITPI
	case TPI_OSICO:
		return(TS_OSICO_TSINFOSZ);
#endif
	default:
		return (0);
	}
}

/*
**	ts_tsinfo2bs() - call tet_tsinfo2bs()
*/

int ts_tsinfo2bs(from, to)
char *from, *to;
{
	return(tet_tsinfo2bs((struct tsinfo *) from, to));
}

