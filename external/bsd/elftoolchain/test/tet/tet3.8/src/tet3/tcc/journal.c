/*
 *	SCCS: @(#)journal.c	1.16 (05/06/27)
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

#ifndef lint
static char sccsid[] = "@(#)journal.c	1.16 (05/06/27) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)journal.c	1.16 05/06/27 TETware release 3.8
NAME:		journal.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	functions which open and write to journal files

MODIFICATIONS:
	Geoff Clare, UniSoft Ltd., August 1996
	Missing <string.h>.

	Andrew Dingwall, UniSoft Ltd., March 1997
	added jnl_uname() function to write a Uname line to the journal

	Andrew Dingwall, UniSoft Ltd., July 1997
	Added jnl_itrace() function to provide support for
	the -I command-line option.
	Added support the MT DLL version of the C runtime support library
	on Win32 systems.

	Andrew Dingwall, UniSoft Ltd., March 1998
	Don't attempt to print the 3rd journal field when an over long
	2nd field causes the journal line to be truncated.
	Flush stderr after printing the itrace - this is needed on
	Win32 systems when stderr is not a terminal.

	Andrew Dingwall, UniSoft Ltd., March 1998
	fixed bug whereby an existing journal file could be overwritten
	when the -i option was used

	Andrew Dingwall, UniSoft Ltd., July 1999
	implemented -j - and -j |shell-command

	Andrew Dingwall, UniSoft Ltd., March 2001
	Added support for recognition of Windows 98 and Windows Me.

	Geoff Clare, The Open Group, June 2005
	Added support for full timestamps.

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <time.h>
#include <fcntl.h>
#include <errno.h>
#include <time.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <windows.h>
#  include <io.h>
#else		/* -END-WIN32-CUT- */
#  include <sys/wait.h>
#  include <signal.h>
#  include <pwd.h>
#  include <unistd.h>
#  include <sys/utsname.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "error.h"
#include "globals.h"
#include "ltoa.h"
#include "bstring.h"
#include "config.h"
#include "dtetlib.h"
#include "scentab.h"
#include "proctab.h"
#include "tcc.h"
#include "tet_jrnl.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* TETware version number */
#ifdef TET_LITE		/* -LITE-CUT-LINE- */
char tcc_version[] = "3.8-lite";
#else			/* -START-LITE-CUT- */
char tcc_version[] = "3.8";
#endif /* TET_LITE */	/* -END-LITE-CUT- */

/* the scenario reference string */
char tcc_scenref_fmt[] = "scenario ref %ld-%d";

/* no of chars in a date, a time, a full timestamp, or a scenario reference */
#define DATESZ		8	/* YYYYMMDD */
#define TIMESZ		8	/* HH:MM:SS */
#define FULLTIMESZ	(sizeof "YYYY-MM-DDTHH:MM:SS.sss")
#define REFSZ		(sizeof tcc_scenref_fmt + (LNUMSZ * 2))

/* permissions for the journal file */
#define MODEANY \
	((mode_t) (S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH))

/* name and handle for the journal file */
static FILE *jfp;
static char *jfname;

/* shell command when journal file is a pipeline */
static char *jfpcmd;

/* the type of the journal file */
static int jftype;
#define JF_FILE		1
#define JF_STDOUT	2
#define JF_PIPE		3

/* static function declarations */
static void jnl_bc_end PROTOLIST((struct proctab *, int, char *));
static void jnl_bc_start PROTOLIST((struct proctab *, int, char *));
static void jnl_cfg2 PROTOLIST((char *, FILE *, char *));
static void jnl_cfg_start PROTOLIST((char *));
static void jnl_cons2 PROTOLIST((struct proctab *, struct proctab *));
static char *jnl_date PROTOLIST((time_t));
static void jnl_ic_se PROTOLIST((struct proctab *, int, char *));
static void jnl_itrace PROTOLIST((int, char *, char *));
static char *jnl_mode PROTOLIST((int));
static void jnl_mwrite PROTOLIST((int, char *, char *, FILE *, char *));
static char *jnl_scenref PROTOLIST((struct proctab *));
static void jnl_tcc_m2 PROTOLIST((char *, FILE *, char *));
#ifndef TET_LITE	/* -START-LITE-CUT- */
static void jnl_rd_start PROTOLIST((struct proctab *, int, char *));
#endif /* !TET_LITE */	/* -END-LITE-CUT- */


/*
**	jnl_init() - determine the name of the journal file and
**		open it
**
**	jopt is the -j command-line option
**	cwd is tcc's initial working directory
*/

void jnl_init(jopt, cwd)
char *jopt, *cwd;
{
	static char fmt[] = "<pipeline to \"%.*s\">";
	char fname[MAXPATH];
	char *p;
	int fd;

	TRACE3(tet_Ttcc, 2, "jnl_init(): jopt = \"%s\", cwd = \"%s\"",
		jopt ? jopt : "", cwd);

	/*
	** determine the name and type of the journal file -
	** options are:
	**	nothing			"journal" in the results directory
	**	-			the standard output
	**	| shell-command		a pipeline
	**	anything else		the named journal file
	*/
	if (jopt && *jopt) {
		if (*jopt == '-' && *(jopt + 1) == '\0') {
			(void) sprintf(fname, "<standard output>");
			jftype = JF_STDOUT;
		}
		else if (*jopt == '|') {
			for (p = jopt + 1; *p; p++)
				if (!isspace(*p))
					break;
			if (!*p) {
				(void) fprintf(stderr, "%s: the shell command specified by -j is empty\n", tet_progname);
				tcc_exit(2);
			}
			(void) sprintf(fname, fmt,
				(int) sizeof fname - (int) sizeof fmt, p);
			jfpcmd = rstrstore(p);
			jftype = JF_PIPE;
		}
		else {
			fullpath(cwd, jopt, fname, sizeof fname, 0);
			jftype = JF_FILE;
		}
	}
	else {
		fullpath(resdirname(), "journal", fname, sizeof fname, 0);
		jftype = JF_FILE;
	}

	/*
	** open a stream to the journal file
	**
	** for a shell command:
	**	open a pipeline to the command
	**
	** for a plain file:
	**	open the journal file,
	**	making sure that an existing file is not overwritten
	*/
	switch (jftype) {
	case JF_STDOUT:
		jfp = stdout;
		break;
	case JF_PIPE:
		ASSERT(jfpcmd && *jfpcmd);
		errno = 0;
		if ((jfp = tcc_popen(jfpcmd, "w")) == (FILE *) 0)
			fatal(errno, "tcc_popen() failed on", jfpcmd);
		if (tet_fioclex(FILENO(jfp)) < 0)
			tcc_exit(1);
		break;
	case JF_FILE:
		if ((fd = OPEN(fname, O_WRONLY | O_CREAT | O_EXCL, MODEANY)) < 0)
			fatal(errno, "can't open", fname);
		if ((jfp = FDOPEN(fd, "w")) == (FILE *) 0)
			fatal(errno, "fdopen() failed on", fname);
		if (tet_fioclex(FILENO(jfp)) < 0)
			tcc_exit(1);
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected jftype", tet_i2a(jftype));
		/* NOTREACHED */
	}

	/* remember the name of the journal file and return */
	jfname = rstrstore(fname);
	TRACE2(tet_Ttcc, 1, "journal file = %s", jfname);
}

/*
**	jnl_close() - close the journal file
*/
void jnl_close()
{
	int rc, status;

	if (!jfp)
		return;

	switch (jftype) {
	case JF_STDOUT:
		/* nothing to do */
		break;
	case JF_PIPE:
		ASSERT(jfpcmd);
		errno = 0;
		if ((rc = tcc_pclose(jfp)) < 0)
			error(errno,
				"tcc_pclose() failed on journal pipeline:",
				jfpcmd);
		else
#ifndef _WIN32						/* -WIN32-CUT-LINE- */
		if (WIFEXITED(rc)) {
			status = WEXITSTATUS(rc);
#else							/* -START-WIN32-CUT- */
			status = rc;
#endif							/* -END-WIN32-CUT- */
			if (status != 0) {
				(void) printf("%s: journal pipeline command \"%s\" terminated with status of %d\n",
					tet_progname, jfpcmd, status);
				(void) fflush(stdout);
			}
#ifndef _WIN32						/* -WIN32-CUT-LINE- */
		}
		else if (WIFSIGNALED(rc)) {
			(void) printf("%s: journal pipeline command \"%s\" was terminated by signal %d\n",
				tet_progname, jfpcmd, WTERMSIG(rc));
			(void) fflush(stdout);
		}
		else if (WIFSTOPPED(rc)) {
			(void) printf("%s: journal pipeline command \"%s\" was stopped by signal %d\n",
				tet_progname, jfpcmd, WSTOPSIG(rc));
			(void) fflush(stdout);
		}
		else
			error(0, "pclose() of journal pipeline returned bad value",
				tet_i2x(rc));
#endif							/* -WIN32-CUT-LINE- */
		break;
	case JF_FILE:
		ASSERT(jfname);
		errno = 0;
		if (fclose(jfp) == EOF) 
			error(errno, "fclose() failed on", jfname);
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected jftype", tet_i2a(jftype));
		/* NOTREACHED */
	}

	jfp = (FILE *) 0;
	jftype = 0;

	if (jfname) {
		TRACE2(tet_Tbuf, 6, "free jfname = %s", tet_i2x(jfname));
		free(jfname);
		jfname = (char *) 0;
	}
	
	if (jfpcmd) {
		TRACE2(tet_Tbuf, 6, "free jfpcmd = %s", tet_i2x(jfpcmd));
		free(jfpcmd);
		jfpcmd = (char *) 0;
	}
}

/*
**	jnl_usable() - see if the journal is usable
**
**	return 1 if it is or 0 if it isn't
*/

int jnl_usable()
{
	struct STAT_ST stbuf;

	if (jfp && !ferror(jfp) && FSTAT(FILENO(jfp), &stbuf) == 0)
		return(1);
	else
		return(0);
}

/*
**	jnl_jfname(), jnl_jfp() - return the name and handle for the
**		journal file
*/

char *jnl_jfname()
{
	return(jfname);
}

FILE *jnl_jfp()
{
	return(jfp);
}

/*
**	jnl_tmpfile() - open a temporary journal file for use within the
**		current processing context
**
**	the file is opened for update
**
**	return 0 if successful or -1 on error
*/

int jnl_tmpfile(prp)
struct proctab *prp;
{
	FILE *fp;
	char *fname;

	/*
	** open a temporary journal file with a unique name ;
	** the file is opened for update because after it is written to
	** it gets rewound and read back in jnl_consolidate()
	*/

	if ((fname = jnl_tfname(resdirname(), "jnl")) == (char *) 0)
		return(-1);

	if ((fp = fopen(fname, "w+")) == (FILE *) 0) {
		error(errno, "can't open", fname);
		return(-1);
	}
	else if (tet_fioclex(FILENO(fp)) < 0) {
		(void) fclose(fp);
		return(-1);
	}

	/* all OK so fill in the proctab and return */
	ASSERT(fp);
	prp->pr_jfp = fp;
	prp->pr_jfname = rstrstore(fname);

	TRACE4(tet_Ttcc, 6, "jnl_tmpfile(%s): pr_jfp = %s, pr_jfname = %s",
		tet_i2x(prp), tet_i2x(prp->pr_jfp), prp->pr_jfname);

	return(0);
}

/*
**	jnl_tfname() - generate a temporary file name and return a pointer
**		thereto
**
**	return (char *) 0 on error
*/

char *jnl_tfname(dir, prefix)
char *dir, *prefix;
{
	static char fmt[] = "%.*s/%.3s%05.5d.%.3s";
	static char salt[] = "AAA";
	static char fname[MAXPATH];
	register char *p;
	register int try, trymax = (sizeof salt - 1) * 26;
	int fd;

	for (try = 0; try < trymax; try++) {
		(void) sprintf(fname, fmt, (int) sizeof fname - 14,
			dir, prefix, tet_mypid, salt);
		errno = 0;
		if ((fd = OPEN(fname, O_WRONLY|O_CREAT|O_EXCL, MODEANY)) >= 0)
			(void) CLOSE(fd);
		if (fd >= 0 || errno != EEXIST)
			return(fname);
		for (p = &salt[sizeof salt - 2]; p >= salt; p--)
			if (++*p > 'Z')
				*p = 'A';
			else
				break;
	}

	/*
	** here if we have exhausted all the available names
	** (could this ever happen ?)
	*/
	error(0, "out of tmp journal file names", (char *) 0);
	return((char *) 0);
}

/*
**	jnl_tcc_*() functions - write a line of the desired type to
**		the journal file
*/

void jnl_tcc_start(argc, argv)
int argc;
char **argv;
{
	char s1[DATESZ + TIMESZ + sizeof tcc_version + 3];
	char s2[TET_JNL_LEN];
	time_t now = time((time_t *) 0);
#ifdef _WIN32	/* -START-WIN32-CUT- */
	char username[64];
	int lusername = sizeof username;
#else		/* -END-WIN32-CUT- */
	struct passwd *pw;
	uid_t uid;
#endif		/* -WIN32-CUT-LINE- */
	char *user;
	register char *p1;
	register char *p2 = s2;
	register char *p3;
	int sp;

	/* generate the first part */
	(void) sprintf(s1, "%s %s %s", tcc_version,
		jnl_time(now), jnl_date(now));

	/* determine the user id and name */
#ifdef _WIN32	/* -START-WIN32-CUT- */
	if (GetUserName(username, &lusername) != TRUE)
		user = "unknown";
	else
		user = username;
#else		/* -END-WIN32-CUT- */
	uid = getuid();
	if ((user = getlogin()) == (char *) 0) {
		if ((pw = getpwuid(uid)) == (struct passwd *) 0 ||
			pw->pw_name == (char *) 0)
				user = "unknown";
		else
			user = pw->pw_name;
	}
#endif		/* -WIN32-CUT-LINE- */

	/* generate the second part */
#ifdef _WIN32	/* -START-WIN32-CUT- */
	(void) sprintf(p2, "User: %s TCC Start, Command line:", user);
#else		/* -END-WIN32-CUT- */
	(void) sprintf(p2, "User: %s (%d) TCC Start, Command line:",
		user, (int) uid);
#endif		/* -WIN32-CUT-LINE- */
	p2 += strlen(p2);

	/*
	** append the command line, quoting arguments which contain
	** embedded spaces, escaping \ and " characters;
	** the conventions used here are interpreted when this line
	** is parsed during RERUN/RESUME processing
	*/
	while (argc-- > 0 && p2 < &s2[sizeof s2 - 2]) {
		*p2++ = ' ';
		p1 = *argv++;
		sp = 0;
		for (p3 = p1; *p3; p3++)
			if (isspace(*p3)) {
				sp = 1;
				break;
			}
		if (sp && p2 < &s2[sizeof s2 - 1])
			*p2++ = '"';
		while (*p1 && p2 < &s2[sizeof s2 - 2]) {
			switch (*p1) {
			case '"':
				*p2++ = '\\';
				*p2++ = *p1;
				break;
			case '\\':
				*p2++ = *p1;
				*p2++ = *p1;
				break;
			default:
				if (isspace(*p1))
					*p2++ = ' ';
				else if (isgraph(*p1))
					*p2++ = *p1;
				else
					*p2++ = '?';
				break;
			}
			p1++;
		}
		if (sp && p2 < &s2[sizeof s2 - 1])
			*p2++ = '"';
	}
	*p2 = '\0';

	jnl_write(TET_JNL_TCC_START, s1, s2, jfp, jfname);
}

void jnl_uname()
{
#ifdef _WIN32	/* -START-WIN32-CUT- */
	OSVERSIONINFO osinf;
	char *sysname, *nodename;
	char compname[MAX_COMPUTERNAME_LENGTH + 1];
	DWORD Lcompname = sizeof compname;
	SYSTEM_INFO sysinf;
	static char fmt[] = "WIN32_platform_type_%d";
	char buf[sizeof fmt + LNUMSZ];
	static char unknown[] = "UNKNOWN";
#  define SYSNAMESZ	(sizeof buf)
	char s1[SYSNAMESZ + MAX_COMPUTERNAME_LENGTH + (LNUMSZ * 3) + 5];
#else		/* -END-WIN32-CUT- */
	struct utsname uts;
	char s1[sizeof uts.sysname + sizeof uts.nodename +
		sizeof uts.release + sizeof uts.version +
		sizeof uts.machine + 5];
#endif		/* -WIN32-CUT-LINE- */

#ifdef TET_LITE	/* -LITE-CUT-LINE- */
	static char s2[] = "System Information";
#else	/* -START-LITE-CUT- */
	static char s2[] = "Local System Information";
#endif /* TET_LITE */	/* -END-LITE-CUT- */

#ifdef _WIN32	/* -START-WIN32-CUT- */
	bzero((char *) &osinf, sizeof osinf);
	osinf.dwOSVersionInfoSize = sizeof osinf;
	if (GetVersionEx(&osinf) != TRUE) {
		error(0, "GetVersionEx() failed, error =",
			tet_i2a(GetLastError()));
		sysname = unknown;
	}
	else
		switch(osinf.dwPlatformId) {
		case VER_PLATFORM_WIN32s:
			sysname = "Windows_3.1";
			break;
		case VER_PLATFORM_WIN32_WINDOWS:
			switch (osinf.dwMinorVersion) {
			case 0:
				sysname = "Windows_95";
				break;
			case 10:
				sysname = "Windows_98";
				break;
			case 90:
				sysname = "Windows_Me";
				break;
			default:
				sysname = "Windows";
				break;
			}
			break;
		case VER_PLATFORM_WIN32_NT:
			sysname = "Windows_NT";
			break;
#  ifdef VER_PLATFORM_WIN32_CE
		case VER_PLATFORM_WIN32_CE:
			sysname = "Windows_CE";
			break;
#  endif
		default:
			(void) sprintf(buf, fmt, osinf.dwPlatformId);
			sysname = buf;
			break;
		}

	if (GetComputerName(compname, &Lcompname) != TRUE) {
		error(0, "GetComputerName() failed, error =",
			tet_i2a(GetLastError()));
		nodename = unknown;
	}
	else
		nodename = compname;

	bzero((char *) &sysinf, sizeof sysinf);
	GetSystemInfo(&sysinf);

	(void) sprintf(s1, "%.*s %.*s %d %d %d",
		SYSNAMESZ, sysname,
		MAX_COMPUTERNAME_LENGTH, nodename,
		osinf.dwMajorVersion,
		osinf.dwMinorVersion,
		sysinf.dwProcessorType);

#else		/* -END-WIN32-CUT- */

	if (uname(&uts) < 0) {
		error(errno, "unable to obtain uname() information",
			(char *) 0);
		return;
	}

	(void) sprintf(s1, "%.*s %.*s %.*s %.*s %.*s",
		(int) sizeof uts.sysname, uts.sysname,
		(int) sizeof uts.nodename, uts.nodename,
		(int) sizeof uts.release, uts.release,
		(int) sizeof uts.version, uts.version,
		(int) sizeof uts.machine, uts.machine);

#endif		/* -WIN32-CUT-LINE- */

	jnl_write(TET_JNL_UNAME, s1, s2, jfp, jfname);
}

void jnl_tc_start(prp)
register struct proctab *prp;
{
	char s1[LNUMSZ + MAXPATH + FULLTIMESZ + 3];
	char fulltimebuf[FULLTIMESZ];
	static char *s2;
	static int s2len;
	static char s2p1fmt[] = "TC Start, %s";
	static char s2p2fmt[] = ", ICs: {%s}";
	register char *p;
	register int needlen;

	if (tet_curtime(fulltimebuf, sizeof fulltimebuf,
		        getmcflag("TET_FULL_TIMESTAMPS", TCC_EXEC)) == -1)
		(void) strcpy(fulltimebuf, "TIME_ERR");

	(void) sprintf(s1, "%d %.*s %s", prp->pr_activity,
		MAXPATH, prp->pr_scen->sc_tcname, fulltimebuf);

	needlen = sizeof s2p1fmt + REFSZ;
	if (prp->pr_exiclist)
		needlen += sizeof s2p2fmt + strlen(prp->pr_exiclist);

	RBUFCHK(&s2, &s2len, needlen);
	p = s2;

	(void) sprintf(p, s2p1fmt, jnl_scenref(prp));
	if (prp->pr_exiclist) {
		p += strlen(p);
		(void) sprintf(p, s2p2fmt, prp->pr_exiclist);
	}

	jnl_write(TET_JNL_INVOKE_TC, s1, s2, prp->pr_jfp, prp->pr_jfname);
	jnl_itrace(TET_JNL_INVOKE_TC, s1, s2);
}

void jnl_mcfg_start(fname, mode)
char *fname;
int mode;
{
	char s1[MAXPATH + LONUMSZ + 2];

	(void) sprintf(s1, "%.*s %s", MAXPATH, fname, jnl_mode(mode));
	jnl_cfg_start(s1);
}


#ifndef TET_LITE	/* -START-LITE-CUT- */

void jnl_scfg_start(sysid, mode)
int sysid, mode;
{
	static char fmt[] = "remote_%03d %s";
	char s1[sizeof fmt + LNUMSZ + LONUMSZ];

	(void) sprintf(s1, fmt, sysid, jnl_mode(mode));
	jnl_cfg_start(s1);
}

#endif /* !TET_LITE */	/* -END-LITE-CUT- */


static void jnl_cfg_start(s1)
char *s1;
{
	static char s2[] = "Config Start";

	jnl_write(TET_JNL_CFG_START, s1, s2, jfp, jfname);
}

void jnl_cfg(s2)
char *s2;
{
	jnl_cfg2(s2, jfp, jfname);
}

static void jnl_cfg2(s2, fp, fname)
FILE *fp;
char *fname, *s2;
{
	jnl_write(TET_JNL_CFG_VALUE, (char *) 0, s2, fp, fname);
}

void jnl_cfg_end()
{
	static char s2[] = "Config End";

	jnl_write(TET_JNL_CFG_END, (char *) 0, s2, jfp, jfname);
}

void jnl_tcc_msg(s2)
char *s2;
{
	jnl_tcc_m2(s2, jfp, jfname);
}

void jnl_tcc_prpmsg(prp, s2)
char *s2;
struct proctab *prp;
{
	jnl_tcc_m2(s2, prp->pr_jfp, prp->pr_jfname);
}

static void jnl_tcc_m2(s2, fp, fname)
char *s2, *fname;
FILE *fp;
{
	jnl_mwrite(TET_JNL_TC_MESSAGE, (char *) 0, s2, fp, fname);
}

void jnl_sceninfo(prp, s2)
struct proctab *prp;
char *s2;
{
	jnl_write(TET_JNL_SCEN_OUT, (char *) 0, s2,
		prp->pr_jfp, prp->pr_jfname);
}

void jnl_tc_end(prp)
struct proctab *prp;
{
	char s1[(LNUMSZ * 2) + FULLTIMESZ + 3];
	char fulltimebuf[FULLTIMESZ];
	static char fmt[] = "TC End, %s";
	char s2[sizeof fmt + REFSZ];

	if (tet_curtime(fulltimebuf, sizeof fulltimebuf,
		        getmcflag("TET_FULL_TIMESTAMPS", TCC_EXEC)) == -1)
		(void) strcpy(fulltimebuf, "TIME_ERR");

	(void) sprintf(s1, "%d %d %s", prp->pr_activity, prp->pr_jnlstatus,
		fulltimebuf);
	(void) sprintf(s2, fmt, jnl_scenref(prp));
	jnl_write(TET_JNL_TC_END, s1, s2, prp->pr_jfp, prp->pr_jfname);
	jnl_itrace(TET_JNL_TC_END, s1, s2);
}

void jnl_user_abort(prp)
struct proctab *prp;
{
	char fulltimebuf[FULLTIMESZ];
	static char s2[] = "User Abort";

	if (tet_curtime(fulltimebuf, sizeof fulltimebuf,
		        getmcflag("TET_FULL_TIMESTAMPS", prp->pr_currmode)) == -1)
		(void) strcpy(fulltimebuf, "TIME_ERR");

	jnl_write(TET_USER_ABORT, fulltimebuf, s2, prp->pr_jfp, prp->pr_jfname);
}

void jnl_captured(prp, s2)
struct proctab *prp;
char *s2;
{
	char *s1 = tet_i2a(prp->pr_activity);

	jnl_mwrite(TET_JNL_CAPTURED_OUTPUT, s1, s2,
		prp->pr_jfp, prp->pr_jfname);
}

void jnl_build_start(prp)
struct proctab *prp;
{
	static char fmt[] = "Build Start, %s";
	char s2[sizeof fmt + REFSZ];

	(void) sprintf(s2, fmt, jnl_scenref(prp));
	jnl_bc_start(prp, TET_JNL_BUILD_START, s2);
}

void jnl_clean_start(prp)
struct proctab *prp;
{
	static char fmt[] = "Clean Start, %s";
	char s2[sizeof fmt + REFSZ];

	(void) sprintf(s2, fmt, jnl_scenref(prp));
	jnl_bc_start(prp, TET_JNL_CLEAN_START, s2);
}

static void jnl_bc_start(prp, id, s2)
struct proctab *prp;
int id;
char *s2;
{
	char s1[LNUMSZ + MAXPATH + FULLTIMESZ + 3];
	char fulltimebuf[FULLTIMESZ];

	if (tet_curtime(fulltimebuf, sizeof fulltimebuf,
		        getmcflag("TET_FULL_TIMESTAMPS", prp->pr_currmode)) == -1)
		(void) strcpy(fulltimebuf, "TIME_ERR");

	(void) sprintf(s1, "%d %.*s %s", prp->pr_activity, MAXPATH,
		prp->pr_scen->sc_tcname, fulltimebuf);

	jnl_write(id, s1, s2, prp->pr_jfp, prp->pr_jfname);
	jnl_itrace(id, s1, s2);
}

void jnl_build_end(prp)
struct proctab *prp;
{
	static char fmt[] = "Build End, %s";
	char s2[sizeof fmt + REFSZ];

	(void) sprintf(s2, fmt, jnl_scenref(prp));
	jnl_bc_end(prp, TET_JNL_BUILD_END, s2);
}

void jnl_clean_end(prp)
struct proctab *prp;
{
	static char fmt[] = "Clean End, %s";
	char s2[sizeof fmt + REFSZ];

	(void) sprintf(s2, fmt, jnl_scenref(prp));
	jnl_bc_end(prp, TET_JNL_CLEAN_END, s2);
}

static void jnl_bc_end(prp, id, s2)
struct proctab *prp;
int id;
char *s2;
{
	char s1[(LNUMSZ * 2) + FULLTIMESZ + 3];
	char fulltimebuf[FULLTIMESZ];

	if (tet_curtime(fulltimebuf, sizeof fulltimebuf,
		        getmcflag("TET_FULL_TIMESTAMPS", prp->pr_currmode)) == -1)
		(void) strcpy(fulltimebuf, "TIME_ERR");

	(void) sprintf(s1, "%d %d %s", prp->pr_activity, prp->pr_jnlstatus,
		fulltimebuf);

	jnl_write(id, s1, s2, prp->pr_jfp, prp->pr_jfname);
	jnl_itrace(id, s1, s2);
}

void jnl_par_start(prp)
struct proctab *prp;
{
	static char fmt[] = "Parallel Start, %s";
	char *s1 = tet_i2a(prp->pr_scen->sc_count);
	char s2[sizeof fmt + REFSZ];

	(void) sprintf(s2, fmt, jnl_scenref(prp));
	jnl_write(TET_JNL_PRL_START, s1, s2, prp->pr_jfp, prp->pr_jfname);
}

void jnl_par_end(prp)
struct proctab *prp;
{
	static char fmt[] = "Parallel End, %s";
	char s2[sizeof fmt + REFSZ];

	(void) sprintf(s2, fmt, jnl_scenref(prp));
	jnl_write(TET_JNL_PRL_END, (char *) 0, s2,
		prp->pr_jfp, prp->pr_jfname);
}

void jnl_seq_start(prp)
struct proctab *prp;
{
	static char implied[] = "Implied ";
	static char fmt[] = "%sSequential Start, %s";
	char s2[sizeof fmt + sizeof implied + REFSZ];

	(void) sprintf(s2, fmt,
		prp->pr_scen->sc_flags & SCF_IMPLIED ? implied : "",
		jnl_scenref(prp));
	jnl_write(TET_JNL_SEQ_START, (char *) 0, s2,
		prp->pr_jfp, prp->pr_jfname);
}

void jnl_seq_end(prp)
struct proctab *prp;
{
	static char implied[] = "Implied ";
	static char fmt[] = "%sSequential End, %s";
	char s2[sizeof fmt + sizeof implied + REFSZ];

	(void) sprintf(s2, fmt,
		prp->pr_scen->sc_flags & SCF_IMPLIED ? implied : "",
		jnl_scenref(prp));
	jnl_write(TET_JNL_SEQ_END, (char *) 0, s2,
		prp->pr_jfp, prp->pr_jfname);
}

void jnl_rpt_start(prp)
struct proctab *prp;
{
	static char fmt[] = "Repeat Start, %s";
	char *s1 = tet_i2a(prp->pr_scen->sc_count);
	char s2[sizeof fmt + REFSZ];

	(void) sprintf(s2, fmt, jnl_scenref(prp));
	jnl_write(TET_JNL_RPT_START, s1, s2, prp->pr_jfp, prp->pr_jfname);
}

void jnl_rpt_end(prp)
struct proctab *prp;
{
	static char fmt[] = "Repeat End, %s";
	char s2[sizeof fmt + REFSZ];

	(void) sprintf(s2, fmt, jnl_scenref(prp));
	jnl_write(TET_JNL_RPT_END, (char *) 0, s2,
		prp->pr_jfp, prp->pr_jfname);
}

void jnl_tloop_start(prp)
struct proctab *prp;
{
	static char fmt[] = "Timed Loop Start, %s";
	char *s1 = tet_l2a(prp->pr_scen->sc_seconds);
	char s2[sizeof fmt + REFSZ];

	(void) sprintf(s2, fmt, jnl_scenref(prp));
	jnl_write(TET_JNL_TLOOP_START, s1, s2, prp->pr_jfp, prp->pr_jfname);
}

void jnl_tloop_end(prp)
struct proctab *prp;
{
	static char fmt[] = "Timed Loop End, %s";
	char s2[sizeof fmt + REFSZ];

	(void) sprintf(s2, fmt, jnl_scenref(prp));
	jnl_write(TET_JNL_TLOOP_END, (char *) 0, s2,
		prp->pr_jfp, prp->pr_jfname);
}

void jnl_rnd_start(prp)
struct proctab *prp;
{
	static char fmt[] = "Random Start, %s";
	char s2[sizeof fmt + REFSZ];

	(void) sprintf(s2, fmt, jnl_scenref(prp));
	jnl_write(TET_JNL_RND_START, (char *) 0, s2,
		prp->pr_jfp, prp->pr_jfname);
}

void jnl_rnd_end(prp)
struct proctab *prp;
{
	static char fmt[] = "Random End, %s";
	char s2[sizeof fmt + REFSZ];

	(void) sprintf(s2, fmt, jnl_scenref(prp));
	jnl_write(TET_JNL_RND_END, (char *) 0, s2,
		prp->pr_jfp, prp->pr_jfname);
}


#ifndef TET_LITE	/* -START-LITE-CUT- */

void jnl_rmt_start(prp)
struct proctab *prp;
{
	static char fmt[] = "Remote Start, %s";
	char s2[sizeof fmt + REFSZ];

	(void) sprintf(s2, fmt, jnl_scenref(prp));
	jnl_rd_start(prp, TET_JNL_RMT_START, s2);
}

void jnl_rmt_end(prp)
struct proctab *prp;
{
	static char fmt[] = "Remote End, %s";
	char s2[sizeof fmt + REFSZ];

	(void) sprintf(s2, fmt, jnl_scenref(prp));
	jnl_write(TET_JNL_RMT_END, (char *) 0, s2,
		prp->pr_jfp, prp->pr_jfname);
}

void jnl_dist_start(prp)
struct proctab *prp;
{
	static char fmt[] = "Distributed Start, %s";
	char s2[sizeof fmt + REFSZ];

	(void) sprintf(s2, fmt, jnl_scenref(prp));
	jnl_rd_start(prp, TET_JNL_DIST_START, s2);
}

void jnl_dist_end(prp)
struct proctab *prp;
{
	static char fmt[] = "Distributed End, %s";
	char s2[sizeof fmt + REFSZ];

	(void) sprintf(s2, fmt, jnl_scenref(prp));
	jnl_write(TET_JNL_DIST_END, (char *) 0, s2,
		prp->pr_jfp, prp->pr_jfname);
}

static void jnl_rd_start(prp, id, s2)
struct proctab *prp;
int id;
char *s2;
{
	static char *s1;
	static int s1len;
	register int len, needlen;
	register int *ip;
	register char *p;
	register struct scentab *ep = prp->pr_scen;

	needlen = 1;
	for (ip = ep->sc_sys; ip < ep->sc_sys + ep->sc_nsys; ip++) {
		if (ip > ep->sc_sys)
			needlen++;
		len = strlen(tet_i2a(*ip));
		needlen += TET_MAX(len, 3);
	}

	RBUFCHK(&s1, &s1len, needlen);

	p = s1;
	for (ip = ep->sc_sys; ip < ep->sc_sys + ep->sc_nsys; ip++) {
		if (ip > ep->sc_sys)
			*p++ = ',';
		(void) sprintf(p, "%03d", *ip);
		p += strlen(p);
	}

	jnl_write(id, s1, s2, prp->pr_jfp, prp->pr_jfname);
}

#endif /* !TET_LITE */	/* -END-LITE-CUT- */


void jnl_var_start(prp)
struct proctab *prp;
{
	static char fmt[] = "Variable Start, %s";
	char s2[sizeof fmt + REFSZ];
	register struct scentab *ep = prp->pr_scen;
	register char **vp;

	(void) sprintf(s2, fmt, jnl_scenref(prp));
	jnl_write(TET_JNL_VAR_START, (char *) 0, s2,
		prp->pr_jfp, prp->pr_jfname);
	for (vp = ep->sc_vars; vp < ep->sc_vars + ep->sc_nvars; vp++)
		jnl_cfg2(*vp, prp->pr_jfp, prp->pr_jfname);
}

void jnl_var_end(prp)
struct proctab *prp;
{
	static char fmt[] = "Variable End, %s";
	char s2[sizeof fmt + REFSZ];

	(void) sprintf(s2, fmt, jnl_scenref(prp));
	jnl_write(TET_JNL_VAR_END, (char *) 0, s2,
		prp->pr_jfp, prp->pr_jfname);
}

void jnl_tcc_end()
{
	char *s1 = jnl_time(time((time_t *) 0));
	static char s2[] = "TCC End";

	jnl_write(TET_JNL_TCC_END, s1, s2, jfp, jfname);
}

/*
**	the following functions are used by tcc to write messages
**	to the journal on behalf of non-API conforming test cases
*/

void jnl_tcm_start(prp)
struct proctab *prp;
{
	char s1[LNUMSZ + sizeof tcc_version + 4];
	static char s2[] = "TCM Start (auto-generated by TCC)";

	(void) sprintf(s1, "%d %s 1", prp->pr_activity, tcc_version);
	jnl_write(TET_JNL_TCM_START, s1, s2, prp->pr_jfp, prp->pr_jfname);
}

void jnl_ic_start(prp)
struct proctab *prp;
{
	static char s2[] = "IC Start (auto-generated by TCC)";

	jnl_ic_se(prp, TET_JNL_IC_START, s2);
}

void jnl_ic_end(prp)
struct proctab *prp;
{
	static char s2[] = "IC End (auto-generated by TCC)";

	jnl_ic_se(prp, TET_JNL_IC_END, s2);
}

static void jnl_ic_se(prp, id, s2)
struct proctab *prp;
int id;
char *s2;
{
	char s1[LNUMSZ + FULLTIMESZ + 6];
	char fulltimebuf[FULLTIMESZ];

	if (tet_curtime(fulltimebuf, sizeof fulltimebuf,
		        getmcflag("TET_FULL_TIMESTAMPS", prp->pr_currmode)) == -1)
		(void) strcpy(fulltimebuf, "TIME_ERR");

	(void) sprintf(s1, "%d 1 1 %s",
		prp->pr_activity, fulltimebuf);
	jnl_write(id, s1, s2, prp->pr_jfp, prp->pr_jfname);
}

void jnl_tp_start(prp)
struct proctab *prp;
{
	char s1[LNUMSZ + FULLTIMESZ + 4];
	char fulltimebuf[FULLTIMESZ];
	static char s2[] = "TP Start (auto-generated by TCC)";

	if (tet_curtime(fulltimebuf, sizeof fulltimebuf,
		        getmcflag("TET_FULL_TIMESTAMPS", prp->pr_currmode)) == -1)
		(void) strcpy(fulltimebuf, "TIME_ERR");

	(void) sprintf(s1, "%d 1 %s",
		prp->pr_activity, fulltimebuf);
	jnl_write(TET_JNL_TP_START, s1, s2, prp->pr_jfp, prp->pr_jfname);
}

void jnl_tp_result(prp, tpno, result)
struct proctab *prp;
int tpno, result;
{
	char s1[(LNUMSZ * 3) + FULLTIMESZ + 4];
	char fulltimebuf[FULLTIMESZ];
	static char *s2;
	static int s2len;
	char *s2p1;
	static char s2p2[] = "(auto-generated by TCC)";
	int needlen;

	if (tet_curtime(fulltimebuf, sizeof fulltimebuf,
		        getmcflag("TET_FULL_TIMESTAMPS", prp->pr_currmode)) == -1)
		(void) strcpy(fulltimebuf, "TIME_ERR");

	(void) sprintf(s1, "%d %d %d %s", prp->pr_activity, tpno, result,
		fulltimebuf);

	s2p1 = tet_getresname(result, (int *) 0);
	needlen = strlen(s2p1) + sizeof s2p2 + 1;
	RBUFCHK(&s2, &s2len, needlen);
	(void) sprintf(s2, "%s %s", s2p1, s2p2);

	jnl_write(TET_JNL_TP_RESULT, s1, s2, prp->pr_jfp, prp->pr_jfname);
}

/*
**	jnl_write() - write a line to a journal, checking for errors
**
**	note that this function can be called recursively if the line is
**	too long or an error occurs when the journal is being written
*/

void jnl_write(id, s1, s2, fp, fname)
int id;
char *s1, *s2, *fname;
FILE *fp;
{
	static char nullstr[] = "";
	char *s0 = tet_i2a(id);
	int len0, len1, len2, s2max, errsave, wrerror;
	char msg[TET_JNL_LEN];
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	struct sigaction sa, oldsa;
	sigset_t mask, oldmask;
	int oldsa_valid = 0, oldmask_valid = 0;
	static int inprogress;
#endif		/* -WIN32-CUT-LINE- */

	if (!s1)
		s1 = nullstr;
	if (!s2)
		s2 = nullstr;

	TRACE4(tet_Ttcc, 1, "JOURNAL: %s|%s|%s", s0, s1, s2);

	/* don't try to write the line if we already know it will fail */
	if (ferror(fp))
		return;

	len0 = strlen(s0);
	len1 = strlen(s1);
	len2 = strlen(s2);

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	/*
	** if we are writing to a pipeline, ignore SIGPIPE for the duration
	** of the write so that if the process dies we get an EPIPE error
	** on the stream
	*/
	if (inprogress++ == 0 && fp == jfp) {
		switch (jftype) {
		case JF_PIPE:
		case JF_STDOUT:
			sa.sa_handler = SIG_IGN;
			sa.sa_flags = 0;
			(void) sigemptyset(&sa.sa_mask);
			if (sigaction(SIGPIPE, &sa, &oldsa) < 0) {
				error(errno,
					"sigaction(SIGPIPE, SIG_IGN) failed",
					(char *) 0);
				break;
			}
			oldsa_valid = 1;
			(void) sigemptyset(&mask);
			(void) sigaddset(&mask, SIGPIPE);
			if (sigprocmask(SIG_UNBLOCK, &mask, &oldmask) < 0) {
				error(errno, "sigprocmask(SIG_UNBLOCK, SIGPIPE) failed",
					(char *) 0);
				break;
			}
			oldmask_valid = 1;
			break;
		}
	}
#endif		/* -WIN32-CUT-LINE- */

	/*
	** write the line and check for errors -
	** if an error occurs on a temporary journal file, 
	** the error message appears in the main journal;
	** if an error occurs on the main journal, jnl_usable() returns
	** false and so the error message appears on stderr 
	** (see tcc_error() for details)
	*/
	s2max = TET_JNL_LEN - len0 - len1 - 2;
	if (
		fprintf(fp, "%s|%.*s|%.*s\n", s0, TET_JNL_LEN - len0 - 2,
			s1, TET_MAX(s2max, 0), s2) < 0 ||
		fflush(fp) < 0
	) {
		errsave = errno;
		wrerror = 1;
	}
	else {
		errsave = 0;
		wrerror = 0;
	}

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	/* restore the previous SIGPIPE disposition if we changed it above */
	if (--inprogress == 0 && oldsa_valid) {
		if (oldmask_valid)
			(void) sigprocmask(SIG_SETMASK, &oldmask, (sigset_t *) 0);
		(void) sigaction(SIGPIPE, &oldsa, (struct sigaction *) 0);
	}
#endif		/* -WIN32-CUT-LINE- */

	if (wrerror) {
		error(errsave, "write error on", fname);
		s2max = (int) sizeof msg - len0 - len1 - 3;
		(void) sprintf(msg, "%s|%.*s|%.*s", s0,
			(int) sizeof msg - len0 - 3, s1,
			TET_MAX(s2max, 0), s2);
		fatal(0, "the journal line that cannot be written is:", msg);
	}

	/* see if the line was truncated */
	if (len0 + len1 + len2 + 2 > TET_JNL_LEN) {
		if (jnl_usable())
			error(0, "the previous journal line has been truncated",
				(char *) 0);
		(void) fprintf(stderr, "%s: a journal line has been truncated - the complete line is:\n%s|%s|%s\n",
			tet_progname, s0, s1, s2);
		(void) fflush(stderr);
	}
}

/*
**	jnl_mwrite() - write a line to a journal
**
**	if the line is too long, split it at convenient places and
**	write out each part as a separate line
*/

static void jnl_mwrite(id, s1, s2, fp, fname)
int id;
char *s1, *fname;
register char *s2;
FILE *fp;
{
	int len1, space;
	char msg[TET_JNL_LEN];
	register char *p;
	register int n;

	/* see if the line is too long -
	** (we assume that only the third field will cause overflow)
	** if it is, split the third field (on a word boundary if possible)
	** and write out as many lines as are required to accomodate the
	** message;
	** each line consists of the first and second fields, followed
	** by the next part of the third field
	*/
	if (s2) {
		len1 = s1 ? strlen(s1) : 0;
		space = TET_JNL_LEN - strlen(tet_i2a(id)) - len1 - 2;
		while ((int) strlen(s2) > space) {
			for (p = s2 + space; p > s2; p--)
				if (isspace(*p))
					break;
			while (p > s2 && isspace(*(p - 1)))
				p--;
			n = (p > s2) ? p - s2 : space;
			(void) sprintf(msg, "%.*s", n, s2);
			jnl_write(id, s1, msg, fp, fname);
			s2 += n;
			while (isspace(*s2))
				s2++;
		}
	}

	/* here to write out the whole line if it was short enough,
	** or the tail end of a line that had to be split
	*/
	jnl_write(id, s1, s2, fp, fname);
}

/*
**	jnl_itrace() - print interactive trace information to stderr
**		when required by the -I command-line option
*/

static void jnl_itrace(id, s1, s2)
int id;
char *s1, *s2;
{
	if (tcc_Iflag) {
		(void) fprintf(stderr, "tcc:%d|%s|%s\n", id, s1, s2);
		(void) fflush(stderr);
	}
}

/*
**	jnl_date() - return a pointer to a printable representation of the
**		date in YYYYMMDD format
*/

static char *jnl_date(now)
time_t now;
{
	struct tm *tp = localtime(&now);
	static char buf[DATESZ + 1];

	(void) sprintf(buf, "%4.4d%2.2d%2.2d",
		tp->tm_year + 1900, tp->tm_mon + 1, tp->tm_mday);

	return(buf);
}

/*
**	jnl_time() - return a pointer to a printable representation of the
**		time in HH:MM:SS format
*/

char *jnl_time(now)
time_t now;
{
	struct tm *tp = localtime(&now);
	static char buf[TIMESZ + 1];

	(void) sprintf(buf, "%2.2d:%2.2d:%2.2d",
		tp->tm_hour, tp->tm_min, tp->tm_sec);

	return(buf);
}

/*
**	jnl_mode() - return a pointer to a printable representation of
**		the current processing mode
**
**	mode should be a configuration mode value
*/

static char *jnl_mode(mode)
int mode;
{
	switch (mode) {
	case CONF_BUILD:
	case CONF_EXEC:
	case CONF_CLEAN:
	case CONF_DIST:
		return(tet_i2a(TC_CONF_MODE(mode)));
	default:
		/* this "can't happen" */
		fatal(0, "unknown config mode", tet_i2a(mode));
		/* NOTREACHED */
		return("");
	}
}

/*
**	jnl_consolidate() - consolidate temporary journal files owned
**		by child proctabs into the journal file at this level
*/

void jnl_consolidate(prp)
register struct proctab *prp;
{
	register struct proctab *child;
#ifndef NOTRACE
	static char null[] = "NULL";
#endif

	TRACE6(tet_Ttcc, 6, "jnl_consolidate(%s): pr_jfp = %s, jfp = %s, pr_jfname = %s, jfname = %s",
		tet_i2x(prp), tet_i2x(prp->pr_jfp), tet_i2x(jfp),
		prp->pr_jfname ? prp->pr_jfname : null, jfname ? jfname : null);

	/* for each child proctab:
	**	if there is a journal file associated with it,
	**	copy the file's contents into the journal file
	**	at this level and unlink the child's journal file;
	**	replace the child journal file details with those on
	**	this level
	*/
	for (child = prp->pr_child; child; child = child->pr_lforw)
		if (child->pr_jfp && child->pr_jfp != prp->pr_jfp)
			jnl_cons2(prp, child);
}

static void jnl_cons2(prp, child)
register struct proctab *prp, *child;
{
	char buf[TET_JNL_LEN + 2];
	static char werrmsg[] = "write error on";

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	struct sigaction sa, oldsa;
	sigset_t mask, oldmask;
	int oldsa_valid = 0, oldmask_valid = 0;
#endif		/* -WIN32-CUT-LINE- */

	TRACE4(tet_Ttcc, 6, "consolidate journal for child proctab %s, pr_jfp = %s, pr_jfname = %s",
		tet_i2x(child), tet_i2x(child->pr_jfp),
		child->pr_jfname ? child->pr_jfname : "<NULL>");

	ASSERT(prp->pr_jfp);
	ASSERT(child->pr_jfp);
	ASSERT(prp->pr_jfp != child->pr_jfp);

	/* rewind the child journal */
	rewind(child->pr_jfp);

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	/*
	** if the current journal is at the top level and we are writing the
	** journal to a pipeline, ignore and unblock SIGPIPE so that we can
	** pick up a proken pipe error via EPIPE
	*/
	if (prp->pr_jfp == jfp) {
		switch (jftype) {
		case JF_PIPE:
		case JF_STDOUT:
			sa.sa_handler = SIG_IGN;
			sa.sa_flags = 0;
			(void) sigemptyset(&sa.sa_mask);
			if (sigaction(SIGPIPE, &sa, &oldsa) < 0) {
				error(errno, "sigaction(SIGPIPE, SIG_IGN) failed",
					(char *) 0);
				break;
			}
			oldsa_valid = 1;
			(void) sigemptyset(&mask);
			(void) sigaddset(&mask, SIGPIPE);
			if (sigprocmask(SIG_UNBLOCK, &mask, &oldmask) < 0) {
				error(errno, "sigprocmask(SIG_UNBLOCK, SIGPIPE) failed",
					(char *) 0);
				break;
			}
			oldmask_valid = 1;
			break;
		}
	}
#endif		/* -WIN32-CUT-LINE- */

	/*
	** read lines from the child journal and write them to the
	** journal at this level -
	** since the whole point of running tcc is to generate a journal,
	** we treat a write error as fatal
	*/
	while (fgets(buf, sizeof buf, child->pr_jfp) != (char *) 0)
		if (fputs(buf, prp->pr_jfp) < 0) {
			error(errno, werrmsg, prp->pr_jfname);
			if (prp->pr_jfp == jfp)
				tcc_exit(1);
			else
				break;
		}
	if (!ferror(prp->pr_jfp) && fflush(prp->pr_jfp) < 0) {
		error(errno, werrmsg, prp->pr_jfname);
		if (prp->pr_jfp == jfp)
			tcc_exit(1);
	}

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
	/* if we modified the disposition of SIGPIPE above, restore it here */
	if (oldsa_valid) {
		if (oldmask_valid)
			(void) sigprocmask(SIG_SETMASK, &oldmask,
				(sigset_t *) 0);
		(void) sigaction(SIGPIPE, &oldsa,
			(struct sigaction *) 0);
	}
#endif		/* -WIN32-CUT-LINE- */

	/*
	** close the child journal and unlink it;
	** update the journal details in the child proctab to point to
	** the journal details in the parent proctab
	*/
	(void) fclose(child->pr_jfp);
	child->pr_jfp = prp->pr_jfp;
	if (UNLINK(child->pr_jfname) < 0)
		error(errno, "can't unlink", child->pr_jfname);
	ASSERT(child->pr_jfname != prp->pr_jfname);
	TRACE2(tet_Tbuf, 6, "free tmp jnl file name %s",
		tet_i2x(child->pr_jfname));
	free(child->pr_jfname);
	child->pr_jfname = prp->pr_jfname;
}

/*
**	jnl_scenref() - generate a scenario reference string
*/

static char *jnl_scenref(prp)
struct proctab *prp;
{
	static char scenref[REFSZ];

	(void) sprintf(scenref, tcc_scenref_fmt,
		prp->pr_scen->sc_ref,
		prp->pr_sys ? *prp->pr_sys : 0);

	return(scenref);
}

