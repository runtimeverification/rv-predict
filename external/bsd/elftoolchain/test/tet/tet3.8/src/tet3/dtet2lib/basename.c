/*
 *	SCCS: @(#)basename.c	1.2 (99/11/15)
 *
 *	UniSoft Ltd., London, England
 *
 * Copyright (c) 1998 The Open Group
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
static char sccsid[] = "@(#)basename.c	1.2 (99/11/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)basename.c	1.2 99/11/15 TETware release 3.8
NAME:		basename.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	May 1998

DESCRIPTION:
	function to return the last part of a path name

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "dtetlib.h"

/*
**	tet_basename() - return the last component of a path name
**
**	note that both '/' and '\' are interpreted as the
**	directory separator character
**
**	Since this function may be called from the child of a
**	multi-threaded parent in strict POSIX threads mode, it must not
**	call any non async-signal safe functions
*/

TET_IMPORT char *tet_basename(path)
char *path;
{
	register char *p;
	register char *retval = path;

	if (path)
		for (p = path; *p; p++)
			if (isdirsep(*p) && *(p + 1))
				retval = p + 1;

	return(retval);
}

