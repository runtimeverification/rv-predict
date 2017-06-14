/*
 *	SCCS: @(#)text.c	1.2 (02/11/06)
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
static char sccsid[] = "@(#)text.c	1.2 (02/11/06) TETware release 3.8";
#endif

/************************************************************************

SCCS:		@(#)text.c	1.2 02/11/06 TETware release 3.8
NAME:		text.c
PRODUCT:	TETware Generic Report Writer.
AUTHOR:		Matthew Hails, UniSoft Ltd.
DATE CREATED:	19 July 2000
SYNOPSIS:

	grw_formatter_t * grw_createtextformatter(void)

DESCRIPTION:

	Text Formatter. This provides the code to output a document in
	plain text. Note that layout calculations are done in characters,
	i.e. it assumes a fixed width font.

************************************************************************/


#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdarg.h>
#include <ctype.h>
#include "grw.h"
#include "formatter.h"


/* Default pagewidth, in characters/bytes. */
#define DEFAULT_PAGEWIDTH	79

/* Minimum pagewidth, in characters/bytes. */
#define MIN_PAGEWIDTH		60

/* Maximum length of one line within a table cell */
#define MAXLINELEN		256

/* Maximum number of cells within one row of a table */
#define MAXTABLECELLS		10

/* Allocation increment for string vectors */
#define ALLOC_INCREMENT		20

/* Minimum column width, in bytes */
#define MINCOLWIDTH		5


/*
 * Structure representing one cell of a table.
 */
struct cell
{
	/* Cell options. Horizontal alignment (GRW_HALIGN_..) are the only
	 * one supported by the text formatter.
	 */
	unsigned long options;

	/* Lines of text within the cell */
	grw_svector_t *lines;

	/* Number of lines in `lines' vector. Not initially filled in, but set
	 * by a call to updatecell(). If `lines' is updated, this needs to be
	 * updated using updatecell().
	 */
	int lc;

	/* Pointer to lines in `lines' vector. Not initially filled in, but set
	 * by a call to updatecell(). If `lines' is updated, this needs to be
	 * updated using updatecell().
	 */
	char **la;
};

/*
 * Structure representing one row of a table.
 */
struct row
{
	/* Number of cells in this row */
	int ncells;

	/* Contents of cells in this row; each cell can have multiple lines */
	struct cell cells[MAXTABLECELLS];

	/* Next row structure in list */
	struct row *next;
};

/*
 * Structure holding details of a table in the document being generated.
 */
struct table
{
	/* Width of table border */
	int border;

	/* First entry in list of table rows */
	struct row *rowhead;

	/* Last entry in list of table rows */
	struct row *rowtail;

	/* Maximum number of cells in each row */
	int maxcells;

	/* Maximum length of each column */
	int maxwidths[MAXTABLECELLS];

	/* Current line in a table cell */
	char cellbuf[MAXLINELEN];

	/* Current position in `cellbuf' */
	int bufpos;
};

/*
 * Private data used by text formatters.
 */
struct textdata
{
	/* State of output */
	unsigned int state;

	/* Width of page */
	int pagewidth;

	/* Current column position, measured from 0 */
	int col;

	/* Current indentation */
	int indent;

	/* Boolean value: non-zero if inside a table cell, zero otherwise */
	int incell;

	/* Current table */
	struct table tbl;
};


/* Local prototypes */
static void begindocument(grw_formatter_t *ftr, char *output, char *title);
static void enddocument(grw_formatter_t *ftr);
static void printh1(grw_formatter_t *ftr, char *heading);
static void printh2(grw_formatter_t *ftr, char *heading);
static void printh3(grw_formatter_t *ftr, char *heading);
static void print(grw_formatter_t *ftr, unsigned long options, char *text);
static void starttable(grw_formatter_t *ftr, unsigned long options);
static void endtable(grw_formatter_t *ftr);
static void fittable(struct table *tbl, int pagewidth);
static void updatecell(struct cell *cell);
static void startrow(grw_formatter_t *ftr);
static void endrow(grw_formatter_t *ftr);
static void startcell(grw_formatter_t *ftr, unsigned long options,
	char *class);
static void endcell(grw_formatter_t *ftr);
static void startulist(grw_formatter_t *ftr);
static void endulist(grw_formatter_t *ftr);
static void printlistentry(grw_formatter_t *ftr, unsigned long options,
	char *text);
static void breakline(grw_formatter_t *ftr);
static void printhline(grw_formatter_t *ftr);
static void printhl(grw_formatter_t *ftr, int linechar, int len, int endchar);
static void txputs(grw_formatter_t *ftr, char *s);
static void txputc(grw_formatter_t *ftr, int c);
static void finishcellline(struct table *tbl);


/*
 * Create a new text formatter.
 *
 * 	pagewidth	Pagewidth, in bytes, for generated document.
 *
 * Returns the new formatter structure.
 */
grw_formatter_t *
grw_createtextformatter(int pagewidth)
{
	struct textdata *txd;

	if (pagewidth < MIN_PAGEWIDTH && pagewidth != 0)
	{
		grw_fatal("illegal pagewidth: %d must be >= %d", pagewidth,
			MIN_PAGEWIDTH);
	}

	txd = grw_malloc(sizeof(*txd));
	memset(txd, 0, sizeof(*txd));
	txd->state = 0U;
	txd->pagewidth = (pagewidth == 0) ? DEFAULT_PAGEWIDTH : pagewidth;
	txd->col = 0;
	txd->indent = 0;
	txd->incell = 0;
	txd->tbl.border = 0;
	txd->tbl.rowhead = NULL;
	txd->tbl.rowtail = NULL;
	txd->tbl.maxcells = 0;

	return grw_createformatter(begindocument, enddocument, printh1,
		printh2, printh3, print, starttable, endtable, startrow,
		endrow, startcell, endcell, startulist, endulist,
		printlistentry, breakline, printhline, txd);
}


/*
 * begindocument()
 *
 * Begin a new document.
 *
 *	ftr		Formatter.
 *	output		Path of output file. "-" specifies standard output.
 *	title		Ignored. This is the "non-visible" title of the
 *			document, which has no equivalent in plain text.
 */
static void
begindocument(grw_formatter_t *ftr, char *output, char *title)
{
	/* Nothing to do */
}


/*
 * enddocument()
 *
 * End the current document.
 *
 *	ftr		Formatter.
 */
static void
enddocument(grw_formatter_t *ftr)
{
	/* Nothing to do */
}


/*
 * printh1()
 *
 * Print a level 1 heading to the current document.
 *
 *	ftr		Formatter.
 *	heading		The heading.
 */
static void
printh1(grw_formatter_t *ftr, char *heading)
{
	struct textdata *txd = ftr->pdata;
	int len;
	int origindent;

	len = (int)strlen(heading) + 4;
	origindent = txd->indent;

	if (len < txd->pagewidth)
		txd->indent += (txd->pagewidth - len) / 2;

	printhl(ftr, '-', len, '+');
	printhl(ftr, ' ', len, '|');
	txputs(ftr, "| ");
	txputs(ftr, heading);
	txputs(ftr, " |\n");
	printhl(ftr, ' ', len, '|');
	printhl(ftr, '-', len, '+');

	txd->indent = origindent;
}


/*
 * printh2()
 *
 * Print a level 2 heading to the current document.
 *
 *	ftr		Formatter.
 *	heading		The heading.
 */
static void
printh2(grw_formatter_t *ftr, char *heading)
{
	txputc(ftr, '\n');
	txputs(ftr, heading);
	txputc(ftr, '\n');
	printhl(ftr, '=', (int)strlen(heading), '=');
}


/*
 * printh3()
 *
 * Print a level 3 heading to the current document.
 *
 *	ftr		Formatter.
 *	heading		The heading.
 */
static void
printh3(grw_formatter_t *ftr, char *heading)
{
	txputc(ftr, '\n');
	txputs(ftr, heading);
	txputc(ftr, '\n');
	printhl(ftr, '-', (int)strlen(heading), '-');
}


/*
 * print()
 *
 * Print some text to the current document.
 *
 *	ftr		Formatter.
 *	options		Options. Only font options apply (GRW_FONT.. bits).
 *			All others are ignored.
 *	text		Text to be printed.
 */
static void
print(grw_formatter_t *ftr, unsigned long options, char *text)
{
	txputs(ftr, text);
}


/*
 * starttable()
 *
 * Start a table.
 *
 *	ftr		Formatter.
 *	options		Options. Only border (GRW_BORDER_..) options apply. All
 *			others are ignored.
 */
static void
starttable(grw_formatter_t *ftr, unsigned long options)
{
	struct textdata *txd = ftr->pdata;
	struct table *tbl;
	struct row *row;
	struct row *nextrow;
	struct cell *cp;

	tbl = &txd->tbl;

	/* Determine border */
	switch (options & GRW_BORDER_MASK)
	{
	case GRW_BORDER_NONE:
	default:
		tbl->border = 0;
		break;
	case GRW_BORDER_NARROW:
		tbl->border = 1;
		break;
	}

	/* Initialize rows */
	for (row = tbl->rowhead; row != NULL; row = nextrow)
	{
		/* Free table cells */
		for (cp = row->cells; cp < row->cells + row->ncells; cp++)
		{
			if (cp->lines != NULL)
				grw_freesvector(cp->lines);
		}

		/* Free this row structure */
		nextrow = row->next;
		free(row);
	}

	tbl->rowhead = NULL;
	tbl->rowtail = NULL;
	tbl->maxcells = 0;
	memset(tbl->maxwidths, 0, sizeof(tbl->maxwidths));
	memset(tbl->cellbuf, 0, sizeof(tbl->cellbuf));
	tbl->bufpos = 0;
}


/*
 * endtable()
 *
 * End a table. This does the actual drawing of the table, using data stored
 * earlier by the row/cell routines.
 *
 *	ftr		Formatter.
 */
static void
endtable(grw_formatter_t *ftr)
{
	struct textdata *txd = ftr->pdata;
	struct table *tbl;
	struct cell *cell;
	struct row *row;
	int pos;
	char hborder[(MAXTABLECELLS * (MAXLINELEN + 1)) + 1];
	int nlines;
	int n;
	char *text;
	int len;
	int i;

	tbl = &txd->tbl;

	/* Make sure the state flag for being in a table cell is off
	 * - otherwise output will be disrupted from here on.
	 */
	txd->incell = 0;

	/* Ensure the data in the table cells is complete and consistent */
	for (row = tbl->rowhead; row != NULL; row = row->next)
	{
		for (cell = row->cells; cell < row->cells + row->ncells; cell++)
			updatecell(cell);

		/* Ensure any unused cells in this row have a zero line
		 * count.
		 */
		for ( ; cell < row->cells + tbl->maxcells; cell++)
			cell->lc = 0;
	}

	/* If the current table width is greater than the pagewidth, attempt to
	 * redo the table cells to get it below the pagewidth.
	 */
	fittable(tbl, txd->pagewidth);
	fittable(tbl, txd->pagewidth); /* XXX */

	/* Create horizontal border for table */
	pos = 0;
	hborder[pos++] = '+';

	for (i = 0; i < tbl->maxcells; i++)
	{
		memset(hborder + pos, '-', tbl->maxwidths[i] + 2);
		pos += tbl->maxwidths[i] + 2;
		hborder[pos++] = '+';
	}

	hborder[pos++] = '\n';
	hborder[pos] = '\0';

	/* Make sure we have some space before the table */
	txputc(ftr, '\n');

	for (row = tbl->rowhead; row != NULL; row = row->next)
	{
		/* Determine the maximum number of lines in any cell */
		nlines = 0;

		for (cell = row->cells; cell < row->cells + row->ncells; cell++)
		{
			/* Update line count if this cell exceeds it */
			if (cell->lc > nlines)
				nlines = cell->lc;
		}

		/* This shouldn't happen, but just in case, ensure we have
		 * at least one line.
		 */
		if (nlines < 1)
			nlines = 1;

		/* If this table has a border, print horizontal line preceding
		 * this row.
		 */
		if (tbl->border > 0)
			txputs(ftr, hborder);

		/* For each line in this row ... */
		for (n = 0; n < nlines; n++)
		{
			/* If this table has a border, print its left hand
			 * edge.
			 */
			if (tbl->border > 0)
				txputs(ftr, "|");

			/* For this line of each cell in this row ... */
			for (cell = row->cells, i = 0;
				cell < row->cells + tbl->maxcells; cell++, i++)
			{
				/* Print the line */
				text = (cell->lc > n) ? cell->la[n] : "";
				len = (int)strlen(text);

				/* Print the cell text, depending on horizontal
				 * alignment. All alignments have a leading
				 * space.
				 */
				txputc(ftr, ' ');

				switch (cell->options & GRW_HALIGN_MASK)
				{
				case GRW_HALIGN_RIGHT:
					/* Align the text to the right of the
					 * of the cell. Pad with spaces then
					 * print text.
					 */
					for (pos = 0;
						pos < tbl->maxwidths[i] - len;
						pos++)
					{
						txputc(ftr, ' ');
					}

					txputs(ftr, text);
					break;

				case GRW_HALIGN_CENTER:
					/* Align the text to the centre of the
					 * of the cell as closely as possible.
					 * Of course, it may not be possible
					 * to get it exactly in the middle, so
					 * start it at the character to the
					 * left of the ideal in that case.
					 */
					for (pos = 0;
						pos <
						(tbl->maxwidths[i] - len) / 2;
						pos++)
					{
						txputc(ftr, ' ');
					}

					txputs(ftr, text);

					for (pos += len;
						pos < tbl->maxwidths[i]; pos++)
					{
						txputc(ftr, ' ');
					}

					break;

 				case GRW_HALIGN_LEFT:
				default:
					/* Align the text to the left hand side
					 * of the cell. Print the text, then
					 * pad to width of cell using spaces.
					 */
					txputs(ftr, text);

					for ( ; len < tbl->maxwidths[i]; len++)
						txputc(ftr, ' ');

					break;
				}

				/* If this table has a border, print the cell
				 * divider at the right hand side of this cell.
				 */
				if (tbl->border > 0)
					txputs(ftr, " |");
			}

			txputc(ftr, '\n');
		}
	}

	if (tbl->border > 0)
		txputs(ftr, hborder);

	txputc(ftr, '\n');
}


/*
 * fittable()
 *
 * Try to ensure that a table fits within a given pagewidth.
 *
 *	tbl		Table data.
 *	pagewidth	Page width, in bytes.
 */
static void
fittable(struct table *tbl, int pagewidth)
{
	int width;
	int i;
	int widestcol;
	int colwidth;
	struct row *row;
	struct cell *cell;
	int idx;
	char *line;
	int modified;
	int pos;
	char buf1[MAXLINELEN];
	char buf2[MAXLINELEN];

	/* Calculate current table width */
	width = 1;

	for (i = 0; i < tbl->maxcells; i++)
	{
		/* Each cell: " " content " |" */
		width += 1 + tbl->maxwidths[i] + 2;
	}

	/* If the table is currently narrow enough to fit within the pagewidth,
	 * then there is nothing else to do, so return.
	 */
	if (width <= pagewidth)
		return;

	/* Find the widest column */
	widestcol = 0;

	for (i = 0; i < tbl->maxcells; i++)
	{
		if (tbl->maxwidths[i] >= tbl->maxwidths[widestcol])
			widestcol = i;
	}

	/* If the columns are all very narrow, then don't do anything */
	if (tbl->maxwidths[widestcol] <= MINCOLWIDTH)
		return;

	/* Calculate new width for widest column */
	colwidth = pagewidth - (width - tbl->maxwidths[widestcol]);
	if (colwidth < MINCOLWIDTH)
	{
		/* It's not going to fit, but wrap to something close */
		colwidth = MINCOLWIDTH;
	}

	/* Search through the target column in each row, attempting to reduce
	 * its width to less than the new width.
	 */
	for (row = tbl->rowhead; row != NULL; row = row->next)
	{
		if (row->ncells <= widestcol)
			continue;

		cell = &row->cells[widestcol];

		/* For each line in the current cell, try to break it on
		 * whitespace. Otherwise, break it at the end of the column.
		 */
		for (idx = 0; idx < cell->lc; idx++)
		{
			line = cell->la[idx];
			modified = 0;

			while ((int)strlen(line) > colwidth)
			{
				/* Find last whitespace character on line */
				for (pos = colwidth; pos > 0; pos--)
				{
					if (isspace((unsigned char)line[pos]))
						break;
				}

				/* If there is no whitespace character before
				 * the required width, then break in the middle
				 * of non-whitespace.
				 */
				if (pos == 0)
					pos = colwidth;

				/* Copy line so we can modify it */
				sprintf(buf1, "%.*s", pos, line);

				/* Shorten line to required width and remove
				 * any trailing whitespace.
				 */
				while (pos > 0
					&& isspace((unsigned char)buf1[pos]))
				{
					buf1[pos--] = '\0';
				}

				/* Save new line. N.B. needs to be done before
				 * entry is set/inserted, in case existing
				 * entry in vector is freed.
				 */
				sprintf(buf2, "%s", line + pos);
				line = buf2;

				/* Replace or insert this line into the
				 * vector.
				 */
				if (modified)
				{
					grw_insertentry(cell->lines, ++idx,
						buf1);
				}
				else
				{
					grw_setentry(cell->lines, idx, buf1);
				}

				modified = 1;

				/* Move line over leading whitespace */
				while (isspace((unsigned char)*line))
					line++;
			}

			/* If the line vector of this cell has changed in some
			 * way, then we need to add the remainder of the line,
			 * and then re-retrieve the lines, since they may have
			 * moved in memory.
			 */
			if (modified)
			{
				grw_insertentry(cell->lines, ++idx, line);
				updatecell(cell);
			}
		}
	}

	/* Update width of the widest column */
	tbl->maxwidths[widestcol] = colwidth;
}


/*
 * updatecell()
 *
 * Updates a cell structure to make it internally consistent.
 *
 *	cell		Cell structure to update.
 */
static void
updatecell(struct cell *cell)
{
	/* Get lines of text for this cell */
	cell->lc = grw_getsize(cell->lines);
	cell->la = grw_getentries(cell->lines);

	/* If the cell ends with a blank line, decrease line count to
	 * effectively remove the line.
	 */
	if (cell->lc > 0 && cell->la[cell->lc - 1][0] == '\0')
		cell->lc--;
}


/*
 * startrow()
 *
 * Start a table row.
 *
 *	ftr		Formatter.
 */
static void
startrow(grw_formatter_t *ftr)
{
	struct textdata *txd = ftr->pdata;
	struct row *row;

	/* Create a new entry for this row */
	row = grw_malloc(sizeof(*row));
	memset(row, 0, sizeof(*row));
	row->ncells = 0;
	row->next = NULL;

	/* Add row to table */
	if (txd->tbl.rowhead == NULL)
	{
		txd->tbl.rowhead = row;
		txd->tbl.rowtail = txd->tbl.rowhead;
	}
	else
	{
		txd->tbl.rowtail->next = row;
		txd->tbl.rowtail = txd->tbl.rowtail->next;
	}
}


/*
 * endrow()
 *
 * End a table row.
 *
 *	ftr		Formatter.
 */
static void
endrow(grw_formatter_t *ftr)
{
	/* Nothing to do */
}


/*
 * startcell()
 *
 * Start a table cell.
 *
 *	ftr		Formatter.
 *	options		Options. Only horizontal alignment (GRW_HALIGN_..)
 *			options apply. All others are ignored.
 *	class		Class attribute. Ignored since this is not applicable
 *			to plain text.
 */
static void
startcell(grw_formatter_t *ftr, unsigned long options, char *class)
{
	struct textdata *txd = ftr->pdata;
	struct row *row;
	struct cell *cell;

	/* Set state flag for being in a cell, and reset buffer position */
	txd->incell = 1;
	txd->tbl.bufpos = 0;

	/* Check we haven't got too many cells */
	row = txd->tbl.rowtail;
	if (row->ncells++ > GRW_NELEM(row->cells))
	{
		grw_fatal("internal error: exceeded maximum number of table cells (%d)",
			GRW_NELEM(row->cells));
	}

	/* Initialize new cell */
	cell = &row->cells[row->ncells - 1];
	cell->lines = grw_createsvector(ALLOC_INCREMENT);
	cell->options = options;

	/* Update maximum number of cells per row if necessary */
	if (row->ncells >= txd->tbl.maxcells)
		txd->tbl.maxcells = row->ncells;
}


/*
 * endcell()
 *
 * End a table cell.
 *
 *	ftr		Formatter.
 */
static void
endcell(grw_formatter_t *ftr)
{
	struct textdata *txd = ftr->pdata;

	/* Add the last line to the vector of lines for this cell */
	if (txd->tbl.bufpos > 0)
		finishcellline(&txd->tbl);

	/* Clear state flag for being in a cell */
	txd->incell = 0;
}


/*
 * startulist()
 *
 * Start an unordered list.
 *
 *	ftr		Formatter.
 */
static void
startulist(grw_formatter_t *ftr)
{
	txputc(ftr, '\n');
}


/*
 * endulist()
 *
 * End an unordered list.
 *
 *	ftr		Formatter.
 */
static void
endulist(grw_formatter_t *ftr)
{
	txputc(ftr, '\n');
}


/*
 * printlistentry()
 *
 * Print a list entry.
 *
 *	pdata		Formatter private data.
 *	options		Options. Only font options apply (GRW_FONT.. bits).
 *			All others are ignored. All others are ignored.
 *	text		Text of list entry.
 */
static void
printlistentry(grw_formatter_t *ftr, unsigned long options, char *text)
{
	txputs(ftr, " + ");
	txputs(ftr, text);
	txputc(ftr, '\n');
}


/*
 * breakline()
 *
 * Introduce a line break.
 *
 *	ftr		Formatter.
 */
static void
breakline(grw_formatter_t *ftr)
{
	txputc(ftr, '\n');
}


/*
 * printhline()
 *
 * Print a horizontal line.
 *
 *	ftr		Formatter.
 */
static void
printhline(grw_formatter_t *ftr)
{
	struct textdata *txd = ftr->pdata;

	printhl(ftr, '-', txd->pagewidth, '-');
}


/*
 * printhl()
 *
 * Print a horizontal line mainly consisting of one character.
 *
 *	ftr		Formatter.
 *	linechar	Character to use for most of the line.
 *	len		Length of line.
 *	endchar		Character to use for the first and last characters of
 *			the line.
 */
static void
printhl(grw_formatter_t *ftr, int linechar, int len, int endchar)
{
	if (len > 0)
		txputc(ftr, endchar);

	while (len-- > 2)
		txputc(ftr, linechar);

	if (len > 0)
		txputc(ftr, endchar);

	txputc(ftr, '\n');
}


/*
 * txputs()
 *
 * Print a string to the output.
 *
 *	ftr		Formatter.
 *	s		String to print to the output.
 */
static void
txputs(grw_formatter_t *ftr, char *s)
{
	for ( ; *s != 0; s++)
		txputc(ftr, *s);
}


/*
 * txputc()
 *
 * Print a single character to the output.
 *
 *	ftr		Formatter.
 *	c		Character to print to the output.
 */
static void
txputc(grw_formatter_t *ftr, int c)
{
	struct textdata *txd = ftr->pdata;

	/* Change tabs to spaces - otherwise XXX */
	if (c == '\t')
		c = ' ';

	if (txd->incell)
	{
		/* We're in a table cell, so add to the buffer holding the
		 * contents of the current cell. The contents is truncated if
		 * it would overflow internal buffer.
		 */
		if (txd->tbl.bufpos + 1 < (int)sizeof(txd->tbl.cellbuf))
		{
			if (c == '\n')
			{
				finishcellline(&txd->tbl);
			}
			else
			{
				txd->tbl.cellbuf[txd->tbl.bufpos++] = c;
			}
		}
	}
	else
	{
		/* We're not in a table cell, so simply print out the
		 * character.
		 */

		/* Add any necessary indentation at the start of the line */
		for ( ; txd->col < txd->indent; txd->col++)
			fputc(' ', ftr->fp);

		/* Print the character */
		fputc(c, ftr->fp);

		/* Update the column position */
		if (c == '\n')
			txd->col = 0;
		else
			txd->col++;
	}
}


/*
 * finishcellline()
 *
 * Finish the line of the current table cell.
 *
 *	tbl		Table being created.
 */
static void
finishcellline(struct table *tbl)
{
	struct row *row;

	/* Null terminate the current cell line */	
	tbl->cellbuf[tbl->bufpos] = '\0';

	/* If the line is empty and this is the first line in the cell, then
	 * ignore the line.
	 */
	row = tbl->rowtail;
	if (tbl->bufpos != 0
		|| grw_getsize(row->cells[row->ncells - 1].lines) > 0)
	{
		/* Add the current line to the vector of lines in the current
		 * cell.
		 */
		grw_addentry(row->cells[row->ncells - 1].lines, tbl->cellbuf);

		/* Update the maximum column length, if necessary */
		if (tbl->bufpos > tbl->maxwidths[row->ncells - 1])
			tbl->maxwidths[row->ncells - 1] = tbl->bufpos;
	}

	/* Reset buffer position */
	tbl->bufpos = 0;
}
