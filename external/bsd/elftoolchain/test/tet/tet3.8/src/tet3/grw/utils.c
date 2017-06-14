/*
 *	SCCS: @(#)utils.c	1.2 (02/11/06)
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
static char sccsid[] = "@(#)utils.c	1.2 (02/11/06) TETware release 3.8";
#endif

/************************************************************************

SCCS:		@(#)utils.c	1.2 02/11/06 TETware release 3.8
NAME:		utils.c
PRODUCT:	TETware Generic Report Writer.
AUTHOR:		Matthew Hails, UniSoft Ltd.
DATE CREATED:	19 July 2000
SYNOPSIS:

	void * grw_malloc(size_t size)
	void * grw_realloc(void *ptr, size_t size)
	char * grw_strdup(char *s)
	void grw_err(char *fmt, ...)
	void grw_fatal(char *fmt, ...)
	int grw_getnerrs(void)
	char * grw_namebase(char *path)
	void grw_setprogname(char *progname)
	void grw_setjournal(char *journal)
	void grw_setlinenumber(int linenumber)
	int grw_atoi(char *string, int *result)

DESCRIPTION:

	Various utility functions: memory allocation, error handling etc.

************************************************************************/

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdarg.h>
#include <limits.h>
#include "grw.h"


/* Local prototypes */
static void verr(char *fmt, va_list ap);


/* Static data */
static int nerrs = 0;
static char *currentprogname = NULL;
static char *currentjournal = NULL;
static int currentlinenumber = 0;


/*
 * grw_malloc()
 *
 * Allocates a block of memory. Uses malloc() to allocate the required amount
 * of memory and exits with an error message if malloc() fails. See malloc()
 * documentation on your system for details.
 *
 *	size	Number of bytes to allocate.
 *
 * Returns a pointer to the new memory. Never returns NULL; the program exits
 * via grw_fatal() if it cannot allocate the required memory.
 */
void *
grw_malloc(size_t size)
{
	char *ptr;

	ptr = malloc(size);
	if (ptr == NULL)
		grw_fatal("malloc failed for %lu bytes", (unsigned long)size);

	return ptr;
}


/*
 * grw_realloc()
 *
 * Reallocates a block of memory. Uses realloc() to allocate the memory and
 * exits with an error message if realloc() fails. See realloc() documentation
 * on your system for details.
 *
 *	ptr	Original pointer. May be NULL.
 *	size	New size of the memory block, in bytes.
 *
 * Returns a pointer to the new memory. Never returns NULL; the program exits
 * via grw_fatal() if it cannot allocate the required memory.
 */
void *
grw_realloc(void *ptr, size_t size)
{
	char *newptr;

	newptr = realloc(ptr, size);
	if (newptr == NULL)
		grw_fatal("realloc failed for %lu bytes", (unsigned long)size);

	return newptr;
}


/*
 * grw_strdup()
 *
 * String duplication (cf. UNIX strdup()). Uses grw_malloc() to allocate the
 * memory.
 *
 *	s	The string to copy.
 *
 * Returns a pointer to a copy of `s' in a newly allocated block of memory.
 * Uses grw_malloc() for the memory allocation, so never returns NULL; the
 * program exits via grw_fatal() if the allocation fails.
 */
char *
grw_strdup(char *s)
{
	char *newstr;

	newstr = grw_malloc(strlen(s) + 1);
	if (newstr)
		strcpy(newstr, s);

	return newstr;
}


/*
 * grw_err()
 *
 * Reports an error message to standard error.
 *
 *	fmt	Error message format string, cf. printf() et al.
 */
void
grw_err(char *fmt, ...)
{
	va_list ap;

	nerrs++;

	va_start(ap, fmt);
	verr(fmt, ap);
	va_end(ap);
}


/*
 * grw_fatal()
 *
 * Reports an error message to standard error and exits with failure status.
 *
 *	fmt	Error message format string, cf. printf() et al.
 *
 * Does not return. Exits with status GRW_ES_ERROR.
 */
void
grw_fatal(char *fmt, ...)
{
	va_list ap;

	va_start(ap, fmt);
	verr(fmt, ap);
	va_end(ap);

	exit(GRW_ES_ERROR);
}


/*
 * grw_usage()
 *
 * Print a usage error and exit with status GRW_ES_USAGE.
 */
void
grw_usage(void)
{
	fprintf(stderr, "Usage: %s [-c content ] [-f html|chtml|text] [-o out] [-p pagewidth] \\\n",
		currentprogname);
	fprintf(stderr, "	[-s stylesheet] [journal]\n");
	exit(GRW_ES_USAGE);
}


/*
 * grw_getnerrs()
 *
 * Get the number of errors that have occurred during processing.
 *
 * Returns the number of errors.
 */
int
grw_getnerrs(void)
{
	return nerrs;
}


/*
 * verr()
 *
 * Common error reporting function. Reports an error message from a variable
 * argument blob. If set, the error mesage is prefixed with the program name,
 * current journal name and current input line number.
 *
 *	fmt	Error message format string, cf. printf() et al.
 *	ap	Argument list, as obtained from va_start().
 */
static void
verr(char *fmt, va_list ap)
{
	if (currentprogname != NULL)
	{
		fputs(currentprogname, stderr);

		if (currentjournal != NULL)
		{
			if (strcmp(currentjournal, "-") == 0)
				fputs(" (<stdin>", stderr);
			else
				fprintf(stderr, " (\"%s\"", currentjournal);

			if (currentlinenumber > 0)
				fprintf(stderr, ", line %d", currentlinenumber);

			fputc(')', stderr);
		}

		fputs(": ", stderr);
	}

	vfprintf(stderr, fmt, ap);
	fputc('\n', stderr);
}


/*
 * grw_namebase()
 *
 * Like UNIX basename(). Does not modify `path' argument but returns a pointer
 * to final component of `path'.
 *
 *	path 	Path.
 *
 * Returns the final path component of `path'.
 */
char *
grw_namebase(char *path)
{
	char *base;
	char *lastsep;

	for (base = path, lastsep = NULL; *base != '\0'; base++)
	{
#ifdef _WIN32	/* -START-WIN32-CUT- */
		if (*base == '/' || *base == '\\')
#else		/* -END-WIN32-CUT- */
		if (*base == '/')
#endif		/* -WIN32-CUT-LINE- */
		{
			lastsep = base;
		}
	}

	return lastsep ? lastsep + 1 : path;
}


/*
 * grw_setprogname()
 *
 * Set the program name used for error reporting.
 *
 *	progname	The program name to be used in error reporting.
 */
void
grw_setprogname(char *progname)
{
	if (currentprogname != NULL)
		free(currentprogname);

	currentprogname = grw_strdup(grw_namebase(progname));
}


/*
 * grw_setjournal()
 *
 * Set the journal file name used for error reporting.
 *
 *	journal		The journal file name to be used in error reporting.
 *			Pass NULL to clear the name.
 */
void
grw_setjournal(char *journal)
{
	if (currentjournal != NULL)
		free(currentjournal);

	if (journal == NULL)
		currentjournal = NULL;
	else
		currentjournal = grw_strdup(journal);
}


/*
 * grw_setlinenumber()
 *
 * Set the line number used for error reporting.
 *
 *	linenumber	The line number to be used in error reporting.
 *			Set to <= 0 to clear it.
 */
void
grw_setlinenumber(int linenumber)
{
	currentlinenumber = linenumber;
}


/*
 * grw_atoi()
 *
 * Read an integer value from a string, like atoi(), but with error detection.
 *
 *	string		String containing integer value. See documentation
 *			on atoi() for details.
 *	result		Result is returned in here. It is set to 0 if
 *			grw_atoi() fails.
 *
 * Returns 0 on success, -1 on failure.
 */
int
grw_atoi(char *string, int *result)
{
	char *cp;
	long l;

	*result = 0;

	l = strtol(string, &cp, 10);
	if (l == 0L && cp == string)
		return -1;

	if (sizeof(long) > sizeof(int) && (l > INT_MAX || l < INT_MIN))
		return -1;

	*result = (int)l;

	return 0;
}
