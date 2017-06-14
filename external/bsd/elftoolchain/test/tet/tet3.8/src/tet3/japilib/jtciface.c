/*
 *	SCCS: @(#)jtciface.c	1.1 (99/09/02)
 *
 *	UniSoft Ltd., London, England
 *
 * Copyright (c) 1999 The Open Group
 * All rights reserved.
 *
 * No part of this source code may be reproduced, stored in a retrieval
 * system, or transmitted, in any form or by any means, electronic,
 * mechanical, photocopying, recording or otherwise, except as stated
 * in the end-user licence agreement, without the prior permission of
 * the copyright owners.
 * A copy of the end-user licence agreement is contained in the file
 * Licence which accompanies this distribution.
 * 
 * Motif, OSF/1, UNIX and the "X" device are registered trademarks and
 * IT DialTone and The Open Group are trademarks of The Open Group in
 * the US and other countries.
 *
 * X/Open is a trademark of X/Open Company Limited in the UK and other
 * countries.
 *
 */

#ifndef lint
static char sccsid[] = "@(#)jtciface.c	1.1 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:		@(#)jtciface.c	1.1 99/09/02 TETware release 3.8
NAME:		jtciface.c
PRODUCT:	TETware
AUTHOR:		Matthew Hails, UniSoft Ltd.
DATE CREATED:	26 July 1999
SYNOPSIS:

	int tet_getmaxic(void)
	int tet_getminic(void)
	int tet_isdefic(int icnum)
	int tet_gettpcount(int icnum)
	int tet_gettestnum(int icnum, int tpnum)
	int tet_invoketp(int icnum, int tpnum)
	void (*tet_startup)()
	void (*tet_cleanup)()

DESCRIPTION:
	Test Case Interface functions for the TETware Java API. This
	includes the Dynamic Test Case Interface functions and the startup
	and cleanup function pointers.

	tet_getmaxic() gets the highest invocable component number for
	this test case.
 
	tet_getminic() gets the lowest invocable component number for this
	test case.

	tet_isdefic() determines whether or not a given invocable
	component number is defined for this test case. It returns 1 if
	the specified IC is defined in the test case, 0 otherwise.

	tet_gettpcount() gets the number of test purposes defined for a
	given invocable component. It returns the number of test purposes
	which have been defined in the specified IC, or 0 if the
	specified IC has not been defined in this test case.

	tet_gettestnum() gets the absolute test number for a given test
	purpose. It returns the absolute test number, or 0 if the given
	test purpose has not been defined in this test case.

	tet_invoketp() invokes a test purpose method of the current
	Java TestCase object, via the current TestSession object. It
	always returns 0.

	tet_startup and tet_cleanup point to startup and cleanup routines
	which invoke the startup and cleanup of the current Java TestCase
	object, via the current TestSession object.

************************************************************************/

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdarg.h>
#include <ctype.h>
#include "jni.h"
#include "tet_api.h"
#include "jetutil.h"

/* Local prototypes */
static void startup(void);
static void cleanup(void);
static int doDTCI(char *op, int icnum, int tpnum);
static int doTCOp(JNIEnv *env, jobject tc, jobject ts, char *op, int icnum,
	int tpnum);
static void strToLower(char *src, char *dest, int len);

/* ================== Test Case Interface Implementation =================== */

/*
 * tet_startup, tet_cleanup
 *
 * See startup(), cleanup().
 */
void (*tet_startup)() = startup;
void (*tet_cleanup)() = cleanup;

/*
 * tet_getmaxic()
 *
 * Get the highest invocable component number for this test case.
 *
 * Returns the highest IC number.
 */
int
tet_getmaxic(void)
{
	return doDTCI("TET_GETMAXIC", -1, -1);
}

/*
 * tet_getminic()
 *
 * Get the lowest invocable component number for this test case.
 *
 * Returns the lowest IC number.
 */
int
tet_getminic(void)
{
	return doDTCI("TET_GETMINIC", -1, -1);
}

/*
 * tet_isdefic()
 *
 * Determine whether or not a given invocable component number is defined for
 * this test case.
 *
 * 	icnum	number of the IC of interest.
 *
 * Returns 1 if the specified IC is defined in the test case, 0 otherwise.
 */
int
tet_isdefic(int icnum)
{
	return doDTCI("TET_ISDEFIC", icnum, -1);
}

/*
 * tet_gettpcount()
 *
 * Get the number of test purposes defined for a given invocable component.
 *
 * 	icnum	number of the IC of interest.
 *
 * Returns the number of test purposes which have been defined in the specified
 * IC, or 0 if the specified IC has not been defined in this test case.
 */
int
tet_gettpcount(int icnum)
{
	return doDTCI("TET_GETTPCOUNT", icnum, -1);
}

/*
 * tet_gettestnum()
 *
 * Get the absolute test number for a given test purpose.
 *
 * 	icnum	number of the invocable component.
 * 	tpnum	number of the test purpose within the scope of the IC specified
 * 		by icnum.
 *
 * Returns the absolute test number for the specified test purpose, or 0 if
 * this test purpose has not been defined in the test case.
 */
int
tet_gettestnum(int icnum, int tpnum)
{
	return doDTCI("TET_GETTESTNUM", icnum, tpnum);
}

/*
 * tet_invoketp()
 *
 * Invoke a test purpose method of the current Java TestSession object.
 *
 * 	icnum	number of the invocable component.
 * 	tpnum	number of the test purpose within the scope of the IC specified
 * 		by icnum.
 *
 * Returns 0 always.
 */
int
tet_invoketp(int icnum, int tpnum)
{
	return doDTCI("TET_INVOKETP", icnum, tpnum);
}

/* ========================== Support Routines ============================ */

/*
 * startup()
 *
 * Startup function. Calls the startup() method of the current TestCase object.
 */
static void
startup(void)
{
	(void) doDTCI("TET_STARTUP", -1, -1);
}

/*
 * cleanup()
 *
 * Cleanup function. Calls the cleanup() method of the current TestCase object.
 */
static void
cleanup(void)
{
	(void) doDTCI("TET_CLEANUP", -1, -1);
}

/*
 * doDTCI()
 *
 * Perform a dynamic test case interface operation.
 *
 *	op	Test case operation to perform. Should be the name of the
 *		class field defined in the test session class.
 *	icnum	Number of the required invocable component, if applicable.
 *	tpnum	Number of the required test purpose, if applicable.
 *
 * Returns the result of the test case operation, or 0 if an error occurred.
 */
static int
doDTCI(char *op, int icnum, int tpnum)
{
	char funcname[sizeof("Longerthanalltet_funcs")];
	JNIEnv *env;
	jobject tc;
	jobject ts;
	int rv;

	strToLower(op, funcname, sizeof(funcname));

	if (jet_enterTCI(funcname, &env, &tc, &ts) != 0)
		return 0;

	/* Use JNI to call the "doTCOp()" method on the TestCase object */
	rv = doTCOp(env, tc, ts, op, icnum, tpnum);

	if (jet_leaveTCI(env, ts) != 0)
		return 0;

	return rv;
}

/*
 * doTCOp()
 *
 * Call the "doTCOp()" method of a SimpleTestCase object.
 *
 *	env	The JNI interface pointer.
 *	tc	The SimpleTestCase object for the current test run.
 *	ts	The TestSession object for the current test run.
 *	op	The test case operation to perform.
 *	icnum	The number of the required invocable component, if applicable.
 *	tpnum	The required test purpose number, if applicable.
 *
 * Returns the result of the doTCOp() call, or 0 if an error occurred. All
 * errors are logged to the execution results file and all exceptions cleared
 * and logged.
 */
static int
doTCOp(JNIEnv *env, jobject tc, jobject ts, char *op, int icnum, int tpnum)
{
	jclass cls;
	jmethodID mid;
	jfieldID fid;
	jint opval;
	int rv;

	/* Get the class object of SimpleTestCase */
	cls = (*env)->FindClass(env, "TET/SimpleTestCase");
	if (jet_checkJNI(env, "FindClass", cls, JET_CLEAR) != 0)
		return 0;

	/* Get the value of the op argument */
	fid = (*env)->GetStaticFieldID(env, cls, op, "I");
	if (jet_checkJNI(env, "GetStaticFieldID", fid, JET_CLEAR) != 0)
		return 0;

	opval = (*env)->GetStaticIntField(env, cls, fid);
	if (jet_checkJNI(env, "GetStaticIntField", JET_JNIOK, JET_CLEAR) != 0)
		return 0;

	/* Call the doTCOp() on the test case object */
	cls = (*env)->GetObjectClass(env, tc);
	if (jet_checkJNI(env, "GetObjectClass", cls, JET_CLEAR) != 0)
		return 0;

	mid = (*env)->GetMethodID(env, cls, "doTCOp",
		"(LTET/TestSession;III)I");
	if (jet_checkJNI(env, "GetMethodID", mid, JET_CLEAR) != 0)
		return 0;

	rv = (int) (*env)->CallIntMethod(env, tc, mid, ts, opval, (jint)icnum,
		(jint)tpnum);
	if (jet_checkJNI(env, "CallIntMethod", JET_JNIOK, JET_CLEAR) != 0)
		return 0;

	return rv;
}

/*
 * strToLower()
 *
 * Convert a string to lower case. Destination string is always null-terminated
 * (unless the destination buffer size is 0) but will be truncated if it is
 * shorter than the source string.
 *
 *	src	Source string.
 *	dest	Destination buffer.
 *	len	Length of destination buffer.
 */
static void
strToLower(char *src, char *dest, int len)
{
	char *s;
	char *d;

	for (s = src, d = dest; *s != '\0' && d < dest + len - 1; s++, d++)
		*d = tolower((unsigned char)*s);

	if (len > 0)
		*d = '\0';
}
