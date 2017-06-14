/*
 *	SCCS: @(#)TestSession.c	1.2 (05/07/08)
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
static char sccsid[] = "@(#)TestSession.c	1.2 (05/07/08) TETware release 3.8";
#endif

/************************************************************************

SCCS:		@(#)TestSession.c	1.2 05/07/08 TETware release 3.8
NAME:		TestSession.c
PRODUCT:	TETware
AUTHOR:		Matthew Hails, UniSoft Ltd. (Based on JETpack 1.02 source)
DATE CREATED:	26 July 1999
SYNOPSIS:

	JNIEXPORT jint JNICALL Java_TET_TestSession_tet_1thistest(
		JNIEnv *env,
		jobject obj)
	JNIEXPORT jobjectArray JNICALL Java_TET_TestSession_getErrList(
		JNIEnv *env, jobject obj)
	JNIEXPORT void JNICALL Java_TET_TestSession_tet_1setcontext(
		JNIEnv *env, jobject obj)
	JNIEXPORT void JNICALL Java_TET_TestSession_tet_1infoline(
		JNIEnv *env, jobject obj, jstring line)
	JNIEXPORT void JNICALL Java_TET_TestSession_tet_1minfoline(
		JNIEnv *env, jobject obj, jobjectArray lines)
	JNIEXPORT void JNICALL Java_TET_TestSession_tet_1result(JNIEnv *env,
		jobject obj, jint result)
	JNIEXPORT void JNICALL Java_TET_TestSession_tet_1delete(JNIEnv *env,
		jobject obj, jint testno, jstring reason)
	JNIEXPORT jstring JNICALL Java_TET_TestSession_tet_1reason(
		JNIEnv *env, jobject obj, jint testno)
	JNIEXPORT jstring JNICALL Java_TET_TestSession_tet_1getvar(
		JNIEnv *env, jobject obj, jstring name)
	JNIEXPORT jlong JNICALL Java_TET_TestSession_spawn(JNIEnv *env,
		jobject obj, jstring file, jobjectArray argv,
		jobjectArray envp)
	JNIEXPORT jint JNICALL Java_TET_TestSession_tet_1wait(JNIEnv *env,
		jobject obj, jlong pid)
	JNIEXPORT void JNICALL Java_TET_TestSession_tet_1kill(JNIEnv *env,
		jobject obj, jlong pid, jint sig)
	JNIEXPORT void JNICALL Java_TET_TestSession_tet_1remsync(JNIEnv *env,
		jobject obj, jlong syncptno, jintArray sysnames,
		jint waittime, jint vote, jobject msg)
	JNIEXPORT jintArray JNICALL Java_TET_TestSession_tet_1remgetlist(
		JNIEnv *env, jobject obj)
	JNIEXPORT jint JNICALL Java_TET_TestSession_tet_1remgetsys(
		JNIEnv *env, jobject obj)
	JNIEXPORT jstring JNICALL Java_TET_TestSession_getsysname(
		JNIEnv *env, jobject obj, jint sysid)
	JNIEXPORT jlong JNICALL Java_TET_TestSession_remtime(JNIEnv *env,
		jobject obj, jint sysid)
	JNIEXPORT void JNICALL Java_TET_TestSession_tet_1exit(JNIEnv *env,
		jobject obj, jint status)
	JNIEXPORT void JNICALL Java_TET_TestSession_tet_1logoff(JNIEnv *env,
		jobject obj)
	JNIEXPORT jstring JNICALL Java_TET_TestSession_getenv(JNIEnv *env,
		jobject obj, jstring name)
	JNIEXPORT jboolean JNICALL Java_TET_TestSession_getLiteIndicator(
		JNIEnv *env, jobject obj)
	JNIEXPORT void JNICALL Java_TET_TestSession_tet_1merror(JNIEnv *env,
		jobject obj, jint errnum, jobjectArray msgs)

DESCRIPTION:
	Implementation of native methods for the Java class
	TET.TestSession. This is part of the Java API for TETware.

	N.B. all native methods in this file which call functions or set
	variables from the TETware C API, must be declared synchronized.
	This will ensure that these functions are only executed from a
	single thread.

	See TestSession.java for more details of the methods and also
	for implementation details such as synchronization.

MODIFICATIONS:
	Geoff Clare, The Open Group, July 2005
	Added <string.h> (for memcpy)

************************************************************************/

#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <limits.h>
#include "jni.h"
#include "tet_api.h"
#include "jetutil.h"

#ifdef _WIN32		/* -START-WIN32-CUT- */
# define ENVIRON	_environ
#else /* _WIN32 */	/* -END-WIN32-CUT- */
# define ENVIRON	environ
  extern char **environ;
#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */

/* Local prototypes */
#ifndef TET_LITE /* -START-LITE-CUT- */
static void doremsync(JNIEnv *env, jobject obj, jlong syncptno,
	jintArray sysnames, jint waittime, jint vote, jobject msg);
static void syncreport(long syncptno, struct tet_syncstat *statp, int nstat);
#endif /* -LITE-CUT-LINE- */

/*
 * Java_TET_TestSession_tet_1thistest()
 *
 * Native method implementation.
 *
 *	Class:		TET.TestSession
 * 	Method:		tet_thistest
 * 	Signature:	()I
 *
 * Retrieves the absolute test number for the currently executing test purpose.
 *
 *	env	The JNI interface pointer.
 *	obj	TestSession object for which this method was invoked.
 *
 * Returns the current test number.
 */
JNIEXPORT jint JNICALL
Java_TET_TestSession_tet_1thistest(JNIEnv *env, jobject obj)
{
	/* N.B. we don't synchronize or use jet_enterAPI()/jet_leaveAPI(),
	 * since we are only reading a global TET variable which has nothing
	 * to do with block or sequence.
	 */
	return (jint)tet_thistest;
}

/*
 * Java_TET_TestSession_getErrList()
 *
 * Native method implementation.
 *
 * 	Class:		TET.TestSession
 * 	Method:		getErrList
 * 	Signature:	()[Ljava/lang/String;
 *
 * Retrieves the list of error strings which describe each value of tet_errno.
 *
 *	env	The JNI interface pointer.
 *	obj	TestSession object for which this method was invoked.
 *
 * Returns the list of error strings.
 */
JNIEXPORT jobjectArray JNICALL
Java_TET_TestSession_getErrList(JNIEnv *env, jobject obj)
{
	/* N.B. we don't synchronize or use jet_enterAPI()/jet_leaveAPI(),
	 * since we are only reading global TET variables, which should be
	 * constant in any case.
	 */
	return jet_mkJsarray(env, tet_errlist, tet_nerr);
}

/*
 * Java_TET_TestSession_tet_1setcontext()
 *
 * Native method implementation.
 *
 * 	Class:		TET.TestSession
 * 	Method:		tet_setcontext
 *	Signature:	()V
 *
 * Sets the current context to the value of the current process ID and resets
 * the sequence number for the current thread to 1.
 *
 *	env	The JNI interface pointer.
 *	obj	TestSession object for which this method was invoked.
 */
JNIEXPORT void JNICALL
Java_TET_TestSession_tet_1setcontext(JNIEnv *env, jobject obj)
{
	long block;

	if (jet_enterAPI(env, obj) != 0)
		return;

	/* Since Java is always threaded and we're not using the thread-safe
	 * C API, we save and restore the current block number so that the
	 * current thread keeps its block number and hence different threads
	 * are still distinguishable.
	 */
	block = tet_block;
	tet_setcontext();
	tet_block = block;

	jet_leaveAPI(env, obj);
}

/*
 * Java_TET_TestSession_tet_1infoline()
 *
 * Native method implementation.
 *
 * 	Class:		TET.TestSession
 * 	Method:		tet_infoline
 *	Signature:	(Ljava/lang/String;)V
 *
 * Calls the TETware C API function tet_infoline().
 *
 *	env	The JNI interface pointer.
 *	obj	TestSession object for which this method was invoked.
 *	line	Line to print to the results file.
 */
JNIEXPORT void JNICALL
Java_TET_TestSession_tet_1infoline(JNIEnv *env, jobject obj, jstring line)
{
	char *msg;

	msg = jet_mkCstring(env, line);
	if (msg != NULL)
	{
		if (jet_enterAPI(env, obj) != 0)
			return;

		tet_infoline(msg);

		jet_leaveAPI(env, obj);

		free(msg);
	}
}

/*
 * Java_TET_TestSession_tet_1minfoline()
 *
 * Native method implementation.
 *
 *	Class:		TET.TestSession
 * 	Method:		tet_minfoline
 * 	Signature:	([Ljava/lang/String;)V
 *
 * Calls the TETware C API function tet_minfoline().
 *
 *	env	The JNI interface pointer.
 *	obj	TestSession object for which this method was invoked.
 *	lines	Group of lines to print to the results file.
 */
JNIEXPORT void JNICALL
Java_TET_TestSession_tet_1minfoline(JNIEnv *env, jobject obj,
	jobjectArray lines)
{
	int nmsg;
	char **msgs;
	int rv;

	/* Convert array of Java String objects into array of C strings */
	if (jet_mkCsarray(env, lines, &msgs, &nmsg) != 0)
		return;

	/* Call the C API function tet_minfoline() */
	if (jet_enterAPI(env, obj) != 0)
		return;

	rv = tet_minfoline(msgs, nmsg);

	jet_leaveAPI(env, obj);

	/* If tet_minfoline() failed, throw a new TetException object */
	if (rv != 0)
		jet_throwtet(env, tet_errno, NULL, 0L, NULL);

	/* Free memory allocated for list of lines */
	jet_freeCsarray(msgs);
}

/*
 * Java_TET_TestSession_tet_1result()
 *
 * Native method implementation.
 *
 * 	Class:		TET.TestSession
 * 	Method:		tet_result
 * 	Signature:	(I)V
 *
 * Calls the TETware C API function tet_result().
 *
 *	env	The JNI interface pointer.
 *	obj	TestSession object for which this method was invoked.
 *	result	The result code.
 */
JNIEXPORT void JNICALL
Java_TET_TestSession_tet_1result(JNIEnv *env, jobject obj, jint result)
{
	if (jet_enterAPI(env, obj) != 0)
		return;

	tet_result((int)result);

	jet_leaveAPI(env, obj);
}

/*
 * Java_TET_TestSession_tet_1delete()
 *
 * Native method implementation.
 *
 * 	Class:		TET.TestSession
 * 	Method:		tet_delete
 * 	Signature:	(ILjava/lang/String;)V
 *
 * Calls the TETware C API function tet_delete().
 *
 *	env	The JNI interface pointer.
 *	obj	TestSession object for which this method was invoked.
 *	testno	Absolute test number of the test purpose to be cancelled. 
 */
JNIEXPORT void JNICALL
Java_TET_TestSession_tet_1delete(JNIEnv *env, jobject obj, jint testno,
	jstring reason)
{
	char *oldreason;
	char *newreason;

	/* N.B. The reason strings are allocated and freed using malloc() and
	 * free(). This relies on the fact that tet_delete() is never called
	 * from inside the C API.
	 */

	/* Convert the Java String object giving the cancellation reason to a C
	 * string we can pass to the C API call tet_delete()
	 */
	if (reason == NULL)
	{
		newreason = NULL;
	}
	else
	{
		newreason = jet_mkCstring(env, reason);
		if (newreason == NULL)
			return;
	}

	if (jet_enterAPI(env, obj) != 0)
	{
		if (newreason != NULL)
			free(newreason);

		return;
	}

	/* Retrieve existing reason and set new one */
	oldreason = tet_reason((int)testno);
	tet_delete((int)testno, newreason);

	jet_leaveAPI(env, obj);

	/* Free old reason if one was present */
	if (oldreason != NULL)
		free(oldreason);
}

/*
 * Java_TET_TestSession_tet_1reason()
 *
 * Native method implementation.
 *
 * 	Class:		TET.TestSession
 * 	Method:		tet_reason
 * 	Signature:	(I)Ljava/lang/String;
 *
 * Calls the TETware C API function tet_reason().
 *
 *	env	The JNI interface pointer.
 *	obj	TestSession object for which this method was invoked.
 *	testno	Absolute test number of the test purpose.
 *
 * Returns a string describing the reason for test purpose cancellation, or
 * NULL if this test purpose is not marked as cancelled.
 */
JNIEXPORT jstring JNICALL
Java_TET_TestSession_tet_1reason(JNIEnv *env, jobject obj, jint testno)
{
	char *reason;

	if (jet_enterAPI(env, obj) != 0)
		return NULL;

	reason = tet_reason((int)testno);

	if (jet_leaveAPI(env, obj) != 0)
		return NULL;

	if (reason == NULL)
		return NULL;

	return jet_mkJstring(env, reason);
}

/*
 * Java_TET_TestSession_tet_1getvar()
 *
 * Native method implementation.
 *
 * 	Class:		TET.TestSession
 * 	Method:		tet_getvar
 * 	Signature:	(Ljava/lang/String;)Ljava/lang/String;
 *
 * Calls the TETware C API function tet_getvar().
 *
 *	env	The JNI interface pointer.
 *	obj	TestSession object for which this method was invoked.
 *	name	Name of the configuration variable.
 *
 * Returns the value of the specified configuration variable. If the variable
 * specified is defined but has no setting, a String object of zero length is
 * returned. If the variable is undefined, NULL is returned.
 */
JNIEXPORT jstring JNICALL
Java_TET_TestSession_tet_1getvar(JNIEnv *env, jobject obj, jstring name)
{
	char *nm;
	char *val;

	nm = jet_mkCstring(env, name);
	if (nm == NULL)
		return NULL;

	if (jet_enterAPI(env, obj) != 0)
	{
		free(nm);
		return NULL;
	}

	val = tet_getvar(nm);
	free(nm);

	if (jet_leaveAPI(env, obj) != 0)
		return NULL;

	if (val == NULL)
		return NULL;

	return jet_mkJstring(env, val);
}

/*
 * Java_TET_TestSession_spawn()
 *
 * Native method implementation.
 *
 * 	Class:		TET.TestSession
 * 	Method:		spawn
 * 	Signature:
 *		(Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)J
 *
 * Calls the TETware C API function tet_spawn().
 *
 *	env	The JNI interface pointer.
 *	obj	TestSession object for which this method was invoked.
 *	file	Path of the subprogram to execute.
 *	argv	Argument array to pass to new process.
 *	envp	Environment data to pass to new process. If this array has
 *		length 0, then the current environment is used.
 *
 * Returns the process ID of the newly created process.
 */
JNIEXPORT jlong JNICALL
Java_TET_TestSession_spawn(JNIEnv *env, jobject obj, jstring file,
	jobjectArray argv, jobjectArray envp)
{
	char *newfile;
	char **newargv;
	jsize elen;
	char **newenvp;
	pid_t pid;

	/* Allocate memory for argument strings and convert from Unicode */
	newfile = jet_mkCstring(env, file);
	if (newfile == NULL)
		return -1;

	if (jet_mkCsarray(env, argv, &newargv, NULL) != 0)
	{
		free(newfile);
		return -1;
	}

	/* A zero-length environment array means use the current environment */
	elen = (*env)->GetArrayLength(env, envp);
	if (jet_checkJNI(env, "GetArrayLength", JET_JNIOK, JET_THROW) != 0)
	{
		jet_freeCsarray(newargv);
		free(newfile);
		return -1;
	}

	if (elen == 0)
	{
		newenvp = ENVIRON;
	}
	else
	{
		if (jet_mkCsarray(env, envp, &newenvp, NULL) != 0)
		{
			jet_freeCsarray(newargv);
			free(newfile);
			return -1;
		}
	}

	/* Call the tet_spawn() API function */
	if (jet_enterAPI(env, obj) != 0)
		return -1;

	pid = tet_spawn(newfile, newargv, newenvp);

	jet_leaveAPI(env, obj);

	/* Free the memory allocated for the argument strings */
	free(newfile);
	jet_freeCsarray(newargv);

	if (elen != 0)
		jet_freeCsarray(newenvp);

	/* Check the return from tet_spawn() and throw a TetException if it
	 * failed.
	 */
	if (pid == (pid_t)-1)
		jet_throwtet(env, tet_errno, NULL, 0L, NULL);

	return (jlong)pid;
}

/*
 * Java_TET_TestSession_tet_1wait()
 *
 * Native method implementation.
 *
 * 	Class:		TET.TestSession
 * 	Method:		jet_wait
 * 	Signature:	(J)I
 *
 * Calls the TETware C API function tet_wait().
 *
 *	env	The JNI interface pointer.
 *	obj	TestSession object for which this method was invoked.
 *	pid	Process ID of the process to wait for.
 *
 * Returns the exit status of the process.
 */
JNIEXPORT jint JNICALL
Java_TET_TestSession_tet_1wait(JNIEnv *env, jobject obj, jlong pid)
{
	int rv;
	int status;

	if (jet_enterAPI(env, obj) != 0)
		return -1;

	rv = tet_wait((pid_t)pid, &status);

	if (jet_leaveAPI(env, obj) != 0)
		return -1;

	if (rv == -1)
		jet_throwtet(env, tet_errno, NULL, 0L, NULL);

	return status;
}

/*
 * Java_TET_TestSession_tet_1kill()
 *
 * Native method implementation.
 *
 * 	Class:		TET.TestSession
 * 	Method:		tet_kill
 * 	Signature:	(JI)V
 *
 * Calls the TETware C API function tet_kill().
 *
 *	env	The JNI interface pointer.
 *	obj	TestSession object for which this method was invoked.
 *	pid	Process ID of the victim process.
 *	sig	Number of the signal to send. Note that this is ignored on a
 *		Windows NT system.
 */
JNIEXPORT void JNICALL
Java_TET_TestSession_tet_1kill(JNIEnv *env, jobject obj, jlong pid, jint sig)
{
	int rv;

	if (jet_enterAPI(env, obj) != 0)
		return;

	rv = tet_kill((pid_t)pid, (int)sig);

	if (jet_leaveAPI(env, obj) != 0)
		return;

	if (rv != 0)
		jet_throwtet(env, tet_errno, NULL, 0L, NULL);
}

/*
 * Java_TET_TestSession_tet_1remsync()
 *
 * Native method implementation.
 *
 * 	Class:		TET.TestSession
 * 	Method:		tet_remsync
 * 	Signature:	(J[IIILTET/SyncMessage;)V
 *
 * In Distributed TETware, it calls the TETware C API function tet_remsync().
 * In TETware-Lite, throws a new TetException.
 *
 *	env		The JNI interface pointer.
 *	obj		TestSession object for which this method was
 *			invoked.
 *			wishes to synchronise. If syncptno is zero, a
 *			successful call to tet_remsync() returns as soon as all
 *			participating systems have synchronised to the next
 *			sync point.
 *	sysnames	List of IDs of the other systems with which to
 *			synchronise. The system ID of the calling system is
 *			ignored if it appears in the list.
 *	waittime	The number of seconds that may elapse between
 *			synchronisation requests from other participating
 *			systems before the calling system times out
 *	vote		How the calling system wishes to vote in the
 *			synchronisation event.
 *	msg		Used to exchange sync message data with other
 *			participating systems.
 */
JNIEXPORT void JNICALL
Java_TET_TestSession_tet_1remsync(JNIEnv *env, jobject obj, jlong syncptno,
	jintArray sysnames, jint waittime, jint vote, jobject msg)
{
#ifdef TET_LITE	/* -LITE-CUT-LINE- */
	jet_errlite(env, "tet_remsync");
#else		/* -START-LITE-CUT- */
	doremsync(env, obj, syncptno, sysnames, waittime, vote, msg);
#endif		/* -END-LITE-CUT- */
}

/*
 * Java_TET_TestSession_tet_1remgetlist()
 *
 * Native method implementation.
 *
 * 	Class:		TET.TestSession
 * 	Method:		tet_remgetlist
 * 	Signature:	()[I
 *
 * Calls the TETware C API function tet_remgetlist().
 *
 *	env	The JNI interface pointer.
 *	obj	TestSession object for which this method was invoked.
 *
 * Returns a list of IDs of the other systems participating in the test case.
 * Returns null in TETware-Lite or for a test case in which no other systems
 * are participating.
 */
JNIEXPORT jintArray JNICALL
Java_TET_TestSession_tet_1remgetlist(JNIEnv *env, jobject obj)
{
	int *names;
	int rv;

	if (jet_enterAPI(env, obj) != 0)
		return NULL;

	rv = tet_remgetlist(&names);

	if (jet_leaveAPI(env, obj) != 0)
		return NULL;

	if (rv <= 0)
		return NULL;

	/* Create a new Java int array object */
	return jet_mkJintarray(env, names, rv);
}

/*
 * Java_TET_TestSession_tet_1remgetsys()
 *
 * Native method implementation.
 *
 * 	Class:		TET.TestSession
 * 	Method:		tet_remgetsys
 * 	Signature:	()I
 *
 * Calls the TETware C API function tet_remgetsys().
 *
 *	env	The JNI interface pointer.
 *	obj	TestSession object for which this method was invoked.
 *
 * Returns the system ID of the current system.
 */
JNIEXPORT jint JNICALL
Java_TET_TestSession_tet_1remgetsys(JNIEnv *env, jobject obj)
{
	int id;

	if (jet_enterAPI(env, obj) != 0)
		return -1;

	id = tet_remgetsys();

	if (jet_leaveAPI(env, obj) != 0)
		return -1;

	return (jint)id;
}

/*
 * Java_TET_TestSession_getsysname()
 *
 * Native method implementation.
 *
 * 	Class:		TET.TestSession
 * 	Method:		getsysname
 * 	Signature:	(I)Ljava/lang/String;
 *
 * In Distributed TETware, it calls the TETware C API function
 * tet_getsysbyid(). In TETware-Lite, throws a new TetException.
 *
 *	env	The JNI interface pointer.
 *	obj	TestSession object for which this method was invoked.
 *	sysid	ID of the system of interest.
 *
 * Returns the name of the required system.
 */
JNIEXPORT jstring JNICALL
Java_TET_TestSession_getsysname(JNIEnv *env, jobject obj, jint sysid)
{
#ifdef TET_LITE	/* -LITE-CUT-LINE- */
	jet_errlite(env, "tet_getsysbyid");
	return NULL;
#else		/* -START-LITE-CUT- */
	int rv;
	struct tet_sysent ent;

	if (jet_enterAPI(env, obj) != 0)
		return NULL;

	rv = tet_getsysbyid((int)sysid, &ent);

	if (jet_leaveAPI(env, obj) != 0)
		return NULL;

	if (rv != 0)
	{
		jet_throwtet(env, tet_errno, NULL, 0L, NULL);
		return NULL;
	}

	return jet_mkJstring(env, ent.ts_name);
#endif		/* -END-LITE-CUT- */
}

/*
 * Java_TET_TestSession_remtime()
 *
 * Native method implementation.
 *
 * 	Class:		TET.TestSession
 * 	Method:		remtime
 * 	Signature:	(I)J
 *
 * In Distributed TETware, it calls the TETware C API function tet_remtime().
 * In TETware-Lite, throws a new TetException.
 *
 *	env	The JNI interface pointer.
 *	obj	TestSession object for which this method was invoked.
 *	sysid	ID of the system of interest.
 *
 * Returns the system time on the system specified by sysid.
 */
JNIEXPORT jlong JNICALL
Java_TET_TestSession_remtime(JNIEnv *env, jobject obj, jint sysid)
{
#ifdef TET_LITE	/* -LITE-CUT-LINE- */
	jet_errlite(env, "tet_remtime");
	return -1;
#else		/* -START-LITE-CUT- */
	time_t systime;
	int rv;

	if (jet_enterAPI(env, obj) != 0)
		return -1;

	rv = tet_remtime((int)sysid, &systime);

	if (jet_leaveAPI(env, obj) != 0)
		return -1;

	if (rv != 0)
	{
		jet_throwtet(env, tet_errno, NULL, 0L, NULL);
		return -1;
	}

	return (jlong)systime;
#endif		/* -END-LITE-CUT- */
}

/*
 * Java_TET_TestSession_tet_1exit()
 *
 * Native method implementation.
 *
 * 	Class:		TET.TestSession
 * 	Method:		tet_exit
 * 	Signature:	(I)V
 *
 * Calls the TETware C API function tet_exit().
 *
 *	env	The JNI interface pointer.
 *	obj	TestSession object for which this method was invoked.
 *	status	Exit status.	
 */
JNIEXPORT void JNICALL
Java_TET_TestSession_tet_1exit(JNIEnv *env, jobject obj, jint status)
{
	if (jet_enterAPI(env, obj) != 0)
		return;

	tet_exit((int)status);

	jet_leaveAPI(env, obj);
}

/*
 * Java_TET_TestSession_tet_1logoff()
 *
 * Native method implementation.
 *
 * 	Class:		TET.TestSession
 * 	Method:		tet_logoff
 * 	Signature:	()V
 *
 * Calls the TETware C API function tet_logoff().
 *
 *	env	The JNI interface pointer.
 *	obj	TestSession object for which this method was invoked.
 */
JNIEXPORT void JNICALL
Java_TET_TestSession_tet_1logoff(JNIEnv *env, jobject obj)
{
	if (jet_enterAPI(env, obj) != 0)
		return;

	tet_logoff();

	jet_leaveAPI(env, obj);
}

/*
 * Java_TET_TestSession_getenv()
 *
 * Native method implementation.
 *
 *	Class:		TET.TestSession
 *	Method:		getenv
 * 	Signature:	(Ljava/lang/String;)Ljava/lang/String;
 *
 * Get a value from the environment. Calls the C library function getenv().
 *
 *	name	Name of environment variable of interest.
 *
 * Returns a new String object to the value corresponding to name in the
 * current environment, or NULL otherwise.
 */
JNIEXPORT jstring JNICALL
Java_TET_TestSession_getenv(JNIEnv *env, jobject obj, jstring name)
{
	char *cname;
	char *val;

	cname = jet_mkCstring(env, name);
	if (cname == NULL)
		return NULL;

	val = getenv(cname);
	if (val == NULL)
		return NULL;

	return jet_mkJstring(env, val);
}

/*
 * Java_TET_TestSession_getLiteIndicator()
 *
 * Native method implementation.
 *
 *	Class:		TET.TestSession
 *	Method:		getLiteIndicator
 * 	Signature:	()Z
 *
 * Determine whether we are running TETware-Lite.
 *
 * Returns JNI_TRUE if this is TETware-Lite, JNI_FALSE otherwise.
 */
JNIEXPORT jboolean JNICALL
Java_TET_TestSession_getLiteIndicator(JNIEnv *env, jobject obj)
{
#ifdef TET_LITE	/* -LITE-CUT-LINE- */
	return JNI_TRUE;
#else		/* -START-LITE-CUT- */
	return JNI_FALSE;
#endif		/* -END-LITE-CUT- */
}

/*
 * Java_TET_TestSession_tet_1merror()
 *
 * Native method implementation.
 *
 *	Class:		TET.TestSession
 *	Method:		tet_merror
 * 	Signature:	(I[Ljava/lang/String;)V
 *
 * Print API messages to the standard channel.
 */
JNIEXPORT void JNICALL
Java_TET_TestSession_tet_1merror(JNIEnv *env, jobject obj, jint errnum,
	jobjectArray msgs)
{
	char **lines;
	int nlines;

	/* Convert array of Java String objects into array of C strings */
	if (jet_mkCsarray(env, msgs, &lines, &nlines) != 0)
		return;

	/* Call the C API function tet_merror() */
	if (jet_enterAPI(env, obj) != 0)
		return;

	tet_merror((int)errnum, lines, nlines);

	jet_leaveAPI(env, obj);

	/* Free memory allocated for list of lines */
	jet_freeCsarray(lines);
}

#ifndef TET_LITE /* -START-LITE-CUT- */

/* Global structure used to communicate sync data between doremsync() and
 * syncreport().
 */
static struct
{
	int called;
	long syncptno;
	struct tet_syncstat *statp;
	int nstat;
} syncdata = { 0, 0L, NULL, 0 };

/*
 * doremsync()
 *
 * Do all the work for Java version of tet_remsync().
 */
static void
doremsync(JNIEnv *env, jobject obj, jlong syncptno, jintArray sysnames,
	jint waittime, jint vote, jobject msg)
{
	struct tet_synmsg msgbuf;
	struct tet_synmsg *synmsg;
	jclass msgcls;
	jmethodID mid;
	jbyteArray darray;
	jsize dlen;
	jint flags;
	char dbuf[TET_SMMSGMAX];
	int nsnames;
	int *snames;
	jint *jip;
	int rv;
	int err;
	jbyteArray newdarray;
	jclass synccls;
	jobjectArray syncarray;
	jobject syncobj;
	int i;

	/* Create C representation of msg object */
	synmsg = NULL;
	msgcls = NULL;
	dlen = 0;

	if (msg != NULL)
	{
		/* Get class of message object */
		msgcls = (*env)->GetObjectClass(env, msg);
		if (jet_checkJNI(env, "GetObjectClass", msgcls, JET_THROW) != 0)
			return;

		/* Retrieve flags value */
		mid = (*env)->GetMethodID(env, msgcls, "getFlags", "()I");
		if (jet_checkJNI(env, "GetMethodID", mid, JET_THROW) != 0)
			return;

		flags = (*env)->CallIntMethod(env, msg, mid);
		if (jet_checkJNI(env, "CallIntMethod", JET_JNIOK, JET_THROW)
			!= 0)
			return;

		/* Retrieve message data */
		mid = (*env)->GetMethodID(env, msgcls, "message", "()[B");
		if (jet_checkJNI(env, "GetMethodID", mid, JET_THROW) != 0)
			return;

		darray = (*env)->CallObjectMethod(env, msg, mid);
		if (jet_checkJNI(env, "CallObjectMethod", JET_JNIOK, JET_THROW)
			!= 0)
			return;

		/* Initialize C structure */
		msgbuf.tsm_flags = (int)flags;
		msgbuf.tsm_sysid = -1;

		if (darray == NULL)
		{
			msgbuf.tsm_data = NULL;
			msgbuf.tsm_dlen = 0;
		}
		else
		{
			dlen = (*env)->GetArrayLength(env, darray);
			if (jet_checkJNI(env, "GetArrayLength", JET_JNIOK,
				JET_THROW) != 0)
				return;

			if (dlen > sizeof(dbuf))
				dlen = sizeof(dbuf);

			(*env)->GetByteArrayRegion(env, darray, 0, dlen,
				(jbyte *)dbuf);
			if (jet_checkJNI(env, "GetByteArrayRegion", JET_JNIOK,
				JET_THROW) != 0)
				return;

			msgbuf.tsm_data = dbuf;
			msgbuf.tsm_dlen = (int)dlen;
		}

		synmsg = &msgbuf;
	}

	/* Get length of sysnames array and allocate space for C array */
	nsnames = (*env)->GetArrayLength(env, sysnames);
	if (jet_checkJNI(env, "GetArrayLength", JET_JNIOK, JET_THROW) != 0)
		return;

	snames = jet_malloc(env, sizeof(*snames) * (nsnames + 1));
	if (snames == NULL)
		return;

	/* Get a pointer to the Java array elements and copy them into the
	 * C array.
	 */
	jip = (*env)->GetIntArrayElements(env, sysnames, NULL);
	if (jet_checkJNI(env, "GetIntArrayElements", jip, JET_THROW) != 0)
	{
		free(snames);
		return;
	}

	for (i = 0; i < nsnames; i++)
		snames[i] = jip[i];

	snames[nsnames] = 0;

	/* Release the array elements */
	(*env)->ReleaseIntArrayElements(env, sysnames, jip, 0);

	/* Prepare to enter TETware C API */
	if (jet_enterAPI(env, obj) != 0)
	{
		free(snames);
		return;
	}

	/* Install sync report function */
	tet_syncerr = syncreport;
	syncdata.called = 0;

	/* Call TETware C API tet_remsync() */
	rv = tet_remsync((long)syncptno, snames, nsnames, (int)waittime,
		(int)vote, synmsg);
	err = tet_errno;

	/* Leave TETware C API */
	jet_leaveAPI(env, obj);

	/* Free storage allocated for array of system names */
	free(snames);

	/* Set msg fields according to contents of C structure */
	if (msg != NULL)
	{
		if (synmsg->tsm_dlen > dlen)
			synmsg->tsm_dlen = dlen;

		if (synmsg->tsm_dlen > 0)
		{
			newdarray = (*env)->NewByteArray(env, synmsg->tsm_dlen);
			if (jet_checkJNI(env, "NewByteArray", newdarray,
				JET_THROW) != 0)
				return;

			(*env)->SetByteArrayRegion(env, newdarray, 0,
				synmsg->tsm_dlen, (jbyte *)synmsg->tsm_data);
			if (jet_checkJNI(env, "SetByteArrayRegion", JET_JNIOK,
				JET_THROW) != 0)
				return;
		}
		else
		{
			newdarray = NULL;
		}

		mid = (*env)->GetMethodID(env, msgcls, "setFields", "([BII)V");
		if (jet_checkJNI(env, "GetMethodID", mid, JET_THROW) != 0)
			return;

		(*env)->CallVoidMethod(env, msg, mid, newdarray,
			synmsg->tsm_flags, synmsg->tsm_sysid);
		if (jet_checkJNI(env, "CallVoidMethod", JET_JNIOK, JET_THROW)
			!= 0)
			return;
	}

	/* Check result of synchronization, and throw a TetException if it
	 * failed.
	 */
	if (syncdata.called)
	{
		/* Construct data for creating new exception object */
		if (syncdata.nstat > 0)
		{
			if (syncdata.statp == NULL)
			{
				jet_throw(env, "java.lang.OutOfMemoryError",
					"malloc() failure in syncreport()");
				return;
			}

			synccls = (*env)->FindClass(env, "TET/SyncState");
			if (jet_checkJNI(env, "FindClass", synccls, JET_THROW)
				!= 0)
				return;

			syncarray = (*env)->NewObjectArray(env,
				syncdata.nstat, synccls, NULL);
			if (jet_checkJNI(env, "NewObjectArray", syncarray,
				JET_THROW) != 0)
				return;

			mid = (*env)->GetMethodID(env, synccls, "<init>",
				"(II)V");
			if (jet_checkJNI(env, "GetMethodID", mid, JET_THROW)
				!= 0)
				return;

			for (i = 0; i < syncdata.nstat; i++)
			{
				syncobj = (*env)->NewObject(env, synccls, mid,
					(jint)syncdata.statp[i].tsy_sysid,
					(jint)syncdata.statp[i].tsy_state);
				if (jet_checkJNI(env, "NewObject", syncobj,
					JET_THROW) != 0)
					return;

				(*env)->SetObjectArrayElement(env, syncarray,
					i, syncobj);
				if (jet_checkJNI(env, "SetObjectArrayElement",
					JET_JNIOK, JET_THROW) != 0)
					return;
			}
		}
		else
		{
			syncarray = NULL;
		}

		/* Now throw the exception */
		jet_throwtet(env, err, NULL, syncdata.syncptno, syncarray);
	}
	else if (rv != 0)
	{
		jet_throwtet(env, err, NULL, (long)syncptno, NULL);
	}
}

/*
 * syncreport()
 *
 * Sync error handler for use with tet_remsync(). Saves arguments in global
 * syncdata structure. Note that after calling this function, if
 * syncdata.nstat is 0, syncdata.statp may be NULL if the call to
 * malloc() failed.
 *
 *	syncptno	Number of the sync point that has failed.
 *	statp		Array of sync structures describing the sync status of
 *			the other systems participating in the event.
 *	nstat		Number of elements of statp array.
 */
static void
syncreport(long syncptno, struct tet_syncstat *statp, int nstat)
{
	if (syncdata.statp != NULL)
		free(syncdata.statp);

	syncdata.syncptno = syncptno;
	syncdata.nstat = nstat;

	if (nstat > 0)
	{
		syncdata.statp
			= malloc(sizeof(*syncdata.statp) * nstat);
		if (syncdata.statp != NULL)
			memcpy(syncdata.statp, statp,
				sizeof(*statp) * nstat);
	}
	else
	{
		syncdata.statp = NULL;
	}

	syncdata.called = 1;
}

#endif /* -LITE-CUT-LINE- */
