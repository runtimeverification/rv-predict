/*
 *      SCCS:  @(#)modetoi.c	1.6 (99/09/03) 
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
static char sccsid[] = "@(#)modetoi.c	1.6 (99/09/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)modetoi.c	1.6 99/09/03 TETware release 3.8
NAME:		modetoi.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Ltd.
DATE CREATED:	August 1993

DESCRIPTION:
	function to convert -M (mode) option(s) to integer
	avoids having to do string comparisons in various places.

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1998
	Renamed the local variable tet_tpi_mode as tpi_mode so as to
	avoid confusion with the global variable called tet_tpi_mode.
 
************************************************************************/

#include <sys/types.h>
#include <time.h>
#include <xti.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "xtilib_xt.h"
#include "tsinfo_xt.h"

/*
**	tet_mode2i() - convert -M (mode) option to integer
*/

int tet_mode2i(mode)
char *mode;
{

	int tpi_mode = -1;

	if ((strcmp(mode, "TCP") == 0) || (strcmp(mode, "tcp") ==0))
		tpi_mode = TPI_TCP;
	else if ((strcmp(mode, "OSICO") == 0) || (strcmp(mode, "osico") == 0))
		tpi_mode = TPI_OSICO;
	else if ((strcmp(mode, "OSICL") == 0) || (strcmp(mode, "osicl") == 0))
		tpi_mode = TPI_OSICL;

	return (tpi_mode);
}

