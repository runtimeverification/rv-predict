/*
 *	SCCS: @(#)handler.c	1.2 (02/11/06)
 *
 * Copyright (c) 2000 The Open Group
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
 */

#ifndef lint
static char sccsid[] = "@(#)handler.c	1.2 (02/11/06) TETware release 3.8";
#endif

/************************************************************************

SCCS:		@(#)handler.c	1.2 02/11/06 TETware release 3.8
NAME:		handler.c
PRODUCT:	TETware Generic Report Writer.
AUTHOR:		Matthew Hails, UniSoft Ltd.
DATE CREATED:	19 July 2000
SYNOPSIS:

	void grw_beginjournal(char *journal, char *content,
		grw_formatter_t *ftr, char *output)
	void grw_handleline(int tag, char *f1a, char *f1b, char *f1c,
		char *f1d, char *f1e, char *f2)
	void grw_endjournal(void)

DESCRIPTION:

	Code to handle the parsed content of a journal file and
	manipulate the information into the required content and layout.
	It uses a `formatter' to output the content in the required
	document format.

************************************************************************/

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <limits.h>
#include <ctype.h>
#include "tet_jrnl.h"
#include "grw.h"


/* Simple content levels */
#define CONTENT_L1	"bceT"		/* Summary table(s). */
#define CONTENT_L2	"bceTB"		/* Summary table(s).
					 * Summary breakdown, excluding passes.
					 */
#define CONTENT_L3	"bceTBpi"	/* Summary table(s).
				 	 * Summary breakdown, including passes.
				 	 */
#define CONTENT_L4	"bceTIi"	/* Summary table(s).
					 * Simple inline report, excluding
					 * passes.
					 */
#define CONTENT_L5	"bceTIif"	/* Summary table(s).
					 * Full inline report, excluding passes.
					 */
#define CONTENT_L6	"bceTIpif"	/* Summary table(s).
					 * Full inline report, including passes.
					 */
#define CONTENT_L7	"bceTIpifx"	/* Summary table(s).
					 * Full inline report including passes.
					 * Extra detail in inline report.
					 */

/* Size of a char buffer to hold an int */
#define INTBUFSIZE		((sizeof(int) * CHAR_BIT + 2) / 3 + 1 + 1)

/* Values for `hd.mode' */
#define MODE_BUILD		0x01
#define MODE_CLEAN		0x02
#define MODE_EXECUTE		0x04

/* Values for `hd.style' entries */
#define STYLE_TABLE		'T'
#define STYLE_BREAKDOWN		'B'
#define STYLE_INLINE		'I'

/* Values for `hd.inc' */
#define INC_PASSES		0x01
#define INC_INTRO		0x02
#define INC_FULL		0x04
#define INC_EXTRA		0x08
#define INC_CONFIG		0x10

/* Result code strings */
#define RESULT_MISSING		"<MissingResult>"
#define RESULT_PASS		"PASS"

/* Allocation increment for string vectors */
#define ALLOC_INCREMENT		20

/* Default title of generated reports */
#define DEFAULT_TITLE		"TETware Test Run Report"

/* Flag values for `state' */
#define STATE_TCC		0x0001
#define STATE_TC		0x0002
#define STATE_IC		0x0004
#define STATE_TP		0x0008
#define STATE_CFG		0x0010
#define STATE_DISTRIB		0x0020


/*
 * Structure representing one test purpose (TP).
 */
struct tp
{
	/* Test purpose number */
	int tpnumber;

	/* Vector holding information lines */
	grw_svector_t *infolines;

	/* Pointer to result code.
	 * N.B. This points into results vector, so the memory belongs to the
	 * results vector. Plus, there is only one copy of each result code.
	 */
	char *result;

	/* Pointer to next TP structure in list */
	struct tp *next;
};

/*
 * Structure representing one invocable component (IC).
 */
struct ic
{
	/* Invocable component number */
	int icnumber;

	/* Pointer to first element of TP list */
	struct tp *tphead;

	/* Pointer to last element of TP list */
	struct tp *tptail;

	/* Pointer to next IC structure in list */
	struct ic *next;
};

/*
 * Structure representing one testcase (TC).
 * May be an API or non-API conforming testcase, build tool or clean tool.
 */
struct tc
{
	/* Mode, i.e. build, clean or execute. One of MODE_.. values */
	unsigned int mode;

	/* Is the testcase API conforming? Note that this relies on the TCM
	 * start line, so all execute-mode testcases appear as API conforming,
	 * since the TCC inserts the relevant lines into the journal.
	 */
	int apiconforming;

	/* Test case name */
	char name[TET_JNL_LEN];

	/* Test case directory */
	char dir[TET_JNL_LEN];

	/* Vector holding informational lines */
	grw_svector_t *infolines;

	/* Vector holding captured output */
	grw_svector_t *captured;

	/* Vector holding extraneous information following this testcase */
	grw_svector_t *xinfo;

	/* Completion status of testcase binary/TCM/build tool/clean tool.
	 * Set to -1 if the status is not recognized.
	 */
	int status;

	/* Boolean value used to indicate success or failure for build and
	 * clean mode results.
	 */
	int success;

	/* Pointer to first element of IC list */
	struct ic *ichead;

	/* Pointer to last element of IC list */
	struct ic *ictail;

	/* Pointer to next TC structure in list */
	struct tc *next;
};

/*
 * Structure representing a configuration section.
 */
struct cfg
{
	/* Pathname or remote system ID */
	char pathname[TET_JNL_LEN];

	/* Mode of configuration */
	char mode[TET_JNL_LEN];

	/* Vector of configuration variables */
	grw_svector_t *vars;

	/* Pointer to next cfg structure in list */
	struct cfg *next;
};

/*
 * Structure representing one result code.
 */
struct rescode
{
	/* String name of result code */
	char *code;

	/* Occurrence count of this result code */
	int count;
};


/* Local prototypes */
static void initdata(void);
static void tcstart(unsigned int mode, char *testcase);
static void tcend(unsigned int mode, char *status);
static void storeinfoline(int incopt, char *tagname, char *prefix,
	char *context, char *infoline);
static void storexinfo(char *prefix, char *infoline);
static int cmprescodes(const void *e1, const void *e2);
static void print_execute_summary(grw_formatter_t *ftr, int style);
static void print_bc_summary(grw_formatter_t *ftr, unsigned int mode,
	int style);
static void print_inline(grw_formatter_t *ftr);
static void printxinfo(grw_formatter_t *ftr, grw_svector_t *xinfo);
static void printtcintro(grw_formatter_t *ftr, struct tc *tc);
static void printlinetable(grw_formatter_t *ftr, char *title,
	grw_svector_t *lines);
static void printulist(grw_formatter_t *ftr, grw_svector_t *items);


/*
 * Private data used by handler.
 * This doesn't need to be in one structure, but helps keep the namespace tidy.
 */
static struct
{
	/* Flag to indicate whether this data has been initialized yet */
	int inited;

	/* Modes for which the output should be included.
	 * Bitmask of MODE_.. options.
	 */
	unsigned int mode;

	/* Style of output. Each character contains a STYLE_.. value.
	 * Nul-terminated.
	 */
	char *style;

	/* Which optional data to include. Bitmask of INC_.. options. */
	unsigned int inc;

	/* Formatter used to produce report document */
	grw_formatter_t *ftr;

	/* Name of journal file */
	char *journal;

	/* Name of output file */
	char *output;

	/* Head of testcase list */
	struct tc *tchead;

	/* Tail of testcase list */
	struct tc *tctail;

	/* Head of configuration list */
	struct cfg *cfghead;

	/* Tail of configuration list */
	struct cfg *cfgtail;

	/* State of parsing */
	unsigned int state;

	/* Current testcase; NULL when not in a testcase */
	struct tc *curtc;

	/* Current IC; NULL when not in an IC */
	struct ic *curic;

	/* Current TP; NULL when not in an TP */
	struct tp *curtp;

	/* Current configuration; NULL when not in a configuration */
	struct cfg *curcfg;

	/* Result code vector */
	grw_svector_t *results;

	/* Version of TETware used by the test run */
	char version[TET_JNL_LEN];

	/* Date on which the run was started */
	char startdate[TET_JNL_LEN];

	/* Start time of the run */
	char starttime[TET_JNL_LEN];

	/* System information */
	char sysinfo[TET_JNL_LEN];

	/* Vector holding extraneous information occurring before testcases */
	grw_svector_t *initxinfo;
} hd;


/*
 * initdata()
 *
 * Initialize handler's private data.
 */
static void
initdata(void)
{
	struct tc *tc;
	struct tc *nexttc;
	struct ic *ic;
	struct ic *nextic;
	struct tp *tp;
	struct tp *nexttp;
	struct cfg *cfg;
	struct cfg *nextcfg;

	/* Reset content specifiers */
	hd.mode = 0U;

	if (hd.style != NULL)
		free(hd.style);

	hd.style = NULL;
	hd.inc = 0U;

	/* Clear formatter */
	hd.ftr = NULL;

	/* Free input/output filenames */
	if (hd.journal != NULL)
		free(hd.journal);

	hd.journal = NULL;

	if (hd.output != NULL)
		free(hd.output);

	hd.output = NULL;

	/* Free tc/ic/tp list */
	for (tc = hd.tchead; tc != NULL; tc = nexttc)
	{
		/* Free string vectors */
		grw_freesvector(tc->infolines);
		grw_freesvector(tc->captured);
		grw_freesvector(tc->xinfo);

		/* Free IC list memory */
		for (ic = tc->ichead; ic != NULL; ic = nextic)
		{
			for (tp = ic->tphead; tp != NULL; tp = nexttp)
			{
				/* Free infolines vector */
				grw_freesvector(tp->infolines);

				/* Free this tp structure */
				nexttp = tp->next;
				free(tp);
			}

			/* Free this ic structure */
			nextic = ic->next;
			free(ic);
		}

		/* Free this tc structure */
		nexttc = tc->next;
		free(tc);
	}

	hd.tchead = NULL;
	hd.tctail = NULL;

	/* Free cfg list */
	for (cfg = hd.cfghead; cfg != NULL; cfg = nextcfg)
	{
		grw_freesvector(cfg->vars);
		nextcfg = cfg->next;
		free(cfg);
	}

	hd.state = 0U;
	hd.curtc = NULL;
	hd.curic = NULL;
	hd.curtp = NULL;
	hd.curcfg = NULL;

	/* Create results vector, or empty it if it already exists */
	if (hd.results == NULL)
		hd.results = grw_createsvector(ALLOC_INCREMENT);
	else
		grw_emptysvector(hd.results);

	/* Create initxinfo vector, or empty it if it already exists */
	if (hd.initxinfo == NULL)
		hd.initxinfo = grw_createsvector(ALLOC_INCREMENT);
	else
		grw_emptysvector(hd.initxinfo);

	/* Set flag to show the data has been initialized */
	hd.inited = 1;
}


/*
 * grw_beginjournal()
 *
 * Handle the beginning of a journal file.
 *
 *	journal		Path of journal file from which to read input.
 *			Pass "-" to specify standard input.
 *	content		Content format. See program syntax in description of
 *			main().
 *	ftr		Document formatter.
 *	output		Path of output file.
 *			Pass "-" to specify standard output.
 */
void
grw_beginjournal(char *journal, char *content, grw_formatter_t *ftr,
	char *output)
{
	static char *levels[] =
	{
		"",
		CONTENT_L1,
		CONTENT_L2,
		CONTENT_L3,
		CONTENT_L4,
		CONTENT_L5,
		CONTENT_L6,
		CONTENT_L7
	};
	int detail;
	char *cp;
	int finish;
	char *style;

	/* Clear private data */
	initdata();

	/*
	 * Parse content...
	 */

	/* If the content is given as a simple level number, convert to
	 * configurable format.
	 */
	if (isdigit((unsigned char)*content))
	{
		detail = *content - '0';

		if (detail == 0 || detail >= GRW_NELEM(levels)
			|| content[1] != '\0')
		{
			grw_fatal("illegal content specifier \"%s\"", content);
		}

		cp = levels[detail];
	}
	else
	{
		cp = content;
	}

	/* First character(s) are the mode(s) */
	hd.mode = 0U;

	for (finish = 0; !finish; )
	{
		switch (*cp)
		{
		case 'b':
			hd.mode |= MODE_BUILD;
			cp++;
			break;
		case 'c':
			hd.mode |= MODE_CLEAN;
			cp++;
			break;
		case 'e':
			hd.mode |= MODE_EXECUTE;
			cp++;
			break;
		default:
			finish = 1;
			break;
		}
	}

	if (hd.mode == 0U)
		grw_fatal("illegal content specifier \"%s\"", content);

	/* Next comes the style of output */
	style = cp;

	for (finish = 0; !finish; )
	{
		switch (*cp)
		{
		case STYLE_TABLE:
		case STYLE_BREAKDOWN:
		case STYLE_INLINE:
			cp++;
			break;
		default:
			finish = 1;
			break;
		}
	}

	if (cp == style)
		grw_fatal("illegal content specifier \"%s\"", content);

	hd.style = grw_malloc((size_t)(cp - style + 1));
	sprintf(hd.style, "%.*s", (int)(cp - style), style);

	/* Remaining characters specify optional data to include */
	hd.inc = 0U;

	for ( ; *cp != '\0'; cp++)
	{
		switch (*cp)
		{
		case 'p':
			hd.inc |= INC_PASSES;
			break;
		case 'i':
			hd.inc |= INC_INTRO;
			break;
		case 'f':
			hd.inc |= INC_FULL;
			break;
		case 'x':
			hd.inc |= INC_EXTRA;
			break;
		case 'c':
			hd.inc |= INC_CONFIG;
			break;
		default:
			grw_fatal("illegal content specifier \"%s\"", content);
			break;
		}
	}

	/* Initialize remainder of private data */
	hd.ftr = ftr;
	hd.journal = grw_strdup(journal);
	hd.output = grw_strdup(output);
}


/*
 * grw_handleline()
 *
 * Handle the next line of the current journal file.
 * A journal line has the following format:
 *
 *	tag|f1a f1b f1c f1d f1e|f2
 *
 * where the fields (f1x, f2) are optional, depending on the tag.
 *
 *
 *	tag	Numeric value identifying the type of this journal line.
 *		One of the values from tet_jrnl.h.
 *	f1a	First subfield of first field. May be the empty string.
 *	f1b	Second subfield of first field. May be the empty string.
 *	f1c	Third subfield of first field. May be the empty string.
 *	f1d	Fourth subfield of first field. May be the empty string.
 *	f1e	Fifth subfield of first field. May be the empty string.
 *	f2	Second field. May be the empty string.
 */
void
grw_handleline(int tag, char *f1a, char *f1b, char *f1c, char *f1d, char *f1e,
	char *f2)
{
	/* Verify grw_beginjornal() has been called */
	if (!hd.inited)
	{
		grw_fatal("internal error: grw_handleline() called before grw_beginjournal()");
	}

	/* The tag parameter indicates what sort of journal line this is */
	switch (tag)
	{
	case TET_JNL_TCC_START:
		/* tag|version time date|who */

		/* Update state */
		hd.state |= STATE_TCC;

		/* Store fields */
		strcpy(hd.version, f1a);
		strcpy(hd.starttime, f1b);
		strcpy(hd.startdate, f1c);

		break;

	case TET_JNL_UNAME:
		/* tag|sysname nodename release version machine|text */
		sprintf(hd.sysinfo, "%s %s %s %s %s", f1a, f1b, f1c, f1d, f1e);
		break;

	case TET_JNL_INVOKE_TC:
		/* tag|activity testcase time|IClist */
		tcstart(MODE_EXECUTE, f1b);
		break;

	case TET_JNL_TCM_START:
		/* tag|activity version ICcount|text */

		/* State checking */
		if (!(hd.state & STATE_TC))
		{
			grw_err("TET_JNL_TCM_START encountered when not in TC");
			return;
		}

		/* If we are saving TC info, then set relevant flag */
		if (hd.curtc != NULL)
			hd.curtc->apiconforming = 1;

		break;

	case TET_JNL_CFG_START:
		/* tag|pathname mode|text or tag|nnn mode|text */

		/* Update state */
		hd.state |= STATE_CFG;

		/* If the output is to include configuration details, create a
		 * new structure to hold this configuration.
		 */
		if (hd.inc & INC_CONFIG)
		{
			/* Create a new configuration structure */
			hd.curcfg = grw_malloc(sizeof(*hd.curcfg));
			strcpy(hd.curcfg->pathname, f1a);
			strcpy(hd.curcfg->mode, f1b);
			hd.curcfg->vars = grw_createsvector(ALLOC_INCREMENT);
			hd.curcfg->next = NULL;

			/* Add this configuration to the end of the list */
			if (hd.cfghead == NULL)
			{
				hd.cfghead = hd.curcfg;
				hd.cfgtail = hd.cfghead;
			}
			else
			{
				hd.cfgtail->next = hd.curcfg;
				hd.cfgtail = hd.cfgtail->next;
			}
		}

		break;

	case TET_JNL_CFG_VALUE:
		/* tag||variable=value */

		/* If the output is to include configuration details, add this
		 * variable to the structure holding the current configuration.
		 */
		if (hd.inc & INC_CONFIG)
		{
			if (hd.curcfg == NULL)
			{
				grw_err("TET_JNL_CFG_VALUE encountered outside a configuration");
				return;
			}

			grw_addentry(hd.curcfg->vars, f2);
		}

		break;

	case TET_JNL_CFG_END:
		/* tag||text */

		/* If the output is to include configuration details, check the
		 * configuration state and finish the current configuration.
		 */
		if (hd.inc & INC_CONFIG)
		{
			if (hd.curcfg == NULL)
				grw_err("TET_JNL_CFG_END encountered outside a configuration");

			hd.curcfg = NULL;
		}

		/* Update state */
		hd.state &= ~STATE_CFG;

		break;

	case TET_JNL_TC_MESSAGE:
		/* tag||text */
		storexinfo("[TCC] ", f2);
		break;

	case TET_JNL_SCEN_OUT:
		/* tag||text */
		storexinfo("[Scenario] ", f2);
		break;

	case TET_JNL_TC_END:
		/* tag|activity status time|text */
		tcend(MODE_EXECUTE, f1b);
		break;

	case TET_USER_ABORT:
		/* tag|time|text */

		/* Store abort message */
		storeinfoline(INC_FULL, "TET_USER_ABORT", "[User abort] ", "",
			f2);
		break;

	case TET_JNL_CAPTURED_OUTPUT:
		/* tag|activity|text */

		/* If we require sufficient detail, then store */
		if ((hd.inc & INC_FULL)
			&& strchr(hd.style, STYLE_INLINE) != NULL)
		{
			/* State checking */
			if (!(hd.state & STATE_TC))
			{
				grw_err("TET_JNL_CAPTURED_OUTPUT encountered when not in TC");
				return;
			}

			/* If we're not saving TC/IC/TP info, then return */
			if (hd.curtc == NULL)
				return;

			/* Store the infoline */
			grw_addentry(hd.curtc->captured, f2);
		}

		break;

	case TET_JNL_BUILD_START:
		/* tag|activity testcase time|text */
		tcstart(MODE_BUILD, f1b);
		break;

	case TET_JNL_BUILD_END:
		/* tag|activity status time|text */
		tcend(MODE_BUILD, f1b);
		break;

	case TET_JNL_TP_START:
		/* tag|activity TPnumber time|text */

		/* State checking */
		if (!(hd.state & STATE_IC))
		{
			grw_err("TET_JNL_TP_START encountered when not in IC");
			return;
		}

		/* Update state */
		hd.state |= STATE_TP;

		/* If we're not saving IC/TP info, then return */
		if (hd.curic == NULL)
			return;

		/* Create a new entry for this TP */
		hd.curtp = grw_malloc(sizeof(*hd.curtp));

		if (grw_atoi(f1b, &hd.curtp->tpnumber) != 0)
			grw_err("invalid test purpose number \"%s\"", f1b);

		hd.curtp->infolines = grw_createsvector(ALLOC_INCREMENT);
		hd.curtp->result = grw_addentry(hd.results, RESULT_MISSING);
		hd.curtp->next = NULL;

		/* Add entry to the end of the current IC's TP list */
		if (hd.curic->tphead == NULL)
		{
			hd.curic->tphead = hd.curtp;
			hd.curic->tptail = hd.curic->tphead;
		}
		else
		{
			hd.curic->tptail->next = hd.curtp;
			hd.curic->tptail = hd.curic->tptail->next;
		}

		break;

	case TET_JNL_TP_RESULT:
		/* tag|activity TPnumber result time|result-name */

		/* State checking */
		if (!(hd.state & STATE_TP))
		{
			grw_err("TET_JNL_TP_RESULT encountered when not in TP");
			return;
		}

		/* Update state */
		hd.state &= ~STATE_TP;

		/* If we're not saving TP info, then return */
		if (hd.curtp == NULL)
			return;

		/* Update TP result */
		hd.curtp->result = grw_addentry(hd.results, f2);

		/* Leave current TP */
		hd.curtp = NULL;

		break;

	case TET_JNL_CLEAN_START:
		/* tag|activity testcase time|text */
		tcstart(MODE_CLEAN, f1b);
		break;

	case TET_JNL_CLEAN_END:
		/* tag|activity status time|text */
		tcend(MODE_CLEAN, f1b);
		break;

	case TET_JNL_IC_START:
		/* tag|activity ICnumber TPcount time|text */

		/* State checking */
		if (!(hd.state & STATE_TC))
		{
			grw_err("TET_JNL_IC_START encountered when not in TC");
			return;
		}

		/* Update state */
		hd.state |= STATE_IC;

		/* If we're not saving TC info, then return */
		if (hd.curtc == NULL)
			return;

		/* This should not be in a non-API conforming testcase */
		if (!hd.curtc->apiconforming)
		{
			grw_err("TET_JNL_IC_START found in non-API conforming TC");
			return;
		}

		/* Create a new entry for this IC */
		hd.curic = grw_malloc(sizeof(*hd.curic));

		if (grw_atoi(f1b, &hd.curic->icnumber) != 0)
		{
			grw_err("invalid invocable component number \"%s\"",
				f1b);
		}

		hd.curic->tphead = NULL;
		hd.curic->tptail = NULL;
		hd.curic->next = NULL;

		/* Add entry to the end of the current TC's IC list */
		if (hd.curtc->ichead == NULL)
		{
			hd.curtc->ichead = hd.curic;
			hd.curtc->ictail = hd.tctail->ichead;
		}
		else
		{
			hd.curtc->ictail->next = hd.curic;
			hd.curtc->ictail = hd.curtc->ictail->next;
		}

		break;

	case TET_JNL_IC_END:
		/* tag|activity ICnumber TPcount time|text */

		/* State checking */
		if (!(hd.state & STATE_IC))
		{
			grw_err("TET_JNL_IC_END encountered when not in IC");
			return;
		}

		/* Update state */
		hd.state &= ~(STATE_IC|STATE_TP);

		/* If we're not saving IC info, then return */
		if (hd.curic == NULL)
			return;

		/* Leave current IC */
		hd.curic = NULL;
		hd.curtp = NULL;

		break;

	case TET_JNL_TCM_INFO:
		/* tag|activity|text */

		/* Save the infoline, if required */
		storeinfoline(INC_EXTRA, "TET_JNL_TCM_INFO", "[TCM/API] ", "",
			f2);
		break;

	case TET_JNL_TC_INFO:
		/* tag|activity TPnumber context block sequence|text */

		/* Save the infoline, if required */
		storeinfoline(INC_FULL, "TET_JNL_TC_INFO", "", f1c, f2);
		break;

	case TET_JNL_PRL_START:
		/* tag|count|text */
		storexinfo("", "Parallel Start");
		break;

	case TET_JNL_PRL_END:
		/* tag||text */
		storexinfo("", "Parallel End");
		break;

	case TET_JNL_SEQ_START:
		/* tag||text */
		storexinfo("", "Implied Sequential Start");
		break;

	case TET_JNL_SEQ_END:
		/* tag||text */
		storexinfo("", "Implied Sequential End");
		break;

	case TET_JNL_RPT_START:
		/* tag|count|text */
		storexinfo("", "Repeat Start");
		break;

	case TET_JNL_RPT_END:
		/* tag||text */
		storexinfo("", "Repeat End");
		break;

	case TET_JNL_TLOOP_START:
		/* tag|seconds|text */
		storexinfo("", "Timed Loop Start");
		break;

	case TET_JNL_TLOOP_END:
		/* tag||text */
		storexinfo("", "Timed Loop End");
		break;

	case TET_JNL_RND_START:
		/* tag||text */
		storexinfo("", "Random Start");
		break;

	case TET_JNL_RND_END:
		/* tag||text */
		storexinfo("", "Random End");
		break;

	case TET_JNL_RMT_START:
		/* tag|sysids|text */
		storexinfo("", "Remote Start");
		break;

	case TET_JNL_RMT_END:
		/* tag||text */
		storexinfo("", "Remote End");
		break;

	case TET_JNL_DIST_START:
		/* tag|sysids|text */

		/* Update state */
		hd.state |= STATE_DISTRIB;

		/* Save info line */
		storexinfo("", "Distributed Start");

		break;

	case TET_JNL_DIST_END:
		/* tag||text */

		/* Save info line */
		storexinfo("", "Distributed End");

		/* Update state */
		hd.state &= ~STATE_DISTRIB;

		break;

	case TET_JNL_TCC_END:
		/* tag|time|text */
		hd.state &= ~STATE_TCC;
		break;

	case TET_JNL_CLEAN_OUTPUT:
	case TET_JNL_VAR_START:
	case TET_JNL_VAR_END:
		/* Never output by current version of tcc */
		grw_err("unsupported journal line tag: \"%d\"", tag);
		break;

	default:
		grw_err("unrecognized journal line tag: \"%d\"", tag);
		break;
	}
}


/*
 * tcstart()
 *
 * Handle the start of a testcase.
 *
 *	mode		Mode of this testcase. One of the MODE_.. values.
 *	testcase	Testcase path.
 */
static void
tcstart(unsigned int mode, char *testcase)
{
	char *name;

	/* Update state */
	hd.state |= STATE_TC;

	/* If this testcase isn't in the required mode, simply return */
	if (!(hd.mode & mode))
		return;

	/* Create a new entry for this TC */
	hd.curtc = grw_malloc(sizeof(*hd.curtc));
	memset(hd.curtc, 0, sizeof(*hd.curtc));
	hd.curtc->mode = mode;
	hd.curtc->apiconforming = 0;
	hd.curtc->infolines = grw_createsvector(ALLOC_INCREMENT);
	hd.curtc->captured = grw_createsvector(ALLOC_INCREMENT);
	hd.curtc->xinfo = grw_createsvector(ALLOC_INCREMENT);
	hd.curtc->status = -1;
	hd.curtc->ichead = NULL;
	hd.curtc->ictail = NULL;
	hd.curtc->next = NULL;

	/* Split testcase path and save in handler data */
	name = grw_namebase(testcase);
	if (name == NULL)
	{
		strcpy(hd.curtc->name, testcase);
		strcpy(hd.curtc->dir, "");
	}
	else
	{
		strcpy(hd.curtc->name, name);
		sprintf(hd.curtc->dir, "%.*s", (int)(name - testcase - 1),
			testcase);
	}

	/* Ensure tcname is not empty */
	if (hd.curtc->name[0] == '\0')
		strcpy(hd.curtc->name, "<Unknown testcase>");

	/* Add this testcase to the end of the list */
	if (hd.tchead == NULL)
	{
		hd.tchead = hd.curtc;
		hd.tctail = hd.tchead;
	}
	else
	{
		hd.tctail->next = hd.curtc;
		hd.tctail = hd.tctail->next;
	}

	hd.curic = NULL;
	hd.curtp = NULL;
}


/*
 * tcend()
 *
 * Handle the start of a testcase.
 *
 *	mode		Mode of this testcase. One of the MODE_.. values.
 *	status		Termination status of this testcase, as a string.
 */
static void
tcend(unsigned int mode, char *status)
{
	/* State checking */
	if (!(hd.state & STATE_TC)
		|| (hd.curtc != NULL && mode != hd.curtc->mode))
	{
		switch (mode)
		{
		case MODE_BUILD:
			grw_err("Build end encountered when not in build operation");
			break;
		case MODE_EXECUTE:
			grw_err("Testcase end encountered when not in testcase");
			break;
		case MODE_CLEAN:
			grw_err("Clean end encountered when not in clean operation");
			break;
		default:
			grw_fatal("internal error (file \"%s\", line %d)",
				__FILE__, __LINE__);
			break;
		}

		return;
	}

	/* Update state */
	hd.state &= ~(STATE_TC|STATE_IC|STATE_TP);

	/* If the current testcase isn't in the required mode, simply return */
	if (hd.curtc == NULL)
		return;

	/* Retrieve status */
	if (grw_atoi(status, &hd.curtc->status) != 0 || hd.curtc->status < 0)
	{
		grw_err("testcase has invalid status: \"%s\"", status);
		hd.curtc->status = -1;
	}

	/* Leave current TC */
	hd.curtc = NULL;
	hd.curic = NULL;
	hd.curtp = NULL;
}


/*
 * storeinfoline()
 *
 * Save this infoline if the style and detail of output requires it.
 *
 *	incopt		Include option required to save this output.
 *	tagname		Journal line tag.
 *	prefix		String with which to prefix the line. May be the empty
 *			string.
 *	context		Context information if present, or the empty string if
 *			not.
 *	infoline	Information line to be stored.
 */
static void
storeinfoline(int incopt, char *tagname, char *prefix, char *context,
	char *infoline)
{
	char buf[2 * TET_JNL_LEN];

	/* Don't save this infoline if it is not required in the output.
	 * (This is checked again before outputting it, but this saves a lot
	 * of space for the other cases).
	 */
	if (!(hd.inc & incopt) || strchr(hd.style, STYLE_INLINE) == NULL)
		return;

	/* State checking */
	if (!(hd.state & STATE_TC))
	{
		grw_err("%s encountered when not in TC", tagname);
		return;
	}

	/* If we're not saving TC/IC/TP info, then return */
	if (hd.curtc == NULL)
		return;

	/* If this is a distributed TETware run, get the system ID from the
	 * context and save with the infoline. The context does not always
	 * contain the system ID, and there is no 100% reliable method of
	 * obtaining it. However, by obeying the following rules we should be
	 * right for almost all cases seen in practice:
	 *
	 * 1. Must be inside 'distributed' or 'remote' scenario directives.
	 * 2. Must be in execute mode.
	 * 3. The context must have more than 5 digits, not starting with a
	 * minus.
	 */
	if ((hd.state & STATE_DISTRIB) && hd.curtc->mode == MODE_EXECUTE
		&& strlen(context) > 5
		&& isdigit((unsigned char)context[0])
		&& isdigit((unsigned char)context[1])
		&& isdigit((unsigned char)context[2])
		&& isdigit((unsigned char)context[3])
		&& isdigit((unsigned char)context[4]))
	{
		sprintf(buf, "[System %.3s] %s%s", context, prefix, infoline);
	}
	else
	{
		/* Simply prepend prefix */
		sprintf(buf, "%s%s", prefix, infoline);
	}

	/* Store the info message in the current TP if possible, or the current
	 * TC if we're not in a TP.
	 */
	if (hd.curtp == NULL)
	{
		grw_addentry(hd.curtc->infolines, buf);
	}
	else
	{
		grw_addentry(hd.curtp->infolines, buf);
	}
}


/*
 * storexinfo()
 *
 * Store extraneous journal information.
 *
 *	prefix		String with which to prefix the infoline.
 *	infoline	Information to be stored.
 */
static void
storexinfo(char *prefix, char *infoline)
{
	char buf[2 * TET_JNL_LEN];

	/* If this information isn't required in the output, simply return */
	if (!(hd.inc & INC_EXTRA))
		return;

	/* Prepend prefix and store the info message somewhere */
	sprintf(buf, "%s%s", prefix, infoline);

	if (hd.tctail == NULL)
	{
		grw_addentry(hd.initxinfo, buf);
	}
	else
	{
		grw_addentry(hd.tctail->xinfo, buf);
	}
}


/*
 * grw_endjournal()
 *
 * Handle the end of the current journal file.
 */
void
grw_endjournal(void)
{
	grw_formatter_t *ftr = hd.ftr;
	char *sp;
	char *modename;
	struct cfg *cfg;
	int ncfg = 0;
	int nvars;
	char **vars;
	char **vp;
	char *eq;

	/* Verify grw_beginjornal() has been called */
	if (!hd.inited)
	{
		grw_fatal("internal error: grw_handleline() called before grw_beginjournal()");
	}

	/* Begin document and print title */
	grw_begindocument(ftr, hd.output, DEFAULT_TITLE);
	grw_printh1(ftr, DEFAULT_TITLE);

	/* If the TCC end line was missing from the journal, report an error
	 * and put a warning into the report.
	 */
	if (hd.state & STATE_TCC)
	{
		grw_err("TCC end line missing - journal incomplete");
		grw_printhline(ftr);
		grw_printh2(ftr, "Warning: TCC end line missing - journal incomplete");
		grw_printhline(ftr);
	}

	/* Print overview information, if required */
	if (hd.inc & INC_INTRO)
	{
		grw_starttable(ftr, GRW_BORDER_NONE);
		grw_startrow(ftr);
		grw_printcell(ftr, GRW_HALIGN_LEFT, NULL, "TETware version:");
		grw_printcell(ftr, GRW_HALIGN_LEFT, NULL, hd.version);
		grw_endrow(ftr);
		grw_startrow(ftr);
		grw_printcell(ftr, GRW_HALIGN_LEFT, NULL,
			"System Information:");
		grw_printcell(ftr, GRW_HALIGN_LEFT, NULL, hd.sysinfo);
		grw_endrow(ftr);
		grw_startrow(ftr);
		grw_printcell(ftr, GRW_HALIGN_LEFT, NULL, "Date of test run:");

		/* Print the date in ISO 8601 (YYYY-MM-DD) if possible, to
		 * make it a little clearer.
		 */
		if (strlen(hd.startdate) == 8)
		{
			grw_printcell(ftr, GRW_HALIGN_LEFT, NULL,
				"%.4s-%.2s-%s", hd.startdate, hd.startdate + 4,
				hd.startdate + 6);
		}
		else
		{
			grw_printcell(ftr, GRW_HALIGN_LEFT, NULL,
				hd.startdate);
		}

		grw_endrow(ftr);
		grw_startrow(ftr);
		grw_printcell(ftr, GRW_HALIGN_LEFT, NULL, "Start time:");
		grw_printcell(ftr, GRW_HALIGN_LEFT, NULL, hd.starttime);
		grw_endrow(ftr);
		grw_endtable(ftr);
		grw_printhline(ftr);
	}

	/* Print configuration information, if required */
	if (hd.inc & INC_CONFIG)
	{
		grw_printh2(ftr, "Configuration Information");

		for (cfg = hd.cfghead; cfg != NULL; cfg = cfg->next)
		{
			/* Determine configuration mode */
			if (strcmp(cfg->mode, "0") == 0)
			{
				if (!(hd.mode & MODE_BUILD))
					continue;

				modename = "Build mode";
			}
			else if (strcmp(cfg->mode, "1") == 0)
			{
				if (!(hd.mode & MODE_EXECUTE))
					continue;

				modename = "Execute mode";
			}
			else if (strcmp(cfg->mode, "2") == 0)
			{
				if (!(hd.mode & MODE_CLEAN))
					continue;

				modename = "Clean mode";
			}
			else if (strcmp(cfg->mode, "3") == 0)
			{
				modename = "Distributed";
			}
			else
			{
				modename = "Unknown mode";
			}

			ncfg++;

			grw_printh3(ftr, "%s configuration for %s", modename,
				cfg->pathname);

			grw_starttable(ftr, GRW_BORDER_NARROW);
			grw_startrow(ftr);
			grw_printcell(ftr, GRW_HALIGN_CENTER|GRW_FONT_BOLD,
				NULL, "Variable");
			grw_printcell(ftr, GRW_HALIGN_CENTER|GRW_FONT_BOLD,
				NULL, "Value");
			grw_endrow(ftr);

			nvars = grw_getsize(cfg->vars);
			vars = grw_getentries(cfg->vars);

			for (vp = vars; vp < vars + nvars; vp++)
			{
				grw_startrow(ftr);

				eq = strchr(*vp, '=');
				if (eq == NULL)
				{
					grw_printcell(ftr, GRW_HALIGN_LEFT,
						NULL, "%s", *vp);
					grw_printcell(ftr, GRW_HALIGN_LEFT,
						NULL, "");
				}
				else
				{
					grw_printcell(ftr, GRW_HALIGN_LEFT,
						NULL, "%.*s", (int)(eq - *vp),
						*vp);
					grw_printcell(ftr, GRW_HALIGN_LEFT,
						NULL, "%s", eq + 1);
				}

				grw_endrow(ftr);
			}

			grw_endtable(ftr);
		}

		if (ncfg == 0)
		{
			grw_print(ftr, GRW_FONT_ITALIC,
				"No applicable configurations");
		}

		grw_printhline(ftr);
	}

	/* Print test case results in required style(s) */
	for (sp = hd.style; *sp != '\0'; sp++)
	{
		switch (*sp)
		{
		case STYLE_TABLE:
		case STYLE_BREAKDOWN:
			if (hd.mode & MODE_BUILD)
				print_bc_summary(ftr, MODE_BUILD, *sp);

			if (hd.mode & MODE_EXECUTE)
				print_execute_summary(ftr, *sp);

			if (hd.mode & MODE_CLEAN)
				print_bc_summary(ftr, MODE_CLEAN, *sp);

			if (*sp == STYLE_TABLE)
				grw_printhline(ftr);

			break;

		case STYLE_INLINE:
			print_inline(ftr);
			grw_printhline(ftr);

			break;

		default:
			grw_fatal("internal error (file \"%s\", line %d)",
				__FILE__, __LINE__);
			break;
		}
	}

	/* End document */
	grw_enddocument(ftr);

	/* Don't need to bother with initdata(), as it's done in
	 * grw_beginjournal().
	 */
}


/*
 * Compare result codes. For use with qsort().
 * N.B. Comparison is the "wrong"-way round, so that we get reverse ordering.
 *
 *	e1	First element. Will be a struct rescode *.
 *	e2	Second element. Will be a struct rescode *.
 */
static int
cmprescodes(const void *e1, const void *e2)
{
	return ((struct rescode *)e2)->count - ((struct rescode *)e1)->count;
}


/*
 * print_execute_summary()
 *
 * Print summary of execution mode results. If there are no execute mode
 * results, then nothing is printed. If there are execute mode results, the
 * output depends on the `style' parameter.
 *
 *	ftr		Formatter.
 *	style		One of the following values:
 *
 *		STYLE_TABLE
 *			Prints a summary table giving the number of occurrences
 *			of each result code in the journal.
 *		STYLE_BREAKDOWN
 *			Prints a breakdown for each result code, containing a
 *			list of the test purposes which gave that result.
 */
static void
print_execute_summary(grw_formatter_t *ftr, int style)
{
	int nres;
	char **res;
	struct rescode *rescodes;
	int i;
	struct rescode *rc;
	struct tc *tc;
	struct ic *ic;
	struct tp *tp;
	int total;
	int subcount;

	/* Initialization */
	nres = grw_getsize(hd.results);
	res = grw_getentries(hd.results);
	total = 0;
	rescodes = grw_malloc(nres * sizeof(*rescodes));

	for (i = 0; i < nres; i++)
	{
		rescodes[i].code = res[i];
		rescodes[i].count = 0;	
	}

	/* Count the occurrence of result codes */
	for (rc = rescodes; rc < rescodes + nres; rc++)
	{
		/* Skip passes if required */
		if (strcmp(rc->code, RESULT_PASS) == 0
			&& style == STYLE_BREAKDOWN && !(hd.inc & INC_PASSES))
		{
			continue;
		}

		for (tc = hd.tchead; tc != NULL; tc = tc->next)
		{
			/* Only interested in execute mode results */
			if (tc->mode != MODE_EXECUTE)
				continue;

			for (ic = tc->ichead; ic != NULL; ic = ic->next)
			{
				for (tp = ic->tphead; tp != NULL; tp = tp->next)
				{
					/* N.B. could use '!=' since we've used
					 * the same string for the same code,
					 * but this is more future-proof.
					 */
					if (strcmp(rc->code, tp->result) == 0)
						rc->count++;
				}
			}
		}

		total += rc->count;
	}

	/* Order the result codes from highest occurence to lowest */
	qsort(rescodes, (size_t)nres, sizeof(*rescodes), cmprescodes);

	/* Print section headers */
	if (style == STYLE_BREAKDOWN)
	{
		grw_printh2(ftr, "Execute mode result breakdown");
	}
	else
	{
		/* Print section header, the start of the table and the column
		 * headings.
		 */
		grw_printh2(ftr, "Execute mode summary");
	}

	/* If there are no results, print that fact under the heading and
	 * finish.
	 */
	if (total == 0)
	{
		grw_starttable(ftr, GRW_BORDER_NONE);
		grw_startrow(ftr);
		grw_printcell(ftr, GRW_HALIGN_LEFT|GRW_FONT_ITALIC, NULL,
			"No results");
		grw_endrow(ftr);
		grw_endtable(ftr);

		if (style == STYLE_BREAKDOWN)
			grw_printhline(ftr);

		return;
	}

	/* Print start of the summary table and the column headings */
	if (style == STYLE_TABLE)
	{
		grw_starttable(ftr, GRW_BORDER_NARROW);
		grw_startrow(ftr);
		grw_printcell(ftr, GRW_HALIGN_CENTER|GRW_FONT_BOLD, NULL,
			"Result Code");
		grw_printcell(ftr, GRW_HALIGN_CENTER|GRW_FONT_BOLD, NULL,
			"Count");
		grw_endrow(ftr);
	}

	/* Print one table row for each result code used in test run */
	for (rc = rescodes; rc < rescodes + nres; rc++)
	{
		/* If this is a summary breakdown, and this result code has
		 * not occurred, or if it's a pass and passes are not to be
		 * included in the report, then skip to the next result code.
		 */
		if (style == STYLE_BREAKDOWN
			&& (rc->count == 0
			|| (strcmp(rc->code, RESULT_PASS) == 0
			&& !(hd.inc & INC_PASSES))))
		{
			continue;
		}

		/* Print heading for this result code */
		if (style == STYLE_BREAKDOWN)
		{
			grw_printh3(ftr, "%s", rc->code);
			grw_starttable(ftr, GRW_BORDER_NARROW);
			grw_startrow(ftr);
			grw_printcell(ftr, GRW_HALIGN_CENTER|GRW_FONT_BOLD,
				NULL, "Test case");
			grw_printcell(ftr, GRW_HALIGN_CENTER|GRW_FONT_BOLD,
				NULL, "Test purposes (IC.TP)");
			grw_endrow(ftr);
		}

		/* Process each testcase in turn */
		for (tc = hd.tchead; tc != NULL; tc = tc->next)
		{
			/* Only interested in execute mode results */
			if (tc->mode != MODE_EXECUTE)
				continue;

			/* Process each IC in turn */
			subcount = 0;

			for (ic = tc->ichead; ic != NULL; ic = ic->next)
			{
				for (tp = ic->tphead; tp != NULL; tp = tp->next)
				{
					/* N.B. could use '!=' since we've used
					 * the same string for the same code,
					 * but this is more future-proof.
					 */
					if (strcmp(rc->code, tp->result) != 0)
						continue;

					subcount++;

					if (style != STYLE_BREAKDOWN)
						continue;

					/* If this is the first TP for this
					 * testcase with this result code, then
					 * start the row.
					 */
					if (subcount == 1)
					{
						grw_startrow(ftr);
						grw_printcell(ftr,
							GRW_HALIGN_LEFT,
							rc->code, "%s/%s",
							tc->dir, tc->name);
						grw_startcell(ftr,
							GRW_HALIGN_LEFT,
							rc->code);
					}
					else
					{
						grw_print(ftr, 0UL, ", ");
					}

					/* Print the IC.TP numbers */
					grw_print(ftr, 0UL, "%d.%d",
						ic->icnumber, tp->tpnumber);
				}
			}

			if (style == STYLE_BREAKDOWN && subcount > 0)
			{
				grw_endcell(ftr);
				grw_endrow(ftr);
			}
		}

		/* If we got at least one occurrence of this result code, then	
		 * start/complete the entry for it.
		 */
		if (rc->count > 0)
		{
			if (style == STYLE_TABLE)
			{
				/* Print row for this result code */
				grw_startrow(ftr);
				grw_printcell(ftr, GRW_HALIGN_LEFT, rc->code,
					"%s", rc->code);
				grw_printcell(ftr, GRW_HALIGN_RIGHT, rc->code,
					"%d", rc->count);
				grw_endrow(ftr);
			}
			else
			{
				grw_endtable(ftr);
			}
		}
	}

	/* Print end of summary table */
	if (style == STYLE_TABLE)
	{
		/* Print a row for the total number of tests */
		grw_startrow(ftr);
		grw_printcell(ftr, GRW_HALIGN_LEFT|GRW_FONT_BOLD, NULL,
			"Total");
		grw_printcell(ftr, GRW_HALIGN_RIGHT|GRW_FONT_BOLD, NULL, "%d",
			total);
		grw_endrow(ftr);

		/* Print the end of the table */
		grw_endtable(ftr);
	}
	else
	{
		grw_printhline(ftr);
	}
}


/*
 * print_bc_summary()
 *
 * Print summary of build or clean mode results. If there are no results for
 * the relevant mode, then nothing is printed. If there are results for the
 * relevant mode, the output depends on the `style' parameter.
 *
 *	ftr		Formatter.
 *	mode		Mode - either MODE_BUILD or MODE_CLEAN.
 *	style		One of the following values:
 *
 *		STYLE_TABLE
 *			Prints a summary table giving the number of successes
 *			and failures.
 *		STYLE_BREAKDOWN
 *			Prints a breakdown for each result code, containing a
 *			list of which testcases succeeded and which failed.
 *
 *		[Note that a build or clean operation is considered to have
 *		one result - success or failure. This applies to API-conforming
 *		and non-API conforming build and clean tools.]
 */
static void
print_bc_summary(grw_formatter_t *ftr, unsigned int mode, int style)
{
	struct tc *tc;
	struct ic *ic;
	struct tp *tp;
	int success;
	int nsuccess;
	int nfailure;
	int gotpass;
	int gotnonpass;

	/* Initialization */
	nsuccess = 0;
	nfailure = 0;

	/* Perform an initial pass through the TC/IC/TP structures. Determine
	 * a single result for each testcase and store this in the `success'
	 * field in the tc structure. Also, count the successes and failures.
	 */
	for (tc = hd.tchead; tc != NULL; tc = tc->next)
	{
		if (tc->mode != mode)
			continue;

		/* Exit status takes priority */
		if (tc->status != 0)
		{
			success = 0;
		}
		else if (tc->apiconforming)
		{
			/* An API-conforming testcase shall be judged success
			 * when it had at least one PASS result, and no
			 * non-PASS results. (Normally will have only one IC/TP
			 * anyway).
			 */
			gotpass = 0;
			gotnonpass = 0;

			for (ic = tc->ichead; ic != NULL; ic = ic->next)
			{
				for (tp = ic->tphead; tp != NULL; tp = tp->next)
				{
					if (strcmp(tp->result, RESULT_PASS)
						== 0)
					{
						gotpass = 1;
					}
					else
					{
						gotnonpass = 1;
					}
				}
			}

			success = (!gotnonpass && gotpass);
		}
		else
		{
			/* The success of a non-API conforming testcase is
			 * determined entirely by the exit status.
			 */
			success = 1;
		}

		/* Save success state in testcase */
		tc->success = success;

		/* Increment counts */
		if (success)
			nsuccess++;
		else
			nfailure++;
	}

	/* If we don't have any results, don't print anything */
	if (nsuccess + nfailure == 0)
		return;

	if (style == STYLE_TABLE)
	{
		/* Print section header */
		if (mode == MODE_BUILD)
			grw_printh2(ftr, "Build mode summary");
		else
			grw_printh2(ftr, "Clean mode summary");

		/* Print the start of the table and the column headings */
		grw_starttable(ftr, GRW_BORDER_NARROW);
		grw_startrow(ftr);
		grw_printcell(ftr, GRW_HALIGN_CENTER|GRW_FONT_BOLD, NULL,
			"Result");
		grw_printcell(ftr, GRW_HALIGN_CENTER|GRW_FONT_BOLD, NULL,
			"Count");
		grw_endrow(ftr);

		/* Successes */
		grw_startrow(ftr);
		grw_printcell(ftr, GRW_HALIGN_LEFT, GRW_CLASS_SUCCESS,
			"Success");
		grw_printcell(ftr, GRW_HALIGN_RIGHT, GRW_CLASS_SUCCESS, "%d",
			nsuccess);
		grw_endrow(ftr);

		/* Failures */
		grw_startrow(ftr);
		grw_printcell(ftr, GRW_HALIGN_LEFT, GRW_CLASS_FAILURE,
			"Failure");
		grw_printcell(ftr, GRW_HALIGN_RIGHT, GRW_CLASS_FAILURE, "%d",
			nfailure);
		grw_endrow(ftr);

		/* Print a row for the total number of tests */
		grw_startrow(ftr);
		grw_printcell(ftr, GRW_HALIGN_LEFT|GRW_FONT_BOLD, NULL,
			"Total");
		grw_printcell(ftr, GRW_HALIGN_RIGHT|GRW_FONT_BOLD, NULL, "%d",
			nsuccess + nfailure);
		grw_endrow(ftr);

		/* Print the end of the table */
		grw_endtable(ftr);
	}
	else
	{
		/* Print section header */
		if (mode == MODE_BUILD)
			grw_printh2(ftr, "Build mode result breakdown");
		else
			grw_printh2(ftr, "Clean mode result breakdown");

		/* Print in a table of testcase results */
		grw_starttable(ftr, GRW_BORDER_NARROW);
		grw_startrow(ftr);

		grw_printcell(ftr, GRW_HALIGN_CENTER|GRW_FONT_BOLD,
			NULL, "Testcase");
		grw_printcell(ftr, GRW_HALIGN_CENTER|GRW_FONT_BOLD,
			NULL, "Result");
		grw_endrow(ftr);

		for (tc = hd.tchead; tc != NULL; tc = tc->next)
		{
			if (tc->mode != mode)
				continue;

			if (tc->success)
			{
				if (hd.inc & INC_PASSES)
				{
					grw_startrow(ftr);
					grw_printcell(ftr, GRW_HALIGN_LEFT,
						GRW_CLASS_SUCCESS, "%s/%s",
						tc->dir, tc->name);
					grw_printcell(ftr, GRW_HALIGN_LEFT,
						GRW_CLASS_SUCCESS, "Success");
					grw_endrow(ftr);
				}
			}
			else
			{
				grw_startrow(ftr);
				grw_printcell(ftr, GRW_HALIGN_LEFT,
					GRW_CLASS_FAILURE, "%s/%s", tc->dir,
					tc->name);
				grw_printcell(ftr, GRW_HALIGN_LEFT,
					GRW_CLASS_FAILURE, "Failure");
				grw_endrow(ftr);
			}
		}

		/* Finish table and print a horizontal line to complete this
		 * section.
		 */
		grw_endtable(ftr);
		grw_printhline(ftr);
	}
}


/*
 * print_inline()
 *
 * Print an inline report.
 *
 *	ftr	Formatter.
 */
static void
print_inline(grw_formatter_t *ftr)
{
	struct tc *tc;
	struct ic *ic;
	struct tp *tp;
	int ntps;

	/* Print title */
	grw_printh2(ftr, "Inline report");

	/* Print additional scenario information, if required */
	printxinfo(ftr, hd.initxinfo);

	/* Print one section for each testcase */
	for (tc = hd.tchead; tc != NULL; tc = tc->next)
	{
		ntps = 0;

		/* Process each IC */
		for (ic = tc->ichead; ic != NULL; ic = ic->next)
		{
			/* Process each TP */
			for (tp = ic->tphead; tp != NULL; tp = tp->next)
			{
				/* If this is a pass and we're not including
				 * passes, skip to next TP.
				 */
				if (strcmp(tp->result, RESULT_PASS) == 0
					&& !(hd.inc & INC_PASSES))
				{
					continue;
				}

				/* If we haven't already started a table for
				 * this testcase, start it and print the column
				 * headings.
				 */
				if (ntps++ == 0)
				{
					printtcintro(ftr, tc);

					grw_startrow(ftr);
					grw_printcell(ftr,
						GRW_FONT_BOLD|GRW_HALIGN_CENTER,
						NULL, "IC");
					grw_printcell(ftr,
						GRW_FONT_BOLD|GRW_HALIGN_CENTER,
						NULL, "TP");
					grw_printcell(ftr,
						GRW_FONT_BOLD|GRW_HALIGN_CENTER,
						NULL, "Result");

					if (hd.inc & INC_FULL)
					{
						grw_printcell(ftr,
							GRW_FONT_BOLD|GRW_HALIGN_CENTER,
							NULL,
							"Information lines");
					}

					grw_endrow(ftr);
				}

				/* Print the row for this test */
				grw_startrow(ftr);
				grw_printcell(ftr, GRW_HALIGN_RIGHT,
					tp->result, "%d", ic->icnumber);
				grw_printcell(ftr, GRW_HALIGN_RIGHT,
					tp->result, "%d", tp->tpnumber);
				grw_printcell(ftr, GRW_HALIGN_LEFT, tp->result,
					"%s", tp->result);

				/* Print the info lines if required */
				if (hd.inc & INC_FULL)
				{
					if (grw_getsize(tp->infolines) > 0)
					{
						grw_startcell(ftr,
							GRW_HALIGN_LEFT,
							tp->result);
						printulist(ftr, tp->infolines);
						grw_endcell(ftr);
					}
					else
					{
						grw_printcell(ftr,
							GRW_HALIGN_LEFT,
							tp->result, "");
					}
				}

				grw_endrow(ftr);
			}
		}

		/* If this testcase has no test purposes, print something
		 * useful out. If its is API conforming, then presumably
		 * something went wrong. If it is non-API conforming, then this
		 * is exactly how things should be, and we need to use the
		 * exit status of the TC to determine the test result.
		 *
		 * [Non-API conforming testcases containing ICs/TPs should be
		 * detected while parsing the journal]
		 *
		 * If this is a successful non-API conforming testcase, then
		 * there's no need to print anything.
		 */
		if ((tc->ichead == NULL || tc->ichead->tphead == NULL)
			&& (tc->apiconforming || tc->status != 0
			|| (hd.inc & INC_PASSES)))
		{
			printtcintro(ftr, tc);

			grw_startrow(ftr);
			grw_printcell(ftr, GRW_HALIGN_LEFT|GRW_FONT_BOLD,
				NULL, "Test Status");
			grw_endrow(ftr);
			grw_startrow(ftr);

			if (tc->apiconforming)
			{
				if (tc->status == 0)
				{
					grw_printcell(ftr, GRW_FONT_ITALIC,
						NULL,
						"No test results in testcase");
				}
			}
			else
			{
				if (tc->status == 0)
				{
					grw_printcell(ftr, GRW_HALIGN_LEFT,
						GRW_CLASS_SUCCESS,
						"Success (0)");
				}
				else
				{
					grw_printcell(ftr, GRW_HALIGN_LEFT,
						GRW_CLASS_FAILURE,
						"Failure (%d)", tc->status);
				}
			}

			grw_endrow(ftr);
			grw_endtable(ftr);
		}
		else if (ntps > 0)
		{
			grw_endtable(ftr);
		}

		/* Print additional scenario information, if required */
		printxinfo(ftr, tc->xinfo);
	}
}


/*
 * printxinfo()
 *
 * Print extraneous information. This does nothing if the information is not
 * required or if there is no information stored (i.e. if `xinfo' has no
 * elements).
 *
 *	ftr	Formatter.
 *	xinfo	Extraneous information lines.
 */
static void
printxinfo(grw_formatter_t *ftr, grw_svector_t *xinfo)
{
	if ((hd.inc & INC_EXTRA) && grw_getsize(xinfo) > 0)
	{
		grw_printh3(ftr, "Scenario Information");
		printulist(ftr, xinfo);
	}
}


/*
 * printtcintro()
 *
 * Print the introduction to a testcase for an inline report.
 *
 *	ftr	Formatter.
 *	tc	The testcase for which the introduction is to be printed.
 */
static void
printtcintro(grw_formatter_t *ftr, struct tc *tc)
{
	char *suffix;

	/* If the journal is for more than one mode, print the mode with the
	 * testcase name.
	 */
	switch (hd.mode)
	{
	case MODE_BUILD:
	case MODE_CLEAN:
	case MODE_EXECUTE:
		suffix = "";
		break;
	default:
		switch (tc->mode)
		{
		case MODE_BUILD:
			suffix = " [build]";
			break;
		case MODE_CLEAN:
			suffix = " [clean]";
			break;
		case MODE_EXECUTE:
			suffix = " [execute]";
			break;
		default:
			suffix = " [invalid mode]";
			break;
		}
		break;
	}

	/* Print section heading for this testcase */
	grw_printh3(ftr, "%s/%s%s", tc->dir, tc->name, suffix);

	/* If this is an API-conforming testcase and the status is non-zero,
	 * print that out now.
	 */
	if (tc->apiconforming && tc->status != 0)
	{
		grw_starttable(ftr, GRW_BORDER_NARROW);
		grw_startrow(ftr);
		grw_printcell(ftr, GRW_FONT_BOLD, NULL, "Test Status");
		grw_endrow(ftr);
		grw_startrow(ftr);

		if (tc->status < 0)
		{
			grw_printcell(ftr, GRW_FONT_ITALIC, GRW_CLASS_FAILURE,
				"Test case has missing or unrecognized exit status");
		}
		else if (tc->status > 0)
		{
			grw_printcell(ftr, GRW_FONT_ITALIC, GRW_CLASS_SUCCESS,
				"Test case has failure exit status (%d)",
				tc->status);
		}

		grw_endrow(ftr);
		grw_endtable(ftr);
	}

	/* If required, print "extra" testcase information, i.e. captured
	 * output and information lines from startup and/or cleanup.
	 */
	if (hd.inc & INC_EXTRA)
	{
		printlinetable(ftr, "Captured Output", tc->captured);
		printlinetable(ftr, "Information lines", tc->infolines);
	}

	/* Start results table */
	grw_starttable(ftr, GRW_BORDER_NARROW);
}


/*
 * printlinetable()
 *
 * Print a table containing a list of lines.
 *
 *	ftr	Formatter.
 *	title	Title of table.
 *	lines	Lines to print in table.
 */
static void
printlinetable(grw_formatter_t *ftr, char *title, grw_svector_t *lines)
{
	if (grw_getsize(lines) > 0)
	{
		grw_starttable(ftr, GRW_BORDER_NARROW);
		grw_startrow(ftr);
		grw_printcell(ftr, GRW_FONT_BOLD|GRW_HALIGN_LEFT, NULL, "%s",
			title);
		grw_endrow(ftr);
		grw_startrow(ftr);
		grw_startcell(ftr, GRW_HALIGN_LEFT, NULL);
		printulist(ftr, lines);
		grw_endcell(ftr);
		grw_endrow(ftr);
		grw_endtable(ftr);
	}
}


/*
 * printulist()
 *
 * Print an unordered list, given a string vector.
 *
 *	ftr	Formatter.
 *	items	String vector containing the list items.
 */
static void
printulist(grw_formatter_t *ftr, grw_svector_t *items)
{
	int nlines;
	char **lines;
	char **lp;

	grw_startulist(ftr);

	nlines = grw_getsize(items);
	lines = grw_getentries(items);

	for (lp = lines; lp < lines + nlines; lp++)
		grw_printlistentry(ftr, 0UL, "%s", *lp);

	grw_endulist(ftr);
}
