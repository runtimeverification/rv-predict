/*
 *      SCCS:  @(#)sysent.c	1.9 (98/08/28) 
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
static char sccsid[] = "@(#)sysent.c	1.9 (98/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)sysent.c	1.9 98/08/28 TETware release 3.8
NAME:		sysent.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	systems file search function

MODIFICATIONS:
	
	Denis McConalogue, DTET Development, May 1993
	XTI support - allow TCCD XTI addresses to be included in
		      the systems file.

	Andrew Dingwall, UniSoft Ltd., July 1997
	added support the MT DLL version of the C runtime support library
	on Win32 systems

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#ifndef TET_LITE	/* -START-LITE-CUT- */

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include "dtmac.h"
#include "sysent.h"
#include "error.h"
#include "globals.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

static FILE *sfp = NULL;		/* systems file stream pointer */

/*
**	tet_libgetsysent() - get an entry from the systems file
**		and return a pointer thereto
**
**	return (struct sysent *) 0 on error
**
**	warning: the string pointed to by sy.sy_name is stored in the static
**	buffer owned by fgetargs() so, if it is needed, it should be copied
**	to somewhere less exposed soon after a call to tet_libgetsysent()
*/

struct sysent *tet_libgetsysent()
{
	static struct sysent sysent;
	char *args[3];
	register int rc;

	if (sfp == NULL && tet_libsetsysent() < 0)
		return((struct sysent *) 0);

	rc = 0;
	while (rc < 2)
		if ((rc = tet_fgetargs(sfp, args, 3)) == EOF)
			return((struct sysent *) 0);

	sysent.sy_sysid = atoi(args[0]);
	sysent.sy_name = args[1];
	if (rc > 2) 
		sysent.sy_tccd = args[2];
	else
		sysent.sy_tccd = (char *)0;

	return(&sysent);
}

/*
**	tet_libsetsysent() - rewind to the start of the systems file,
**		opening it if necessary
**
**	return 0 if successful or -1 on error
*/

int tet_libsetsysent()
{
	static char file[] = "systems";
	char path[MAXPATH + 1];

	if (sfp != NULL) {
		rewind(sfp);
		return(0);
	}

	ASSERT(tet_root[0]);
	(void) sprintf(path, "%.*s/%s",
		(int) sizeof path - (int) sizeof file - 1, tet_root, file);

	if ((sfp = fopen(path, "r")) == NULL) {
		error(errno, "can't open", path);
		return(-1);
	}

	(void) tet_fioclex(FILENO(sfp));
	return(0);
}

/*
**	tet_libendsysent() - close the systems file
*/

void tet_libendsysent()
{
	if (sfp != NULL) {
		(void) fclose(sfp);
		sfp = NULL;
	}
}

#else	/* -END-LITE-CUT- */

int tet_sysent_c_not_empty;

#endif /* !TET_LITE */	/* -LITE-CUT-LINE- */

