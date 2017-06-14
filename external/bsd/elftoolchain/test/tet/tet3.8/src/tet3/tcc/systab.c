/*
 *	SCCS: @(#)systab.c	1.3 (00/04/03)
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
static char sccsid[] = "@(#)systab.c	1.3 (00/04/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)systab.c	1.3 00/04/03 TETware release 3.8
NAME:		systab.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	system table administration functions

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., March 2000
	Corrected a problem in ist2() which would sometimes miss out
	system IDs.

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <time.h>
#include <errno.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "bstring.h"
#include "error.h"
#include "llist.h"
#include "config.h"
#include "scentab.h"
#include "dirtab.h"
#include "systab.h"
#include "tcc.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

static struct systab *systab;		/* head of the system table */

/* static function declarations */
static void syadd PROTOLIST((struct systab *));
static struct systab *syalloc PROTOLIST((void));
#if 0
static void syfree PROTOLIST((struct systab *));
static void syrm PROTOLIST((struct systab *));
#endif
#ifndef TET_LITE	/* -START-LITE-CUT- */
static void ist2 PROTOLIST((struct scentab *, int *, int));
#endif /* !TET_LITE */	/* -END-LITE-CUT- */


/*
**	initsystab() - allocate a tcc system table element for each
**		system mentioned in the scenario tree
*/

void initsystab()
{
#ifdef TET_LITE	/* -LITE-CUT-LINE- */
	register struct systab *sp;
#else	/* -START-LITE-CUT- */
	int zero = 0;
#endif /* TET_LITE */	/* -END-LITE-CUT- */

	TRACE1(tet_Ttcc, 2, "initsystab(): set up a systab for each system mentioned in the chosen scenario");

#ifdef TET_LITE	/* -LITE-CUT-LINE- */

	sp = syalloc();
	sp->sy_sysid = 0;
	syadd(sp);

#else	/* -START-LITE-CUT- */

	ist2(sctree, &zero, 1);

#endif /* TET_LITE */	/* -END-LITE-CUT- */

	TRACE1(tet_Ttcc, 2, "initsystab() RETURN");
}


#ifndef TET_LITE	/* -START-LITE-CUT- */

/*
**	ist2() - extend the initsystab() processing for a particular
**		level in the scenario tree
*/

static void ist2(ep, sys, nsys)
register struct scentab *ep;
int *sys, nsys;
{
	register int *ip;
	register struct systab *sp;
	static int *oldsys;

	/*
	** traverse the tree on this level, examining test case elements
	** and descending subtrees
	*/
	for (; ep; ep = ep->sc_forw) {
		ASSERT(ep->sc_magic == SC_MAGIC);
		switch (ep->sc_type) {
		case SC_DIRECTIVE:
			switch (ep->sc_directive) {
			case SD_REMOTE:
			case SD_DISTRIBUTED:
				ist2(ep->sc_child, ep->sc_sys, ep->sc_nsys);
				break;
			}
			/* fall through */
		case SC_SCENARIO:
			ist2(ep->sc_child, sys, nsys);
			break;
		case SC_TESTCASE:
			if (sys == oldsys)
				break;
			for (ip = sys; ip < sys + nsys; ip++)
				if (syfind(*ip) == (struct systab *) 0) {
					sp = syalloc();
					sp->sy_sysid = *ip;
					syadd(sp);
				}
			oldsys = sys;
			break;
		}
	}
}

#endif /* !TET_LITE */	/* -END-LITE-CUT- */


/*
**	syalloc(), syfree() - functions to allocate and free a
**		tcc system table element
*/

static struct systab *syalloc()
{
	register struct systab *sp;

	errno = 0;
	if ((sp = (struct systab *) malloc(sizeof *sp)) == (struct systab *) 0)
		fatal(errno, "can't allocate system table element",
			(char *) 0);

	TRACE2(tet_Tbuf, 6, "allocate systab element = %s", tet_i2x(sp));

	bzero((char *) sp, sizeof *sp);
	sp->sy_magic = SY_MAGIC;
	sp->sy_sysid = -1;
	sp->sy_activity = -1;
#ifndef TET_LITE	/* -START-LITE-CUT- */
	sp->sy_currcfmode = -1;
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

	return(sp);
}

#if 0	/* this function not used anywhere */
static void syfree(sp)
struct systab *sp;
{
	TRACE2(tet_Tbuf, 6, "free systab element = %s", tet_i2x(sp));

	if (sp) {
		ASSERT(sp->sy_magic == SY_MAGIC);
		bzero((char *) sp, sizeof *sp);
		free((char *) sp);
	}
}
#endif

/*
**	syadd() - add a systab element to the tcc system table
*/

static void syadd(sp)
struct systab *sp;
{
	tet_listinsert((struct llist **) &systab, (struct llist *) sp);
}

/*
**	syrm() - remove a systam element from the tcc system table
*/

#if 0	/* this function not used anywhere */
static void syrm(sp)
struct systab *sp;
{
	tet_listremove((struct llist **) &systab, (struct llist *) sp);
}
#endif

/*
**	syfind() - find the systab entry for the named system
**		and return a pointer thereto
**
**	return (struct systab *) 0 if not found
*/

struct systab *syfind(sysid)
int sysid;
{
	register struct systab *sp;

	for (sp = systab; sp; sp = sp->sy_next) {
		ASSERT(sp->sy_magic == SY_MAGIC);
		if (sp->sy_sysid == sysid)
			break;
	}

	return(sp);
}

/*
**	symax() - return the highest sysid in the systab
*/

int symax()
{
	register struct systab *sp;
	register int max = -1;

	for (sp = systab; sp; sp = sp->sy_next) {
		ASSERT(sp->sy_magic == SY_MAGIC);
		if (sp->sy_sysid > max)
			max = sp->sy_sysid;
	}

	return(max);
}

