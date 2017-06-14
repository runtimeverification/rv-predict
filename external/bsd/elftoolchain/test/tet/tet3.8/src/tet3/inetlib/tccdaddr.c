/*
 *      SCCS:  @(#)tccdaddr.c	1.12 (02/01/18) 
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
static char sccsid[] = "@(#)tccdaddr.c	1.12 (02/01/18) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tccdaddr.c	1.12 02/01/18 TETware release 3.8
NAME:		tccdaddr.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to determine tccd INET address and port number

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., October 1996
	Windows NT port

	Andrew Dingwall, UniSoft Ltd., December 1996
	remove casts from inet_addr() call - for n-bit portability

	Andrew Dingwall, UniSoft Ltd., March 1997
	remove #ifndef __hpux from #include <arpa/inet.h>
	since current HP-UX implementations now have this file

	Andrew Dingwall, UniSoft Ltd., July 1998
	Enable the tccd port number to be specified in the systems file.
	Added support for shared API libraries.
	Changes to conform to UNIX98.
 
************************************************************************/

#include <stdlib.h>
#include <errno.h>
#include <time.h>
#include <sys/types.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <winsock.h>
#else		/* -END-WIN32-CUT- */
#  include <sys/uio.h>
#  include <sys/socket.h>
#  include <netinet/in.h>
#  include <arpa/inet.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "tptab_in.h"
#include "sysent.h"
#include "error.h"
#include "ltoa.h"
#include "inetlib_in.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_gettccdaddr() - look up the INET address and port number for
**		a TCCD and store it in the related tptab entry
**
**	return 0 if successful or -1 on error
*/

TET_IMPORT int tet_gettccdaddr(pp)
struct ptab *pp;
{
	struct in_addr addr, *ap;
	register struct sysent *sp;
	register struct tptab *tp = (struct tptab *) pp->pt_tdata;
	register int port;

	/* look up the host name in the systems file */
	errno = 0;
	if ((sp = tet_libgetsysbyid(pp->ptr_sysid)) == (struct sysent *) 0) {
		error(errno, "can't get systems file entry for sysid",
			tet_i2a(pp->ptr_sysid));
		return(-1);
	}

	/* if the system name is a host name, get the INET address */
	if ((addr.s_addr = inet_addr(sp->sy_name)) == -1) {
		if ((ap = tet_gethostaddr(sp->sy_name)) == (struct in_addr *) 0)
			return(-1);
		else
			addr = *ap;
	}

	/* get the port number */
	if (sp->sy_tccd) {
		if ((port = atoi(sp->sy_tccd)) <= 0 || port >= (1 << 16)) {
			error(0,
		"bad port number specified in the systems file for sysid",
				tet_i2a(sp->sy_sysid));
			return(-1);
		}
	}
	else if ((port = tet_gettccdport()) == -1)
		return(-1);

	/* all ok so fill in the address details */
	tp->tp_sin.sin_family = AF_INET;
	tp->tp_sin.sin_addr = addr;
	tp->tp_sin.sin_port = htons((unsigned short) port);

	return(0);
}

