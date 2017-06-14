/*
 *      SCCS:  @(#)tc3.c	1.3 (96/10/03) 
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
static char sccsid[] = "@(#)tc3.c	1.3 (96/10/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tc3.c	1.3 96/10/03 TETware release 3.8
NAME:		tc3.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Ltd.
DATE CREATED:	October 1993

DESCRIPTION:
	demo test suite slave system test case 3

MODIFICATIONS:
	Geoff Clare, UniSoft Ltd., Oct 1996
	Use tet_remsync() instead of (deprecated) tet_sync().
	Added tp2.

************************************************************************/

#include <stdlib.h>
#include <stdio.h>
#include <tet_api.h>

#define TIMEOUT	10	/* sync time out */

int sys0[] = { 0 };	/* system IDs to sync with */

static void error(err, rptstr)
int err;	/* tet_errno value, or zero if N/A */
char *rptstr;	/* failure to report */
{
	char *errstr, *colonstr = ": ";
	char errbuf[20];

	if (err == 0)
		errstr = colonstr = "";
	else if (err > 0 && err < tet_nerr)
		errstr = tet_errlist[err];
	else {
		(void) sprintf(errbuf, "unknown tet_errno value %d", tet_errno);
		errstr = errbuf;
	}

	if (tet_printf("%s%s%s", rptstr, colonstr, errstr) < 0) {
		(void) fprintf(stderr, "tet_printf() failed: tet_errno %d\n",
			tet_errno);
		exit(EXIT_FAILURE);
	}
}

static void tp1()
{
	tet_infoline("This is tp1 in the third test case (tc3, slave)");

	(void) tet_printf("sync with master (sysid: %d)", *sys0);

	if (tet_remsync(101L, sys0, 1, TIMEOUT, TET_SV_YES,
				(struct tet_synmsg *)0) != 0) {
		error(tet_errno, "tet_remsync() failed on slave");
		tet_result(TET_UNRESOLVED);
	}
	else
		tet_result(TET_PASS);
}

static void tp2()
{
	int rescode = TET_UNRESOLVED;
	struct tet_synmsg msg;
	char rcvbuf[TET_SMMSGMAX];

	tet_infoline("This is tp2 in the third test case (tc3, slave)");

	(void) tet_printf("sync with master (sysid: %d) and receive data",
			  *sys0);

	msg.tsm_flags = TET_SMRCVMSG;
	msg.tsm_dlen = sizeof(rcvbuf);
	msg.tsm_data = rcvbuf;

	if (tet_remsync(201L, sys0, 1, TIMEOUT, TET_SV_YES, &msg) != 0)
		error(tet_errno, "tet_remsync() failed on slave");
	else if (msg.tsm_sysid == -1)
		error(0, "tet_remsync() set tsm_sysid to -1 on slave");
	else if (msg.tsm_flags & TET_SMTRUNC)
		error(0, "tet_remsync() set TET_SMTRUNC flag on slave");
	else if (msg.tsm_dlen <= 0)
		error(0, "tet_remsync() set tsm_dlen <= 0 on slave");
	else
	{
		(void) tet_printf("received message \"%.*s\" from master",
			msg.tsm_dlen, rcvbuf);
		rescode = TET_PASS;
	}

	tet_result(rescode);
}

void (*tet_startup)() = NULL, (*tet_cleanup)() = NULL;

struct tet_testlist tet_testlist[] = { {tp1,1}, {tp2,2}, {NULL,0} };

