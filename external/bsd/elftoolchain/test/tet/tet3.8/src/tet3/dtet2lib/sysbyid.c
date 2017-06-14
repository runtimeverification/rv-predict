/*
 *      SCCS:  @(#)sysbyid.c	1.5 (96/11/04) 
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
static char sccsid[] = "@(#)sysbyid.c	1.5 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)sysbyid.c	1.5 96/11/04 TETware release 3.8
NAME:		sysbyid.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	systems file search function

MODIFICATIONS:

************************************************************************/

#ifndef TET_LITE	/* -START-LITE-CUT- */

#include <errno.h>
#include "dtmac.h"
#include "sysent.h"

/*
**	tet_libgetsysbyid() - get systems file entry for sysid and return
**		a pointer thereto
**
**	return (struct sysent *) 0 on if not found or on error
*/

struct sysent *tet_libgetsysbyid(sysid)
register int sysid;
{
	static struct sysent *sp;

	if (tet_libsetsysent() < 0)
		return((struct sysent *) 0);

	errno = 0;
	do {
		if (sp && sp->sy_sysid == sysid)
			break;
	} while ((sp = tet_libgetsysent()) != (struct sysent *) 0);

	return(sp);
}

#else	/* -END-LITE-CUT- */

int tet_sysbyid_c_not_empty;

#endif /* !TET_LITE */	/* -LITE-CUT-LINE- */

