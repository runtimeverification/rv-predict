/*
 *	SCCS: @(#)jet-spawn.c	1.1 (99/09/02)
 *
 *	UniSoft Ltd., London, England
 *
 * Copyright (c) 1999 The Open Group
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
 *
 */

#ifndef lint
static char sccsid[] = "@(#)jet-spawn.c	1.1 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:		@(#)jet-spawn.c	1.1 99/09/02 TETware release 3.8
NAME:		jet-spawn.c
PRODUCT:	TETware
AUTHOR:		Matthew Hails, UniSoft Ltd.
DATE CREATED:	3 August 1999

DESCRIPTION:
	Spawn tool for TETware Java API. Deals with extra arguments
	added by API: searches for magic string "TET_JAVA_SPAWN_MAGIC",
	the argument following that is taken as the name of the class.
	Then executes the java interpreter with the appropriate
	environment, for that class, parsing the argument list with the
	magic string and class name removed to the class.

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <errno.h>
#include "jtools.h"

#define TET_JAVA_SPAWN_MAGIC	"TET_JAVA_SPAWN_MAGIC"

int
main(int argc, char **argv)
{
	char **newargv;
	int n;
	int i;

	/* Allocate storage for new argument vector */
	newargv = malloc(argc * sizeof(*newargv));

	/* Search for magic string in arguments - this indicates the argument
	 * after that is the name of a Java class which should be executed
	 * using the Java interpreter.
	 */
	for (n = 1; n < argc; n++)
		if (strcmp(argv[n], TET_JAVA_SPAWN_MAGIC) == 0)
			break;

	if (n >= argc - 1)
		jt_err(argv[0],
			"process not launched by TET.TestSession.tet_jspawn()");

	/* The java interpreter */
	newargv[0] = "java";

	/* Name of the Java class */
	newargv[1] = argv[n + 1];

	/* The other API arguments */
	for (i = 1; i < n; i++)
		newargv[i + 1] = argv[i];

	/* The user argv */
	for (i += 2; i < argc; i++)
		newargv[i - 1] = argv[i];

	newargv[argc - 1] = NULL;

	/* Execute Java interpreter with classpath and library path set to
	 * contain the appropriate TETware paths.
	 */
	jt_tool("java", OP_SETCLASSPATH|OP_SETLIBPATH, argc - 1, newargv, "");
	return EXIT_SUCCESS;
}
