/*
 *      SCCS:  @(#)kshapid.c	1.2 (04/11/26)
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1999 UniSoft Ltd.
 *
 * All rights reserved.  No part of this source code may be reproduced,
 * stored in a retrieval system, or transmitted, in any form or by any
 * means, electronic, mechanical, photocopying, recording or otherwise,
 * except as stated in the end-user licence agreement, without the prior
 * permission of the copyright owners.
 */

#ifndef lint
static char sccsid[] = "@(#)kshapid.c	1.2 (04/11/26) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)kshapid.c	1.2 04/11/26
NAME:		kshapid.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	May 1999

DESCRIPTION:
	distributed ksh API helper process

MODIFICATIONS:

	Geoff Clare, November 2004
		Updated for TETware 3.7.
 
************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#ifndef _WIN32  /* -WIN32-CUT-LINE- */
#  include <unistd.h>
#endif          /* -WIN32-CUT-LINE- */
#include <signal.h>
#include <ctype.h>
#include <string.h>
#include <errno.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "globals.h"
#include "bstring.h"
#include "tcmfuncs.h"
#include "error.h"
#include "ftoa.h"
#include "ltoa.h"
#include "llist.h"
#include "ptab.h"
#include "server.h"
#include "servlib.h"
#include "synreq.h"
#include "sigsafe.h"
#include "tslib.h"
#include "dtetlib.h"
#include "tet_api.h"
#include "tet_jrnl.h"
#include "apilib.h"


/* reliable BUFCHK() call */
#ifdef NOTRACE
static void rbufchk PROTOLIST((char **, int *, int));
#  define RBUFCHK(bpp, lp, newlen)	rbufchk(bpp, lp, newlen)
#else /* NOTRACE */
static void rbuftrace PROTOLIST((char **, int *, int, char *, int));
#  define RBUFCHK(bpp, lp, newlen) \
	rbuftrace(bpp, lp, newlen, srcFile, __LINE__)
#  ifndef NEEDsrcFile
#    define NEEDsrcFile
#  endif /* NEEDsrcFile */
#endif /* NOTRACE */

/* macros to send reply messages */
#define REPLY1(rc)		REPLY2(rc, (char *) 0)
#define REPLY2(rc, s1)		REPLY3(rc, s1, (char *) 0)
#define REPLY3(rc, s1, s2)	REPLY4(rc, s1, s2, (char *) 0)
#define REPLY4(rc, s1, s2, s3)	reply(rc, s1, s2, s3)
#define REPLY_DATA1(s1) 	REPLY_DATA2(s1, (char *) 0)
#define REPLY_DATA2(s1, s2) 	reply_data(s1, s2)
#define INVALID_PARAM1(rc)	INVALID_PARAM2(rc, (char *) 0)
#define INVALID_PARAM2(rc, s1)	INVALID_PARAM3(rc, s1, (char *) 0)
#define INVALID_PARAM3(rc, s1, s2) \
				INVALID_PARAM4(rc, s1, s2, (char *) 0)
#define INVALID_PARAM4(rc, s1, s2, s3) \
				invalid_param(rc, s1, s2, s3)


/*
** structure of the line cache stack used by getline() and ungetline()
** this is a linked list
** the lc_next and lc_last elements must be 1st and 2nd so as to
** enable the stack to be manipulated by the llist functions
*/
struct lcache {
        struct lcache *lc_next;		/* ptr to next element in the list */
        struct lcache *lc_last;		/* ptr to prev element in the list */
        char *lc_line;          	/* the stored input line */
};
static struct lcache *lcache;		/* the line cache stack itself */


/* global variables used by various library functions */
struct ptab *tet_sdptab, *tet_xdptab;	/* ptab elements for syncd and xresd */

#ifndef _WIN32  /* -WIN32-CUT-LINE- */
sigset_t tet_blockable_sigs;		/* signals blocked by
					   tet_sigsafe_start() */
#endif          /* -WIN32-CUT-LINE- */


/* static variables */
#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

#undef TET_VERSION
#define TET_VERSION	"3.8"

static char tet_version[] = TET_VERSION;/* version number - used to check the
					   ksh API version number */
static int child_tcm;			/* true if this process is attached
					   to a child TCM/API */
static struct synreq *synreq;		/* to report auto-sync errors */
static int done_usync_reply;		/* to coordinate usync replies */
static struct tet_synmsg synmsg;	/* sync message data parameters */
static struct tet_synmsg *synmsgp;


/* static function declarations */
static void badusage PROTOLIST((void));
static int check_master PROTOLIST((char *));
static int find_next_request PROTOLIST((char *, int));
static int fldcount PROTOLIST((char *));
static void flush_output PROTOLIST((void));
static void free_data_lines PROTOLIST((char **, int));
static int get_data_lines PROTOLIST((char ***, int *));
static int getline PROTOLIST((char *, int));
static void invalid_param PROTOLIST((char *, char *, char *, char *));
static int isallnum PROTOLIST((char *));
static struct lcache *lcalloc PROTOLIST((void));
static void lcfree PROTOLIST((struct lcache *));
static struct lcache *lcpop PROTOLIST((void));
static void lcpush PROTOLIST((struct lcache *));
static void process PROTOLIST((char *));
static void reply PROTOLIST((int, char *, char *, char *));
static void reply_data PROTOLIST((char *, char *));
static void req_async PROTOLIST((char *, int, char **));
static void req_context PROTOLIST((char *, int, char **));
static void req_getsysbyid PROTOLIST((char *, int, char **));
static void req_icend PROTOLIST((char *, int, char **));
static void req_icstart PROTOLIST((char *, int, char **));
static void req_merror PROTOLIST((char *, int, char **));
static void req_minfoline PROTOLIST((char *, int, char **));
static void req_result PROTOLIST((char *, int, char **));
static void req_setblock PROTOLIST((char *, int, char **));
static void req_shutdown PROTOLIST((char *, int, char **));
static void req_tcmstart PROTOLIST((char *, int, char **));
static void req_thistest PROTOLIST((char *, int, char **));
static void req_tpend PROTOLIST((char *, int, char **));
static void req_tpstart PROTOLIST((char *, int, char **));
static void req_us2 PROTOLIST((long, int, int, char **, int));
static void req_usync PROTOLIST((char *, int, char **));
static char *rstrstore PROTOLIST((char *));
static char *smflags PROTOLIST((int));
static char *sp2us PROTOLIST((char *));
static void syncerr PROTOLIST((long, struct tet_syncstat *, int));
static void ungetline PROTOLIST((char *));
static void usync_reply(int, struct tet_syncstat *, int);

#ifdef NOTRACE
static void rbufchk PROTOLIST((char **, int *, int));
#else
static void rbuftrace PROTOLIST((char **, int *, int, char *, int));
#endif



/* extern function declarations */
extern void tet_dtcmerror PROTOLIST((int, char *, int, char *, char *));


/* the request structure */
struct request {
	char *re_name;			/* request name */
	void (*re_func) PROTOLIST((char *, int, char **));
					/* request handler function */
};

static struct request request[] = {
	{ "tet_async", req_async },
	{ "tet_context", req_context },
	{ "tet_getsysbyid", req_getsysbyid },
	{ "tet_icend", req_icend },
	{ "tet_icstart", req_icstart },
	{ "tet_merror", req_merror },
	{ "tet_minfoline", req_minfoline },
	{ "tet_result", req_result },
	{ "tet_setblock", req_setblock },
	{ "tet_shutdown", req_shutdown },
	{ "tet_tcmstart", req_tcmstart },
	{ "tet_thistest", req_thistest },
	{ "tet_tpend", req_tpend },
	{ "tet_tpstart", req_tpstart },
	{ "tet_usync", req_usync }
};
static int Nrequest = sizeof request / sizeof request[0];



int main(argc, argv)
int argc;
char **argv;
{
	char line[BUFSIZ];
	char *p;
	int c;

	/* must be first */
	tet_api_status |= TET_API_INITIALISED;
	tet_pname = "tetkshapid";
	tet_init_globals(tet_pname, -1, -1, tet_dtcmerror, tet_genfatal);
	tet_init_blockable_sigs();

#ifndef NOTRACE
	/* initialise the trace subsystem from -T command-line options */
	tet_traceinit(argc, argv);
#endif

	/* process the command-line arguments */
	while ((c = GETOPT(argc, argv, "T:Vc")) != EOF)
	switch (c) {
	case 'T':       /* trace options -
			   already processed by tet_traceinit() */
		break;
	case 'V':	/* print version information */
		(void) fprintf(stderr, "version %s\n", tet_version);
		tet_exit(0);
		break;
	case 'c':	/* this process is attached to a child TCM/API */
		child_tcm = 1;
		break;
	default:
		badusage();
		break;
	}

	/* replace any white space in tet_version[] */
	(void) sp2us(tet_version);

	/* set tet_activity from the environment */
	if ((p = getenv("TET_ACTIVITY")) == (char *) 0 || !*p)
		tet_activity = 0;
	else
		tet_activity = atol(p);

	/* read in the result code file if there is one */
	p = getenv("TET_CODE");
	if (p && *p && tet_readrescodes(p) < 0)
		tet_exit(1);

	/* initialise the server and transport stuff */
	tet_tcminit(argc, argv);

	/* allocate memory for the synreq array */
	errno = 0;
	if ((synreq = (struct synreq *) malloc(sizeof *synreq * tet_Nsname)) == (struct synreq *) 0) {
		tet_error(errno, "can't allocate memory for synreq array");
		tet_exit(1);
	}
	TRACE2(tet_Tbuf, 6, "allocate synreq = %s", tet_i2x(synreq));

	/* install a user-sync error handler */
	tet_syncerr = syncerr;

	/* detatch from the controlling terminal */
	tet_tiocnotty();

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	/*
	** ignore SIGPIPE so that we can detect when the TCM/API process
	** dies unexpectedly via the EPIPE error
	*/
	(void) signal(SIGPIPE, SIG_IGN);
#endif		/* -WIN32-CUT-LINE- */

	/* run the main loop */
	while (find_next_request(line, sizeof line) != EOF)
		process(line);

	tet_exit(0);

	/* NOTREACHED */
	return(0);
}

/*
**	badusage() - print a usage message and exit
*/

static void badusage()
{
	(void) fprintf(stderr, "usage: %s [-c] [-V]\n", tet_pname);
	tet_exit(2);
	/* NOTREACHED */
}


/************************************************************************
*									*
*	request processing functions					*
*									*
************************************************************************/

/*
**	process() - process a request line
*/

static void process(line)
char *line;
{
	static char *buf;
	static int buflen;
	static char **flds;
	static int fldslen;
	static char *errbuf;
	static int eblen;
	int Nflds;
	int n;
	struct request *rp;
	static char fmt1[] = "api version mismatch: expected %s, found";
	static char fmt2[] = "unknown request: %s";
#ifndef NOTRACE
	char **ap;
#endif

	/* copy the line to a scratchpad, growing it if necessary */
	RBUFCHK(&buf, &buflen, (int) strlen(line) + 1);
	(void) strcpy(buf, line);

	/*
	** count the number of fields in the input line
	** and allocate a large enough flds array
	*/
	n = fldcount(line);
	RBUFCHK((char **) &flds, &fldslen, n * (int) sizeof *flds);

	/* split the input line into fields */
	if ((Nflds = tet_getargs(buf, flds, n)) == 0)
		return;

	/* check the api version number */
	if (strcmp(*(flds + 1), tet_version)) {
		RBUFCHK(&errbuf, &eblen,
			(int) sizeof fmt1 + (int) strlen(tet_version));
		(void) sprintf(errbuf, fmt1, tet_version);
		error(0, errbuf, flds[1]);
		REPLY1(ER_MAGIC);
		tet_exit(1);
	}

	TRACE2(tet_Ttcm, 1, "REQUEST = %s",
		Nflds > 2 ? *(flds + 2) : "<none>");

#ifndef NOTRACE
	if (tet_Ttcm) {
		TRACE2(tet_Ttcm, 6, "request has %s fields", tet_i2a(Nflds));
		for (ap = flds; ap < flds + Nflds; ap++)
			TRACE3(tet_Ttcm, 6, "field %s = \"%s\"",
				tet_i2a((((int) (ap - flds)) + 1)), *ap);
	}
#endif

	/* process the request */ 
	if (Nflds > 2) {
		for (rp = request; rp < &request[Nrequest]; rp++)
			if (!strcmp(rp->re_name, *(flds + 2))) {
				(*rp->re_func)(rp->re_name, Nflds - 3, flds + 3);
				return;
			}
	}
	else {
		REPLY1(ER_OK);
		return;
	}

	/* here when the request is unknown */
	RBUFCHK(&errbuf, &eblen,
		(int) sizeof fmt2 + (int) strlen(*(flds + 2)));
	(void) sprintf(errbuf, fmt2, *(flds + 2));
	tet_error(ER_REQ, errbuf);
	REPLY1(ER_REQ);
}

/*
**	req_thistest() - process a tet_thistest request
**
**	request is:
**	thistest-value
**
**	reply is:
**	reply-code
*/

static void req_thistest(request, argc, argv)
char *request;
int argc;
char **argv;
{
	/* perform a sanity check on the request */
	if (argc != 1 || !isallnum(*argv)) {
		INVALID_PARAM1(request);
		return;
	}

	/* set tet_thistest */
	tet_thistest = atoi(*argv);
	TRACE2(tet_Ttcm, 3, "THISTEST: thistest = %s",
		tet_i2a(tet_thistest));
	REPLY1(ER_OK);
}

/*
**	req_context() - process a tet_context request
**
**	request is:
**	context-value
**
**	reply is:
**	reply-code
*/

static void req_context(request, argc, argv)
char *request;
int argc;
char **argv;
{
	/* perform a sanity check on the request */
	if (argc != 1 || !isallnum(*argv)) {
		INVALID_PARAM1(request);
		return;
	}

	/* set the context, reset the block and sequence numbers */
	tet_context = atol(*argv) % 10000000L;
	TRACE2(tet_Ttcm, 3, "CONTEXT: context = %s", tet_l2a(tet_context));
	tet_block = 1;
	tet_sequence = 1;
	REPLY1(ER_OK);
}

/*
**	req_setblock() - process a tet_setblock request
**
**	there are no parameters to this request
**
**	reply is:
**	reply-code
*/

/* ARGSUSED */
static void req_setblock(request, argc, argv)
char *request;
int argc;
char **argv;
{
	/* perform a sanity check on the request */
	if (argc != 0) {
		INVALID_PARAM1(request);
		return;
	}

	/* just call the API function */
	tet_setblock();
	REPLY1(ER_OK);
}

/*
**	req_minfoline() - process a tet_minfoline request
**
**	request is:
**	number-of-infolines
**	infoline ...
**
**	reply is:
**	reply-code
*/

static void req_minfoline(request, argc, argv)
char *request;
int argc;
char **argv;
{
	char **lines;
	int nlines;
	int rc;
#ifndef NOTRACE
	char **lp;
#endif

	/* perform a sanity check on the request */
	if (argc != 1 || !isallnum(*argv)) {
		INVALID_PARAM1(request);
		return;
	}

	/* read in all the infolines */
	nlines = atoi(*argv);
	TRACE2(tet_Ttcm, 3, "MINFOLINE: nlines = %s", tet_i2a(nlines));
	if ((rc = get_data_lines(&lines, &nlines)) != ER_OK) {
		REPLY1(rc);
		return;
	}

#ifndef NOTRACE
	if (tet_Ttcm)
		for (lp = lines; lp < lines + nlines; lp++)
			TRACE2(tet_Ttcm, 4, "MINFOLINE: line = \"%s\"", *lp);
#endif

	/* call the API function to do the work */
	if (tet_minfoline(lines, nlines) < 0)
		rc = -tet_errno;

	/* finally, free up storage allocated in get_data_lines() and return */
	free_data_lines(lines, nlines);
	REPLY1(rc);
}

/*
**	req_result() - process a tet_result request
**
**	request is:
**	number-of-result-name-lines (should always be 1)
**	result-name
**
**	reply is:
**	reply-code
*/

static void req_result(request, argc, argv)
char *request;
int argc;
char **argv;
{
	char **lines;
	int nlines;
	int rc;
	int result;
	static char fmt[] = "invalid result name: %.24s";
	char msg[sizeof fmt + 24];

	/* perform a sanity check on the request */
	if (argc != 1 || !isallnum(*argv)) {
		INVALID_PARAM1(request);
		return;
	}

	/* read in the single-line result name */
	nlines = atoi(*argv);
	TRACE2(tet_Ttcm, 3, "RESULT: nlines = %s", tet_i2a(nlines));
	if (nlines == 0) {
		tet_error(ER_INVAL, "nlines parameter cannot be zero in tet_result request");
		REPLY1(ER_INVAL);
		return;
	}
	if ((rc = get_data_lines(&lines, &nlines)) != ER_OK) {
		REPLY1(rc);
		return;
	}

	/* convert the result name to a code */
	if (nlines > 0) {
		TRACE2(tet_Ttcm, 4, "result name = \"%s\"", *lines);
		if ((result = tet_getrescode(*lines, (int *) 0)) < 0) {
			(void) sprintf(msg, fmt, *lines);
			tet_error(ER_INVAL, msg);
			result = TET_NORESULT;
		}
		TRACE2(tet_Ttcm, 8, "result code = %s", tet_i2a(result));
		rc = ER_OK;
	}
	else {
		tet_error(ER_INVAL, "data line missing in tet_result request");
		result = TET_NORESULT;
		rc = ER_INVAL;
	}

	/* call the API function to do the work */
	tet_result(result);

	/* finally, free up storage allocated in get_data_lines() and return */
	free_data_lines(lines, nlines);
	REPLY1(rc);
}

/*
**	req_shutdown() - process a tet_shutdown request
**
**	there are no parameters to this request
**
**	reply is:
**	reply-code
**
**	this function does not return
*/

/* ARGSUSED */
static void req_shutdown(request, argc, argv)
char *request;
int argc;
char **argv;
{
	/* perform a sanity check on the request */
	if (argc != 0) {
		INVALID_PARAM1(request);
		return;
	}

	REPLY1(ER_OK);
	TRACE1(tet_Ttcm, 1, "SHUTDOWN: about to call tet_exit(0)");
	tet_exit(0);
}

/*
**	req_async() - process a tet_async request
**
**	request is:
**	spno sync-vote sync-timeout
**
**	reply is:
**	reply-code system-nlines
**	sysid {spno|sync-state}
**	...
*/

static void req_async(request, argc, argv)
char *request;
int argc;
char **argv;
{
	long spno;
	int nsys, timeout, vote;
	struct synreq *sp;

	/* tokenise the sync vote */
	vote = -1;
	if (argc >= 2) {
		if (!strcmp(*(argv + 1), "YES"))
			vote = SV_YES;
		else if (!strcmp(*(argv + 1), "NO"))
			vote = SV_NO;
	}

	/* perform a sanity check on the request */
	if (
		argc != 3 ||
		!isallnum(*argv) ||
		vote < 0 ||
		!isallnum(*(argv + 2))
	) {
		INVALID_PARAM2(request, "0");
		return;
	}

	/* extract the fixed parameters from the request */
	spno = atol(*argv);
	nsys = synreq ? tet_Nsname : 0;
	timeout = atoi(*(argv + 2));

	TRACE4(tet_Ttcm, 3, "ASYNC: spno = %s, vote = %s, timeout = %s",
		tet_l2a(spno), tet_ptsvote(vote), tet_i2a(timeout));
	TRACE4(tet_Ttcm, 4, "ASYNC: decode spno: ICno = %s, TPno = %s, flag = %s",
		tet_i2a(EX_ICNO(spno)), tet_i2a(EX_TPNO(spno)),
		EX_FLAG(spno) == S_TPEND ? "END" : "START");

	/* do the auto-sync */
	if (tet_tcm_async(spno, vote, timeout, synreq, &nsys) < 0) {
		REPLY2(tet_sderrno, "0");
		return;
	}

	/* generate the reply */
	TRACE2(tet_Ttcm, 3, "ASYNC reply: nsys = %s", tet_i2a(nsys));
	REPLY2(tet_sderrno, tet_i2a(nsys));
	for (sp = synreq; sp < synreq + nsys; sp++) {
		TRACE4(tet_Ttcm, 4, "ASYNC reply: sysid = %s, %s = %s",
			tet_i2a(sp->sy_sysid),
			tet_sderrno == ER_OK ? "spno" : "state",
			tet_sderrno == ER_OK ?
				tet_l2a(sp->sy_spno) :
				tet_systate(sp->sy_state));
#ifndef NOTRACE
		if (tet_Ttcm && tet_sderrno == ER_OK)
			TRACE4(tet_Ttcm, 6, "ASYNC reply: decode spno: ICno = %s, TPno = %s, flag = %s",
				tet_i2a(EX_ICNO(sp->sy_spno)),
				tet_i2a(EX_TPNO(sp->sy_spno)),
				EX_FLAG(sp->sy_spno) == S_TPEND ?
					"END" : "START");
#endif
		REPLY_DATA2(tet_i2a(sp->sy_sysid),
			tet_sderrno == ER_OK ?
				tet_l2a(sp->sy_spno) :
				sp2us(tet_systate(sp->sy_state)));
		}
}

/*
**	req_usync() - process a tet_usync request
**
**	request is:
**	spno sync-vote sync-timeout nlines
**	sysid ...
**	sync-message-flag ...
**	sync-message-data-line
**	...
**
**	sync-message-flags are only specified when sync message data is
**	to be sent or received
**	sync-message-data-lines are only specified when sync message data
**	is to be sent
**
**
**	reply is:
**	reply-code system-nlines sync-message-data-nlines sync-message-sysid
**	sysid sync-state
**	...
**	sync-message-flag ...
**	sync-message-data-line
**	...
**
**	sync-message-flags are only returned when the caller expected to
**	send or receive sync message data, AND the reply code is OK or SYNCERR
*/

static void req_usync(request, argc, argv)
char *request;
int argc;
char **argv;
{
	char **lines;
	long spno;
	int timeout, vote, nlines, rc;

	/* initialise the global variables */
	bzero((char *) &synmsg, sizeof synmsg);
	synmsg.tsm_sysid = -1;
	synmsgp = (struct tet_synmsg *) 0;

	/* tokenise the sync vote */
	vote = -1;
	if (argc >= 2) {
		if (!strcmp(*(argv + 1), "YES"))
			vote = SV_YES;
		else if (!strcmp(*(argv + 1), "NO"))
			vote = SV_NO;
	}

	/* perform a sanity check on the request */
	if (
		argc != 4 ||
		!isallnum(*argv) ||
		vote < 0 ||
		!isallnum(*(argv + 2)) ||
		!isallnum(*(argv + 3))
	) {
		INVALID_PARAM4(request, "0", "0", "-1");
		return;
	}

	/* extract the fixed parameters from the request */
	spno = atol(*argv);
	timeout = atoi(*(argv + 2));
	nlines = atoi(*(argv + 3));

	TRACE5(tet_Ttcm, 3,
		"USYNC: spno = %s, vote = %s, timeout = %s, nlines = %s",
		tet_l2a(spno), tet_ptsvote(vote), tet_i2a(timeout),
		tet_i2a(nlines));

	if (nlines == 0) {
		tet_error(ER_INVAL, "nlines parameter cannot be zero in tet_usync request");
		REPLY4(ER_INVAL, "0", "0", "-1");
		return;
	}

	/* retrieve the accompanying paramater lines */
	if ((rc = get_data_lines(&lines, &nlines)) != ER_OK) {
		REPLY4(rc, "0", "0", "-1");
		return;
	}

	/*
	** perform the rest of the processing and free the storage
	** allocated in get_data_lines()
	*/
	req_us2(spno, vote, timeout, lines, nlines);
	free_data_lines(lines, nlines);
}

/*
**	req_us2() - extend the req_usync() processing
*/

static void req_us2(spno, vote, timeout, lines, nlines)
long spno;
int vote, timeout, nlines;
char **lines;
{
	static char **flds;
	static int lflds;
	int nflds;
	static int *sysnames;
	static int lsysname;
	int nsysname;
	int n;
	char *p1, *p2;
	char smdata[TET_SMMSGMAX + 1];	/* one more than the max so as to
					   force a TET_SMTRUNC condition on
					   overflow */
	char buf[80];
	char *msgs[2];
	static char fmt[] = "invalid sync message data flag in tet_usync request: %.24s";
	char msg[sizeof fmt + 24];

	/* ensure that we have a line containing the system list */
	if (nlines < 1) {
		tet_error(ER_INVAL, "system list line missing in tet_usync request");
		REPLY4(ER_INVAL, "0", "0", "-1");
		return;
	}

	TRACE2(tet_Ttcm, 3, "system list = \"%s\"", *lines);

	/* extract the system list from the request */
	if ((nflds = fldcount(*lines)) == 0) {
		tet_error(ER_INVAL, "empty system list line in tet_usync request");
		REPLY4(ER_INVAL, "0", "0", "-1");
		return;
	}
	RBUFCHK((char **) &flds, &lflds, nflds * (int) sizeof *flds);
	nsysname = tet_getargs(*lines, flds, nflds);
	RBUFCHK((char **) &sysnames, &lsysname,
		nsysname * (int) sizeof *sysnames);
	for (n = 0; n < nsysname; n++)
		*(sysnames + n) = atoi(*(flds + n));
	nlines--;
	lines++;

	/*
	** extract the sync message flags and sync message data from
	** the request if there is any
	*/
	if (nlines > 0) {
		/* the sync message flags */
		if ((nflds = fldcount(*lines)) > 0) {
			RBUFCHK((char **) &flds, &lflds,
				nflds * (int) sizeof *flds);
			(void) sprintf(buf, "%.*s",
				(int) sizeof buf - 1, *lines);
			if ((nflds = tet_getargs(buf, flds, nflds)) == 1) {
				if (!strcmp(*flds, "SNDMSG"))
					synmsg.tsm_flags =  TET_SMSNDMSG;
				else if (!strcmp(*flds, "RCVMSG"))
					synmsg.tsm_flags = TET_SMRCVMSG;
				else {
					(void) sprintf(msg, fmt, *flds);
					tet_error(ER_INVAL, msg);
					REPLY4(ER_INVAL, "0", "0", "-1");
					return;
				}
			}
			else {
				ASSERT(nflds > 0);
				msgs[0] = "invalid sync message data flags line in tet_usync request:";
				msgs[1] = *lines;
				tet_merror(ER_INVAL, msgs, 2);
				REPLY4(ER_INVAL, "0", "0", "-1");
				return;
			}
		}
		TRACE2(tet_Ttcm, 3, "smflags = %s", smflags(synmsg.tsm_flags));
		/* the sync message data */
		bzero(smdata, (int) sizeof smdata);
		if (synmsg.tsm_flags == TET_SMSNDMSG) {
			p2 = smdata;
			while (++lines, --nlines > 0) {
				TRACE2(tet_Ttcm, 4,
					"sync message data = \"%s\"",
					*lines);
				for (p1 = *lines; *p1; p1++)
					if (p2 < &smdata[TET_SMMSGMAX])
						*p2++ = *p1;
					else
						break;
				if (p2 <= &smdata[TET_SMMSGMAX])
					*p2++ = '\0';
			}
			synmsg.tsm_dlen = (int) (p2 - smdata);
		}
		else
			synmsg.tsm_dlen = TET_SMMSGMAX;
		synmsg.tsm_data = smdata;
		synmsgp = &synmsg;
	}

	/* call the API function to do the work */
	done_usync_reply = 0;
	if (tet_remsync(spno, sysnames, nsysname, timeout, vote, synmsgp) < 0) {
		if (!done_usync_reply) {
			synmsgp = (struct tet_synmsg *) 0;
			usync_reply(-tet_errno, (struct tet_syncstat *) 0, 0);
		}
		return;
	}

	usync_reply(ER_OK, (struct tet_syncstat *) 0, 0);
}

/*
**	usync_reply() - send a user-sync reply
**
**	since this function can be called from the request processing
**      function or by the API via the user sync error handler, the
**      sync message data information is passed in via the global
**	variable syncmsgp
*/

static void usync_reply(rc, statp, nstat)
int rc, nstat;
struct tet_syncstat *statp;
{
	int nlines = 0;
	int smsysid;
	char *p, *s;

	/*
	** if we have received sync message data,
	** count the number of (zero-terminated) data lines
	*/
	if (
		(rc == ER_OK || rc == ER_SYNCERR) &&
		synmsgp &&
		(synmsgp->tsm_flags & TET_SMRCVMSG) &&
		synmsgp->tsm_sysid >= 0
	) {
		ASSERT(synmsgp->tsm_dlen <= TET_SMMSGMAX);
		for (p = synmsgp->tsm_data; p < synmsgp->tsm_data + synmsgp->tsm_dlen; p++)
			switch (*p) {
			case '\n':
				*p = '\0';
				/* fall through */
			case '\0':
				nlines++;
				break;
			}
		*p = '\0';
	}

	/* send the reply */
	switch (rc) {
	case ER_OK:
	case ER_SYNCERR:
		if (synmsgp) {
			smsysid = synmsgp->tsm_sysid;
			break;
		}
		/* else fall through */
	default:
		smsysid = -1;
		break;
	}
	if (!statp)
		nstat = 0;
	TRACE4(tet_Ttcm, 3, "USYNC reply: nstat = %s, smdlines = %s, smsysid = %s",
		tet_i2a(nstat), tet_i2a(nlines), tet_i2a(smsysid));
	REPLY4(rc, tet_i2a(nstat), tet_i2a(nlines), tet_i2a(smsysid));

	/* write out the sync state data if there is any */
	if (statp)
		while (--nstat >= 0) {
			TRACE3(tet_Ttcm, 3, "USYNC reply: sysid = %s, state = %s",
				tet_i2a(statp->tsy_sysid),
				tet_systate(statp->tsy_state));
			REPLY_DATA2(tet_i2a(statp->tsy_sysid),
				sp2us(tet_systate(statp->tsy_state)));
			statp++;
		}

	/* return now if we did not want to send/receive sync message data */
	switch (rc) {
	case ER_OK:
	case ER_SYNCERR:
		if (synmsgp)
			break;
		/* else fall through */
	default:
		return;
	}

	/* write out the sync message flags */
	s = smflags(synmsgp->tsm_flags);
	for (p = s; *p; p++)
		if (*p == '|')
			*p = ' ';
	TRACE2(tet_Ttcm, 3, "USYNC reply: sync message flags = \"%s\"", s);
	REPLY_DATA1(s);

	/* write out the sync message lines */
	if ((synmsgp->tsm_flags & TET_SMRCVMSG) && synmsgp->tsm_sysid >= 0) {
		p = synmsgp->tsm_data;
		while (
			--nlines >= 0 &&
			p <= synmsgp->tsm_data + synmsgp->tsm_dlen
		) {
			TRACE2(tet_Ttcm, 4, "USYNC reply: sync message data = \"%s\"", p);
			REPLY_DATA1(p);
			p += strlen(p) + 1;
		}
	}
}

/*
**	syncerr() - the user-sync error handler
*/
 
/* ARGSUSED */
static void syncerr(spno, statp, nstat)
long spno;
struct tet_syncstat *statp;
int nstat;
{
	usync_reply(-tet_errno, statp, nstat);
	done_usync_reply = 1;
}

/*
**	req_tcmstart() - send a TCM start message to XRESD
**
**	request is:
**	iccount number-of-version-string-lines (should always be 1)
**	version-string
**
**	reply is:
**	reply-code
*/

static void req_tcmstart(request, argc, argv)
char *request;
int argc;
char **argv;
{
	char buf[128];
	char **lines;
	char *version;
	int iccount, nlines, rc;

	if (check_master(request) < 0)
		return;

	/* perform a sanity check on the request */
	if (argc != 2 || !isallnum(*argv) || !isallnum(*(argv + 1))) {
		INVALID_PARAM1(request);
		return;
	}

	/* extract the fixed parameters from the request */
	iccount = atoi(*argv);
	nlines = atoi(*(argv + 1));

	TRACE3(tet_Ttcm, 3, "iccount = %s, nlines = %s",
		tet_i2a(iccount), tet_i2a(nlines));

	/* read in the single-line version string */
	if (nlines == 0) {
		tet_error(ER_INVAL, "nlines parameter cannot be zero in tet_tcmstart request");
		REPLY1(ER_INVAL);
		return;
	}
	if ((rc = get_data_lines(&lines, &nlines)) != ER_OK) {
		REPLY1(rc);
		return;
	}

	/* determine the version string to use */
	if (nlines > 0) {
		version = *lines;
		TRACE2(tet_Ttcm, 4, "version = \"%s\"", version);
		rc = ER_OK;
	}
	else {
		tet_error(ER_INVAL,
			"data line missing in tet_tcmstart request");
		version = "<unknown>";
		rc = ER_INVAL;
	}

	/* generate the TCM Start line and send it to XRESD */
	(void) sprintf(buf, "%d|%ld %s %d|TCM Start", 
		TET_JNL_TCM_START, tet_activity, version, iccount);
	if (tet_xdxres(tet_xrid, buf) < 0)
		rc = tet_xderrno;

	REPLY1(rc);
	free_data_lines(lines, nlines);
}

/*
**	req_icstart() - send an IC start message to XRESD
**
**	request is:
**	icno tpcount
**
**	reply is:
**	reply-code
*/

static void req_icstart(request, argc, argv)
char *request;
int argc;
char **argv;
{
	int icno, tpcount;

	if (check_master(request) < 0)
		return;

	/* perform a sanity check on the request */
	if (argc != 2 || !isallnum(*argv) || !isallnum(*(argv + 1))) {
		INVALID_PARAM1(request);
		return;
	}

	/* extract the fixed parameters from the request */
	icno = atoi(*argv);
	tpcount = atoi(*(argv + 1));
	TRACE3(tet_Ttcm, 3, "ICSTART: icno = %s, tpcount = %s",
		tet_i2a(icno), tet_i2a(tpcount));

	/* send the message to XRESD and return */
	(void) tet_xdicstart(tet_xrid, icno, tet_activity, tpcount);
	REPLY1(tet_xderrno);
}

/*
**	req_icend() - send an IC start message to XRESD
**
**	there are no parameters to this request
**
**	reply is:
**	reply-code
*/

static void req_icend(request, argc, argv)
char *request;
int argc;
char **argv;
{
	if (check_master(request) < 0)
		return;

	/* perform a sanity check on the request */
	if (argc != 0) {
		INVALID_PARAM1(request);
		return;
	}

	/* send the message to XRESD and return */
	(void) tet_xdicend(tet_xrid);
	REPLY1(tet_xderrno);
}

/*
**	req_tpstart() - send a TP start message to XRESD
**
**	request is:
**	testnum
**
**	reply is:
**	reply-code
*/

static void req_tpstart(request, argc, argv)
char *request;
int argc;
char **argv;
{
	int testnum;

	if (check_master(request) < 0)
		return;

	/* perform a sanity check on the request */
	if (argc != 1 || !isallnum(*argv)) {
		INVALID_PARAM1(request);
		return;
	}

	/* extract the fixed parameters from the request */
	testnum = atoi(*argv);
	TRACE2(tet_Ttcm, 3, "TPSTART: testnum = %s", tet_i2a(testnum));

	/* send the message to XRESD and return */
	(void) tet_xdtpstart(tet_xrid, testnum);
	REPLY1(tet_xderrno);
}

/*
**	req_tpend() - send a TP start message to XRESD
**
**	there are no parameters to this request
**
**	reply is:
**	reply-code
*/

static void req_tpend(request, argc, argv)
char *request;
int argc;
char **argv;
{
	if (check_master(request) < 0)
		return;

	/* perform a sanity check on the request */
	if (argc != 0) {
		INVALID_PARAM1(request);
		return;
	}

	/* send the message to XRESD and return */
	(void) tet_xdtpend(tet_xrid);
	REPLY1(tet_xderrno);
}

/*
**	req_merror() - process a tet_merror request
**
**	request is:
**	number-of-error-lines
**	error-line
**	...
**
**	reply is:
**	reply code
*/

static void req_merror(request, argc, argv)
char *request;
int argc;
char **argv;
{
	char **lines;
	int nlines;
	int rc;
#ifndef NOTRACE
	char **lp;
#endif

	/* perform a sanity check on the request */
	if (argc != 1 || !isallnum(*argv)) {
		INVALID_PARAM1(request);
		return;
	}

	/* read in the error message lines */
	nlines = atoi(*argv);
	TRACE2(tet_Ttcm, 3, "MERROR: nlines = %s", tet_i2a(nlines));
	if (nlines == 0) {
		tet_error(ER_INVAL, "nlines parameter cannot be zero in tet_merror request");
		REPLY1(ER_INVAL);
		return;
	}
	if ((rc = get_data_lines(&lines, &nlines)) != ER_OK) {
		REPLY1(rc);
		return;
	}
	if (nlines > 0)
		rc = ER_OK;
	else {
		tet_error(ER_INVAL, "data line(s) missing in tet_merror request");
		rc = ER_INVAL;
	}

#ifndef NOTRACE
	if (tet_Ttcm)
		for (lp = lines; lp < lines + nlines; lp++)
			TRACE2(tet_Ttcm, 4, "MERROR: line = \"%s\"", *lp);
#endif

	/* call the library function to do the work */
	if (nlines > 0)
		tet_merror(0, lines, nlines);

	/* finally, free up storage allocated in get_data_lines() and return */
	free_data_lines(lines, nlines);
	REPLY1(rc);
}

/*
**	req_getsysbyid - call tet_getsysbyid
**
**	request is:
**	sysid
**
**	reply is:
**	reply-code sysid hostname
*/

static void req_getsysbyid(request, argc, argv)
char *request;
int argc;
char **argv;
{
	int sysid;
	struct tet_sysent sysent;

	/* perform a sanity check on the request */
	if (argc != 1 || !isallnum(*argv)) {
		INVALID_PARAM3(request, "-1", "-");
		return;
	}

	/* extract the fixed parameters from the request */
	sysid = atoi(*argv);
	TRACE2(tet_Ttcm, 3, "GETSYSBYID: sysid = %s", tet_i2a(sysid));

	/* call the API function */
	if (tet_getsysbyid(sysid, &sysent) < 0)
		REPLY3(-tet_errno, "-1", "-");
	else
		REPLY3(ER_OK, tet_i2a(sysent.ts_sysid), sysent.ts_name);
}

/************************************************************************
*									*
*	reply functions							*
*									*
************************************************************************/

/*
**	reply() - common reply function
*/

static void reply(rc, s1, s2, s3)
int rc;
char *s1, *s2, *s3;
{
	static char fmt[] = " %s";
	char buf[48];
#ifndef NOTRACE
	static char null[] = "<NULL>";
#endif

	(void) sprintf(buf, "%.*s", (int) sizeof buf - 1, tet_ptrepcode(rc));
	(void) sp2us(buf);

	TRACE2(tet_Ttcm, 1, "REPLY = %s", tet_ptrepcode(rc));
	TRACE4(tet_Ttcm, 6, "fields = \"%s\" \"%s\" \"%s\"",
		s1 ? s1 : null, s2 ? s2 : null, s3 ? s3 : null);

	errno = 0;
	(void) printf("tet_reply %s %s", tet_version,
		!strncmp(buf, "ER_", 3) ? buf + 3 : buf);
	if (s1 && *s1)
		(void) printf(fmt, s1);
	if (s2 && *s2)
		(void) printf(fmt, s2);
	if (s3 && *s3)
		(void) printf(fmt, s3);
	flush_output();
}

/*
**	reply_data() - send additional data after a reply message
*/

static void reply_data(s1, s2)
char *s1, *s2;
{
	static char fmt[] = " %s";
#ifndef NOTRACE
	static char null[] = "<NULL>";
#endif

	TRACE3(tet_Ttcm, 6, "REPLY DATA = \"%s\" \"%s\"",
		s1 ? s1 : null, s2 ? s2 : null);

	errno = 0;
	(void) printf("tet_reply_data");
	if (s1 && *s1)
		(void) printf(fmt, s1);
	if (s2 && *s2)
		(void) printf(fmt, s2);
	flush_output();
}

/*
**	flush_output() - flush stdout and check for errors
*/

static void flush_output()
{
	(void) putchar('\n');
	if (ferror(stdout) || fflush(stdout) < 0) {
		tet_error(errno, "can't write output to co-process");
		tet_exit(1);
	}
}


/************************************************************************
*									*
*	input line functions						*
*									*
************************************************************************/

/*
**	find_next_request() - search for the next request line
**
**	return 0 if successful; otherwise, return EOF
**
**	on successful return the line is stored in the buffer at *line
*/

static int find_next_request(line, len)
char *line;
int len;
{
	static char tag[] = "tet_request";
	char buf[sizeof tag + 2];
	char *flds[1];

	TRACE3(tet_Ttcm, 9, "call to find_next_request(%s, %s)",
		tet_i2x(line), tet_i2a(len));

	while (getline(line, len) != EOF) {
		(void) sprintf(buf, "%.*s", (int) sizeof buf - 1, line);
		if (tet_getargs(buf, flds, 1) == 1 && !strcmp(flds[0], tag)) {
			TRACE1(tet_Ttcm, 9, "find_next_request(): return 0");
			return(0);
		}
	}

	TRACE1(tet_Ttcm, 9, "find_next_request(): return EOF");
	return(EOF);
}

/*
**	get_data_lines() - read up to *nlp data lines and store them in
**		memory obtained from malloc()
**	set the counter at *nlp to the number of lines actually read
**	set the pointer at *lpp to point to the start of the list
**
**	return ER_OK if successful or other ER_* code on error
**
**	the memory allocated by a successful call to this function may
**	subsequently be freed by a call to free_data_lines()
*/

static int get_data_lines(lpp, nlp)
char ***lpp;
int *nlp;
{
	static char tag[] = "tet_request_data";
	char buf[sizeof tag + 2];
	char line[BUFSIZ];
	char *flds[1];
	int nflds;
	int llines;
	int count = 0;
	int want = *nlp;
	int rc = ER_OK;
	char *p;
	char *msgs[2];

	TRACE2(tet_Ttcm, 9, "call to get_data_lines(): want to read %s lines",
		tet_i2a(*nlp));

	*lpp = (char **) 0;
	*nlp = 0;
	llines = 0;
	while (count < want && getline(line, sizeof line) != EOF) {
		(void) sprintf(buf, "%.*s", (int) sizeof buf - 1, line);
		if (
			(nflds = tet_getargs(buf, flds, 1)) != 1 ||
			strcmp(flds[0], tag)
		) {
			ungetline(line);
			if (nflds < 1 || strcmp(flds[0], "tet_request")) {
				msgs[0] = "received bad format data line:";
				msgs[1] = line;
				tet_merror(ER_INVAL, msgs, 2);
				rc = ER_INVAL;
			}
			break;
		}
		for (p = line + sizeof tag; *p; p++)
			if (!isspace(*p))
				break;
		RBUFCHK((char **) lpp, &llines,
			(count + 2) * (int) sizeof **lpp);
		*(*lpp + count++) = rstrstore(p);
		*(*lpp + count) = (char *) 0;
	}

	TRACE3(tet_Ttcm, 9,
		"get_data_lines(): return %s after reading %s lines",
		tet_ptrepcode(rc), tet_i2a(count));

	if (rc != ER_OK && count > 0) {
		free_data_lines(*lpp, count);
		*lpp = (char **) 0;
		count = 0;
	}

	*nlp = count;
	return(rc);
}

/*
**	free_data_lines() - free the memory allocated by a call to
**		get_data_lines()
*/

static void free_data_lines(lines, nlines)
char **lines;
int nlines;
{
	char **lp;

	if (lines)
		for (lp = lines; lp < lines + nlines; lp++)
			if (*lp) {
				TRACE2(tet_Tbuf, 6, "free data line = %s",
					tet_i2x(*lp));
				free((void *) *lp);
			}

	TRACE2(tet_Tbuf, 6, "free data line list = %s", tet_i2x(lines));
	if (lines)
		free((void *) lines);
}

/*
**	getline() - read a line from stdin
**
**	the newline is replaced with a zero byte
**
**	return 0 if successful; otherwise, return EOF
*/

static int getline(line, len)
char *line;
int len;
{
	struct lcache *lcp;
	char *p;

	TRACE3(tet_Ttcm, 10, "call to getline(%s, %s)",
		tet_i2x(line), tet_i2a(len));

	ASSERT(len > 0);

	if ((lcp = lcpop()) != (struct lcache *) 0) {
		(void) sprintf(line, "%.*s", len - 1, lcp->lc_line);
		lcfree(lcp);
		TRACE2(tet_Ttcm, 10,
			"getline(): return 0 after lcpop(), line = \"%s\"",
			line);
		return(0);
	}

	if (feof(stdin)) {
		TRACE1(tet_Ttcm, 10, "getline(): return EOF (1)");
		return(EOF);
	}

	errno = 0;
	if (fgets(line, len, stdin) == (char *) 0) {
		if (ferror(stdin))
			tet_error(errno, "can't read input from co-process");
		TRACE1(tet_Ttcm, 10, "getline(): return EOF (2)");
		return(EOF);
	}

	for (p = line; *p; p++)
		if (*p == '\n') {
			*p = '\0';
			break;
		}

	TRACE2(tet_Ttcm, 10, "getline(): return 0 after fgets(), line = \"%s\"", line);
	return(0);
}

/*
**	ungetline() - store a line for subsequent retrieval by getline()
*/

static void ungetline(line)
char *line;
{
	struct lcache *lcp;

	ASSERT(line);
	TRACE2(tet_Ttcm, 10, "call to ungetline(\"%s\")", line);

	/* store the line and push it on to the stack */
	lcp = lcalloc();
	lcp->lc_line = rstrstore(line);
	lcpush(lcp);
}

/*
**	fldcount() - return the number of fields in a line
*/

static int fldcount(line)
char *line;
{
	char *p;
	int n, sp;

	sp = 1;
	n = 0;
	for (p = line; *p; p++)
		if (isspace(*p))
			sp = 1;
		else if (sp) {
			sp = 0;
			n++;
		}

	return(n);
}

/*
**	lcalloc(), lcfree() - functions to allocate and free a
**		line cache element
*/

static struct lcache *lcalloc()
{
	register struct lcache *lcp;

	errno = 0;
	if ((lcp = (struct lcache *) malloc(sizeof *lcp)) == (struct lcache *) 0)
		fatal(errno, "can't allocate line cache element", (char *) 0);

	TRACE2(tet_Tbuf, 6, "allocate lcache element = %s", tet_i2x(lcp));
	bzero((char *) lcp, sizeof *lcp);
	return(lcp);
}

static void lcfree(lcp)
struct lcache *lcp;
{
	TRACE2(tet_Tbuf, 6, "free lcache element = %s", tet_i2x(lcp));

	if (lcp) {
		if (lcp->lc_line) {
			TRACE2(tet_Tbuf, 6, "free lcache line = %s",
				tet_i2x(lcp->lc_line));
			free(lcp->lc_line);
		}
		free((char *) lcp);
	}
}

/*
**	lcpush(), lcpop() - line cache stack manipulation functions
*/

static void lcpush(lcp)
struct lcache *lcp;
{
	tet_listinsert((struct llist **) &lcache, (struct llist *) lcp);
}

static struct lcache *lcpop()
{
	struct lcache *lcp;

	if ((lcp = lcache) != (struct lcache *) 0)
		tet_listremove((struct llist **) &lcache,
			(struct llist *) lcp);

	return(lcp);
}


/************************************************************************
*									*
*	utility functions						*
*									*
************************************************************************/

/*
**	sp2us() - ensure that a string contains only one field
**
**	replace all white space with underscores
**
**	returns its argument
*/

static char *sp2us(s)
char *s;
{
	char *p;

	for (p = s; *p; p++)
		if (isspace(*p))
			*p = '_';

	return(s);
}

/*
**	isallnum() - see if a string is all numeric and contains at least
**		one digit
**
**	return 1 if it is or 0 if it isn't
*/

static int isallnum(s)
char *s;
{
	if (!s || !*s)
		return(0);

	while (*s)
		if (!isdigit(*s))
			return(0);
		else
			s++;

	return(1);
}

/*
**	check_master() - see if this is the master system
**
**	return 0 if it is
**	send a (no-parameter) error reply and return -1 if it isn't
*/

static int check_master(request)
char *request;
{
	static char fmt[] = "%.24s request invalid for STCM";
	char msg[sizeof fmt + 24];

	switch (tet_myptype) {
	case PT_MTCM:
		return(0);
	case PT_STCM:
		break;
	default:
		fatal(0, "unexpected system type:", tet_ptptype(tet_myptype));
		/* NOTREACHED */
	}

	(void) sprintf(msg, fmt, request);
	tet_error(ER_INVAL, msg);
	REPLY1(ER_INVAL);

	return(-1);
}

/*
**	invalid_param() - invalid parameter reporting function
*/

static void invalid_param(request, s1, s2, s3)
char *request, *s1, *s2, *s3;
{
	static char fmt[] = "paramaters invalid or missing in %.24s request";
	char msg[sizeof fmt + 24];

	(void) sprintf(msg, fmt, request);
	tet_error(ER_INVAL, msg);
	REPLY4(ER_INVAL, s1, s2, s3);
}


/*
**	rstrstore() - reliable strstore() call
**
**	there is no return on error
*/

static char *rstrstore(s)
char *s;
{
	char *p;

	if ((p = tet_strstore(s)) == (char *) 0)
		fatal(0, "can't continue", (char *) 0);

	return(p);
}

#ifdef NOTRACE

/*
**	rbufchk() - reliable tet_bufchk() call
*/

static void rbufchk(bpp, lp, newlen)
char **bpp;
int *lp, newlen;
{
	if (tet_bufchk(bpp, lp, newlen) < 0)
		fatal(0, "can't continue", (char *) 0);
}

#else /* NOTRACE */

/*
**	rbuftrace() - reliable tet_buftrace() call
*/

static void rbuftrace(bpp, lp, newlen, file, line)
char **bpp, *file;
int *lp, newlen, line;
{
	if (tet_buftrace(bpp, lp, newlen, file, line) < 0)
		fatal(0, "can't continue", (char *) 0);
}

#endif /* NOTRACE */


/*
**	smflags() - return printable representation of synmsg flags value
*/

static char *smflags(fval)
int fval;
{
	static struct flags flags[] = {
		{ TET_SMSNDMSG, "SNDMSG" },
		{ TET_SMRCVMSG, "RCVMSG" },
		{ TET_SMDUP, "DUP" },
		{ TET_SMTRUNC, "TRUNC" }
	};

	return(tet_f2a(fval, flags, sizeof flags / sizeof flags[0]));
}


/*
**	Dummy test case data to satisy references in tcm_m.o
*/

void (*tet_startup)() = NULL, (*tet_cleanup)() = NULL;
struct tet_testlist tet_testlist[] = { {NULL,0} };
