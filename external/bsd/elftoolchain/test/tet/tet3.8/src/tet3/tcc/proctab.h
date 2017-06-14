/*
 *	SCCS: @(#)proctab.h	1.7 (05/12/07)
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

SCCS:   	@(#)proctab.h	1.7 05/12/07 TETware release 3.8
NAME:		proctab.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	definitions related to the tcc process table

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., December 1997
	Added pr_distflag flag to the proctab execution context.

	Andrew Dingwall, UniSoft Ltd., March 2000
	Added extra toolstate flag values for use in engine_shutdown().

	Andrew Dingwall, The Open Group, March 2003
	Enhancement to copy source files to remote systems.

	Neil Moses, The Open Group, December 2005
	Added *pr_recon and pr_nrecon to hold details of reconnectable
	systems. Also added PRF_RECONNECT constant.

************************************************************************/

/*
**	tcc process table
**
**	when a scenario is being executed a proctab element is allocated
**	for each thread of control at each level in the scenario tree
**
**	proctab structures are NOT manipulated by the llist routines
*/

struct proctab {
	struct proctab *pr_rqforw;	/* next element on the runq */
	struct proctab *pr_rqback;	/* prev element on the runq */
	struct proctab *pr_parent;	/* element at previous level */
	struct proctab *pr_child;	/* first element at next level */
	struct proctab *pr_lforw;	/* next element at this level */
	struct proctab *pr_lback;	/* prev element at this level */
	long pr_magic;			/* magic number */
	int pr_state;			/* current state - see below */
	int pr_flags;			/* flags - see below */
	struct scentab *pr_scen;	/* scenario element being processed */
	struct scentab *pr_altscen;	/* alt scenario tree (for RANDOM) */
	char *pr_exiclist;		/* the IC list to use in EXEC mode */
	int pr_numtc;			/* no of TCs below here (for RANDOM) */
	time_t pr_starttime;		/* start time of timed_loop or exec */
	int pr_tcstate;			/* test case state - see below */
	int pr_currmode;		/* current mode of operation */
	int pr_activity;		/* activity counter for journal */
	int pr_jnlstatus;		/* exit status for journal */
	int pr_level;			/* level in the tree */
	char *pr_srclock;		/* source directory lock */
	char *pr_execlock;		/* execution directory lock */
	char *pr_tmpdir;		/* temporary execution directory */
	char *pr_outfile;		/* output capture file */
	char *pr_tetxres;		/* old-style tet_xres file */
	char *pr_tcedir;		/* testcase/tool execution directory */
	long pr_remid;			/* testcase/tool process id */
	int pr_exitcode;		/* testcase/tool exit status */
	int pr_toolstate;		/* testcase/tool state - see below */
	time_t pr_nextattn;		/* time for next attention */
	int pr_waitinterval;		/* interval between waits */
	int pr_waitcount;		/* no of waits to do after a kill */
	struct {
		int prc_modes;		/* modes of operation */
		int prc_loopcount;	/* loop counter (for REPEAT,
					   TIMED_LOOP and RANDOM) */
		int *prc_sys;		/* system list */
		int prc_nsys;		/* no of systems in the list */
		int *prc_recon;		/* list of reconnect sysids */
		int prc_nrecon;		/* number of reconnect sysids */
		FILE *prc_jfp;		/* journal file handle */
		char *prc_jfname;	/* journal file name */
#ifndef TET_LITE	/* -START-LITE-CUT- */
		long prc_snid;		/* sync ID */
		long prc_xrid;		/* xres ID */
		char *prc_xfname;	/* xresd file name */
		int prc_distflag;	/* test cases are distributed */
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
	} pr_context;
};

/* short names for members of pr_context */
#define pr_modes		pr_context.prc_modes
#define pr_loopcount		pr_context.prc_loopcount
#define pr_sys			pr_context.prc_sys
#define pr_nsys			pr_context.prc_nsys
#define pr_recon		pr_context.prc_recon
#define pr_nrecon		pr_context.prc_nrecon
#define pr_jfp			pr_context.prc_jfp
#define pr_jfname		pr_context.prc_jfname
#ifndef TET_LITE	/* -START-LITE-CUT- */
#  define pr_snid		pr_context.prc_snid
#  define pr_xrid		pr_context.prc_xrid
#  define pr_xfname		pr_context.prc_xfname
#  define pr_distflag		pr_context.prc_distflag
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

/* magic number for proctab elements */
#define PR_MAGIC	0x70524f63

/* values for pr_state - discrete values */
#define PRS_IDLE	1	/* ignore this element */
#define PRS_PROCESS	2	/* process this element */
#define PRS_NEXT	3	/* move on to next element */
#define PRS_SLEEP	4	/* waiting for child proctabs to come off
				   the run queue */
#define PRS_WAIT	5	/* waiting for an exec to finish */

/* values for pr_tcstate - discrete values */
#define TCS_START	1	/* start processing a test case */
#define TCS_LOCK	2	/* lock a test case */
#define TCS_UNLOCK	3	/* unlock a test case */
#define TCS_COPY	4	/* copy a test case directory tree */
#define TCS_REMCOPY	5	/* copy source files to remote systems */
#define TCS_PREBUILD	6	/* execute the prebuild tool */
#define TCS_BUILD	7	/* build a test case */
#define TCS_BUILDFAIL	8	/* execute the build fail tool */
#define TCS_EXEC	9	/* execute a test case */
#define TCS_CLEAN	10	/* clean a test case */
#define TCS_JOURNAL	11	/* perform journal file processing */
#define TCS_SAVE	12	/* perform save files processing */
#define TCS_END		13	/* finish processing a test case */

/* values for pr_toolstate - discrete values */
#define PTS_IDLE	1	/* idle */
#define PTS_RUNNING	2	/* tool running */
#define PTS_EXITED	3	/* tool exited */
#define PTS_ABORT	4	/* tool should be aborted */
#define PTS_SIGTERM	5	/* SIGTERM sent to tool */
#define PTS_SIGKILL	6	/* SIGKILL sent to tool */
/* these two used in the quick kill+wait in quick_killtc() */
#define PTS_UNWAITEDFOR	7	/* not yet waited for */
#define PTS_KILLED_AND_UNWAITEDFOR \
			8	/* signalled but not yet waited for */

/* values for pr_flags - a bit field */
#define PRF_ATTENTION	0001	/* element needs servicing */
#define PRF_RUNQ	0002	/* element is on the run queue */
#define PRF_STEP	0004	/* return to parent when state is PRS_NEXT */
#define PRF_SHLOCK	0010	/* lock is shared (else exclusive) */
#define PRF_AUTORESULT	0020	/* need a result based on tool exit status */
#ifndef TET_LITE	/* -START-LITE-CUT- */
#  define PRF_TC_CHILD	0040	/* this is a testcase child proctab */
#  define PRF_JNL_CHILD	0100	/* testcase child proctabs have own journals */
#  define PRF_RECONNECT	0200	/* testcase child is reconnectable */
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

/* initial and maximum values for pr_waitinterval */
#define WAITINTERVAL_START	1
#define WAITINTERVAL_MAX	5


/* context-dependent error handler */
#ifdef TET_LITE	/* -LITE-CUT-LINE- */
#  define prperror(prp, sysid, errnum, s1, s2) \
	tcc_prperror(prp, -1, errnum, srcFile, __LINE__, s1, s2)
#else	/* -START-LITE-CUT- */
#  define prperror(prp, sysid, errnum, s1, s2) \
	tcc_prperror(prp, sysid, errnum, srcFile, __LINE__, s1, s2)
#endif /* TET_LITE */	/* -END-LITE-CUT- */

#ifndef NEEDsrcFile
#define NEEDsrcFile
#endif

/* the execution engine's run queue */
extern struct proctab *runq;

/*
** macro used by the execution engine to have the specified function run
** on all the proctabs for a particular test case
**
** return 0 if (all of) the function invocation(s) succeeded,
** or -1 if (at least one of) the function invocation(s) failed
*/
#ifdef TET_LITE	/* -LITE-CUT-LINE- */
#  define RUN_PROCTABS(prp, func)	((func)((prp)))
#else	/* -START-LITE-CUT- */
#  define RUN_PROCTABS(prp, func) \
	((prp)->pr_child ? run_child_proctabs((prp), (func)) : (func)((prp)))
#endif /* TET_LITE */	/* -END-LITE-CUT- */


/* extern function declarations */
extern int is_resume_point PROTOLIST((struct proctab *));
extern void jnl_build_end PROTOLIST((struct proctab *));
extern void jnl_build_start PROTOLIST((struct proctab *));
extern void jnl_captured PROTOLIST((struct proctab *, char *));
extern void jnl_clean_end PROTOLIST((struct proctab *));
extern void jnl_clean_start PROTOLIST((struct proctab *));
extern void jnl_consolidate PROTOLIST((struct proctab *));
extern void jnl_ic_end PROTOLIST((struct proctab *));
extern void jnl_ic_start PROTOLIST((struct proctab *));
extern void jnl_par_end PROTOLIST((struct proctab *));
extern void jnl_par_start PROTOLIST((struct proctab *));
extern void jnl_rnd_end PROTOLIST((struct proctab *));
extern void jnl_rnd_start PROTOLIST((struct proctab *));
extern void jnl_rpt_end PROTOLIST((struct proctab *));
extern void jnl_rpt_start PROTOLIST((struct proctab *));
extern void jnl_sceninfo PROTOLIST((struct proctab *, char *));
extern void jnl_seq_end PROTOLIST((struct proctab *));
extern void jnl_seq_start PROTOLIST((struct proctab *));
extern void jnl_tcc_prpmsg PROTOLIST((struct proctab *, char *));
extern void jnl_tcm_start PROTOLIST((struct proctab *));
extern void jnl_tc_end PROTOLIST((struct proctab *));
extern void jnl_tc_start PROTOLIST((struct proctab *));
extern void jnl_tloop_end PROTOLIST((struct proctab *));
extern void jnl_tloop_start PROTOLIST((struct proctab *));
extern int jnl_tmpfile PROTOLIST((struct proctab *));
extern void jnl_tp_result PROTOLIST((struct proctab *, int, int));
extern void jnl_tp_start PROTOLIST((struct proctab *));
extern void jnl_user_abort PROTOLIST((struct proctab *));
extern void jnl_var_end PROTOLIST((struct proctab *));
extern void jnl_var_start PROTOLIST((struct proctab *));
extern int jnlproc_api PROTOLIST((struct proctab *));
extern int jnlproc_nonapi PROTOLIST((struct proctab *));
extern struct proctab *pralloc PROTOLIST((void));
extern void prcfree PROTOLIST((struct proctab *));
extern void prfree PROTOLIST((struct proctab *));
extern void proc_parallel PROTOLIST((struct proctab *));
extern void proc_random PROTOLIST((struct proctab *));
extern void proc_rtloop PROTOLIST((struct proctab *));
extern void proc_sequential PROTOLIST((struct proctab *));
extern void proc_tcwait PROTOLIST((struct proctab *));
extern void proc_testcase PROTOLIST((struct proctab *));
extern void proc_variable PROTOLIST((struct proctab *));
extern void runqadd PROTOLIST((struct proctab *));
extern void runqrm PROTOLIST((struct proctab *));
extern int sfproc PROTOLIST((struct proctab *, char **, int));
extern int tcc_lock PROTOLIST((struct proctab *, int, char *, char [], int));
extern int tcc_mkalldirs PROTOLIST((struct proctab *, char *));
extern int tcc_mktmpdir PROTOLIST((struct proctab *, char *, char **));
extern void tcc_prperror PROTOLIST((struct proctab *, int, int, char *, int,
	char *, char *));
extern int tcc_rmtmpdir PROTOLIST((struct proctab *, char *));
extern int tccopy PROTOLIST((struct proctab *, char *, char *));
extern long tcc_texec PROTOLIST((struct proctab *, char *, char **, char *,
	char *));
extern int tcc_unlock PROTOLIST((struct proctab *, int, char *));
extern void tcexecdir PROTOLIST((struct proctab *, char *, char [], int));
extern void tcexecname PROTOLIST((struct proctab *, char *, char [], int));
extern void tcsrcdir PROTOLIST((struct proctab *, char [], int));
extern void tcsrcname PROTOLIST((struct proctab *, char [], int));
extern int toolexec PROTOLIST((struct proctab *, char *, char **, char *));
extern char **toolprep PROTOLIST((struct proctab *, char *, int));
extern int toolwait PROTOLIST((struct proctab *));

#ifndef TET_LITE	/* -START-LITE-CUT- */
extern int child_proctabs_tstate PROTOLIST((struct proctab *, int));
extern int configure_tccd PROTOLIST((struct proctab *));
extern int copy_sfiles2rmt PROTOLIST((struct proctab *, FILE *, char *));
extern int get_snid_xrid PROTOLIST((struct proctab *));
extern int getremfile PROTOLIST((struct proctab *, char *, char *));
extern void jnl_dist_end PROTOLIST((struct proctab *));
extern void jnl_dist_start PROTOLIST((struct proctab *));
extern void jnl_rmt_end PROTOLIST((struct proctab *));
extern void jnl_rmt_start PROTOLIST((struct proctab *));
extern void proc_rdist PROTOLIST((struct proctab *));
extern void rm_snid_xrid PROTOLIST((struct proctab *));
extern int run_child_proctabs PROTOLIST((struct proctab *,
	int (*) PROTOLIST((struct proctab *)) ));
extern void setup_child_proctabs PROTOLIST((struct proctab *));
extern void unlink_xres PROTOLIST((struct proctab *));
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

