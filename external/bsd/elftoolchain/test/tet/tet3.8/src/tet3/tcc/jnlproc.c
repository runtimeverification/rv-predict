/*
 *	SCCS: @(#)jnlproc.c	1.11 (05/11/29)
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
static char sccsid[] = "@(#)jnlproc.c	1.11 (05/11/29) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)jnlproc.c	1.11 05/11/29 TETware release 3.8
NAME:		jnlproc.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	testcase journal processing functions

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., May 1997
	made context, block and sequence long -
	to match what the API writes to the xres file

	Andrew Dingwall, UniSoft Ltd., December 1997
	replaced SCF_DIST scenario flag with pr_distflag proctab flag

	Geoff Clare, The Open Group, November 2005
	abort whole run when a result code with the "Abort" action is seen

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <time.h>
#include <errno.h>
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "error.h"
#include "servlib.h"
#include "dtetlib.h"
#include "scentab.h"
#include "proctab.h"
#include "tcc.h"
#include "tet_api.h"
#include "tet_jrnl.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* structure used when re-ordering xres lines */
struct xrlist {
	int xr_id;		/* xres line id */
	long xr_context;	/* xres line context (id == TC_INFO) */
	long xr_block;		/* xres line block (id == TC_INFO) */
	long xr_sequence;	/* xres line sequence (id == TC_INFO) */
	char *xr_line;		/* the line itself */
};


/* static function declarations */
static int jp1_nonapi PROTOLIST((struct proctab *));
static int jp_cmp PROTOLIST((const void *, const void *));
static int jp_reorder PROTOLIST((struct proctab *, FILE *, char *, int,
	char [], char [], int *));
static int jp_split PROTOLIST((struct proctab *, char *, char *, char **,
	char []));
static int jp_tetxres PROTOLIST((struct proctab *));
static void jp_trim PROTOLIST((char *));
static int jp_xres PROTOLIST((struct proctab *, FILE *, char *));

#ifndef TET_LITE	/* -START-LITE-CUT- */
static FILE *open_xresdfile PROTOLIST((struct proctab *));
#endif /* !TET_LITE */	/* -END-LITE-CUT- */


/*
**	jnlproc_api() - process an xres file for an API-conforming
**		test case
**
**	return 0 if all the TPs reported PASS or -1 if at least one TP
**		did not report PASS
**
**	this function is called at the proctab level which owns the journal;
**	the XRESD file to be processed is always this level, but it is
**	possible for tet_xres files to be at this level (prp->pr_child false)
**	or at the level below (prp->pr_child true)
*/

int jnlproc_api(prp)
struct proctab *prp;
{
#ifndef TET_LITE	/* -START-LITE-CUT- */
	FILE *fp;
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
	int rc;

	TRACE3(tet_Ttcc, 6, "jnlproc_api(%s): transfer the xres file(s) to journal file %s",
		tet_i2x(prp), prp->pr_jfname);


#ifndef TET_LITE	/* -START-LITE-CUT- */
	ASSERT(prp->pr_xfname);

	/* close the XRESD file */
	rm_snid_xrid(prp);
#endif /* !TET_LITE */	/* -END-LITE-CUT- */


	/*
	** if the XRESD file is non-empty, process it;
	** otherwise, process each tet_xres file
	*/
#ifndef TET_LITE	/* -START-LITE-CUT- */
	if ((fp = open_xresdfile(prp)) != (FILE *) 0) {
		TRACE2(tet_Ttcc, 6, "using XRESD file %s", prp->pr_xfname);
		rc = jp_xres(prp, fp, prp->pr_xfname);
		(void) fclose(fp);
	}
	else
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
	{
		TRACE1(tet_Ttcc, 6, "using tet_xres file(s)");
		rc = RUN_PROCTABS(prp, jp_tetxres);
	}

	TRACE3(tet_Ttcc, 6, "jnlproc_api(%s) RETURN %s",
		tet_i2x(prp), tet_i2a(rc));
	return(rc);
}

/*
**	jp_tetxres() - process a tet_xres file for an API-conforming
**		test case
**
**	return 0 if all the TPs reported PASS or -1 if at least one TP
**		did not report PASS
**
**	errors also return -1 so as to avoid the risk of reporting a
**	false PASS
*/

static int jp_tetxres(prp)
struct proctab *prp;
{
	char *fname;
	FILE *fp;
	int rc;

	ASSERT_LITE(*prp->pr_sys == 0);

	/*
	** if this is a remote system first get the tet_xres file on to the
	** local system;
	** if the testcase exited with non-zero exit code, don't attempt
	** to transfer the file if it doesn't exist
	*/
#ifndef TET_LITE	/* -START-LITE-CUT- */
	if (*prp->pr_sys > 0) {
		if ((fname = jnl_tfname(resdirname(), "txr")) == (char *) 0) {
			prperror(prp, *prp->pr_sys, 0, "can't generate file name to receive contents of",
				prp->pr_tetxres);
			return(-1);
		}
		if (
			(prp->pr_exitcode != 0 &&
			tet_tcaccess(*prp->pr_sys, prp->pr_tetxres, 04) < 0)
		||
			(getremfile(prp, prp->pr_tetxres, tet_basename(fname)) < 0)
		) {
			(void) UNLINK(fname);
			return(-1);
		}
	}
	else
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
	{
		if (prp->pr_exitcode != 0 &&
			tet_eaccess(prp->pr_tetxres, 04) < 0)
				return(-1);
		fname = prp->pr_tetxres;
	}

	/* open the file */
	TRACE3(tet_Ttcc, 6, "jp_tetxres(%s): about to open %s",
		tet_i2x(prp), fname);
	if ((fp = fopen(fname, "r")) == (FILE *) 0) {
		prperror(prp, *prp->pr_sys, errno, "can't open", fname);
		return(-1);
	}

	/* process it */
	rc = jp_xres(prp, fp, prp->pr_tetxres);
	(void) fclose(fp);


#ifndef TET_LITE	/* -START-LITE-CUT- */
	/*
	** if we created a temporary file to receive the tet_xres file
	** from a remote system, unlink it now
	*/
	if (fname != prp->pr_tetxres)
		(void) UNLINK(fname);
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	return(rc);
}

/*
**	jp_xres() - process an xres file that has been generated by
**		an API-conforming test case or tool
**
**	return 0 if all the TPs reported PASS or -1 if at least one TP
**		did not report PASS
**
**	note that fname is the name of the origin file - for a remote system
**	this is the name of the tet_xres file on the remote system
*/

static int jp_xres(prp, fp, fname)
struct proctab *prp;
FILE *fp;
char *fname;
{
	char line[LBUFLEN];
	char buf[LBUFLEN];
	char abortmsg[50];
	char *flds[3];
	int id, tpno, rc = 0;

	TRACE4(tet_Ttcc, 6, "jp_xres(%s): read lines from %s on system %s",
		tet_i2x(prp), fname,
		prp->pr_child ? "0" : tet_i2a(*prp->pr_sys));
	ASSERT(fp);

	*abortmsg = '\0';

	while (!feof(fp) && fgets(line, sizeof line, fp) != (char *) 0)
	{
		int result = TET_NORESULT, abortflag = 0;

		jp_trim(line);
		if (jp_split(prp, fname, line, flds, buf) < 0) {
			rc = -1;
			continue;
		}
		id = atoi(flds[0]);
		jnl_write(id, flds[1], flds[2], prp->pr_jfp, prp->pr_jfname);
		if (
			id == TET_JNL_TP_START
#ifndef TET_LITE	/* -START-LITE-CUT- */
			&&
			prp->pr_distflag == 0
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
		) {
			tpno = -1;
			(void) sscanf(flds[1], "%*s %d", &tpno);
			if (jp_reorder(prp, fp, fname, tpno, line, buf, &result) < 0)
				rc = -1;
		}
		else if (id == TET_JNL_TP_RESULT)
		{
			(void) sscanf(flds[1], "%*d %*d %d", &result);
		}

		/* check for an "Abort" action */
		(void) tet_getresname(result, &abortflag);
		if (abortflag)
		{
			tcc_modes |= TCC_ABORT;
			(void) sprintf(abortmsg,
				"TCC aborting on result code %d", result);
		}
	}
	if (*abortmsg != '\0')
	{
		jnl_tcc_msg(abortmsg);
	}

	return(rc);
}

/*
**	jp_reorder() - read xres lines up to a TP result line and
**		reorder them into the journal
**
**	return 0 if the TP reported PASS, -1 otherwise
*/

static int jp_reorder(prp, fp, fname, tpno, line, buf, resp)
struct proctab *prp;
FILE *fp;
char *fname, line[], buf[];
int tpno;
int *resp;
{
	char *flds[3];
	struct xrlist *lines1 = (struct xrlist *) 0;
	int llines1 = 0, nlines1 = 0;
	register struct xrlist *lp1;
	struct xrlist *lines2 = (struct xrlist *) 0;
	int llines2 = 0, nlines2 = 0;
	register struct xrlist *lp2;
	struct xrlist *lp3;
	long offs;
	int id = -1, rc = 0;
	int done = 0, result = TET_NORESULT;

	TRACE3(tet_Ttcc, 6, "jp_reorder(%s): reorder xres lines for TP %s",
		tet_i2x(prp), tet_i2a(tpno));

	line[0] = '\0';
	while (offs = ftell(fp), fgets(line, LBUFLEN, fp) != (char *) 0) {
		jp_trim(line);
		if (jp_split(prp, fname, line, flds, buf) < 0) {
			rc = -1;
			continue;
		}
		switch (id = atoi(flds[0])) {
		case TET_JNL_TP_RESULT:
			(void) sscanf(flds[1], "%*s %*s %d", &result);
			done = 1;
			break;
		case TET_JNL_IC_START:
		case TET_JNL_IC_END:
		case TET_JNL_TP_START:
			line[0] = '\0';
			(void) fseek(fp, offs, SEEK_SET);
			done = 1;
			break;
		}
		if (done)
			break;
		RBUFCHK((char **) &lines1, &llines1,
			((int) (nlines1 + 1) * sizeof *lines1));
		lp1 = lines1 + nlines1++;
		lp1->xr_context = 0L;
		lp1->xr_block = 0L;
		lp1->xr_sequence = 0L;
		if (id == TET_JNL_TC_INFO)
			(void) sscanf(flds[1], "%*s %*s %ld %ld %ld",
				&lp1->xr_context, &lp1->xr_block,
				&lp1->xr_sequence);
		lp1->xr_id = id;
		lp1->xr_line = rstrstore(line);
	}

	/*
	** here we have a (possibly empty) list of lines (lines1, nlines1),
	** the id of the last line read (id) and the line itself (line)
	*/

	for (lp1 = lines1; lp1 < lines1 + nlines1; lp1++) {
		if (!lp1->xr_line)
			continue;
		ASSERT(!jp_split(prp, fname, lp1->xr_line, flds, buf));
		jnl_write(lp1->xr_id, flds[1], flds[2], prp->pr_jfp,
			prp->pr_jfname);
		TRACE2(tet_Tbuf, 6, "free xres line 1 = %s",
			tet_i2x(lp1->xr_line));
		free(lp1->xr_line);
		lp1->xr_line = (char *) 0;
		if (lp1->xr_id != TET_JNL_TC_INFO)
			continue;
		for (lp3 = lp1++, nlines2 = 0; lp1 < lines1 + nlines1; lp1++) {
			if (
				lp1->xr_id == TET_JNL_TC_INFO &&
				lp1->xr_context == lp3->xr_context &&
				lp1->xr_block == lp3->xr_block
			) {
				RBUFCHK((char **) &lines2, &llines2,
					((int) (nlines2 + 1) * sizeof *lines2));
				lp2 = lines2 + nlines2++;
				*lp2 = *lp1;
				lp1->xr_line = (char *) 0;
			}
		}
		if (nlines2 > 1)
			qsort((char *) lines2, (unsigned) nlines2,
				sizeof *lines2, jp_cmp);
		for (lp2 = lines2; lp2 < lines2 + nlines2; lp2++) {
			ASSERT(!jp_split(prp, fname, lp2->xr_line, flds, buf));
			jnl_write(lp2->xr_id, flds[1], flds[2], prp->pr_jfp,
				prp->pr_jfname);
			TRACE2(tet_Tbuf, 6, "free xres line 2 = %s",
				tet_i2x(lp2->xr_line));
			free(lp2->xr_line);
			lp2->xr_line = (char *) 0;
		}
		lp1 = lp3;
	}

	/*
	** free all allocated storage, making sure that we haven't missed
	** any of the lines
	*/
	for (lp1 = lines1; lp1 < lines1 + nlines1; lp1++)
		ASSERT(lp1->xr_line == (char *) 0);
	TRACE2(tet_Tbuf, 6, "free lines1 = %s", tet_i2x(lines1));
	free((char *) lines1);
	for (lp2 = lines2; lp2 < lines2 + nlines2; lp2++)
		ASSERT(lp2->xr_line == (char *) 0);
	TRACE2(tet_Tbuf, 6, "free lines2 = %s", tet_i2x(lines2));
	free((char *) lines2);

	/* write out the result line */
	if (id == TET_JNL_TP_RESULT) {
		ASSERT(!jp_split(prp, fname, line, flds, buf));
		jnl_write(id, flds[1], flds[2], prp->pr_jfp, prp->pr_jfname);
	}
	else
		jnl_tp_result(prp, tpno, result);

	/* determine the return code from the result and return */
	if (rc == 0)
		rc = (result == TET_PASS) ? 0 : -1;
	if (resp != NULL)
		*resp = result;
	return(rc);
}

/*
**	jp_cmp() - comparison routine for qsort()
*/

static int jp_cmp(ep1, ep2)
const void *ep1, *ep2;
{
	return(((struct xrlist *) ep1)->xr_sequence -
		((struct xrlist *) ep2)->xr_sequence);
}

/*
**	jp_trim() - remove line terminators from a journal line
**
**	we check for CR as well as LF because the file might have come
**	from a system which uses CRLF as a line terminator
*/

static void jp_trim(line)
char *line;
{
	register char *p;

	for (p = line; *p; p++)
		if (*p == '\r' || *p == '\n') {
			*p = '\0';
			break;
		}
}

/*
**	jp_split() - split a line into |-delimited fields
**
**	return pointers to each field in the array at *fldp
**	use buf as a scratchpad
**
**	return 0 if 3 fields were found, -1 otherwise
*/

static int jp_split(prp, fname, line, fldp, buf)
struct proctab *prp;
char *fname, *line, **fldp, buf[];
{

	if (jnlproc_split(line, fldp, buf) < 0) {
		prperror(prp, prp->pr_child ? -1 : *prp->pr_sys, 0,
			"ignored bad format line in tet_xres file", fname);
		return(-1);
	}
	else
		return(0);
}

/*
**	jnlproc_nonapi() - perform journal processing for a
**		non-API-conforming test case or tool
**
**	return 0 if all the tools returned zero exit code or -1 if at
**		least one tool returned a non-zero exit code
**
**	this function is called at the proctab level which owns the journal;
**	it is possible for there to be child proctabs below this level
*/

int jnlproc_nonapi(prp)
struct proctab *prp;
{
	int rc;

	TRACE2(tet_Ttcc, 6, "jnlproc_nonapi(%s)", tet_i2x(prp));

#ifndef TET_LITE	/* -START-LITE-CUT- */
	if (prp->pr_flags & PRF_JNL_CHILD) {
		ASSERT(prp->pr_child);
		rc = run_child_proctabs(prp, jp1_nonapi);
	}
	else
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
		rc = jp1_nonapi(prp);


	TRACE3(tet_Ttcc, 6, "jnlproc_nonapi(%s) RETURN %s",
		tet_i2x(prp), tet_i2a(rc));
	return(rc);
}

/*
**	jp1_nonapi() - perform non-API journal processing for a
**		single journal
**
**	return 0 if the tool returned a zero exit code, -1 otherwise
*/

static int jp1_nonapi(prp)
struct proctab *prp;
{
	if (prp->pr_flags & PRF_AUTORESULT) {
		jnl_tp_result(prp, 1, prp->pr_exitcode ? TET_FAIL : TET_PASS);
		jnl_ic_end(prp);
	}

	return(prp->pr_exitcode ? -1 : 0);
}


#ifndef TET_LITE	/* -START-LITE-CUT- */

/*
**	open_xresdfile() - open the XRESD file and see if it is empty
**
**	return fp to the opened file if the file is not empty
**	return (FILE *) 0 if the file is empty (after closing the file)
**	or on error
*/

static FILE *open_xresdfile(prp)
struct proctab *prp;
{
	FILE *fp;
	char buf[LBUFLEN];

	TRACE3(tet_Ttcc, 6, "open_xresdfile(%s): about to open %s",
		tet_i2x(prp), prp->pr_xfname);

	/* open the file */
	if ((fp = fopen(prp->pr_xfname, "r")) == (FILE *) 0) {
		prperror(prp, -1, errno, "can't open", prp->pr_xfname);
		return((FILE *) 0);
	}

	/*
	** if there is not at least one line in the file,
	** close it and return
	*/
	if (fgets(buf, sizeof buf, fp) == (char *) 0) {
		TRACE2(tet_Ttcc, 6, "open_xresdfile(): %s is empty",
			prp->pr_xfname);
		(void) fclose(fp);
		return((FILE *) 0);
	}
	
	/*
	** here if file has at least one line in it -
	** rewind the file and return
	*/
	rewind(fp);
	return(fp);
}

#endif /* !TET_LITE */	/* -END-LITE-CUT- */


/*
**	jnlproc_split() - split a line into |-delimited fields
**
**	return pointers to each field in the array at *fldp
**	use buf as a scratchpad
**
**	return 0 if 3 fields were found, -1 otherwise
*/

int jnlproc_split(line, fldp, buf)
char *line, **fldp, buf[];
{
	register char *p1, *p2;
	register int nflds = 0, new = 1;

	for (p1 = line, p2 = buf; *p1 && p2 < &buf[LBUFLEN - 1]; p1++, p2++) {
		if (new && nflds++ < 3) {
			*fldp++ = p2;
			new = 0;
		}
		if (*p1 == '|' && nflds < 3) {
			*p2 = '\0';
			new = 1;
		}
		else
			*p2 = *p1;
	}

	*p2 = '\0';
	if (new && nflds++ < 3)
		*fldp++ = p2;

	if (nflds != 3) {
		TRACE3(tet_Ttcc, 1, "jnlproc_split(): wrong number of fields (%s) in \"%s\"",
			tet_i2a(nflds), line);
		return(-1);
	}

	return(0);
}

