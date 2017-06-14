/*
 *	SCCS: @(#)scentab.h	1.7 (05/12/07)
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

SCCS:   	@(#)scentab.h	1.7 05/12/07 TETware release 3.8
NAME:		scentab.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	definitions related to scenario table elements

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., December 1997
	removed SCF_DIST scenario flag - the "distributed" attribute
	is now part of a test case's execution context
	(see struct proctab in proctab.h)

	Neil Moses, The Open Group, December 2005
	Added *scd_recon and scd_nrecon to hold details of reconnectable
	systems.

************************************************************************/

/* this for HP-UX and other systems which define sc_flags in signal.h */
#undef sc_flags

/*
**	scenario elements
**
**	a scenario element is allocated for each element in the
**	scenario file
**
**	processing of a scenario file is performed in several stages:
**
**		in the first stage the input stream is tokenised,
**		directive arguments are checked, directives implied by the
**		language syntax are added and include file processing is
**		performed; 
**		as this is done the elements are arranged in a linear
**		list
**
**		in the second stage, the linear list is transformed into a
**		tree that can be used to control the execution of the
**		scenario
**
**		in the third stage, the tree is pruned to remove un-needed
**		scenarios, referenced scenarios are copied into place
**
**		the tree may be pruned even more when in RERUN or RESUME mode
*/

/*
**	structure of a scenario element -
**	the sc_next and sc_last elements must be 1st and 2nd so as to
**	enable scentab lists to be manipulated by the llist functions
*/
struct scentab {
	/* these two used when the element is in a list or on a stack */
	struct scentab *sc_next;	/* ptr to next element */
	struct scentab *sc_last;	/* ptr to prev element */

	/* these four used when the element is in a tree */
	struct scentab *sc_parent;	/* ptr to element's parent in a tree */
	struct scentab *sc_child;	/* ptr to first of this element's
					   children */
	struct scentab *sc_forw;	/* ptr to next element at this level */
	struct scentab *sc_back;	/* ptr to prev element at this level */

	long sc_magic;			/* magic number */
	int sc_type;			/* element type - see below */
	union {				/* element data - see below */
		char *scd_text;			/* some text */
		struct {			/* a test case */
			char *scd_tcname;	/* test case name */
			char *scd_sciclist;	/* optional IC list from
						   scenario file */
			char *scd_exiclist;	/* optional IC list to
						   execute */
		} scd_tcdata;
		struct {			/* a directive */
			int scd_directive;	/* directive name */
			union {			/* arguments */
				int *scd_nlist;	/* a list of numbers */
				char **scd_str;	/* some strings */
				long scd_long;	/* a number */
			} scd_un;
			int scd_int1;		/* another number */
			int scd_int2;		/* another number */
			int scd_int3;		/* another number */
			int *scd_recon;		/* list of reconnect sysids */
			int scd_nrecon;		/* number of reconnect sysids */
		} scd_didata;
		struct {			/* a referenced scenario */
			char *scd_scen_name;	/* scenario name */
			struct scentab *scd_scenptr;
						/* ptr to its start */
		} scd_scdata;
	} sc_data;
	int sc_flags;			/* flags - see below */
	long sc_ref;			/* scenario reference */
	char *sc_fname;			/* name of the source file from which
					   this element is derived */
	int sc_lineno;			/* line number in the source file */
};

/* magic number for scenario elements */
#define SC_MAGIC	0x7343456e

/*
**	scenario element types
*/

/* tree nodes */
#define SC_SCENARIO		1	/* scenario header */
#define SC_DIRECTIVE		2	/* scenario directive */
/* leaf nodes */
#define SC_TESTCASE		3	/* test case name */
#define SC_SCENINFO		4	/* scenario information line */
#define	SC_SCEN_NAME		5	/* referenced scenario */

/*
**	scenario element data
**
**	the name of the stored data depends on the type of the
**	element in which it is stored (sc_type)
*/

/* a HEADER */
/* name of this scenario */
#define sc_scenario	sc_data.scd_text

/* a DIRECTIVE */
/* directive value */
#define sc_directive	sc_data.scd_didata.scd_directive

/* a TESTCASE */
/*
** the name of the test case
** the IC list from the scenario file
** the IC list to execute (possibly modified after rerun processing)
*/
#define sc_tcname	sc_data.scd_tcdata.scd_tcname
#define sc_sciclist	sc_data.scd_tcdata.scd_sciclist
#define sc_exiclist	sc_data.scd_tcdata.scd_exiclist

/* a Scenario Information Line */
#define sc_sceninfo	sc_data.scd_text

/* a scenario (test list) to be interpolated */
/* the name of the scenario */
#define sc_scen_name	sc_data.scd_scdata.scd_scen_name
/* pointer to the scenario header */
#define sc_scenptr	sc_data.scd_scdata.scd_scenptr

/*
**	directive arguments and variables -
**	the meanings of directive arguments and variables depends on
**	which directive this is (sc_directive)
*/

/* REPEAT and PARALLEL directives; also TIMED_LOOP when in RERUN mode */
/* the count argument */
#define sc_count	sc_data.scd_didata.scd_int1

/*
** REPEAT and TIMED_LOOP each have an iteration count and a resume count when
** in RESUME mode
*/
#define sc_itcount	sc_data.scd_didata.scd_int2
#define sc_rescount	sc_data.scd_didata.scd_int3

/* TIMED_LOOP directive in normal or RESUME mode */
/* the seconds argument */
#define sc_seconds	sc_data.scd_didata.scd_un.scd_long

/* REMOTE and DISTRIBUTED directives */
/* the system list and number of systems in the list */
#define sc_sys	sc_data.scd_didata.scd_un.scd_nlist
#define sc_nsys	sc_data.scd_didata.scd_int1

/* Systems that can be reconnected */
#define sc_recon	sc_data.scd_didata.scd_recon
#define sc_nrecon	sc_data.scd_didata.scd_nrecon

/* VARIABLE directive */
/* list of variables and number of variables in the list */
#define sc_vars		sc_data.scd_didata.scd_un.scd_str
#define sc_nvars	sc_data.scd_didata.scd_int1


/*
**	scenario element flags
*/
#define SCF_IMPLIED		00001	/* element is implied by the
					   scenario file language */
#define SCF_PROCESSED		00002	/* used to avoid processing a
					   scenario more than once */
#define SCF_RESOLVED		00004	/* attempt has been made to resolve
					   this scenario name */
#define SCF_ERROR		00010	/* used to avoid reporting a
					   scenario error than once */
#define SCF_NEEDED		00020	/* this element is needed */
#define SCF_DATA_USED		00040	/* sc_data elements are used elsewhere
					   so don't free them in scfree() */
#define SCF_SKIP_BUILD		00100	/* skip the build processing stage */
#define SCF_SKIP_EXEC		00200	/* skip the exec processing stage */
#define SCF_SKIP_CLEAN		00400	/* skip the clean processing stage */
#define SCF_SKIP_ALL		(SCF_SKIP_BUILD|SCF_SKIP_EXEC|SCF_SKIP_CLEAN)


/* the linear scenario list - generated by pass 1 */
extern struct scentab *sclist;

/* the scenario tree - generated by pass 2 */
extern struct scentab *sctree;

/* RESUME mode controls in rrproc.c */
extern struct scentab *resume_scen;


/* scenario element tracing */
#ifdef NOTRACE
#define TRACESCELEM(flag, level, eptr, text)
#else
extern void tracescelem PROTOLIST((int, int, struct scentab *, char *));
#define TRACESCELEM(flag, level, eptr, text) \
	if ((flag) >= (level)) \
		tracescelem((flag), (level), (eptr), (text)); \
	else
#endif


/* function declarations */
extern void copy_refscen PROTOLIST((struct scentab *, struct scentab *));
extern struct scentab *scalloc PROTOLIST((void));
extern void scfree PROTOLIST((struct scentab *));
extern void scpush PROTOLIST((struct scentab *, struct scentab **));
extern struct scentab *scpop PROTOLIST((struct scentab **));
extern void scrm_lnode PROTOLIST((struct scentab *));
extern void scstore PROTOLIST((struct scentab *, struct scentab *,
	struct scentab **));

