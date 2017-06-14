/*
 *      SCCS:  @(#)host.c	1.12 (05/06/28) 
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
static char sccsid[] = "@(#)host.c	1.12 (05/06/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)host.c	1.12 05/06/28 TETware release 3.8
NAME:		host.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	INET host address lookup function

MODIFICATIONS:
	Geoff Clare, UniSoft Ltd., August 1996
	Missing <stdio.h> (for sprintf).

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.

	Geoff Clare, The Open Group, June 2005
	Missing <string.h> (for strcmp).
 
************************************************************************/

#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <time.h>
#include <sys/types.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <winsock.h>
#else		/* -END-WIN32-CUT- */
#  include <netinet/in.h>
#  include <netdb.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "error.h"
#include "dtmsg.h"
#include "ptab.h"
#include "inetlib_in.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


#define NHCACHE		2	/* no of host cache entries - one for localhost
				   and one for the master system */

/* host cache structure */
struct hcache {
	int hc_refcnt;
	struct in_addr hc_addr;
	char hc_host[SNAMELEN];
};

static struct hcache hcache[NHCACHE];	/* the cache itself */

/*
**	tet_gethostaddr() - find the INET address of a host and return a
**		pointer thereto
**
**	return (struct in_addr *) 0 if the address cannot be found
*/

TET_IMPORT struct in_addr *tet_gethostaddr(host)
char *host;
{
	register struct hcache *cp1, *cp2;
	register struct hostent *hp;
	register int max;
	int err;

	/* see if host is already in the cache */
	for (cp1 = hcache; cp1 < &hcache[NHCACHE]; cp1++)
		if (!strcmp(cp1->hc_host, host)) {
			cp1->hc_refcnt++;
			return(&cp1->hc_addr);
		}

	/* look up the host's address in the hosts file */
	CLEAR_SOCKET_ERRNO;
	if ((hp = gethostbyname(host)) == (struct hostent *) 0) {
		err = SOCKET_ERRNO;
		error(err != ENOTTY ? err : 0,
			"can't find hosts entry for", host);
		return((struct in_addr *) 0);
	}

	/* store the entry in the least used cache slot */
	max = (int) ((unsigned) ~0 >> 1);
	for (cp2 = cp1 = hcache; cp1 < &hcache[NHCACHE]; cp1++)
		if (cp1->hc_refcnt <= 0) {
			cp2 = cp1;
			break;
		}
		else if (cp1->hc_refcnt <= max) {
			max = cp1->hc_refcnt;
			cp2 = cp1;
		}

	cp2->hc_addr = *((struct in_addr *) hp->h_addr);
	cp2->hc_refcnt = 1;
	(void) sprintf(cp2->hc_host, "%.*s",
		(int) sizeof cp2->hc_host - 1, host);

	return(&cp2->hc_addr);
}

