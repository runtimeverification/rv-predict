/*
 *      SCCS:  @(#)addarg.c	1.4 (96/11/04) 
 *
 * (C) Copyright 1994 UniSoft Ltd., London, England
 *
 * All rights reserved.  No part of this source code may be reproduced,
 * stored in a retrieval system, or transmitted, in any form or by any
 * means, electronic, mechanical, photocopying, recording or otherwise,
 * except as stated in the end-user licence agreement, without the prior
 * permission of the copyright owners.
 */

#ifndef lint
static char sccsid[] = "@(#)addarg.c	1.4 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)addarg.c	1.4 96/11/04 TETware release 3.8
NAME:		addarg.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	December 1993

DESCRIPTION:
	function to concatenate two argv argument lists

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "dtetlib.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/*
**	tet_addargv() - concatenate two argv argument lists and return
**		a pointer to the results
**
**	return (char **) 0 on error
**
**	if successful, the return value points to a list containing
**	argv1[0], argv2[0] ... argv2[n], argv1[1] ... argv1[n], (char *) 0
**
**	this function is typically used to include additional option
**	arguments in an argv list before an exec
*/

char **tet_addargv(argv1, argv2)
register char **argv1, **argv2;
{
	register char **ap;
	register int newargc;
	char **newargv;
	int nalen;

	/* count the arguments in both lists + 1 for the terminating 0 */
	newargc = 1;
	if (argv1) {
		for (ap = argv1; *ap; ap++)
			;
		newargc += (ap - argv1);
	}
	if (argv2) {
		for (ap = argv2; *ap; ap++)
			;
		newargc += (ap - argv2);
	}

	/* allocate memory for the new argv array */
	newargv = (char **) 0;
	nalen = 0;
	if (BUFCHK((char **) &newargv, &nalen, (int) (newargc * sizeof *newargv)) < 0)
		return((char **) 0);

	/* copy over argv1[0] */
	ap = newargv;
	if (argv1 && *argv1)
		*ap++ = *argv1++;

	/* then copy over all of argv2 */
	if (argv2)
		while (*argv2)
			*ap++ = *argv2++;

	/* finally, copy over the rest of argv1 and terminate the array */
	if (argv1)
		while (*argv1)
			*ap++ = *argv1++;
	*ap = (char *) 0;

	return(newargv);
}

