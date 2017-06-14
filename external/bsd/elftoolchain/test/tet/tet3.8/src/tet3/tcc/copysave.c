/*
 *	SCCS: @(#)copysave.c	1.9 (03/03/26)
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
static char sccsid[] = "@(#)copysave.c	1.9 (03/03/26) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)copysave.c	1.9 03/03/26 TETware release 3.8
NAME:		copysave.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	functions to deal with copying and saving files

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., June 1997
	don't update TET_TSROOT in the distributed configuration
	after processing a remote TET_RUN (rtrcopy());
	instead always use get_runtime_tsroot() to determine the location
	of the runtime test suite root directory on a particular system

	Andrew Dingwall, UniSoft Ltd., October 1997
	in tcc_rmtmpdir(), don't check for being in the tmpdir subtree
	if we haven't changed directory yet

	Andrew Dingwall, The Open Group, March 2003
	Enhancement to copy source files to remote systems.


************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <errno.h>
#include <ctype.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "btmsg.h"
#include "globals.h"
#include "error.h"
#include "ftoa.h"
#include "ltoa.h"
#include "config.h"
#include "ftype.h"
#include "servlib.h"
#include "dtetlib.h"
#include "scentab.h"
#include "proctab.h"
#include "systab.h"
#include "tcc.h"
#include "tcclib.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* definitions of source file copy option bits */
#define SF_FTYPE_ASCII		001
#define SF_FTYPE_BINARY		002
#define SF_KEEP_MTIME		004
#define SF_UNCOND_COPY		010


/* static function declarations */
static void inittd2 PROTOLIST((struct systab *));
static void inittd3 PROTOLIST((int, char [], int));
#ifndef TET_LITE	/* -START-LITE-CUT- */
static int copy_sf2r2 PROTOLIST((struct proctab *, char *, char *[], int,
	char *, int));
static int copy_sf2r3 PROTOLIST((struct proctab *, char *, char *, char *,
	char *, int, int *, int));
static int copy_sf2r3_2 PROTOLIST((struct proctab *, char *, char *, char *,
	char *, int, int *, int));
static int copy_sf2r4 PROTOLIST((struct proctab *, char *, char *, int,
	int *, int));
static int copy_sf2r4_2 PROTOLIST((struct proctab *, char *, char *, int,
	int *, int));
static int copy_sf2r4dir PROTOLIST((struct proctab *, char *, char *, int,
	int *, int));
static int copy_sf2r4dir_2 PROTOLIST((struct proctab *, char *, char *, int,
	int *, int));
static int copy_sf2r5 PROTOLIST((struct proctab *, char *, struct STAT_ST *,
	char *, int, int));
static int copy_sf2r5_2 PROTOLIST((struct proctab *, char *, struct STAT_ST *,
	char *, int, int));
static int copy_sf2r6 PROTOLIST((char *, struct STAT_ST *, FILE *, char *,
	int, int));
static int copy_sf2r6_2 PROTOLIST((char *, struct STAT_ST *, FILE *, char *,
	int, int));
static int copy_sf2r7 PROTOLIST((char *, FILE *, char *, int, int));
static int copy_sf2dotchk PROTOLIST((char *));
static int copy_sf2patmatch PROTOLIST((struct proctab *, char *, char *,
	char *, char *, int, int *, int));
static int copy_sf2patmatch_2 PROTOLIST((struct proctab *, char *, char *,
	char *, char *, int, int *, int));
static int init1sfdir PROTOLIST((struct systab *));
static void insferror PROTOLIST ((char *, char *, int, char *));
static int ispatt PROTOLIST((char *));
static int process_tftfile PROTOLIST((char *));
static char *prsfopts PROTOLIST((int));
static void rtrc2 PROTOLIST((int, char *));
#endif /* !TET_LITE */	/* -END-LITE-CUT- */


/*
**	rtlcopy() - copy the test suite to the runtime directory
**		on the local system if so required
*/

void rtlcopy()
{
	static char fmt[] = "can't copy test suite %.*s to runtime directory %.*s on";
	char msg[sizeof fmt + (MAXPATH * 2)];
	char dest[MAXPATH];

	TRACE3(tet_Ttcc, 1, "copying test suite %s to run-time directory %s on the local system",
		tet_tsroot, tet_run);

	/* determine the name of the destination directory */
	fullpath(tet_run, tet_basename(tet_tsroot), dest, sizeof dest, 0);

	/* do the copy */
	errno = 0;
	if (tet_fcopy(tet_tsroot, dest) < 0) {
		(void) sprintf(msg, fmt, MAXPATH, tet_tsroot, MAXPATH, dest);
		fatal(errno, msg, "the local system");
	}

	/* update tet_tsroot to refer to the new location */
	TRACE2(tet_Tbuf, 6, "free tet_tsroot = %s", tet_i2x(tet_tsroot));
	free(tet_tsroot);
	tet_tsroot = rstrstore(dest);
	TRACE2(tet_Ttcc, 1, "new tet_tsroot = %s", tet_tsroot);
}


#ifndef TET_LITE	/* -START-LITE-CUT- */

/*
**	rtrcopy() - copy the test suite to the runtime directory
**		on each of the remote systems if so required
*/

void rtrcopy()
{
	register int sysid, sysmax;
	char *rtdir;

	for (sysid = 1, sysmax = symax(); sysid <= sysmax; sysid++)
		if (syfind(sysid) != (struct systab *) 0 &&
			(rtdir = getdcfg("TET_RUN", sysid)) != (char *) 0)
				rtrc2(sysid, rtdir);
}

/*
**	rtrc2() - extend the rtrcopy() processing for a particular system
*/

static void rtrc2(sysid, rtdir)
int sysid;
char *rtdir;
{
	static char fmt[] = "can't copy test suite %.*s to runtime directory %.*s on system";
	char msg[sizeof fmt + (MAXPATH * 2)];
	char dest[MAXPATH];
	char *tsroot;

	/* determine the name of the destination directory */
	tsroot = getdcfg("TET_TSROOT", sysid);
	ASSERT(tsroot);
	fullpath(rtdir, tet_basename(tsroot), dest, sizeof dest, 1);

	TRACE4(tet_Ttcc, 1,
		"copying test suite %s to run-time directory %s on system %s",
		tsroot, rtdir, tet_i2a(sysid));

	/* do the copy */
	errno = 0;
	if (tet_tcrcopy(sysid, tsroot, dest) < 0) {
		(void) sprintf(msg, fmt, MAXPATH, tsroot, MAXPATH, dest);
		if (!IS_ER_ERRNO(tet_tcerrno))
			errno = 0;
		fatal(errno ? errno : tet_tcerrno, msg, tet_i2a(sysid));
	} 
}

#endif /* !TET_LITE */	/* -END-LITE-CUT- */


/*
**	inittmpdir() - create a temporary execution directory if necessary
**		on each system for use when TET_EXEC_IN_PLACE is false
*/

void inittmpdir()
{
	register int sysid, sysmax;
	register struct systab *sp;

	for (sysid = 0, sysmax = symax(); sysid <= sysmax; sysid++)
		if ((sp = syfind(sysid)) != (struct systab *) 0)
			inittd2(sp);
}

/*
**	inittd2() - extend the inittmpdir() processing for a particular system
*/

static void inittd2(sp)
struct systab *sp;
{
	static char tmpdirname[] = "TET_TMP_DIR";
	char buf[MAXPATH];
	char *tdir;

	TRACE2(tet_Ttcc, 4, "inittd2(%s)", tet_i2a(sp->sy_sysid));

	ASSERT_LITE(sp->sy_sysid == 0);

	/*
	** if no temporary directory has been specified on this system,
	** create it if necessary and install the name in the distributed
	** configuration for this system
	*/
	if ((tdir = getdcfg(tmpdirname, sp->sy_sysid)) == (char *) 0) {
		inittd3(sp->sy_sysid, buf, sizeof buf);
		tdir = buf;
		putdcfg(tmpdirname, sp->sy_sysid, tdir);
	}

	TRACE3(tet_Ttcc, 1, "TET_TMP_DIR on system %s = %s",
		tet_i2a(sp->sy_sysid), tdir);
}

/*
**	inittd3() - determine the default location for the temporary
**		directory and create it if necessary
**
**	return the name of the default location
*/

static void inittd3(sysid, tdir, tdirlen)
int sysid, tdirlen;
char tdir[];
{
	static char fmt[] = "can't create directory %.*s on system";
	char msg[sizeof fmt + MAXPATH];
	char *tsroot;

	TRACE2(tet_Ttcc, 4, "inittd3(): sysid = %s", tet_i2a(sysid));

	/* determine the name of the default tmpdir */
	tsroot = get_runtime_tsroot(sysid);
	ASSERT(tsroot);
	fullpath(tsroot, "tet_tmp_dir", tdir, tdirlen, sysid > 0 ? 1 : 0);

	/* return now if this directory exists already */
	if (tcc_access(sysid, tdir, 0) == 0)
		return;

	/* here to create the tmpdir */
	errno = 0;
	if (tcc_mkdir(sysid, tdir) < 0) {
		(void) sprintf(msg, fmt, MAXPATH, tdir);
		fatal(errno ? errno : tet_tcerrno, msg, tet_i2a(sysid));
	}

	/* all OK so return */
	TRACE3(tet_Ttcc, 2, "created directory %s on system %s",
		tdir, tet_i2a(sysid));
}

/*
**	tcc_mktmpdir() - create the temporary directory for use when
**		TET_EXEC_IN_PLACE is false
**
**	return 0 if successful or -1 on error
**
**	if successful, the name of the newly-created directory is returned
**	indirectly through *tdp
*/

int tcc_mktmpdir(prp, tmproot, tdp)
struct proctab *prp;
char *tmproot, **tdp;
{

	ASSERT_LITE(*prp->pr_sys == 0);

	/* create the temporary directory */
#ifdef TET_LITE	/* -LITE-CUT-LINE- */
	if ((tet_tcerrno = tcf_mktmpdir(tmproot, tdp)) != ER_OK)
		*tdp = (char *) 0;
#else	/* -START-LITE-CUT- */
	*tdp = tet_tcmktmpdir(*prp->pr_sys, tmproot);
#endif /* TET_LITE */	/* -END-LITE-CUT- */

	/* handle an error return */
	if (*tdp == (char *) 0) {
		if (!IS_ER_ERRNO(tet_tcerrno))
			errno = 0;
		prperror(prp, *prp->pr_sys, errno ? errno : tet_tcerrno,
			"can't create temporary directory below", tmproot);
		return(-1);
	}

	TRACE3(tet_Ttcc, 4,
		"created temporary execution directory %s on system %s",
		*tdp, tet_i2a(*prp->pr_sys));

	return(0);
}

/*
**	tcc_mkalldirs() - make directories recursively
**
**	return 0 if successful or -1 on error
*/

int tcc_mkalldirs(prp, dir)
struct proctab *prp;
char *dir;
{
	int rc;

	ASSERT_LITE(*prp->pr_sys == 0);

#ifdef TET_LITE	/* -LITE-CUT-LINE- */
	rc = tet_mkalldirs(dir);
	tet_tcerrno = rc < 0 ? tet_maperrno(errno) : ER_OK;
#else /* TET_LITE */	/* -START-LITE-CUT- */
	rc = tet_tcmkalldirs(*prp->pr_sys, dir);
#endif /* TET_LITE */	/* -END-LITE-CUT- */

	if (rc < 0) {
		if (!IS_ER_ERRNO(tet_tcerrno))
			errno = 0;
		prperror(prp, *prp->pr_sys, errno ? errno : tet_tcerrno,
			"can't recursively make directory", dir);
		return(-1);
	}

	return(0);
}

/*
**	tcc_rmtmpdir() - remove the temporary directory which is used when
**		TET_EXEC_IN_PLACE is false
**
**	return 0 if successful or -1 on error
*/

int tcc_rmtmpdir(prp, tmpdir)
struct proctab *prp;
char *tmpdir;
{
	struct systab *sp;
	char *tetroot;
	int err, rc;

	/*
	** if we are currently in the tmpdir subtree (or don't yet know where
	** we are), go back to TET_ROOT
	*/
	sp = syfind(*prp->pr_sys);
	ASSERT(sp);
	
	if (sp->sy_cwd) {
#ifdef _WIN32	/* -START-WIN32-CUT- */
		rc = _strnicmp(tmpdir, sp->sy_cwd, strlen(tmpdir));
#else		/* -END-WIN32-CUT- */
		rc = strncmp(tmpdir, sp->sy_cwd, strlen(tmpdir));
#endif		/* -WIN32-CUT-LINE- */
	}
	else
		rc = 0;

	if (!rc) {
		tetroot = getdcfg("TET_ROOT", *prp->pr_sys);
		ASSERT(tetroot && *tetroot);
		if (sychdir(sp, tetroot) < 0) {
			prperror(prp, *prp->pr_sys, errno ? errno : tet_tcerrno,
				"can't change directory to", tetroot);
			return(-1);
		}
	}

	/* remove the tmpdir */
#ifdef TET_LITE	/* -LITE-CUT-LINE- */
	rc = tcf_rmrf(tmpdir);
	err = errno;
#else	/* -START-LITE-CUT- */
	rc = tet_tcrmalldirs(*prp->pr_sys, tmpdir);
	err = tet_tcerrno;
#endif /* TET_LITE */	/* -END-LITE-CUT- */
	if (rc < 0) {
		prperror(prp, *prp->pr_sys, err,
			"can't remove temporary directory subtree", tmpdir);
		return(-1);
	}

	TRACE3(tet_Ttcc, 4,
		"removed temporary execution directory %s on system %s",
		tmpdir, tet_i2a(*prp->pr_sys));
	return(0);
}

/*
**	tccopy() - copy the test case directory to the temporary
**		execution directory when TET_EXEC_IN_PLACE is false
**
**	return 0 if successful or -1 on error
*/

int tccopy(prp, from, to)
struct proctab *prp;
char *from, *to;
{
	static char fmt[] = "can't copy test case directory %.*s to temporary directory";
	char msg[sizeof fmt + MAXPATH];
	int err, rc;

#ifdef TET_LITE	/* -LITE-CUT-LINE- */
	if ((rc = tet_fcopy(from, to)) < 0)
		err = errno;
#else	/* -START-LITE-CUT- */
	if ((rc = tet_tcrcopy(*prp->pr_sys, from, to)) < 0)
		err = IS_ER_ERRNO(tet_tcerrno) ? errno : tet_tcerrno;
#endif /* TET_LITE */	/* -END-LITE-CUT- */
	if (rc < 0) {
		(void) sprintf(msg, fmt, MAXPATH, from);
		prperror(prp, *prp->pr_sys, err, msg, to);
		return(-1);
	}

	TRACE3(tet_Ttcc, 4, "copied test case directory %s to %s", from, to);

	return(0);
}


#ifndef TET_LITE	/* -START-LITE-CUT- */

/*
**	initsfdir() - create saved files directories on each remote system
*/

void initsfdir()
{
	register int sysid, sysmax;
	register struct systab *sp;
	int rc = 0;

	for (sysid = 1, sysmax = symax(); sysid <= sysmax; sysid++)
		if ((sp = syfind(sysid)) != (struct systab *) 0 &&
			init1sfdir(sp) < 0)
				rc = -1;

	if (rc < 0)
		tcc_exit(1);
}

/*
**	init1sfdir() - create the saved files directory on a single
**		remote system
**
**	return 0 if successful or -1 on error
*/

static int init1sfdir(sp)
struct systab *sp;
{
	static char fmt[] =
		"can't create saved files directory %s%.*s on system";
	static char below[] = "below ";
	char msg[sizeof fmt + sizeof below + MAXPATH];
	char resroot[MAXPATH];
	char *sfdir, *tsroot;

	TRACE2(tet_Ttcc, 4, "initsfdir(%s)", tet_i2a(sp->sy_sysid));

	/*
	** determine the name of the results directory root on the
	** remote system
	*/
	tsroot = get_runtime_tsroot(sp->sy_sysid);
	ASSERT(tsroot);
	fullpath(tsroot, "results", resroot, sizeof resroot, 1);

	/* create the results directory on the remote system if necessary */
	if (tet_tcmkalldirs(sp->sy_sysid, resroot) < 0) {
		(void) sprintf(msg, fmt, "",
			sizeof msg - sizeof fmt, resroot);
		if (!IS_ER_ERRNO(tet_tcerrno))
			errno = 0;
		error(errno ? errno : tet_tcerrno, msg, tet_i2a(sp->sy_sysid));
		return(-1);
	}

	/* create the saved files directory on the remote system */
	if ((sfdir = tet_tcmksdir(sp->sy_sysid, resroot, resdirsuffix())) == (char *) 0) {
		(void) sprintf(msg, fmt, below,
			sizeof msg - sizeof fmt - sizeof below, resroot);
		error(tet_tcerrno, msg, tet_i2a(sp->sy_sysid));
		return(-1);
	}

	/* all OK so remember the directory name and return */
	sp->sy_sfdir = rstrstore(sfdir);
	return(0);
}

#endif /* !TET_LITE */	/* -END-LITE-CUT- */


/*
**	sfproc() - perform save files processing on a single system
**
**	return 0 if successful or -1 on error
*/

int sfproc(prp, sfiles, nsfiles)
register struct proctab *prp;
char **sfiles;
int nsfiles;
{
	static char fmt[] = "can't copy save files from %.*s on system %03d to";
	char msg[sizeof fmt + MAXPATH + LNUMSZ];
	struct systab *sp;
	int rc;
	int tsfiles = 0;
	char savedir[MAXPATH];
#ifndef TET_LITE	/* -START-LITE-CUT- */
	char path[MAXPATH];
	static char remote[] = "REMOTE%03d";
	char subdir[sizeof remote];
#endif /* !TET_LITE */	/* -END-LITE-CUT- */


	TRACE3(tet_Ttcc, 4, "sfproc(%s): sysid = %s",
		tet_i2x(prp), tet_i2a(*prp->pr_sys));

	ASSERT(prp->pr_nsys == 1);
	ASSERT_LITE(*prp->pr_sys == 0);

	/* change directory to the test case execution directory */
	sp = syfind(*prp->pr_sys);
	ASSERT(sp);
	if (sychdir(sp, prp->pr_tcedir) < 0) {
		prperror(prp, *prp->pr_sys, errno ? errno : tet_tcerrno,
			"can't change directory to", prp->pr_tcedir);
		return(-1);
	}

	/* then do the save */
#ifdef TET_LITE	/* -LITE-CUT-LINE- */

	tsfiles = 0;
	tcexecdir(prp, resdirname(), savedir, sizeof savedir);
	tet_tcerrno = tcf_procdir(".", savedir, sfiles, nsfiles, TCF_TS_LOCAL);
	rc = (tet_tcerrno == ER_OK) ? 0 : -1;

#else /* TET_LITE */	/* -START-LITE-CUT- */

	/*
	** if TET_TRANSFER_SAVE_FILES is true for this system, we must
	** copy the files to REMOTEnnn below the results directory on
	** the local system; if this is the local system, TCCD can do
	** this unaided (tet_tctslfiles()), otherwise TCCD must work with
	** XRESD to do this (tet_tctsmfiles())
	**
	** if TET_TRANSFER_SAVE_FILES is false for this system, we must
	** copy the files to the saved files directory on this system;
	** TCCD can always do this unaded
	*/

	tsfiles = getcflag("TET_TRANSFER_SAVE_FILES", *prp->pr_sys,
		prp->pr_currmode);

	if (tsfiles) {
		(void) sprintf(subdir, remote, *prp->pr_sys % 1000);
		fullpath(resdirname(), subdir, path, sizeof path,
			*prp->pr_sys ? 1 : 0);
		tcexecdir(prp, path, savedir, sizeof savedir);
		if (*prp->pr_sys > 0)
			rc = tet_tctsmfiles(*prp->pr_sys, sfiles, nsfiles,
				savedir + strlen(resdirname()) + 1);
		else
			rc = tet_tctslfiles(*prp->pr_sys, sfiles, nsfiles,
				(char *) 0, savedir);
	}
	else
	{
		tcexecdir(prp,
			(*prp->pr_sys > 0) ? sp->sy_sfdir : resdirname(),
			savedir, sizeof savedir);
		rc = tet_tctslfiles(*prp->pr_sys, sfiles, nsfiles,
			(char *) 0, savedir);
	}

#endif /* TET_LITE */	/* -END-LITE-CUT- */

	if (rc < 0) {
		(void) sprintf(msg, fmt, MAXPATH, prp->pr_tcedir, *prp->pr_sys);
		prperror(prp, tsfiles ? 0 : *prp->pr_sys, tet_tcerrno,
			msg, savedir);
	}

	return(rc);
}

/*
**	inittft() - initialise the Transfer File Types subsystem
**
**	there is no return on error
*/

void inittft()
{
#ifndef TET_LITE	/* -START-LITE-CUT- */

	static char tet_transfer_file_types[] = "TET_TRANSFER_FILE_TYPES";
	char fname[MAXPATH];
	int done, sysid, sysmax;
	int errors = 0;
	char *p;

	TRACE1(tet_Ttcc, 1, "call to inittft()");

	/* determine the name of the TFT file */
	if (tcc_modes & TCC_BUILD)
		p = getmcfg(tet_transfer_file_types, TCC_BUILD);
	else
		p = (char *) 0;
	if ((!p || !*p) && (tcc_modes & TCC_EXEC))
		p = getmcfg(tet_transfer_file_types, TCC_EXEC);
	if ((!p || !*p) && (tcc_modes & TCC_CLEAN))
		p = getmcfg(tet_transfer_file_types, TCC_CLEAN);
	if (!p || !*p)
		p = "tet_transfer_file_types";

	/* pick up the generic TFT file if there is one */
	done = 0;
	fullpath(tet_root, p, fname, sizeof fname, 0);
	if (tet_eaccess(fname, 04) == 0) {
		if (process_tftfile(fname) < 0)
			errors++;
		else
			done = 1;
	}

	/* pick up the testsuite-specific result code file if there is one */
	fullpath(tet_tsroot, p, fname, sizeof fname, 0);
	if (tet_eaccess(fname, 04) == 0) {
		if (process_tftfile(fname) < 0)
			errors++;
		else
			done = 1;
	}

	if (errors)
		tcc_exit(1);

	/* if we have a list of file types, send it to each system */
	if (done) {
		for (sysid = 0, sysmax = symax(); sysid <= sysmax; sysid++)
			if (syfind(sysid) && tet_tcsndftype(sysid) < 0) {
				error(tet_tcerrno,
					"can't send file type list to system",
					tet_i2a(sysid));
				errors++;
			}
	}

	if (errors)
		tcc_exit(1);

#endif /* !TET_LITE */	/* -END-LITE-CUT- */
}



#ifndef TET_LITE	/* -START-LITE-CUT- */

/*
**	process_tftfile() - read in a Transfer File Types file
**
**	return 0 if successful or -1 on error
*/
static int process_tftfile(fname)
char *fname;
{
	FILE *fp;
	int lineno;
	char *p;
	char line[LBUFLEN];
	char *flds[3];
	int ftype, nflds, rc;
	char badopt[2];

	TRACE2(tet_Ttcc, 4, "call to process_tftfile(): fname = %s", fname);

	if ((fp = fopen(fname, "r")) == (FILE *) 0) {
		error(errno, "can't open", fname);
		return(-1);
	}

	/* read each line in turn */
	lineno = 0;
	rc = 0;
	while (fgets(line, sizeof line, fp) != (char *) 0) {
		lineno++;
		/* strip a trailing newline */
		for (p = line; *p; p++)
			if (*p == '\n') {
				*p = '\0';
				break;
			}
		/* split the line into fields */
		nflds = split(line, flds, sizeof flds / sizeof flds[0], ' ');
		/* ignore comments */
		if (nflds >= 1 && strcmp(flds[0], "#") == 0)
			continue;
		/* ensure that we have the correct number of fields */
		switch (nflds) {
		case 0:
			continue;
		case 2:
			break;
		default:
			insferror("wrong number of fields", (char *) 0,
				lineno, fname);
			rc = -1;
			continue;
		}
		/* interpret the file type field */
		switch (*flds[1]) {
		case 'a':
			ftype = TET_FT_ASCII;
			break;
		case 'b':
			ftype = TET_FT_BINARY;
			break;
		default:
			badopt[0] = *flds[1];
			badopt[1] = '\0';
			insferror("unknown file type indicator",
				badopt, lineno, fname);
			rc = -1;
			continue;
		}
		if (tet_addftype(flds[0], ftype) < 0) {
			rc = -1;
			break;
		}
	}

	fclose(fp);
	return(rc);
}

/*
**	copy_sfiles2rmt() - copy source files to one or more remote systems
**
**	ifp and ifname refer to the transsnfer source files instruction file
**
**	return 0 to continue with the build stage, or -1 to abandon the
**	build stage
*/
int copy_sfiles2rmt(prp, ifp, ifname)
struct proctab *prp;
FILE *ifp;
char *ifname;
{
	char line[LBUFLEN];
	char *flds[5];
	int nflds;
	int lineno = 0;
	int errors = 0;
	char *p;
	struct proctab proctmp;
	struct proctab *child, *prp0;
	int sys0 = 0;
	char srcdir[MAXPATH];
	int rc;

	TRACE2(tet_Ttcc, 4, "copy_sfiles2rmt(): ifname = %s", ifname);

	/*
	** If there is only one system (which could be remote), prp
	** refers to that system.
	** If there is more than one system, there will be child proctabs
	** for each system.
	** If the local system is not in the system list, there is no
	** proctab for the local system, so we must fudge one here.
	*/

	/* make prp0 point to the local system's proctab if there is one */
	prp0 = (struct proctab *) 0;
	if (prp->pr_nsys > 1) {
		ASSERT(prp->pr_child);
		for (child = prp->pr_child; child; child = child->pr_lforw)
			if (*child->pr_sys == 0) {
				prp0 = child;
				break;
			}
	}
	else if (*prp->pr_sys == 0)
		prp0 = prp;

	/*
	** if no proctab for the local system could be found, we'll have to
	** fake one
	*/
	if (prp0 == (struct proctab *) 0) {
		proctmp = *prp;
		proctmp.pr_sys = &sys0;
		proctmp.pr_nsys = 1;
		prp0 = &proctmp;
	}

	/* determine the location of the source directory on the local system */
	tcsrcdir(prp0, srcdir, sizeof srcdir);


	/* read each line from the instruction file and process it */
	while (fgets(line, sizeof line, ifp) != (char *) 0) {
		lineno++;
		/* strip a trailing newline */
		for (p = line; *p; p++)
			if (*p == '\n') {
				*p = '\0';
				break;
			}
		/* split the line into fields */
		nflds = split(line, flds, sizeof flds / sizeof flds[0], ' ');
		/* ignore comments */
		if (nflds >= 1 && strcmp(flds[0], "#") == 0)
			continue;
		/* ensure that we have the correct number of fields */
		switch (nflds) {
		case 0:
			continue;
		case 3:
		case 4:
			break;
		default:
			insferror("wrong number of fields", (char *) 0,
				lineno, ifname);
			errors++;
			continue;
		}
		/* process the broken-out line */
		if (copy_sf2r2(prp, srcdir, flds, nflds, ifname, lineno) < 0)
			errors++;
	}

	rc = errors ? -1 : 0;
	TRACE2(tet_Ttcc, 4, "copy_sfiles2rmt() returns %s", tet_i2a(rc));
	return(rc);
}

/*
**	copy_sf2r2() - extend the copy_sfiles2rmt() processing
**
**
**	return 0 if successful or -1 on error
*/
static int copy_sf2r2(prp, srcdir, flds, nflds, ifname, lineno)
struct proctab *prp;
char *flds[];
int nflds, lineno;
char *srcdir, *ifname;
{
	static int *sys;
	static int syslen;
	int dups, n, nsys, ok;
	int *ip1, *ip2;
	int needlen, scount;
	int istart, iend;
#  define MAXARGS (LBUFLEN / 4)
#  if MAXARGS < 10
#    define MAXARGS 10
#  endif
	char *sysids[MAXARGS];
	int nsysids;
	char **ap, *p;
	int errors = 0;
	int options;
	char badopt[2];

	TRACE5(tet_Ttcc, 8, "copy_sf2r2(): insfile: src = <%s>, dest = <%s>, sys = <%s>, options = <%s>",
		flds[0], flds[1], flds[2], nflds >= 4 ? flds[3] : "NONE");

	/* ignore a non-relative source or destination path name */
	if (isabspathloc(flds[0]) || isabspathrem(flds[1])) {
		insferror("absolute path name not permitted", (char *) 0,
			lineno, ifname);
		errors++;
	}

	/* check for .. in path names */
	if (copy_sf2dotchk(flds[0]) || copy_sf2dotchk(flds[1])) {
		insferror("path component \"..\" not permitted", (char *) 0,
			lineno, ifname);
		errors++;
	}

	if (errors)
		return(-1);

	/*
	** parse the system ID list
	*/
	nsys = 0;
	if (strcmp(flds[2], "all") == 0) {
		needlen = sizeof *sys * (nsys + prp->pr_nsys);
		RBUFCHK((char **) &sys, &syslen, needlen);
		ip2 = sys + nsys;
		for (ip1 = prp->pr_sys; ip1 < prp->pr_sys + prp->pr_nsys; ip1++)
			if (*ip1 > 0) {
				*ip2++ = *ip1;
				nsys++;
			}
	}
	else {
		nsysids = split(flds[2], sysids,
			sizeof sysids / sizeof sysids[0], ',');
		for (ap = sysids; ap < sysids + nsysids; ap++) {
			scount = 1;
			if (isnumrange(*ap, &istart, &iend))
				scount += iend - istart;
			else
				istart = iend = atoi(*ap);
			needlen = sizeof *sys * (nsys + scount);
			RBUFCHK((char **) &sys, &syslen, needlen);
			ip2 = sys + nsys;
			for (n = istart; n <= iend; n++) {
				if (n == 0) {
					insferror("ignored local system ID",
						(char *) 0, lineno, ifname);
					continue;
				}
				ASSERT(n > 0);
				dups = 0;
				for (ip1 = sys; ip1 < sys + nsys; ip1++)
					if (*ip1 == n) {
						dups = 1;
						break;
					}
				if (dups) {
					insferror("ignored duplicate "
						"system ID", tet_i2a(n),
						lineno, ifname);
					continue;
				}
				ok = 0;
				for (ip1 = prp->pr_sys; ip1 < prp->pr_sys + prp->pr_nsys; ip1++)
					if (*ip1 == n) {
						ok = 1;
						break;
					}
				if (!ok) {
					insferror("ignored non-participating "
						"system ID", tet_i2a(n),
						lineno, ifname);
					continue;
				}
				*ip2++ = n;
				nsys++;
			}
		}
	}

	/* parse the options field */
	options = 0;
	if (nflds >= 4) {
		for (p = flds[3]; *p; p++)
			switch (*p) {
			case 'a':
				options |= SF_FTYPE_ASCII;
				break;
			case 'b':
				options |= SF_FTYPE_BINARY;
				break;
			case 'm':
				options |= SF_KEEP_MTIME;
				break;
			case 'u':
				options |= SF_UNCOND_COPY;
				break;
			default:
				badopt[0] = *p;
				badopt[1] = '\0';
				insferror("ignored unknown option",
					badopt, lineno, ifname);
				break;
			}
	}

	/* resolve an ascii/binary conflict */
	if (
		(options & (SF_FTYPE_ASCII | SF_FTYPE_BINARY)) ==
			(SF_FTYPE_ASCII | SF_FTYPE_BINARY)
	) {
		insferror("both ascii and binary file types specified",
			"(assuming binary)", lineno, ifname);
		options &= ~SF_FTYPE_ASCII;
	}

	/* make sure we have at least one remote system */
	if (nsys == 0) {
		insferror("ignored entry -", "no valid remote system IDs",
			lineno, ifname);
		return(0);
	}

	/*
	** here:
	**	flds[0] is the source path from the instruction file
	**
	**	flds[1] is the destination directory from the instruction file
	**
	**	bits in options represent the set of option flags
	**	from the instruction file
	**
	**	sys points to the list of remote systems to use,
	**	and nsys defines the number of remote systems in the list
	*/

	/* process the next component of the source path */
	return(copy_sf2r3(prp, srcdir, "", flds[0], flds[1], options,
		sys, nsys));
}

/*
**	copy_sf2r3() - inspect an element in the source path
**
**	this function may be called recursively
**
**	at any level:
**		head is the concatenation of elements already processed -
**			it is a full path name
**		elem is the current element to consider
**		tail is the concatenation of elements yet to be processed
**
**	if the current element contains pattern matching characters,
**	the function is called once for each match;
**	otherwise, the current element is appended to head, the next
**	element is shifted off tail, and the function is called again
**
**	the copy is done once all the elements have been shifted into head
**
**	return 0 if successful or -1 on error
*/
static int copy_sf2r3(prp, head, elem, tail, destdir, options, sys, nsys)
struct proctab *prp;
char *head, *elem, *tail, *destdir;
int options, *sys, nsys;
{
	int rc;

#  ifndef NOTRACE
	static int level;

	level++;
	TRACE2(tet_Ttcc, 8, "copy_sf2r3(): enter at level %s", tet_i2a(level));
	TRACE5(tet_Ttcc, 8,
		"head = <%s>, elem = <%s>, tail = <%s>, destdir = <%s>",
		head, elem, tail, destdir);
#  endif

	rc = copy_sf2r3_2(prp, head, elem, tail, destdir, options, sys, nsys);

#  ifndef NOTRACE
	TRACE3(tet_Ttcc, 8, "copy_sf2r3(): return %s from level %s",
		tet_i2a(rc), tet_i2a(level));
	--level;
#  endif

	return(rc);
}

static int copy_sf2r3_2(prp, head, elem, tail, destdir, options, sys, nsys)
struct proctab *prp;
char *head, *elem, *tail, *destdir;
int options, *sys, nsys;
{
	char newelem[MAXPATH];
	char path[MAXPATH];
	char *newhead;
	char *p1, *p2;
	int len1, len2;
	int rc;

	ASSERT(isabspathloc(head));

	/* perform pattern-matching if appropriate */
	if (*elem && ispatt(elem)) {
		return(copy_sf2patmatch(prp, head, elem, tail, destdir,
			options, sys, nsys));
	}

	/*
	** here if the current element is not a pattern
	**
	** if we have a current element:
	**	append it to the current head
	*/
	if (*elem) {
		len1 = (int) sizeof path - 1;
		p1 = path;
		sprintf(p1, "%.*s", (int) sizeof path - 1, head);
		len2 = strlen(path);
		p1 += len2;
		len1 -= len2;
		if (len1 > 0)
			sprintf(p1, "/%.*s", len1 - 1, elem);
		newhead = path;
	}
	else
		newhead = head;

	/*
	** if there is a tail:
	**	pop the first element off the tail and recurse
	** otherwise:
	**	start the copy operation
	*/
	if (*tail) {
		p2 = newelem;
		for (p1 = tail; *p1; p1++) {
			if (isdirsep(*p1)) {
				do {
					p1++;
				} while (*p1 && isdirsep(*p1));
				break;
			}
			else
				*p2++ = *p1;
		}
		*p2 = '\0';
		rc = copy_sf2r3(prp, newhead, newelem, p1, destdir,
			options, sys, nsys);
	}
	else
		rc = copy_sf2r4(prp, newhead, destdir, options, sys, nsys);

	return(rc);
}


/*
**	copy_sf2r4() - copy the file or directory to all target systems
**
**	return 0 if successful or -1 on error
*/
static int copy_sf2r4(prp, srcpath, destdir, options, sys, nsys)
struct proctab *prp;
char *srcpath, *destdir;
int options, *sys, nsys;
{
	int rc;

#  ifndef NOTRACE
	static int level;

	level++;
	TRACE2(tet_Ttcc, 8, "copy_sf2r4(): enter at level %s", tet_i2a(level));
	TRACE4(tet_Ttcc, 8, "srcpath = <%s>, destdir = <%s>, options = %s",
		srcpath, destdir, prsfopts(options));
#  endif

	rc = copy_sf2r4_2(prp, srcpath, destdir, options, sys, nsys);

#  ifndef NOTRACE
	TRACE3(tet_Ttcc, 8, "copy_sf2r4(): return %s from level %s",
		tet_i2a(rc), tet_i2a(level));
	--level;
#  endif

	return(rc);
}

static int copy_sf2r4_2(prp, srcpath, destdir, options, sys, nsys)
struct proctab *prp;
char *srcpath, *destdir;
int options, *sys, nsys;
{
	struct STAT_ST stbuf;
	int errors;
	int *ip;


	/* see if the source is a file or a directory */
	if (STAT(srcpath, &stbuf) < 0) {
		error(errno, "can't stat", srcpath);
		return(-1);
	}

	/* process a directory */
	if (S_ISDIR(stbuf.st_mode))
		return(copy_sf2r4dir(prp, srcpath, destdir, options,
			sys, nsys));

	/* ignore a non-regular file */
	if (!S_ISREG(stbuf.st_mode)) {
		fprintf(stderr, "%s: ignored non-regular file %s\n",
			tet_progname, srcpath);
		return(0);
	}

	/*
	** here, srcpath is known to be a regular file
	**
	** extend the copy processing to each target system in turn
	*/
	errors = 0;
	for (ip = sys; ip < sys + nsys; ip++)
		if (copy_sf2r5(prp, srcpath, &stbuf, destdir, options, *ip) < 0)
			errors++;

	return(errors ? -1 : 0);
}


/*
**	copy_sf2r4dir() - extend the processing from copy_sf2r4()
**		when srcpath is known to be a directory
**
**	return 0 if successful or -1 on error
*/
static int copy_sf2r4dir(prp, srcpath, destdir, options, sys, nsys)
struct proctab *prp;
char *srcpath, *destdir;
int options, *sys, nsys;
{
	int rc;

#  ifndef NOTRACE
	static int level;

	level++;
	TRACE2(tet_Ttcc, 8, "copy_sf2r4dir(): enter at level %s",
		tet_i2a(level));
	TRACE3(tet_Ttcc, 8, "srcpath = <%s>, destdir = <%s>",
		srcpath, destdir);
#  endif

	rc = copy_sf2r4dir_2(prp, srcpath, destdir, options, sys, nsys);

#  ifndef NOTRACE
	TRACE3(tet_Ttcc, 8, "copy_sf2r4dir(): return %s from level %s",
		tet_i2a(rc), tet_i2a(level));
	--level;
#  endif

	return(rc);
}

static int copy_sf2r4dir_2(prp, srcpath, destdir, options, sys, nsys)
struct proctab *prp;
char *srcpath, *destdir;
int options, *sys, nsys;
{
	char *srcfile;
	char **files, **fip;
	char newsrcpath[MAXPATH];
	char path[MAXPATH];
	char *newdestdir;
	int errors, len1, len2, rc;
	char *p;

	/*
	** if the last component of srcpath is not a . :
	**	append it to destdir
	**
	** this operation replaces a destdir consisting of a single .
	*/
	srcfile = tet_basename(srcpath);
	if (strcmp(srcfile, ".") != 0) {
		p = path;
		len1 = (int) sizeof path - 1;
		if (strcmp(destdir, ".") != 0) {
			sprintf(p, "%.*s/", len1 - 1, destdir);
			len2 = strlen(p);
			p += len2;
			len1 -= len2;
		}
		else
			*p = '\0';
		if (len1 > 0)
			sprintf(p, "%.*s", len1, srcfile);
		newdestdir = path;
	}
	else
		newdestdir = destdir;

	/* get a directory listing */
	if ((files = tet_lsdir(srcpath)) == (char **) 0)
		return(-1);

	/*
	** make p point to the source path to use
	**
	** this operation discards a trailing . component that we don't
	** need any more
	*/
	len1 = (int) sizeof newsrcpath - 1;
	if (strcmp(srcfile, ".") == 0)
		tcc_dirname(srcpath, newsrcpath, sizeof newsrcpath);
	else
		sprintf(newsrcpath, "%.*s", len1, srcpath);
	len2 = strlen(newsrcpath);
	p = newsrcpath + len2;
	len1 -= len2;

	/* process each directory entry in turn */
	errors = 0;
	for (fip = files; *fip; fip++) {
		if (len1 > 0)
			sprintf(p, "/%.*s", len1 - 1, *fip);
		rc = copy_sf2r4(prp, newsrcpath, newdestdir, options,
			sys, nsys);
		if (rc < 0)
			errors++;
	}

	/* free up memory allocated by tet_lsdir() */
	for (fip = files; *fip; fip++) {
		TRACE2(tet_Tbuf, 6, "sf2dir: free file name = %s",
			tet_i2x(*fip));
		free((void *) *fip);
	}
	TRACE2(tet_Tbuf, 6, "sf2dir: free file list = %s", tet_i2x(files));
	free((void *) files);

	return(errors ? -1 : 0);
}


/*
**	copy_sf2r5() - copy the source file to a single target system
**
**	return 0 if successful or -1 on error
*/
static int copy_sf2r5(prp, srcpath, stp, destdir, options, sysid)
struct proctab *prp;
char *srcpath, *destdir;
struct STAT_ST *stp;
int options, sysid;
{
	int rc;

	TRACE5(tet_Ttcc, 8, "copy_sf2r5(): srcpath = <%s>, destdir = <%s>, options = %s, sysid = %s",
		srcpath, destdir, prsfopts(options), tet_i2a(sysid));

	rc = copy_sf2r5_2(prp, srcpath, stp, destdir, options, sysid);

	TRACE2(tet_Ttcc, 8, "copy_sf2r5() returns %s", tet_i2a(rc));
	return(rc);
}

static int copy_sf2r5_2(prp, srcpath, stp, destdir, options, sysid)
struct proctab *prp;
char *srcpath, *destdir;
struct STAT_ST *stp;
int options, sysid;
{
	char remsrcdir[MAXPATH];
	char destpath[MAXPATH];
	struct proctab *child, *q;
	int len1, len2, rc;
	char *mode;
	long mtime;
	FILE *sfp;
	char *p;

	/* find the proctab entry for the remote system */
	if (prp->pr_nsys > 1) {
		ASSERT(prp->pr_child);
		for (child = prp->pr_child; child; child = child->pr_lforw)
			if (*child->pr_sys == sysid)
				break;
		ASSERT(child);
		q = child;
	}
	else {
		ASSERT(*prp->pr_sys == sysid);
		q = prp;
	}

	/*
	** determine the location of the test case source directory on
	** the remote system
	*/
	tcsrcdir(q, remsrcdir, sizeof remsrcdir);

	/* determine the destination path name */
	p = destpath;
	len1 = (int) sizeof destpath - 1;
	sprintf(p, "%.*s", len1 - 1, remsrcdir);
	len2 = strlen(p);
	p += len2;
	len1 -= len2;
	if (len1 > 0 && strcmp(destdir, ".") != 0) {
		sprintf(p, "/%.*s", len1 - 1, destdir);
		len2 = strlen(p);
		p += len2;
		len1 -= len2;
	}
	if (len1 > 0)
		sprintf(p, "/%.*s", len1 - 1, tet_basename(srcpath));

	/*
	** don't overwrite an existing destination file
	** unless SF_UNCOND_COPY is set or the source file is newer
	*/
	if (
		(options & SF_UNCOND_COPY) == 0 &&
		tet_tcftime(sysid, destpath, (long *) 0, &mtime) == 0 &&
		stp->st_mtime <= (time_t) mtime
	) {
		TRACE3(tet_Ttcc, 8,
			"src mtime %s >= dest mtime %s: return without copying",
			tet_i2a(stp->st_mtime), tet_l2a(mtime));
		return(0);
	}

	/* determine the type of copy to perform */
	if ((options & (SF_FTYPE_ASCII | SF_FTYPE_BINARY)) == 0) {
		rc = tet_getftype(srcpath);
		switch (rc) {
		case 0:
		case TET_FT_ASCII:
			options |= SF_FTYPE_ASCII;
			break;
		case TET_FT_BINARY:
			options |= SF_FTYPE_BINARY;
			break;
		default:
			error(0, "unexpected tet_getftype() return",
				tet_i2a(rc));
			return(-1);
		}
	}

	/* open the source file */
	mode = (options & SF_FTYPE_BINARY) ? "rb" : "r";
	if ((sfp = fopen(srcpath, mode)) == (FILE *) 0) {
		error(errno, "can't open", srcpath);
		return(-1);
	}

	rc = copy_sf2r6(srcpath, stp, sfp, destpath, options, sysid);

	fclose(sfp);
	return(rc);
}

/*
**	copy_sf2r6() - extend the copy processing once the source file
**		is open
**
**	return 0 if successful or -1 on error
*/
static int copy_sf2r6(srcpath, stp, sfp, destpath, options, sysid)
char *srcpath, *destpath;
struct STAT_ST *stp;
FILE *sfp;
int options, sysid;
{
	int rc;

	TRACE5(tet_Ttcc, 8, "copy_sf2r6(): srcpath = <%s>, destpath = <%s>, sysid = %s, options = %s",
		srcpath, destpath, tet_i2a(sysid), prsfopts(options));

	rc = copy_sf2r6_2(srcpath, stp, sfp, destpath, options, sysid);

	TRACE2(tet_Ttcc, 8, "copy_sf2r6() returns %s", tet_i2a(rc));
	return(rc);
}

static int copy_sf2r6_2(srcpath, stp, sfp, destpath, options, sysid)
char *srcpath, *destpath;
struct STAT_ST *stp;
FILE *sfp;
int options, sysid;
{
	char destdir[MAXPATH];
	static char fmt1[] = "can't create directory %.*s on system";
	static char fmt2[] = "can't open file %.*s on system";
	static char fmt3[] = "close error on file %*.s on system";
	static char fmt4[] = "can't set file times on %*.s on system";
	/* NOTE: this works because fmt4 is the longest format string */
	char msg[sizeof fmt4 + MAXPATH];
	int dfid, rc;

	/* ensure that the destination directory exists */
	tcc_dirname(destpath, destdir, sizeof destdir);
	if (tet_tcmkalldirs(sysid, destdir) < 0) {
		sprintf(msg, fmt1, MAXPATH, destdir);
		if (!IS_ER_ERRNO(tet_tcerrno))
			errno = 0;
		error(errno ? errno : tet_tcerrno, msg, tet_i2a(sysid));
		return(-1);
	}

	/* open the destination file */
	dfid = tet_tcfopen(sysid, destpath,
		(options & SF_FTYPE_BINARY) ? 1 : 0);
	if (dfid < 0) {
		sprintf(msg, fmt2, MAXPATH, destpath);
		if (!IS_ER_ERRNO(tet_tcerrno))
			errno = 0;
		error(errno ? errno : tet_tcerrno, msg, tet_i2a(sysid));
		return(-1);
	}

	/* perform the copy */
	rc = copy_sf2r7(srcpath, sfp, destpath, dfid, sysid);

	/* close the destination file */
	if (tet_tcfclose(sysid, dfid) < 0) {
		sprintf(msg, fmt3, MAXPATH, destpath);
		if (!IS_ER_ERRNO(tet_tcerrno))
			errno = 0;
		error(errno ? errno : tet_tcerrno, msg, tet_i2a(sysid));
		return(-1);
	}

	/*
	** return now if an error has occurred , or if it is not required
	** to set the mod time on the destination file
	*/
	if (rc < 0 || (options & SF_KEEP_MTIME) == 0)
		return(rc);

	/* here to set the file times on the destination file */
	rc = tet_tcutime(sysid, destpath, (long) stp->st_atime,
			(long) stp->st_mtime);
	if (rc < 0) {
		sprintf(msg, fmt4, MAXPATH, destpath);
		if (!IS_ER_ERRNO(tet_tcerrno))
			errno = 0;
		error(errno ? errno : tet_tcerrno, msg, tet_i2a(sysid));
		return(-1);
	}

	return(0);
}

/*
**	copy_sf2r7() - extend the copy processing once the source and
**		destination files are both open
**
**	return 0 if successful or -1 on error
*/
static int copy_sf2r7(srcpath, sfp, destpath, dfid, sysid)
char *srcpath, *destpath;
FILE *sfp;
int dfid, sysid;
{
	static char fmt[] = "write error on file %.*s on system";
	char msg[sizeof fmt + MAXPATH];
	char buf[BT_DLEN];
	int nbytes;

	/* do the copy */
	while ((nbytes = fread((void *) buf, sizeof buf[0], sizeof buf, sfp)) > 0) {
		if (tet_tcfwrite(sysid, dfid, buf, nbytes) < 0) {
			sprintf(msg, fmt, MAXPATH, destpath);
			if (!IS_ER_ERRNO(tet_tcerrno))
				errno = 0;
			error(errno ? errno : tet_tcerrno, msg, tet_i2a(sysid));
			return(-1);
		}
	}
	if (ferror(sfp)) {
		error(errno, "read error on", srcpath);
		return(-1);
	}

	return(0);
}


/*
**	copy_sf2patmatch() - expand shell pattern matching characters
**		in the source path name
**
**	return 0 if successful or -1 on error
*/
static int copy_sf2patmatch(prp, head, pattern, tail, destdir, options,
	sys, nsys)
struct proctab *prp;
char *head, *pattern, *tail, *destdir;
int options, *sys, nsys;
{
	int rc;

#  ifndef NOTRACE
	static int level;

	level++;
	TRACE2(tet_Ttcc, 8, "copy_sf2patmatch(): enter at level %s",
		tet_i2a(level));
	TRACE4(tet_Ttcc, 8, "head = <%s>, pattern = <%s>, tail = <%s>",
		head, pattern, tail);
#  endif

	rc = copy_sf2patmatch_2(prp, head, pattern, tail, destdir, options,
		sys, nsys);

#  ifndef NOTRACE
	TRACE3(tet_Ttcc, 8, "copy_sf2patmatch(): return %s from level %s",
		tet_i2a(rc), tet_i2a(level));
	--level;
#  endif

	return(rc);
}

static int copy_sf2patmatch_2(prp, head, pattern, tail, destdir, options,
	sys, nsys)
struct proctab *prp;
char *head, *pattern, *tail, *destdir;
int options, *sys, nsys;
{
	struct STAT_ST stbuf;
	char **files, **fip;
	char path[MAXPATH];
	char elem[MAXPATH];
	char *p1, *p2;
	int len1, len2;
	int errors, rc, recurse;

	/* get a directory listing */
	if ((files = tet_lsdir(head)) == (char **) 0)
		return(-1);

	/*
	** process each directory entry that matches the specified pattern
	**
	** if a match is found:
	**	if tail is non-empty:
	**		if the next element in tail contains a pattern:
	**			recurse
	**		otherwise:
	**			if the current element is a directory and
	**			the next element in tail exists:
	**				recurse
	**	otherwise:
	**		recurse
	*/
	errors = 0;
	for (fip = files; *fip; fip++) {
		if (!tet_pmatch(*fip, pattern))
			continue;
		recurse = 0;
		if (*tail) {
			p2 = elem;
			for (p1 = tail; *p1; p1++)
				if (isdirsep(*p1))
					break;
				else
					*p2++ = *p1;
			*p2 = '\0';
			if (ispatt(elem))
				recurse = 1;
			else {
				p2 = path;
				len1 = (int) sizeof path - 1;
				sprintf(p2, "%.*s", len1, head);
				len2 = strlen(p2);
				p2 += len2;
				len1 -= len2;
				if (len1 > 0) {
					sprintf(p2, "/%.*s", len1 - 1, *fip);
					len2 = strlen(p2);
					p2 += len2;
					len1 -= len2;
				}
				if (
					len1 > 0 &&
					STAT(path, &stbuf) == 0 &&
					S_ISDIR(stbuf.st_mode)
				) {
					sprintf(p2, "/%.*s", len1 - 1, elem);
					if (tet_eaccess(path, 0) == 0)
						recurse = 1;
				}
			}
		}
		else
			recurse = 1;
		if (recurse) {
			rc = copy_sf2r3(prp, head, *fip, tail, destdir,
				options, sys, nsys);
			if (rc < 0)
				errors++;
		}
	}

	/* free up storage allocated by tet_lsdir() */
	for (fip = files; *fip; fip++) {
		TRACE2(tet_Tbuf, 6, "sf2patmatch: free file name = %s",
			tet_i2x(*fip));
		free((void *) *fip);
	}
	TRACE2(tet_Tbuf, 6, "sf2patmatch: free file list = %s", tet_i2x(files));
	free((void *) files);

	return(errors ? -1 : 0);
}


/*
**	copy_sf2dotchk() - check to see if the specified path contains
**		a .. component
**
**	return 1 if it does or 0 if it doesn't
*/
static int copy_sf2dotchk(path)
char *path;
{
	static char dots[] = "..";
	char *p;
	char dirsep;
	int rc;

	while (*path) {
		for (p = path; *p; p++) {
			if (isdirsep(*p)) {
				dirsep = *p;
				*p = '\0';
				rc = strcmp(path, dots);
				*p = dirsep;
				if (rc == 0)
					return(1);
				path = p + 1;
				break;
			}
		}
		if (!*p) {
			if (strcmp(path, dots) == 0)
				return(1);
			break;
		}
	}

	return(0);
}

/*
**	insferror() - report an error in an instruction file
*/
static void insferror(s1, s2, lineno, fname)
char *s1, *s2, *fname;
int lineno;
{
	fprintf(stderr, "%s: %s ", tet_progname, s1);
	if (s2 && *s2)
		fprintf(stderr, "%s ", s2);
	fprintf(stderr, "at line %d in file %s\n", lineno, fname);
	fflush(stderr);
}

/*
**	prsfopts() - return a printable representation of a set of
**		"transfer source files" options
*/
static char *prsfopts(fval)
int fval;
{
	static struct flags flags[] = {
		{ SF_FTYPE_ASCII, "ASCII" },
		{ SF_FTYPE_BINARY, "BINARY" },
		{ SF_KEEP_MTIME, "KEEP_MTIME" },
		{ SF_UNCOND_COPY, "UNCOND_COPY" }
	};

	return(tet_f2a(fval, flags, sizeof flags / sizeof flags[0]));
}

/*
**	ispatt() - see if a string contains a shell pattern matching character
**
**	return 1 if it does or 0 if it doesn't
*/
static int ispatt(s)
char *s;
{
	while (*s) {
		switch(*s) {
		case '*':
		case '?':
		case '[':
		case ']':
			return(1);
		}
		s++;
	}

	return(0);
}

#endif /* !TET_LITE */	/* -END-LITE-CUT- */

