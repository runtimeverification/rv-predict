/*
 *      SCCS:  @(#)ltoo.c	1.6 (98/08/28) 
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
static char sccsid[] = "@(#)ltoo.c	1.6 (98/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)ltoo.c	1.6 98/08/28 TETware release 3.8
NAME:		ltoo.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to convert long int to octal ascii
	avoids having to use sprintf to format error messages

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#include "dtmac.h"
#include "ltoa.h"

/*
**	tet_l2o() - convert long int to octal ascii
*/

TET_IMPORT char *tet_l2o(n)
long n;
{
	static char buf[NLBUF][LONUMSZ];
	static int count;
	register char *p;
	register unsigned long u;

	if (++count >= NLBUF)
		count = 0;
	p = &buf[count][LONUMSZ - 1];
	*p = '\0';

	if ((u = (unsigned long) n) != 0)
		do {
			*--p = (u & 07) + '0';
		} while ((u >>= 3) != 0L);

	*--p = '0';

	return(p);
}

