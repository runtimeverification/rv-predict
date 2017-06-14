/*
 *      SCCS:  @(#)targs.c	1.8 (96/11/04) 
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
static char sccsid[] = "@(#)targs.c	1.8 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)targs.c	1.8 96/11/04 TETware release 3.8
NAME:		targs.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to generate a list of trace option arguments

MODIFICATIONS:
	Geoff Clare, UniSoft Ltd., August 1996
	Forward trace flags from MTCC to MTCM via TCCD.

************************************************************************/

#ifndef NOTRACE

#include <stdio.h>
#include <string.h>
#include <errno.h>
#include "dtmac.h"
#include "trace.h"
#include "dtmsg.h"
#include "ltoa.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* global variables in trace.c */
extern struct tflags tet_tflags[];
extern int tet_Ntflags;
extern struct stype tet_stype[];
extern int tet_Nstype;


/*
**	tet_traceargs() - construct an exec argv for process ptype and return
**		a pointer thereto
**
**	if argv is non-zero,
**		newargv consists of argv[0], zero or more -T arguments,
**			then argv[1] ... argv[n]
**	otherwise newargv consists only of the -T arguments
*/

char **tet_traceargs(ptype, argv)
int ptype;
char **argv;
{
	register char *p, **ap;
	register int argc, needlen, tfcount;
	register long sys;
	register struct tflags *tp;
	register struct stype *sp;
	int fullargs;
	static char **newargv;
	static int nalen;

	/* count the argv arguments */
	if (argv) {
		for (ap = argv; *ap; ap++)
			;
		argc = ap - argv;
	}
	else
		argc = 0;

	if (tet_Ttrace > 0)
		tet_tftrace();

	TRACE2(tet_Ttrace, 4, "trace arguments for %s:", tet_ptptype(ptype));

	/* add the number of trace flags that we will generate */
	tfcount = 0;
	for (tp = tet_tflags; tp < &tet_tflags[tet_Ntflags]; tp++)
		if (tp->tf_sys && tp->tf_value > 0)
			tfcount++;

	/* make sure that the receiving area is big enough */
	argc += tfcount + 1;
	needlen = (argc * sizeof *newargv) + (tfcount * (LNUMSZ + tet_Nstype + 4));
	if (BUFCHK((char **) &newargv, &nalen, needlen) < 0)
		return((char **) 0);

	/* copy over the first argument */
	ap = newargv;
	p = (char *) (ap + argc);
	if (argv) {
		TRACE2(tet_Ttrace, 4, "first arg = \"%s\"", *argv);
		*ap++ = *argv++;
	}

	/* construct a -T arg for each non-zero trace flag that is to
		be passed to the execed process -
		process that may themselves exec other processes receive
		flags to be passed on to them as well */
	for (tp = tet_tflags; tp < &tet_tflags[tet_Ntflags]; tp++) {
		if (!tp->tf_sys || tp->tf_value <= 0)
			continue;
		sys = 0;
		TF_SET(sys, ptype);
		switch (ptype) {
		case PT_MTCC:
			TF_SET(sys, PT_STCC);
			TF_SET(sys, PT_MTCM);
			TF_SET(sys, PT_SYNCD);
			TF_SET(sys, PT_XRESD);
			/* fall through */
		case PT_STCC:
			TF_SET(sys, PT_STCM);
			TF_SET(sys, PT_MTCM);
			fullargs = 1;
			break;
		case PT_MTCM:
		case PT_STCM:
			TF_SET(sys, PT_STCC);
			TF_SET(sys, PT_MTCM);
			fullargs = 1;
			break;
		default:
			fullargs = 0;
			break;
		}
		if ((sys & tp->tf_sys) == 0)
			continue;
		if (tp->tf_sys == ~0)
			fullargs = 0;
		*ap++ = p;
		*p++ = '-';
		*p++ = 'T';
		if (fullargs) {
			for (sp = tet_stype; sp < &tet_stype[tet_Nstype]; sp++)
				if (TF_ISSET(tp->tf_sys, sp->st_ptype))
					*p++ = sp->st_name;
			*p++ = ',';
		}
		(void) sprintf(p, "%c%d", tp->tf_name, tp->tf_value);
		TRACE2(tet_Ttrace, 4, "trace arg = \"%s\"", *(ap - 1));
		p += strlen(p) + 1;
	}

	/* copy over the rest of the original arguments */
	if (argv)
		while (*argv) {
			TRACE2(tet_Ttrace, 4, "other arg = \"%s\"", *argv);
			*ap++ = *argv++;
		}

	*ap = (char *) 0;

	return(newargv);
}

#else

int tet_targs_c_not_empty;

#endif /* !NOTRACE */

