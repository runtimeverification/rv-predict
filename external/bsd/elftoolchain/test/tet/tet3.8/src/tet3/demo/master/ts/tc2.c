/*
 *      SCCS:  @(#)tc2.c	1.3 (96/10/11) 
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
static char sccsid[] = "@(#)tc2.c	1.3 (96/10/11) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tc2.c	1.3 96/10/11 TETware release 3.8
NAME:		tc2.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Ltd.
DATE CREATED:	October 1993

DESCRIPTION:
	demo test suite master system test case 2

MODIFICATIONS:

************************************************************************/

#include <stdlib.h>
#include <tet_api.h>

void (*tet_startup)() = NULL, (*tet_cleanup)() = NULL;
void tp1();

struct tet_testlist tet_testlist[] = { {tp1,1}, {NULL,0} };

void tp1()
{
	static char *lines[] = {
		"This is the second test case (tc2, master).",
		"",
		"The master part of this test purpose reports PASS",
		"but the slave part of this test purpose reports FAIL",
		"so the consolidated result of the test purpose is FAIL.",
		"",
		"The lines in this block of text are printed by a single",
		"call to tet_minfoline() in the master part of the test",
		"purpose so output from the slave part of the test purpose",
		"won't be mixed up with these lines."
	};
	static int Nlines = sizeof lines / sizeof lines[0];

	tet_minfoline(lines, Nlines);
	tet_result(TET_PASS);
}

