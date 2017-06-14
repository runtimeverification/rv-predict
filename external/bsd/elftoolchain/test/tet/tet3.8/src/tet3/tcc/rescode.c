/*
 *	SCCS: @(#)rescode.c	1.7 (03/03/26)
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
static char sccsid[] = "@(#)rescode.c	1.7 (03/03/26) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)rescode.c	1.7 03/03/26 TETware release 3.8
NAME:		rescode.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	result code file processing functions

MODIFICATIONS:
	Andrew Dingwall, The Open Group, March 2003
	Changed the order of looking up TET_RESCODES_FILE in the
	per-mode configurations to match the order described in the
	documentation.

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
#include "dtmsg.h"
#include "error.h"
#include "globals.h"
#include "ltoa.h"
#include "servlib.h"
#include "dtetlib.h"
#include "restab.h"
#include "config.h"
#include "systab.h"
#include "tcc.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* name of the temporary master results code file name */
static char *rcftmp;


/* static function declarations */
static void irc2 PROTOLIST((void));
#ifndef TET_LITE	/* -START-LITE-CUT- */
static int rdist2 PROTOLIST((struct systab *, char **, int));
static void rescode_distribute PROTOLIST((void));
#endif /* !TET_LITE */	/* -END-LITE-CUT- */


/*
**	initrescode() - set up the results code file
*/

void initrescode()
{
	struct systab *sp;

	TRACE1(tet_Ttcc, 4, "initrescode()");

	/* initialise the rescode table */
	irc2();
	if ((sp = syfind(0)) != (struct systab *) 0)
		sp->sy_rcfname = rcftmp;

#ifndef TET_LITE	/* -START-LITE-CUT- */
	/* propagate the rescode table to each remote system */
	if (symax() > 0)
		rescode_distribute();

	/* then send the file name to XRESD */
	if (tet_xdcodesfile(rcftmp) < 0)
		fatal(tet_xderrno,
			"can't send results code file name to XRESD",
			(char *) 0);
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

}


/*
**	irc2() - extend the initrescode() processing
**
**	there is no return on error
*/

static void irc2()
{
	char fname[MAXPATH];
	register struct restab *rtp;
	register char *p;
	FILE *fp;
	static char tet_rescodes_file[] = "TET_RESCODES_FILE";

	TRACE1(tet_Ttcc, 4, "irc2()");

	/* determine the name of the rescode file */
	if (tcc_modes & TCC_BUILD)
		p = getmcfg(tet_rescodes_file, TCC_BUILD);
	else
		p = (char *) 0;
	if ((!p || !*p) && (tcc_modes & TCC_EXEC))
		p = getmcfg(tet_rescodes_file, TCC_EXEC);
	if ((!p || !*p) && (tcc_modes & TCC_CLEAN))
		p = getmcfg(tet_rescodes_file, TCC_CLEAN);
	if (!p || !*p)
		p = "tet_code";

	/* pick up the generic result code file if there is one */
	fullpath(tet_root, p, fname, sizeof fname, 0);
	if (tet_initrestab() < 0 ||
		(tet_eaccess(fname, 04) == 0 && tet_readrescodes(fname) < 0))
			tcc_exit(1);

	/* pick up the testsuite-specific result code file if there is one */
	fullpath(tet_tsroot, p, fname, sizeof fname, 0);
	if (tet_eaccess(fname, 04) == 0 && tet_readrescodes(fname) < 0)
		tcc_exit(1);

	/*
	** here to install the master rescode file -
	** create a temporary file and open it
	*/
	if ((rcftmp = tet_mktfname("tcc")) == (char *) 0)
		tcc_exit(1);
	if ((fp = fopen(rcftmp, "w")) == (FILE *) 0)
		fatal(errno, "can't open combined rescode file", rcftmp);

	/* write out the default results codes */
	(void) fprintf(fp, "# master results code file\n\n");
	for (rtp = tet_restab; rtp < tet_restab + tet_nrestab; rtp++)
		if (fprintf(fp, "%d \"%s\" %s\n", rtp->rt_code, rtp->rt_name,
			rtp->rt_abrt ? "Abort" : "Continue") < 0)
				fatal(errno, "write error on", rcftmp);

	if (fclose(fp) < 0)
		fatal(errno, "close error on", rcftmp);
}

#ifndef TET_LITE	/* -START-LITE-CUT- */
/*
**	rescode_distribute() - propagate the result codes file to each
**		remote system
*/

static void rescode_distribute()
{
	register int sysid, sysmax;
	FILE *fp;
	char line[LBUFLEN];
	char **lines = (char **) 0;
	int llines = 0, nlines = 0;
	register char *p, **lp;
	register struct systab *sp;
	int rc = 0;

	TRACE1(tet_Ttcc, 4, "rescode_distribute()");

	/* open the master results code file */
	if ((fp = fopen(rcftmp, "r")) == (FILE *) 0)
		fatal(errno, "can't open", rcftmp);

	/* read in all the lines */
	while (fgets(line, sizeof line, fp) != (char *) 0) {
		for (p = line; *p; p++)
			if (*p == '\n') {
				*p = '\0';
				break;
			}
		RBUFCHK((char **) &lines, &llines, (int) ((nlines + 1) * sizeof *lines));
		*(lines + nlines++) = rstrstore(line);
	}

	(void) fclose(fp);

	/* distribute the lines to each remote system */
	for (sysid = 1, sysmax = symax(); sysid <= sysmax; sysid++)
		if ((sp = syfind(sysid)) != (struct systab *) 0 &&
			rdist2(sp, lines, nlines) < 0)
				rc = -1;

	/* free storage allocated here */
	for (lp = lines; lp < lines + nlines; lp++) {
		TRACE2(tet_Tbuf, 6, "free rescode line = %s", tet_i2x(*lp));
		free(*lp);
		*lp = (char *) 0;
	}
	TRACE2(tet_Tbuf, 6, "free rescode list = %s", tet_i2x(lines));
	free((char *) lines);

	if (rc < 0)
		tcc_exit(1);
}

/*
**	rdist2() - extend the rescode_distribute() processing
**
**	send the result codes file to a single system
**
**	return 0 if successful or -1 on error
*/

static int rdist2(sp, lines, nlines)
struct systab *sp;
char **lines;
int nlines;
{
	static char fmt[] = "can't open tmp result codes file %.*s on system";
	char msg[sizeof fmt + MAXPATH];
	char cfname[MAXPATH];
	int fid, rc = 0;

	TRACE2(tet_Ttcc, 4, "rdist2(): sysid = %s", tet_i2a(sp->sy_sysid));

	/*
	** determine the name of the remote temporary result codes file
	** below the saved files directory
	*/
	fullpath(sp->sy_sfdir, "tet_rescode", cfname, sizeof cfname, 1);

	/* open the remote file */
	if ((fid = tet_tcfopen(sp->sy_sysid, cfname, 0)) < 0) {
		(void) sprintf(msg, fmt, sizeof msg - sizeof fmt, cfname);
		if (!IS_ER_ERRNO(tet_tcerrno))
			errno = 0;
		error(errno ? errno : tet_tcerrno, msg, tet_i2a(sp->sy_sysid));
		return(-1);
	}
	sp->sy_rcfname = rstrstore(cfname);

	/* send all the result code lines to the remote system */
	if (tet_tcputsv(sp->sy_sysid, fid, lines, nlines) < 0) {
		if (!IS_ER_ERRNO(tet_tcerrno))
			errno = 0;
		error(errno ? errno : tet_tcerrno,
			"can't send result code lines to system",
			tet_i2a(sp->sy_sysid));
		rc = -1;
	}

	/* close the file */
	if (tet_tcfclose(sp->sy_sysid, fid) < 0) {
		error(tet_tcerrno, "tet_tcfclose() failed for tmp result code file on system",
			tet_i2a(sp->sy_sysid));
		rc = -1;
	}

	return(rc);
}
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

/*
**	rescode_cleanup() - remove a temporary rescode file before exit
*/

void rescode_cleanup()
{
#ifndef TET_LITE	/* -START-LITE-CUT- */
	register int sysid, sysmax;
	register struct systab *sp;
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	/* remove the local tmp result code file */
	(void) UNLINK(rcftmp);

#ifndef TET_LITE	/* -START-LITE-CUT- */
	/* remove all the remote tmp rescode files */
	for (sysid = 1, sysmax = symax(); sysid <= sysmax; sysid++)
		if ((sp = syfind(sysid)) != (struct systab *) 0 &&
			sp->sy_rcfname)
				(void) tet_tcunlink(sysid, sp->sy_rcfname);
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

}

