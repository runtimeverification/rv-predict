/*
 *      SCCS:  @(#)trace.c	1.12 (98/08/28) 
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
static char sccsid[] = "@(#)trace.c	1.12 (98/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)trace.c	1.12 98/08/28 TETware release 3.8
NAME:		trace.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	trace subsystem functions

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., January 1994
	added timestamp to trace messages

	Geoff Clare, UniSoft Ltd., August 1996
	Changes for TETWare.

	Andrew Dingwall, UniSoft Ltd., July 1997
	Added tet_tfclear().
	Added support the MT DLL version of the C runtime support library
	on Win32 systems.

	Andrew Dingwall, UniSoft Ltd., February 1998
	Use TETware-specific macros to access threads functions and
	data items.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#ifndef NOTRACE

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <fcntl.h>
#include <time.h>
#include <sys/types.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <process.h>
#else		/* -END-WIN32-CUT- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "trace.h"
#include "dtmsg.h"
#include "dtthr.h"
#include "error.h"
#include "globals.h"
#include "ltoa.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

FILE *tet_tfp = NULL;			/* trace file pointer */

/* the trace flags themselves */
TET_IMPORT int tet_Tbuf, tet_Ttcm;
int tet_Ttcc, tet_Ttrace, tet_Tscen, tet_Texec;
#ifndef TET_LITE	/* -START-LITE-CUT- */
int tet_Tio, tet_Tloop, tet_Tserv, tet_Tsyncd, tet_Ttccd, tet_Txresd;
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

/* trace flag cmd line identifiers and addresses */
struct tflags tet_tflags[] = {
	{ 't', &tet_Ttrace,	0, 0 },	/* must be first */
	{ 'b', &tet_Tbuf,	0, 0 },
	{ 'c', &tet_Ttcm,	0, 0 },
	{ 'g', &tet_Texec,	0, 0 },
	{ 'm', &tet_Ttcc,	0, 0 },
	{ 'p', &tet_Tscen,	0, 0 },
#ifndef TET_LITE	/* -START-LITE-CUT- */
	{ 'e', &tet_Tserv,	0, 0 },
	{ 'i', &tet_Tio,	0, 0 },
	{ 'l', &tet_Tloop,	0, 0 },
	{ 's', &tet_Ttccd,	0, 0 },
	{ 'x', &tet_Txresd,	0, 0 },
	{ 'y', &tet_Tsyncd,	0, 0 }
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
};
int tet_Ntflags = sizeof tet_tflags / sizeof tet_tflags[0];

/* map of trace flag cmd line system names to process types */
struct stype tet_stype[] = {
	{ 'M', PT_MTCC },
	{ 'S', PT_STCC },
	{ 'C', PT_MTCM },
	{ 'D', PT_STCM },
	{ 'X', PT_XRESD },
	{ 'Y', PT_SYNCD },
	{ 'T', PT_STAND }
};
int tet_Nstype = sizeof tet_stype / sizeof tet_stype[0];


/* static function declarations */
static int tflagset PROTOLIST((char *, int));


/*
**	tet_traceinit() - scan the argv for -T options and process them
**
**	options can take one of the forms:
**
**		-Txvalue
**		-Tx value
**		-TP,xvalue
**		-TP,x value
**
**	where x is a flag name and value is its value
**	x can be "all" to set all flags to the given value
**	more than one -T option may appear; the effect of -T options accumulate
**	trace options are normally passed to exec'd processes as well
**	if P is present, this restricts the passing of the flag value to
**	processes of type P; more than one P may be present
**
**	a sequence -Tx4 -TP,x2 will cause flag x to have value 4 in the
**	current process and value 2 in process P
**	the sequence -TP,x4 -TQ,x2 will cause flag x to have value 2 in
**	processes P and Q, unless P is the current process in which case its
**	flag x will have value 4
**
**	it is currently not possible to pass different values of the same flag
**	to different processes
**
**	disclaimer: the whole scheme is not perfect (or bug-free), but will
**	probably do what you want if you are persistent enough!
*/

TET_IMPORT void tet_traceinit(argc, argv)
register int argc;
register char **argv;
{
	register struct tflags *tp;
	register int value;
	register long sys;

	while (++argv, --argc > 0)
		if (**argv == '-' && *(*argv + 1) == 'T') {
			TRACE2(tet_Ttrace, 10, "tet_traceinit: arg = \"%s\"",
				*argv);
			value = argc > 1 ? atoi(*(argv + 1)) : 0;
			if (tflagset(*argv + 2, value) > 0)
				if (argc > 1) {
					*(argv + 1) = *argv;
					++argv;
					--argc;
				}
		}

	/* avoid exporting trace flag arguments to processes that don't
		need them */
	for (tp = tet_tflags; tp < &tet_tflags[tet_Ntflags]; tp++) {
		if (tp->tf_value <= 0) {
			tp->tf_sys = 0;
			continue;
		}
		sys = 0;
		if (
			tp->tf_vp == &tet_Ttcc ||
			tp->tf_vp == &tet_Tscen ||
			tp->tf_vp == &tet_Texec
		) {
			TF_SET(sys, PT_MTCC);
		}
		else if (tp->tf_vp == &tet_Ttcm) {
			TF_SET(sys, PT_MTCM);
			TF_SET(sys, PT_STCM);
		}
#ifndef TET_LITE	/* -START-LITE-CUT- */
		else if (tp->tf_vp == &tet_Tsyncd)
			TF_SET(sys, PT_SYNCD);
		else if (tp->tf_vp == &tet_Ttccd)
			TF_SET(sys, PT_STCC);
		else if (tp->tf_vp == &tet_Txresd)
			TF_SET(sys, PT_XRESD);
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
		else
			continue;
		tp->tf_sys &= sys;
	}

	if (tet_Ttrace > 0)
		tet_tftrace();
}

/*
**	tet_tfclear() - clear all the trace flags (except tet_Ttrace)
*/

TET_IMPORT void tet_tfclear()
{
	register struct tflags *tp;

	TRACE1(tet_Ttrace, 10, "tet_tfclear(): clear trace flags");

	for (tp = tet_tflags; tp < &tet_tflags[tet_Ntflags]; tp++) {
		if (tp->tf_vp == &tet_Ttrace)
			continue;
		*tp->tf_vp = 0;
		tp->tf_value = 0;
		tp->tf_sys = 0L;
	}
}

/*
**	tflagset() - set value and inheretance for a flag
**
**	return 0 if the flag value was part of arg
**	or 1 if value (taken from *(argv + 1)) was used
*/

static int tflagset(arg, value)
char *arg;
int value;
{
	register struct tflags *tp;
	register struct stype *sp;
	register char *p;
	register int all, rc;
	register long sys;
	char buf[2];

	buf[1] = '\0';

	all = 1;
	for (p = arg; *p; p++)
		if (*p == ',') {
			all = 0;
			break;
		}

	if (all) {
		p = arg;
		sys = ~0;
	}
	else {
		sys = 0;
		for (p = arg; *p != ','; p++)
			for (sp = tet_stype; sp < &tet_stype[tet_Nstype]; sp++)
				if (*p == sp->st_name)
					TF_SET(sys, sp->st_ptype);
		p++;
	}

	all = strncmp(p, "all", 3) ? 0 : 1;

	p += (all * 2);

	if (*(p + 1)) {
		value = atoi(p + 1);
		rc = 0;
	}
	else
		rc = 1;

	for (tp = tet_tflags; tp < &tet_tflags[tet_Ntflags]; tp++) {
		if (all || tp->tf_name == *p) {
			buf[0] = tp->tf_name;
			tp->tf_sys |= sys;
			if (value > tp->tf_value) {
				TRACE3(tet_Ttrace, 10,
					"global trace flag %s = %s",
					buf, tet_i2a(value));
				tp->tf_value = value;
			}
			if (TF_ISSET(sys, tet_myptype) && value > *tp->tf_vp) {
				TRACE3(tet_Ttrace, 10,
					"local trace flag %s = %s",
					buf, tet_i2a(value));
				*tp->tf_vp = value;
			}
		}
		if (!all && tp->tf_name == *p)
			break;
	}

	if (!all && tp >= &tet_tflags[tet_Ntflags]) {
		buf[0] = *p;
		error(0, "unknown trace flag name", buf);
	}

	return(rc);
}

/*
**	tet_tftrace() - print trace flag information
*/

void tet_tftrace()
{
	register struct tflags *tp;
	char name[2];

	TRACE1(tet_Ttrace, 10, "trace flags:");
	name[1] = '\0';
	for (tp = tet_tflags; tp < &tet_tflags[tet_Ntflags]; tp++) {
		name[0] = tp->tf_name;
		TRACE5(tet_Ttrace, 10,
			"name = '%s', lvalue = %s, gvalue = %s, sys = %s",
			name, tet_i2a(*tp->tf_vp), tet_i2a(tp->tf_value),
			tet_i2x(tp->tf_sys));
	}
}

/*
**	tet_trace() - print trace info to a file, opening it if necessary
*/

TET_IMPORT void tet_trace(s1, s2, s3, s4, s5, s6)
char *s1, *s2, *s3, *s4, *s5, *s6;
{
	register int save_errno;
	time_t now;
	register struct tm *tp;

	save_errno = errno;

	if (!tet_tfp)
		tet_tfopen();

#ifndef TET_THREADS
	(void) fprintf(tet_tfp, "%s (%ld)", tet_progname, (long) GETPID());
#else
	(void) fprintf(tet_tfp, "%s (%ld.%ld)", tet_progname, (long) GETPID(),
		(long) TET_THR_SELF());
#endif
	if (tet_Ttrace > 0) {
		now = time((time_t *) 0);
		tp = localtime(&now);
		if (tet_Ttrace > 1)
			(void) fprintf(tet_tfp, " %d:%02d:%02d",
				tp->tm_hour, tp->tm_min, tp->tm_sec);
		else
			(void) fprintf(tet_tfp, " %d:%02d",
				tp->tm_min, tp->tm_sec);
	}
	(void) fprintf(tet_tfp, ": ");
	(void) fprintf(tet_tfp, s1, s2, s3, s4, s5, s6);
	(void) putc('\n', tet_tfp);
	(void) fflush(tet_tfp);

	errno = save_errno;
}

/*
**	tet_tfopen() - open the trace file
*/

void tet_tfopen()
{
	register char *p;
	register int fd;

	if (tet_tfp)
		return;

	if ((fd = FCNTL_F_DUPFD(FILENO(stderr), 2)) < 0)
		fatal(errno, "can't dup", tet_i2a(FILENO(stderr)));

	if (tet_fappend(fd) < 0 || tet_fioclex(fd) < 0)
		fatal(0, "can't continue", (char *) 0);

	if ((tet_tfp = FDOPEN(fd, "a")) == NULL)
		fatal(errno, "fdopen failed on fd", tet_i2a(fd));

	errno = 0;
	if ((p = malloc(BUFSIZ)) == (char *) 0)
		fatal(errno, "can't allocate buffer for trace file",
			(char *) 0);
	TRACE2(tet_Tbuf, 6, "allocate trace file stdio buffer = %s",
		tet_i2x(p));

	setbuf(tet_tfp, p);
}

#else

int tet_trace_c_not_used;

#endif /* !NOTRACE */

