/*
 *      SCCS:  @(#)rescode.c	1.8 (05/11/29) 
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
static char sccsid[] = "@(#)rescode.c	1.8 (05/11/29) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)rescode.c	1.8 05/11/29 TETware release 3.8
NAME:		rescode.c
PRODUCT:	TETware
AUTHOR:		David Sawyer, UniSoft Ltd.
DATE CREATED:	August 1992

DESCRIPTION:
	open result codes file and generate table.
	
	return result codes string matching result code etc.

	This file is based on parts of apilib/dresfile.c

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, August 1993
	changed dtet/ to dtet2/ in #include

	Andrew Dingwall, UniSoft Ltd., December 1993
	added tracing to malloc() calls
	sorted out loop terminator problems in read_codes()

	Andrew Dingwall, UniSoft Ltd., August 1996
	made this file generic and moved it from xresd to here

	Andrew Dingwall, UniSoft Ltd., September 1996
	tet_getrescode() renamed as tet_getresname()
	complementary function tet_getrescode() added

	Geoff Clare, UniSoft Ltd., Sept 1996
	Moved tet_addresult() to here from xresd.

	Andrew Dingwall, UniSoft Ltd., July 1999
	added support for shared API libraries

	Geoff Clare, The Open Group, November 2005
	In tet_addresult() prioritise on Abort actions before code values.

************************************************************************/

#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <ctype.h>
#include <errno.h>
#include "dtmac.h"
#include "error.h"
#include "restab.h"
#include "dtetlib.h"
#include "tet_api.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* default result code table */
static struct restab restab_dflt[] = {
	{ "PASS",		TET_PASS,		0 },
	{ "FAIL",		TET_FAIL,		0 },
	{ "UNRESOLVED",		TET_UNRESOLVED,		0 },
	{ "NOTINUSE",		TET_NOTINUSE,		0 },
	{ "UNSUPPORTED",	TET_UNSUPPORTED,	0 },
	{ "UNTESTED",		TET_UNTESTED,		0 },
	{ "UNINITIATED",	TET_UNINITIATED,	0 },
	{ "NORESULT",		TET_NORESULT,		0 }
};

#define Nrestab_dflt	(sizeof restab_dflt / sizeof restab_dflt[0])

static char invalid_result[] = "INVALID RESULT";

/* the result code table, its length and the number of entries in it */
struct restab *tet_restab;
int tet_nrestab;
static int lrestab;

/* static function declarations */
static void badresline PROTOLIST((char *, int, char *));
static struct restab *getrtbycode PROTOLIST((int));
static struct restab *getrtbyname PROTOLIST((char *));
static char **procline PROTOLIST((char *));
static int rtaddupdate PROTOLIST((struct restab *));


/*
**	tet_getresname() - look up result code in the rescode table;
**		return name if found, otherwise a default string
**
**	if abortflag is not NULL, set *abortflag to true or false depending
**	on whether or not the corresponding action is to abort
*/

char *tet_getresname(result, abortflag)
int result;
int *abortflag;
{
	register struct restab *rtp;
	register char *name;
	register int abrt;

	if (!tet_restab && tet_initrestab() < 0) {
		name = "UNKNOWN";
		abrt = 0;
	}
	else if ((rtp = getrtbycode(result)) == (struct restab *) 0) {
		name = invalid_result;
		abrt = 0;
	}
	else {
		name = rtp->rt_name;
		abrt = rtp->rt_abrt;
	}

	if (abortflag)
		*abortflag = abrt;
	return(name);
}

/*
**	tet_getrescode() - look up result name in the rescode table
**
**	return the result code corresponding to name if found
**	or -1 if not found or an error occurs
**
**	if abortflag is not NULL, set *abortflag to true or false depending
**	on whether or not the corresponding action is to abort
*/

int tet_getrescode(name, abortflag)
char *name;
int *abortflag;
{
	register struct restab *rtp;
	register int code, abrt;

	if (
		(tet_restab || tet_initrestab() == 0) &&
		(rtp = getrtbyname(name)) != (struct restab *) 0
	) {
		code = rtp->rt_code;
		abrt = rtp->rt_abrt;
	}
	else {
		code = -1;
		abrt = 0;
	}

	if (abortflag)
		*abortflag = abrt;
	return(code);
}

/*
**	tet_readrescodes() - read result codes from the tet_code file into
**		the results code table
**
**	return 0 if successful or -1 on error
*/

int tet_readrescodes(fname)
char *fname;
{
	FILE *fp;
	char **argv;
	char buf[BUFSIZ];
	struct restab rtmp;
	register char *p, **ap;
	register int line;
	int rc = 0;

	/* install the default table first time through */
	if (!tet_restab && tet_initrestab() < 0)
		return(-1);

	/* open the result codes file */
	ASSERT(fname && *fname);
	if ((fp = fopen(fname, "r")) == NULL) {
		error(errno, "can't open result code file", fname);
		return(-1);
	}

	/* process each line in the file */
	line = 0;
	while (fgets(buf, sizeof buf, fp) != NULL) {
		line++;
		if (*(argv = procline(buf)) == (char *) 0)
			continue;
		/* establish defaults */
		rtmp.rt_code = 0;
		rtmp.rt_name = invalid_result;
		rtmp.rt_abrt = 0;
		/* process each field */
		for (ap = argv; *ap; ap++)
			switch (ap - argv) {
			case 0:
				/* result code */
				rtmp.rt_code = atoi(*ap);
				break;
			case 1:
				/* quoted result name */
				if (**ap != '"' ||
					*(p = *ap + strlen(*ap) - 1) != '"') {
						badresline("quotes missing",
							line, fname);
						break;
					}
				*p = '\0';
				if ((p = tet_strstore(++*ap)) == (char *) 0) {
					rc = -1;
					break;
				}
				rtmp.rt_name = p;
				for (p = rtmp.rt_name; *p; p++)
					if (*p == '"') {
						badresline("quotes unexpected",
							line, fname);
						break;
					}
				break;
			case 2:
				/* result action indicator */
				if (strcmp(*ap, "Continue") == 0)
					break;
				else if (strcmp(*ap, "Abort") == 0)
					rtmp.rt_abrt = 1;
				else
					badresline("invalid action field",
						line, fname);
				break;
			case 3:
				/* junk field */
				badresline("extra field(s) ignored",
					line, fname);
				break;
			}
		if (rc < 0 || (rc = rtaddupdate(&rtmp)) < 0)
			break;
	}

	(void) fclose(fp);
	return(rc);
}

/*
**	procline() - split a result codes line into fields
**		and return a pointer to a null-terminated array of
**		string field pointers
**
**	double quotes may be used to protect spaces embedded in fields
**	and will be retained in the returned strings
*/

/* number of fields on an input line -
	field 1 = result code
	field 2 = result code name
	field 3 = continue/abort
	field 4 = any trailing junk
*/
#define NFLDS	4

static char **procline(s)
char *s;
{
	static char *argv[NFLDS + 1];
	register char *p, **ap;
	register int argc, new, quote;

	/* strip comments and a trailing newline */
	for (p = s; *p; p++)
		if (*p == '\n' || *p == '#') {
			*p = '\0';
			break;
		}

	/* clear the argv array */
	for (ap = argv; ap < &argv[NFLDS]; ap++)
		*ap = (char *) 0;

	/* split the line into at most NFLDS fields */
	ap = argv;
	argc = quote = 0;
	new = 1;
	for (p = s; *p; p++) {
		if (!quote && isspace(*p)) {
			*p = '\0';
			new = 1;
			continue;
		}
		if (new && argc++ < NFLDS) {
			*ap++ = p;
			new = 0;
		}
		if (*p == '"')
			quote = !quote;
	}

	*ap = (char *) 0;

	return(argv);
}

/*
**	badresline() - complain about a bad results code line
*/

static void badresline(msg, line, file)
char *msg, *file;
int line;
{
	char buf[128];

	(void) sprintf(buf, "%s in line %d, file", msg, line);
	error(0, buf, file);
}

/*
**	rtaddupdate() - add a new entry to the restab or update an existing one
**
**	return 0 if successful or -1 on error
*/

static int rtaddupdate(rtp1)
register struct restab *rtp1;
{
	register struct restab *rtp2;

	if ((rtp2 = getrtbycode(rtp1->rt_code)) == (struct restab *) 0) {
		if (BUFCHK((char **) &tet_restab, &lrestab, (int) ((tet_nrestab + 1) * sizeof *tet_restab)) < 0)
			return(-1);
		*(tet_restab + tet_nrestab++) = *rtp1;
	}
	else {
		ASSERT(rtp2->rt_name);
		if (rtp2->rt_name != invalid_result) {
			TRACE2(tet_Tbuf, 6, "free restab name = %s",
				tet_i2x(rtp2->rt_name));
			free(rtp2->rt_name);
		}
		rtp2->rt_name = rtp1->rt_name;
		rtp2->rt_abrt = rtp1->rt_abrt;
	}

	return(0);
}

/*
**	getrtbycode() - return pointer to restab entry corresponding to
**		the specified result code
**
**	return (struct restab *) 0 if no entry for code can be found
*/

static struct restab *getrtbycode(code)
register int code;
{
	register struct restab *rtp;

	for (rtp = tet_restab; rtp < tet_restab + tet_nrestab; rtp++)
		if (rtp->rt_code == code)
			return(rtp);

	return((struct restab *) 0);
}

/*
**	getrtbyname() - return pointer to restab entry corresponding to
**		the specified result name
**
**	return (struct restab *) 0 if no entry for name can be found
*/

static struct restab *getrtbyname(name)
register char *name;
{
	register struct restab *rtp;

	for (rtp = tet_restab; rtp < tet_restab + tet_nrestab; rtp++)
		if (!strcmp(rtp->rt_name, name))
			return(rtp);

	return((struct restab *) 0);
}

/*
**	tet_initrestab() - copy the default result code values into
**		the result code table
**
**	return 0 if successful or -1 on error
*/

int tet_initrestab()
{
	register struct restab *rtp;
	struct restab rtmp;

	for (rtp = restab_dflt; rtp < restab_dflt + Nrestab_dflt; rtp++) {
		rtmp.rt_code = rtp->rt_code;
		rtmp.rt_abrt = rtp->rt_abrt;
		if ((rtmp.rt_name = tet_strstore(rtp->rt_name)) == (char *) 0 ||
			rtaddupdate(&rtmp) < 0)
				return(-1);
	}

	return(0);
}

/*
**	tet_addresult() - arbitrate between result codes
**
**	return result with highest priority
*/

TET_IMPORT int tet_addresult(lastresult, thisresult)
register int lastresult, thisresult;
{
	int lastabort = 0, thisabort = 0;

	if (lastresult < 0)
		return(thisresult);

	/* First compare abort flags.  Codes with an Abort action
	   take priority over those with no Abort action. */

	(void) tet_getresname(lastresult, &lastabort);
	(void) tet_getresname(thisresult, &thisabort);
	if (thisabort && !lastabort)
		return(thisresult);
	if (!thisabort && lastabort)
		return(lastresult);

	/* Abort flags are the same, so go by result code priority */

	switch (thisresult) {
	case TET_PASS:
		/* lowest priority */
		return(lastresult);

	case TET_FAIL:
		/* highest priority */
		return(thisresult);

	case TET_UNRESOLVED:
	case TET_UNINITIATED:
		/* high priority */
		switch (lastresult) {
		case TET_FAIL:
			return(lastresult);
		default:
			return(thisresult);
		}

	case TET_NORESULT:
		/* output by tet_result() for invalid result codes,
		   and so must supersede everything that isn't some
		   sort of definite failure */
		switch (lastresult) {
		case TET_FAIL:
		case TET_UNRESOLVED:
		case TET_UNINITIATED:
			return(lastresult);
		default:
			return(thisresult);
		}

	case TET_UNSUPPORTED:
	case TET_NOTINUSE:
	case TET_UNTESTED:
		/* low priority */
		switch (lastresult) {
		case TET_PASS:
			return(thisresult);
		default:
			return(lastresult);
		}

	default:
		/* user-supplied codes: middle priority */
		switch (lastresult) {
		case TET_PASS:
		case TET_UNSUPPORTED:
		case TET_NOTINUSE:
		case TET_UNTESTED:
			return(thisresult);
		default:
			return(lastresult);
		}
	}
}

