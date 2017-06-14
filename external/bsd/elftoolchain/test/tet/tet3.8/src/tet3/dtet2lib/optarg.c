/*
 *      SCCS:  @(#)optarg.c	1.6 (96/11/04) 
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
static char sccsid[] = "@(#)optarg.c	1.6 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)optarg.c	1.6 96/11/04 TETware release 3.8
NAME:		optarg.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to generate an option argument string

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "dtetlib.h"

/*
**	tet_mkoptarg() - make a '-' option string
**
**	return the number of (non-null) characters in the string
**
**	if first is false, the string starts with a space
*/

int tet_mkoptarg(s, intopt, arg, first)
char *s;
int intopt;
register char *arg;
int first;
{
	register char *p = s;
	char opt = (char) (intopt & 0377);

	if (!first)
		*p++ = ' ';

	*p++ = '-';
	*p++ = opt;

	if (arg)
		while (*arg)
			*p++ = *arg++;

	*p = '\0';

	return(p - s);
}

