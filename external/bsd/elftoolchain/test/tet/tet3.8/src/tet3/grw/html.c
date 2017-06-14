/*
 *	SCCS: @(#)html.c	1.3 (03/04/01)
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
static char sccsid[] = "@(#)html.c	1.3 (03/04/01) TETware release 3.8";
#endif

/************************************************************************

SCCS:		@(#)html.c	1.3 03/04/01 TETware release 3.8
NAME:		html.c
PRODUCT:	TETware Generic Report Writer.
AUTHOR:		Matthew Hails, UniSoft Ltd.
DATE CREATED:	19 July 2000
SYNOPSIS:

	grw_formatter_t * grw_createhtmlformatter(int usecolor)

DESCRIPTION:

	HTML Formatter. This provides the code to output a document in HTML.

************************************************************************/


#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdarg.h>
#include <ctype.h>
#include "grw.h"
#include "tet_jrnl.h" 
#include "formatter.h"


/*
 * HTML colours. Designed so that they use the standard colour map.
 * N.B. if these are modified, and it is still the intention to use the
 * standard colour map, only use the values 00, 33, 66, 99, cc and ff for
 * each RGB component.
 *
 * Note that these are only the HTML colours, so will be overriden by the
 * style sheet colours if using a style sheet.
 */
#define COLOR_BG	"white"
#define COLOR_TITLE	"#ccccff"
#define COLOR_NEUTRAL	"#cccccc"
#define COLOR_PASS	"#33cc33"
#define COLOR_FAIL	"#ff5555"


/*
 * Private data used by HTML formatters.
 * (Note that the code which deals with table state does not currently support
 * nested tables. This applies to the `state', `hasborder' and `cellfpos'
 * fields).
 */
struct htmldata
{
	/* Non-zero indicates to include colour information; zero means don't
	 * include colour information.
	 */
	int color;

	/* URI of stylesheet. May be NULL. */
	char *stylesheet;

	/* State of HTML */
	unsigned int state;

	/* Does the current table have a border */
	int hasborder;

	/* Stream position at the start of the last table cell */
	long cellfpos;
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
static void printhtml(grw_formatter_t *ftr, unsigned long options,
	char *prefix, char *text, char *suffix);


/*
 * grw_createhtmlformatter()
 *
 * Create a new HTML formatter.
 *
 *	usecolor	If non-zero, the generated HTML will include colour
 *			information; otherwise, it will not.
 *	stylesheet	URI of (external) style sheet. If NULL, no link to a
 *			style sheet will be included in generated documents.
 *
 * Returns the new formatter structure.
 */
grw_formatter_t *
grw_createhtmlformatter(int usecolor, char *stylesheet)
{
	struct htmldata *htd;

	htd = grw_malloc(sizeof(*htd));
	memset(htd, 0, sizeof(*htd));
	htd->color = usecolor;
	htd->stylesheet = (stylesheet == NULL) ? NULL : grw_strdup(stylesheet);
	htd->state = 0U;
	htd->hasborder = 1;
	htd->cellfpos = 0L;

	return grw_createformatter(begindocument, enddocument, printh1,
		printh2, printh3, print, starttable, endtable, startrow,
		endrow, startcell, endcell, startulist, endulist,
		printlistentry, breakline, printhline, htd);
}


/*
 * begindocument()
 *
 * Begin a new document.
 *
 *	ftr		Formatter.
 *	output		Path of output file. "-" specifies standard output.
 *	title		Non-visible title of the document used in <title> tag.
 */
static void
begindocument(grw_formatter_t *ftr, char *output, char *title)
{
	struct htmldata *htd = ftr->pdata;

	/* Print the beginning of the document */
	fputs("<!DOCTYPE HTML PUBLIC", ftr->fp);
	fputs(" \"-//W3C//DTD HTML 4.0 Transitional//EN\"\n", ftr->fp);
	fputs("	\"http://www.w3.org/TR/REC-html40/loose.dtd\">\n", ftr->fp);
	fputs("<html>\n<head>\n<title>", ftr->fp);
	printhtml(ftr, 0UL, "", title, "");
	fputs("</title>\n", ftr->fp);

	if (htd->stylesheet != NULL)
	{
		fprintf(ftr->fp,
			"<link href=\"%s\" rel=\"stylesheet\" type=\"text/css\">\n",
			htd->stylesheet);
	}

	fputs("</head>\n", ftr->fp);

	if (htd->color)
	{
		fprintf(ftr->fp, "<body bgcolor=\"%s\">\n", COLOR_BG);
	}
	else
	{
		fputs("<body>\n", ftr->fp);
	}
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
	/* Print the end of the document */
	fputs("</body>\n</html>\n", ftr->fp);
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
	struct htmldata *htd = ftr->pdata;

	if (htd->color)
	{
		fprintf(ftr->fp, "<p><table border=\"1\" width=\"100%%\"");
		fprintf(ftr->fp, " cellpadding=\"3\" cellspacing=\"0\">\n");
		fprintf(ftr->fp,
			"<tr><td bgcolor=\"%s\" class=\"%s\"><center><h1>\n",
			COLOR_TITLE, GRW_CLASS_H1);
		printhtml(ftr, 0UL, "", heading,
			"</h1></center></td></tr></table></p>\n");
	}
	else
	{

		printhtml(ftr, 0UL, "<center><h1>", heading,
			"</h1></center>\n");
	}
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
	struct htmldata *htd = ftr->pdata;

	if (htd->color)
	{
		fprintf(ftr->fp, "<p><table border=\"1\" cellpadding=\"3\"");
		fprintf(ftr->fp, " cellspacing=\"0\">\n");
		fprintf(ftr->fp, "<tr><td bgcolor=\"%s\" class=\"%s\">",
			COLOR_TITLE, GRW_CLASS_H2);
		printhtml(ftr, 0UL, "<font size=\"+2\"><b>\n", heading,
			"</b></font></td></tr></table></p>\n");
	}
	else
	{

		printhtml(ftr, 0UL, "<h2>", heading, "</h2>\n");
	}
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
	printhtml(ftr, 0UL, "<h3>", heading, "</h3>\n");
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
	printhtml(ftr, options, "", text, "");
}


/*
 * starttable()
 *
 * Start a table.
 *
 *	ftr		Formatter.
 *	options		Options. Only border (GRW_BORDER_..) options apply.
 *			All others are ignored.
 */
static void
starttable(grw_formatter_t *ftr, unsigned long options)
{
	struct htmldata *htd = ftr->pdata;
	int border;
	int padding;
	int spacing;

	/* Determine table border, cell padding and spacing.
	 * Non-colour is straightforward, as is colour for no border.
	 * Colour with a border is different: it actually has no border, but
	 * the padding and spacing are increased, giving a border where the
	 * cells are coloured.
	 */
	border = 0;
	padding = 2;
	spacing = 1;

	switch (options & GRW_BORDER_MASK)
	{
	case GRW_BORDER_NARROW:
		if (htd->color)
		{
			border = 0;
			padding = 5;
			spacing = 4;
		}
		else
		{
			border = 1;
		}

		htd->hasborder = 1;
		break;

	case GRW_BORDER_NONE:
	default:
		htd->hasborder = 0;
		break;
	}

	/* Print the start of the table. Also print a start paragraph tag
	 * - this helps space out the tables.
	 */
	fprintf(ftr->fp, "<p><table border=\"%d\" cellpadding=\"%d\"", border,
		padding);
	fprintf(ftr->fp, " cellspacing=\"%d\">\n", spacing);
}


/*
 * endtable()
 *
 * End a table.
 *
 *	ftr		Formatter.
 */
static void
endtable(grw_formatter_t *ftr)
{
	/* Print tag for end of table. Also end a paragraph - this helps space
	 * out the tables.
	 */
	fprintf(ftr->fp, "</table></p>\n");
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
	/* Print tag for start of table row */
	fprintf(ftr->fp, "<tr>\n");
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
	/* Print tag for end of table row */
	fprintf(ftr->fp, "</tr>\n");
}


/*
 * startcell()
 *
 * Start a table cell.
 *
 *	ftr		Formatter.
 *	options		Options. Only font (GRW_FONT_..) and horizontal
 *			alignment (GRW_HALIGN_..) options apply. All others
 *			are ignored.
 *	class		Class attribute - for use by style sheets. May be NULL.
 */
static void
startcell(grw_formatter_t *ftr, unsigned long options, char *class)
{
	struct htmldata *htd = ftr->pdata;
	char *cls;
	char *cp;

	/* Print the start of the tag */
	fprintf(ftr->fp, "<td");

	/* Add horizontal alignment if necessary */
	switch (options & GRW_HALIGN_MASK)
	{
	case GRW_HALIGN_LEFT:
		fprintf(ftr->fp, " align=\"left\"");
		break;
	case GRW_HALIGN_RIGHT:
		fprintf(ftr->fp, " align=\"right\"");
		break;
	case GRW_HALIGN_CENTER:
		fprintf(ftr->fp, " align=\"center\"");
		break;
	default:
		/* Don't print any alignment attribute */
		break;
	}

	/* Tidy up class name, if necessary */
	if (class == NULL)
	{
		cls = NULL;
	}
	else
	{
		/* Duplicate class name so can write over string */
		cls = grw_strdup(class);

		/* Skip leading whitespace */
		while (isspace((unsigned char)*cls))
			cls++;

		/* Truncate class on first character likely to cause problems
		 * (this isn't exactly the character set allowed in class
		 * attributes, but should work). We allow letters, digits and
		 * hyphens. Underscores are turned to hyphens.
		 */
		for (cp = cls; *cp != '\0'; cp++)
		{
			if (*cp == '_')
			{
				*cp = '-';
			}
			else if (!isalnum((unsigned char)*cp) && *cp != '-')
			{
				*cp = '\0';
				break;
			}
		}
	}

	/* Add colour and class attributes */
	if (htd->color)
	{
		if (cls == NULL)
		{
			if (htd->hasborder)
			{
				fprintf(ftr->fp,
					" bgcolor=\"%s\" class=\"%s\"",
					COLOR_NEUTRAL, GRW_CLASS_NEUTRAL);
			}
			else
			{
				fprintf(ftr->fp, " class=\"%s\"",
					GRW_CLASS_NOBORDER);
			}
		}
		else if (strcmp(cls, "PASS") == 0
			|| strcmp(cls, GRW_CLASS_SUCCESS) == 0)
		{
			fprintf(ftr->fp, " bgcolor=\"%s\" class=\"%s\"",
				COLOR_PASS, cls);
		}
		else if (strcmp(cls, GRW_CLASS_NEUTRAL) == 0)
		{
			fprintf(ftr->fp, " bgcolor=\"%s\" class=\"%s\"",
				COLOR_NEUTRAL, cls);
		}
		else
		{
			fprintf(ftr->fp, " bgcolor=\"%s\" class=\"%s\"",
				COLOR_FAIL, cls);
		}
	}
	else
	{
		if (cls == NULL)
		{
			if (htd->hasborder)
			{
				fprintf(ftr->fp, " class=\"%s\"",
					GRW_CLASS_NEUTRAL);
			}
			else
			{
				fprintf(ftr->fp, " class=\"%s\"",
					GRW_CLASS_NOBORDER);
			}
		}
		else
		{
			fprintf(ftr->fp, " class=\"%s\"", cls);
		}
	}

	/* Free memory used in class duplication */
	free(cls);

	/* Print end of tag */
	fprintf(ftr->fp, ">");

	/* Save stream position for use by endcell() */
	htd->cellfpos = ftell(ftr->fp);	
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
	struct htmldata *htd = ftr->pdata;
	long pos;

	/* If the cell would be empty, print a (non-breaking) space so that
	 * the cell isn't rendered funny by some browsers.
	 */
	pos = ftell(ftr->fp);
	if (pos != -1 && pos == htd->cellfpos)
		fputs("&nbsp;", ftr->fp);

	/* Print end of cell tag */
	fputs("</td>\n", ftr->fp);
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
	/* Print tag for start of unordered list */
	fputs("<ul>\n", ftr->fp);
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
	/* Print tag for end of unordered list */
	fputs("</ul>\n", ftr->fp);
}


/*
 * printlistentry()
 *
 * Print a list entry.
 *
 *	ftr		Formatter.
 *	options		Options. Only font options apply (GRW_FONT.. bits).
 *			All others are ignored. All others are ignored.
 *	text		Text of list entry.
 */
static void
printlistentry(grw_formatter_t *ftr, unsigned long options, char *text)
{

	printhtml(ftr, options, "<li>", text, "\n");
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
	fputs("<br>\n", ftr->fp);
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
	fputs("\n<hr>\n", ftr->fp);
}


/*
 * printhtml()
 *
 * Print HTML.
 *
 *	ftr		Formatter.
 *	options		Font options.
 *	prefix		Prefix to main text. No markup substitution will be
 *			performed on this string.
 *	text		Non-markup text to be printed. Any necessary
 *			substitution is performed on the text so that
 *			markup-type characters are substitued for their
 *			appropriate HTML entities.
 *	suffix		Suffix to main text. No markup substitution will be
 *			performed on this string.
 */
static void
printhtml(grw_formatter_t *ftr, unsigned long options, char *prefix,
	char *text, char *suffix)
{
	char new_text[TET_JNL_LEN];
	/* Print prefix. This will not need markup substitution. */
	fputs(prefix, ftr->fp);
	if (strcmp(text, "http:") == 0
	||  strcmp(text, "https:") == 0
	||  strcmp(text, "ftp:") == 0
	||  strcmp(text, "file:") == 0)
	{
		sprintf(new_text, "<a href=%s>%s</a>", text, text);
		fputs(new_text, ftr->fp);
		return;

	}
		/* Print the start tag of any font changes */
	if (options & GRW_FONT_BOLD)
		fputs("<b>", ftr->fp);
	
	if (options & GRW_FONT_ITALIC)
		fputs("<i>", ftr->fp);

	if (options & GRW_FONT_CODE)
		fputs("<tt>", ftr->fp);
	/* Print text to output, substituting markup */
	for ( ; *text != '\0'; text++)
	{
		switch (*text)
		{
		case '&':
			fputs("&amp;", ftr->fp);
			break;
		case '<':
			fputs("&lt;", ftr->fp);
			break;
		case '>':
			fputs("&gt;", ftr->fp);
			break;
		case '\"':
			fputs("&quot;", ftr->fp);
			break;
		default:
			fputc(*text, ftr->fp);
			break;
		}
	}

	/* Print the end tag of any font changes */
	if (options & GRW_FONT_CODE)
		fputs("</tt>", ftr->fp);

	if (options & GRW_FONT_ITALIC)
		fputs("</i>", ftr->fp);

	if (options & GRW_FONT_BOLD)
		fputs("</b>", ftr->fp);

	/* Print suffix. This will not need markup substitution. */
	fputs(suffix, ftr->fp);
}
