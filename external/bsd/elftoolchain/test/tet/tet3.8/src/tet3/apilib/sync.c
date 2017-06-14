/*
 *      SCCS:  @(#)sync.c	1.20 (99/11/15) 
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
static char sccsid[] = "@(#)sync.c	1.20 (99/11/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)sync.c	1.20 99/11/15 TETware release 3.8
NAME:		sync.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

SYNOPSIS:
	#include "tet_api.h"

	int tet_remsync(long syncptno, int *syncnames, int nsysname,
		int waittime, int vote, struct tet_synmsg *msgp);

	int tet_sync(long syncptno, int *syncnames, int waittime);

	int tet_msync(long syncptno, int *syncnames, int waittime,
		struct tet_synmsg *msgp);

	void (*tet_syncerr)(struct tet_syncstat *statp, int nstat);

	void tet_syncreport(struct tet_syncstat *statp, int nstat);

DESCRIPTION:
	DTET API functions

	Tet_remsync() performs a user sync operation, returning 0 if
	successful or -1 on error.  Tet_sync() and tet_msync() provide
	a subset of the facilities of tet_remsync() and are included
	for backwards compatibility.

	Tet_syncreport() is the default sync-error-reporting function.
	Tet_syncerr is initially set to point to this function, but it
	may be changed to point to a user-supplied reporting function.

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, August 1993
	make syncptno parameter long rather than int
	changed dtet to tet2 in #include

	Andrew Dingwall, UniSoft Ltd., December 1993
	changed dapi.h to dtet2/tet_api.h

	Andrew Dingwall, UniSoft Ltd., October 1994
	added tet_msync() API function

	Geoff Clare, UniSoft Ltd., July-August 1996
	Changes for TETWare.

	Geoff Clare, UniSoft Ltd., Sept 1996
	Changes for TETWare-Lite.

	Andrew Dingwall, UniSoft Ltd., June 1997
	1) fixed the way in which sy_ptype is determined so that sync
	still works when there is no system 0
	2) in tet_syncreport(), print messages in an atomic operation if
	possible using tet_merror() - that way, when a sync fails which
	involves 3 or more systems, the lines from each system don't get
	mixed up in the journal

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
	Andrew Dingwall, UniSoft Ltd., July 1999
	added support for 'other language' APIs

	Andrew Dingwall, UniSoft Ltd., October 1999
	added support for strict POSIX threads

************************************************************************/

#ifndef TET_LITE /* -START-LITE-CUT- */

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "dtthr.h"
#include "synreq.h"
#include "error.h"
#include "globals.h"
#include "servlib.h"
#include "dtetlib.h"
#include "tet_api.h"
#include "tet_jrnl.h"
#include "apilib.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

#define REPORT_YES	1
#define REPORT_NO	0

/* static function declarations */
static int tet_ms2 PROTOLIST((long, int *, int, int, int, struct tet_synmsg *,
	int));


TET_IMPORT int tet_remsync(syncptno, syncnames, nsyncname, waittime, vote, msgp)
long syncptno;
int *syncnames, nsyncname, waittime, vote;
struct tet_synmsg *msgp;
{
	tet_check_api_status(TET_CHECK_API_INITIALISED);

	if (!syncnames || nsyncname <= 0 ||
	    (vote != TET_SV_YES && vote != TET_SV_NO))
	{
		tet_errno = TET_ER_INVAL;
		return -1;
	}

	return tet_ms2(syncptno, syncnames, nsyncname, waittime, vote,
		msgp, REPORT_NO);
}

TET_IMPORT int tet_sync(syncptno, syncnames, waittime)
long syncptno;
int *syncnames, waittime;
{
	int nsyncname, defaultsync = 0;

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	if (!syncnames) 
		syncnames = &defaultsync;

	/* count the entries in syncnames[] */
	for (nsyncname = 0; syncnames[nsyncname] != 0; nsyncname++)
		;

	/* include the 0 terminator so that system 0 participates */
	nsyncname++;

	return tet_ms2(syncptno, syncnames, nsyncname, waittime, TET_SV_YES,
		(struct tet_synmsg *) 0, REPORT_YES);
}

TET_IMPORT int tet_msync(syncptno, syncnames, waittime, msgp)
long syncptno;
int *syncnames, waittime;
struct tet_synmsg *msgp;
{
	int nsyncname, defaultsync = 0;

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	if (!syncnames) 
		syncnames = &defaultsync;

	/* count the entries in syncnames[] */
	for (nsyncname = 0; syncnames[nsyncname] != 0; nsyncname++)
		;

	/* include the 0 terminator so that system 0 participates */
	nsyncname++;

	return tet_ms2(syncptno, syncnames, nsyncname, waittime, TET_SV_YES,
		msgp, REPORT_YES);
}

static int tet_ms2(syncptno, syncnames, nsys, waittime, vote, msgp, report_errs)
long syncptno;
int *syncnames, nsys, waittime, vote, report_errs;
struct tet_synmsg *msgp;
{
	register struct synreq *sp;
	register int *ip;
	register struct tet_syncstat *tsp;
	struct synmsg synmsg, *synmsgp;
	static struct synreq *synreq;
	static int reqlen;
	static struct tet_syncstat *synstat;
	static int statlen;

	API_LOCK;

	/* make sure that the synreq buffer is big enough */
	if (BUFCHK((char **) &synreq, &reqlen, (int) (nsys * sizeof *synreq)) < 0)
	{
		tet_errno = TET_ER_ERR;
		API_UNLOCK;
		return(-1);
	}

	/* copy the system names in to the synreq buffer */
	sp = synreq;
	for (ip = syncnames; ip < &syncnames[nsys]; ip++)
		if (*ip != tet_mysysid) {
			sp->sy_sysid = *ip;
			if (tet_snames && tet_Nsname > 0 &&
				*ip == *tet_snames)
					sp->sy_ptype = PT_MTCM;
			else
				sp->sy_ptype = PT_STCM;
			sp++;
		}

	/* recalculate nsys (will go down if tet_mysysid was in syncnames) */
	nsys = (sp - synreq);
	if (nsys == 0) {
		error(0, "syncnames does not include any other systems",
			(char *) 0);
		tet_errno = TET_ER_ERR;
		API_UNLOCK;
		return(-1);
	}

	/* set up the synmsg structure */
	if (msgp && (msgp->tsm_flags & (TET_SMSNDMSG | TET_SMRCVMSG))) {
		synmsg.sm_data = msgp->tsm_data;
		synmsg.sm_mdlen = msgp->tsm_dlen;
		if (msgp->tsm_flags & TET_SMSNDMSG) {
			synmsg.sm_flags = SM_SNDMSG;
			if ((synmsg.sm_dlen = msgp->tsm_dlen) > TET_SMMSGMAX) {
				synmsg.sm_dlen = TET_SMMSGMAX;
				synmsg.sm_flags |= SM_TRUNC;
			}
		}
		else {
			synmsg.sm_flags = SM_RCVMSG;
			synmsg.sm_dlen = 0;
		}
		synmsgp = &synmsg;
	}
	else
		synmsgp = (struct synmsg *) 0;

	/* perform the sync operation and handle the reply codes */
	if (tet_sdusync(tet_snid, tet_xrid, syncptno, vote, waittime, synreq, nsys, synmsgp) < 0) {
		if (report_errs) {
			char buf[100];
			switch (tet_sderrno) {
			case ER_DONE:
				(void) sprintf(buf,
				    "sync event already happened, syncptno = %ld",
				    syncptno);
				tet_error(0, buf);
				break;
			case ER_INVAL:
				error(0, "invalid request parameter", (char *) 0);
				break;
			case ER_DUPS:
				error(0, "duplicates in syncnames list", (char *) 0);
				break;
			}
		}
		tet_errno = -tet_sderrno;
		API_UNLOCK;
		return(-1);
	}

	/* if this sytem voted NO, then other NO votes are not an error */
	if (vote == TET_SV_NO && tet_sderrno == ER_SYNCERR) {
		tet_sderrno = ER_OK;
		for (sp = synreq; sp < synreq + nsys; sp++)
			if (sp->sy_state != SS_SYNCYES &&
			    sp->sy_state != SS_SYNCNO) {
				tet_sderrno = ER_SYNCERR;
				break;
			}
	}

	/* do message processing */
	if (tet_sderrno == ER_OK || tet_sderrno == ER_SYNCERR) {
		if (msgp && (msgp->tsm_flags & (TET_SMSNDMSG | TET_SMRCVMSG))) {
			msgp->tsm_dlen = synmsg.sm_dlen;
			msgp->tsm_sysid = synmsg.sm_sysid;
			if (synmsg.sm_flags & SM_RCVMSG)
				msgp->tsm_flags = (msgp->tsm_flags & ~TET_SMSNDMSG) | TET_SMRCVMSG;
			if (synmsg.sm_flags & SM_DUP)
				msgp->tsm_flags |= TET_SMDUP;
			if (synmsg.sm_flags & SM_TRUNC)
				msgp->tsm_flags |= TET_SMTRUNC;
		}
	}

	/* return now if request succeeded */
	if (tet_sderrno == ER_OK) {
		API_UNLOCK;
		return(0);
	}

	/* make sure that the synstat buffer is big enough */
	if (BUFCHK((char **) &synstat, &statlen, (int) (nsys * sizeof *synstat)) < 0)
	{
		tet_errno = TET_ER_ERR;
		API_UNLOCK;
		return(-1);
	}

	/* here if the request failed in an "expected" way -
		pass detailed status info to reporting function */

	tet_errno = -tet_sderrno;

	if (tet_syncerr) {
		int sav_errno = tet_errno;

		tsp = synstat;
		for (sp = synreq; sp < synreq + nsys; sp++) {
			tsp->tsy_sysid = sp->sy_sysid;
			tsp->tsy_state = sp->sy_state;
			tsp++;
		}

		(*tet_syncerr)(syncptno, synstat, nsys);

		tet_errno = sav_errno; /* in case (*tet_syncerr)() changed it */
	}

	API_UNLOCK;
	return(-1);
}

TET_IMPORT void tet_syncreport(syncptno, statp, nsys)
long syncptno;
struct tet_syncstat *statp;
int nsys;
{
	struct tet_syncstat *tsp;
	char *p;
	char buf[512];
	static char nullstr[] = "";
	static char sstr[] = "s";
	char **lines, **lp;

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	/*
	** allocate memory for the list of message line pointers 
	** if possible
	*/
	errno = 0;
	if ((lines = (char **) malloc((nsys + 1) * sizeof *lines)) == (char **) 0)
		error(errno,
			"can't allocate memory for sync report line pointers",
			(char *) 0);
	else
		TRACE2(tet_Tbuf, 6, "allocate sync report line pointers = %s",
			tet_i2x(lines));
	lp = lines;

	/*
	** generate a TCM info message detailing processes that caused the
	** sync to fail (as required by the spec)
	**
	** the message lines are stored in memory if possible, then
	** written out all at once;
	** if this is not possible they are written out one-at-a-time
	*/
	(void) sprintf(buf, "sync operation failed, syncptno = %ld, ",
		syncptno);
	p = buf + strlen(buf);
	if (tet_errno == TET_ER_SYNCERR)
		(void) sprintf(p, "%sother system%s did not sync or timed out",
			nsys > 1 ? "one or more of the " : nullstr,
			nsys == 1 ? nullstr : sstr);
	else if (tet_errno == TET_ER_TIMEDOUT)
		(void) sprintf(p, "request timed out");
	else if (tet_errno >= 0 && tet_errno < tet_nerr)
		(void) sprintf(p, "%s", tet_errlist[tet_errno]);
	else
		(void) sprintf(p, "tet_errno = %d (unknown value)", tet_errno);
	if (lines)
		*lp++ = tet_strstore(buf);
	else
		tet_error(0, buf);

	/* generate the per-system diagnostics */
	for (tsp = statp; tsp < statp + nsys; tsp++) {
		(void) sprintf(buf, "system = %2d, state = %s",
			tsp->tsy_sysid, tet_systate(tsp->tsy_state));
		if (lines)
			*lp++ = tet_strstore(buf);
		else
			tet_error(0, buf);
	}

	/* emit all the message lines at once if we stored them */
	if (lines)
		tet_merror(0, lines, nsys + 1);

	/* free storage allocated here */
	if (lines) {
		for (lp = lines; lp < lines + nsys + 1; lp++)
			if (*lp) {
				TRACE2(tet_Tbuf, 6,
					"free sync report line = %s",
					tet_i2x(*lp));
				free(*lp);
			}
		TRACE2(tet_Tbuf, 6, "free sync report line pointers = %s",
			tet_i2x(lines));
		free((char *) lines);
	}
}

TET_IMPORT void (*tet_syncerr)() = tet_syncreport;

#else /* -END-LITE-CUT- */

/* avoid "empty" file */
int tet_sync_not_supported;

#endif /* -LITE-CUT-LINE- */

