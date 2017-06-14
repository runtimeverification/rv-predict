/*
 *      SCCS:  @(#)tcutime.c	1.1 (03/03/26) 
 *
 * (C) Copyright 2003 The Open Group
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
static char sccsid[] = "@(#)tcutime.c	1.1 (03/03/26) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcutime.c	1.1 03/03/26 TETware release 3.8
NAME:		tcutime.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, The Open Group
DATE CREATED:	March 2003

DESCRIPTION:
	function to request TCCD to set the access and mod times on a file

MODIFICATIONS:

************************************************************************/

#include "dtmac.h"
#include "dtmsg.h"
#include "avmsg.h"
#include "servlib.h"

/*
**	tet_tcutime() - send an OP_UTIME message to TCCD and receive a reply
**
**	return 0 if successful or -1 on error
*/

int tet_tcutime(sysid, path, atime, mtime)
int sysid;
char *path;
long atime, mtime;
{
	struct avmsg *mp;

	/* make sure that path is non-null and that the times are non-zero */
	if (!path || !*path || !atime || !mtime) {
		tet_tcerrno = ER_INVAL;
		return(-1);
	}

	/* get the TCCD message buffer */
	if ((mp = (struct avmsg *) tet_tcmsgbuf(sysid, avmsgsz(OP_UTIME_ARGC))) == (struct avmsg *) 0) {
		tet_tcerrno = ER_ERR;
		return(-1);
	}

	/*
	** set up the request message
	** NOTE: assumes that a time will fit into a signed 32-bit value
	*/
	mp->av_argc = OP_UTIME_ARGC;
	AV_PATH(mp) = path;
	AV_ATIME(mp) = atime;
	AV_MTIME(mp) = mtime;

	/* send the request and receive the reply */
	return(tet_tcrsys(sysid, OP_UTIME));
}

