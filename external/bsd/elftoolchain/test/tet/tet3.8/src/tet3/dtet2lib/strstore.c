/*
 *      SCCS:  @(#)strstore.c	1.8 (98/08/28) 
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
static char sccsid[] = "@(#)strstore.c	1.8 (98/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)strstore.c	1.8 98/08/28 TETware release 3.8
NAME:		strstore.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to allocate string storage in static memory

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include "dtmac.h"
#include "error.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

#ifndef NOTRACE
#include "ltoa.h"
#endif


/*
**	tet_strstore() - store a string in memory obtained from malloc
**		and return a pointer thereto
**
**	return (char *) 0 on error
*/

TET_IMPORT char *tet_strstore(s)
char *s;
{
	size_t len;
	register char *p;

	len = strlen(s) + 1;

	errno = 0;
	if ((p = malloc(len)) == (char *) 0)
		error(errno, "can't get memory for string:", s);
	else
		(void) strcpy(p, s);

	TRACE4(tet_Tbuf, 6, "tet_strstore(\"%.24s%s\") returns %s",
		s, len > 25 ? " ..." : "", tet_i2x(p));
	return(p);
}

