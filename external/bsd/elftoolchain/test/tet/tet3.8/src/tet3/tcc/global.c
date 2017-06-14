/*
 *	SCCS: @(#)global.c	1.4 (98/09/01)
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1996 X/Open Company Limited
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
static char sccsid[] = "@(#)global.c	1.4 (98/09/01) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)global.c	1.4 98/09/01 TETware release 3.8
NAME:		global.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	tcc global data

	data items in this file are also shared by tetscpp
	(the unsupported scenario analysing program)

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., June 1997
	added support for the -I command-line option

	Andrew Dingwall, UniSoft Ltd., July 1998
	moved tet_root[] to dtet2lib/globals.c

************************************************************************/

#include <stdio.h>
#include <sys/types.h>
#include <time.h>
#include "dtmac.h"
#include "tcc.h"

/* operation modes from the command line (-b, -e, -c) */
int tcc_modes;

/* compatibility mode derived from TET_COMPAT -
** used to resolve ambiguities in the scenario language
*/
int tet_compat;

/* alternate execution directory on the local system
** from TET_EXECUTE in the environment or from the -a command-line option
*/
char *tet_execute;

/* test suite root on the local system */
char *tet_tsroot;

/* test suite root on the local system from the environment
** (defaults to tet_root)
*/
char *tet_suite_root;

/* runtime directory on the local system from the environment */
char *tet_run;

/* temporary directory on the local system from the environment */
char *tet_tmp_dir;

/* flag derived from the -I command-line option */
int tcc_Iflag;

/* flag derived from the -p command-line option */
int report_progress;

/* test case timeout from the -t command-line option */
int tcc_timeout;

/* scenario error counter */
int scenerrors;

