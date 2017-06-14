/*
 *	SCCS: @(#)llib-ltcm.c	1.2 (96/11/04)
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


/************************************************************************

SCCS:   	@(#)llib-ltcm.c	1.2 96/11/04 TETware release 3.8
NAME:		llib-ltcm.c
PRODUCT:	TETware
AUTHOR:		Geoff Clare, UniSoft Ltd.
DATE CREATED:	October 1996

DESCRIPTION:

	Lint library for use in linting applications which use the
	Test Case Manager (TCM) and API library.

MODIFICATIONS:

************************************************************************/

/* LINTLIBRARY */

#define TET_API_ONLY /* exclude non-API definitions in llib-lapi.c */
#include "llib-lapi.c"

extern	void	(*tet_startup)();
extern	void	(*tet_cleanup)();
extern	struct tet_testlist tet_testlist[];

int main(argc, argv)
int argc;
char **argv;
{
	(*tet_startup)();
	(*tet_cleanup)();
	return tet_testlist[0].icref;
}

int	tet_nosigreset;
char *	tet_pname;
int	tet_thistest;

