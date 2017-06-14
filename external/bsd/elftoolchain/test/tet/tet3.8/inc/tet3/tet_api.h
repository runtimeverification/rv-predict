/*
 *	SCCS: @(#)tet_api.h	1.31 (99/11/15)
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

/*
 * Copyright 1990 Open Software Foundation (OSF)
 * Copyright 1990 Unix International (UI)
 * Copyright 1990 X/Open Company Limited (X/Open)
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose and without fee is hereby granted, provided
 * that the above copyright notice appear in all copies and that both that
 * copyright notice and this permission notice appear in supporting
 * documentation, and that the name of OSF, UI or X/Open not be used in 
 * advertising or publicity pertaining to distribution of the software 
 * without specific, written prior permission.  OSF, UI and X/Open make 
 * no representations about the suitability of this software for any purpose.  
 * It is provided "as is" without express or implied warranty.
 *
 * OSF, UI and X/Open DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, 
 * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO 
 * EVENT SHALL OSF, UI or X/Open BE LIABLE FOR ANY SPECIAL, INDIRECT OR 
 * CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF 
 * USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR 
 * OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR 
 * PERFORMANCE OF THIS SOFTWARE.
 */

/************************************************************************

SCCS:   	@(#)tet_api.h	1.31 99/11/15 TETware release 3.8
NAME:		'C' API header
PRODUCT:	TETware
AUTHOR:		Geoff Clare, UniSoft Ltd.
DATE CREATED:	23 July 1990
CONTENTS:

	struct tet_testlist definition
	result code values for use with tet_result()
	TET_NULLFP null function pointer for use with tet_fork()
	declarations/prototypes for all API interfaces

MODIFICATIONS:

	Geoff Clare, UniSoft Ltd., 10 Oct 1990
		Remove const keywords.

	Geoff Clare, UniSoft Ltd., 18 Oct 1990
		Add tet_pname.

	Geoff Clare, UniSoft Ltd., 28 Nov 1990
		Add tet_nosigreset.

	Geoff Clare, UniSoft Ltd., 21 June 1991
		Make tet_fork() prototype consistent with definition.

	DTET development - this file is derived from TET Release 1.10
	David G. Sawyer
	John-Paul Leyland
	UniSoft Ltd, June 1992

	Andrew Dingwall, UniSoft Ltd., October 1992
	Added DTET API function declarations.

	Denis McConalogue, UniSoft Limited, September 1993
	changed prototype for tet_sync()

	Andrew Dingwall, UniSoft Ltd., February 1994
	removed comment from #include line (for strict ANSI compliance?)

	Andrew Dingwall, UniSoft Ltd., October 1994
	added support for tet_msync() API function

	Geoff Clare, UniSoft Ltd., July 1996
	Changes for TETWare.

	Geoff Clare, UniSoft Ltd., Sept 1996
	Changes for TETWare-Lite and POSIX threads.

	Andrew Dingwall, UniSoft Ltd., June 1997
	changes to support the defined test case interface

	Geoff Clare, UniSoft Ltd., June 1997
	Changes for NT threads.

	Andrew Dingwall, UniSoft Ltd., December 1997
	protect against multiple inclusion

	Andrew Dingwall, UniSoft Ltd., August 1998
	Added support for shared libraries.

	Andrew Dingwall, UniSoft Ltd., August 1999
	Added sypport for other language APIs.

	Geoff Clare, UniSoft Ltd., October 1999
	Added declarations of tet_thr_join(), tet_pthread_join() and
	tet_pthread_detach().

	Andrew Dingwall, UniSoft Ltd., November 1999
	added #defines to support strict/unrestricted POSIX threads

************************************************************************/

#ifndef TET_API_H_INCLUDED
#define TET_API_H_INCLUDED

#ifdef __cplusplus
extern "C" {
#endif


/*
** support for shared API libraries on Win32 systems
*/

#if defined(_WIN32) && defined(TET_SHLIB)	/* -START-WIN32-CUT- */

   /*
   ** We are using a shared API library on a Win32 system.
   **
   ** The shared API library is linked with the multithreaded DLL version
   ** of the MS C runtime support library (msvcrt.lib).
   ** User code must link with this version as well.
   **
   ** Un-comment the following lines if you want to check that a test case
   ** is being compiled with the correct options.
   **
   ** #  ifndef _DLL
   ** #     error must compile with cc -MD when using a shared API library
   ** #  endif
   */

   /* TET_SHLIB implies TET_THREADS on Win32 platforms */
#  ifndef TET_THREADS
#    define TET_THREADS
#  endif

   /*
   ** TET_IMPORT is an attribute for a symbol that is imported from the
   ** shared library to a program.
   ** TET_EXPORT is an attribute for a symbol that is exported to the
   ** shared library from a program.
   ** Definitions used when compiling TETware source files appear in dtmac.h.
   ** These definitions are sufficient for user-level code.
   */
#  ifndef TET_IMPORT
     /*
     ** Un-comment the following to get a sanity check when compiling TETware
     ** source files.
     **
     ** #    ifdef TET_SHLIB_SOURCE
     ** #      error a TETware source file must include dtmac.h before tet_api.h
     ** #    endif
     */
#    define TET_IMPORT	_declspec(dllimport)
#  endif

#else	/* -END-WIN32-CUT- */

   /* not using a shared API library on a Win32 system */
#  ifndef TET_IMPORT
#    define TET_IMPORT
#  endif

#endif	/* -WIN32-CUT-LINE- */

#ifndef TET_EXPORT
#  define TET_EXPORT
#endif

/*
** TET_EXPORT_DATA is used to declare a data item that is exported from
** a program to a shared library.
** TET_IMPORT_DATA, TET_IMPORT_ARRAY, TET_IMPORT_FUNC and TET_IMPORT_FUNC_PTR
** are used to declare a data item, array, function or function pointer that
** is imported by a program from a shared library.
** Definitions used when compiling TETware source files appear in dtmac.h.
** These definitions are sufficient for user-level code.
*/
#ifndef TET_EXPORT_DATA
#  define TET_EXPORT_DATA(TYPE, NAME) \
	TET_EXPORT extern TYPE NAME
#endif
#ifndef TET_IMPORT_DATA
#  define TET_IMPORT_DATA(TYPE, NAME) \
	TET_IMPORT extern TYPE NAME
#endif
#ifndef TET_IMPORT_ARRAY
#  define TET_IMPORT_ARRAY(TYPE, NAME, DIM) \
	TET_IMPORT extern TYPE NAME DIM
#endif
#ifndef TET_IMPORT_FUNC
#  define TET_IMPORT_FUNC(TYPE, NAME, ARGS) \
	TET_IMPORT extern TYPE NAME ARGS
#endif
#ifndef TET_IMPORT_FUNC_PTR
#  define TET_IMPORT_FUNC_PTR(TYPE, NAME, ARGS) \
	TET_IMPORT extern TYPE (*NAME) ARGS
#endif

/*
** Support for strict/unrestricted POSIX threads.
**
** Users should only define TET_THREADS or TET_POSIX_THREADS.
** TET_UNRESTRICTED_POSIX_THREADS implies TET_POSIX_THREADS, so we define
** TET_POSIX_THREADS here in case some user code has been compiled with
** TET_UNRESTRICTED_POSIX_THREADS defined instead.
*/
#if defined(TET_UNRESTRICTED_POSIX_THREADS) && !defined(TET_POSIX_THREADS)
#  define TET_POSIX_THREADS		1
#endif

/* the following are needed for types used in declarations */
#include <sys/types.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
   typedef int pid_t;
#endif		/* -END-WIN32-CUT- */
#define TET_PID_T_DEFINED
#if defined(__STDC__) || defined(__cplusplus) || defined(_WIN32)
#  include <time.h>
#  include <stdarg.h>
#endif
#ifdef TET_POSIX_THREADS
#  include <pthread.h>
#else /* !TET_POSIX_THREADS */
#  ifdef TET_THREADS
#    ifndef _WIN32
#      include <thread.h>
#    endif
#  endif /* TET_THREADS */
#endif /* !TET_POSIX_THREADS */


/* values of the standard result codes - may be passed to tet_result() */
#define TET_PASS	0
#define TET_FAIL	1
#define TET_UNRESOLVED	2
#define TET_NOTINUSE	3
#define TET_UNSUPPORTED	4
#define TET_UNTESTED	5
#define TET_UNINITIATED	6
#define TET_NORESULT	7


/* tet_errno values, may be used to index into tet_errlist[] */
#define TET_ER_OK		0	/* ok  success */
#define TET_ER_ERR		1	/* general error code */
#define TET_ER_MAGIC		2	/* bad magic number */
#define TET_ER_LOGON		3	/* not logged on */
#define TET_ER_RCVERR		4	/* receive message error */
#define TET_ER_REQ		5	/* unknown request code */
#define TET_ER_TIMEDOUT		6	/* request timed out */
#define TET_ER_DUPS		7	/* request contained duplicate IDs */
#define TET_ER_SYNCERR		8	/* sync completed unsuccessfully */
#define TET_ER_INVAL		9	/* invalid request parameter */
#define TET_ER_TRACE		10	/* tracing not configured */
#define TET_ER_WAIT		11	/* process not terminated */
#define TET_ER_XRID		12	/* bad xrid in xresd request */
#define TET_ER_SNID		13	/* bad snid in syncd request */
#define TET_ER_SYSID		14	/* sysid not in system name list */
#define TET_ER_INPROGRESS	15	/* event in progress */
#define TET_ER_DONE		16	/* event finished or already happened */
#define TET_ER_CONTEXT		17	/* request out of context */
#define TET_ER_PERM		18	/* priv request/kill error */
#define TET_ER_FORK		19	/* can't fork */
#define TET_ER_NOENT		20	/* no such file or directory */
#define TET_ER_PID		21	/* no such process */
#define TET_ER_SIGNUM		22	/* bad signal number */
#define TET_ER_FID		23	/* bad file id */
#define TET_ER_INTERN		24	/* server internal error */
#define TET_ER_ABORT		25	/* abort TCM on TP end */
#define TET_ER_2BIG		26	/* argument list too long */


/*
**	function prototype macros
*/

#if defined(__STDC__) || defined(__cplusplus) || defined(_WIN32)
#  ifndef TET_PROTOTYPES
#    define TET_PROTOTYPES
#  endif
#endif

#ifdef TET_PROTOTYPES
#  define TET_PROTOLIST(list)	list
#else
#  define TET_PROTOLIST(list)	()
#endif


/* NULL function pointer - may be used as an argument to tet_fork() */
#define TET_NULLFP	((void (*) ()) 0)


#ifndef TET_LITE /* -START-LITE-CUT- */

   /*
   ** sync requests and sync error reporting
   */

   /* structure used in tet_msync() and tet_remsync() calls */
   struct tet_synmsg {
   	char *tsm_data;	/* ptr to sync message data buffer */
   	int tsm_dlen;	/* no of bytes in sm_data */
   	int tsm_sysid;	/* id of system sending sync message data */
   	int tsm_flags;	/* flags - see below */
   };

   /* values for tsm_flags (a bit field) */
#  define TET_SMSNDMSG	001	/* system is sending message data */
#  define TET_SMRCVMSG	002	/* system is receiving message data */
#  define TET_SMDUP	004	/* more than one system attempted to send */
#  define TET_SMTRUNC	010	/* message data was truncated */

#  define TET_SMMSGMAX	1024	/* maximum size of a tet_msync message -
   				   size must be expressable in 12 bits
   				   (see ST_COUNTMASK in ldst.h) */

   /* sync votes */
#  define TET_SV_YES		1
#  define TET_SV_NO		2

   /* structure of an element in the array describing sync status
      that is passed by the API to the sync error reporting function */
   struct tet_syncstat {
   	int tsy_sysid;	/* system ID */
   	int tsy_state;	/* sync state */
   };

   /* values for tsy_state */
#  define TET_SS_NOTSYNCED	1	/* sync request not received */
#  define TET_SS_SYNCYES	2	/* system voted YES */
#  define TET_SS_SYNCNO		3	/* system voted NO */
#  define TET_SS_TIMEDOUT	4	/* system timed out */
#  define TET_SS_DEAD		5	/* process exited */


   /* structure of the data item that is filled in by tet_getsysbyid() */
#  define TET_SNAMELEN	32		/* maximum system name length */
   struct tet_sysent {
   	int ts_sysid;			/* system ID */
   	char ts_name[TET_SNAMELEN];	/* system name */
   };

#endif /* -END-LITE-CUT- */


/*
**	declarations of public functions provided by the API
*/

/* functions in TETware-Lite and in Distrubuted TETware */
TET_IMPORT_FUNC(void, tet_delete, TET_PROTOLIST((int, char *)));
TET_IMPORT_FUNC(void, tet_exit, TET_PROTOLIST((int)));
TET_IMPORT_FUNC(char *, tet_getvar, TET_PROTOLIST((char *)));
TET_IMPORT_FUNC(void, tet_infoline,  TET_PROTOLIST((char *)));
TET_IMPORT_FUNC(int, tet_kill, TET_PROTOLIST((pid_t, int)));
TET_IMPORT_FUNC(void, tet_logoff, TET_PROTOLIST((void)));
TET_IMPORT_FUNC(int, tet_minfoline, TET_PROTOLIST((char **, int)));
TET_IMPORT_FUNC(int, tet_printf, TET_PROTOLIST((char *, ...)));
TET_IMPORT_FUNC(char *, tet_reason, TET_PROTOLIST((int)));
TET_IMPORT_FUNC(int, tet_remgetlist, TET_PROTOLIST((int **)));
TET_IMPORT_FUNC(int, tet_remgetsys, TET_PROTOLIST((void)));
TET_IMPORT_FUNC(void, tet_result, TET_PROTOLIST((int)));
TET_IMPORT_FUNC(void, tet_setblock, TET_PROTOLIST((void)));
TET_IMPORT_FUNC(void, tet_setcontext, TET_PROTOLIST((void)));
TET_IMPORT_FUNC(pid_t, tet_spawn, TET_PROTOLIST((char *, char **, char **)));
TET_IMPORT_FUNC(int, tet_vprintf, TET_PROTOLIST((char *, va_list)));
TET_IMPORT_FUNC(int, tet_wait, TET_PROTOLIST((pid_t, int *)));
#ifndef _WIN32
   TET_IMPORT_FUNC(int, tet_exec, TET_PROTOLIST((char *, char *[], char *[])));
   TET_IMPORT_FUNC(int, tet_fork,
	TET_PROTOLIST((void (*) TET_PROTOLIST((void)),
	void (*) TET_PROTOLIST((void)), int, int)));
#endif /* !_WIN32 */

/* functions only in Distributed TETware */
#ifndef TET_LITE /* -START-LITE-CUT- */
   TET_IMPORT_FUNC(int, tet_getsysbyid,
	TET_PROTOLIST((int, struct tet_sysent *)));
   TET_IMPORT_FUNC(int, tet_msync,
	TET_PROTOLIST((long, int *, int, struct tet_synmsg *)));
   TET_IMPORT_FUNC(int, tet_remsync,
	TET_PROTOLIST((long, int *, int, int, int, struct tet_synmsg *)));
   TET_IMPORT_FUNC(int, tet_remtime, TET_PROTOLIST((int, time_t *)));
   TET_IMPORT_FUNC(int, tet_sync, TET_PROTOLIST((long, int *, int)));
   TET_IMPORT_FUNC(void, tet_syncreport,
	TET_PROTOLIST((long, struct tet_syncstat *, int)));
#  if !defined(TET_THREADS) && !defined(TET_POSIX_THREADS)
     TET_IMPORT_FUNC(int, tet_remexec, TET_PROTOLIST((int, char *, char **)));
     TET_IMPORT_FUNC(int, tet_remkill, TET_PROTOLIST((int)));
     TET_IMPORT_FUNC(int, tet_remwait, TET_PROTOLIST((int, int, int *)));
#  endif /* !THREADS */
#endif /* -END-LITE-CUT- */

/* functions only in the Thread-safe API */
#if defined(TET_THREADS) || defined(TET_POSIX_THREADS)
#  ifdef TET_POSIX_THREADS
     TET_IMPORT_FUNC(int, tet_pthread_create,
	TET_PROTOLIST((pthread_t *, pthread_attr_t *,
	void *(*) TET_PROTOLIST((void *)), void *, int)));
     TET_IMPORT_FUNC(int, tet_pthread_join, TET_PROTOLIST((pthread_t, void **)));
     TET_IMPORT_FUNC(int, tet_pthread_detach, TET_PROTOLIST((pthread_t)));
#  else /* !TET_POSIX_THREADS */
#    ifndef _WIN32	/* -WIN32-CUT-LINE- */
       TET_IMPORT_FUNC(int, tet_thr_create,
	TET_PROTOLIST((void *, size_t, void *(*) TET_PROTOLIST((void *)),
	void *, long, thread_t *, int)));
       TET_IMPORT_FUNC(int, tet_thr_join, TET_PROTOLIST((thread_t, void **)));
#    else		/* -START-WIN32-CUT- */
       TET_IMPORT_FUNC(unsigned long, tet_beginthreadex,
	(void *, unsigned int, unsigned int (__stdcall *) (void *), void *,
	unsigned int, unsigned int *, int));
#    endif		/* -END-WIN32-CUT- */
#  endif /* !TET_POSIX_THREADS */
#  ifndef _WIN32
     TET_IMPORT_FUNC(int, tet_fork1,
	TET_PROTOLIST((void (*) TET_PROTOLIST((void)),
	void (*) TET_PROTOLIST((void)), int, int)));
#  endif /* !_WIN32 */
#endif /* THREADS */


/*
**	declarations of public data items provided by the API
*/

/* the following are imported from the API library */
#if defined(TET_THREADS) || defined(TET_POSIX_THREADS)
#  ifndef _WIN32
#    define tet_child (*tet_thr_child())
     TET_IMPORT_FUNC(pid_t *, tet_thr_child, TET_PROTOLIST((void)));
#  endif /* !_WIN32 */
#  define tet_errno (*tet_thr_errno())
   TET_IMPORT_FUNC(int *, tet_thr_errno, TET_PROTOLIST((void)));
#else /* !THREADS */
#  ifndef _WIN32
     TET_IMPORT_DATA(pid_t, tet_child);
#  endif /* !_WIN32 */
   TET_IMPORT_DATA(int, tet_errno);
#endif /* !THREADS */

TET_IMPORT_ARRAY(char *, tet_errlist, []);
TET_IMPORT_DATA(int, tet_nerr);
#ifndef TET_LITE /* -START-LITE-CUT- */
  TET_IMPORT_FUNC_PTR(void, tet_syncerr,
	TET_PROTOLIST((long, struct tet_syncstat *, int)));
#endif /* -END-LITE-CUT- */

/* the following are in the TCM */
extern int tet_nosigreset;

/* the following are in the TCM and must be "exported" to the API library */
TET_IMPORT_DATA(char *, tet_pname);
TET_IMPORT_DATA(int, tet_thistest);


/*
**	declarations used by the interface between the TCM and the test case
**
**	these items must be provided by the user
*/

/* the test case startup and cleanup functions */
extern void (*tet_startup)();
extern void (*tet_cleanup)();

/* and EITHER this data structure */
struct tet_testlist {
	void (*testfunc)();
	int icref;
};
extern struct tet_testlist tet_testlist[];

/* OR all of these test case interface functions */
extern int tet_getmaxic TET_PROTOLIST((void));
extern int tet_getminic TET_PROTOLIST((void));
extern int tet_gettestnum TET_PROTOLIST((int, int));
extern int tet_gettpcount TET_PROTOLIST((int));
extern int tet_invoketp TET_PROTOLIST((int, int));
extern int tet_isdefic TET_PROTOLIST((int));


/*
**	declaration of the interface between an executed program and the
**	child process controller
**
**	this item must be provided by the user
*/

extern int tet_main TET_PROTOLIST((int, char *[]));


/*
**	declarations of other public interfaces provided by the API
**
**	these interfaces should not be used by C/C++ test cases but are
**	provided to support other language APIs that use the C API
*/

/* the entry points into the TCM and the child process controllers */
extern void tet_tcm_main TET_PROTOLIST((int, char **));
extern void tet_tcmchild_main TET_PROTOLIST((int, char **));
#ifndef TET_LITE	/* -START-LITE-CUT- */
  extern void tet_tcmrem_main TET_PROTOLIST((int, char **));
#endif			/* -END-LITE-CUT- */

/* the block and sequence numbers used when the API writes to the journal */
#if defined(TET_THREADS) || defined(TET_POSIX_THREADS)
   TET_IMPORT_FUNC(long *, tet_thr_block, TET_PROTOLIST((void)));
#  define tet_block	(*tet_thr_block())
   TET_IMPORT_FUNC(long *, tet_thr_sequence, TET_PROTOLIST((void)));
#  define tet_sequence	(*tet_thr_sequence())
   TET_IMPORT_DATA(long, tet_next_block);
#else
   TET_IMPORT_DATA(long, tet_block);
   TET_IMPORT_DATA(long, tet_sequence);
#endif

/* the TCM/API error reporting functions */
TET_IMPORT_FUNC(void, tet_merror, TET_PROTOLIST((int, char **, int)));
TET_IMPORT_FUNC(void, tet_error, TET_PROTOLIST((int, char *)));


#ifdef __cplusplus
}
#endif

#endif /* TET_API_H_INCLUDED */

