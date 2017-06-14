/*
 *	SCCS: @(#)getcwd.c	1.4 (99/09/02)
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
static char sccsid[] = "@(#)getcwd.c	1.4 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)getcwd.c	1.4 99/09/02 TETware release 3.8
NAME:		getcwd.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	September 1996

DESCRIPTION:
	emulation of the getcwd() library routine for WIN32

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1999
	added support for shared API libraries

************************************************************************/

#ifdef _WIN32	/* -START-WIN32-CUT- */

#include <stdlib.h>
#include <ctype.h>
#include <direct.h>
#include "dtmac.h"


/*
**	tet_getcwd() - emulate the UNIX getcwd() library routine
*/

TET_IMPORT char *tet_getcwd(buffer, maxlen)
char *buffer;
int maxlen;
{
	register char *p;
	char *cwd;

	/* get the current working directory */
	if ((cwd = _getcwd(buffer, maxlen)) == (char *) 0)
		return((char *) 0);

	/* turn the drive letter into lower case */
	if (isupper(*cwd))
		*cwd = tolower(*cwd);

	/* replace each \ character in the path with a / character */
	for (p = cwd; *p; p++)
		if (*p == '\\')
			*p = '/';

	return(cwd);
}

#else		/* -END-WIN32-CUT- */

int tet_getcwd_c_not_used;

#endif		/* -WIN32-CUT-LINE- */

