/*
 *      SCCS:  @(#)buftrace.c	1.7 (98/08/28) 
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
static char sccsid[] = "@(#)buftrace.c	1.7 (98/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)buftrace.c	1.7 98/08/28 TETware release 3.8
NAME:		buftrace.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	trace interface to tet_bufchk()

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 

************************************************************************/

#ifndef NOTRACE

#include <stdio.h>
#include "dtmac.h"
#include "error.h"
#include "ltoa.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_buftrace() - call tet_bufchk, emit trace information
**
**	return 0 if successful or -1 on error
*/

TET_IMPORT int tet_buftrace(bpp, lp, newlen, file, line)
char **bpp, *file;
int *lp, newlen, line;
{
	register int rc;

	TRACE6(tet_Tbuf, 6,
		"call bufchk from %s, %s: buf = %s, len = %s, newlen = %s",
		file ? file : "??", tet_i2a(line), tet_i2x(*bpp), tet_i2a(*lp),
		tet_i2a(newlen));

	ASSERT(newlen >= 0);
	if (*lp >= newlen) {
		TRACE1(tet_Tbuf, 6, "buffer was big enough");
		return(0);
	}

	rc = tet_bufchk(bpp, lp, newlen);

	TRACE2(tet_Tbuf, 6, "new buffer = %s", tet_i2x(*bpp));
	return(rc);
}

#else

int tet_buftrace_c_not_empty;

#endif /* !NOTRACE */

