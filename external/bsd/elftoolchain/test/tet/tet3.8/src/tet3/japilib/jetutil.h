/*
 *	SCCS: @(#)jetutil.h	1.1 (99/09/02)
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

/************************************************************************

SCCS:		@(#)jetutil.h	1.1 99/09/02 TETware release 3.8
NAME:		jetutil.h
PRODUCT:	TETware
AUTHOR:		Matthew Hails, UniSoft Ltd.
DATE CREATED:	26 July 1999

DESCRIPTION:
	Function prototypes and defines for utility functions used by the
	C code in the TETware Java API.

************************************************************************/

#ifndef JETUTIL_H_INCLUDED
#define JETUTIL_H_INCLUDED

#include "jni.h"

#define JET_JNIOK	((void *)1)

#define JET_CLEAR	1
#define JET_THROW	2

void jet_storeJenv(JNIEnv *env, jobject tc, jobject ts);
void jet_logmsg(char *fmt, ...);
void jet_fatal(JNIEnv *env, char *fmt, ...);
void jet_throw(JNIEnv *env, char *classname, char *msg);
int jet_checkJNI(JNIEnv *env, char *funcname, void *rv, int throwaction);
void jet_throwtet(JNIEnv *env, int err, char *msg, long syncpt,
	jobjectArray syncarray);
void jet_errlite(JNIEnv *env, char *apimethname);
void * jet_malloc(JNIEnv *env, size_t size);
int jet_enterAPI(JNIEnv *env, jobject ts);
int jet_leaveAPI(JNIEnv *env, jobject ts);
int jet_enterTCI(char *tcifuncname, JNIEnv **envp, jobject *tcp, jobject *tsp);
int jet_leaveTCI(JNIEnv *env, jobject ts);
char * jet_mkCstring(JNIEnv *env, jstring jstr);
int jet_mkCsarray(JNIEnv *env, jobjectArray arr, char ***sarray, int *len);
void jet_freeCsarray(char **sarray);
jstring jet_mkJstring(JNIEnv *env, char *cstr);
jobjectArray jet_mkJsarray(JNIEnv *env, char **csarray, int nelem);
jintArray jet_mkJintarray(JNIEnv *env, int *carray, int nelem);

#endif /* JETUTIL_H_INCLUDED */
