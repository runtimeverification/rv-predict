/*
 *	SCCS: @(#)dirtab.c	1.3 (98/03/05)
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
static char sccsid[] = "@(#)dirtab.c	1.3 (98/03/05) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)dirtab.c	1.3 98/03/05 TETware release 3.8
NAME:		dirtab.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	the scenario directive table and related functions

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., March 1998
	added support for number ranges in directive arguments

************************************************************************/

#include <string.h>
#include "dtmac.h"
#include "dirtab.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* number of entries in an array */
#define NUMENTRIES(x)	(sizeof x / sizeof x[0])


/*
**	valid enclosing directive lists
*/

/* all directives */
static int enc_all[] = {
#ifndef TET_LITE	/* -START-LITE-CUT- */
	SD_DISTRIBUTED, SD_REMOTE,
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
	SD_PARALLEL, SD_RANDOM, SD_REPEAT, SD_SEQUENTIAL, SD_TIMED_LOOP,
	SD_VARIABLE
};

/* these directives may appear within the scope of random */
static int enc_random[] = {
#ifndef TET_LITE	/* -START-LITE-CUT- */
	SD_DISTRIBUTED, SD_REMOTE,
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
	SD_SEQUENTIAL, SD_VARIABLE
};

/* these directives may appear within the scope of parallel */
static int enc_parallel[] = {
#ifndef TET_LITE	/* -START-LITE-CUT- */
	SD_DISTRIBUTED, SD_REMOTE,
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
	SD_SEQUENTIAL
};


#ifndef TET_LITE	/* -START-LITE-CUT- */
/* these directives may appear within the scope of remote and distributed */
static int enc_remdist[] = {
	SD_PARALLEL, SD_RANDOM, SD_REPEAT, SD_SEQUENTIAL, SD_TIMED_LOOP,
	SD_VARIABLE
};
#endif /* !TET_LITE */	/* -END-LITE-CUT- */


/* the directives table itself */
static struct dirtab dirtab[] = {

	{
		"include", SD_INCLUDE, 0, SDF_NEED_ATTACH, (int *) 0, 0
	},

	{	
		"parallel", SD_PARALLEL, SD_END_PARALLEL,
		SDF_OPT_ARG | SDF_NUMERIC_ARGS | SDF_OPT_ATTACH | SDF_ENDDIR,
		enc_parallel, NUMENTRIES(enc_parallel)
	},

	{
		"endparallel", SD_END_PARALLEL, SD_PARALLEL, 0, (int *) 0, 0
	},

	{
		"group", SD_PARALLEL, SD_END_PARALLEL,
		SDF_OPT_ARG | SDF_NUMERIC_ARGS | SDF_OPT_ATTACH | SDF_ENDDIR,
		enc_parallel, NUMENTRIES(enc_parallel)
	},

	{
		"endgroup", SD_END_PARALLEL, SD_PARALLEL, 0, (int *) 0, 0
	},

	{
		"repeat", SD_REPEAT, SD_END_REPEAT,
		SDF_OPT_ARG | SDF_NUMERIC_ARGS | SDF_OPT_ATTACH | SDF_ENDDIR,
		enc_all, NUMENTRIES(enc_all)
	},

	{
		"endrepeat", SD_END_REPEAT, SD_REPEAT, 0, (int *) 0, 0
	},

	{
		"random", SD_RANDOM, SD_END_RANDOM,
		SDF_OPT_ATTACH | SDF_ENDDIR,
		enc_random, NUMENTRIES(enc_random)
	},

	{
		"endrandom", SD_END_RANDOM, SD_RANDOM, 0, (int *) 0, 0
	},

	{
		"timed_loop", SD_TIMED_LOOP, SD_END_TIMED_LOOP,
		SDF_NEED_ARG | SDF_NUMERIC_ARGS | SDF_OPT_ATTACH | SDF_ENDDIR,
		enc_all, NUMENTRIES(enc_all)
	},

	{
		"endtimed_loop", SD_END_TIMED_LOOP, SD_TIMED_LOOP, 0,
		(int *) 0, 0
	},

#ifndef TET_LITE	/* -START-LITE-CUT- */

	{
		"remote", SD_REMOTE, SD_END_REMOTE,
		SDF_NEED_MARG | SDF_NRANGE_ARGS | SDF_OPT_ATTACH | SDF_ENDDIR,
		enc_remdist, NUMENTRIES(enc_remdist)
	},

	{
		"endremote", SD_END_REMOTE, SD_REMOTE, 0, (int *) 0, 0
	},

	{
		"distributed", SD_DISTRIBUTED, SD_END_DISTRIBUTED,
		SDF_NEED_MARG | SDF_NRANGE_ARGS | SDF_OPT_ATTACH | SDF_ENDDIR,
		enc_remdist, NUMENTRIES(enc_remdist)
	},

	{
		"enddistributed", SD_END_DISTRIBUTED, SD_DISTRIBUTED, 0,
		(int *) 0, 0
	},

#endif /* !TET_LITE */	/* -END-LITE-CUT- */

#if 0
	{
		"variable", SD_VARIABLE, SD_END_VARIABLE,
		SDF_NEED_MARG | SDF_VARFMT_ARGS | SDF_OPT_ATTACH | SDF_ENDDIR,
		enc_all, NUMENTRIES(enc_all)
	},

	{
		"endvariable", SD_END_VARIABLE, SD_VARIABLE, 0, (int *) 0, 0
	},
#endif

	{
		(char *) 0, SD_SEQUENTIAL, 0, 0, enc_all, NUMENTRIES(enc_all)
	}
};

/*
**	getdirbyname(), getdirbyvalue() - directive table lookup functions
*/

struct dirtab *getdirbyname(name)
char *name;
{
	register struct dirtab *dp;

	for (dp = dirtab; dp < &dirtab[NUMENTRIES(dirtab)]; dp++)
		if (dp->dt_name && !strcmp(name, dp->dt_name))
			return(dp);

	TRACE2(tet_Tscen, 6, "getdirbyname(%s) failed", name);
	return((struct dirtab *) 0);
}

struct dirtab *getdirbyvalue(directive)
int directive;
{
	register struct dirtab *dp;

	for (dp = dirtab; dp < &dirtab[NUMENTRIES(dirtab)]; dp++)
		if (directive == dp->dt_directive)
			return(dp);

	TRACE2(tet_Tscen, 6, "getdirbyvalue(%s) failed", tet_i2a(directive));
	return((struct dirtab *) 0);
}

