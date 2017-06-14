/*
 *      SCCS:  @(#)mapsig.c	1.6 (96/11/04) 
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
static char sccsid[] = "@(#)mapsig.c	1.6 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)mapsig.c	1.6 96/11/04 TETware release 3.8
NAME:		mapsig.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to convert local signal number to machine-independent
	signal number

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "sigmap.h"
#include "error.h"
#include "ltoa.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_mapsignal() - map local signal number to DTET signal number
**
**	return DTET signal number if successful or -1 on error
*/

int tet_mapsignal(sig)
register int sig;
{
	register struct sigmap *sp, *se;
	extern struct sigmap tet_sigmap[];
	extern int tet_Nsigmap;

	/* if the DTET signal number is the same as the local one, use that;
		otherwise we have to search the whole table */
	if (sig >= 0 && sig < tet_Nsigmap && sig == tet_sigmap[sig].sig_dtet)
		return(sig);
	else
		for (sp = tet_sigmap, se = sp + tet_Nsigmap; sp < se; sp++)
			if (sp->sig_local == sig)
				return(sp->sig_dtet);

	error(0, "local signal not found in sigmap:", tet_i2a(sig));
	return(-1);
}

