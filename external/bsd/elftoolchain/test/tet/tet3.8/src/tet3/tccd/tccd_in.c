/*
 *      SCCS:  @(#)tccd_in.c	1.21 (05/07/13) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1992 X/Open Company Limited
 * (C) Copyright 1994 UniSoft Limited
 * (C) Copyright 2002-2005 The Open Group
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
static char sccsid[] = "@(#)tccd_in.c	1.21 (05/07/13) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tccd_in.c	1.21 05/07/13 TETware release 3.8
NAME:		tccd_in.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	server-specific functions for tccd INET version

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., November 1993
	enhancements for FIFO transport interface

	Andrew Dingwall, UniSoft Ltd., February 1995
	clear sockaddr_in before using it

	Geoff Clare, UniSoft Ltd., August 1996
	Missing <unistd.h>.

	Andrew Dingwall, UniSoft Ltd., March 1997
	remove #ifndef __hpux from #include <arpa/inet.h>
	since current HP-UX implementations now have this file

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
	Changes to conform to UNIX98.
 
	Andrew Dingwall, UniSoft Ltd., August 1999
	check for missing option arguments

	Andrew Dingwall, UniSoft Ltd., November 2000
	Accept unqualified host names in the systems.equiv file.

	Andrew Dingwall, The Open Group, January 2002
	Updated to align with UNIX 2003.

	Matthew Hails, The Open Group, August 2003
	Modified address_len argument to getpeername() to use SOCKLEN_T type.

	Geoff Clare, The Open Group, September 2003
	Always allow connections from "localhost".

	Geoff Clare, The Open Group, July 2005
	Added support for domain names (.my.domain) and subnets (1.2.3.0/24)

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/types.h>
#include <time.h>
#ifdef _WIN32		/* -START-WIN32-CUT- */
#  include <winsock.h>
#else			/* -END-WIN32-CUT- */
#  include <unistd.h>
#  include <netinet/in.h>
#  include <sys/uio.h>
#  include <sys/socket.h>
#  include <netdb.h>
#  include <arpa/inet.h>
#  ifndef INETD
#    include <signal.h>
#    include <sys/wait.h>
#  endif /* !INETD */
#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "tptab_in.h"
#include "tsinfo_in.h"
#include "error.h"
#include "globals.h"
#include "ltoa.h"
#include "tccd.h"
#include "tccd_bs.h"
#include "bstring.h"
#include "server.h"
#include "server_in.h"
#include "inetlib_in.h"
#include "servlib.h"
#include "dtetlib.h"

/* only the INETD version on supported on WIN32 platforms */
#if defined(_WIN32) && !defined(INETD)	/* -START-WIN32-CUT- */
#  define INETD
#endif					/* -END-WIN32-CUT- */

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

static unsigned long raddr;		/* remote host address */
static char rhostname[256];		/* remote host name */

#ifdef INETD
SOCKET tet_listen_sd = INVALID_SOCKET;	/* inetd listens on the socket for us */
#else
SOCKET tet_listen_sd;			/* socket on which to listen */
int Tccdport = 0;			/* tccd well-known port number */
#endif

#ifdef _WIN32	/* -START-WIN32-CUT- */
/* socket handle of incoming connection from the command line */
SOCKET Connect_sd = INVALID_SOCKET;
#endif		/* -END-WIN32-CUT- */


/* static function declarations */
#ifndef INETD
static SIG_FUNC_T waitchild PROTOLIST((int));
#endif /* !INETD */


/*
**	ss_tsargproc() - transport-specific tccd command-line argument
**		processing
**
**	return 0 if only firstarg was used or 1 if both args were used
*/

#ifdef INETD
#  ifndef _WIN32	/* -WIN32-CUT-LINE- */
/* ARGSUSED */
#  endif		/* -WIN32-CUT-LINE- */
#endif
int ss_tsargproc(firstarg, nextarg)
char *firstarg, *nextarg;
{
	register int rc = 0;
#if !defined(INETD) || defined(_WIN32)
	static char optmsg[] = "option needs an argument";
#endif

	switch (*(firstarg + 1)) {
#ifndef INETD
	case 'p':
		if (*(firstarg + 2))
			Tccdport = atoi(firstarg + 2);
		else {
			if (!nextarg)
				fatal(0, "-p", optmsg);
			Tccdport = atoi(nextarg);
			rc = 1;
		}
		if (Tccdport <= 0)
			fatal(0, "bad port number:",
				rc == 1 ? nextarg : firstarg + 2);
		break;
#endif /* !INETD */
#ifdef _WIN32		/* -START-WIN32-CUT- */
	case 's':
		if (*(firstarg + 2))
			Connect_sd = atoi(firstarg + 2);
		else {
			if (!nextarg)
				fatal(0, "-s", optmsg);
			Connect_sd = atoi(nextarg);
			rc = 1;
		}
		if (Connect_sd <= 0)
			fatal(0, "bad incoming socket number:",
				rc == 1 ? nextarg : firstarg + 2);
		break;
#endif /* _WIN32 */	/* -END-WIN32-CUT- */
	default:
		fatal(0, "unknown option", firstarg);
		/* NOTREACHED */
	}

	return(rc);
}

/*
**	ss_tsinitb4fork() - tccd INET initialisation before forking a daemon
*/

void ss_tsinitb4fork()
{
#ifdef INETD

	register struct ptab *pp;
	register struct tptab *tp;
	register SOCKET sd;
	SOCKLEN_T len;

#  ifdef _WIN32		/* -START-WIN32-CUT- */

	if (Connect_sd == INVALID_SOCKET)
		fatal(0, "-s option must be specified", (char *) 0);
	sd = Connect_sd;

#  else /* _WIN32 */	/* -END-WIN32-CUT- */

	/* move the incoming request socket off stdin */
	errno = 0;
	if ((sd = fcntl(0, F_DUPFD, 3)) < 3)
		fatal(errno, "can't dup stdin socket", (char *) 0);
	(void) close(0);

#  endif /* _WIN32 */	/* -WIN32-CUT-LINE- */

	/* allocate a ptab entry for the incoming request */
	if ((pp = tet_ptalloc()) == (struct ptab *) 0)
		exit(1);
	tp = (struct tptab *) pp->pt_tdata;
	tp->tp_sd = sd;

	/* get remote address and store it */
	len = (SOCKLEN_T)(sizeof tp->tp_sin);
	if (getpeername(tp->tp_sd, (struct sockaddr *) &tp->tp_sin, &len) == SOCKET_ERROR)
		fatal(SOCKET_ERRNO, "can't get name of connected peer",
			(char *) 0);

	/* log the connection */
	(void) tet_ss_tsafteraccept(pp);

	/* register the ptab entry */
	tet_ss_newptab(pp);

	/* prepare to receive the message */
	pp->pt_state = PS_RCVMSG;
	pp->pt_flags |= PF_ATTENTION;

#else /* INETD */

	struct sockaddr_in sin;
	register int sd;
	struct sigaction sa;

	/* get the tccd port number if not overridden on the cmd line */
	bzero((char *) &sin, sizeof sin);
	if (Tccdport <= 0 && (Tccdport = tet_gettccdport()) < 0)
		exit(1);
	else
		sin.sin_port = htons((unsigned short) Tccdport);

	/* get a socket to listen on */
	if ((sd = socket(AF_INET, SOCK_STREAM, 0)) < 0)
		fatal(errno, "can't create listen socket", (char *) 0);

	/* make sure that listen socket is not stdin, stdout or stderr */
	if (sd < 3) {
		errno = 0;
		if ((tet_listen_sd = fcntl(sd, F_DUPFD, 3)) < 3)
			fatal(errno, "can't dup listen socket", (char *) 0);
		(void) close(sd);
	}
	else
		tet_listen_sd = sd;

	/* bind the socket to the tccd port */
	sin.sin_family = AF_INET;
	sin.sin_addr.s_addr = INADDR_ANY;
	if (bind(tet_listen_sd, (struct sockaddr *) &sin, sizeof sin) < 0)
		fatal(errno, "can't bind to listen socket", (char *) 0);

	/* arrange to accept incoming connections */
	tet_ts_listen(tet_listen_sd);

	/* arrange to reap child daemon processes */
	sa.sa_handler = waitchild;
	sa.sa_flags = 0;
	(void) sigemptyset(&sa.sa_mask);
	(void) sigaction(SIGCHLD, &sa, (struct sigaction *) 0);

#endif /* INETD */
}

/*
**	ts_forkdaemon() - start a daemon process
*/

#ifndef INITTAB
void ts_forkdaemon()
{
#ifdef INETD

	/* inetd has already done it for us */

#else /* INETD */

	tet_si_forkdaemon();

#endif /* INETD */
}
#endif /* !INITTAB */

/*
**	ts_logstart() - make a log file entry when the master daemon starts
*/

void ts_logstart()
{
#ifdef INETD

	/* no master daemon */

#else

	logent("START", (char *) 0);

#endif
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
#ifdef INETD

	/* inetd has already accepted the connection */

#else

	/* accept the connection unless we are closing down */
	if (tet_listen_sd != INVALID_SOCKET)
		tet_ts_accept(tet_listen_sd);

#endif
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
	register struct hostent *hp;
	register char *p;

#ifndef INETD
	register int pid;
	struct sigaction sa;
#endif

	raddr = tp->tp_sin.sin_addr.s_addr;

	/* log the connection */
	if ((hp = gethostbyaddr((char *) &tp->tp_sin.sin_addr, sizeof tp->tp_sin.sin_addr, tp->tp_sin.sin_family)) != (struct hostent *) 0)
		p = hp->h_name;
	else
		p = inet_ntoa(tp->tp_sin.sin_addr);
	(void) sprintf(rhostname, "%.*s", (int) sizeof rhostname - 1, p);
	logent("connection received from", rhostname);

#ifdef INETD

	/* inetd does the accept for us */
	pp->pt_flags |= PF_CONNECTED;

#else

	if ((pid = tet_dofork()) < 0) {
		error(errno, "can't fork", (char *) 0);
		return(-1);
	}
	else if (!pid) {
		/* in child */
		tet_mypid = getpid();
		(void) close(tet_listen_sd);
		tet_listen_sd = INVALID_SOCKET;
		sa.sa_handler = SIG_DFL;
		sa.sa_flags = 0;
		(void) sigemptyset(&sa.sa_mask);
		(void) sigaction(SIGCHLD, &sa, (struct sigaction *) 0);
	}
	else {
		/* in parent - return -1 to close the socket and
			free the ptab entry */
		return(-1);
	}

#endif /* INETD */

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
	static char equiv[] = "systems.equiv";
	char fname[MAXPATH + 1];
	char *argv[1];
	register int argc, rc;
	FILE *fp;
	size_t len;
	struct hostent *hp;

	/* Connections from "localhost" are always allowed */
	if (strcmp(rhostname, "localhost") == 0)
		return ER_OK;

	ASSERT(tet_root[0]);
	(void) sprintf(fname, "%.*s/%s",
		(int) sizeof fname - (int) sizeof equiv - 1,
		tet_root, equiv);

	/* open the systems.equiv file */
	if ((fp = fopen(fname, "r")) == NULL) {
		error(errno, "can't open", fname);
		return(ER_ERR);
	}

	/* look for the requesting system in the systems.equiv file */

	rc = ER_ERR;
	while ((argc = tet_fgetargs(fp, argv, 1)) != EOF) {
		if (argc < 1)
			continue;

		if (argv[0][0] >= '0' && argv[0][0] <= '9') {
			/* systems.equiv entry may be an IP
			   address or subnet */

			char *sp = strchr(argv[0], '/');
			if (sp == NULL) {
				/* plain IP address */
				if (inet_addr(argv[0]) == raddr) {
					rc = ER_OK;
					break;
				}
			}
			else if ((len = atoi(sp+1)) > 0 && len <= 32) {
				/* subnet of the form a.b.c.d/n */
				unsigned long subnet, mask = 1;
				*sp = '\0';
				subnet = inet_addr(argv[0]);
				*sp = '/';
				mask = (mask << (32-len)) - 1;
				if (subnet != -1 &&
				    (ntohl(raddr) & ~mask) == ntohl(subnet)) {
					rc = ER_OK;
					break;
				}
			}
		}

		if (argv[0][0] == '.' || strchr(argv[0], '/') == NULL) {
			/* systems.equiv entry may be a host or domain name */
			if (argv[0][0] != '.') {
				/* host name */
				if (!strcmp(argv[0], rhostname)) {
					rc = ER_OK;
					break;
				}
				if (strchr(argv[0], '.') == NULL) {
					/* host name with no domain part */
					len = strlen(argv[0]);
					if (len < strlen(rhostname) &&
					    !strncmp(argv[0], rhostname, len) &&
					    rhostname[len] == '.' &&
					    (hp = gethostbyname(argv[0])) &&
					    !strcmp(hp->h_name, rhostname)) {
						rc = ER_OK;
						break;
					}

				}
			}
			else {
				/* domain name (e.g. ".opengroup.org")
				   - allow all hosts in the domain */
				size_t rlen = strlen(rhostname);
				len = strlen(argv[0]);
				if (rlen > len &&
				    !strcmp(argv[0], &rhostname[rlen-len])) {
					rc = ER_OK;
					break;
				}
			}
		}
	}

	if (rc != ER_OK)
		error(0, "refused login request from", rhostname);
	else
		logent("access allowed by systems.equiv entry:", argv[0]);

	(void) fclose(fp);
	return(rc);
}

/*
**	waitchild() - reap a child daemon
*/

#ifdef INETD

	/* no daemon children to wait for */

#else

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

#endif

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
	register struct sockaddr_in *ap;

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
		ap = &((struct tptab *) tet_sdptab->pt_tdata)->tp_sin;
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
		ap = &((struct tptab *) tet_xdptab->pt_tdata)->tp_sin;
		break;
	default:
		error(0, "received tsinfo for unexpected ptype",
			tet_ptptype(mp->ts_ptype));
		pp->ptm_rc = ER_ERR;
		return;
	}

	/* store the data */
	ap->sin_family = AF_INET;
	ap->sin_addr.s_addr = htonl(mp->ts_addr);
	ap->sin_port = htons(mp->ts_port);
	TRACE4(tet_Ttccd, 2, "received tsinfo for %s: addr = %s, port = %s",
		tet_ptptype(mp->ts_ptype), inet_ntoa(ap->sin_addr),
		tet_i2a(ntohs(ap->sin_port)));

	/* all ok so set up the reply message and return */
	pp->ptm_rc = ER_OK;
}

