/*
 *      SCCS:  @(#)inetoul.c	1.6 (96/11/04) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1993 X/Open Company Limited
 *
 * All rights reserved.  No part of this source code may be reproduced,
 * stored in a retrieoctet system, or transmitted, in any form or by any
 * means, electronic, mechanical, photocopying, recording or otherwise,
 * except as stated in the end-user licence agreement, without the prior
 * permission of the copyright owners.
 *
 * X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
 * the UK and other countries.
 */

#ifndef lint
static char sccsid[] = "@(#)inetoul.c	1.6 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)inetoul.c	1.6 96/11/04 TETware release 3.8
NAME:		inetoul.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Ltd.
DATE CREATED:	August 1993

DESCRIPTION:
	Convert an inet address of the form aa.bb.cc.dd to unsigned long.
	Returns 0 if the char format address is invalid. Network byte
	ordering is assumed.

MODIFICATIONS:
	
************************************************************************/

#include <stdio.h>
#include <ctype.h>
#include <sys/types.h>
#include <errno.h>
#ifdef TCPTPI
#  include <netinet/in.h>
#endif
#include "dtmac.h"
#include "error.h"
#include "dtetlib.h"

#define	N_PARTS	4

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

unsigned long tet_inetoul(cp)
register char *cp;
{
#ifdef TCPTPI
	unsigned long ip=0, base, part;
	int i;
	char c, *sp=cp;
	
	for (i=0; i < N_PARTS; i++, cp++) {
	
	        /* Accumulate part up to `.' with 0x=hex, 0=octal,
		   anything else assumed decimal */
	
		base  = 10;
		if (*cp == '0') {
	                base = 8, cp++;
			if (*cp == 'X' || *cp == 'x')
				base = 16, cp ++;
		}
	
		part = 0;
	        while ((c = *cp) && (c != '.')) {
	                if (isdigit(c)) {
	                        part = (part*base) + (c-'0');
	                        cp++;
	                }
			else if (base == 16 && isxdigit(c)) {
	                        part = (part*base) + (c+10-(islower(c)?'a':'A'));
	                        cp++;
			}
			else if (*cp != '.') {
				error(0, "bad format IP address", sp);
				return (0);
			}
	        }
		ip = (ip << 8) + part;
	}
	return (htonl(ip));
#else
	return ((unsigned long) 0);
#endif /* TCPTPI */
}

