/*
 *	SCCS: @(#)ictp.c	1.25 (05/06/24)
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1996 X/Open Company Limited
 * (C) Copyright 2005 The Open Group
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

/*
 * Copyright 1990 Open Software Foundation (OSF)
 * Copyright 1990 Unix International (UI)
 * Copyright 1990 X/Open Company Limited (X/Open)
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose and without fee is hereby granted, provided
 * that the above copyright notice appear in all copies and that both that
 * copyright notice and this permission notice appear in supporting
 * documentation, and that the name of OSF, UI or X/Open not be used in 
 * advertising or publicity pertaining to distribution of the software 
 * without specific, written prior permission.  OSF, UI and X/Open make 
 * no representations about the suitability of this software for any purpose.  
 * It is provided "as is" without express or implied warranty.
 *
 * OSF, UI and X/Open DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, 
 * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO 
 * EVENT SHALL OSF, UI or X/Open BE LIABLE FOR ANY SPECIAL, INDIRECT OR 
 * CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF 
 * USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR 
 * OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR 
 * PERFORMANCE OF THIS SOFTWARE.
 */

#ifndef lint
static char sccsid_ictp[] = "@(#)ictp.c	1.25 (05/06/24) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)ictp.c	1.25 05/06/24 TETware release 3.8
NAME:		ictp.c
PRODUCT:	TETware
AUTHOR:		Geoff Clare, UniSoft Ltd.
DATE CREATED:	25 July 1990
DESCRIPTION:

	tet_icstart(), tet_icend(), tet_tpstart() and
	tet_tpend() are used by the TCM to output TCM start lines, IC
	start and end lines and test purpose start and result lines to
	the combined execution results file maintained by XRESD

MODIFICATIONS:
	
	June 1992
	DTET development - apilib/dresfile.c is derived from
	TET release 1.10

	Andrew Dingwall, UniSoft Ltd,. October 1992
	Some non-API functions specific to parent TCMs moved from
	apilib/dresfile.c to here.

	All vestages of the local execution results file and temporary
	execution results file removed - in the DTET, all the processing
	associated with these files is done by XRESD.

	All these functions completely re-written - auto-syncs are used
	to pass deletion and Abort information between TCMs as follows:

		if a TP is deleted within a TCM, that TCM votes NO at the
		TP start sync point
		if a TCM receives a NO sync vote at TP start, it knows that
		the TP has been deleted in another TCM

		if a result code action is Abort, XRESD returns ER_ABORT
		when the MTCM calls tet_xdtpend() from tet_tpend();
		the MTCM syncs with a NO vote to the end of the last TP

		STCMs will receive this NO vote at the start of any remaining
		TPs in the current IC, thus causing them to believe that
		these TPs have been deleted in the MTCM

		when STCMs receive the NO vote at the start of the next IC,
		they will interpret it as the signal to perform Abort
		processing

	Andrew Dingwall, UniSoft Ltd., November 1992
	Increased tpend sync timeout to 10 mins so as to allow for
	larger inbalance between test part execution times.

	Denis McConalogue, UniSoft Limited, June 1993
	API enhancements - moved all functions from [ms]tcmdist.c
			   to here. Added tet_ismaster().

	Denis McConalogue, UniSoft Limited, September 1993
	do not open tet_combined in tet_opencom() if an tet_xrid is
	already initialised.

	Andrew Dingwall, UniSoft Ltd., November 1994
	signal IC end from MTCM when result code action is Abort

	Geoff Clare, UniSoft Ltd., August 1996
	Changes for TETWare.

	Andrew Dingwall, UniSoft Ltd., August 1996
	changes for use with tetware tcc

	Geoff Clare, UniSoft Ltd., Sept 1996
	Changes for TETWare-Lite.

	Andrew Dingwall, UniSoft Ltd., June 1997
	changes to support the defined test case interface

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.

	Andrew Dingwall, UniSoft Ltd., July 1999
	moved TCM code out of the API library

	Geoff Clare, The Open Group, June 2005
	Use tet_curtime() in local curtime() function.

************************************************************************/

/*
** This file is a component of the TCM (tcm.o) and/or one of the child
** process controllers (tcmchild.o and tcmrem.o).
** On UNIX systems, these .o files are built using ld -r.
** There is no equivalent to ld -r in MSVC, so on Win32 systems each .c
** file is #included in a scratch .c or .cpp file and a single object
** file built from that.
**
** This imposes some restictions on the contents of this file:
**
**	+ Since this file might be included in a C++ program, all
**	  functions must have both ANSI C and common C definitions.
**
**	+ The only .h file that may appear in this file is tcmhdrs.h;
**	  all other .h files that are needed must be #included in there.
**
**	+ The scope of static variables and functions encompasses all
**	  the source files, not just this file.
**	  So all static variables and functions must have unique names.
*/


/*
** all the header files are included by tcmhdrs.h
** don't include any other header files directly
*/
#include "tcmhdrs.h"


#ifdef NEEDsrcFile
#  ifdef srcFile
#    undef srcFile
#  endif
#  define srcFile srcFile_ictp
   static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


#define MODE666 (mode_t) (S_IRUSR|S_IWUSR|S_IRGRP|S_IWGRP|S_IROTH|S_IWOTH)

#ifdef TET_LITE	/* -LITE-CUT-LINE- */
   char *tet_tmpresfile;
   static char *tmpresenv;
   static char *resenv, *resfile;
#else		/* -START-LITE-CUT- */
   int tet_iclast = ~(~0 << S_ICBITS) - 2;
					/* used in auto sync before cleanup */
   int tet_sync_del = 0;		/* true when a TP is deleted in a
					   test case part on another system */
   struct synreq *tet_synreq = (struct synreq *) 0;
					/* used when analysing the results
					   of an auto-sync */
#endif		/* -END-LITE-CUT- */


/* static function declarations */
static void icend2 PROTOLIST((int, int));
static int icstart2 PROTOLIST((int, int));
static int tpend2 PROTOLIST((int, int, int));
static void tpstart2 PROTOLIST((int, int, int));
#ifdef TET_LITE	/* -LITE-CUT-LINE- */
   static char *curtime PROTOLIST((void));
   static void lite_output PROTOLIST((int, char *, char *));
#else		/* -START-LITE-CUT- */
   static int ismaster PROTOLIST((void));
   static int mtcm_tpend2 PROTOLIST((void));
#endif		/* -END-LITE-CUT- */


/*
**	tet_icstart() - signal IC start
**
**	return 0 if successful or -1 to abort the test case
**
**	in Distributed TETware, if the previous result code action was Abort,
**	this function is only executed by STCMs
*/

#ifdef PROTOTYPES
int tet_icstart(int icno, int tpcount)
#else
int tet_icstart(icno, tpcount)
int icno, tpcount;
#endif
{
	int rc;

	TRACE3(tet_Ttcm, 7, "tet_icstart(): icno = %s, tpcount = %s",
		tet_i2a(icno), tet_i2a(tpcount));

	rc = icstart2(icno, tpcount);

	TRACE2(tet_Ttcm, 7, "tet_icstart() RETURN %s", tet_i2a(rc));
	return(rc);
}

#ifdef TET_LITE	/* -LITE-CUT-LINE- */

#ifdef PROTOTYPES
static int icstart2(int icno, int tpcount)
#else
static int icstart2(icno, tpcount)
int icno, tpcount;
#endif
{
	char buf[128];

	(void) sprintf(buf, "%d %d %s", icno, tpcount, curtime());
	lite_output(TET_JNL_IC_START, buf, "IC Start");
	return(0);
}

#else		/* -START-LITE-CUT- */

#ifdef PROTOTYPES
static int icstart2(int icno, int tpcount)
#else
static int icstart2(icno, tpcount)
int icno, tpcount;
#endif
{
	register struct synreq *sp;
	int nsys = tet_Nsname;
	int errflag;
	char errmsg[128];

	/* the MTCM informs XRESD of IC start */
	if (
		ismaster() &&
		tet_xdicstart(tet_xrid, icno, tet_activity, tpcount) < 0
	) {
		tet_error(tet_xderrno, "can't inform XRESD of IC start");
		tet_exit(EXIT_FAILURE);
	}

	/*
	** then all TCMs sync on IC start -
	**	if the MTCM votes NO, this means that the previous result
	**	code action was Abort
	*/
	ASSERT(tet_synreq);
	if (tet_tcm_async(MK_ASPNO(icno, 0, S_ICSTART), SV_YES, SV_SYNC_TIMEOUT,
		tet_synreq, &nsys) < 0) {
			(void) sprintf(errmsg,
				"Auto Sync failed at start of IC %d", icno);
			tet_error(tet_sderrno, errmsg);
			tet_exit(EXIT_FAILURE);
	}

	if (tet_sderrno == ER_OK)
		return(0);

	/*
	** here if the auto sync failed in an expected way -
	** 	a NO vote from the MTCM means that the test is to be aborted
	*/
	errflag = 0;
	for (sp = tet_synreq; sp < tet_synreq + nsys; sp++)
		switch (sp->sy_state) {
		case SS_SYNCYES:
			break;
		case SS_SYNCNO:
			if (sp->sy_sysid == 0)
				break;
			/* else fall through */
		default:
			(void) sprintf(errmsg,
		"Auto sync error at start of IC %d, sysid = %d, state = %s",
				icno, sp->sy_sysid, tet_systate(sp->sy_state));
			tet_error(tet_sderrno, errmsg);
			errflag = 1;
			break;
		}


	if (errflag)
		tet_exit(EXIT_FAILURE);

	/* here if MTCM voted NO - return -1 to force a cleanup and exit */
	return(-1);
}

#endif		/* -END-LITE-CUT- */

/*
**	tet_icend() - signal IC end
*/

#ifdef PROTOTYPES
void tet_icend(int icno, int tpcount)
#else
void tet_icend(icno, tpcount)
int icno, tpcount;
#endif
{

	TRACE3(tet_Ttcm, 7, "tet_icend(): icno = %s, tpcount = %s",
		tet_i2a(icno), tet_i2a(tpcount));

	icend2(icno, tpcount);

	TRACE1(tet_Ttcm, 7, "tet_icend() RETURN");
}


#ifdef TET_LITE	/* -LITE-CUT-LINE- */

#ifdef PROTOTYPES
static void icend2(int icno, int tpcount)
#else
static void icend2(icno, tpcount)
int icno, tpcount;
#endif
{
	char buf[128];

	(void) sprintf(buf, "%d %d %s", icno, tpcount, curtime());
	lite_output(TET_JNL_IC_END, buf, "IC End");
}

#else		/* -START-LITE-CUT- */

/* ARGSUSED */
#ifdef PROTOTYPES
static void icend2(int icno, int tpcount)
#else
static void icend2(icno, tpcount)
int icno, tpcount;
#endif
{
	/* the MTCM informs XRESD of IC end */
	if (ismaster() && tet_xdicend(tet_xrid) < 0) {
		tet_error(tet_xderrno, "can't inform XRESD of IC end");
		tet_exit(EXIT_FAILURE);
	}
}

#endif		/* -END-LITE-CUT- */


/*
**	tet_tpstart() - signal TP start
*/

#ifdef PROTOTYPES
void tet_tpstart(int icno, int tpno, int testnum)
#else
void tet_tpstart(icno, tpno, testnum)
int icno, tpno, testnum;
#endif
{

	TRACE4(tet_Ttcm, 7, "tet_tpstart(): icno = %s, tpno = %s, testnum = %s",
		tet_i2a(icno), tet_i2a(tpno), tet_i2a(testnum));

#ifdef TET_THREADS
	tet_next_block = 1;
#endif
	tet_block = 1;
	tet_sequence = 1;

	tpstart2(icno, tpno, testnum);

	TRACE1(tet_Ttcm, 7, "tet_tpstart() RETURN");
}


#ifdef TET_LITE	/* -LITE-CUT-LINE- */

#ifdef PROTOTYPES
static void tpstart2(int icno, int tpno, int testnum)
#else
static void tpstart2(icno, tpno, testnum)
int icno, tpno, testnum;
#endif
{
	char buf[128];

	(void) sprintf(buf, "%d %s", testnum, curtime());
	lite_output(TET_JNL_TP_START, buf, "TP Start");

	/* create temporary result file */
	(void) remove(tet_tmpresfile);
	if ((tet_tmpresfp = fopen(tet_tmpresfile, "a+b")) == (FILE *) 0)
		fatal(errno, "cannot create temporary result file:",
			tet_tmpresfile);

	/* override umask (must be writable by set-uid children) */
	(void) CHMOD(tet_tmpresfile, MODE666);

	/*
	** put pathname in environment to be picked up by tet_result() in
	** exec'ed programs
	*/
	ASSERT(tmpresenv);
	if (tet_putenv(tmpresenv) != 0)
		tet_error(0, "tet_putenv() failed setting TET_TMPRESFILE");
}

#else		/* -START-LITE-CUT- */

#ifdef PROTOTYPES
static void tpstart2(int icno, int tpno, int testnum)
#else
static void tpstart2(icno, tpno, testnum)
int icno, tpno, testnum;
#endif
{
	register struct synreq *sp;
	register int vote;
	int nsys = tet_Nsname;
	int errflag;
	char errmsg[128];

	/* the MTCM informs XRESD of TP start */
	if (ismaster() && tet_xdtpstart(tet_xrid, testnum) < 0) {
		tet_error(tet_xderrno, "can't inform XRESD of TP start");
		tet_exit(EXIT_FAILURE);
	}

	/*
	** see if the TP has been deleted in this TCM and set our vote
	** accordingly
	*/
	vote = tet_reason(testnum) ? SV_NO : SV_YES;

	/* then all the TCMs sync on TP start */
	ASSERT(tet_synreq);
	if (tet_tcm_async(MK_ASPNO(icno, tpno, S_TPSTART), vote,
		SV_SYNC_TIMEOUT, tet_synreq, &nsys) < 0) {
			(void) sprintf(errmsg,
				"Auto Sync failed at start of TP %d", testnum);
			tet_error(tet_sderrno, errmsg);
			tet_exit(EXIT_FAILURE);
	}

	if (tet_sderrno == ER_OK)
		return;

	/*
	** here if sync failed in an expected way -
	**	a NO vote means that the TP has been deleted in another TCM
	*/
	errflag = 0;
	for (sp = tet_synreq; sp < tet_synreq + nsys; sp++)
		switch (sp->sy_state) {
		case SS_SYNCYES:
			break;
		case SS_SYNCNO:
			tet_sync_del = 1;
			break;
		default:
			(void) sprintf(errmsg,
		"Auto Sync error at start of TP %d, sysid = %d, state = %s",
				testnum, sp->sy_sysid,
				tet_systate(sp->sy_state));
			tet_error(tet_sderrno, errmsg);
			errflag = 1;
			break;
		}

	if (errflag)
		tet_exit(EXIT_FAILURE);
}

#endif		/* -END-LITE-CUT- */


/*
**	tet_tpend() - signal TP end
**
**	return 0 if successful or -1 to abort the test case
*/

#ifdef PROTOTYPES
int tet_tpend(int icno, int tpno, int testnum)
#else
int tet_tpend(icno, tpno, testnum)
int icno, tpno, testnum;
#endif
{
	int rc;

	TRACE4(tet_Ttcm, 7, "tet_tpend(): icno = %s, tpno = %s, testnum = %s",
		tet_i2a(icno), tet_i2a(tpno), tet_i2a(testnum));

	rc = tpend2(icno, tpno, testnum);

	TRACE2(tet_Ttcm, 7, "tet_tpend(): RETURN %s", tet_i2a(rc));
	return(rc);
}


#ifdef TET_LITE	/* -LITE-CUT-LINE- */

#ifdef PROTOTYPES
static int tpend2(int icno, int tpno, int testnum)
#else
static int tpend2(icno, tpno, testnum)
int icno, tpno, testnum;
#endif
{
	char *res;
	int have_result, nextres, err;
	int result = TET_NORESULT;
	int abrt = 0;
	char buf[128];

	/*
	 * output a "TP Result" line to the execution results file,
	 * based on the result code(s) written to a temporary result
	 * file by tet_result()
	 */

	/* rewind temporary result file */
	if (fseek(tet_tmpresfp, 0L, SEEK_SET) != 0)
	{
		tet_error(errno, "failed to rewind temporary result file");

		/* fall through: no results will be read */
	}

	/*
	** read result code(s) from temporary file - if more than one
	** has been written, choose the one with the highest priority
	*/

	have_result = 0;
	while (fread((void *)&nextres, sizeof(nextres), (size_t) 1, tet_tmpresfp) == 1)
	{
		/* if it's the first result, take it (for now) */
		if (!have_result)
		{
			result = nextres;
			have_result = 1;
			continue;
		}

		/* decide if this result supersedes any previous ones */

		result = tet_addresult(result, nextres);
	}
	err = errno;

	if (ferror(tet_tmpresfp))
	{
		tet_error(err, "read error on temporary results file");
		have_result = 0;
	}

	(void) fclose(tet_tmpresfp);
	(void) UNLINK(tet_tmpresfile);
	(void) tet_putenv("TET_TMPRESFILE=");

	if (!have_result)
	{
		result = TET_NORESULT;
		res = "NORESULT";
	}
	else
	{
		res = tet_get_code(result, &abrt);
		if (res == NULL)
		{
			/*
			** This should never happen, as the codes have
			** already been validated by tet_result().
			** It is not a serious problem - the name is only
			** there to make the results file more readable
			*/
			res = "(NO RESULT NAME)";
		}
	}

	(void) sprintf(buf, "%d %d %s", testnum, result, curtime());
	lite_output(TET_JNL_TP_RESULT, buf, res);

	/*
	** Abort is done here rather than in tet_result() since the
	** latter may have been called in a child.
	** Test purposes should not assume that tet_result() will not return
	** when called with an abort code.
	*/
	if (abrt)
	{
#  ifdef TET_THREADS
		tet_cln_threads(0);
		tet_mtx_destroy();
		tet_mtx_init();
#  endif
		(void) sprintf(buf, "ABORT on result code %d \"%s\"",
			result, res);
		lite_output(TET_JNL_TCM_INFO, "", buf);
		return(-1);
	}

	return(0);
}

#else		/* -START-LITE-CUT- */

#ifdef PROTOTYPES
static int tpend2(int icno, int tpno, int testnum)
#else
static int tpend2(icno, tpno, testnum)
int icno, tpno, testnum;
#endif
{
	register struct synreq *sp;
	int err;
	int nsys = tet_Nsname;
	char errmsg[128];

	/*
	** all the TCMs sync YES on TP end -
	**	there is an assumption here that the parts of a TP will
	**	arrive at their ends within 10 minutes of each other;
	**	if this is not so for a particular test, the test writer
	**	is expected to use tet_sync() with a longer timeout
	**	to delay the ends of the quicker test parts
	*/
	ASSERT(tet_synreq);
	if (tet_tcm_async(MK_ASPNO(icno, tpno, S_TPEND), SV_YES,
		SV_SYNC_TIMEOUT * 10, tet_synreq, &nsys) < 0) {
			(void) sprintf(errmsg, 
				"Auto Sync failed at end of TP %d", testnum);
			tet_error(tet_sderrno, errmsg);
			tet_exit(EXIT_FAILURE);
	}
	err = tet_sderrno;

	/* then the MTCM informs XRESD of TP end */
	if (ismaster() && mtcm_tpend2() < 0)
		return(-1);

	if (err == ER_OK)
		return(0);

	/* here if a TCM voted NO ("can't happen"), timed out or died */
	for (sp = tet_synreq; sp < tet_synreq + nsys; sp++)
		switch (sp->sy_state) {
		case SS_SYNCYES:
			break;
		default:
			(void) sprintf(errmsg,
		"Auto Sync error at end of TP %d, sysid = %d, state = %s",
				testnum, sp->sy_sysid,
				tet_systate(sp->sy_state));
			tet_error(err, errmsg);
			break;
		}

	tet_exit(EXIT_FAILURE);

	/* NOTREACHED */
	return(-1);
}

/*
**	mtcm_tpend2() - inform XRESD of TP end from MTCM
**
**	return 0 if successful or -1 if XRESD indicates that the testcase
**	must be aborted
*/

#ifdef PROTOTYPES
static int mtcm_tpend2(void)
#else
static int mtcm_tpend2()
#endif
{
	char errmsg[128];
	struct synreq *synreq;
	int nsys, errflag;
	register struct synreq *sp;

	/* signal TP end to XRESD */
	(void) tet_xdtpend(tet_xrid);
	switch (tet_xderrno) {
	case ER_OK:
		return(0);
	case ER_ABORT:
		/* XRESD prints the "ABORT on result code" message */
		break;
	default:
		tet_error(tet_xderrno, "can't inform XRESD of TP end");
		tet_exit(EXIT_FAILURE);
		/* NOTREACHED */
	}

	/*
	** here if previous TP result code action was Abort -
	** allocate memory for synreq array; don't worry if it fails
	*/
	errno = 0;
	if ((synreq = (struct synreq *) malloc((size_t)(sizeof *synreq * tet_Nsname))) == (struct synreq *) 0) {
		tet_error(errno, " can't allocate synreq array for Abort sync");
		nsys = 0;
	}
	else
		nsys = tet_Nsname;

	/* signal IC end to XRESD */
	if (tet_xdicend(tet_xrid) < 0)
		tet_error(tet_xderrno, "ABORT: can't inform XRESD of IC end");

	/*
	** sync NO to the end of the last IC -
	**	this communicates the Abort action to other TCMs
	*/
	if (tet_tcm_async(MK_ASPNO(tet_iclast, ~0, S_ICEND), SV_NO,
		SV_SYNC_TIMEOUT, synreq, &nsys) < 0) {
			tet_error(tet_sderrno, "Abort Auto Sync failed");
			tet_exit(EXIT_FAILURE);
	}

	/* make sure that the other TCMs are still alive */
	if (synreq) {
		errflag = 0;
		for (sp = synreq; sp < synreq + nsys; sp++)
			switch (sp->sy_state) {
			case SS_SYNCYES:
			case SS_SYNCNO:
				break;
			default:
				(void) sprintf(errmsg,
			"Abort Auto Sync error, sysid = %d, state = %s",
					sp->sy_sysid,
					tet_systate(sp->sy_state));
				tet_error(tet_sderrno, errmsg);
				errflag = 1;
				break;
			}
		if (errflag)
			tet_exit(EXIT_FAILURE);
	}

	/* here if we should attempt to call (*tet_cleanup)() */
	return(-1);
}

#endif		/* -END-LITE-CUT- */


/*
**	tet_tcmstart() - send TCM Start journal line to XRESD
*/

#ifdef PROTOTYPES
void tet_tcmstart(char *versn, int no_ics)
#else
void tet_tcmstart(versn, no_ics)
char *versn;
int no_ics;
#endif
{
	char buf[128];

#ifdef TET_LITE	/* -LITE-CUT-LINE- */

	(void) sprintf(buf, "%s %d", versn, no_ics);
	lite_output(TET_JNL_TCM_START, buf, "TCM Start");

#else		/* -START-LITE-CUT- */

	if (!ismaster()) 
		return;

	(void) sprintf(buf, "%d|%ld %s %d|TCM Start", 
		TET_JNL_TCM_START, tet_activity, versn, no_ics);

	if (tet_xdxres(tet_xrid, buf) < 0) {
		tet_error(tet_xderrno,
			"can't send \"TCM Start\" journal line to XRESD");
		tet_exit(EXIT_FAILURE);
	}

#endif		/* -END-LITE-CUT- */

}


#ifndef TET_LITE	/* -START-LITE-CUT- */

/*
**	tet_init_synreq() - allocate memory for the tet_synreq array
*/

#ifdef PROTOTYPES
void tet_init_synreq(void)
#else
void tet_init_synreq()
#endif
{
	if (tet_synreq != (struct synreq *) 0)
		return;

	errno = 0;
	if ((tet_synreq = (struct synreq *) malloc(sizeof *tet_synreq * tet_Nsname)) == (struct synreq *) 0) {
		tet_error(errno, "can't allocate memory for tet_synreq array");
		tet_exit(EXIT_FAILURE);
	}

	TRACE2(tet_Tbuf, 6, "allocate tet_synreq = %s", tet_i2x(tet_synreq));
}

/*
**	ismaster() - return true if we are running on the master
**			 system.
*/

static int ismaster()
{
	if (tet_myptype == PT_MTCM)
		return (1);
	else if (tet_myptype == PT_STCM)
		return (0);
	else {
		tet_error(0, "Can't determine system type");
		tet_exit(EXIT_FAILURE);
		/* NOTREACHED */
		return(0);
	}
}

#endif		/* -END-LITE-CUT- */



#ifdef TET_LITE	/* -LITE-CUT-LINE- */

/*
**	tet_openres() - open the tet_xres file in TETware-Lite
*/

#ifdef PROTOTYPES
void tet_openres(void)
#else
void tet_openres()
#endif
{
	char cwdbuf[MAXPATH];
	static char resvar[] = "TET_RESFILE";
	static char resname[] = "tet_xres";
	static char tmpvar[] = "TET_TMPRESFILE";
	static char tmpname[] = "tet_res.tmp";

	/* set full path name of execution results file and temp results
	   file, in a form convenient for placing in the environment */

	if (GETCWD(cwdbuf, (size_t)MAXPATH) == NULL)
		fatal(errno, "getcwd() failed", (char *) 0);

	resenv = (char *) malloc(strlen(cwdbuf)+sizeof(resvar)+sizeof(resname)+4);
	if (resenv == NULL)
		fatal(errno, "can't allocate resenv in tet_openres()",
			(char *) 0);
	TRACE2(tet_Tbuf, 6, "allocate resenv = %s", tet_i2x(resenv));

	tmpresenv = (char *) malloc(strlen(cwdbuf)+sizeof(tmpvar)+sizeof(tmpname)+4);
	if (tmpresenv == NULL)
		fatal(errno, "can't allocate tmpresenv in tet_openres()",
			(char *) 0);
	TRACE2(tet_Tbuf, 6, "allocate tmpresenv = %s",
		tet_i2x(tmpresenv));

	(void) sprintf(resenv, "%s=%s/%s", resvar, cwdbuf, resname);
	resfile = resenv + sizeof(resvar);

	(void) sprintf(tmpresenv, "%s=%s/%s", tmpvar, cwdbuf, tmpname);
	tet_tmpresfile = tmpresenv + sizeof(tmpvar);

	/* create the execution results file and open in append mode */

	(void) remove(resfile);
	tet_resfp = fopen(resfile, "a");
	if (tet_resfp == NULL)
		fatal(errno, "cannot create results file:", resfile);

	/* override umask (must be writable by set-uid children) */
	(void) CHMOD(resfile, MODE666);

	/* put pathname in environment to be picked up in exec'ed programs */
	if (tet_putenv(resenv) != 0)
		tet_error(0, "tet_putenv() failed when setting TET_RESFILE");

	tet_combined_ok = 1;
}

/*
**	lite_output() - print a line to the tet_xres file in TETware-Lite
*/

#ifdef PROTOTYPES
static void lite_output(int mtype, char *fields, char *data)
#else
static void lite_output(mtype, fields, data)
int mtype;
char *fields;
char *data;
#endif
{
	char outbuf[TET_JNL_LEN];
	char *obp;
	static char fmt[] = "%d|%ld%s%.64s|";
	char header[sizeof fmt + (LNUMSZ * 2) + 64];

	if (data == NULL)
		data = "";

	(void) sprintf(header, fmt, mtype, tet_activity,
		fields[0] == '\0' ? "" : " ", fields);
	tet_msgform(header, data, outbuf);
	obp = outbuf;
	tet_routput(&obp, 1);
}

/*
**	curtime() - return string containing current time
*/

#ifdef PROTOTYPES
static char *curtime(void)
#else
static char *curtime()
#endif
{
	char *p;
	static int full = -1;
	static char s[sizeof "YYYY-MM-DDTHH:MM:SS.sss"];

	if (full == -1)
	{
		p = tet_getvar("TET_FULL_TIMESTAMPS");
		if (p != NULL && (*p == 'T' || *p == 't'))
			full = 1;
		else
			full = 0;
	}

	if (tet_curtime(s, sizeof s, full) == -1)
	{
		(void) strcpy(s, "TIME_ERR");
	}

	return s;
}

#endif		/* -LITE-CUT-LINE- */

