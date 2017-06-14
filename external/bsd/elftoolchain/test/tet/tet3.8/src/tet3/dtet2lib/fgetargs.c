/*
 *      SCCS:  @(#)fgetargs.c	1.5 (96/09/30) 
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
static char sccsid[] = "@(#)fgetargs.c	1.5 (96/09/30) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)fgetargs.c	1.5 96/09/30 TETware release 3.8
NAME:		fgetargs.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to read a non-blank, non-comment line from a file and
	split it into fields

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "dtetlib.h"

/*
**	tet_fgetargs() - read a non-blank, non-comment line from a file
**		and split it into up to maxargs fields
**
**	return the number of fields found, or EOF on end-of-file
*/

int tet_fgetargs(fp, argv, maxargs)
FILE *fp;
register char **argv;
register int maxargs;
{
	static char buf[BUFSIZ];
	register char *p;
	register int argc;

	do {
		if (fgets(buf, sizeof buf, fp) == NULL)
			argc = EOF;
		else {
			for (p = buf; *p; p++)
				if (*p == '#' || *p == '\n') {
					*p = '\0';
					break;
				}
			argc = tet_getargs(buf, argv, maxargs);
		}
	} while (!argc);

	return(argc);
}

