/*
 *	SCCS: @(#)dtthr.h	1.18 (99/11/15)
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

SCCS:   	@(#)dtthr.h	1.18 99/11/15 TETware release 3.8
NAME:		dtthr.h
PRODUCT:	TETware
AUTHOR:		Geoff Clare, UniSoft Ltd.
DATE CREATED:	July 1996

DESCRIPTION:
	macros used for thread safety

MODIFICATIONS:
	
	Geoff Clare, UniSoft Ltd., Sept 1996
	Moved alarm stuff to alarm.h.
	Changes for POSIX threads.
	
	Geoff Clare, UniSoft Ltd., Oct 1996
	Added tet_thr_equal definitions.
	
	Geoff Clare, UniSoft Ltd., June 1997
	Changes to support NT threads.

	Andrew Dingwall, UniSoft Ltd., January 1998
	Changes to enable this file to be used on UNIX systems which
	support both UI threads and POSIX threads.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
	Andrew Dingwall, UniSoft Ltd., October 1999
	added support for strict POSIX threads

************************************************************************/

#ifdef TET_THREADS

/*
**	support for POSIX threads
**
**	A strictly conforming POSIX application may only call async-safe
**	functions in the child of a multi-threaded parent process.
**	A consequence of this requirement is that the API can't report
**	errors to the journal under these conditions.
**
**	The API functions aren't async-signal safe, particularly in
**	Distributed TETware.
**	We have made tet_exec() and tet_error() async-signal safe (with
**	some loss of functionality) when called from the child of a
**	multi-threaded process.
**	If a test case attempts to call any other API function, the API
**	prints a message to stderr and exits.
**
**	All this is quite inconvenient, particularly when not all real-world
**	pthread implementations actually require it.
**	If TETware is compiled with -DTET_UNRESTRICTED_POSIX_THREADS,
**	these restrictions are not enforced.
*/

/* TET_UNRESTRICTED_POSIX_THREADS implies TET_POSIX_THREADS */
#  if defined(TET_UNRESTRICTED_POSIX_THREADS) && !defined(TET_POSIX_THREADS)
#    define TET_POSIX_THREADS	1
#  endif

/* it's more useful in the code to have TET_STRICT_POSIX_THREADS defined */
#  if defined(TET_POSIX_THREADS) && !defined(TET_UNRESTRICTED_POSIX_THREADS)
#    define TET_STRICT_POSIX_THREADS
#  endif


/*
**	macros to access threads functions
**
**	a set of macros and typedefs appears here for each threads
**	implementation that is supported by TETware;
**	each set of compatibility macros also defines TET_THR_COMPAT_MACROS
**	which is used to ensure that exactly one set of compatibility
**	macros is defined
**
**	note that the macros which appear should only be used internally
**	by TETware and (in some cases) only provide sufficient functionality
**	for use by TETware routines
*/

/* first some include files */

#  ifndef TET_SHLIB_BUILD_SCRIPT
#    ifdef _WIN32		/* -START-WIN32-CUT- */
#      include <windows.h>
#      include <errno.h>
#    else 		/* -END-WIN32-CUT- */
#      include <signal.h>
#      ifdef TET_POSIX_THREADS
#        include <sys/types.h>
#        include <pthread.h>
#      else
#        include <thread.h>
#        include <synch.h>
#      endif /* TET_POSIX_THREADS */
#    endif /* _WIN32 */	/* -WIN32-CUT-LINE- */
#  endif /* TET_SHLIB_BUILD_SCRIPT */


/*
** The following data types are defined:
**
**	tet_cond_t		condition variable
**	tet_mutex_t		mutex variable
**	tet_thread_key_t	thread key - used to access TLS
**	tet_thread_t		thread identifier
**	tet_timestruc_t		time structure (seconds and microseconds)
**
** The following macros are defined which look like functions.
** (Note that arguments with side effects won't always work as expected.)
** It's only meaningful to use values returned by macros not marked as "void".
** A "void" macro does not return a meaningful value and might not return a
** value at all!
**
**	int TET_COND_DESTROY(cvp)
**	int TET_COND_INIT(cvp)
**	int TET_COND_SIGNAL(cvp)
**	int TET_COND_TIMEDWAIT(cvp, mp, tvp)
**	void TET_MUTEX_INIT(mp)
**	void TET_MUTEX_DESTROY(mp)
**	void TET_MUTEX_LOCK(mp)
**	void TET_MUTEX_UNLOCK(mp)
**	int TET_THR_CREATE(func, arg, tp)
**	int TET_THR_EQUAL(a, b)
**	void TET_THR_EXIT(sp)
**	void TET_THR_GETSPECIFIC(key, vp)
**	int TET_THR_JOIN(tid, sp)
**	int TET_THR_KEYCREATE(kp)
**	int TET_THR_KILL(tid, sig)
**	tet_thread_t TET_THR_SELF()
**	void TET_THR_SETSPECIFIC(key, value)
**	int TET_THR_SIGSETMASK(how, sp, osp)
*/

/*
** the WIN32 API on Windows NT and Windows 95
*/

#  ifdef _WIN32	/* -START-WIN32-CUT- */

#    ifdef TET_THR_COMPAT_MACROS
#      error more than one threads API-specific macro has been defined
#    else
#      define TET_THR_COMPAT_MACROS
#    endif /* TET_THR_COMPAT_MACROS */

     /* typedefs for data items */
     /* tet_cond_t		not used on Win32 systems */
     typedef CRITICAL_SECTION	tet_mutex_t;
     typedef DWORD		tet_thread_key_t;
     typedef unsigned int	tet_thread_t;
     /* tet_timestruc_t		not used on Win32 systems */

     /* macros which access functions */
     /* TET_COND_DESTROY		not used on Win32 systems */
     /* TET_COND_INIT			not used on Win32 systems */
     /* TET_COND_SIGNAL 		not used on Win32 systems */
     /* TET_COND_TIMEDWAIT 		not used on Win32 systems */
#    define TET_MUTEX_INIT(mp)		InitializeCriticalSection((mp))
#    define TET_MUTEX_DESTROY(mp)	DeleteCriticalSection((mp))
#    define TET_MUTEX_LOCK(mp)		EnterCriticalSection((mp))
#    define TET_MUTEX_UNLOCK(mp)	LeaveCriticalSection((mp))
     /* TET_THR_CREATE			not used on Win32 systems */
#    define TET_THR_EQUAL(a, b)		((a) == (b))
     /* TET_THR_EXIT			not used on Win32 systems */
#    define TET_THR_GETSPECIFIC(key, vp) \
	(*(vp) = TlsGetValue((key)))
     /* TET_THR_JOIN			not used on Win32 systems */
#    define TET_THR_KEYCREATE(kp) \
	((*(kp) = TlsAlloc()) == 0xffffffff ? ENOMEM : 0)
     /* TET_THR_KILL			not used on Win32 systems */
#    define TET_THR_SELF()		GetCurrentThreadId()
#    define TET_THR_SETSPECIFIC(key, value) \
	((void) TlsSetValue((key), (value)))
     /* TET_THR_SIGSETMASK		not used on Win32 systems */


#  endif /* _WIN32 */	/* -END-WIN32-CUT- */


/*
** POSIX threads on UNIX-like systems
*/

#  ifdef TET_POSIX_THREADS

#    ifdef TET_THR_COMPAT_MACROS
#      error more than one threads API-specific macro has been defined
#    else
#      define TET_THR_COMPAT_MACROS
#    endif /* TET_THR_COMPAT_MACROS */

     /* typedefs for data items */
     typedef pthread_cond_t	tet_cond_t;
     typedef pthread_mutex_t	tet_mutex_t;
     typedef pthread_key_t	tet_thread_key_t;
     typedef pthread_t		tet_thread_t;
     typedef struct timespec	tet_timestruc_t;

     /* macros which access functions */
#    define TET_COND_DESTROY(cvp)	pthread_cond_destroy((cvp))
#    define TET_COND_INIT(cvp) \
	pthread_cond_init((cvp), (pthread_condattr_t *) 0)
#    define TET_COND_SIGNAL(cvp)	pthread_cond_signal((cvp))
#    define TET_COND_TIMEDWAIT(cvp, mp, tvp) \
	pthread_cond_timedwait((cvp), (mp), (tvp))
#    define TET_MUTEX_INIT(mp) \
	((void) pthread_mutex_init((mp), (pthread_mutexattr_t *) 0))
#    define TET_MUTEX_DESTROY(mp)	((void) pthread_mutex_destroy((mp)))
#    define TET_MUTEX_LOCK(mp)		((void) pthread_mutex_lock((mp)))
#    define TET_MUTEX_UNLOCK(mp)	((void) pthread_mutex_unlock((mp)))
#    define TET_THR_CREATE(func, arg, tp) \
	pthread_create((tp), (pthread_attr_t *) 0, (func), (arg))
#    define TET_THR_EQUAL(a, b)		pthread_equal((a), (b))
#    define TET_THR_EXIT(sp)		pthread_exit((sp))
#    define TET_THR_GETSPECIFIC(key, vp) \
	(*(vp) = pthread_getspecific((key)))
#    define TET_THR_JOIN(tid, sp)	pthread_join((tid), (sp))
#    define TET_THR_KEYCREATE(kp)	pthread_key_create((kp), TET_NULLFP)
#    define TET_THR_KILL(tid, sig)	pthread_kill((tid), (sig))
#    define TET_THR_SELF()		pthread_self()
#    define TET_THR_SETSPECIFIC(key, value) \
	((void) pthread_setspecific((key), (value)))
#    define TET_THR_SIGSETMASK(how, sp, osp) \
	pthread_sigmask((how), (sp), (osp))


#  endif /* TET_POSIX_THREADS */


/*
** UI threads - the default
**
** (this doesn't imply favouring any particular type of threads;
** it's just that TETware thread-safety was implemented using
** UI threads first - the other implementations came later!)
*/

#  ifndef TET_THR_COMPAT_MACROS

#  define TET_THR_COMPAT_MACROS

     /* typedefs for data items */
     typedef cond_t		tet_cond_t;
     typedef mutex_t		tet_mutex_t;
     typedef thread_key_t	tet_thread_key_t;
     typedef thread_t		tet_thread_t;
     typedef timestruc_t	tet_timestruc_t;

     /* macros which access functions */
#    define TET_COND_DESTROY(cvp)	cond_destroy((cvp))
#    define TET_COND_INIT(cvp) \
	cond_init((cvp), USYNC_THREAD, (void *) 0)
#    define TET_COND_SIGNAL(cvp)	cond_signal((cvp))
#    define TET_COND_TIMEDWAIT(cvp, mp, tvp) \
	cond_timedwait((cvp), (mp), (tvp))
#    define TET_MUTEX_INIT(mp) \
	((void) mutex_init((mp), USYNC_THREAD, (void *) 0))
#    define TET_MUTEX_DESTROY(mp)	((void) mutex_destroy((mp)))
#    define TET_MUTEX_LOCK(mp)		((void) mutex_lock((mp)))
#    define TET_MUTEX_UNLOCK(mp)	((void) mutex_unlock((mp)))
#    define TET_THR_CREATE(func, arg, tp) \
	thr_create((void *) 0, (size_t) 0, (func), (arg), 0L, (tp))
#    define TET_THR_EQUAL(a, b)		((a) == (b))
#    define TET_THR_EXIT(sp)		thr_exit((sp))
#    define TET_THR_GETSPECIFIC(key, vp) \
	(*(vp) = (void *) 0, (void) thr_getspecific((key), (vp)))
#    define TET_THR_JOIN(tid, sp)	thr_join((tid), (thread_t *) 0, (sp))
#    define TET_THR_KEYCREATE(kp)	thr_keycreate((kp), TET_NULLFP)
#    define TET_THR_KILL(tid, sig)	thr_kill((tid), (sig))
#    define TET_THR_SELF()		thr_self()
#    define TET_THR_SETSPECIFIC(key, value) \
	((void) thr_setspecific((key), (value)))
#    define TET_THR_SIGSETMASK(how, sp, osp) \
	thr_sigsetmask((how), (sp), (osp))

#  endif /* !TET_THR_COMPAT_MACROS */

   /* Keys for thread-specific data */
   TET_IMPORT_DATA(tet_thread_key_t, tet_errno_key);
   TET_IMPORT_DATA(tet_thread_key_t, tet_block_key);
   TET_IMPORT_DATA(tet_thread_key_t, tet_sequence_key);
#  ifndef _WIN32	/* -WIN32-CUT-LINE- */
     TET_IMPORT_DATA(tet_thread_key_t, tet_child_key);
     TET_IMPORT_DATA(tet_thread_key_t, tet_alarm_flag_key);
#  endif /* !_WIN32 */	/* -WIN32-CUT-LINE- */

   TET_IMPORT_DATA(tet_thread_t, tet_start_tid);

#  ifndef _WIN32	/* -WIN32-CUT-LINE- */

     /* macros for signal-safe mutex locking */
     /* These must be used in matching pairs as if they were braces */
#    define MTX_LOCK(mp) { sigset_t MTX_LOCK_oss; int MTX_LOCK_maskret = \
	TET_THR_SIGSETMASK(SIG_BLOCK, &tet_blockable_sigs, &MTX_LOCK_oss); \
	TET_MUTEX_LOCK(mp);
#    define MTX_UNLOCK(mp) TET_MUTEX_UNLOCK(mp); if (MTX_LOCK_maskret == 0) \
	(void) TET_THR_SIGSETMASK(SIG_SETMASK, &MTX_LOCK_oss, (sigset_t *)0); }

#  else /* _WIN32 */	/* -START-WIN32-CUT- */

#    define MTX_LOCK(mp) { TET_MUTEX_LOCK(mp);
#    define MTX_UNLOCK(mp) TET_MUTEX_UNLOCK(mp); }

#  endif /* _WIN32 */	/* -END-WIN32-CUT- */

   /* top-level API mutex with calls that can be nested */
#  define API_LOCK	tet_api_lock(1, srcFile, __LINE__)
#  define API_UNLOCK	tet_api_lock(0, srcFile, __LINE__)
   extern void tet_api_lock();

#  ifndef NEEDsrcFile
#    define NEEDsrcFile
#  endif

#else /* TET_THREADS */

   /* allow use of symbols without putting them in #ifdef TET_THREADS */
#  define MTX_LOCK(mp)		{
#  define MTX_UNLOCK(mp)	}
#  define API_LOCK
#  define API_UNLOCK

   extern long tet_sequence;

#endif /* TET_THREADS */

