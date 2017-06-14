/*
 *	SCCS: @(#)SimpleTestCase.c	1.1 (99/09/02)
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
static char sccsid[] = "@(#)SimpleTestCase.c	1.1 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:		@(#)SimpleTestCase.c	1.1 99/09/02 TETware release 3.8
NAME:		SimpleTestCase.c
PRODUCT:	TETware
AUTHOR:		Matthew Hails, UniSoft Ltd.
DATE CREATED:	26 July 1999
SYNOPSIS:

	JNIEXPORT void JNICALL Java_TET_SimpleTestCase_tcmMain(
		JNIEnv *env, jobject obj, jobject ts, jobjectArray args)

DESCRIPTION:
	Implementation of native methods for the Java class
	TET.SimpleTestCase.

************************************************************************/

#include <stdlib.h>
#include <stdio.h>
#include "jni.h"
#include "tet_api.h"
#include "jetutil.h"

/*
 * Java_TET_SimpleTestCase_tcmMain()
 *
 * Native method implementation.
 *
 * 	Class:		TET_SimpleTestCase
 * 	Method:		tcmMain
 * 	Signature:	(LTET/TestSession;[Ljava/lang/String;)V
 *
 * Pass control to the TCM. Calls the C function tet_tcm_main(). Control
 * re-enters Java code in doTCOp(). This function does not return.
 *
 *	env	The JNI interface pointer.
 *	obj	SimpleTestCase object for which this method was	invoked.
 *	ts	TestSession object for this test run.
 *	args	Command line arguments passed from parent.
 */
JNIEXPORT void JNICALL
Java_TET_SimpleTestCase_tcmMain(JNIEnv *env, jobject obj, jobject ts,
	jobjectArray args)
{
	int argc;
	char **argv;

	/* Construct argument list for tet_tcm_main() */
	if (jet_mkCsarray(env, args, &argv, &argc) != 0)
	{
		jet_logmsg("Error creating arg list for tet_tcm_main()");
		exit(EXIT_FAILURE);
	}

	/* Save current JNI pointers for test case interface to pick up */
	jet_storeJenv(env, obj, ts);

	/* Pass control to the C TCM */
	tet_tcm_main(argc, argv);

	/* tet_tcm_main() should not return, but in case it does, exit here */
	exit(EXIT_FAILURE);
}
