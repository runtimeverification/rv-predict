/*
 *	SCCS: @(#)tetsleep.c	1.3 (97/07/21)
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
static char sccsid[] = "@(#)tetsleep.c	1.3 (97/07/21) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tetsleep.c	1.3 97/07/21 TETware release 3.8
NAME:		tetsleep.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	September 1996

DESCRIPTION:
	emulation of sleep() for WIN32

MODIFICATIONS:

************************************************************************/

#ifdef _WIN32	/* -START-WIN32-CUT- */

#include <windows.h>
#include "dtmac.h"

/*
**	tet_sleep() - like sleep() but does not return a value
*/

void tet_sleep(delay)
int delay;
{
	Sleep((DWORD) delay * 1000);
}

#else		/* -END-WIN32-CUT- */

int tet_tetsleep_c_not_used;

#endif		/* -WIN32-CUT-LINE- */

