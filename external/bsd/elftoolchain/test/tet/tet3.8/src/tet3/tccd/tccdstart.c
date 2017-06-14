/*
 *	SCCS: @(#)tccdstart.c	1.6 (03/08/28)
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
static char sccsid[] = "@(#)tccdstart.c	1.6 (03/08/28) TETware release 3.8";
static char *copyright[] = {
	"(C) Copyright 1996 X/Open Company Limited",
	"All rights reserved"
};
#endif

/************************************************************************

SCCS:   	@(#)tccdstart.c	1.6 03/08/28 TETware release 3.8
NAME:		tccdstart.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	October 1996

DESCRIPTION:
	inetd-like program used to launch tccd on WIN32 platforms

	on other platforms this program just prints a diagnostic and
	exits


MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., May 1997
	port to Windows 95

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
	Changes to conform to UNIX98.
 
	Matthew Hails, The Open Group, August 2003
	Modified address_len argument to accept() to use SOCKLEN_T type.

************************************************************************/

#include <stdio.h>

#ifdef _WIN32		/* -START-WIN32-CUT- */

#include <windows.h>
#include <winsock.h>
#include <signal.h>
#include <time.h>
#include <process.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "error.h"
#include "globals.h"
#include "bstring.h"
#include "ltoa.h"
#include "dtetlib.h"
#include "inetlib_in.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* static function declarations */
static void cleanup PROTOLIST((int));
static void doaccept PROTOLIST((SOCKET, char **));
static void log PROTOLIST((char *, char *));
static void tccdstart_fatal PROTOLIST((int, char *, int, char *, char *));


int main(argc, argv)
int argc;
char **argv;
{
	char **ap;
	char **tccdargv = (char **) 0;
	int ltccdargv = 0, ntccdargv = 0;
	extern int optind;
	extern char *optarg;
	int errors = 0;
	int tccdport = 0;
	char opt[3];
	struct sockaddr_in sin;
	SOCKET sd;
	WORD version = MAKEWORD(1, 1);
	WSADATA wsadata;
	int c, rc;
	char *p;
	static char usage[] = "[-e name=value] [-l logfile] [-p port]";

	/* must be first */
	tet_init_globals("tccdstart", PT_STAND, -1, tet_generror,
		tccdstart_fatal);

#ifndef NOTRACE
	tet_traceinit(argc, argv);
#endif

	/* ensure that we are running on Windows NT */
	if (!tet_iswinNT()) {
		error(0, "Distributed TETware needs Windows NT to run",
			(char *) 0);
		exit(1);
	}

	/* initialise the argv for tccd */
	if (BUFCHK((char **) &tccdargv, &ltccdargv, (ntccdargv + 2) * sizeof *tccdargv) < 0)
		return(1);
	*(tccdargv + ntccdargv++) = "tccd";
	*(tccdargv + ntccdargv) = (char *) 0;

	/* process the command-line arguments */
	while ((c = GETOPT(argc, argv, "T:e:l:p:")) != EOF)
		switch (c) {
		case 'T':	/* trace option - handled by tet_traceinit() */
			break;
		case 'e':	/* tccd command-line options */
		case 'l':
			if (BUFCHK((char **) &tccdargv, &ltccdargv, (ntccdargv + 3) * sizeof *tccdargv) < 0)
				return(1);
			(void) sprintf(opt, "-%c", c);
			if ((p = tet_strstore(opt)) == (char *) 0)
				return(1);
			*(tccdargv + ntccdargv++) = p;
			*(tccdargv + ntccdargv++) = optarg;
			*(tccdargv + ntccdargv) = (char *) 0;
			break;
		case 'p':	/* optional port number to listen on */
			if ((tccdport = atoi(optarg)) <= 0) {
				error(0, "bad port number:", optarg);
				errors++;
			}
			break;
		default:
			errors++;
			break;
		}

	if (errors) {
		(void) fprintf(stderr, "usage: %s %s", tet_progname, usage);
		exit(2);
	}

	/* install signal handlers */
	if (signal(SIGINT, SIG_IGN) != SIG_IGN)
		(void) signal(SIGINT, cleanup);
	if (signal(SIGBREAK, SIG_IGN) != SIG_IGN)
		(void) signal(SIGBREAK, cleanup);
	if (signal(SIGTERM, SIG_IGN) != SIG_IGN)
		(void) signal(SIGTERM, cleanup);

	/* initialise the winsock library */
	if ((rc = WSAStartup(version, &wsadata)) != 0)
		fatal(rc, "WSAStartup() failed", (char *) 0);

	/*
	** determine the tccd port number to use if it was not overridden on
	** the command line
	*/
	if (tccdport <= 0 && (tccdport = tet_gettccdport()) < 0)
		exit(1);

	/* get a socket to listen on */
	if ((sd = socket(AF_INET, SOCK_STREAM, 0)) == INVALID_SOCKET)
		fatal(SOCKET_ERRNO, "can't create listen socket", (char *) 0);
	(void) SOCKET_FIOCLEX(sd);

	/* bind the listen socket to the well-known tccd portg number */
	bzero((char *) &sin, sizeof sin);
	sin.sin_family = AF_INET;
	sin.sin_addr.s_addr = INADDR_ANY;
	sin.sin_port = ntohs((unsigned short) tccdport);
	if (bind(sd, (struct sockaddr *) &sin, sizeof sin) == SOCKET_ERROR)
		fatal(SOCKET_ERRNO, "can't bind to listen socket", (char *) 0);

	/* arrange to listen on the socket */
	if (listen(sd, 5) == SOCKET_ERROR)
		fatal(SOCKET_ERRNO, "listen() failed on sd", tet_i2a(sd));

	/* loop forever, accepting connections and starting tccds */
	log("accepting connections", (char *) 0);
	for (;;)
		doaccept(sd, tccdargv);

	/* NOTREACHED */
}

/*
**	doaccept() - accept a connection and spawn a tccd process
*/

static void doaccept(sd, argv)
SOCKET sd;
char **argv;
{
	fd_set rfds;
	SOCKET nsd;
	struct sockaddr_in remaddr;
	SOCKLEN_T addrlen = (SOCKLEN_T)(sizeof remaddr);
	struct hostent *hp;
	char *remhost;
	char **newargv;
	static char nsdstr[LNUMSZ];
	static char *sargv[] = { "-s", nsdstr, (char *) 0 };
	int pid;

	/* wait for a connection to arrive */
	FD_ZERO(&rfds);
	FD_SET(sd, &rfds);
	if (select(0, &rfds, (fd_set *) 0,  (fd_set *) 0, (struct timeval *) 0) == SOCKET_ERROR)
		fatal(SOCKET_ERRNO, "select() failed", (char *) 0);

	/* accept the connection */
	if ((nsd = accept(sd, (struct sockaddr *) &remaddr, &addrlen)) == INVALID_SOCKET)
		fatal(SOCKET_ERRNO, "accept() failed on sd", tet_i2a(sd));

	/* log the connection */
	if ((hp = gethostbyaddr((char *) &remaddr.sin_addr, sizeof remaddr.sin_addr, remaddr.sin_family)) != (struct hostent *) 0)
		remhost = hp->h_name;
	else
		remhost = inet_ntoa(remaddr.sin_addr);
	log("connection received from", remhost);

	/* build the argv for tccd */
	(void) sprintf(nsdstr, "%ld", nsd);
	newargv = tet_addargv(argv, sargv);
	argv = tet_traceargs(PT_STCC, newargv);
	TRACE2(tet_Tbuf, 6, "free newargv = %s", tet_i2x(newargv));
	free((char *) newargv);

	/* start up the new tccd */
	(void) _flushall();
	if ((pid = _spawnvp(_P_NOWAIT, *argv, argv)) == -1)
		fatal(errno, "spawnvp() failed, path =", *argv);

	/* free up resources allocated here */
	CloseHandle((HANDLE) pid);
	closesocket(nsd);
}

/*
**	log() - make a log entry
*/

static void log(s1, s2)
char *s1, *s2;
{
	time_t now;
	struct tm *tm;
	static char *months[] = {
		"Jan", "Feb", "Mar", "Apr", "May", "Jun",
		"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
	};

	now = time((time_t *) 0);
	tm = localtime(&now);

	(void) printf("%s: %2d %s %2d:%02d:%02d: %s",
		tet_progname, tm->tm_mday, months[tm->tm_mon], tm->tm_hour,
		tm->tm_min, tm->tm_sec, s1);
	if (s2 && *s2)
		(void) printf(" %s", s2);
	(void) putchar('\n');
	(void) fflush(stdout);
}

/*
**	tccdstart_fatal() - fatal error handler
*/

static void tccdstart_fatal(errnum, file, line, s1, s2)
int errnum, line;
char *file, *s1, *s2;
{
	(*tet_liberror)(errnum, file, line, s1, s2);
	(void) WSACleanup();
	exit(1);
}

/*
**	cleanup() - cleanup and exit on receipt of a signal
*/

static void cleanup(sig)
int sig;
{
	if (sig)
		log("going down on signal", tet_i2a(sig));

	(void) WSACleanup();
	exit(sig ? 3 : 0);
}

#else /* _WIN32 */	/* -END-WIN32-CUT- */

int main()
{
	static char *text[] = {
		"tccdstart is not used on this type of system.\n",
		"Please refer to the TETware Installation and User Guide",
		"for details on how to start tccd on your system."
	};

	char **tp;

	for (tp = text; tp < &text[sizeof text / sizeof text[0]]; tp++)
		(void) fprintf(stderr, "%s\n", *tp);

	return(1);
}

#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */

