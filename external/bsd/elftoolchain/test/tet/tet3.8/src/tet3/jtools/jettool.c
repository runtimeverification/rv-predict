/*
 *	SCCS: @(#)jettool.c	1.1 (99/09/02)
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
static char sccsid[] = "@(#)jettool.c	1.1 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:		@(#)jettool.c	1.1 99/09/02 TETware release 3.8
NAME:		jettool.c
PRODUCT:	TETware
AUTHOR:		Matthew Hails, UniSoft Ltd.
DATE CREATED:	9 July 1999
SYNOPSIS:

	void jt_tool(char *cmd, int ops, int argc, char **argv, char *suffix)
	void jt_execvp(char *file, char **argv)
	void jt_err(char *progname, char *fmt, ...)

DESCRIPTION:
	Utility routines for Java Enabled TETware build/clean/execute
	tools.

	jt_tool() executes an external program, optionally setting
	various environment variables. This routine does not return.
	If an error occurs, it exits with a failure status.

	jt_execvp() executes a file. On Unix systems, this calls execvp().
	On Win32 systems, this uses _spawnvp(), so it differs from a `real'
	exec since the process ID of the executed program is different.
	On success, this function does not return.

	jt_err() prints an error message to standard error and exits with
	a failure exit code.

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <errno.h>
#include <ctype.h>
#include "jtools.h"

#define NELEM(a)	((int)(sizeof(a)/sizeof((a)[0])))

#ifdef _WIN32		/* -START-WIN32-CUT- */
# include <process.h>
# define DIRSEP		'\\'
# define ISDIRSEP(s)	((s) == '\\' || (s) == '/')
# define PATHSEP	';'
#else /* _WIN32 */	/* -END-WIN32-CUT- */
# include <unistd.h>
# define DIRSEP		'/'
# define ISDIRSEP(s)	((s) == '/')
# define PATHSEP	':'
#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */

/* Local prototypes */
static char *config_path(char *);

/* Environment variable names giving search path for shared libraries */
static char *libpathvars[] =
{
#ifdef _WIN32		/* -START-WIN32-CUT- */
	"PATH"
#else /* _WIN32 */	/* -END-WIN32-CUT- */
	"LD_LIBRARY_PATH", "LIBPATH", "SHLIB_PATH"
#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */
};

/*
 * jt_tool()
 *
 * Execute an external program, optionally setting various environment
 * variables.
 *
 *	cmd		Name of command to execute.
 *	ops		Bitmask of operations to perform before the execution.
 *	argc		Argument count.
 *	argv		Command line arguments. The class name will be argv[1].
 *	suffix		Suffix to append to the class name.
 *
 * This routine does not return. If an error occurs, it exits with a failure
 * status.
 */
void
jt_tool(char *cmd, int ops, int argc, char **argv, char *suffix)
{
	/* The first two are static, since they are passed to jt_putenv() */
	static char newclasspath[MAXPATH];
	static char newlibpaths[NELEM(libpathvars)][MAXPATH];
	char *tetroot;
	char *classpath;
	char *libpath;
	int i;
	char **newargs;
	char *cp;

	/* Verify we have at least one argument */
	if (argc < 2)
		jt_err(argv[0], "incorrect argument count");

	/* Verify TET_ROOT is set in the environment */
	tetroot = getenv("TET_ROOT");
	if (tetroot == NULL || tetroot[0] == '\0')
		jt_err(argv[0], "TET_ROOT is NULL or not set in environment");

	/* If required, set the CLASSPATH environment variable to include the
	 * JET jar file.
	 */
	if (ops & OP_SETCLASSPATH)
	{
		classpath = getenv("CLASSPATH");
		if (classpath == NULL || classpath[0] == '\0')
		{
			sprintf(newclasspath,
				"CLASSPATH=.%c%s%clib%cjava%cjet.jar",
				PATHSEP, tetroot, DIRSEP, DIRSEP, DIRSEP);
		}
		else
		{
			sprintf(newclasspath,
				"CLASSPATH=%s%clib%cjava%cjet.jar%c%s%c.",
				tetroot, DIRSEP, DIRSEP, DIRSEP, PATHSEP,
				classpath, PATHSEP);
		}

		if (jt_putenv(newclasspath) != 0)
			jt_err(argv[0],
				"error setting CLASSPATH in environment");
	}

	/* If required, set the environment variable used as the search path
	 * for shared libraries to include the directory containing libjet.
	 */
	if (ops & OP_SETLIBPATH)
	{
		for (i = 0; i < NELEM(libpathvars); i++)
		{
			libpath = getenv(libpathvars[i]);
			if (libpath == NULL)
			{ 
				sprintf(newlibpaths[i], "%s=%s%clib%cjava",
					libpathvars[i], tetroot, DIRSEP,
					DIRSEP);
			}
			else
			{
				sprintf(newlibpaths[i], "%s=%s%clib%cjava%c%s",
					libpathvars[i], tetroot, DIRSEP,
					DIRSEP, PATHSEP, libpath);
			}

			if (jt_putenv(newlibpaths[i]) != 0)
				jt_err(argv[0],
					"error setting %s in environment",
					libpathvars[i]);
		}
	}

	/* Prepare command and new argument vector */
	newargs = malloc(sizeof(*newargs) * (argc + 1));
	if (newargs == NULL)
		jt_err(argv[0], "memory allocation failure");

	newargs[0] = cmd;

	newargs[1] = malloc(strlen(argv[1]) + strlen(suffix) + 1);
	if (newargs[1] == NULL)
		jt_err(argv[0], "memory allocation failure");

	/* Copy the classname and suffix into the first argument */
	sprintf(newargs[1], "%s%s", argv[1], suffix);

	/* Some versions of Sun's javac on Win32 don't recognize '/' as a
	 * directory separator when it comes to verifying that the class is in
	 * a correctly named file. So we swap to backslashes here.
	 */
	if (ops & OP_SWAPDIRSEP)
	{
		for (cp = newargs[1]; *cp != '\0'; cp++)
			if (ISDIRSEP(*cp))
				*cp = DIRSEP;
	}

	for (i = 2; i <= argc; i++)
		newargs[i] = argv[i];

	/* Exec command */
	cmd = config_path(cmd);
	jt_execvp(cmd, newargs);
	jt_err(argv[0], "error executing command \"%s\": %s", cmd,
		strerror(errno));
}

/*
 * jt_err()
 *
 * Prints error message to standard error and exits with a failure exit code.
 *
 *	progname	Name of program. Used to prefix error message.
 *	fmt		Format of error message, cf. printf().
 */
void
jt_err(char *progname, char *fmt, ...)
{
	char *cp;
	va_list vlist;

	for (cp = progname; *cp != '\0'; cp++)
	{
		if (ISDIRSEP(*cp) && *(cp + 1) != '\0')
			progname = cp + 1;
	}

	fprintf(stderr, "%s: ", progname);

	va_start(vlist, fmt);
	vfprintf(stderr, fmt, vlist);
	va_end(vlist);

	fprintf(stderr, "\n");

	exit(EXIT_FAILURE);
}

/*
 * jt_execvp()
 *
 * Executes a file. On Unix systems, this calls execvp(). On Win32 systems,
 * this uses _spawnvp(), so it differs from a `real' exec since the process ID
 * of the executed program is different.
 *
 *	file	File to execute. The PATH environment is used.
 *	argv	Arguments to command.
 *
 * On success, this function does not return.
 */
void
jt_execvp(char *file, char **argv)
{
#ifdef _WIN32		/* -START-WIN32-CUT- */
	int rv;

	rv = _spawnvp(_P_WAIT, file, argv);
	if (rv != -1)
		exit(rv);
#else /* _WIN32 */	/* -END-WIN32-CUT- */
	execvp(file, argv);
#endif /* _WIN32 */	/* -WIN32-CUT-LINE- */
}

/*
**	config_path() - look for a configuration variable named
**		TET_<cmd>_PATH in the configuration file
**
**	if one is found, return a pointer to its value stored in
**	malloc'd memory; otherwise just return cmd
*/

static char *config_path(char *cmd)
{
	FILE *fp;
	char *fname;
	static char varfmt[] = "TET_%.32s_PATH";
	char var[sizeof varfmt + 32];
	int varlen;
	char buf[BUFSIZ];
	char *p, *val;

	/* return now if cmd has a path prefix */
	for (p = cmd; *p; p++)
		if (ISDIRSEP(*p))
			return(cmd);

	/*
	** get the name of the configuration file out of the environment
	** received from tcc
	*/
	if ((fname = getenv("TET_CONFIG")) == (char *) 0 || !*fname)
		return(cmd);

	/* generate the name of the config variable that we are looking for */
	(void) sprintf(var, varfmt, cmd);
	varlen = strlen(var);
	for (p = var; *p; p++)
		if (islower(*p))
			*p = toupper(*p);

	/* open the config file */
	if ((fp = fopen(fname, "r")) == (FILE *) 0) {
		(void) fprintf(stderr, "warning: can't open %s: errno = %d\n",
			fname, errno);
		return(cmd);
	}

	/*
	** search for the variable in the config file, ignoring
	** blank lines, comments and badly formatted lines;
	** if we find the variable that we are looking for, arrange to
	** return its value
	*/
	while (fgets(buf, sizeof buf, fp) != (char *) 0) {
		for (p = buf; p < &buf[sizeof buf]; p++)
			if (*p == '\n') {
				*p = '\0';
				break;
			}
		if (
			buf[0] == '\0' ||
			buf[0] == '#' ||
			(p = strchr(buf, '=')) == (char *) 0 ||
			varlen != (int) (p - buf) ||
			strncmp(buf, var, (size_t) varlen)
		) {
			continue;
		}
		if (
			*(val = p + 1) != '\0' &&
			(p = (char *) malloc(strlen(val) + 1)) != (char *) 0
		) {
			(void) strcpy(p, val);
			cmd = p;
		}
		break;
	}

	/* finally, close the config file and return */
	(void) fclose(fp);
	return(cmd);
}
