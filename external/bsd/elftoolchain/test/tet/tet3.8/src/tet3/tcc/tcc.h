/*
 *	SCCS: @(#)tcc.h	1.12 (05/12/07)
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

SCCS:   	@(#)tcc.h	1.12 05/12/07 TETware release 3.8
NAME:		tcc.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	declarations of tcc global data and functions not declared in
	other files

REQUIRES PRIOR INCLUSION OF:
	<stdio.h>	(for FILE)
	<time.h>	(for time_t)

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., June 1997
	added support for the -I command-line option

	Andrew Dingwall, UniSoft Ltd., July 1998
	Changes to support %include in scenario files.
	Added support for shared API libraries.

	Andrew Dingwall, UniSoft Ltd., July 1999
	added support for writing the journal to stdout or to a pipeline
 
	Andrew Dingwall, The Open Group, March 2003
	Enhancement to copy source files to remote systems.

	Neil Moses, The Open Group, December 2005
	Added TET_DEFAULT_RECONNECT_TIMEOUT for reconnect enhancement.

************************************************************************/


/* tell tcclib.h that it has been included in a tcc source file */
#define TCC	1

/*
** alternate execution directory on the local system
** from TET_EXECUTE in the environment or from the -a command-line option
*/
extern char *tet_execute;

/* test suite root on the local system */
extern char *tet_tsroot;

/* test suite root on the local system from the environment
** (defaults to tet_root)
*/
extern char *tet_suite_root;

/* runtime directory on the local system from the environment */
extern char *tet_run;

/* temporary directory on the local system from the environment */
extern char *tet_tmp_dir;

/* scenario error counter */
extern int scenerrors;
#define MAXSCENERRORS	50

/* tcc version number */
extern char tcc_version[];

/* valid command-line options */
extern char tcc_options[];

/* format of a scenario reference */
extern char tcc_scenref_fmt[];

/* modes of operation from the command line */
extern int tcc_modes;

/* RESUME mode controls in rrproc.c */
extern int resume_mode;
extern char *resume_iclist;
extern int resume_found;


/*
** processing stages and modes of operation -
** the low 3 bits are not used so as to distinguish processing
** modes (defined here) from configuration modes (defined in config.h)
*/
#define TCC_START	00010
#define TCC_BUILD	00020
#define TCC_EXEC	00040
#define TCC_CLEAN	00100
#define TCC_RESUME	00200
#define TCC_RERUN	00400
#define TCC_ABORT	01000
#define TCC_END		02000

/* compatibility mode - used to resolve ambiguities in the scenario language */
extern int tet_compat;
#define COMPAT_DTET	1
#define COMPAT_ETET	2

#ifndef TET_LITE	/* -LITE-CUT-LINE- */
#define TET_DEFAULT_RECONNECT_TIMEOUT "600"
#endif	/* -LITE-CUT-LINE- */

/* flag derived from the -I command-line option */
extern int tcc_Iflag;

/* flag derived from the -p command-line option */
extern int report_progress;

/* test case timeout from the -t command-line option */
extern int tcc_timeout;

/* length of an input line buffer */
#define LBUFLEN		1024

/* fake tet_tcerrno for use in TET_LITE */
#ifdef TET_LITE	/* -LITE-CUT-LINE- */
extern int tet_tcerrno;
#endif	/* -LITE-CUT-LINE- */

/* reliable BUFCHK() call */
#ifdef NOTRACE
extern void rbufchk PROTOLIST((char **, int *, int));
#  define RBUFCHK(bpp, lp, newlen)	rbufchk(bpp, lp, newlen)
#else /* NOTRACE */
extern void rbuftrace PROTOLIST((char **, int *, int, char *, int));
#  define RBUFCHK(bpp, lp, newlen) \
	rbuftrace(bpp, lp, newlen, srcFile, __LINE__)
#  ifndef NEEDsrcFile
#    define NEEDsrcFile
#  endif /* NEEDsrcFile */
#endif /* NOTRACE */

/* flag values used with yesstr(), nostr() and okstr() calls */
#define YN_CMDLINE	1	/* -y/-n options from the command line */
#define YN_OJFILE	2	/* -y/-n options from the old journal file */


/* extern function declarations */
extern void check_empty_timed_loops PROTOLIST((void));
extern void config_cleanup PROTOLIST((void));
extern void distcfg PROTOLIST((void));
extern void doconfig PROTOLIST((void));
extern void engine_shutdown PROTOLIST((void));
extern void exec_block_signals PROTOLIST((void));
extern void execscen PROTOLIST((void));
extern void execsigtrap PROTOLIST((void));
extern void exec_unblock_signals PROTOLIST((void));
extern void fullpath PROTOLIST((char *, char *, char [], int, int));
extern char *get_runtime_tsroot PROTOLIST((int));
extern char *getcfg PROTOLIST((char *, int, int));
extern int getcflag PROTOLIST((char *, int, int));
extern char *getdcfg PROTOLIST((char *, int));
extern char *getmcfg PROTOLIST((char *, int));
extern int getmcflag PROTOLIST((char *, int));
#ifdef SY_MAGIC
extern void init1environ PROTOLIST((struct systab *));
#endif /* SY_MAGIC */
extern void initcfg PROTOLIST((char *, char *, char *, char *));
extern void initenviron PROTOLIST((void));
extern void initrescode PROTOLIST((void));
extern void initresdir PROTOLIST((char *, char *));
extern void initsigtrap PROTOLIST((void));
extern void initsystab PROTOLIST((void));
extern void inittmpdir PROTOLIST((void));
extern void inittft PROTOLIST((void));
extern int isnumrange PROTOLIST((char *, int *, int *));
extern int iszorpnum PROTOLIST((char *));
extern void jnl_cfg PROTOLIST((char *));
extern void jnl_cfg_end PROTOLIST((void));
extern void jnl_close PROTOLIST((void));
extern void jnl_init PROTOLIST((char *, char *));
extern char *jnl_jfname PROTOLIST((void));
extern FILE *jnl_jfp PROTOLIST((void));
extern void jnl_mcfg_start PROTOLIST((char *, int));
extern void jnl_tcc_end PROTOLIST((void));
extern void jnl_tcc_msg PROTOLIST((char *));
extern void jnl_tcc_start PROTOLIST((int, char **));
extern char *jnl_tfname PROTOLIST((char *, char *));
extern char *jnl_time PROTOLIST((time_t));
extern void jnl_uname PROTOLIST((void));
extern int jnl_usable PROTOLIST((void));
extern void jnl_write PROTOLIST((int, char *, char *, FILE *, char *));
extern int jnlproc_split PROTOLIST((char *, char **, char []));
extern int nextmode PROTOLIST((int, int));
extern void nostr PROTOLIST((char *, int));
extern void ocfilename PROTOLIST((char *, char *, int));
extern int okstr PROTOLIST((char *, int));
#ifdef TET_STRUCT_CFLIST_DEFINED
extern struct cflist *per_system_config PROTOLIST((int, int));
#endif
extern char *prcfmode PROTOLIST((int));
extern int proc1cmdline PROTOLIST((char *));
extern int proc1scfile PROTOLIST((FILE *, char *));
extern int proc2sclist PROTOLIST((void));
extern int proc3sctree PROTOLIST((char *));
extern void proclopt PROTOLIST((char *));
extern void procscen PROTOLIST((char *, char *, char *));
extern int procvopt PROTOLIST((char *));
extern char *prpflags PROTOLIST((int));
extern char *prpstate PROTOLIST((int));
extern char *prscdir PROTOLIST((int));
extern char *prscflags PROTOLIST((int));
extern char *prsctype PROTOLIST((int));
extern char *prtccmode PROTOLIST((int));
extern char *prtcstate PROTOLIST((int));
extern char *prtoolstate PROTOLIST((int));
extern void putdcfg PROTOLIST((char *, int, char *));
extern void rescode_cleanup PROTOLIST((void));
extern char *resdirname PROTOLIST((void));
extern char *resdirsuffix PROTOLIST((void));
extern void rrproc PROTOLIST((char *, char *));
extern char *rstrstore PROTOLIST((char *));
extern void rtlcopy PROTOLIST((void));
extern void scenermsg PROTOLIST((char *, char *, int, char *));
extern void scenerror PROTOLIST((char *, char *, int, char *));
extern void scengiveup PROTOLIST((void));
extern int split PROTOLIST((char *, char **, int, int));
extern int symax PROTOLIST((void));
extern int tcc_access PROTOLIST((int, char *, int));
extern int tcc_chdir PROTOLIST((int, char *));
extern void tcc_dirname PROTOLIST((char *, char [], int));
extern void tcc_error PROTOLIST((int, char *, int, char *, char *));
extern void tcc_exit PROTOLIST((int));
extern void tcc_fatal PROTOLIST((int, char *, int, char *, char *));
extern int tcc_kill PROTOLIST((int, long, int));
extern int tcc_mkdir PROTOLIST((int, char *));
extern int tcc_pclose PROTOLIST((FILE *));
extern FILE *tcc_popen PROTOLIST((char *, char *));
extern int tcc_putenv PROTOLIST((int, char *));
extern int tcc_putenvv PROTOLIST((int, char **, int));
extern int tcc_rmdir PROTOLIST((int, char *));
extern void tcc_sloop PROTOLIST((void));
extern int tcc_timeouts PROTOLIST((time_t));
extern int tcc_unlink PROTOLIST((int, char *));
extern int tcc_waitnohang PROTOLIST((int, long, int *));
extern int tcc2cfmode PROTOLIST((int));
extern void toolpfree PROTOLIST((char **));
extern void yesstr PROTOLIST((char *, int));
extern void ynproc PROTOLIST((int));

#ifdef TET_LITE	/* -LITE-CUT-LINE- */
extern int tet_config_putenv PROTOLIST((int));
#else /* TET_LITE */	/* -START-LITE-CUT- */
extern void dtcc_cleanup PROTOLIST((void));
extern void initdtcc PROTOLIST((void));
extern void initsfdir PROTOLIST((void));
extern void jnl_scfg_start PROTOLIST((int, int));
extern void rtrcopy PROTOLIST((void));
#endif /* TET_LITE */	/* -END-LITE-CUT- */


