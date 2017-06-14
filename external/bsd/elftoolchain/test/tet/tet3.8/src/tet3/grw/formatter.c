/*
 *	SCCS: @(#)formatter.c	1.2 (02/11/06)
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
static char sccsid[] = "@(#)formatter.c	1.2 (02/11/06) TETware release 3.8";
#endif

/************************************************************************

SCCS:		@(#)formatter.c	1.2 02/11/06 TETware release 3.8
NAME:		formatter.c
PRODUCT:	TETware Generic Report Writer.
AUTHOR:		Matthew Hails, UniSoft Ltd.
DATE CREATED:	19 July 2000
SYNOPSIS:

	grw_formatter_t * grw_createformatter(
		void (*begindocument)(grw_formatter_t *ftr,
		char *output, char *title),
		void (*enddocument)(grw_formatter_t *ftr),
		void (*printh1)(grw_formatter_t *ftr, char *heading),
		void (*printh2)(grw_formatter_t *ftr, char *heading),
		void (*printh3)(grw_formatter_t *ftr, char *heading),
		void (*print)(grw_formatter_t *ftr, unsigned long options,
			char *text),
		void (*starttable)(grw_formatter_t *ftr, unsigned long options),
		void (*endtable)(grw_formatter_t *ftr),
		void (*startrow)(grw_formatter_t *ftr),
		void (*endrow)(grw_formatter_t *ftr),
		void (*startcell)(grw_formatter_t *ftr, unsigned long options,
			char *class),
		void (*endcell)(grw_formatter_t *ftr),
		void (*startulist)(grw_formatter_t *ftr),
		void (*endulist)(grw_formatter_t *ftr),
		void (*printlistentry)(grw_formatter_t *ftr,
			unsigned long options, char *text),
		void (*breakline)(grw_formatter_t *ftr),
		void (*printhline)(grw_formatter_t *ftr),
		void *pdata)
	void grw_begindocument(grw_formatter_t *ftr, char *output,
		char *title)
	void grw_enddocument(grw_formatter_t *ftr)
	void grw_printh1(grw_formatter_t *ftr, char *fmt, ...)
	void grw_printh2(grw_formatter_t *ftr, char *fmt, ...)
	void grw_printh3(grw_formatter_t *ftr, char *fmt, ...)
	void grw_print(grw_formatter_t *ftr, unsigned long options,
		char *fmt, ...)
	void grw_starttable(grw_formatter_t *ftr, unsigned long options)
	void grw_endtable(grw_formatter_t *ftr)
	void grw_startrow(grw_formatter_t *ftr)
	void grw_endrow(grw_formatter_t *ftr)
	void grw_printcell(grw_formatter_t *ftr, unsigned long options,
		char *class, char *fmt, ...)
	void grw_startcell(grw_formatter_t *ftr, unsigned long options,
		char *class)
	void grw_endcell(grw_formatter_t *ftr)
	void grw_startulist(grw_formatter_t *ftr)
	void grw_endulist(grw_formatter_t *ftr)
	void grw_printlistentry(grw_formatter_t *ftr, unsigned long options,
		char *fmt, ...)
	void grw_breakline(grw_formatter_t *ftr)
	void grw_printhline(grw_formatter_t *ftr)


DESCRIPTION:

	Code to handle the production of different document formats.
	This is a generic layer which uses the grw_formatter_t object
	Passed to the routines.

************************************************************************/


#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdarg.h>
#include "tet_jrnl.h"
#include "grw.h"
#include "formatter.h"


/* Maximum length of one line in the output. Lines longer than this are
 * truncated. (Should never happen).
 */
#define MAXLINELEN	(TET_JNL_LEN * 2)

/* Flag values for state of generated document */
#define FSTATE_DOC	0x0001
#define FSTATE_TABLE	0x0002
#define FSTATE_TR	0x0004
#define FSTATE_TD	0x0008
#define FSTATE_UL	0x0010


/* Local prototypes */
static void checkstate(grw_formatter_t *ftr, unsigned int reqstate);
static void grw_vsnprintf(char *s, size_t maxsize, char *format, va_list ap);


/*
 * grw_createformatter()
 *
 * Create a new formatter.
 *
 *	begindocument	Pointer to function which begins a new document.
 *	enddocument	Pointer to function which ends current document.
 *	printh1		Pointer to function which prints a level 1 heading.
 *	printh2		Pointer to function which prints a level 2 heading.
 *	printh3		Pointer to function which prints a level 3 heading.
 *	print		Pointer to function which prints text.
 *	starttable	Pointer to function which starts a table.
 *	endtable	Pointer to function which ends a table.
 *	startrow	Pointer to function which starts a table row.
 *	endrow		Pointer to function which ends a table row.
 *	startcell	Pointer to function which starts a table cell.
 *	endcell		Pointer to function which ends a table cell.
 *	startulist	Pointer to function which starts an unordered list.
 *	endulist	Pointer to function which ends an unordered list.
 *	printlistentry	Pointer to function which prints an entry in a list.
 *	breakline	Pointer to function which prints a line break.
 *	printhline	Pointer to function which prints a horizontal line.
 *	pdata		Pointer to formatter private data.
 *
 * Returns the new formatter structure.
 */
grw_formatter_t *
grw_createformatter(
	void (*begindocument)(grw_formatter_t *ftr, char *output, char *title),
	void (*enddocument)(grw_formatter_t *ftr),
	void (*printh1)(grw_formatter_t *ftr, char *heading),
	void (*printh2)(grw_formatter_t *ftr, char *heading),
	void (*printh3)(grw_formatter_t *ftr, char *heading),
	void (*print)(grw_formatter_t *ftr, unsigned long options, char *text),
	void (*starttable)(grw_formatter_t *ftr, unsigned long options),
	void (*endtable)(grw_formatter_t *ftr),
	void (*startrow)(grw_formatter_t *ftr),
	void (*endrow)(grw_formatter_t *ftr),
	void (*startcell)(grw_formatter_t *ftr, unsigned long options,
		char *class),
	void (*endcell)(grw_formatter_t *ftr),
	void (*startulist)(grw_formatter_t *ftr),
	void (*endulist)(grw_formatter_t *ftr),
	void (*printlistentry)(grw_formatter_t *ftr, unsigned long options,
		char *text),
	void (*breakline)(grw_formatter_t *ftr),
	void (*printhline)(grw_formatter_t *ftr),
	void *pdata)
{
	grw_formatter_t *ftr;

	ftr = grw_malloc(sizeof(*ftr));
	ftr->begindocument = begindocument;
	ftr->enddocument = enddocument;
	ftr->printh1 = printh1;
	ftr->printh2 = printh2;
	ftr->printh3 = printh3;
	ftr->print = print;
	ftr->starttable = starttable;
	ftr->endtable = endtable;
	ftr->startrow = startrow;
	ftr->endrow = endrow;
	ftr->startcell = startcell;
	ftr->endcell = endcell;
	ftr->startulist = startulist;
	ftr->endulist = endulist;
	ftr->printlistentry = printlistentry;
	ftr->breakline = breakline;
	ftr->printhline = printhline;
	ftr->fp = NULL;
	ftr->output = NULL;
	ftr->pdata = pdata;

	return ftr;
}


/*
 * grw_begindocument()
 *
 * Begin a new document.
 *
 *	ftr		Formatter.
 *	output		Path of output file. "-" specifies standard output.
 *	title		Non-visible title of the document used in <title> tag.
 */
void
grw_begindocument(grw_formatter_t *ftr, char *output, char *title)
{
	/* Check state */
	checkstate(ftr, 0U);

	/* Initialize private data, opening file for output if necessary */
	if (strcmp(output, "-") == 0)
	{
		ftr->fp = stdout;
	}
	else
	{
		ftr->fp = fopen(output, "w");
		if (ftr->fp == NULL)
			grw_fatal("error opening \"%s\" for writing", output);
	}

	ftr->output = grw_strdup(output);

	/* Call formatter routine for format-specific processing */
	ftr->begindocument(ftr, output, title);

	/* Update state */
	ftr->state |= FSTATE_DOC;
}


/*
 * grw_enddocument()
 *
 * End the current document.
 *
 *	ftr		Formatter.
 */
void
grw_enddocument(grw_formatter_t *ftr)
{
	/* Check state */
	checkstate(ftr, FSTATE_DOC);

	/* Call formatter routine for format-specific processing */
	ftr->enddocument(ftr);

	/* Check errors on stream */
	if (ferror(ftr->fp))
		grw_fatal("error writing to \"%s\"", ftr->output);

	/* Flush the stream if its standard output; close if it's to a file */
	if (ftr->fp == stdout)
	{
		fflush(ftr->fp);
	}
	else
	{
		if (fclose(ftr->fp) != 0)
			grw_fatal("error writing to \"%s\"", ftr->output);
	}

	/* Clear data */
	ftr->fp = NULL;
	free(ftr->output);
	ftr->output = NULL;

	/* Update state */
	ftr->state &= ~FSTATE_DOC;
}


/*
 * grw_printh1()
 *
 * Print a level 1 heading to the current document.
 *
 *	ftr		Formatter.
 *	fmt		Format of output, cf. printf.
 */
void
grw_printh1(grw_formatter_t *ftr, char *fmt, ...)
{
	va_list ap;
	char buf[MAXLINELEN];

	/* Check state */
	checkstate(ftr, FSTATE_DOC);

	/* Print heading from variable argument list */
	va_start(ap, fmt);
	grw_vsnprintf(buf, sizeof(buf), fmt, ap);
	va_end(ap);

	/* Call formatter routine to actually print the heading */
	ftr->printh1(ftr, buf);
}


/*
 * grw_printh2()
 *
 * Print a level 2 heading to the current document.
 *
 *	ftr		Formatter.
 *	fmt		Format of output, cf. printf.
 */
void
grw_printh2(grw_formatter_t *ftr, char *fmt, ...)
{
	va_list ap;
	char buf[MAXLINELEN];

	/* Check state */
	checkstate(ftr, FSTATE_DOC);

	/* Print heading from variable argument list */
	va_start(ap, fmt);
	grw_vsnprintf(buf, sizeof(buf), fmt, ap);
	va_end(ap);

	/* Call formatter routine to actually print the heading */
	ftr->printh2(ftr, buf);
}


/*
 * grw_printh3()
 *
 * Print a level 3 heading to the current document.
 *
 *	ftr		Formatter.
 *	fmt		Format of output, cf. printf.
 */
void
grw_printh3(grw_formatter_t *ftr, char *fmt, ...)
{
	va_list ap;
	char buf[MAXLINELEN];

	/* Check state */
	checkstate(ftr, FSTATE_DOC);

	/* Print heading from variable argument list */
	va_start(ap, fmt);
	grw_vsnprintf(buf, sizeof(buf), fmt, ap);
	va_end(ap);

	/* Call formatter routine to actually print the heading */
	ftr->printh3(ftr, buf);
}


/*
 * grw_print()
 *
 * Print some text to the current document.
 *
 *	ftr		Formatter.
 *	options		Options. Only font options apply (GRW_FONT.. bits).
 *			All others are ignored.
 *	fmt		Format of output, cf. printf.
 */
void
grw_print(grw_formatter_t *ftr, unsigned long options, char *fmt, ...)
{
	va_list ap;
	char buf[MAXLINELEN];

	/* Check state */
	checkstate(ftr, FSTATE_DOC);

	/* Print text from variable argument list */
	va_start(ap, fmt);
	grw_vsnprintf(buf, sizeof(buf), fmt, ap);
	va_end(ap);

	/* Call formatter routine to actually print the text */
	ftr->print(ftr, options, buf);
}


/*
 * grw_starttable()
 *
 * Start a table.
 *
 *	ftr		Formatter.
 *	options		Options. Only border (GRW_BORDER_..) and horizontal
 *			alignment (GRW_HALIGN_..) options apply. All others
 *			are ignored.
 */
void
grw_starttable(grw_formatter_t *ftr, unsigned long options)
{
	/* Check state */
	checkstate(ftr, FSTATE_DOC);

	/* Call formatter routine for format-specific processing */
	ftr->starttable(ftr, options);

	/* Update state */
	ftr->state |= FSTATE_TABLE;
}


/*
 * grw_endtable()
 *
 * End a table.
 *
 *	ftr		Formatter.
 */
void
grw_endtable(grw_formatter_t *ftr)
{
	/* Check state */
	checkstate(ftr, FSTATE_DOC|FSTATE_TABLE);

	/* Call formatter routine for format-specific processing */
	ftr->endtable(ftr);

	/* Update state */
	ftr->state &= ~FSTATE_TABLE;
}

	
/*
 * grw_startrow()
 *
 * Start a table row.
 *
 *	ftr		Formatter.
 */
void
grw_startrow(grw_formatter_t *ftr)
{
	/* Check state */
	checkstate(ftr, FSTATE_DOC|FSTATE_TABLE);

	/* Call formatter routine for format-specific processing */
	ftr->startrow(ftr);

	/* Update state */
	ftr->state |= FSTATE_TR;
}


/*
 * grw_endrow()
 *
 * End a table row.
 *
 *	ftr		Formatter.
 */
void
grw_endrow(grw_formatter_t *ftr)
{
	/* Check state */
	checkstate(ftr, FSTATE_DOC|FSTATE_TABLE|FSTATE_TR);

	/* Call formatter routine for format-specific processing */
	ftr->endrow(ftr);

	/* Update state */
	ftr->state &= ~FSTATE_TR;
}


/*
 * grw_printcell()
 *
 * Print a table cell in one go.
 *
 *	ftr		Formatter.
 *	options		Options. Only font (GRW_FONT_..) and horizontal
 *			alignment (GRW_HALIGN_..) options apply. All others
 *			are ignored.
 *	class		Class attribute - for use by style sheets. May be NULL.
 *	fmt		Format of cell content, cf. printf.
 */
void
grw_printcell(grw_formatter_t *ftr, unsigned long options, char *class,
	char *fmt, ...)
{
	va_list ap;
	char buf[MAXLINELEN];

	/* (State checking is performed by startcell()) */

	/* Print the start of the table cell */
	grw_startcell(ftr, options, class);

	/* Print the cell contents */
	va_start(ap, fmt);
	grw_vsnprintf(buf, sizeof(buf), fmt, ap);
	va_end(ap);

	ftr->print(ftr, options, buf);

	/* Print the end of the table cell */
	grw_endcell(ftr);
}


/*
 * grw_startcell()
 *
 * Start a table cell.
 *
 *	ftr		Formatter.
 *	options		Options. Only font (GRW_FONT_..) and horizontal
 *			alignment (GRW_HALIGN_..) options apply. All others
 *			are ignored.
 *	class		Class attribute - for use by style sheets. May be NULL.
 */
void
grw_startcell(grw_formatter_t *ftr, unsigned long options, char *class)
{
	/* Check state */
	checkstate(ftr, FSTATE_DOC|FSTATE_TABLE|FSTATE_TR);

	/* Call formatter routine for format-specific processing */
	ftr->startcell(ftr, options, class);

	/* Update state */
	ftr->state |= FSTATE_TD;
}


/*
 * grw_endcell()
 *
 * End a table cell.
 *
 *	ftr		Formatter.
 */
void
grw_endcell(grw_formatter_t *ftr)
{
	/* Check state */
	checkstate(ftr, FSTATE_DOC|FSTATE_TABLE|FSTATE_TR|FSTATE_TD);

	/* Call formatter routine for format-specific processing */
	ftr->endcell(ftr);

	/* Update state */
	ftr->state &= ~FSTATE_TD;
}


/*
 * grw_startulist()
 *
 * Start an unordered list.
 *
 *	ftr		Formatter.
 */
void
grw_startulist(grw_formatter_t *ftr)
{
	/* Check state */
	checkstate(ftr, FSTATE_DOC);

	/* Call formatter routine for format-specific processing */
	ftr->startulist(ftr);

	/* Update state */
	ftr->state |= FSTATE_UL;
}


/*
 * grw_endulist()
 *
 * End an unordered list.
 *
 *	ftr		Formatter.
 */
void
grw_endulist(grw_formatter_t *ftr)
{
	/* Check state */
	checkstate(ftr, FSTATE_DOC|FSTATE_UL);

	/* Call formatter routine for format-specific processing */
	ftr->endulist(ftr);

	/* Update state */
	ftr->state &= ~FSTATE_UL;
}


/*
 * grw_printlistentry()
 *
 * Print a list entry.
 *
 *	ftr		Formatter.
 *	options		Options. Only font options apply (GRW_FONT.. bits).
 *			All others are ignored. All others are ignored.
 *	fmt		Format of list entry, cf. printf.
 */
void
grw_printlistentry(grw_formatter_t *ftr, unsigned long options, char *fmt, ...)
{
	va_list ap;
	char buf[MAXLINELEN];

	/* Check state */
	checkstate(ftr, FSTATE_DOC|FSTATE_UL);

	/* Print list entry */
	va_start(ap, fmt);
	grw_vsnprintf(buf, sizeof(buf), fmt, ap);
	va_end(ap);

	/* Call formatter routine to actually print the text of the entry */
	ftr->printlistentry(ftr, options, buf);
}


/*
 * grw_breakline()
 *
 * Introduce a line break.
 *
 *	ftr		Formatter.
 */
void
grw_breakline(grw_formatter_t *ftr)
{
	/* Check state */
	checkstate(ftr, FSTATE_DOC);

	/* Call formatter routine for format-specific processing */
	ftr->breakline(ftr);
}


/*
 * grw_printhline()
 *
 * Print a horizontal line.
 *
 *	ftr		Formatter.
 */
void
grw_printhline(grw_formatter_t *ftr)
{
	/* Check state */
	checkstate(ftr, FSTATE_DOC);

	/* Call formatter routine for format-specific processing */
	ftr->printhline(ftr);
}


/*
 * checkstate()
 *
 * Check state formatter. If it is not in the correct state to perform the
 * required operation, then it exits using grw_fatal().
 *
 *	ftr		Formatter.
 *	reqstate	Required state of processing.
 */
static void
checkstate(grw_formatter_t *ftr, unsigned int reqstate)
{
	/* If formatter data is NULL, something is seriously wrong */
	if (ftr == NULL)
	{
		grw_fatal("internal error: formatter function called with NULL formatter");
	}

	/* Check we're in the required state */
	if ((reqstate & ftr->state) != reqstate)
	{
		grw_fatal("internal error: formatter in illegal state (0x%x required 0x%x)",
			ftr->state, reqstate);
	}

	/* If we're inside a document, check that the file stream is ok */
	if (reqstate & FSTATE_DOC)
	{
		if (ftr->fp == NULL)
			grw_fatal("internal error: formatter function called without corresponding begindocument() call ");

		if (ferror(ftr->fp))
			grw_fatal("error writing to \"%s\"", ftr->output);
	}
}


/*
 * grw_vsnprintf()
 *
 * Print formatted output to a string, limiting the number of characters
 * written. (Local version of vsnprintf() function which appears on many
 * systems now, and I believe is in latest ANSI C).
 * The code is adapted from apilib/dresfile.c.
 *
 *	s		Destination buffer for formatted output.
 *	maxsize		Maximum number of bytes to write, including
 *			nul-terminator.
 *	format		Format string, cf. printf().
 *	ap		Argument list.
 */
static void
grw_vsnprintf(char *s, size_t maxsize, char *format, va_list ap)
{
#ifdef _WIN32	/* -START-WIN32-CUT- */
	static char devnull[] = "nul";
#else		/* -END-WIN32-CUT- */
	static char devnull[] = "/dev/null";
#endif		/* -WIN32-CUT-LINE- */
	char bigbuf[10 * TET_JNL_LEN];
	char *bp;
	int len;
	int actuallen;
	FILE *fp;

	/* Use largest of caller-supplied and internal buffers */
	if (maxsize > sizeof(bigbuf))
	{
		bp = s;
		len = (int)maxsize;
	}
	else
	{
		bp = bigbuf;
		len = (int)sizeof(bigbuf);
	}

	/* First find out how big a buffer we need for the formatted output */
	fp = fopen(devnull, "w");
	if (fp != NULL)
	{
		actuallen = vfprintf(fp, format, ap) + 1;
		fclose(fp);

		if (actuallen >= len)
		{
			grw_fatal("buffer would overflow in grw_vsnprintf()");
		}
	}

	/* Use vsprintf() to do the formatting */
	if (vsprintf(bp, format, ap) >= len)
	{
		/* This could happen if the fopen of the null device failed */
		/* No point trying to continue with corrupted memory */
		grw_fatal("vsprintf() overflowed buffer in grw_vsnprintf()");
	}

	/* If we used internal buffer, copy into caller-supplied buffer */
	if (bp == bigbuf)
		sprintf(s, "%.*s", (int)maxsize, bigbuf);
}
