/*
 *	SCCS: @(#)parser.c	1.2 (02/11/06)
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
static char sccsid[] = "@(#)parser.c	1.2 (02/11/06) TETware release 3.8";
#endif

/************************************************************************

SCCS:		@(#)parser.c	1.2 02/11/06 TETware release 3.8
NAME:		parser.c
PRODUCT:	TETware Generic Report Writer.
AUTHOR:		Matthew Hails, UniSoft Ltd.
DATE CREATED:	19 July 2000
SYNOPSIS:

	void grw_parsejournal(char *journal, char *content,
		struct grw_formatter *ftr, char *output)

DESCRIPTION:

	Code used to parse a journal file.

************************************************************************/

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <ctype.h>
#include "tet_jrnl.h"
#include "grw.h"


#define	NSUBFIELDS	5


/*
 * grw_parsejournal()
 *
 * Parse a journal file.
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
grw_parsejournal(char *journal, char *content, struct grw_formatter *ftr,
	char *output)
{
	FILE *fp;
	char line[TET_JNL_LEN + 1];
	int linenum;
	int discardnext;
	int len;
	int tag;
	char *f1;
	char *f2;
	char *subfields[NSUBFIELDS];
	int i;
	char *cp;
	int new;

	/* Initialize input stream: open file or set to stdin */
	if (strcmp(journal, "-") == 0)
	{
		fp = stdin;
		clearerr(stdin);
	}
	else
	{
		fp = fopen(journal, "r");
		if (fp == NULL)
		{
			grw_fatal("failed to open journal file \"%s\"",
				journal);
		}
	}

	/* Set journal name for error reporting */
	grw_setjournal(journal);

	/* Run handler's beginning of journal processing */
	grw_beginjournal(journal, content, ftr, output);

	/* Read lines from journal file and process */
	discardnext = 0;

	for (linenum = 1; fgets(line, sizeof(line), fp) != NULL; linenum++)
	{
		/* If this is a continuation, then discard the rest of the
		 * line (it should not be longer than TET_JNL_LEN anyway!).
		 */
		if (discardnext)
			continue;

		/* Check read whole line. If so, then remove newline.
		 * Otherwise, discard the next line.
		 */
		len = strlen(line);
		if (line[len - 1] == '\n')
		{
			line[len - 1] = '\0';
			discardnext = 0;
		}
		else
		{
			discardnext = 1;
		}

		/* Set line number for error reporting */
		grw_setlinenumber(linenum);

		/* Get the tag from the beginning of the line */
		if (grw_atoi(line, &tag) != 0)
		{
			grw_err("invalid tag in line: \"%s\"", line);
			continue;
		}

		/* Split journal file line into required fields */
		for (i = 0; i < NSUBFIELDS; i++)
			subfields[i] = "";

		for (f1 = line; *f1 != '\0' && *f1 != '|'; f1++)
			;

		if (*f1 == '|')
		{
			f1++;

			for (f2 = f1; *f2 != '\0' && *f2 != '|'; f2++)
				;

			if (*f2 == '|')
				*f2++ = '\0';
		}
		else
		{
			f2 = "";
		}

		for (i = 0, new = 1, cp = f1; *cp != '\0' && i < NSUBFIELDS;
			cp++)
		{
			if (isspace((unsigned char)*cp))
			{
				*cp = '\0';
				new = 1;
			}
			else if (new)
			{
				subfields[i++] = cp;
				new = 0;
			}
		}

		/* Invoke line handling code on this line */
		grw_handleline(tag, subfields[0], subfields[1], subfields[2],
			subfields[3], subfields[4], f2);
	}

	/* Reset line number used in error reporting */
	grw_setlinenumber(0);

	/* Run handler's end of journal processing */
	grw_endjournal();

	/* Reset journal name used in error reporting */
	grw_setjournal(NULL);

	/* Check errors on stream */
	if (ferror(fp))
		grw_fatal("error reading from journal \"%s\"", journal);

	if (fp != stdin)
	{
		/* Close the journal file */
		if (fclose(fp) != 0)
			grw_fatal("error reading from journal \"%s\"", journal);
	}
}
