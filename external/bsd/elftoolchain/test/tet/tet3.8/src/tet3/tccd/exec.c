/*
 *      SCCS:  @(#)exec.c	1.16 (02/01/18) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1992 X/Open Company Limited
 * (C) Copyright 1994 UniSoft Limited
 *
 * All rights reserved.  No part of this source code may be reproduced,
 * stored in a retrieval system, or transmitted, in any form or by any
 * means, electronic, mechanical, photocopying, recording or otherwise,
 * except as stated in the end-user licence agreement, without the prior
 * permission of the copyright owners.
 *
 * X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
 * the UK and other countries.
 */

#ifndef lint
static char sccsid[] = "@(#)exec.c	1.16 (02/01/18) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)exec.c	1.16 02/01/18 TETware release 3.8
NAME:		exec.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	exec request processing functions

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., November 1993
	enhancements for FIFO transport interface
	make sure that a user exec is relative to the current directory

	Andrew Dingwall, UniSoft Ltd., August 1996
	changed for tetware-style configuration

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <sys/types.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <direct.h>
#else		/* -END-WIN32-CUT- */
#  include <unistd.h>
#  include <setjmp.h>
#  include <signal.h>
#  include <sys/wait.h>
#endif		/* -WIN32-CUT-LINE- */
#include <errno.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "sptab.h"
#include "etab.h"
#include "ltoa.h"
#include "error.h"
#include "globals.h"
#include "avmsg.h"
#include "valmsg.h"
#include "bstring.h"
#include "tslib.h"
#include "servlib.h"
#include "dtetlib.h"
#include "tccd.h"
#include "tcclib.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* static function declarations */
static int op_e2 PROTOLIST((struct ptab *));
static int op_e3 PROTOLIST((struct ptab *, struct etab *));
static int op_w2 PROTOLIST((struct ptab *));
static void tes2 PROTOLIST((int));
static int waitforchild PROTOLIST((int, int));

#ifndef _WIN32	/* -WIN32-CUT-LINE- */
  static SIG_FUNC_T catchalarm PROTOLIST((int));
  static int wfc2 PROTOLIST((int, int, time_t));
#endif		/* -WIN32-CUT-LINE- */


/*
**	op_exec() - fork and exec a process
**
**	if the exec failed, a subsequent op_wait will show a process
**	exit status of ~0 (255)
*/

void op_exec(pp)
register struct ptab *pp;
{
	if ((pp->ptm_rc = op_e2(pp)) == ER_OK) {
		((struct valmsg *) pp->ptm_data)->vm_nvalue = OP_EXEC_NVALUE;
		pp->ptm_mtype = MT_VALMSG;
		pp->ptm_len = valmsgsz(OP_EXEC_NVALUE);
	}
	else {
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
	}
}

/*
**	op_e2() - extend the op_exec() processing
**
**	return ER_OK if successful or other ER_* error code on error
*/

static int op_e2(pp)
register struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;
	register struct sptab *sp = (struct sptab *) pp->pt_sdata;
	register struct etab *ep;
	register int rc;

	/* do a sanity check on the request message */
	if (OP_EXEC_NARG(mp) < 1 || !AV_PATH(mp) || !AV_ARG(mp, 0))
		return(ER_INVAL);

	/* put ti and ts args in the environment for exec */
	if (
		tet_ti_tcmputenv(pp->ptr_sysid, AV_SNID(mp), AV_XRID(mp),
			sp->sp_snames, sp->sp_nsname) < 0 ||
		tet_ts_tcmputenv() < 0
	) {
		return(ER_ERR);
	}

	/* get an etab element to hold the exec'd process details */
	if ((ep = etalloc()) == (struct etab *) 0)
		return(ER_ERR);

	if ((rc = op_e3(pp, ep)) == ER_OK)
		etadd(ep);
	else
		etfree(ep);

	return(rc);
}

/*
**	op_e3() - extend the op_exec() processing some more
**
**	return ER_OK if successful or other ER_* error code on error
*/

static int op_e3(pp, ep)
struct ptab *pp;
struct etab *ep;
{
	register char *dp = pp->ptm_data;
	register int rc;
	char *path;
	int flag, pid;
	register char *p;
	char *dir, *tet_execute;
	int done;

#define mp	((struct avmsg *) dp)		/* ptr to request message */

	/* make sure that the buffer is big enough for the reply message */
	if (BUFCHK(&pp->ptm_data, &pp->pt_mdlen, valmsgsz(OP_EXEC_NVALUE)) < 0)
		return(ER_ERR);
	dp = pp->ptm_data;


	/*
	** change directory if so required:
	**	for user execs, cd to TET_EXECUTE if one has been specified,
	**		otherwise to TET_ROOT
	*/
	dir = (char *) 0;
	switch (AV_FLAG(mp)) {
	case AV_EXEC_USER:
		tet_execute = getenv("TET_EXECUTE");
		if (tet_execute && *tet_execute) {
			TRACE2(tet_Ttccd, 4, "chdir to TET_EXECUTE: \"%s\"",
				tet_execute);
			dir = tet_execute;
		}
		else if (tet_root[0]) {
			TRACE2(tet_Ttccd, 4, "chdir to TET_ROOT: \"%s\"",
				tet_root);
			dir = tet_root;
		}
		break;
	}
	if (dir && CHDIR(dir) < 0)
		error(errno, "can't chdir to", dir);

	/* make sure that a user exec is relative to the current directory */
	switch (AV_FLAG(mp)) {
	case AV_EXEC_USER:
		p = AV_PATH(mp);
		do {
			done = 1;
			while (isdirsep(*p)) {
				p++;
				done = 0;
			}
			while (*p == '.' && isdirsep(*(p + 1))) {
				p += 2;
				done = 0;
			}
			while (*p == '.' && *(p + 1) == '.' && isdirsep(*(p + 2))) {
				p += 3;
				done = 0;
			}
		} while (!done);
		path = p;
		break;
	default:
		path = AV_PATH(mp);
		break;
	}

	/* add a null-terminating arg (just to make sure) */
	AV_ARG(mp, OP_EXEC_NARG(mp) - 1) = (char *) 0;

	/* translate the flag argument for tcf_exec() */
	switch (AV_FLAG(mp)) {
	case AV_EXEC_TEST:
		flag = TCF_EXEC_TEST;
		break;
	case AV_EXEC_USER:
		flag = TCF_EXEC_USER;
		break;
	default:
		flag = TCF_EXEC_MISC;
		break;
	}

	/* call the tcc action function to do the exec */
	rc = tcf_exec(path, &AV_ARG(mp, 0), AV_OUTFILE(mp), AV_SNID(mp),
		flag, &pid);

#undef mp

#define rp	((struct valmsg *) dp)		/* ptr to reply message */

	if (rc == ER_OK) {
		VM_PID(rp) = (long) pid;
		ep->et_ptab = pp;
		ep->et_pid = pid;
		ep->et_state = ES_RUNNING;
	}

	return(rc);

#undef rp

}


#ifndef _WIN32	/* -WIN32-CUT-LINE- */

/*
**	tcc_exec_signals() - restore default signal handling in the 
**		child process before an exec
**
**	this function is called from tcf_exec() in tcclib
*/

void tcc_exec_signals()
{
	tes2(SIGHUP);
	tes2(SIGINT);
	tes2(SIGQUIT);
	tes2(SIGTSTP);
	tes2(SIGTTIN);
	tes2(SIGTTOU);
}

static void tes2(sig)
int sig;
{
	struct sigaction sa;

	sa.sa_handler = SIG_DFL;
	sa.sa_flags = 0;
	(void) sigemptyset(&sa.sa_mask);
	(void) sigaction(sig, &sa, (struct sigaction *) 0);
}

#endif		/* -WIN32-CUT-LINE- */


/*
**	op_wait() - wait for a process
**
**	some possible message returns are:
**		wait timed out:				rc = ER_TIMEDOUT
**		timeout = 0 and proc not terminated:	rc = ER_WAIT
**		proc not started by OP_EXEC:		rc = ER_PID
**		ok with status in VM_STATUS:		rc = ER_OK
*/

void op_wait(pp)
register struct ptab *pp;
{
	if ((pp->ptm_rc = op_w2(pp)) == ER_OK) {
		((struct valmsg *) pp->ptm_data)->vm_nvalue = OP_WAIT_NVALUE;
		pp->ptm_mtype = MT_VALMSG;
		pp->ptm_len = valmsgsz(OP_WAIT_NVALUE);
	}
	else {
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
	}
}

/*
**	op_w2() - extend the op_wait() processing
**
**	return ER_OK if successful or other ER_* error code on error
*/

static int op_w2(pp)
register struct ptab *pp;
{
	register struct valmsg *mp = (struct valmsg *) pp->ptm_data;
	register struct etab *ep;
	register int rc;

	TRACE3(tet_Ttccd, 4, "op_wait: pid = %s, timeout = %s",
		tet_l2a(VM_PID(mp)), tet_l2a(VM_WTIMEOUT(mp)));

	/* do a sanity check on the request message */
	if ((int) mp->vm_nvalue != OP_WAIT_NVALUE)
		return(ER_INVAL);

	/* make sure that the buffer is big enough for the reply */
	if (BUFCHK(&pp->ptm_data, &pp->pt_mdlen, valmsgsz(OP_WAIT_NVALUE)) < 0)
		return(ER_ERR);
	mp = (struct valmsg *) pp->ptm_data;

	/* find exec table entry for this process */
	if ((ep = etfind((int) VM_PID(mp))) == (struct etab *) 0 ||
		ep->et_ptab != pp) {
			TRACE1(tet_Ttccd, 4, "can't find proc in exec table");
			return(ER_PID);
	}

	/* wait for the process to terminate if necessary */
	if (ep->et_state != ES_TERMINATED) {
		rc = waitforchild((int) VM_PID(mp), (int) VM_WTIMEOUT(mp));
		TRACE2(tet_Ttccd, 4, "waitforchild() returned %s",
			tet_ptrepcode(rc));
	}
	else {
		rc = ER_OK;
		TRACE1(tet_Ttccd, 4, "proc already terminated");
	}

	/* handle return code */
	switch (rc) {
	case ER_WAIT:
	case ER_TIMEDOUT:
		break;
	case ER_OK:
		VM_STATUS(mp) = (long) ((unsigned) ep->et_status);
		TRACE3(tet_Ttccd, 4, "op_wait: return status = %s, signal %s",
			tet_l2a((VM_STATUS(mp) >> 8) & 0xff),
			tet_l2a(VM_STATUS(mp) & 0xff));
		/* fall through */
	/* case ER_PID: */
	default:
		etrm(ep);
		etfree(ep);
	}

	TRACE2(tet_Ttccd, 4, "op_w2() returning %s", tet_ptrepcode(rc));
	return(rc);
}

/*
**	op_kill() - send signal to process
**
*/

void op_kill(pp)
register struct ptab *pp;
{
	register struct valmsg *mp = (struct valmsg *) pp->ptm_data;
	register struct etab *ep;
	register int signum, rc;

	TRACE3(tet_Ttccd, 4, "op_kill: pid = %s, signum = %s",
		tet_l2a(VM_PID(mp)), tet_l2a(VM_SIGNUM(mp)));

	/* all reply messages contain no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the request message */
	if ((int) mp->vm_nvalue != OP_KILL_NVALUE) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	/* make sure that the process to be killed was started by
		the requester */
	if ((ep = etfind((int) VM_PID(mp))) == (struct etab *) 0 ||
		ep->et_ptab != pp) {
			TRACE1(tet_Ttccd, 4, "can't find proc in exec table");
			pp->ptm_rc = ER_PID;
			return;
	}

	/* map the machine-independent signal number to its local equivalent */
	if ((signum = tet_unmapsignal((int) VM_SIGNUM(mp))) < 0) {
		pp->ptm_rc = ER_SIGNUM;
		return;
	}

#ifndef NOTRACE
	if (signum != (int) VM_SIGNUM(mp)) {
		TRACE3(tet_Ttccd, 4, "unmapsig(%s) returns signal %s",
			tet_l2a(VM_SIGNUM(mp)), tet_i2a(signum));
	}
#endif

	/* do the kill */
	if (KILL((int) VM_PID(mp), signum) < 0)
		switch (errno) {
		case EINVAL:
			rc = ER_SIGNUM;
			break;
		case ESRCH:
			rc = ER_PID;
			break;
		case EPERM:
			rc = ER_PERM;
			break;
		default:
			error(errno, "kill failed on pid", tet_l2a(VM_PID(mp)));
			rc = ER_ERR;
			break;
		}
	else
		rc = ER_OK;

	pp->ptm_rc = rc;
}

/*
**	tet_ss_ptalloc() - allocate server-specific data element in a ptab
**		structure
**
**	return 0 if successful or -1 on error
*/

int tet_ss_ptalloc(pp)
struct ptab *pp;
{
	register struct sptab *sp;

	errno = 0;
	if ((sp = (struct sptab *) malloc(sizeof *sp)) == (struct sptab *) 0) {
		error(errno, "can't get memory for ss data", (char *) 0);
		pp->pt_sdata = (char *) 0;
		return(-1);
	}
	TRACE2(tet_Tbuf, 6, "allocate sptab = %s", tet_i2x(sp));
	bzero((char *) sp, sizeof *sp);

	sp->sp_snames = (int *) 0;
	sp->sp_nsname = 0;

	pp->pt_sdata = (char *) sp;

	return(0);
}

/*
**	tet_ss_ptfree() - free server-specific data element in a ptab structure
*/

void tet_ss_ptfree(pp)
struct ptab *pp;
{
	register struct sptab *sp = (struct sptab *) pp->pt_sdata;

	TRACE2(tet_Tbuf, 6, "free sptab = %s", tet_i2x(sp));

	if (sp) {
		if (sp->sp_snames) {
			TRACE2(tet_Tbuf, 6, "free snames = %s",
				tet_i2x(sp->sp_snames));
			free((char *) sp->sp_snames);
		}
		free((char *) sp);
		pp->pt_sdata = (char *) 0;
	}
}

/*
**	waitforchild() - wait for child processes to terminate, or until
**		timeout expires
**
**	return ER_OK if successful or other ER_* error code on error
*/

#ifdef _WIN32		/* -START-WIN32-CUT- */

static int waitforchild(pid, timeout)
int pid, timeout;
{
	int rc, status;
	register struct etab *ep;

	/* call the WIN32 tcc action function to do the wait */
	if ((rc = tcf_win32wait(pid, timeout, &status)) == ER_OK) {
		ep = etfind(pid);
		ASSERT(ep != (struct etab *) 0);
		ep->et_state = ES_TERMINATED;
		ep->et_status = status;
	}

	return(rc);
}

#else /* _WIN32 */		/* -END-WIN32-CUT- */

static sigjmp_buf wait_env;
static int wait_timedout;

static int waitforchild(pid, timeout)
int pid;
register int timeout;
{
	register int rc;
	volatile unsigned int alarm_save;
	volatile struct sigaction sig_save;
	register time_t interval, start;
	struct sigaction sa;

	/* this to keep gcc -Wall happy */
	bzero((char *) &sig_save, sizeof sig_save);
	alarm_save = 0;

	/* remember current time */
	start = time((time_t *) 0);

	/* remember alarm state and signal disposition */
	if (timeout > 0) {
		alarm_save = alarm(0);
		sa.sa_handler = catchalarm;
		sa.sa_flags = 0;
		(void) sigemptyset(&sa.sa_mask);
		(void) sigaction(SIGALRM, &sa, (struct sigaction *) &sig_save);
	}

	/* mark current place for timeout processing */
	wait_timedout = 0;
	if (sigsetjmp(wait_env, 1) == 0)
		rc = wfc2(pid, timeout, start);
	else
		rc = ER_TIMEDOUT;

	/*
	** restore previous alarm and signal disposition,
	** allowing for elapsed time spent in wfc2()
	*/
	if (timeout > 0) {
		if (alarm_save != 0) {
			interval = time((time_t *) 0) - start;
			if ((unsigned) interval < alarm_save - 1)
				alarm_save -= (unsigned) interval;
			else
				alarm_save = 2;
		}
		(void) alarm(alarm_save);
		(void) sigaction(SIGALRM, (struct sigaction *) &sig_save,
			(struct sigaction *) 0);
	}

	return(rc);
}

/*
**	wfc2() - extend the waitforchild() processing
**
**	return ER_OK if successful or other ER_* error code on error
**	or if the timeout expires
*/

static int wfc2(pid, timeout, start)
int pid;
register int timeout;
time_t start;
{
	register struct etab *ep;
	register int rc, tleft, save_errno;
	int status;

	/* reap all terminated children until pid is found, storing
		exit status in the exec table */
	tleft = timeout > 0 ? timeout : 0;
	for (;;) {
		status = 0;
		if (timeout > 0)
			(void) alarm((unsigned) TET_MAX(tleft, 2));
		rc = tet_dowait3(&status, timeout ? 0 : WNOHANG);
		save_errno = errno;
		if (timeout > 0)
			(void) alarm(0);
		TRACE4(tet_Ttccd, 4,
			"wait3 returned pid = %s, status = %s, signal %s",
			tet_i2a(rc), tet_i2a((status >> 8) & 0xff),
			tet_i2a(status & 0xff));
		if (!rc)
			return(ER_WAIT);
		else if (rc < 0)
			switch (save_errno) {
			case ECHILD:
				return(ER_PID);
			case EINTR:
				if (wait_timedout) {
					wait_timedout = 0;
					return(ER_TIMEDOUT);
				}
				else if ((rc = tet_maperrno(save_errno)) != ER_ERR)
					return(rc);
				/* else fall through */
			default:
				error(save_errno, "wait failed for pid",
					tet_i2a(pid));
				return(ER_ERR);
			}
		if ((ep = etfind(rc)) != (struct etab *) 0) {
			ep->et_state = ES_TERMINATED;
			ep->et_status = tet_mapstatus(status);
		}
		if (rc == pid)
			return(ER_OK);
		if (timeout > 0 && (tleft -= time((time_t *) 0) - start) < 1)
			return(ER_TIMEDOUT);
	}
}

/*
**	catchalarm() - SIGALRM signal handler
*/

/* ARGSUSED */
static SIG_FUNC_T catchalarm(sig)
int sig;
{
	struct sigaction sa;
	
	sa.sa_handler = SIG_IGN;
	sa.sa_flags = 0;
	(void) sigemptyset(&sa.sa_mask);
	(void) sigaction(SIGALRM, &sa, (struct sigaction *) 0);

	wait_timedout = 1;
	siglongjmp(wait_env, 1);
}

#endif /* _WIN32 */		/* -WIN32-CUT-LINE- */

