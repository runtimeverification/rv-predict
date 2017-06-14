/*
 *      SCCS:  @(#)etab.c	1.7 (99/09/02) 
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
static char sccsid[] = "@(#)etab.c	1.7 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)etab.c	1.7 99/09/02 TETware release 3.8
NAME:		etab.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	executed process table administration functions

MODIFICATIONS:

************************************************************************/


#include <stdlib.h>
#include <signal.h>
#include <errno.h>
#include <sys/types.h>
#include <time.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "etab.h"
#include "error.h"
#include "llist.h"
#include "bstring.h"
#include "tccd.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif

static struct etab *etab;		/* ptr to head of exec table */

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/*
**	etalloc() - allocate an exec table element and return a pointer thereto
**
**	return (struct etab *) 0 on error
*/

struct etab *etalloc()
{
	register struct etab *ep;

	errno = 0;

	if ((ep = (struct etab *) malloc(sizeof *ep)) == (struct etab *) 0) {
		error(errno, "can't allocate etab element", (char *) 0);
		return((struct etab *) 0);
	}
	TRACE2(tet_Tbuf, 6, "allocate etab = %s", tet_i2x(ep));
	bzero((char *) ep, sizeof *ep);

	return(ep);
}

/*
**	etfree() - free an exec table element
*/

void etfree(ep)
struct etab *ep;
{
	TRACE2(tet_Tbuf, 6, "free etab = %s", tet_i2x(ep));

	if (ep)
		free((char *) ep);
}

/*
**	etadd() - insert an element in the etab list
*/

void etadd(ep)
struct etab *ep;
{
	tet_listinsert((struct llist **) &etab, (struct llist *) ep);
}

/*
**	etrm() - remove an element from the etab list
*/

void etrm(ep)
struct etab *ep;
{
	tet_listremove((struct llist **) &etab, (struct llist *) ep);
}

/*
**	etfind() - search the exec table for entry matching pid and
**		return a pointer thereto
**
**	return (struct etab *) 0 if not found
*/

struct etab *etfind(pid)
register int pid;
{
	register struct etab *ep;

	for (ep = etab; ep; ep = ep->et_next)
		if (ep->et_pid == pid)
			break;

	return(ep);
}

/*
**	etdead() - etab processing when a process logs off or dies
*/

void etdead(pp)
struct ptab *pp;
{
	register struct etab *ep;
	register int done;

	do {
		done = 1;
		for (ep = etab; ep; ep = ep->et_next)
			if (ep->et_ptab == pp) {
				if (ep->et_state == ES_RUNNING)
					(void) KILL(ep->et_pid, SIGHUP);
				etrm(ep);
				etfree(ep);
				done = 0;
				break;
			}
	} while (!done);
}

