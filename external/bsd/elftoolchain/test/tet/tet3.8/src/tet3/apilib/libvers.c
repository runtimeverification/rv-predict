/*
 *	SCCS: @(#)libvers.c	1.1 (98/09/01)
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
static char sccsid[] = "@(#)libvers.c	1.1 (98/09/01) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)libvers.c	1.1 98/09/01 TETware release 3.8
NAME:		libvers.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	July 1998

DESCRIPTION:
	API library version strings

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "apilib.h"


/*
** the definition of TET_VERSION must be here and not in a header file
** because we want to pick up the value of the Q keyword that is defined
** in this file
**
** this definition of TET_VERSION (before expansion by SCCS) must be the same
** as the one in tcm/ckversion.c
*/
#ifdef TET_LITE /* -LITE-CUT-LINE- */
#  define TET_VERSION			"3.8-lite"
#else /* -START-LITE-CUT- */
#  define TET_VERSION			"3.8"
#endif /* -END-LITE-CUT- */

#define TET_VERSION_STRINGS		tet_apilib_version
#define TET_VERSION_STORAGE_CLASS	TET_IMPORT


/*
** apilib/version.c contains a definition of TET_VERSION_STRINGS in terms
** of the #defines supplied in this file
*/
#include "version.c"


