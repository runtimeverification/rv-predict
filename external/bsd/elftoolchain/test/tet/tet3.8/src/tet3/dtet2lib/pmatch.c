/*
 *	SCCS: @(#)pmatch.c	1.1 (03/03/26)
 *
 * (C) Copyright 2003 X/Open Company Limited
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
static char sccsid[] = "@(#)pmatch.c	1.1 (03/03/26) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)pmatch.c	1.1 03/03/26 TETware release 3.8
NAME:		pmatch.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, The Open Group
DATE CREATED:	March 2003

DESCRIPTION:
	shell pattern matching function
	original code by Denis McConalogue

	this function moved from tcclib/procdir.c to here

MODIFICATIONS:
 
************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "dtetlib.h"

/*
**	tet_pmatch() - Match a string against a pattern.
**
**	Shell metacharacters (*, ?, [, ]) are allowed in the
**	pattern, and the matching rules are the same as for
**	filename completion.
**
**	Returns 1 if the string matches, 0 otherwise.
*/

#define MASK(c) ((c) & 0177)
#define META(c) ((c) == '?' || (c) == '*' || (c) == '[' || (c) == ']')

int tet_pmatch(str, pattern)
register char *str, *pattern;
{
        register int schar;
        register char c;
        int unresolved, lchar, notflag;

        schar = MASK(*str);
	str++;

	switch (c = *pattern++) {
	case '[':
		unresolved = 0;
		notflag  = 0;
		lchar = 077777;
		if (*pattern == '!') {
			notflag++;
			pattern++;
		}
		while ((c = *pattern++))
			switch (c) {
			case ']':
				return (unresolved ?
					tet_pmatch(str, pattern) : 0);
			case '-':
				if (notflag) {
					if (lchar > schar || schar > *pattern++)
						unresolved++;
					else
						return(0);
				}
				else if (lchar <= schar && schar <= *pattern++)
					unresolved++;
				break;
			default:
				if (notflag) {
					if (schar != (lchar = MASK(c)))
						unresolved++;
					else
						return(0);
				}
				else if (schar == (lchar = MASK(c)))
					unresolved++;
				break;	
			}
		return(0);
	case '?':
		return (schar ? tet_pmatch(str, pattern) : 0);
	case '*':
		if (!*pattern)
			return(1);
		str--;
		while (*str)
			if (tet_pmatch(str++, pattern))
				return(1);
		return(0);
	case '\\':
		if ((c = *pattern) && (META(c) || c == '\\'))
			pattern++;
		else
			c = '\\';
		if (MASK(c) != schar)
			return(0);
		return(tet_pmatch(str, pattern));
	case 0:
		return(schar == 0 ? 1 : 0);
	default:
		if (MASK(c) != schar)
			return(0);
	}

	return(tet_pmatch(str, pattern));
}

