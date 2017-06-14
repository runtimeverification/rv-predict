/*
 *      SCCS:  @(#)ptype.c	1.8 (98/08/28) 
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
static char sccsid[] = "@(#)ptype.c	1.8 (98/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)ptype.c	1.8 98/08/28 TETware release 3.8
NAME:		ptype.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	function to return printable representation of process type

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#include <stdio.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ltoa.h"
#include "dtetlib.h"

/*
**	tet_ptptype() - return a printable representation of a process type
*/

TET_IMPORT char *tet_ptptype(ptype)
int ptype;
{
	static char text[] = "process-type ";
	static char msg[sizeof text + LNUMSZ];
	
	switch (ptype) {
	case PT_NOPROC:
		return("<no process>");
	case PT_MTCC:
		return("MTCC");
	case PT_STCC:
		return("STCC");
	case PT_MTCM:
		return("MTCM");
	case PT_STCM:
		return("STCM");
	case PT_XRESD:
		return("XRESD");
	case PT_SYNCD:
		return("SYNCD");
	case PT_STAND:
		return("STANDALONE");
	default:
		(void) sprintf(msg, "%s%d", text, ptype);
		return(msg);
	}
}

