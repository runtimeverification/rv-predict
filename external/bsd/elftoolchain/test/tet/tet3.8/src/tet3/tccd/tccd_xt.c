/*
 *      SCCS:  @(#)tccd_xt.c	1.12 (02/01/24) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1993 X/Open Company Limited
 * (C) Copyright 1994 UniSoft Limited
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
static char sccsid[] = "@(#)tccd_xt.c	1.12 (02/01/24) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tccd_xt.c	1.12 02/01/24 TETware release 3.8
NAME:		tccd_xt.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Ltd.
DATE CREATED:	May 1993

DESCRIPTION:
	server-specific functions for tccd XTI version

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., November 1993
	added ptm_mtype assignment
	added malloc tracing

	Andrew Dingwall, UniSoft Ltd., May 1994
	clear malloc'd memory when not all of it will be filled in

	Andrew Dingwall, UniSoft Ltd., November 1994
	updated t_alloc() structure type names in line with latest XTI spec

	Andrew Dingwall, UniSoft Ltd., October 1996
	changes for TETware

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
	Andrew Dingwall, UniSoft Ltd., August 1999
	check for missing option arguments

	Andrew Dingwall, The Open Group, January 2002
	Updated to align with UNIX 2003.


************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/types.h>
#include <time.h>
#include <signal.h>
#include <sys/wait.h>
#include <unistd.h>
#ifdef TCPTPI
#  include <netdb.h>
#endif
#include <xti.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "tptab_xt.h"
#include "error.h"
#include "globals.h"
#include "ltoa.h"
#include "tccd.h"
#include "tccd_bs.h"
#include "server.h"
#include "xtilib_xt.h"
#include "tsinfo_xt.h"
#include "servlib.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

char *tet_tpname = "/dev/tcp";		/* transport interface provider name */
int tet_tpi_mode = -1;			/* transport provider mode */

int tet_listen_fd;			/* listen file descriptor */
struct netbuf *Tccdaddr;		/* TCCD address */
struct t_call *tet_calls[MAX_CONN_IND];	/* to hold connection indications */


/* static function declarations */
static SIG_FUNC_T waitchild PROTOLIST((int));


/*
**	ss_tsargproc() - transport-specific tccd command-line argument
**		processing
**
**	return 0 if only firstarg was used or 1 if both args were used
*/

int ss_tsargproc(firstarg, nextarg)
char *firstarg, *nextarg;
{
	register int rc = 0;
	char *mode;
	static struct netbuf np;	/* XTI TCCD address */
	register struct netbuf *p;
	static char optmsg[] = "option requires an argument";

	switch (*(firstarg + 1)) {
	case 'M':
		if (*(firstarg+2))
			mode = firstarg+2;
		else {
			if (!nextarg)
				fatal(0, "-M", optmsg);
			mode = nextarg;
			rc = 1;
		}
		if ((tet_tpi_mode = tet_mode2i(mode)) < 0)
			fatal(0, "invalid TPI mode (-M switch)", mode);
		
		return (rc);
		break;

	case 'p':
		if (*(firstarg+2)) {
			if ((p = tet_lname2addr(firstarg+2)) == (struct netbuf *)0)
				fatal(0, "invalid -p TCCD address",
					firstarg + 2);
	
		}
		else {
			if (!nextarg)
				fatal(0, "-p", optmsg);
			if ((p = tet_lname2addr(nextarg)) == (struct netbuf *)0)
				fatal(0, "invalid -p TCCD address", nextarg);

			rc = 1;
		}
		errno = 0;
		if ((np.buf = (char *) malloc(p->maxlen)) == (char *) 0)
			fatal(errno, "can't malloc TCCD address buffer",
				(char *) 0);
		TRACE2(tet_Tbuf, 6, "allocate np.buf = %s", tet_i2x(np.buf));

		/* all ok - copy static data to safer place */
		np.maxlen	= p->maxlen;
		np.len		= p->len;
		(void) memcpy(np.buf, p->buf, p->len);
		if (np.maxlen > np.len)
			(void) memset(np.buf + np.len, '\0',
				np.maxlen - np.len);
	
		Tccdaddr = &np;
		break;

	case 'P':
		if (*(firstarg + 2))
			tet_tpname = firstarg + 2;
		else {
			if (!nextarg)
				fatal(0, "-P", optmsg);
			tet_tpname = nextarg;
			rc = 1;
		}
                break;

	default:
		fatal(0, "unknown option", firstarg);
		/* NOTREACHED */
	}
	return (rc);
}

/*
**	ss_tsinitb4fork() - tccd XTI initialisation before forking a daemon
*/

void ss_tsinitb4fork()
{

	register int fd;
	struct t_bind  req;
	struct t_bind *ret;
	struct sigaction sa;

	/* make sure a tccd address has been specified */
	if (!Tccdaddr)
		xt_fatal(0, "tccd transport address (-p option) not specified",
				(char *)0);

	/* make sure a transport mode has been specified */
	if (tet_tpi_mode < 0) {
		tet_tpi_mode = tet_mode2i("tcp");
		error(0, "transport mode defaulting to \"tcp\"", (char *)0);
	}

	/* get a descriptor to listen on */
	if (!tet_tpname || !*tet_tpname)
		fatal(0, "no transport provider interface defined", (char *)0);
	
	if ((fd = t_open(tet_tpname, O_RDWR, (struct t_info *)0)) < 0)
		xt_fatal(t_errno, "can't open transport provider", tet_tpname);


	/* make sure the listen fd is not stdin, stdout or stderr */
	if (fd < 3) {
		errno = 0;
		if ((tet_listen_fd = fcntl(fd, F_DUPFD, 3)) < 3)
			fatal(errno, "can't dup listen fd", (char *) 0);
		(void) close(fd);
	}
	else
		tet_listen_fd = fd;

	/* allocate the bind request and return structures */
	if ((ret = T_ALLOC_BIND(fd)) == (struct t_bind *)0)
		xt_fatal(t_errno, "can't allocate T_BIND", (char *)0);
	TRACE2(tet_Tbuf, 6, "t_alloc() = %s", tet_i2x(ret));
	
	req.qlen	= MAX_CONN_IND;
	req.addr.maxlen	= Tccdaddr->maxlen;
	req.addr.len	= Tccdaddr->len;
	req.addr.buf	= Tccdaddr->buf;

	
	/* bind the file descriptor to the tccd address */
	if (t_bind(tet_listen_fd, &req, ret) < 0)
		xt_fatal(t_errno, "can't bind listen fd",
			tet_i2a(tet_listen_fd));

	/* make sure the address actually bound was the one we want */
	if (!SAME_XTIADDR(&req, ret))
		xt_fatal(0, "can't bind tccd address",
			tet_addr2lname(Tccdaddr));

	if (ret->qlen == 0)
		xt_fatal(0,"bound address can't accept connections", (char *)0);

	/* arrange to accept incoming connections */
	tet_ts_listen(tet_listen_fd);

	/* arrange to reap child daemon processes */
	sa.sa_handler = waitchild;
	sa.sa_flags = 0;
	(void) sigemptyset(&sa.sa_mask);
	(void) sigaction(SIGCHLD, &sa, (struct sigaction *) 0);
}

/*
**	ts_forkdaemon() - start a daemon process
*/

void ts_forkdaemon()
{

	tet_si_forkdaemon();
	
}

/*
**	ts_logstart() - make a log file entry when the master daemon starts
*/

void ts_logstart()
{

	logent("START", (char *) 0);

}

/*
**	tet_ss_tsconnect() - server-specific connect processing
**
**	return 0 if successful or -1 on error
*/

int tet_ss_tsconnect(pp)
struct ptab *pp;
{
	/* syncd and xresd addresses have alread been stored when
		op_tsinfo() was called (which also allocated the ptabs) -
		if this had not happened, we would not have got past
		tet_ti_logon() */
	switch (pp->ptr_ptype) {
	case PT_SYNCD:
	case PT_XRESD:
		return(0);
	}

	error(0, "don't know how to connect to", tet_ptptype(pp->ptr_ptype));
	return(-1);
}

/*
**	tet_ss_tsaccept() - server-specific accept() processing
*/

void tet_ss_tsaccept()
{

	/* accept the connection unless we are closing down */
	if (tet_listen_fd >= 0)
		tet_ts_accept(tet_listen_fd);

}

/*
**	tet_ss_tsafteraccept() - server-specific things to do after an accept()
**
**	return 0 if successful or -1 on error
*/

int tet_ss_tsafteraccept(pp)
struct ptab *pp;
{
	register struct tptab *tp = (struct tptab *) pp->pt_tdata;
	register char *p;
	register int pid;
	struct sigaction sa;

	/* log the connection */
	logent("connection received from", tet_addr2lname(&tp->tp_call));


	if ((pid = tet_dofork()) < 0) {
		error(errno, "can't fork", (char *) 0);
		return(-1);
	}
	else if (!pid) {
		/* in child */
		tet_mypid = getpid();
		(void) close(tet_listen_fd);
		tet_listen_fd = -1;
		sa.sa_handler = SIG_DFL;
		sa.sa_flags = 0;
		(void) sigemptyset(&sa.sa_mask);
		(void) sigaction(SIGCHLD, &sa, (struct sigaction *) 0);
	}
	else {
		/* in parent - return -1 to close the connection and
			free the ptab entry */
		return(-1);
	}


	return(0);
}

/*
**	ss_tslogon() - make sure that we want to accept a logon request
**		from the connected remote host
**
**	return ER_OK if successful or other ER_* error code on error
*/

int ss_tslogon()
{
	/* the XTI version of tccd does not use the systems.equiv file */
	return(ER_OK);
}

/*
**	waitchild() - reap a child daemon
*/


/* ARGSUSED */
static SIG_FUNC_T waitchild(sig)
int sig;
{
	int status;
	struct sigaction sa, oldsa;
	int oldsa_valid = 0;
#ifndef NOTRACE
	register int pid;
#endif

	sa.sa_handler = SIG_DFL;
	sa.sa_flags = 0;
	(void) sigemptyset(&sa.sa_mask);
	if (sigaction(SIGCHLD, &sa, &oldsa) == 0)
		oldsa_valid = 1;

#ifdef NOTRACE
	while (tet_dowait3(&status, WNOHANG) > 0)
		;
#else
	while ((pid = tet_dowait3(&status, WNOHANG)) > 0)
		TRACE4(tet_Ttccd, 2,
			"waitchild reaped %s, status %s, signal %s",
			tet_i2a(pid), tet_i2a((status >> 8) & 0xff),
			tet_i2a(status & 0xff));
#endif

	if (oldsa_valid)
		(void) sigaction(SIGCHLD, &oldsa, (struct sigaction *) 0);
}


/*
**	ts_bs2tsinfo() - call tet_bs2tsinfo()
*/

int ts_bs2tsinfo(from, fromlen, to, tolen)
char *from;
int fromlen;
char **to;
int *tolen;
{
	return(tet_bs2tsinfo(from, fromlen, (struct tsinfo **) to, tolen));
}

/*
**	op_tsinfo() - receive transport-specific data
*/

void op_tsinfo(pp)
register struct ptab *pp;
{
	register struct tsinfo *mp = (struct tsinfo *) pp->ptm_data;
	register struct netbuf *ap;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* decide where to store the data;
		allocate a ptab entry for the server if necessary */
	switch (mp->ts_ptype) {
	case PT_SYNCD:
		if (!tet_sdptab) {
			if ((tet_sdptab = tet_ptalloc()) == (struct ptab *) 0) {
				pp->ptm_rc = ER_ERR;
				return;
			}
			tet_sdptab->ptr_sysid = 0;
			tet_sdptab->ptr_ptype = PT_SYNCD;
			tet_sdptab->pt_flags = PF_SERVER;
		}
		ap = &((struct tptab *) tet_sdptab->pt_tdata)->tp_call;
		break;
	case PT_XRESD:
		if (!tet_xdptab) {
			if ((tet_xdptab = tet_ptalloc()) == (struct ptab *) 0) {
				pp->ptm_rc = ER_ERR;
				return;
			}
			tet_xdptab->ptr_sysid = 0;
			tet_xdptab->ptr_ptype = PT_XRESD;
			tet_xdptab->pt_flags = PF_SERVER;
		}
		ap = &((struct tptab *) tet_xdptab->pt_tdata)->tp_call;
		break;
	default:
		error(0, "received tsinfo for unexpected ptype",
			tet_ptptype(mp->ts_ptype));
		pp->ptm_rc = ER_ERR;
		return;
	}

	/* store the data */
	switch(tet_tpi_mode) {
#ifdef TCPTPI
	case TPI_TCP:

		errno = 0;
		if ((ap->buf = (char *) malloc(sizeof (struct sockaddr_in))) == (char *) 0) {
			error(errno, "can't get buffer for tsinfo",
				(char *) 0);
			pp->ptm_rc = ER_ERR;
			return;
		}
		TRACE2(tet_Tbuf, 6, "allocate TCP tsinfo buffer = %s",
			tet_i2x(ap->buf));
		(void) memset((char *) ap->buf, '\0',
			sizeof (struct sockaddr_in));

		((struct sockaddr_in *)ap->buf)->sin_family
			= AF_INET;
		((struct sockaddr_in *)ap->buf)->sin_addr.s_addr
			= htonl(mp->ts.inet.ts_addr);
		((struct sockaddr_in *)ap->buf)->sin_port
			= htons(mp->ts.inet.ts_port);
	
		ap->maxlen  = sizeof (struct sockaddr_in);
		ap->len     = sizeof (struct sockaddr_in);

		TRACE4(tet_Ttccd, 2,
			"received tsinfo for %s: len  = %s, addr = %s",
			tet_ptptype(mp->ts_ptype), tet_i2a(ap->len),
			tet_addr2lname(ap));
		break;
#endif
#ifdef OSITPI
	case TPI_OSICO:

		ap->maxlen = mp->ts.osico.ts_len;
		ap->len	   = mp->ts.osico.ts_len;

		errno = 0;
		if ((ap->buf = (char *) malloc(ap->len)) == (char *) 0) {
			error(errno, "can't get buffer for tsinfo",
				(char *) 0);
			pp->ptm_rc = ER_ERR;
			return;
		}
		TRACE2(tet_Tbuf, 6, "allocate OSICO tsinfo buffer = %s",
			tet_i2x(ap->buf));
		(void) memcpy(ap->buf, mp->ts.osico.ts_nsap, ap->len);

		TRACE4(tet_Ttccd, 2,
			"received tsinfo for %s: len  = %s, addr = %s",
			tet_ptptype(mp->ts_ptype), tet_i2a(ap->len),
			tet_addr2lname(ap));
		break;
#endif
	default:
	
		error(0, "received tsinfo for invalid tpi mode",
				tet_i2a(tet_tpi_mode));
		pp->ptm_rc = ER_ERR;
		return;

	}
	/* all ok so set up the reply message and return */
	pp->ptm_rc = ER_OK;
}

