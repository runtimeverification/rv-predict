/*
 *	SCCS: @(#)getopt.c	1.3 (97/07/21)
 *
 * Copyright 1990, 1991, 1992 by the Massachusetts Institute of Technology and
 * UniSoft Group Limited.
 * 
 * Permission to use, copy, modify, distribute, and sell this software and
 * its documentation for any purpose is hereby granted without fee,
 * provided that the above copyright notice appear in all copies and that
 * both that copyright notice and this permission notice appear in
 * supporting documentation, and that the names of MIT and UniSoft not be
 * used in advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.  MIT and UniSoft
 * make no representations about the suitability of this software for any
 * purpose.  It is provided "as is" without express or implied warranty.
 *
 * $XConsortium: getopt.c,v 1.2 92/07/01 11:59:04 rws Exp $
 */

#ifndef lint
static char sccsid[] = "@(#)getopt.c	1.3 (97/07/21) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)getopt.c	1.3 97/07/21 TETware release 3.8
NAME:		getopt.c
PRODUCT:	TETware
AUTHOR:		this file lifted from the ETET contrib/port directory
DATE CREATED:	September 1996

DESCRIPTION:
	getopt() function for WIN32 platforms

MODIFICATIONS:

************************************************************************/


#ifdef _WIN32	/* -START-WIN32-CUT- */

#include <stdio.h>
#include <string.h>
#include "dtmac.h"

char	*optarg;
int 	optind = 1;
int 	opterr = 1;

int tet_getopt(argc, argv, optstring)
int 	argc;
char	**argv;
char	*optstring;
{
	static int 	avplace;
	char	*ap;
	char	*cp;
	int 	c;

	if (optind >= argc)
		return(EOF);

	ap = argv[optind] + avplace;

	/* At begining of arg but not an option */
	if (avplace == 0) {
		if (ap[0] != '-')
			return(EOF);
		else if (ap[1] == '-') {
			/* Special end of options option */
			optind++;
			return(EOF);
		} else if (ap[1] == '\0')
			return(EOF);	/* single '-' is not allowed */
	}

	/* Get next letter */
	avplace++;
	c = *++ap;

	cp = strchr(optstring, c);
	if (cp == NULL || c == ':') {
		if (opterr)
			fprintf(stderr, "Unrecognised option -- %c\n", c);
		return('?');
	}

	if (cp[1] == ':') {
		/* There should be an option arg */
		avplace = 0;
		if (ap[1] == '\0') {
			/* It is a separate arg */
			if (++optind >= argc) {
				if (opterr)
					fprintf(stderr, "Option requires an argument\n");
				return('?');
			}
			optarg = argv[optind++];
		} else {
			/* is attached to option letter */
			optarg = ap + 1;
			++optind;
		}
	} else {

		/* If we are out of letters then go to next arg */
		if (ap[1] == '\0') {
			++optind;
			avplace = 0;
		}

		optarg = NULL;
	}

	return(c);
}

#else		/* -END-WIN32-CUT- */

int tet_getopt_c_not_used;

#endif		/* -WIN32-CUT-LINE- */

