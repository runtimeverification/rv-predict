/*
 *	SCCS: @(#)llib-ltcmc.c	1.2 (96/11/04)
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

SCCS:   	@(#)llib-ltcmc.c	1.2 96/11/04 TETware release 3.8
NAME:		llib-ltcm.c
PRODUCT:	TETware
AUTHOR:		Geoff Clare, UniSoft Ltd.
DATE CREATED:	October 1996

DESCRIPTION:

	Lint library for use in linting applications which use a
	child TCM (e.g. tcmchild.o or tcmrem.o) and the API library.

MODIFICATIONS:

************************************************************************/

/* LINTLIBRARY */

#define TET_API_ONLY /* exclude non-API definitions in llib-lapi.c */
#include "llib-lapi.c"

extern int tet_main();

int main(argc, argv)
int argc;
char **argv;
{
	return tet_main(argc, argv);
}

char *	tet_pname;
int	tet_thistest;

/* needed to stop lint complaining that it is declared but not defined */
int	tet_nosigreset;
