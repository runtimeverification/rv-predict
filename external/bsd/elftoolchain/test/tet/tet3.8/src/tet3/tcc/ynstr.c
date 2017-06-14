/*
 *	SCCS: @(#)ynstr.c	1.5 (02/08/09)
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
static char sccsid[] = "@(#)ynstr.c	1.5 (02/08/09) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)ynstr.c	1.5 02/08/09 TETware release 3.8
NAME:		ynstr.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	functions associated with tcc's -y and -n command-line options

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <time.h>
#include "dtmac.h"
#include "error.h"
#include "ltoa.h"
#include "dtetlib.h"
#include "tcc.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* the lists of -y and -n strings from the command line */
static char **ylcmd, **nlcmd;
static int Nylcmd, Nnlcmd;

/* the lists of -y and -n strings from the old journal file */
static char **ylojf, **nlojf;
static int Nylojf, Nnlojf;


/* static function declarations */
static void addstr PROTOLIST((char *, char ***, int *));
static char *findstr PROTOLIST((char *, char **, int));
static int instring PROTOLIST((char *, char *));
static void ynstr2 PROTOLIST((char *, char ***, int *));


/*
**	yesstr() - store a -y argument from the tcc command line
*/

void yesstr(s, flag)
char *s;
int flag;
{
	register char ***spp;
	register int *nspp;

	/* determine which list to use */
	switch (flag) {
	case YN_CMDLINE:
		spp = &ylcmd;
		nspp = &Nylcmd;
		break;
	case YN_OJFILE:
		spp = &ylojf;
		nspp = &Nylojf;
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected flag value:", tet_i2a(flag));
		/* NOTREACHED */
		return;
	}

	ynstr2(s, spp, nspp);
}

/*
**	nostr() - store a -n argument from the tcc command line
*/

void nostr(s, flag)
char *s;
int flag;
{
	register char ***spp;
	register int *nspp;

	/* determine which list to use */
	switch (flag) {
	case YN_CMDLINE:
		spp = &nlcmd;
		nspp = &Nnlcmd;
		break;
	case YN_OJFILE:
		spp = &nlojf;
		nspp = &Nnlojf;
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected flag value:", tet_i2a(flag));
		/* NOTREACHED */
		return;
	}

	ynstr2(s, spp, nspp);
}

/*
**	ynstr2() - common subroutine for yesstr() and nostr()
*/

static void ynstr2(s, spp, nspp)
char *s, ***spp;
int *nspp;
{
	if (s && *s && findstr(s, *spp, *nspp) == (char *) 0)
		addstr(s, spp, nspp);
}

/*
**	okstr() - see if it OK to process this test case
**
**	return 1 if it is or 0 if it isn't
*/

int okstr(s, flag)
char *s;
int flag;
{
	register char **sp;
	register char **yeslist, **nolist;
	register int Nyeslist, Nnolist;

	/* determine which lists to use */
	switch (flag) {
	case YN_CMDLINE:
		yeslist = ylcmd;
		Nyeslist = Nylcmd;
		nolist = nlcmd;
		Nnolist = Nnlcmd;
		break;
	case YN_OJFILE:
		yeslist = ylojf;
		Nyeslist = Nylojf;
		nolist = nlojf;
		Nnolist = Nnlojf;
		break;
	default:
		/* this "can't happen" */
		fatal(0, "unexpected flag value:", tet_i2a(flag));
		/* NOTREACHED */
		return(0);
	}

	/*
	** if no strings have been specified and the string contains one
	** of the no strings, don't process the test case
	*/
	if (Nnolist) {
		for (sp = nolist + Nnolist - 1; sp >= nolist; sp--)
			if (instring(s, *sp))
				break;
		if (sp >= nolist)
			return(0);
	}

	/*
	** if yes strings have been specified, only process the
	** test case if the string contains one of the yes strings
	*/
	if (Nyeslist) {
		for (sp = yeslist + Nyeslist - 1; sp >= yeslist; sp--)
			if (instring(s, *sp))
				break;
		return(sp >= yeslist ? 1 : 0);
	}

	/*
	** here if -y was not specified, and either -n was not specified
	** or string did not match any of the no strings
	*/
	return(1);
}

/*
**	instring() - see if s2 appears anywhere within s1
**
**	return 1 if it does or 0 if it doesn't
*/

static int instring(s1, s2)
char *s1, *s2;
{
	register char *p1, *p2, *p3;

	for (p1 = s1; *p1; p1++) {
		for (p2 = s2, p3 = p1; *p2 && *p3; p2++, p3++)
			if (*p2 != *p3)
				break;
		if (!*p2)
			return(1);
	}

	return(0);
}

/*
**	findstr() - find a string in a string list and return a
**		pointer thereto
**
**	return (char *) 0 if the string is not in the list
*/

static char *findstr(s, sp, nsp)
char *s;
register char **sp;
register int nsp;
{
	while (--nsp >= 0)
		if (!strcmp(s, *sp))
			return(*sp);
		else
			sp++;

	return((char *) 0);
}

/*
**	addstr() - add a string to the end of a string list
*/

static void addstr(s, spp, nspp)
char *s, ***spp;
int *nspp;
{
	int len = *nspp * sizeof **spp;

	RBUFCHK((char **) spp, &len, (*nspp + 1) * (int) sizeof **spp);
	*(*spp + (*nspp)++) = rstrstore(s);
}

