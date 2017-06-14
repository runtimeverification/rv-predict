/*
 *	SCCS: @(#)jchild.c	1.1 (99/09/02)
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
static char sccsid[] = "@(#)jchild.c	1.1 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:		@(#)jchild.c	1.1 99/09/02 TETware release 3.8
NAME:		jchild.c
PRODUCT:	TETware
AUTHOR:		Matthew Hails, UniSoft Ltd.
DATE CREATED:	10 Aug 1999
SYNOPSIS:

	int tet_main(int argc, char **argv)

DESCRIPTION:
	Child process main function for the TETware Java API.

	tet_main() runs the Java child process controller.

************************************************************************/

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdarg.h>
#include "jni.h"
#include "tet_api.h"
#include "jetutil.h"

/* Local prototypes */
static int runTetMain(JNIEnv *env, jobject tc, jobject ts, int argc,
	char **argv);

/*
 * tet_main()
 *
 * Entry point for child test case. Called by child process controller.
 *
 *	argc	Number of strings in argv array.
 *	argv	Array of command line arguments.
 *
 * Returns result of child test case run, or 1 on error.
 */
int
tet_main(int argc, char **argv)
{
	JNIEnv *env;
	jobject tc;
	jobject ts;
	int rv;

	if (jet_enterTCI("tet_main", &env, &tc, &ts) != 0)
		return 1;

	/* Make Java call using JNI: tc.runTetMain(ts, argv) */
	rv = runTetMain(env, tc, ts, argc, argv);

	if (jet_leaveTCI(env, ts) != 0)
		return 1;

	return rv;
}

/*
 * runTetMain()
 *
 * Calls the "runTetMain()" method of a ChildTestCase object.
 *
 *	env	The JNI interface pointer.
 *	tc	The ChildTestCase object for the current test run.
 *	ts	The TestSession object for the current test run.
 *	argc	Number of elements of argv array.
 *	argv	Array of argument strings passed to child process.
 *
 * Returns the return value of the child test case, or non-zero if an error
 * occurred invoking the child test case.
 */
static int
runTetMain(JNIEnv *env, jobject tc, jobject ts, int argc, char **argv)
{
	jobjectArray args;
	jclass cls;
	jmethodID mid;
	int rv;

	/* Construct new String array object from argv */ 
	args = jet_mkJsarray(env, argv + 1, argc - 1);
	if (args == NULL)
		return 1;

	/* Get the class of the ChildTestCase object */
	cls = (*env)->GetObjectClass(env, tc);
	if (jet_checkJNI(env, "GetObjectClass", cls, JET_CLEAR) != 0)
		return 1;

	/* Call the runTetMain() method of the ChildTestCase object */
	mid = (*env)->GetMethodID(env, cls, "runTetMain",
		"(LTET/TestSession;[Ljava/lang/String;)I");
	if (jet_checkJNI(env, "GetMethodID", mid, JET_CLEAR) != 0)
		return 1;

	rv = (int) (*env)->CallIntMethod(env, tc, mid, ts, args);
	if (jet_checkJNI(env, "CallIntMethod", JET_JNIOK, JET_CLEAR) != 0)
		return 1;

	return rv;
}
