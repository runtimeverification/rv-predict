/*
 *      SCCS:  @(#)lname2addr.c	1.7 (99/09/03)
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1993 X/Open Company Limited
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
static char sccsid[] = "@(#)lname2addr.c	1.7 (99/09/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)lname2addr.c	1.7 99/09/03 TETware release 3.8
NAME:		lname2addr.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Ltd.
DATE CREATED:	April 1993

DESCRIPTION:
	function to convert printable XTI caller address into struct netbuf
	format.

	Warning: The address returned is a pointer to a static buffer. All 
		 the fields of the structure and the address pointed to by
		 the buf field of the structure should be copied to local
		 storage after calling this function.

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#include <sys/types.h>
#include <time.h>
#include <xti.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "xtilib_xt.h"

/*
**	tet_lname2addr() - convert XTI call address from ascii to binary
*/

TET_IMPORT struct netbuf *tet_lname2addr(ln)
char *ln;
{
	static struct netbuf ret;
	static char   addr[MAX_ADDRL];

	register char *p=addr;

	char c;
	unsigned int len;

	if (!ln || !*ln)
		return (struct netbuf *)0;

	for (len=0; *ln; ln++) {
	        if (len >= MAX_ADDRL)
			return (struct netbuf *)0;

		if (isdigit(*ln))
			c = (*ln - '0')<<4;
		else {
			if (isxdigit(*ln))
				c = (*ln + 10 - (islower(*ln) ? 'a':'A'))<<4;
			else
				return (struct netbuf *)0;
		}
		ln++;
	
		if (isdigit(*ln))
			c += (*ln - '0');
		else {
			if (isxdigit(*ln))
				c += (*ln + 10 - (islower(*ln) ? 'a':'A'));
			else
				return (struct netbuf *)0;
		}
		*p++=c;
		len++;
	}

	ret.maxlen	= MAX_ADDRL;
	ret.len		= len;
	ret.buf		= addr;
	
	return (&ret);
}

