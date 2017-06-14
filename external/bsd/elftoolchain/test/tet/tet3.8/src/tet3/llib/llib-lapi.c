/*
 *      SCCS:  @(#)llib-lapi.c	1.5 (96/11/04) 
 *
 * (C) Copyright 1994 UniSoft Ltd., London, England
 *
 * All rights reserved.  No part of this source code may be reproduced,
 * stored in a retrieval system, or transmitted, in any form or by any
 * means, electronic, mechanical, photocopying, recording or otherwise,
 * except as stated in the end-user licence agreement, without the prior
 * permission of the copyright owners.
 */

/************************************************************************

SCCS:   	@(#)llib-lapi.c	1.5 96/11/04 TETware release 3.8
NAME:		llib-lapi.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	December 1993

DESCRIPTION:
	TETware API lint library

MODIFICATIONS:

	Geoff Clare, UniSoft Ltd., Oct 1996
	Changes for TETware.

************************************************************************/

/* LINTLIBRARY */

/* band-aid for non-posix systems */
#if defined(SVR2) || defined(BSD42) || defined(BSD43)
typedef int pid_t;
#endif

#ifndef __STDC__
#include <time.h>
#include <varargs.h>
#endif
#if defined(TET_THREADS) && !defined(TET_POSIX_THREADS)
#include <synch.h>
#endif
#include "tet_api.h"

#undef tet_child
pid_t	tet_child;
#undef tet_errno
int	tet_errno;
char *	tet_errlist[1];
int	tet_nerr;
void	(*tet_syncerr)();

void tet_delete(test_no, reason)
int test_no;
char *reason;
{
	;
}

int tet_exec(file, argv, envp)
char *file, *argv[], *envp[];
{
	return(0);
}

void tet_exit(status)
int status;
{
	;
}

int tet_fork(childproc, parentproc, waittime, exitvals)
void (*childproc)(), (*parentproc)();
int waittime, exitvals;
{
	return(0);
}

#if defined(TET_THREADS) || defined(TET_POSIX_THREADS)
int tet_fork1(childproc, parentproc, waittime, exitvals)
void (*childproc)(), (*parentproc)();
int waittime, exitvals;
{
	return(0);
}
#endif /* THREADS */

int tet_getsysbyid(sysid, sysp)
int sysid;
struct tet_sysent *sysp;
{
	return 0;
}

char *tet_getvar(name)
char *name;
{
	return((char *) 0);
}

void tet_infoline(data)
char *data;
{
	;
}

int tet_kill(pid, sig)
pid_t pid;
int sig;
{
	return 0;
}

void tet_logoff()
{
	;
}

int tet_minfoline(lines, nlines)
char **lines;
int nlines;
{
	return 0;
}

int tet_msync(syncptno, syncnames, waittime, msgp)
long syncptno;
int *syncnames, waittime;
struct tet_synmsg *msgp;
{
	return 0;
}

#ifdef __STDC__
/* PRINTFLIKE1 */ /* VARARGS1 */
int tet_printf(char *format, ...)
#else
/* PRINTFLIKE1 */ /* VARARGS1 */
int tet_printf(format)
char *format;
#endif
{
	return 0;
}

#ifdef TET_POSIX_THREADS
int tet_pthread_create(new_thread, attr, start_routine, arg, waittime)
pthread_t *new_thread;
pthread_attr_t *attr;
void *(*start_routine)();
void *arg;
int waittime;
{
	return 0;
}
#endif

char *tet_reason(test_no)
int test_no;
{
	return((char *) 0);
}

int tet_remexec(sysname, file, argv)
int sysname;
char *file, **argv;
{
	return(0);
}

int tet_remgetlist(sysnames)
int **sysnames;
{
	return(0);
}

int tet_remgetsys()
{
	return(0);
}

int tet_remkill(remoteid)
int remoteid;
{
	return(0);
}

int tet_remsync(syncptno, syncnames, nsyncname, waittime, vote, msgp)
long syncptno;
int *syncnames, nsyncname, waittime, vote;
struct tet_synmsg *msgp;
{
	return 0;
}

int tet_remtime(sysid, tp)
int sysid;
time_t *tp;
{
	return 0;
}

int tet_remwait(remoteid, waittime, statloc)
int remoteid, waittime, *statloc;
{
	return(0);
}

void tet_result(result)
int result;
{
	;
}

void tet_setblock()
{
	;
}

void tet_setcontext()
{
	;
}

pid_t tet_spawn(file, argv, envp)
char *file;
char *argv[];
char *envp[];
{
	return 0;
}

int tet_sync(syncptno, syncnames, waittime)
long syncptno;
int *syncnames, waittime;
{
	return(0);
}

void tet_syncreport(syncptno, statp, nsys)
long syncptno;
struct tet_syncstat *statp;
int nsys;
{
	;
}

#if defined(TET_THREADS) || defined(TET_POSIX_THREADS)
pid_t *tet_thr_child()
{
	return (pid_t *)0;
}
#endif

#if defined(TET_THREADS) || defined(TET_POSIX_THREADS)
int *tet_thr_errno()
{
	return (int *)0;
}
#endif

#if defined(TET_THREADS) && !defined(TET_POSIX_THREADS)
int tet_thr_create(stack_base, stack_size, start_routine, arg, flags,
		new_thread, waittime)
void *stack_base;
size_t stack_size;
void *(*start_routine)();
void *arg;
long flags;
thread_t *new_thread;
int waittime;
{
	return 0;
}
#endif

int tet_vprintf(format, ap)
char *format;
va_list ap;
{
	return 0;
}

int tet_wait(pid, statp)
pid_t pid;
int *statp;
{
	return 0;
}

#ifndef TET_API_ONLY

/* non-API functions and data: these go in llib-lapi.ln (for linting
   TETware source), but not in llib-ltcm.ln and llib-ltcmc.ln */

#if defined(TET_THREADS) || defined(TET_POSIX_THREADS)
void tet_api_lock(getlock, file, line)
int getlock;
char *file;
int line;
{
	;
}
void tet_cln_threads(signum)
int signum;
{
	;
}
#endif /* THREADS */

void tet_config()
{
	;
}

void tet_delreas(ntests)
int ntests;
{
	;
}

void tet_disconnect()
{
	;
}

void tet_error(errno_val, msg)
int errno_val;
char *msg;
{
	;
}

int tet_killw(child, timeout)
pid_t child;
unsigned int timeout;
{
	return 0;
}

#if defined(TET_THREADS) || defined(TET_POSIX_THREADS)
void tet_mtx_init()
{
	;
}
void tet_mtx_destroy()
{
	;
}
void tet_mtx_lock()
{
	;
}
void tet_mtx_unlock()
{
	;
}
#endif /* THREADS */

char * tet_signame(sig)
int sig;
{
	return (char *)0;
}

#if defined(TET_THREADS) || defined(TET_POSIX_THREADS)
long * tet_thr_block()
{
	return (long *)0;
}
long * tet_thr_sequence()
{
	return (long *)0;
}
void tet_thrtab_reset()
{
	;
}
#endif /* THREADS */

int	tet_combined_ok;
long	tet_activity;
long	tet_context;
#undef tet_block
long	tet_block;
#undef tet_sequence
long	tet_sequence;
#if defined(TET_THREADS) || defined(TET_POSIX_THREADS)
long		tet_next_block;
thread_key_t	tet_block_key;
thread_key_t	tet_sequence_key;
thread_key_t	tet_child_key;
thread_key_t	tet_errno_key;
mutex_t tet_top_mtx;
mutex_t tet_thrtab_mtx;
mutex_t tet_thrwait_mtx;
mutex_t tet_sigalrm_mtx;
mutex_t tet_alarm_mtx;
#endif /* THREADS */

#endif /* TET_API_ONLY */
