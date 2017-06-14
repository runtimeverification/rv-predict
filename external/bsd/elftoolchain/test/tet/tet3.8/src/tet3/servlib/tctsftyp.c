/*
 *	SCCS: @(#)tctsftyp.c	1.2 (05/07/08)
 *
 *	The Open Group, Reading, England
 *
 * Copyright (c) 2003 The Open Group
 * All rights reserved.
 *
 * No part of this source code may be reproduced, stored in a retrieval
 * system, or transmitted, in any form or by any means, electronic,
 * mechanical, photocopying, recording or otherwise, except as stated
 * in the end-user licence agreement, without the prior permission of
 * the copyright owners.
 * A copy of the end-user licence agreement is contained in the file
 * Licence which accompanies this distribution.
 * 
 * Motif, OSF/1, UNIX and the "X" device are registered trademarks and
 * IT DialTone and The Open Group are trademarks of The Open Group in
 * the US and other countries.
 *
 * X/Open is a trademark of X/Open Company Limited in the UK and other
 * countries.
 *
 */

#ifndef lint
static char sccsid[] = "@(#)tctsftyp.c	1.2 (05/07/08) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tctsftyp.c	1.2 05/07/08 TETware release 3.8
NAME:		tctsftyp.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, The Open Group
DATE CREATED:	March 2003

DESCRIPTION:
	send the file type list to TCCD on a remote system

MODIFICATIONS:

	Geoff Clare, The Open Group, July 2005
	Missing sysid parameter declaration.

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "error.h"
#include "avmsg.h"
#include "ftype.h"
#include "ltoa.h"
#include "servlib.h"
#include "dtetlib.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* maximum length of a file name suffix - should be plenty! */
#define SUFMAX	128


/* static function declarations */
static int tcsndft2 PROTOLIST((int, char *[], int));
static int tcsndft3 PROTOLIST((int, char *[], int));


/*
**	tet_tcsndftype() - send the internal list of file types to
**		TCCD on a remote system
**
**	the list must previously have been set up by one or more calls to
**	tet_addftype()
**
**	return 0 if successful or -1 (with tet_tcerrno set) on error
*/
int tet_tcsndftype(sysid)
int sysid;
{
	struct tet_ftype *ftp;
	char line[SUFMAX + LNUMSZ + 2];
	char *lines[AV_NLINE];
	int Nlines = 0;
	char *s;
	int rc = 0;

	/* rewind the list position */
	tet_setftent();

	/*
	** look up each file type entry in turn -
	** when we have AV_NLINE entries, send them to the remote system
	*/
	while ((ftp = tet_getftent()) != (struct tet_ftype *) 0) {
		sprintf(line, "%.*s %d", SUFMAX, ftp->ft_suffix, ftp->ft_ftype);
		if ((s = tet_strstore(line)) == (char *) 0) {
			tet_tcerrno = ER_ERR;
			rc = -1;
			break;
		}
		lines[Nlines++] = s;
		if (Nlines > AV_NLINE) {
			rc = tcsndft2(sysid, lines, Nlines);
			Nlines = 0;
			if (rc < 0)
				break;
		}
	}

	if (Nlines > 0 && rc == 0) {
		rc = tcsndft2(sysid, lines, Nlines);
		Nlines = 0;
	}

	return(rc);
}

/*
**	tcsndft2() - send a set of file type entries to the remote system
**
**	return 0 if successful or -1 (with tet_tcerrno set) on error
*/
static int tcsndft2(sysid, lines, Nlines)
int sysid;
char *lines[];
int Nlines;
{
	int n, rc;

	ASSERT(Nlines > 0);

	rc = tcsndft3(sysid, lines, Nlines);

	/* free the lines allocated in the calling function  */
	for (n = 0; n < Nlines; n++) {
		TRACE2(tet_Tbuf, 6, "tcsndft2(): free line = %s",
			tet_i2x(lines[n]));
		if (lines[n]) {
			free((void *) lines[n]);
			lines[n] = (char *) 0;
		}
	}

	return(rc);
}

/*
**	tcsndft3() - extend the tcsndft2 processing
**
**	return 0 if successful or -1 (with tet_tcerrno set) on error
*/
static int tcsndft3(sysid, lines, Nlines)
int sysid;
char *lines[];
int Nlines;
{
	extern char tet_tcerrmsg[];
	struct avmsg *mp;
	char *dp;
	int n;

	/* get the TCCD message buffer */
	if ((mp = (struct avmsg *) tet_tcmsgbuf(sysid, avmsgsz(OP_TSFTYPE_ARGC(Nlines)))) == (struct avmsg *) 0) {
		tet_tcerrno = ER_ERR;
		return(-1);
	}

	/* set up the request message */
	mp->av_argc = OP_TSFTYPE_ARGC(Nlines);
	for (n = 0; n < Nlines; n++)
		AV_TSFTYPE(mp, n) = lines[n];

	/* send the request and receive the reply */
	dp = tet_tctalk(sysid, OP_TSFTYPE, TALK_DELAY);

	/* handle the return codes */
	switch (tet_tcerrno) {
	case ER_OK:
		return(0);
	case ER_ERR:
		if (!dp)
			break;
		/* else fall through */
	default:
		error(0, tet_tcerrmsg, tet_ptrepcode(tet_tcerrno));
		break;
	}

	/* here for server error return */
	return(-1);
}

