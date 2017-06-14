/*
 *	SCCS: @(#)tcxconf.c	1.3 (96/11/04)
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
static char sccsid[] = "@(#)tcxconf.c	1.3 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcxconf.c	1.3 (96/11/04) TETware release 3.8
NAME:		tcxconf.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	perform a configuration variable exchange with a TCCD

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "error.h"
#include "ltoa.h"
#include "servlib.h"
#include "dtetlib.h"
#include "config.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/* scratchpad variable lists */
static struct cflist scfg, tcfg;

/* static function declarations */
static int mconfig PROTOLIST((int, struct cflist *, struct cflist *));
static int xconfig PROTOLIST((int, struct cflist *, struct cflist *));


/*
**	tet_tcxconfig() - perform a config variable exchange
**		with a remote system
**
**	sysid specifies which system to use
**	fname specifies the remote config file name
**	mlp points to the master config list
**	vlp points to the tcc command-line config list (-v options)
**	rlp points to the config list where the lines received from
**		the remote system should be stored
**
**	return 0 if successful or -1 on error
*/

int tet_tcxconfig(sysid, fname, mlp, vlp, rlp)
int sysid;
char *fname;
struct cflist *mlp, *vlp, *rlp;
{
	struct cflist *slp;
	register int rc;
	register char **cp;

	/* send the config file name to TCCD */
	if (tet_tccfname(sysid, fname) < 0) {
		error(0, "tet_tccfname failed, rc =",
			tet_ptrepcode(tet_tcerrno));
		return(-1);
	}

	/*
	** if there are command-line variables, merge them with the
	** master set;
	** otherwise, just use the master config variables
	*/
	if (vlp->cf_nconf > 0) {
		rc = mconfig(sysid, mlp, vlp);
		slp = &scfg;
	}
	else {
		rc = 0;
		slp = mlp;
	}

	/* then perform the config variable exchange */
	if (!rc)
		rc = xconfig(sysid, slp, rlp);
	else
		tet_tcerrno = ER_ERR;

	/* free any memory allocated by mconfig() */
	if (tcfg.cf_conf) {
		for (cp = tcfg.cf_conf; cp < tcfg.cf_conf + tcfg.cf_nconf; cp++)
			if (*cp) {
				TRACE2(tet_Tbuf, 6, "free tmp conf line = %s",
					tet_i2x(*cp));
				free(*cp);
			}
		TRACE2(tet_Tbuf, 6, "free tcfg.cf_conf = %s",
			tet_i2x(tcfg.cf_conf));
		free((char *) tcfg.cf_conf);
		tcfg.cf_conf = (char **) 0;
		tcfg.cf_lconf = tcfg.cf_nconf = 0;
	}
	if (scfg.cf_conf) {
		TRACE2(tet_Tbuf, 6, "free scfg.cf_conf = %s",
			tet_i2x(scfg.cf_conf));
		free((char *) scfg.cf_conf);
		scfg.cf_conf = (char **) 0;
		scfg.cf_lconf = scfg.cf_nconf = 0;
	}

	return(rc);
}

/*
**	mconfig() - merge the command-line variables with the master set
**
**	return 0 if successful or -1 on error
*/

static int mconfig(sysid, mlp, vlp)
int sysid;
register struct cflist *mlp, *vlp;
{
	static char fmt[] = "TET_REM%03d_%.*s";
	char buf[MAXPATH * 2];
	register char **cp1, **cp2;
	register char *p;

	/* allocate storage for the list of config lines to send */
	scfg.cf_nconf = mlp->cf_nconf + vlp->cf_nconf;
	if (BUFCHK((char **) &scfg.cf_conf, &scfg.cf_lconf, (int) (scfg.cf_nconf * sizeof *scfg.cf_conf)) < 0)
		return(-1);

	/* copy over the master config lines */
	cp2 = scfg.cf_conf;
	for (cp1 = mlp->cf_conf; cp1 < mlp->cf_conf + mlp->cf_nconf; cp1++)
		*cp2++ = *cp1;

	/*
	** do two passes over the MTCC cmd line variables:
	**	in the first pass:
	**		if a variable is not a TET_REMnnn one, prepend
	**		a TET_REMnnn string so that it will override a
	**		local value on the remote system;
	**		a pointer to the new string is stored in a temporary
	**		list (tconf) so that it can be freed later
	**	in the second pass:
	**		add the TET_REMnnn variables for this system last
	**		of all, giving them precedence over all the others
	*/

	/* first pass */
	for (cp1 = vlp->cf_conf; cp1 < vlp->cf_conf + vlp->cf_nconf; cp1++) {
		if (tet_remvar(*cp1, -1) != *cp1)
			continue;
		(void) sprintf(buf, fmt, sysid % 1000,
			sizeof buf - sizeof fmt, *cp1);
		if (BUFCHK((char **) &tcfg.cf_conf, &tcfg.cf_lconf, (int) ((tcfg.cf_nconf + 1) * sizeof *tcfg.cf_conf)) < 0)
			return(-1);
		if ((p = tet_strstore(buf)) == (char *) 0)
			return(-1);
		*(tcfg.cf_conf + tcfg.cf_nconf++) = p;
		if (cp2 < scfg.cf_conf + scfg.cf_nconf)
			*cp2++ = p;
	}

	/* second pass */
	for (cp1 = vlp->cf_conf; cp1 < vlp->cf_conf + vlp->cf_nconf; cp1++) {
		if (tet_remvar(*cp1, sysid) == *cp1)
			continue;
		if (cp2 < scfg.cf_conf + scfg.cf_nconf)
			*cp2++ = *cp1;
	}

	scfg.cf_nconf = cp2 - scfg.cf_conf;
	return(0);
}

/*
**	xconfig() - perform the actual configuration variable exchange
**
**	return 0 if successful or -1 on error
*/

static int xconfig(sysid, slp, rlp)
int sysid;
struct cflist *slp, *rlp;
{
	int nlines, done;
	register int n;
	register char **cp1, **cp2;

	/* send the config lines */
	if (tet_tcsndconfv(sysid, slp->cf_conf, slp->cf_nconf) < 0) {
		error(0, "tet_tcsndconfv failed, rc =",
			tet_ptrepcode(tet_tcerrno));
		return(-1);
	}

	/* receive the merged ones back */
	do {
		if ((cp1 = tet_tcrcvconfv(sysid, &nlines, &done)) == (char **) 0) {
			error(0, "tet_tcrcvconfv failed, rc =",
				tet_ptrepcode(tet_tcerrno));
			return(-1);
		}
		if (nlines <= 0)
			continue;
		if (BUFCHK((char **) &rlp->cf_conf, &rlp->cf_lconf, (int) ((rlp->cf_nconf + nlines) * sizeof *rlp->cf_conf)) < 0) {
			tet_tcerrno = ER_ERR;
			return(-1);
		}
		cp2 = rlp->cf_conf + rlp->cf_nconf;
		for (n = 0; n < nlines; n++)
			if (*cp1 && (*cp2++ = tet_strstore(*cp1)) == (char *) 0) {
				tet_tcerrno = ER_ERR;
				return(-1);
			}
			else
				cp1++;
		rlp->cf_nconf = cp2 - rlp->cf_conf;
	} while (!done);

	/* all OK so return success */
	return(0);
}

