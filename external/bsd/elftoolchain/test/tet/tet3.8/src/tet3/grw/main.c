/*
 *	SCCS: @(#)main.c	1.2 (02/11/06)
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
static char sccsid[] = "@(#)main.c	1.2 (02/11/06) TETware release 3.8";
static char *copyright[] = {
	"(C) Copyright 2000 The Open Group",
	"All rights reserved"
};
#endif

/************************************************************************

SCCS:		@(#)main.c	1.2 02/11/06 TETware release 3.8
NAME:		main.c
PRODUCT:	TETware Generic Report Writer.
AUTHOR:		Matthew Hails, UniSoft Ltd.
DATE CREATED:	19 July 2000
SYNOPSIS:

	int main(int argc, char **argv)

DESCRIPTION:
	Entry point to the Generic Report Writer.

************************************************************************/


#include <stdlib.h>
#include <stdio.h>
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include <string.h>
#include "dtmac.h"
#include "grw.h"


/* Default values for options */
#define DEFAULT_CONTENT	"3"
#define DEFAULT_FORMAT	"chtml"


/*
 * main()
 *
 * Entry point to Generic Report Writer. For values of options see accompanying
 * documentation.
 *
 *	argc	Number of elements in argv.
 *	argv	Command line argument vector.
 *
 * main() does not return. It exits with one of the GRW_ES_.. values defined
 * in grw.h.
 */
int
main(int argc, char **argv)
{
	char *content;
	char *format;
	char *output;
	int pagewidth;
	char *stylesheet;
	int c;
	struct grw_formatter *ftr;
	char *journal;
	extern char *optarg;
	extern int optind;

	/* Set program name for error reporting */
	grw_setprogname(argv[0]);

	/* Set default values for options */
	content = DEFAULT_CONTENT;
	format = DEFAULT_FORMAT;
	output = "-";
	pagewidth = 0;
	stylesheet = NULL;

	/* Process command line options */
	while ((c = GETOPT(argc, argv, "c:f:o:p:s:?")) != -1)
	{
		switch (c)
		{
		case 'c':
			/* Content specifier */
			content = optarg;
			break;
		case 'f':
			/* Document format of output */
			format = optarg;
			break;
		case 'o':
			/* Output file */
			output = optarg;
			break;
		case 'p':
			/* Output file */
			if (grw_atoi(optarg, &pagewidth) != 0)
			{
				grw_err("illegal argument to -w options");
				grw_usage();
			}
			break;
		case 's':
			/* Stylesheet */
			stylesheet = optarg;
			break;
		default:
			/* Illegal option */
			grw_usage();
			break;
		}
	}

	/* Create the formatter */
	if (strcmp(format, "html") == 0)
	{
		ftr = grw_createhtmlformatter(0, stylesheet);
	}
	else if (strcmp(format, "chtml") == 0)
	{
		ftr = grw_createhtmlformatter(1, stylesheet);
	}
	else if (strcmp(format, "text") == 0)
	{
		ftr = grw_createtextformatter(pagewidth);
	}
	else
	{
		grw_fatal("unrecognized format \"%s\"", format);
		/* NOTREACHED */
		return(GRW_ES_ERROR);
	}

	/* Deal with the arguments remaining after the options.
	 * No arguments means read from standard input.
	 * One argument specifies a journal file to read.
	 * More than one argument is a usage error.
	 */
	if (optind >= argc)
	{
		journal = "-";
	}
	else if (optind == argc - 1)
	{
		journal = argv[optind];
	}
	else
	{
		grw_usage();
		/* NOTREACHED */
		return(GRW_ES_ERROR);
	}

	/* Parse journal file and produce report */
	grw_parsejournal(journal, content, ftr, output);

	/* Exit - only with OK status if no errors in parsing */
	exit(grw_getnerrs() == 0 ? GRW_ES_OK : GRW_ES_ERROR);
}
