/*
 *	SCCS: @(#)tetunlnk.c	1.3 (99/09/02)
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1997 X/Open Company Limited
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
static char sccsid[] = "@(#)tetunlnk.c	1.3 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tetunlnk.c	1.3 99/09/02 TETware release 3.8
NAME:		tetunlnk.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	March 1997

DESCRIPTION:
	emulation of unlink() for WIN32

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1999
	added support for shared API libraries

************************************************************************/

#ifdef _WIN32	/* -START-WIN32-CUT- */

#include <io.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include "dtmac.h"

/*
**	tet_unlink() - unlink a file on a WIN32 platform
**
**	if the file is read-only, the mode must first be changed to
**	read/write before unlink() is called
*/

TET_IMPORT int tet_unlink(file)
char *file;
{
	if (_access(file, 02) < 0)
		switch (errno) {
		case EACCES:
			(void) _chmod(file, _S_IREAD | _S_IWRITE);
			break;
		case ENOENT:
			return(-1);
		}

	return(_unlink(file));
}

#else		/* -END-WIN32-CUT- */

int tet_tetunlnk_c_not_used;

#endif		/* -WIN32-CUT-LINE- */

