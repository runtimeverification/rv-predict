/*
 *	SCCS: @(#)tciface.c	1.3 (98/08/28)
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1997 X/Open Company Limited
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
static char sccsid[] = "@(#)tciface.c	1.3 (98/08/28) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tciface.c	1.3 98/08/28 TETware release 3.8
NAME:		tciface.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	June 1997

DESCRIPTION:
	An interface has been defined between the TCM control logic
	(which is generic) and the means by which test purposes are
	defined and called (which is specific to the C API).
	The C TCM uses the functions which constitute this defined
	interface when it counts and executes the user-supplied test
	purpose functions.


	The functions in this file are the defaults that are used when
	replacements are not supplied by the user.
	These functions implement an interface to the tet_testlist[]
	array that is specified for the base TET.

	If a user wishes to supply any of the interface functions they
	must all be supplied.


	The provision of the defined interface make for the following
	possibilities:

	1) The user may provide replacements for these functions, thus
	avoiding the need to specify all the test purposes in a static
	array at compile time.

	2) When an API is to be implemented in a language which can be
	linked with C, (i.e., functions in the other language may call,
	and be called from, C functions), it is possible for the new API
	to take advantage of all the functionality provided by the
	existing C API without having to implement it all from scratch.
	This will enable new APIs to be implemented much more easily and,
	at the same time, reduce the maintenance overhead associated with
	each new API.


	The following functions are defined by this interface:

	int tet_getmaxic(void)
	int tet_getminic(void)

	Returns the highest and lowest IC numbers defined in this test
	case.

	int tet_isdefic(int icnum)

	Returns 1 if the specified IC has been defined in this test
	case, 0 otherwise.

	int tet_gettpcount(int icnum)

	Returns the number of TPs that have been defined in the
	specified IC.

	int tet_gettestnum(int icnum, int tpnum)

	Returns the global test number (starting from 1) of the
	specified TP within the specified IC.
	This function is called by the TCM when the value is to be
	assigned to the global variable tet_thistest.

	void tet_invoketp(int icnum, int tpnum)

	This function is called by the TCM to invoke a particular TP.
	The purpose of the return value is reserved for future use.
	For now it just returns 0.


MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., January 1998
	Permit the iclist to include IC 0.

************************************************************************/

#include "dtmac.h"
#include "tet_api.h"


/*
**	tet_getmaxic(), tet_getminic() - return the highest and lowest
**		IC number defined in this test case
*/

int tet_getmaxic()
{
	register struct tet_testlist *tp;
	register int icmax;

	icmax = 0;
	for (tp = tet_testlist; tp->testfunc != TET_NULLFP; tp++)
		if (tp->icref > icmax)
			icmax = tp->icref;

	return(icmax);
}

int tet_getminic()
{
	register struct tet_testlist *tp;
	register int icmin;

	icmin = TET_MAX(tet_testlist[0].icref, 0);
	for (tp = tet_testlist; tp->testfunc != TET_NULLFP; tp++)
		if (tp->icref >= 0 && tp->icref < icmin)
			icmin = tp->icref;

	return(icmin);
}

/*
**	tet_isdefic() - return 1 if the specified IC has been defined
**		in this test case, 0 otherwise
*/

int tet_isdefic(icnum)
int icnum;
{
	register struct tet_testlist *tp;

	for (tp = tet_testlist; tp->testfunc != TET_NULLFP; tp++)
		if (tp->icref == icnum)
			return(1);

	return(0);
}

/*
**	tet_gettpcount() - return the number of TPs defined in the
**		specified IC
**
**	return 0 if the specified IC is not defined in this test case
*/

int tet_gettpcount(icnum)
int icnum;
{
	register struct tet_testlist *tp;
	register int tpcount;

	tpcount = 0;
	for (tp = tet_testlist; tp->testfunc != TET_NULLFP; tp++)
		if (tp->icref == icnum)
			tpcount++;

	return(tpcount);
}

/*
**	tet_gettestnum() - return the absolute test number (starting
**		at 1) for the specified TP within the specified IC
**
**	return 0 if the specified TP has not been defined in this test case
*/

int tet_gettestnum(icnum, tpnum)
int icnum, tpnum;
{
	register struct tet_testlist *tp;
	register int testnum;

	testnum = 0;
	for (tp = tet_testlist; tp->testfunc != TET_NULLFP; tp++) {
		testnum++;
		if (tp->icref == icnum && --tpnum == 0)
			return(testnum);
	}

	return(0);
}

/*
**	tet_invoketp() - invoke the specified TP within the specified IC
**
**	Always returns 0.
*/

int tet_invoketp(icnum, tpnum)
int icnum, tpnum;
{
	register struct tet_testlist *tp;

	for (tp = tet_testlist; tp->testfunc != TET_NULLFP; tp++)
		if (tp->icref == icnum && --tpnum == 0) {
			(*tp->testfunc)();
			break;
		}

	return(0);
}

