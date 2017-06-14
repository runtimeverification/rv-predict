/*
 *      SCCS:  @(#)generror.c	1.7 (98/08/28) 
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
static char sccsid[] = "@(#)generror.c	1.7 (98/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)generror.c	1.7 98/08/28 TETware release 3.8
NAME:		generror.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	generic error message printing function

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "globals.h"
#include "dtetlib.h"

/*
**	tet_generror() - generic error printing routine
*/

void tet_generror(errnum, file, line, s1, s2)
int errnum, line;
char *file, *s1, *s2;
{
	tet_prerror(stderr, errnum, tet_progname, file, line, s1, s2);
}

