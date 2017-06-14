/*
 *	SCCS: @(#)jetutil.c	1.2 (05/07/08)
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
static char sccsid[] = "@(#)jetutil.c	1.2 (05/07/08) TETware release 3.8";
#endif

/************************************************************************

SCCS:		@(#)jetutil.c	1.2 05/07/08 TETware release 3.8
NAME:		jetutil.c
PRODUCT:	TETware
AUTHOR:		Matthew Hails, UniSoft Ltd. (Based on JETpack 1.02 source)
DATE CREATED:	26 July 1999
SYNOPSIS:

	void jet_storeJenv(JNIEnv *env, jobject tc, jobject ts)
	void jet_logmsg(char *fmt, ...)
	void jet_fatal(JNIEnv *env, char *fmt, ...)
	void jet_throw(JNIEnv *env, char *classname, char *msg)
	int jet_checkJNI(JNIEnv *env, char *funcname, void *rv,
		int throwaction)
	void jet_throwtet(JNIEnv *env, int err, char *msg, long syncpt,
		jobjectArray syncarray)
	void jet_errlite(JNIEnv *env, char *apimethname)
	void * jet_malloc(JNIEnv *env, size_t size)
	int jet_enterAPI(JNIEnv *env, jobject ts)
	int jet_leaveAPI(JNIEnv *env, jobject ts)
	int jet_enterTCI(char *tcifuncname, JNIEnv **envp, jobject *tcp,
		jobject *tsp);
	int jet_leaveTCI(JNIEnv *env, jobject ts);
	char * jet_mkCstring(JNIEnv *env, jstring jstr)
	int jet_mkCsarray(JNIEnv *env, jobjectArray arr, char ***sarray,
		int *len)
	void jet_freeCsarray(char **sarray)
	jstring jet_mkJstring(JNIEnv *env, char *cstr)
	jobjectArray jet_mkJsarray(JNIEnv *env, char **csarray, int nelem)
	jintArray jet_mkJintarray(JNIEnv *env, int *carray, int nelem)

DESCRIPTION:
	Utility functions for use by implementations of TETware Java
	native methods.

	Note that a lot of functions which can return a failure result
	also throw an exception on a failure return. In the serious case
	that the JNI calls to create an exception or error fail, the
	current process is terminated by calling abort().

MODIFICATIONS:
	Geoff Clare, The Open Group, July 2005
	Added <string.h>

************************************************************************/

#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <limits.h>
#include "jni.h"
#include "tet_api.h"
#include "jetutil.h"

#define JET_OP_GET	1
#define JET_OP_SET	2

/* Local prototypes */
static void jet_verr(int isfatal, JNIEnv *env, char *fmt, va_list ap);

/* Environment of Java thread which is currently executing in the test case */
static struct
{
	int stored;
	JNIEnv *env;
	jobject tcobj;
	jobject tsobj;
} jet_Jenv = { 0, NULL, NULL, NULL };

/*
 * jet_storeJenv()
 *
 * Stores the JNI environment for the current thread. Designed to be called
 * when the caller is about to enter the C test case manager or child process
 * controller to save the JNI environment for retrieval by the dynamic test
 * case functions or the child process tet_main() function.
 *
 *	env		The JNI interface pointer.
 *	tc		The test case object.
 *	ts		The test session object.
 */
void
jet_storeJenv(JNIEnv *env, jobject tc, jobject ts)
{
	jet_Jenv.env = env;
	jet_Jenv.tcobj = tc;
	jet_Jenv.tsobj = ts;
	jet_Jenv.stored = 1;
}

/*
 * jet_verr()
 *
 * Error handling routine. Prints a formatted error message of a variable
 * argument list - either to the TETware execution results file or to stderr.
 *
 *	isfatal		Is the error fatal? If non-zero, then this process is
 *			terminated.
 *	env		The JNI interface pointer.
 *	fmt		Format string, cf. printf().
 *	ap		Variable argument list.
 */
static void
jet_verr(int isfatal, JNIEnv *env, char *fmt, va_list ap)
{
#define MAX_ERROR_MESSAGE 512
	char buf[MAX_ERROR_MESSAGE];

	vsprintf(buf, fmt, ap);

	/* If the C API has been initialized, we can use TET to log the message
	 * to the execution results file. Otherwise, use stderr.
	 */
	if (jet_Jenv.stored)
	{
		tet_error(0, buf);
	}
	else
	{
		fputs(buf, stderr);
		fputc('\n', stderr);
	}

	/* If this is a fatal error then exit */
	if (isfatal)
	{
		/* If necessary, log off all TETware servers */
		if (jet_Jenv.stored)
			tet_logoff();

		/* Flush all stdio streams, just in case FatalError() doesn't */
		fflush(NULL);

		/* Call FatalError() to exit the VM and this process */
		if (env != NULL)
			(*env)->FatalError(env, buf);

		/* Ensure process is terminated */
		exit(1);
	}
}

/*
 * jet_logmsg()
 *
 * Log an error message - either to execution results file or stderr.
 *
 *	fmt		Format string, cf. printf().
 */
void
jet_logmsg(char *fmt, ...)
{
	va_list ap;

	va_start(ap, fmt);
	jet_verr(0, NULL, fmt, ap);
	va_end(ap);
}

/*
 * jet_fatal()
 *
 * Process a fatal error. Logs the message and exits.
 *
 *	env		The JNI interface pointer.
 *	fmt		Format string, cf. printf().
 */
void
jet_fatal(JNIEnv *env, char *fmt, ...)
{
	va_list ap;

	va_start(ap, fmt);
	jet_verr(1, env, fmt, ap);
	va_end(ap);
}

/*
 * jet_throw()
 *
 * Throws an exception. If another Exception or Error is currently being
 * thrown, then this routine does nothing. Otherwise it attempts to throw a new
 * exception of the specified class. If this fails, it aborts execution of
 * current process using abort(), first printing an error message (using
 * TETware if we are under the control of the TCM, or to stderr otherwise).
 *
 *	env		The JNI interface pointer.
 *	classname	Name of the class to throw. Must be a subclass of
 *			java.lang.Throwable.
 *	msg		The message used to construct the throwable object.
 */
void
jet_throw(JNIEnv *env, char *classname, char *msg)
{
	jclass cls;

	/* If an exception is already being thrown - just return, we can't
	 * throw another one without dealing with the existing one.
	 */
	if ((*env)->ExceptionOccurred(env) != NULL)
		return;

	/* Find the correct exception to throw and throw it */
	cls = (*env)->FindClass(env, classname);
	if (cls == NULL || (*env)->ThrowNew(env, cls, msg) != 0)
	{
		jet_fatal(env, "Fatal error throwing \"%s\", message \"%s\"",
			classname, msg);
	}
}

/*
 * jet_checkJNI()
 *
 * Check the results of a call to a JNI function. If an exception is being
 * thrown or the rv argument is NULL, this logs an error message and returns
 * a failure status.
 * 
 *	env		The JNI interface pointer.
 *	funcname	Name of the JNI function which was called.
 *	rv		Return value of the JNI function in question.
 *	throwaction	If this is JET_THROW, this function ensures that the
 *			JNI error causes an Exception or Error to be thrown. If
 *			one is not already being thrown, it uses jet_throw() to
 *			throw a java.lang.InternalError object.
 *			If this is JET_CLEAR, this function ensures that all
 *			Exceptions/Errors are cleared when this function
 *			returns.
 *
 * Returns 0 if the JNI function succeeded, -1 if it did not.
 */
int
jet_checkJNI(JNIEnv *env, char *funcname, void *rv, int throwaction)
{
	static char fmt[] = "JNI function %.*s() failed";
	char msg[sizeof(fmt) + sizeof("AStringLongerThanLongestJNIFuncName")];
	jthrowable t;
	char *cmsg;
	jclass cls;
	jmethodID mid;
	jstring jmsg;
	jclass errcls;

	/* Check if JNI function threw an exception or error */
	t = (*env)->ExceptionOccurred(env);
	if (t != NULL)
	{
		if (throwaction == JET_THROW)
		{
			/* Log the error message */
			jet_logmsg(
				"JNI function %s() threw an exception/error",
				funcname);
		}
		else
		{
			/* Clear the exception */
			(*env)->ExceptionClear(env);

			/* Attempt call the getMessage() method on the
			 * throwable object.
			 */
			cmsg = NULL;

			cls = (*env)->GetObjectClass(env, t);
			if (cls != NULL)
			{
				mid = (*env)->GetMethodID(env, cls, "toString",
					"()Ljava/lang/String;");
				if (mid != NULL)
				{
					jmsg = (*env)->CallObjectMethod(env, t,
						mid);
					if (jmsg != NULL)
						cmsg = jet_mkCstring(env, jmsg);
				}
			}

			/* Clear any exceptions which occurred while attempting
			 * the method call.
			 */
			(*env)->ExceptionClear(env);

			/* Log the error message */
			jet_logmsg("JNI function %s() threw exception/error (cleared):",
				funcname);

			if (cmsg == NULL)
			{
				jet_logmsg("    <unknown Throwable object>");
			}
			else
			{
				jet_logmsg("    %s", cmsg);
				free(cmsg);
			}

			/* If the Throwable object is a subclass of TetError,
			 * this is a fatal error, so print an error message to
			 * that effect and exit.
			 */
			errcls = (*env)->FindClass(env, "TET/TetError");
			if (errcls == NULL)
				jet_fatal(env, "Can't find class TET.TetError - abandoning test case");

			if ((*env)->IsInstanceOf(env, t, errcls))
				jet_fatal(env, "Caught TetError - abandoning test case");

			/* Again, clear any exceptions */
			(*env)->ExceptionClear(env);
		}

		return -1;
	}

	/* Check if JNI function returned a failure value without throwing an
	 * exception (probably should not happen).
	 */
	if (rv == NULL)
	{
		sprintf(msg, fmt, (int)(sizeof(msg) - sizeof(fmt) - 1),
			funcname);
		jet_logmsg("%s", msg);

		if (throwaction)
			jet_throw(env, "java.lang.InternalError", msg);

		return -1;
	}

	return 0;
}

/*
 * jet_throwtet()
 *
 * Throws a TetException. If another Exception or Error is currently being
 * thrown, then this routine does nothing. Otherwise, it constructs a new
 * TET.TetException object and throws it. If this fails, it attempts to throw
 * an error using jet_throw().
 *
 *	env		The JNI interface pointer.
 *	err		Value of tet_errno used when constructing the
 *			TetException object. 
 *	msg		The detail message for this exception.
 *	syncpt		Synchronization point used in constructor.
 *	syncarray	SyncState array to pass to the TetException
 *			constructor.
 */
void
jet_throwtet(JNIEnv *env, int err, char *msg, long syncpt,
	jobjectArray syncarray)
{
	static char fmt[] = "(TET errno %d)";
	jclass cls;
	jmethodID mid;
	char buf[sizeof(fmt) + ((sizeof(int) * CHAR_BIT + 2) / 3 + 1 + 1)];
	char *errmsg;
	jstring msgobj;
	jobject ex;

	/* Check to see if an exception if currently being thrown. If so then
	 * we just return, since we won't be able to throw another without
	 * dealing with the first.
	 */
	if ((*env)->ExceptionOccurred(env) != NULL)
		return;

	/* Create a new TetException object */
	cls = (*env)->FindClass(env, "TET/TetException");
	if (jet_checkJNI(env, "FindClass", cls, JET_THROW) != 0)
		return;

	mid = (*env)->GetMethodID(env, cls, "<init>",
		"(ILjava/lang/String;J[LTET/SyncState;)V");
	if (jet_checkJNI(env, "GetMethodID", mid, JET_THROW) != 0)
		return;

	/* If a message was passed in, use that. Otherwise use the message
	 * corresponding to the value of tet_errno.
	 */
	if (msg == NULL)
	{
		if (err >= 0 && err < tet_nerr)
		{
			errmsg = tet_errlist[err];
		}
		else
		{
			sprintf(buf, fmt, err);
			errmsg = buf;
		}
	}
	else
	{
		errmsg = msg;
	}

	msgobj = jet_mkJstring(env, errmsg);
	if (msgobj == NULL)
		return;

	/* Create the object */
	ex = (*env)->NewObject(env, cls, mid, (jint)err, msgobj, (jlong)syncpt,
		syncarray);
	if (jet_checkJNI(env, "NewObject", ex, JET_THROW) != 0)
		return;

	/* Throw the new TetException object */
	if ((*env)->Throw(env, ex) != 0)
	{
		jet_checkJNI(env, "Throw", NULL, JET_THROW);
		return;
	}
}

/*
 * jet_errlite()
 *
 * Deals with a Distributed TETware method being called on TETware-Lite. Logs
 * a message to the journal and throws a new TetException using jet_throwtet().
 * 
 *	env		The JNI interface pointer.
 *	apimethname	Name of the offending Distributed TETware API method.
 */
void
jet_errlite(JNIEnv *env, char *apimethname)
{
	static char fmt[] = "Distributed method %.*s() called in TETware-Lite";
	char msg[sizeof(fmt) + sizeof("longerthanallapimethodnames")];

	sprintf(msg, fmt, (int)(sizeof(msg) - sizeof(fmt)), apimethname);
	jet_throwtet(env, TET_ER_CONTEXT, msg, 0L, NULL);
}

/*
 * jet_malloc()
 *
 * Local version of malloc(). Same as standard C function, plus it throws an
 * java.lang.OutOfMemoryError if the memory could not be allocated.
 *
 *	env		The JNI interface pointer.
 *
 * Returns a pointer to the newly allocated memory, or NULL if the memory
 * could not be allocated. On a failure return, an error is being thrown.
 */
void *
jet_malloc(JNIEnv *env, size_t size)
{
	void *p;

	p = malloc(size);
	if (p == NULL)
	{
		jet_throw(env, "java.lang.OutOfMemoryError",
			"malloc() failure in jet_malloc()");
	}

	return p;
}

/*
 * jet_threadData()
 *
 * Modify or retrieve the thread-specific data for the current thread.
 *
 *	env		The JNI interface pointer.
 *	ts		The TestSession object.
 *	op		Operation to perform. One of:
 *
 *				JET_OP_SET	Modify thread-specific data
 *						based on current C API
 *						globals.
 *				JET_OP_GET	Retrieve thread-specific data
 *						and use it to modify C API
 *						globals.
 *
 * Returns 0 on success, -1 on failure. On a failure return, an error is being
 * thrown.
 */
static int
jet_threadData(JNIEnv *env, jobject ts, int op)
{
	jclass tscls;
	jmethodID mid;
	jobject obj;
	jclass cls;
	jfieldID blockFID;
	jfieldID seqFID;
	jlong block;
	jlong sequence;

	/* Call ts.getThreadData() via JNI */
	tscls = (*env)->GetObjectClass(env, ts);
	if (jet_checkJNI(env, "GetObjectClass", tscls, JET_THROW) != 0)
		return -1;

	mid = (*env)->GetMethodID(env, tscls, "getThreadData",
		"()LTET/ThreadData;");
	if (jet_checkJNI(env, "GetMethodID", mid, JET_THROW) != 0)
		return -1;

	obj = (*env)->CallObjectMethod(env, ts, mid);
	if (jet_checkJNI(env, "CallObjectMethod", mid, JET_THROW) != 0)
		return -1;

	/* Retrieve field IDs for block and sequence from ThreadData object */
	cls = (*env)->GetObjectClass(env, obj);
	if (jet_checkJNI(env, "GetObjectClass", cls, JET_THROW) != 0)
		return -1;

	blockFID = (*env)->GetFieldID(env, cls, "block", "J");
	if (jet_checkJNI(env, "GetFieldID", blockFID, JET_THROW) != 0)
		return -1;

	seqFID = (*env)->GetFieldID(env, cls, "sequence", "J");
	if (jet_checkJNI(env, "GetFieldID", seqFID, JET_THROW) != 0)
		return -1;

	if (op == JET_OP_GET)
	{
		/* Get block and sequence from ThreadData object */
		block = (*env)->GetLongField(env, obj, blockFID);
		sequence = (*env)->GetLongField(env, obj, seqFID);
		if (jet_checkJNI(env, "GetLongField", JET_JNIOK, JET_THROW)
			!= 0)
			return -1;

		/* Use these to set the TETware C API globals */
		tet_block = (long)block;
		tet_sequence = (long)sequence;
	}
	else
	{
		/* Set the block and sequence in the ThreadData object from
		 * the TETware C API globals.
		 */
		(*env)->SetLongField(env, obj, blockFID, (jlong)tet_block);
		(*env)->SetLongField(env, obj, seqFID, (jlong)tet_sequence);
		if (jet_checkJNI(env, "SetLongField", JET_JNIOK, JET_THROW)
			!= 0)
			return -1;
	}

	return 0;
}

/*
 * jet_enterAPI()
 *
 * Enter the TETware C API/TCM. This should be called before accessing the C
 * API or C TCM. It sets the TET globals which are thread-dependent. These are
 * tet_block and tet_sequence. Note that tet_errno is not set, since this is
 * only relevant for the user of the API to read, not to set.
 *
 *	env		The JNI interface pointer.
 *	ts		The TestSession object.
 *
 * Returns 0 on success, -1 on failure. On a failure return, an exception/error
 * is being thrown.
 */
int
jet_enterAPI(JNIEnv *env, jobject ts)
{
	return jet_threadData(env, ts, JET_OP_GET);
}

/*
 * jet_leaveAPI()
 *
 * Leave the TETware C API/TCM. This should be called after accessing the C API
 * or C TCM. It stores the TET globals which are thread-dependent. These are
 * either stored in the current thread object, where it is a TetThread, or in
 * globals. These are tet_block and tet_sequence. Note that tet_errno is not
 * stored, since it is only used when a TetException object is created, which
 * is only done inside the synchronized native class methods which access the
 * API.
 *
 *	env		The JNI interface pointer.
 *	ts		The TestSession object.
 *
 * Returns 0 on success, -1 on failure. On a failure return, an exception/error
 * is being thrown.
 */
int
jet_leaveAPI(JNIEnv *env, jobject ts)
{
	return jet_threadData(env, ts, JET_OP_SET);
}

/*
 * jet_enterTCI()
 *
 * Enter the Test Case Interface. Should be called whenever control enters one
 * of the Test Case Interface functions (from one of the various TCMs).
 *
 *	tcifuncname	Name of the Test Case Interface function invoked.
 *	envp		The JNI interface pointer is returned here.
 *	tcp		The TestCase object is returned here.
 *	tsp		The TestSession object is returned here.
 *
 * Returns 0 on success, -1 on failure. Any exceptions are cleared before the
 * function returns.
 */
int
jet_enterTCI(char *tcifuncname, JNIEnv **envp, jobject *tcp, jobject *tsp)
{
	JNIEnv *env;
	jobject ts;

	/* Check that jet_storeJenv() has been called to store the JNI
	 * environment. If this is not the case, it is almost certainly because
	 * we have been called before the C API has been initialized.
	 */
	if (!jet_Jenv.stored)
	{
		jet_logmsg("%s() called before TETware API is initialized",
			tcifuncname);
		return -1;
	}

	env = jet_Jenv.env;
	ts = jet_Jenv.tsobj;

	/* Set thread environment from TETware globals. If this fails, an
	 * error is being thrown, so we need to clear that and log an error
	 * message.
	 */
	if (jet_leaveAPI(env, ts) != 0)
	{
		(*env)->ExceptionClear(env);
		jet_logmsg(
			"Error setting thread environment in call to %s()",
			tcifuncname);
		return -1;
	}

	*envp = jet_Jenv.env;
	*tcp = jet_Jenv.tcobj;
	*tsp = jet_Jenv.tsobj;

	return 0;
}

/*
 * jet_leaveTCI()
 *
 * Leave the Test Case Interface. Should be called whenever control is about
 * to leave one of the Test Case Interface functions and return to one of the
 * various TCMs.
 *
 *	env		The JNI interface pointer.
 *	ts		The TestSession object.
 *
 * Returns 0 on success, -1 on failure. Any exceptions are cleared before the
 * function returns.
 */
int
jet_leaveTCI(JNIEnv *env, jobject ts)
{
	/* Set TETware globals from thread environment. If this fails, an
	 * error is being thrown, so we need to clear that and log an error
	 * message.
	 */
	if (jet_enterAPI(env, ts) != 0)
	{
		(*env)->ExceptionClear(env);
		jet_logmsg("Error accessing thread environment");
		return -1;
	}

	return 0;
}

/*
 * jet_mkCstring()
 *
 * Make a C string from a Java string. The string is returned in malloced
 * storage.
 *
 *	env		The JNI interface pointer.
 *	jstr		The Java string.
 *
 * Returns a pointer to the newly allocated string, or NULL on failure.
 * On a failure return, an exception/error is being thrown.
 */
char *
jet_mkCstring(JNIEnv *env, jstring jstr)
{
	static char digits[] = "0123456789abcdef";
	jsize ulen;
	jchar *ustr;
	jchar *u;
	jsize alen;
	char *astr;
	char *a;

	/* Get the length of the Java (Unicode) string */
	ulen = (*env)->GetStringLength(env, jstr);
	if (jet_checkJNI(env, "GetStringLength", JET_JNIOK, JET_THROW) != 0)
		return NULL;

	/* Get the Unicode characters of the Java string */
	ustr = (jchar *) (*env)->GetStringChars(env, jstr, 0);
	if (jet_checkJNI(env, "GetStringChars", ustr, JET_THROW) != 0)
		return NULL;

	/* Determine the length of the corresponding ASCII version */
	for (alen = 0, u = ustr; u < ustr + ulen; u++)
		alen += (*u > 0x7f) ? 6 : 1;

	/* Allocate memory for ASCII string and convert from Unicode.
	 * Characters 0 - 127 (0x7f) in Unicode are the same as ASCII, so if
	 * a character is in this range, it is simply assigned to the
	 * corresponding C character. Otherwise a Unicode escape sequence is
	 * constructed. These are of the form \uxxxx, where each x is a hex
	 * digit.
 	 */
	astr = jet_malloc(env, alen + 1);
	if (astr != NULL)
	{
		for (a = astr, u = ustr; u < ustr + ulen; u++)
		{
			if (*u > 0x7f)
			{
				*a++ = '\\';
				*a++ = 'u';
				*a++ = digits[(*u >> 12) & 0xf];
				*a++ = digits[(*u >> 8)  & 0xf];
				*a++ = digits[(*u >> 4)  & 0xf];
				*a++ = digits[*u         & 0xf];
			}
			else
			{
				*a++ = (*u & 0x7f);
			}
		}

		*a = '\0';
	}

	(*env)->ReleaseStringChars(env, jstr, ustr);

	return astr;
}

/*
 * jet_mkCsarray()
 *
 * Make an array of C strings from an array of Java objects. The array is
 * returned in malloced storage and may be freed using jet_freeCsarray().
 *
 *	env		The JNI interface pointer.
 *	arr		The array of Java objects.
 *	sarray		The new array of C strings is returned here, as a NULL
 *			terminated array of char *'s.
 *	len		If this is non-NULL, the number of elements of
 *			`sarray', not including the NULL terminating string, is
 *			returned in here.
 *
 * Returns 0 on success, -1 on failure. On a failure return, an exception/error
 * is being thrown.
 */
int
jet_mkCsarray(JNIEnv *env, jobjectArray arr, char ***sarray, int *len)
{
	jsize nelems;
	char **newarray;
	jstring js;
	int i;

	/* Obtain the number of array elements */ 
	nelems = (*env)->GetArrayLength(env, arr);
	if (jet_checkJNI(env, "GetArrayLength", JET_JNIOK, JET_THROW) != 0)
		return -1;

	/* Allocate memory for the new array, allowing for a terminating NULL
	 * string.
	 */
	newarray = jet_malloc(env, sizeof(*newarray) * (nelems + 1));
	if (newarray == NULL)
		return -1;

	/* Initialize every element of the new array to NULL so we can call
	 * jet_freeCsarray() if there are later errors.
	 */
	for (i = 0; i <= nelems; i++)
		newarray[i] = NULL;

	/* Convert each Java string into a C string */
	for (i = 0; i < nelems; i++)
	{
		js = (*env)->GetObjectArrayElement(env, arr, i);
		if (jet_checkJNI(env, "GetObjectArrayElement", js, JET_THROW)
			!= 0)
		{
			jet_freeCsarray(newarray);
			return -1;
		}

		newarray[i] = jet_mkCstring(env, js);
		if (newarray[i] == NULL)
		{
			jet_freeCsarray(newarray);
			return -1;
		}
	}

	/* Add NULL terminating string */
	newarray[i] = NULL;

	/* Assign return parameters */
	*sarray = newarray;

	if (len != NULL)
		*len = nelems;

	return 0; 
}

/*
 * jet_freeCsarray()
 *
 * Free an array of strings created using jet_mkCsarray().
 *
 *	sarray		The array of C strings created by jet_mkCsarray().
 */
void
jet_freeCsarray(char **sarray)
{
	char **s;

	for (s = sarray; *s != NULL; s++)
		free(*s);

	free(sarray);
}

/*
 * jet_mkJstring()
 *
 * Make a Java string from a C string.
 *
 *	env		The JNI interface pointer.
 *	cstr		The C string.
 *
 * Returns a pointer to the new java.lang.String object, or NULL on failure.
 * On a failure return, an exception/error is being thrown.
 */
jstring
jet_mkJstring(JNIEnv *env, char *cstr)
{
	jchar *jc;
	size_t len;
	int i;
	jstring jstr;

	len = strlen(cstr);

	/* Allocate memory for unicode characters */
	jc = jet_malloc(env, sizeof(*jc) * len);
	if (jc == NULL)
		return NULL;

	/* Construct unicode characters from C chars. The first 0 - 127
	 * characters of unicode are the same as ASCII, so we can just assign
	 * the bottom 7 bits of each C character to the corresponding unicode
	 * character.
	 */
	for (i = 0; i < len; i++)
		jc[i] = (cstr[i] & 0x7f);

	/* Construct a new java.lang.String object */
	jstr = (*env)->NewString(env, jc, (jsize)len);
	if (jet_checkJNI(env, "NewString", jstr, JET_THROW) != 0)
		jstr = NULL;

	/* Free memory allocated for unicode chars */
	free(jc);

	return jstr;
}

/*
 * jet_mkJsarray()
 *
 * Make an array of Java strings from an array of C strings.
 *
 *	env		The JNI interface pointer.
 *	csarray		The array of C strings.
 *	nelem		The number of elements in csarray.
 *
 * Returns a pointer to the new array object, or NULL on failure.
 * On a failure return, an exception/error is being thrown.
 */
jobjectArray
jet_mkJsarray(JNIEnv *env, char **csarray, int nelem)
{
	jclass cls;
	jobjectArray newarray;
	int i;
	jstring newstr;

	/* Get class object for String class */
	cls = (*env)->FindClass(env, "java/lang/String");
	if (jet_checkJNI(env, "FindClass", cls, JET_THROW) != 0)
		return NULL;

	/* Create a new array of Strings, with each element initially null */
	newarray = (*env)->NewObjectArray(env, nelem, cls, NULL);
	if (jet_checkJNI(env, "NewObjectArray", newarray, JET_THROW) != 0)
		return NULL;

	/* Create new String objects for each element from the corresponding
	 * C string and set them into the new String object array.
	 */
	for (i = 0; i < nelem; i++)
	{
		newstr = jet_mkJstring(env, csarray[i]);
		if (newstr == NULL)
			return NULL;

		(*env)->SetObjectArrayElement(env, newarray, i, newstr);
		if (jet_checkJNI(env, "SetObjectArrayElement", JET_JNIOK,
			JET_THROW) != 0)
			return NULL;
	}

	return newarray;
}

/*
 * jet_mkJintarray()
 *
 * Make an array of Java integers from an array of C integers.
 *
 *	env		The JNI interface pointer.
 *	carray		The array of C integers.
 *	nelem		The number of elements in carray.
 *
 * Returns a pointer to the new int array object, or NULL on failure.
 * On a failure return, an exception/error is being thrown.
 */
jintArray
jet_mkJintarray(JNIEnv *env, int *carray, int nelem)
{
	jintArray newarray;
	jint *jip;
	int i;

	/* Create a new Java int array object */
	newarray = (*env)->NewIntArray(env, (jsize)nelem);
	if (jet_checkJNI(env, "NewIntArray", newarray, JET_THROW) != 0)
		return NULL;

	/* Get a pointer to the elements and fill in the values */
	jip = (*env)->GetIntArrayElements(env, newarray, NULL);
	if (jet_checkJNI(env, "GetIntArrayElements", jip, JET_THROW) != 0)
		return NULL;

	for (i = 0; i < nelem; i++)
		jip[i] = carray[i];

	/* Release the array elements */
	(*env)->ReleaseIntArrayElements(env, newarray, jip, 0);

	return newarray;
}
