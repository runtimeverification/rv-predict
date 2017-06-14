/*
 *	SCCS: @(#)tccdsrv.c	1.1 (00/09/05)
 *
 *	UniSoft Ltd., London, England
 *
 * Copyright (c) 2000 The Open Group
 * All rights reserved.
 *
 * No part of this source code may be reproduced, stored in a retrieval
 * system, or transmitted, in any form or by any means, electronic,
 * mechanical, photocopying, recording or otherwise, except as stated
 * in the end-user licence agreement, without the prior permission of
 * the copyright owners.
 * A copy of the end-user licence agreement is contained in the file
 * Licence which accompanies this distribution.
 * 
 * Motif, OSF/1, UNIX and the "X" device are registered trademarks and
 * IT DialTone and The Open Group are trademarks of The Open Group in
 * the US and other countries.
 *
 * X/Open is a trademark of X/Open Company Limited in the UK and other
 * countries.
 *
 ************************************************************************
 *
 * The following copyright applies to a small portion of this code:
 *
 * Copyright 1990, 1991, 1992 by the Massachusetts Institute of Technology and
 * UniSoft Group Limited.
 * 
 * Permission to use, copy, modify, distribute, and sell this software and
 * its documentation for any purpose is hereby granted without fee,
 * provided that the above copyright notice appear in all copies and that
 * both that copyright notice and this permission notice appear in
 * supporting documentation, and that the names of MIT and UniSoft not be
 * used in advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.  MIT and UniSoft
 * make no representations about the suitability of this software for any
 * purpose.  It is provided "as is" without express or implied warranty.
 *
 * $XConsortium: getopt.c,v 1.2 92/07/01 11:59:04 rws Exp $
 */

#ifndef lint
static char sccsid[] = "@(#)tccdsrv.c	1.1 (00/09/05) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tccdsrv.c	1.1 00/09/05 TETware release 3.8
NAME:		tccdsrv.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	July 2000

DESCRIPTION:
	tccdsrv is the Windows NT service equivalent to tccdstart.
	It is a multi-threaded program and does not use any of the
	TETware header or library files.

MODIFICATIONS:

************************************************************************/

#include <stdio.h>

#ifdef _WIN32		/* -START-WIN32-CUT- */

#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <stdarg.h>
#include <io.h>
#include <process.h>
#include <windows.h>
#include <winsock.h>

#include "msgs.gen"

/* functions always have prototypes on Win32 */
#define PROTOLIST(list)		list

/* the maximum length of a file name */
#define MAXPATH			1024

/* the size of a digit string to contain a (long) (10 digits + sign + null) */
#define LNUMSZ			12


/* error handler macros */
#define error(err, s1, s2)	errorfunc(srcFile, __LINE__, err, s1, s2)
#define fatal(err, s1, s2)	fatalfunc(srcFile, __LINE__, err, s1, s2)
#define w32error(err, s1, s2)	w32errorfunc(srcFile, __LINE__, err, s1, s2)
#define w32fatal(err, s1, s2)	w32fatalfunc(srcFile, __LINE__, err, s1, s2)

/* trace macros */
#ifdef TRACE
#  define DEBUG(params)		if (dbflag) debug params
#  ifndef TRACEFILE
#    define TRACEFILE		"c:/tmp/tccdsrv.txt"
#  endif
#else
#  define DEBUG(params)
#endif

/* other macros */
#define isdirsep(c)		((c) == '/' || (c) == '\\')
#define tet_min(a, b)		((a) < (b) ? (a) : (b))

/* install parameter block */
struct inst_params {
	int ip_aflag;
	int ip_sflag;
	char *ip_topt;
	char *ip_uopt;
	char *ip_xopt;
	char **ip_tccdargs;
	int ip_ntccdargs;
	int ip_tccdport;
};


/* the name of the event log key below HKEY_LOCAL_MACHINE */
static char ev_key[] =
	"SYSTEM\\CurrentControlSet\\Services\\EventLog\\Application\\tccdsrv";

/* the name of the tccd parameter key below HKEY_LOCAL_MACHINE */
static char arg_key[] = "SOFTWARE\\UniSoft\\TETware\\3.8\\tccd";

/* the name of the service */
static char service_name[] = "tccdsrv";

/* other local variables */
#ifdef TRACE
#  ifndef DBFLAG
#    define DBFLAG 0
#  endif
   static int dbflag = DBFLAG;
#endif
static HANDLE ev_handle = (HANDLE) -1;
static int is_service = 0;
static char *progname;
static char *w32optarg;
static int w32optind = 1;
static int w32opterr = 1;
static DWORD service_state = SERVICE_STOPPED;
static SERVICE_STATUS_HANDLE status_handle;
static char srcFile[] = __FILE__;
static HANDLE stop_event_handle;

/* declarations of entry points used by the SCM */
/* extern function declarations */
void WINAPI service_control PROTOLIST((DWORD));
void WINAPI service_main PROTOLIST((DWORD, LPTSTR *));

/* static function declarations */
static char *basename PROTOLIST((char *));
static void cleanup PROTOLIST((int));
static void dirname PROTOLIST((char *, char [], int));
static void errorfunc PROTOLIST((char *, int, int, char *, char *));
static void fatalfunc PROTOLIST((char *, int, int, char *, char *));
static void install_tccdsrv PROTOLIST((struct inst_params *));
static void logfunc PROTOLIST((char *));
static SC_HANDLE open_sc_manager PROTOLIST((void));
static void reg_close_key PROTOLIST((char *, HKEY));
static HKEY reg_create_key PROTOLIST((char *));
static void reg_delete_key PROTOLIST((char *));
static void reg_set_dword_value PROTOLIST((char *, HKEY, char *, DWORD));
static void reg_set_fatal PROTOLIST((char *, LONG, char *));
static void reg_set_multistring_value PROTOLIST((char *, HKEY, char *, char **,
	int));
static void reg_set_string_value PROTOLIST((char *, HKEY, char *, char *));
static void remove_tccdsrv PROTOLIST((void));
static int report_status PROTOLIST((DWORD, DWORD, DWORD));
static void rm_empty_key_path PROTOLIST((char *));
static char *rstrdup PROTOLIST((char *));
static void run_tccdservice_start PROTOLIST((void));
static void service_stop PROTOLIST((void));
static void service_start PROTOLIST((void));
static void ss2 PROTOLIST((void));
static void ss3 PROTOLIST((void));
static void ss4 PROTOLIST((HKEY));
static void ss5 PROTOLIST((SOCKET, int, char *, char **));
static void start_tccdsrv PROTOLIST((SC_HANDLE));
static void w32errorfunc PROTOLIST((char *, int, int, char *, char *));
static void w32fatalfunc PROTOLIST((char *, int, int, char *, char *));
static char *wsaerrmsg PROTOLIST((int));

#ifdef TRACE
  static char *pr_service_state PROTOLIST((int));
  static void debug PROTOLIST((char *, ...));
#endif


int main(argc, argv)
int argc;
char **argv;
{
	char **tccdargs;
	int ntccdargs;
	OSVERSIONINFO osinf;
	int c;
	int iflag = 0;
	int rflag = 0;
	int errors = 0;
	char buf[32];
	static char msg[] = "can't allocate memory for tccd arguments";
	struct inst_params inst_params;

	progname = *argv;

	DEBUG(("in main()"));

	/* make sure that we are running on Windows NT */
	memset((void *) &osinf, '\0', sizeof osinf);
	osinf.dwOSVersionInfoSize = sizeof osinf;
	if (GetVersionEx(&osinf) == FALSE)
		w32fatal(GetLastError(), "GetVersionEx() failed", (char *) 0);
	if (osinf.dwPlatformId != VER_PLATFORM_WIN32_NT)
		fatal(0, "Distributed TETware needs Windows NT to run",
			(char *) 0);

	/* initialise the install parameter block */
	memset((void *) &inst_params, '\0', sizeof inst_params);

	/* allocate memory for the tccd arguments */
	errno = 0;
	ntccdargs = 0;
	tccdargs = (char **) malloc((ntccdargs + 1) * sizeof *tccdargs);
	if (tccdargs == (char **) 0)
		fatal(errno, msg, (char *) 0);
	*(tccdargs + ntccdargs) = (char *) 0;

	/* process the command-line arguments */
	while ((c = w32getopt(argc, argv, "T:ade:i:l:p:r:st:u:x:")) != EOF)
		switch (c) {
		case 'T':
			/* tccd option */
			(void) sprintf(buf, "-%c%.*s",
				c, sizeof buf - 3, w32optarg);
			errno = 0;
			tccdargs = (char **) realloc((void *) tccdargs,
				(ntccdargs + 2) * sizeof *tccdargs);
			if (tccdargs == (char **) 0)
				fatal(errno, msg,  (char *) 0);
			*(tccdargs + ntccdargs++) = rstrdup(buf);
			*(tccdargs + ntccdargs) = (char *) 0;
			break;
		case 'a':
			/* install service for automatic start, else demand */
			inst_params.ip_aflag = 1;
			break;
		case 'd':
			/* debug flag */
#ifdef TRACE
			dbflag = 1;
#else
			fprintf(stderr, "%s: tracing not configured\n",
				progname);
			exit(2);
#endif
			break;
		case 'e':
		case 'l':
			/* tccd options */
			(void) sprintf(buf, "-%c", c);
			errno = 0;
			tccdargs = (char **) realloc((void *) tccdargs,
				(ntccdargs + 3) * sizeof *tccdargs);
			if (tccdargs == (char **) 0)
				fatal(errno, msg, (char *) 0);
			*(tccdargs + ntccdargs++) = rstrdup(buf);
			*(tccdargs + ntccdargs++) = w32optarg;
			*(tccdargs + ntccdargs) = (char *) 0;
			break;
		case 'i':
			/* install service */
			if (!strcmp(w32optarg, "nstall"))
				iflag = 1;
			else
				errors++;
			break;
		case 'p':
			/* tccd listen port number */
			if ((inst_params.ip_tccdport = atoi(w32optarg)) <= 0) {
				error(0, "bad port number:", w32optarg);
				errors++;
			}
			break;
		case 'r':
			/* remove service */
			if (!strcmp(w32optarg, "emove"))
				rflag = 1;
			else
				errors++;
			break;
		case 's':
			/* start service after installing */
			inst_params.ip_sflag = 1;
			break;
		case 't':
			/* tet root */
			inst_params.ip_topt = w32optarg;
			break;
		case 'u':
			/* user name */
			inst_params.ip_uopt = w32optarg;
			break;
		case 'x':
			/* password */
			inst_params.ip_xopt = w32optarg;
			break;
		default:
			errors++;
			break;
		}

	DEBUG(("at end of argc loop"));

	if (
		rflag && (
			iflag ||
			ntccdargs > 0 ||
			inst_params.ip_aflag ||
			inst_params.ip_sflag ||
			inst_params.ip_tccdport > 0 ||
			inst_params.ip_topt ||
			inst_params.ip_uopt ||
			inst_params.ip_xopt
		)
	)
		errors++;

	if (iflag && !inst_params.ip_uopt && inst_params.ip_xopt)
		errors++;

	if (errors) {
		fprintf(stderr, "usage:\t%s -install [-a] [-e name=value] [-l logfile] [-p port] [-s] [-t tet_root] [-u username [-x password]]\n",
			progname);
		fprintf(stderr, "or:\t%s -remove\n", progname);
		exit(2);
	}

	if (iflag) {
		inst_params.ip_tccdargs = tccdargs;
		inst_params.ip_ntccdargs = ntccdargs;
		install_tccdsrv(&inst_params);
	}
	else if (rflag)
		remove_tccdsrv();
	else
		run_tccdservice_start();

	cleanup(0);
}

/************************************************************************
*									*
*	the service code						*
*									*
************************************************************************/

/*
**	run_tccdservice_start()	- run the tccdsrv service
*/
static void run_tccdservice_start()
{
	SERVICE_TABLE_ENTRY dispatch_table[] = {
		{ service_name, (LPSERVICE_MAIN_FUNCTION) service_main },
		{ NULL, NULL }
	};

	is_service = 1;

	if (_isatty(0) && _isatty(1)) {
		printf("%s: about to contact the Service Control Manager\n",
			progname);
		printf("type ^C to cancel\n");
		Sleep(5000);
	}

	DEBUG(("run_tccdservice_start(): about to call StartServiceCtrlDispatcher()"));

	/* transfer control to the SCM */
	if (StartServiceCtrlDispatcher(dispatch_table) == FALSE)
		w32fatal(GetLastError(), "StartServiceCtrlDispatcher() failed",
			(char *) 0);

	DEBUG(("run_tccdservice_start(): after call to StartServiceCtrlDispatcher()"));
}

/*
**	service_main() - the SCM entry point
**
**	there should be no calls to any of the fatal error handlers
**	below here
*/
void WINAPI service_main(argc, argv)
DWORD argc;
LPTSTR *argv;
{
#ifdef TRACE
	char username[64];
	DWORD unlen;
	BOOL rc;
	int c;

	w32optind = 1;
	w32opterr = 0;
	while ((c = w32getopt(argc, argv, "d")) != EOF)
		switch (c) {
		case 'd':
			dbflag = 1;
			break;
		default:
			break;
		}
#endif


#ifdef TRACE
	if (dbflag) {
		debug("call to service_main(%d, %#lx)",
			(int) argc, (long) argv);
		unlen = sizeof username;
		rc = GetUserName((LPSTR) username, &unlen);
		if (rc == FALSE)
			w32error(GetLastError(), "GetUserName() failed",
				(char *) 0);
		else
			debug("username = \"%s\"", username);
	}
#endif

	/* register the control handler for this service */
	status_handle = RegisterServiceCtrlHandler(service_name,
		service_control);
	if (status_handle == 0)
		w32fatal(GetLastError(),
			"RegisterServiceControlHandler() failed", (char *) 0);

	/* report back to the SCM and start the service */
	if (report_status(SERVICE_START_PENDING, NO_ERROR, 3000) == 0)
		service_start();

	logfunc("shutting down");
	(void) report_status(SERVICE_STOPPED, NO_ERROR, 0);

	DEBUG(("service_main() RETURN"));
}

/*
**	service_start() - start the service
**
**	this level performs winsock initialisation and cleanup
*/
static void service_start()
{
	WORD version = MAKEWORD(1, 1);
	WSADATA wsadata;
	int rc;

	DEBUG(("call to service_start()"));

	if ((rc = WSAStartup(version, &wsadata)) != 0) {
		error(rc, "WSAStartup() failed", (char *) 0);
		return;
	}

	ss2();

	(void) WSACleanup();
	DEBUG(("service_start() normal RETURN"));
}

/*
**	ss2() - extend the service_start() processing
**
**	this level creates and removes the stop event that is used
**	by the service control thread to communicate with the main thread
*/
static void ss2()
{
	BOOL rc;

	DEBUG(("call to ss2()"));

	if (report_status(SERVICE_START_PENDING, NO_ERROR, 3000) < 0)
		return;

	stop_event_handle = CreateEvent(NULL, TRUE, FALSE, NULL);
	if (stop_event_handle == NULL) {
		w32error(GetLastError(), "CreateEvent() failed", (char *) 0);
		return;
	}

	ss3();

	(void) CloseHandle(stop_event_handle);
	stop_event_handle = NULL;
	DEBUG(("ss2() normal RETURN"));
}

/*
**	ss3() - extend the service_start() processing some more
**
**	this level opens and closes the registry
*/
static void ss3()
{
	static char fmt[] = "RegOpenKeyEx(\"%.128s\") failed";
	char msg[sizeof fmt + 128];
	HKEY key_handle;
	LONG rval;

	DEBUG(("call to ss3()"));

	if (report_status(SERVICE_START_PENDING, NO_ERROR, 3000) < 0)
		return;

	/* open the registry key containing the tccd path, args and port */
	rval = RegOpenKeyEx(HKEY_LOCAL_MACHINE, arg_key, 0, KEY_READ,
		&key_handle);
	if (rval != ERROR_SUCCESS) {
		(void) sprintf(msg, fmt, arg_key);
		w32error(rval, msg, (char *) 0);
		return;
	}

	ss4(key_handle);

	(void) RegCloseKey(key_handle);
	DEBUG(("ss3() normal RETURN"));
}

/*
**	ss4() - extend the service_start() processing yet more
**
**	this level extracts some data from the registry,
**	then opens and closes the listen socket
*/
static void ss4(key_handle)
HKEY key_handle;
{
	static char fmt[] = "RegQueryValueEx(\"%.128s\", \"%.16s\") failed";
	char msg[sizeof fmt + 128 + 16];
	char *name;
	LONG rval;
	DWORD dvalue;
	char *svalue;
	DWORD bufsz;
	char *tccdpath;
 	char **tccdargs;
	int ntccdargs, nregargs;
	char **ap, *p;
	int new;
	int tccdport;
	SOCKET sd;
	struct servent *sp;

	DEBUG(("call to ss4(%#lx)", (long) key_handle));

	if (report_status(SERVICE_START_PENDING, NO_ERROR, 3000) < 0)
		return;

	/* get tccdpath from the registry */
	bufsz = 0;
	name = "path";
	rval = RegQueryValueEx(key_handle, name, NULL, NULL, NULL, &bufsz);
	switch (rval) {
	case ERROR_SUCCESS:
		errno = 0;
		tccdpath = (char *) malloc(bufsz);
		if (tccdpath == (char *) 0) {
			error(errno, "can't allocate memory for tccd path",
				(char *) 0);
			return;
		}
		rval = RegQueryValueEx(key_handle, name, NULL, NULL,
			(LPBYTE) tccdpath, &bufsz);
		if (rval != ERROR_SUCCESS) {
			(void) sprintf(msg, fmt, arg_key, name);
			w32error(rval, msg, (char *) 0);
			return;
		}
		break;
	default:
		(void) sprintf(msg, fmt, arg_key, name);
		w32error(rval, msg, (char *) 0);
		return;
	}

	/* start an argv for tccd */
	errno = 0;
	ntccdargs = 3;
	tccdargs = (char **) malloc((ntccdargs + 1) * sizeof *tccdargs);
	if (tccdargs == (char **) 0) {
		error(errno, "can't allocate memory for tccd argv",
			(char *) 0);
		return;
	}
	ap = tccdargs;
	*ap++ = "tccd";
	*ap++ = "-s";
	*ap++ = (char *) 0;	/* for the sd returned by accept() */
	*ap = (char *) 0;

	/* get command-line args from the registry */
	bufsz = 0;
	name = "args";
	rval = RegQueryValueEx(key_handle, name, NULL, NULL, NULL, &bufsz);
	switch (rval) {
	case ERROR_SUCCESS:
		errno = 0;
		svalue = (char *) malloc(bufsz);
		if (svalue == (char *) 0) {
			error(errno, "can't allocate memory for tccd args",
				(char *) 0);
			return;
		}
		rval = RegQueryValueEx(key_handle, name, NULL, NULL,
			(LPBYTE) svalue, &bufsz);
		if (rval != ERROR_SUCCESS) {
			(void) sprintf(msg, fmt, arg_key, name);
			w32error(rval, msg, (char *) 0);
			return;
		}
		nregargs = 0;
		new = 1;
		for (p = svalue; p < svalue + bufsz; p++) {
			if (*p == '\0') {
				new = 1;
				if (*(p + 1) == '\0')
					break;
			}
			else if (new) {
				nregargs++;
				new = 0;
			}
		}
		errno = 0;
		tccdargs = (char **) realloc((void *) tccdargs,
			(ntccdargs + nregargs + 1) * sizeof *tccdargs);
		if (tccdargs == (char **) 0) {
			error(errno, "can't grow memory for tccd argv",
				(char *) 0);
			return;
		}
		new = 1;
		ap = tccdargs + ntccdargs;
		for (p = svalue; p < svalue + bufsz; p++) {
			if (*p == '\0') {
				new = 1;
				if (*(p + 1) == '\0')
					break;
			}
			else if (new) {
				if (ap < tccdargs + ntccdargs + nregargs)
					*ap++ = p;
				else
					break;
				new = 0;
			}
		}
		ntccdargs += nregargs;
		*ap = (char *) 0;
		break;
	case ERROR_FILE_NOT_FOUND:
		break;
	default:
		(void) sprintf(msg, fmt, arg_key, name);
		w32error(rval, msg, (char *) 0);
		return;
	}

	/* get port number from the registry or from the services database */
	bufsz = sizeof dvalue;
	name = "port";
	rval = RegQueryValueEx(key_handle, name, NULL, NULL, (LPBYTE) &dvalue,
		&bufsz);
	switch (rval) {
	case ERROR_SUCCESS:
		tccdport = dvalue;
		break;
	case ERROR_FILE_NOT_FOUND:
		WSASetLastError(0);
		sp = getservbyname("tcc", "tcp");
		if (sp == (struct servent *) 0) {
			error(WSAGetLastError(), "tcc/tcp: unknown service",
				(char *) 0);
			return;
		}
		tccdport = ntohs((unsigned short) sp->s_port);
		break;
	default:
		(void) sprintf(msg, fmt, arg_key, name);
		w32error(rval, msg, (char *) 0);
		return;
	}

	/* get a socket to listen on */
	if ((sd = socket(AF_INET, SOCK_STREAM, 0)) == INVALID_SOCKET) {
		error(WSAGetLastError(), "can't create listen socket",
			(char *) 0);
		return;
	}

	ss5(sd, tccdport, tccdpath, tccdargs);

	(void) closesocket(sd);
	DEBUG(("ss4() normal RETURN"));
}

/*
**	ss5() - extend the service_start() processing even more
**
**	this level sets up the listen socket,
**	then performs the main service loop
*/
static void ss5(sd, tccdport, tccdpath, tccdargs)
SOCKET sd;
int tccdport;
char *tccdpath, **tccdargs;
{
	BOOL rc;
	DWORD rval;
	struct sockaddr_in sin;
	fd_set rfds;
	int nfds;
	struct timeval tv;
	SOCKET nsd;
	struct sockaddr_in remaddr;
	int addrlen;
	char *remhost;
	struct hostent *hp;
	char nsdstr[LNUMSZ];
	int pid;
	static char fmt[] = "connection received from %.64s";
	char msg[sizeof fmt + 64];

	DEBUG(("call to ss5(%d, %d, %s, %#lx)",
		sd, tccdport, tccdpath, (long) tccdargs));

	if (report_status(SERVICE_START_PENDING, NO_ERROR, 3000) < 0)
		return;

	/* mark the socket as non-inheritable */
	rc = SetHandleInformation((HANDLE) sd, HANDLE_FLAG_INHERIT, 0);
	if (rc == FALSE) {
		w32error(GetLastError(), "SetHandleInformation(HANDLE_FLAG_INHERIT, 0) failed on listen socket",
			(char *) 0);
		return;
	}

	/* bind the socket to the well-known tccd port */
	(void) memset((void *) &sin, '\0', sizeof sin);
	sin.sin_family = AF_INET;
	sin.sin_addr.s_addr = INADDR_ANY;
	sin.sin_port = htons((unsigned short) tccdport);
	if (bind(sd, (struct sockaddr *) &sin, sizeof sin) == SOCKET_ERROR) {
		error(WSAGetLastError(), "bind() failed on listen socket",
			(char *) 0);
		return;
	}

	/* arrange to listen on the socket */
	if (listen(sd, 5) == SOCKET_ERROR) {
		error(WSAGetLastError(), "listen() failed on listen socket",
			(char *) 0);
		return;
	}

	if (report_status(SERVICE_RUNNING, NO_ERROR, 0) < 0)
		return;

	/* loop forever, accepting connections and starting tccds */
	logfunc("accepting connections");
	for (;;) {
		FD_ZERO(&rfds);
		FD_SET(sd, &rfds);
		tv.tv_sec = 1;
		tv.tv_usec = 0;
		nfds = select(0, &rfds, (fd_set *) 0, (fd_set *) 0, &tv);
		switch (nfds) {
		case 0:
			rval = WaitForSingleObject(stop_event_handle, 0);
			switch (rval) {
			case WAIT_OBJECT_0:
				logfunc("shutting down");
				return;
			case WAIT_FAILED:
				w32error(GetLastError(), "WaitForSingleObject(stop_event) failed",
					(char *) 0);
				return;
			default:
				break;
			}
			continue;
		case SOCKET_ERROR:
			error(WSAGetLastError(), "select() failed", (char *) 0);
			return;
		}
		addrlen = sizeof remaddr;
		nsd = accept(sd, (struct sockaddr *) &remaddr, &addrlen);
		if (nsd == INVALID_SOCKET) {
			error(WSAGetLastError(),
				"accept() failed on listen socket",
				(char *) 0);
			return;
		}
		hp = gethostbyaddr((char *) &remaddr.sin_addr,
			sizeof remaddr.sin_addr, remaddr.sin_family);
		if (hp == (struct hostent *) 0)
			remhost = inet_ntoa(remaddr.sin_addr);
		else
			remhost = hp->h_name;
		(void) sprintf(msg, fmt, remhost);
		logfunc(msg);
		(void) sprintf(nsdstr, "%ld", nsd);
		*(tccdargs + 2) = nsdstr;
#ifdef TRACE
		if (dbflag) {
			char **ap;
			extern char **_environ;
			debug("about to spawn \"%s\"", tccdpath);
			for (ap = tccdargs; *ap; ap++)
				debug("tccdarg[%d] = \"%s\"",
					ap - tccdargs, *ap);
			for (ap = _environ; *ap; ap++)
				debug("env = \"%s\"", *ap);
		}
#endif
		(void) _flushall();
		if ((pid = _spawnvp(_P_NOWAIT, tccdpath, tccdargs)) == -1) {
			error(errno, "spawnvp() failed: path =", tccdpath);
			return;
		}
		(void) CloseHandle((HANDLE) pid);
		(void) closesocket(nsd);
	}
}

/*
**	service_stop() - request the main thread to stop
**
**	the calls to exit() from here should never happen
*/
static void service_stop()
{
	BOOL rval;
	int err;

	DEBUG(("call to service_stop()"));

	if (stop_event_handle) {
		rval = SetEvent(stop_event_handle);
		if (rval == FALSE) {
			err = GetLastError();
			(void) report_status(SERVICE_STOPPED, err, 0);
			w32fatal(err, "SetEvent(stop_event) failed",
				(char *) 0);
		}
	}
	else {
		if (status_handle)
			(void) report_status(SERVICE_STOPPED, NO_ERROR, 0);
		cleanup(0);
	}

	DEBUG(("service_stop() RETURN"));
}

/*
**	service_control() - the SCM control function
*/
void WINAPI service_control(request)
DWORD request;
{
	DEBUG(("call to service_control(%d)", request));

	switch (request) {
	case SERVICE_CONTROL_STOP:
		(void) report_status(SERVICE_STOP_PENDING, NO_ERROR, 1000);
		service_stop();
		return;
	case SERVICE_CONTROL_INTERROGATE:
		break;
	default:
		break;
	}

	report_status(service_state, NO_ERROR, 0);

	DEBUG(("service_control() RETURN"));
}

/*
**	report_status() - send a status report to the SCM
**
**	return 0 if successful or -1 on error
*/
static int report_status(current_state, exit_code, wait_hint)
DWORD current_state, exit_code, wait_hint;
{
	static DWORD checkpoint = 1;
	SERVICE_STATUS ss;
	BOOL rval;
	int rc;

	DEBUG(("call to report_status(%s, %d, %d)",
		pr_service_state(current_state), exit_code, wait_hint));

	(void) memset((void *) &ss, '\0', sizeof ss);

	ss.dwServiceType = SERVICE_WIN32_OWN_PROCESS;
	ss.dwCurrentState = current_state;
	ss.dwWin32ExitCode = exit_code;
	ss.dwServiceSpecificExitCode = 0;
	ss.dwWaitHint = wait_hint;

	switch (current_state) {
	case SERVICE_START_PENDING:
		ss.dwControlsAccepted = 0;
		break;
	default:
		ss.dwControlsAccepted = SERVICE_ACCEPT_STOP;
		break;
	}

	switch (current_state) {
	case SERVICE_RUNNING:
	case SERVICE_STOPPED:
		ss.dwCheckPoint = 0;
		break;
	default:
		ss.dwCheckPoint = checkpoint++;
		break;
	}

	service_state = current_state;

	rval = SetServiceStatus(status_handle, &ss);
	if (rval == FALSE)
		w32error(GetLastError(), "SetServiceStatus() failed",
			(char *) 0);

	rc = (rval == FALSE) ? -1 : 0;
	DEBUG(("report_status() RETURN %d", rc));
	return(rc);
}


/************************************************************************
*									*
*	service installation and removal				*
*									*
************************************************************************/

/*
**	install_tccdsrv() - install the tccdsrv service
*/
static void install_tccdsrv(ip)
struct inst_params *ip;
{
	static char tccdsrv_prog[] = "tccdsrv.exe";
	char event_message_file[MAXPATH];
	char tccdsrv_path[MAXPATH];
	char tccdpath[MAXPATH];
	LONG rval;
	HKEY key_handle;
	SC_HANDLE scm_handle;
	SC_HANDLE service_handle;
	SERVICE_STATUS service_status;
	char user[32];
	LPCTSTR username, password;
	char *p;
	BOOL rc;
	int try;


	/* work out the path to tccd */
	if (
		(p = ip->ip_topt) == (char *) 0 &&
		((p = getenv("TET_ROOT")) == (char *) 0 || !*p)
	) {
		fprintf(stderr, "%s: you must specify the value of TET_ROOT,\n",
			progname);
		fprintf(stderr,
	"either in the environment or by using the -t command-line option\n");
		exit(2);
	}
	(void) sprintf(tccdpath, "%.*s/bin/tccd.exe",
		sizeof tccdpath - 14, p);

	/* determine this program's full path name */
	rval = GetModuleFileName(NULL, event_message_file,
		sizeof event_message_file);
	if (rval == 0)
		w32fatal(GetLastError(), "GetModuleFileName() failed",
			(char *) 0);

	/* create the registry key for the event log */
	key_handle = reg_create_key(ev_key);
	reg_set_string_value(ev_key, key_handle, "EventMessageFile",
		event_message_file);
	reg_set_dword_value(ev_key, key_handle, "TypesSupported", 0x7);
	reg_close_key(ev_key, key_handle);

	/* store tccd's args in the registry */
	(void) RegDeleteKey(HKEY_LOCAL_MACHINE, arg_key);
	key_handle = reg_create_key(arg_key);
	reg_set_string_value(arg_key, key_handle, "path", tccdpath);
	if (ip->ip_ntccdargs > 0)
		reg_set_multistring_value(arg_key, key_handle, "args",
			ip->ip_tccdargs, ip->ip_ntccdargs);
	if (ip->ip_tccdport > 0)
		reg_set_dword_value(arg_key, key_handle, "port",
			ip->ip_tccdport);
	reg_close_key(arg_key, key_handle);


	/*
	** install the service
	*/

	/* open the service control manager */
	scm_handle = open_sc_manager();

	/* delete an existing service */
	service_handle = OpenService(scm_handle, service_name,
		SERVICE_ALL_ACCESS);
	if (service_handle != NULL) {
		rc = ControlService(service_handle, SERVICE_CONTROL_STOP,
			&service_status);
		for (try = 0; try < 50; try++) {
			if (
				rc == FALSE ||
				service_status.dwCurrentState == SERVICE_STOPPED
			)
				break;
			Sleep(100);
			rc = QueryServiceStatus(service_handle,
				&service_status);
		}
		(void) DeleteService(service_handle);
		(void) CloseServiceHandle(service_handle);
		Sleep(100);
	}

	/* determine the name of the service executable */
	dirname(event_message_file, tccdsrv_path,
		sizeof tccdsrv_path - sizeof tccdsrv_prog - 1);
	p = tccdsrv_path + strlen(tccdsrv_path);
	(void) sprintf(p, "\\%s", tccdsrv_prog);

	/* determine which username and password to use */
	if (ip->ip_uopt) {
		(void) sprintf(user, ".\\%.*s", sizeof user - 3, ip->ip_uopt);
		username = (LPCTSTR) user;
		password = (LPCTSTR) ip->ip_xopt;
	}
	else {
		username = NULL;
		password = NULL;
	}

	/* create the new tccd service */
	service_handle = CreateService(scm_handle, service_name,
		"tccd launcher", SERVICE_ALL_ACCESS, SERVICE_WIN32_OWN_PROCESS,
		ip->ip_aflag ? SERVICE_AUTO_START : SERVICE_DEMAND_START,
		SERVICE_ERROR_NORMAL, tccdsrv_path, NULL, NULL, NULL,
		username, password);
	if (service_handle == NULL) 
		w32fatal(GetLastError(), "CreateService(\"tccdsrv\") failed",
			(char *) 0);

	/* start the service if so required */
	if (ip->ip_sflag)
		start_tccdsrv(service_handle);
	(void) CloseServiceHandle(service_handle);
	(void) CloseServiceHandle(scm_handle);

	printf("tccdsrv service installed\n");
}

/*
**	start_tccdsrv() - start the tccdsrv service
*/
static void start_tccdsrv(service_handle)
SC_HANDLE service_handle;
{
	BOOL rc;
	LPCTSTR *sargp;
	DWORD nsargs;

#ifdef TRACE
	char *sargs[1];


	/* construct an argv to be passed to service_main() */
	if (dbflag) {
		sargs[0] = "-d";
		sargp = sargs;
		nsargs = 1;
		debug("will pass { \"%s\" } to service_main()",
			sargs[0]);
	}
	else
#endif
	{
		sargp = NULL;
		nsargs = 0;
	}

	/* start the service */
	DEBUG(("about to call StartService(%#lx, %ld, %#lx)",
		(long) service_handle, (long) nsargs, (long) sargs));
	rc = StartService(service_handle, nsargs, sargp);
	if (rc == FALSE)
		w32error(GetLastError(), "StartService(\"tccdsrv\") failed",
			(char *) 0);
	DEBUG(("StartService() returned %d", rc));
}

/*
**	remove_tccdsrv() - remove (uninstall) the tccdsrv service
*/
static void remove_tccdsrv()
{
	char *p;
	SC_HANDLE scm_handle;
	SC_HANDLE service_handle;
	SERVICE_STATUS service_status;
	DWORD err;

	/* remove the registry key for the event log */
	reg_delete_key(ev_key);

	/* remove the registey key path containing the tccd args */
	reg_delete_key(arg_key);
	if ((p = strrchr(arg_key, '\\')) != (char *) 0) {
		*p = '\0';
		rm_empty_key_path(arg_key);
		*p = '\\';
	}


	/*
	** uninstall the service
	*/

	/* open the service control manager */
	scm_handle = open_sc_manager();

	/* delete the service */
	service_handle = OpenService(scm_handle, service_name,
		SERVICE_ALL_ACCESS);
	if (service_handle == NULL) {
		err = GetLastError();
		switch (err) {
		case ERROR_SERVICE_DOES_NOT_EXIST:
			break;
		default:
			w32fatal(GetLastError(),
				"OpenService(\"tccdsrv\") failed",
				(char *) 0);
			break;
		}
	}
	else {
		(void) ControlService(service_handle, SERVICE_CONTROL_STOP,
			&service_status);
		if (DeleteService(service_handle) == FALSE) {
			err = GetLastError();
			switch (err) {
			case ERROR_SERVICE_MARKED_FOR_DELETE:
				break;
			default:
				w32error(GetLastError(),
					"DeleteService(\"tccdsrv\") failed",
					(char *) 0);
				break;
			}
		}
		(void) CloseServiceHandle(service_handle);
	}

	(void) CloseServiceHandle(scm_handle);

	printf("tccdsrv service removed\n");
}

/*
**	open_sc_manager() - open the service control manager,
**		checking for errors
**
**	return a handle to the SCM
**	there is no return on error
*/
static SC_HANDLE open_sc_manager()
{
	SC_HANDLE rval;

	rval = OpenSCManager(NULL, NULL, SC_MANAGER_ALL_ACCESS);
	if (rval == NULL) 
		w32fatal(GetLastError(), "OpenSCManager() failed",
			(char *) 0);

	return(rval);
}


/************************************************************************
*									*
*	registry access functions					*
*									*
************************************************************************/

/*
**      reg_create_key() - create a new key in the registry or open an
**		existing one, checking for errors
**
**	there is no return on error
*/
static HKEY reg_create_key(key_name)
char *key_name;
{
	DWORD disposition;
	HKEY key_handle;
	LONG rval;
	static char fmt[] = "RegCreateKeyEx(\"%.128s\") failed";
	char msg[sizeof fmt + 128];

	rval = RegCreateKeyEx(HKEY_LOCAL_MACHINE, key_name, 0, (LPTSTR) 0,
		REG_OPTION_NON_VOLATILE, KEY_ALL_ACCESS, NULL, &key_handle,
		&disposition);

	if (rval != ERROR_SUCCESS) {
		(void) sprintf(msg, fmt, key_name);
		w32fatal(rval, msg, (char *) 0);
	}

	return(key_handle);
}

/*
**	reg_close_key() - close an open registry key, checking for errors
**
**	there is no return on error
*/
static void reg_close_key(key_name, key_handle)
char *key_name;
HKEY key_handle;
{
	DWORD rval;
	static char  fmt[] = "RegCloseKey(\"%.128s\") failed";
	char msg[sizeof fmt + 128];

	rval = RegCloseKey(key_handle);

	if (rval != ERROR_SUCCESS) {
		(void) sprintf(msg, fmt, key_name);
		w32fatal(rval, msg, (char *) 0);
	}
}

/*
**	reg_delete_key() - delete a registry key, checking for some errors
**
**      this function is successful if the specified key could be deleted
**	or did not exist
**
**	there is no return for other errors
*/
static void reg_delete_key(key_name)
char *key_name;
{
	DWORD rval;
	static char  fmt[] = "RegDeleteKey(\"%.128s\") failed";
	char msg[sizeof fmt + 128];

	rval = RegDeleteKey(HKEY_LOCAL_MACHINE, key_name);

	switch (rval) {
	case ERROR_SUCCESS:
	case ERROR_FILE_NOT_FOUND:
		break;
	default:
		(void) sprintf(msg, fmt, key_name);
		w32fatal(rval, msg, (char *) 0);
		break;
	}
}

/*
**	reg_set_string_value() - add a string value to a registry key,
**		checking for errors
**
**	there is no return on error
*/
static void reg_set_string_value(key_name, key_handle, name, value)
char *key_name;
HKEY key_handle;
char *name, *value;
{
	LONG rval;

	rval = RegSetValueEx(key_handle, name, 0, REG_SZ,
		(CONST BYTE *) value, strlen(value) + 1);

	if (rval != ERROR_SUCCESS)
		reg_set_fatal(key_name, rval, name);
}

/*
**	reg_set_multistring_value() - add a multistring value to a registry key,
**		checking for errors
**
**	there is no return on error
*/
static void reg_set_multistring_value(key_name, key_handle, name, valp, nval)
char *key_name;
HKEY key_handle;
char *name, **valp;
int nval;
{
	LONG rval;
	char *bp, *p, **pp;
	int buflen, size, space;

	if (nval <= 0)
		return;

	buflen = 1;
	for (pp = valp; pp < valp + nval; pp++)
		buflen += strlen(*pp) + 1;

	errno = 0;
	if ((bp = (char *) malloc(buflen)) == (char *) 0)
		fatal(errno, "can't allocate memory for registry strings",
			(char *) 0);

	p = bp;
	space = buflen;
	for (pp = valp; pp < valp + nval; pp++) {
		(void) sprintf(p, "%.*s", space - 1, *pp);
		size = strlen(p) + 1;
		p += size;
		if ((space -= size) <= 1)
			break;
	}
	*p = '\0';

	rval = RegSetValueEx(key_handle, name, 0, REG_MULTI_SZ,
		(CONST BYTE *) bp, (DWORD) buflen);

	free((void *) bp);

	if (rval != ERROR_SUCCESS)
		reg_set_fatal(key_name, rval, name);
}

/*
**	reg_set_dword_value() - add a DWORD value to a registry key,
**		checking for errors
**
**	there is no return on error
*/
static void reg_set_dword_value(key_name, key_handle, name, value)
char *key_name;
HKEY key_handle;
char *name;
DWORD value;
{
	LONG rval;

	rval = RegSetValueEx(key_handle, name, 0, REG_DWORD,
		(CONST BYTE *) &value, sizeof value);

	if (rval != ERROR_SUCCESS)
		reg_set_fatal(key_name, rval, name);
}

/*
**	reg_set_fatal() - common fatal error handler for the reg_set_*
**		functions
**
**	thsi function does not return
*/
static void reg_set_fatal(key_name, rval, name)
char *key_name;
LONG rval;
char *name;
{
	static char fmt[] = "RegSetValueEx(\"%.128s\", \"%.16s\") failed";
	char msg[sizeof fmt + 128 + 16];

	(void) sprintf(msg, fmt, key_name, name);
	w32fatal(rval, msg, (char *) 0);
}

/*
**      rm_empty_key_path() - remove empty keys leading to the specified
**		key path
**
**      note that this function is only safe to use with keys below
**      HKEY_LOCAL_MACHINE\SOFTWARE, or other system keys that have only
**	one level below the top level
**
**      note that the string at *key_path is modified and then restored
**	before the function returns, so it should not be a string literal
**
**	note that this function may be called recursively
*/
static void rm_empty_key_path(key_path)
char *key_path;
{
	char *p;
	LONG rval;
	HKEY key_handle;
	DWORD nsubkeys;

	/* find the right-most key path separator */
	if ((p = strrchr(key_path, '\\')) == (char *) 0) {
		/* we don't want to remove SOFTWARE (the top level key) */
		return;
	}

	rval = RegOpenKeyEx(HKEY_LOCAL_MACHINE, key_path, 0, KEY_ALL_ACCESS,
		&key_handle);
	if (rval == ERROR_SUCCESS) {
		rval = RegQueryInfoKey(key_handle, NULL, NULL, NULL, &nsubkeys,
			NULL, NULL, NULL, NULL, NULL, NULL, NULL);
		reg_close_key(key_path, key_handle);
		if (rval == ERROR_SUCCESS && nsubkeys == 0) {
			reg_delete_key(key_path);
			*p = '\0';
			rm_empty_key_path(key_path);
			*p = '\\';
		}
	}
}


/************************************************************************
*									*
*	utility functions						*
*									*
************************************************************************/

/*
**	rstrdup() - reliable _strdup() call
*/
static char *rstrdup(s)
char *s;
{
	char *p;

	errno = 0;
	if ((p = _strdup(s)) == (char *) 0)
		fatal(errno, "can't allocate memory for string", (char *) 0);

	return(p);
}

/*
**	basename() - return a pointer to the last component of a path name
*/
static char *basename(path)
char *path;
{
	register char *p;
	register char *retval = path;

	if (path)
		for (p = path; *p; p++)
			if (isdirsep(*p) && *(p + 1))
				retval = p + 1;

	return(retval);
}

/*
**	dirname() - copy all but the last component of a path name
**		into a caller-supplied buffer
*/
static void dirname(path, dir, dirlen)
char *path, dir[];
int dirlen;
{
	register int len;

	if ((len = basename(path) - path - 1) == 0 && isdirsep(*path))
		len++;

	if (len <= 0) {
		path = ".";
		len = 1;
	}

	(void) sprintf(dir, "%.*s", tet_min(len, dirlen - 1), path);
}

/*
**	w32getopt() - getopt() for Win32 systems
*/
static int w32getopt(argc, argv, optstring)
int argc;
char **argv;
char *optstring;
{
	static int avplace;
	char *ap;
	char *cp;
	int c;

	if (w32optind >= argc)
		return(EOF);

	ap = argv[w32optind] + avplace;

	/* At begining of arg but not an option */
	if (avplace == 0) {
		if (ap[0] != '-')
			return(EOF);
		else if (ap[1] == '-') {
			/* Special end of options option */
			w32optind++;
			return(EOF);
		} else if (ap[1] == '\0')
			return(EOF);	/* single '-' is not allowed */
	}

	/* Get next letter */
	avplace++;
	c = *++ap;

	cp = strchr(optstring, c);
	if (cp == NULL || c == ':') {
		if (w32opterr)
			fprintf(stderr, "Unrecognised option -- %c\n", c);
		return('?');
	}

	if (cp[1] == ':') {
		/* There should be an option arg */
		avplace = 0;
		if (ap[1] == '\0') {
			/* It is a separate arg */
			if (++w32optind >= argc) {
				if (w32opterr)
					fprintf(stderr, "Option requires an argument\n");
				return('?');
			}
			w32optarg = argv[w32optind++];
		} else {
			/* is attached to option letter */
			w32optarg = ap + 1;
			++w32optind;
		}
	} else {

		/* If we are out of letters then go to next arg */
		if (ap[1] == '\0') {
			++w32optind;
			avplace = 0;
		}

		w32optarg = NULL;
	}

	return(c);
}

/*
**	errorfunc() - report a system or winsock error
*/
static void errorfunc(file, line, err, s1, s2)
char *file, *s1, *s2;
int line, err;
{
	char buf[MAXPATH];
	char *p;
	int space, len;;

	p = buf;
	space = sizeof buf - 1;

	(void) sprintf(p, "tccdsrv (%s, %d): ", file, line);
	len = strlen(p);
	p += len;
	space -= len;

	if (space > 0) {
		(void) sprintf(p, "%.*s", space, s1);
		len = strlen(p);
		p += len;
		space -= len;
	}

	if (space > 1 && s2 && *s2) {
		(void) sprintf(p, " %.*s", space - 1, s2);
		len = strlen(p);
		p += len;
		space -= len;
	}

	if (space > 2 && err > 0) {
		(void) sprintf(p, ": %.*s", space - 2,
			err >= WSABASEERR ? wsaerrmsg(err) : strerror(err));
		len = strlen(p);
		p += len;
		space -= len;
	}

	logfunc(buf);
}

/*
**	fatalfunc() - fatal error handler for fatal system or winsock errors
*/
static void fatalfunc(file, line, err, s1, s2)
char *file, *s1, *s2;
int line, err;
{
	errorfunc(file, line, err, s1, s2);
	cleanup(1);
}

/*
**	w32errorfunc() - report a win32 error
*/
static void w32errorfunc(file, line, err, s1, s2)
char *file, *s1, *s2;
int line, err;
{
	char buf[MAXPATH];
	char *p;
	int space, len;;

	p = buf;
	space = sizeof buf - 1;

	(void) sprintf(p, "tccdsrv (%s, %d): ", file, line);
	len = strlen(p);
	p += len;
	space -= len;

	if (space > 0) {
		(void) sprintf(p, "%.*s", space, s1);
		len = strlen(p);
		p += len;
		space -= len;
	}

	if (space > 1 && s2 && *s2) {
		(void) sprintf(p, " %.*s", space - 1, s2);
		len = strlen(p);
		p += len;
		space -= len;
	}

	if (space > 2 && err != 0) {
		*p++ = ':';
		*p++ = ' ';
		space -= 2;
		FormatMessage(FORMAT_MESSAGE_FROM_SYSTEM, NULL, err,
			MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
			p, space, NULL);
		len = strlen(p);
		p += len;
		space -= len;
	}

	logfunc(buf);
}

/*
**	w32fatalfunc() - fatal error handler for fatal win32 errors
*/
static void w32fatalfunc(file, line, err, s1, s2)
char *file, *s1, *s2;
int line, err;
{
	w32errorfunc(file, line, err, s1, s2);
	cleanup(1);
}

/*
**	logfunc() - write a message to the event log or to stderr
**
**	note that this function can be called recursively
*/
static void logfunc(s)
char *s;
{
	char *msgs[1];
	int iss, rc;

	DEBUG((s));

	/*
	** write the message to stderr -
	** if we think we are running as a service
	** this is just just in case we have been invoked from a console
	** by mistake!
	*/
	(void) fprintf(stderr, "%s\n", s);
	(void) fflush(stderr);

	/* return now if we're not running as a service */
	if (!is_service)
		return;

	/* open the event log first time through */
	if (ev_handle == (HANDLE) -1) {
		ev_handle = RegisterEventSource(NULL, basename(ev_key));
		if (ev_handle == NULL) {
			iss = is_service;
			is_service = 0;
			w32error(GetLastError(),
				"RegisterEventSource() failed", (char *) 0);
			logfunc(s);
			is_service = iss;
			cleanup(1);
		}
	}

	/* print the message to the event log */
	msgs[0] = s;
	rc = ReportEvent(ev_handle, EVENTLOG_INFORMATION_TYPE, 0,
		TET_INFO_MSG, NULL, sizeof msgs / sizeof msgs[0], 0,
		msgs, NULL);
	if (rc == FALSE) {
		iss = is_service;
		is_service = 0;
		w32error(GetLastError(), "ReportEventLog() failed", (char *) 0);
		logfunc(s);
		is_service = iss;
		cleanup(1);
	}
}

/*
**	wsaerrmsg() - return a printable representation of a
**		Winsock error code
*/
static char *wsaerrmsg(wsa_errnum)
int wsa_errnum;
{
	static char fmt[] = "Error %d";
	static char msg[sizeof fmt + LNUMSZ];

	switch (wsa_errnum) {
	case WSAEACCES:
		return("Permission denied");
	case WSAEADDRINUSE:
		return("Address already in use");
	case WSAEADDRNOTAVAIL:
		return("Can't assign requested address");
	case WSAEAFNOSUPPORT:
		return("Address family not supported by protocol family");
	case WSAEALREADY:
		return("Operation already in progress");
	case WSAEBADF:
		return("Bad file number");
	case WSAECONNABORTED:
		return("Software caused connection abort");
	case WSAECONNREFUSED:
		return("Connection refused");
	case WSAECONNRESET:
		return("Connection reset by peer");
	case WSAEDESTADDRREQ:
		return("Destination address required");
	case WSAEDISCON:
		return("Connection has been disconnected gracefully");
	case WSAEDQUOT:
		return("Disk quota exceeded");
	case WSAEFAULT:
		return("Bad address");
	case WSAEHOSTDOWN:
		return("Host is down");
	case WSAEHOSTUNREACH:
		return("Host is unreachable");
	case WSAEINPROGRESS:
		return("Operation now in progress");
	case WSAEINTR:
		return("Interrupted system call");
	case WSAEINVAL:
		return("Invalid argument");
	case WSAEISCONN:
		return("Socket is already connected");
	case WSAELOOP:
		return("Too many levels of symbolic links");
	case WSAEMFILE:
		return("Too many open files");
	case WSAEMSGSIZE:
		return("Message too long");
	case WSAENAMETOOLONG:
		return("File name too long");
	case WSAENETDOWN:
		return("Network is down");
	case WSAENETRESET:
		return("Network dropped connection on reset");
	case WSAENETUNREACH:
		return("Network is unreachable");
	case WSAENOBUFS:
		return("No buffer space available");
	case WSAENOPROTOOPT:
		return("Option not supported by protocol");
	case WSAENOTCONN:
		return("Socket is not connected");
	case WSAENOTEMPTY:
		return("Directory not empty");
	case WSAENOTSOCK:
		return("Socket operation on non-socket");
	case WSAEOPNOTSUPP:
		return("Operation not supported on socket");
	case WSAEPFNOSUPPORT:
		return("Protocol family not supported");
	case WSAEPROCLIM:	/* not documented anywhere ?? */
		return("WSAEPROCLIM");
	case WSAEPROTONOSUPPORT:
		return("Protocol not supported");
	case WSAEPROTOTYPE:
		return("Protocol wrong type for socket");
	case WSAEREMOTE:
		return("Too many levels of remote path");
	case WSAESHUTDOWN:
		return("Can't send after socket shutdown");
	case WSAESOCKTNOSUPPORT:
		return("Socket type not supported");
	case WSAESTALE:
		return("Stale NFS file handle");
	case WSAETIMEDOUT:
		return("Connection timed out");
	case WSAETOOMANYREFS:
		return("Too many references: can't splice");
	case WSAEUSERS:
		return("Too many users");
	case WSAEWOULDBLOCK:
		return("Operation would block");
	case WSAHOST_NOT_FOUND:
		return("Host not found");
	case WSANOTINITIALISED:
		return("Winsock not initialised");
	case WSANO_DATA:
		return("Valid name but no data");
	case WSANO_RECOVERY:
		return("Unrecoverable error");
	case WSASYSNOTREADY:
		return("Network subsystem is not usable");
	case WSATRY_AGAIN:
		return("Try again");
	case WSAVERNOTSUPPORTED:
		return("Winsock version not supported");
	default:
		(void) sprintf(msg, fmt, wsa_errnum);
		return(msg);
	}
}

#ifdef TRACE

/*
**	debug() - print a debug message
*/
static void debug(char *fmt, ...)
{
	static char tracefile[] = TRACEFILE;
	static FILE *tfp;
	va_list ap;
	int save_errno = errno;

	if (!tfp && (tfp = fopen(tracefile, "a")) == NULL)
		fatal(errno, "can't open", tracefile);

	(void) fprintf(tfp, "tccdsrv (%d.%ld): ",
		_getpid(), (long) GetCurrentThreadId());

	va_start(ap, fmt);
	(void) vfprintf(tfp, fmt, ap);
	(void) putc('\n', tfp);
	(void) fflush(tfp);
	va_end(ap);

	errno = save_errno;
}

/*
**	pr_service_state() - return a printable representation of a
**		service state
*/
static char *pr_service_state(state)
int state;
{
	static char fmt[] = "(state %d)";
	static char buf[sizeof fmt + LNUMSZ];

	switch (state) {
	case SERVICE_STOPPED:
		return("STOPPED");
	case SERVICE_START_PENDING:
		return("START_PENDING");
	case SERVICE_STOP_PENDING:
		return("STOP_PENDING");
	case SERVICE_RUNNING:
		return("RUNNING");
	case SERVICE_CONTINUE_PENDING:
		return("CONTINUE_PENDING");
	case SERVICE_PAUSE_PENDING:
		return("PAUSE_PENDING");
	case SERVICE_PAUSED:
		return("PAUSED");
	default:
		(void) sprintf(buf, fmt, state);
		return(buf);
	}
}

#endif /* TRACE */

/*
**	cleanup() - clean up and exit
*/
static void cleanup(status)
int status;
{
	DEBUG(("call to cleanup(%d), is_service = %d, ev_handle = %#lx",
		status, is_service, (long) ev_handle));

	/* close the event log */
	if (ev_handle != (HANDLE) -1 && ev_handle != NULL)
		(void) DeregisterEventSource(ev_handle);

	DEBUG(("cleanup(): about to call exit(%d)", status));
	exit(status);
}


#else			/* -END-WIN32-CUT- */

int main()
{
	static char *text[] = {
		"tccdsrv is not used on this type of system.\n",
		"Please refer to the TETware Installation and User Guide",
		"for details on how to start tccd on your system."
	};

	char **tp;

	for (tp = text; tp < &text[sizeof text / sizeof text[0]]; tp++)
		(void) fprintf(stderr, "%s\n", *tp);

	return(1);
}

#endif			/* -WIN32-CUT-LINE- */

