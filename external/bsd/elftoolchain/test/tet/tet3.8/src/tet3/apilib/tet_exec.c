/*
 *	SCCS: @(#)tet_exec.c	1.25 (99/11/15)
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1996 X/Open Company Limited
 *
 * All rights reserved.  No part of this source code may be reproduced,
 * stored in a retrieval system, or transmitted, in any form or by any
 * means, electronic, mechanical, photocopying, recording or otherwise,
 * except as stated in the end-user licence agreement, without the prior
 * permission of the copyright owners.
 * A copy of the end-user licence agreement is contained in the file
 * Licence which accompanies this distribution.
 * 
 * X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
 * the UK and other countries.
 */

/*
 * Copyright 1990 Open Software Foundation (OSF)
 * Copyright 1990 Unix International (UI)
 * Copyright 1990 X/Open Company Limited (X/Open)
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose and without fee is hereby granted, provided
 * that the above copyright notice appear in all copies and that both that
 * copyright notice and this permission notice appear in supporting
 * documentation, and that the name of OSF, UI or X/Open not be used in 
 * advertising or publicity pertaining to distribution of the software 
 * without specific, written prior permission.  OSF, UI and X/Open make 
 * no representations about the suitability of this software for any purpose.  
 * It is provided "as is" without express or implied warranty.
 *
 * OSF, UI and X/Open DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, 
 * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO 
 * EVENT SHALL OSF, UI or X/Open BE LIABLE FOR ANY SPECIAL, INDIRECT OR 
 * CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF 
 * USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR 
 * OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR 
 * PERFORMANCE OF THIS SOFTWARE.
 */

#ifndef lint
static char sccsid[] = "@(#)tet_exec.c	1.25 (99/11/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tet_exec.c	1.25 99/11/15 TETware release 3.8
NAME:		'C' API exec process function
PRODUCT:	TETware
AUTHOR:		Geoff Clare, UniSoft Ltd.
DATE CREATED:	31 July 1990
SYNOPSIS:

	int tet_exec(char *file, char *argv[], char *envp[]);

DESCRIPTION:

	Tet_exec() is used to execute the specified file in the same
	way as the POSIX.1 execve() interface except that it passes
	extra information on the command line and in the environment for
	use by a main() provided with the API.  This then calls the
	user-supplied tet_main() routine with the argv[] which was
	passed to tet_exec().

	Tet_exec() does not return if successful, but may return -1 if
	either execve() or malloc() fails.

	tet_exec() is not implemented on WIN32 platforms.

MODIFICATIONS:

	June 1992
	This file is derived from TET release 1.10

	Andrew Dingwall, UniSoft Ltd., October 1992
	Modified to work with DTET tcmchild processes.

	Denis McConalogue, UniSoft Limited, August 1993
                changed dtet to tet2 in #include

	Denis McConalogue, UniSoft Limited, September 1993
	logoff XRESD and SYNCD before exec

	Geoff Clare, UniSoft Ltd., July 1996
	Changes for TETWare.

	Geoff Clare, UniSoft Ltd., Sept 1996
	Changes for TETWare-Lite.

	Geoff Clare, UniSoft Ltd., Oct 1996
	Enable tracing in TETware-Lite.

	Andrew Dingwall, UniSoft Ltd., April 1997
	unbundled exec preparation and cleanup functions from tet_exec()
	so as to enable tet_spawn() to work correctly on Windows NT

	Andrew Dingwall, UniSoft Ltd., June 1997
	added call to tet_xdxrsend() after tet_xdlogon() -
	needed to make parallel remote and distributed test cases work
	correctly

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
	Andrew Dingwall, UniSoft Ltd., July 1999
	added support for 'other language' APIs

	Andrew Dingwall, UniSoft Ltd., October 1999
	added support for strict POSIX threads


************************************************************************/

#include <stdlib.h>
#include <stdio.h>
#include <sys/types.h>
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include <string.h>
#include <errno.h>
#include <time.h>
#include "dtmac.h"
#include "dtthr.h"
#include "tet_api.h"
#include "servlib.h"
#include "dtetlib.h"
#include "apilib.h"
#include "dtmsg.h"
#include "ptab.h"
#include "tslib.h"
#include "error.h"
#include "globals.h"
#include "ltoa.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;
#endif


#ifndef _WIN32	/* -WIN32-CUT-LINE- */
extern char **ENVIRON;
#endif		/* -WIN32-CUT-LINE- */


/*
** these are hard limits for the maximum number of arguments and environment
** variables that tet_exec() can handle when called from a child of a
** multi-threaded parent in strict POSIX mode
**
** they can be overridden on the compiler command line when the API is built
*/
#ifdef TET_STRICT_POSIX_THREADS
#  ifndef TET_EXEC_MAXARGS
#    define TET_EXEC_MAXARGS	256	/* max number of args to tet_exec() */
#  endif
#  ifndef TET_EXEC_MAXENVS
#    define TET_EXEC_MAXENVS	256	/* max number of env vars */
#  endif
#endif


static struct envlist {
	char *name;
	char *curptr;
	int ok;
} envlist[] = {
	{ "TET_CONFIG", 	NULL, 0 },
	{ "TET_ROOT", 		NULL, 0 },
	{ "TET_TIARGS",		NULL, 0 },
#ifndef TET_LITE /* -START-LITE-CUT- */
	{ "TET_TSARGS",		NULL, 0 },
#else /* -END-LITE-CUT- */
	{ "TET_RESFILE", 	NULL, 0 },
	{ "TET_TMPRESFILE", 	NULL, 0 },
#endif /* -LITE-CUT-LINE- */
	{ NULL,			NULL, 0 }
};


/* static function declarations */
#ifdef TET_STRICT_POSIX_THREADS
   static int as_strncmp PROTOLIST((char *, char *, size_t));
#endif


/*
**	tet_exec_prep() - prepare an argv and an envp for an exec() or
**		a spawn() operation
**
**	return 0 if successful or -1 on error
**
**	if successful, the argv and envp to use are returned indirectly
**	through *nargvp and *nenvp
**
**	Note that in the thread-safe API, this function must be called
**	with the API lock acquired.
**
**	The memory allocated by this function may be freed by a call to
**	tet_exec_cleanup().
**
**	NOTE: this function may only call async-safe functions when
**	it is called from the child of a multi-threaded parent
**	when running in strict POSIX mode.
*/

int tet_exec_prep(file, argv, envp, nargvp, nenvpp)
char *file, *argv[], *envp[], ***nargvp, ***nenvpp;
{
	char *cp;
	register char **ep;
	register struct envlist *elp;
	int cnt, addcnt, n;
#ifdef TET_STRICT_POSIX_THREADS
	char *p1, *p2;
	static char msg[] = "child of multi-threaded parent in strict POSIX mode";
	static char *nargv[TET_EXEC_MAXARGS + 1];
	static char *nenv[TET_EXEC_MAXENVS + 1];
	static char s1[LNUMSZ], s2[LNUMSZ], s3[LNUMSZ], s4[LNUMSZ];
#endif

	/* allocate new argv array, with room for four extra args plus
	   NULL terminator */
	for (cnt = 0; argv[cnt] != NULL; cnt++)
		;
	cnt += (TET_TCMC_USER_ARGS + 1);
	errno = 0;

#ifdef TET_STRICT_POSIX_THREADS
	if (IS_CHILD_OF_MULTITHREAD_PARENT)
	{
		if (cnt > TET_EXEC_MAXARGS) {
			tet_error(0, msg);
			tet_error(0, "too many arguments passed to tet_exec_prep()");
			tet_errno = TET_ER_2BIG;
			errno = E2BIG;
			return -1;
		}
		*nargvp = nargv;
	}
	else
#endif
	{
		errno = 0;
		*nargvp = (char **) malloc((size_t)(cnt * sizeof(char *)));
		TRACE2(tet_Tbuf, 6, "allocate newargv = %s", tet_i2x(*nargvp));
		if (*nargvp == NULL) {
			tet_error(errno, "can't allocate memory for newargv in tet_exec_prep()");
			tet_errno = TET_ER_ERR;
			errno = ENOMEM;
			return -1;
		}
	}

	/*
	** first few args are exec file name, TP number, current activity,
	** context and block
	*/
	**nargvp = file;
#ifdef TET_STRICT_POSIX_THREADS
	if (IS_CHILD_OF_MULTITHREAD_PARENT)
	{
		for (p1 = tet_i2a(tet_thistest), p2 = s1; *p1 && p2 < &s1[sizeof s1 - 1]; p1++, p2++)
			*p2 = *p1;
		*p2 = '\0';
		*(*nargvp + TET_TCMC_THISTEST) = s1;
		for (p1 = tet_l2a(tet_activity), p2 = s2; *p1 && p2 < &s2[sizeof s2 - 1]; p1++, p2++)
			*p2 = *p1;
		*p2 = '\0';
		*(*nargvp + TET_TCMC_ACTIVITY) = s2;
		for (p1 = tet_l2a(tet_context), p2 = s3; *p1 && p2 < &s3[sizeof s3 - 1]; p1++, p2++)
			*p2 = *p1;
		*p2 = '\0';
		*(*nargvp + TET_TCMC_CONTEXT) = s3;
		for (p1 = tet_l2a(tet_next_block), p2 = s4; *p1 && p2 < &s4[sizeof s4 - 1]; p1++, p2++)
			*p2 = *p1;
		*p2 = '\0';
		*(*nargvp + TET_TCMC_BLOCK) = s4;
	}
	else
#endif
	{
		*(*nargvp + TET_TCMC_THISTEST) = tet_strstore(tet_i2a(tet_thistest));
		*(*nargvp + TET_TCMC_ACTIVITY) = tet_strstore(tet_l2a(tet_activity));
		*(*nargvp + TET_TCMC_CONTEXT) = tet_strstore(tet_l2a(tet_context));
		*(*nargvp + TET_TCMC_BLOCK) =
#ifdef TET_THREADS
					tet_strstore(tet_l2a(tet_next_block));
#else
					tet_strstore(tet_l2a(tet_block));
#endif
	}

	/* copy remaining args from argv[] and add NULL terminator */
	n = TET_TCMC_USER_ARGS;
	while (*argv != NULL && n < cnt - 1)
		*(*nargvp + n++) = *argv++;
	*(*nargvp + n) = NULL;

	/* make sure that all the new arg strings could be allocated */
	for (n = 1; n < TET_TCMC_USER_ARGS; n++)
		if (*(*nargvp + n) == (char *) 0) {
			tet_error(errno, "can't allocate memory for new arg in tet_exec_prep()");
			tet_errno = TET_ER_ERR;
			errno = ENOMEM;
			return(-1);
		}


#ifndef TET_LITE /* -START-LITE-CUT- */
#  ifdef TET_STRICT_POSIX_THREADS
	/*
	** if we are in the child of a multi-threaded parent, we will have
	** to make do with the inherited values of TET_TI_ARGS and TET_TS_ARGS
	*/
	if (!IS_CHILD_OF_MULTITHREAD_PARENT)
#  endif
	{
		/*
		** generate an updated TET_TIARGS string -
		** tet_ti_tcmputenv does this and puts it in the environment
		*/
		if (tet_ti_tcmputenv(tet_mysysid, tet_snid, tet_xrid, tet_snames, tet_Nsname) < 0) {
			tet_error(errno, "can't add TET_TIARGS to environment in tet_exec_prep()");
			tet_errno = TET_ER_ERR;
			errno = ENOMEM;
			return(-1);
		}

		/*
		** generate a TET_TSARGS string -
		** tet_ts_tcmputenv does this and puts it in the environment
		*/
		if (tet_ts_tcmputenv() < 0) {
			tet_error(errno, "can't add TET_TSARGS to environment in tet_exec_prep()");
			tet_errno = TET_ER_ERR;
			errno = ENOMEM;
			return(-1);
		}
	}
#endif /* -END-LITE-CUT- */


	/* locate each needed env var in current environment */
	for (elp = envlist; elp->name != NULL; elp++)
		elp->curptr = NULL;
	for (ep = ENVIRON; *ep != NULL; ep++)
	{
		if ((cp = tet_equindex(*ep)) == (char *) 0)
			continue;
		for (elp = envlist; elp->name != NULL; elp++)
		{
			if (
				elp->curptr == NULL &&
				!
#ifdef TET_STRICT_POSIX_THREADS
				as_strncmp
#else
				strncmp
#endif
				(*ep, elp->name, (size_t) (cp - *ep))
			) {
				elp->curptr = *ep;
				break;
			}
		}
	}

	/* ensure each needed env var is in the environment passed to
	   tet_exec() and has the value from environ[] */
	for (elp = envlist; elp->name != NULL; elp++)
	{
		elp->ok = 0;
		if (elp->curptr == NULL)
		{
			/* no value in environ[], so we don't need
			   to bother looking in envp[] */
			elp->ok++;
		}
	}
	for (ep = envp, cnt = 0; *ep != NULL; ep++, cnt++)
	{
		if ((cp = tet_equindex(*ep)) == (char *) 0)
			continue;
		for (elp = envlist; elp->name != NULL; elp++)
		{
			if (
				elp->ok == 0 &&
				!
#ifdef TET_STRICT_POSIX_THREADS
				as_strncmp
#else
				strncmp
#endif
				(*ep, elp->name, (size_t) (cp - *ep))
			) {
				elp->ok++;
				if (elp->curptr != NULL)
					*ep = elp->curptr;
				break;
			}
		}
	}

	/* if all the variables are present in the environment passed to
		tet_exec(), then we can use that -
		otherwise, we must allocate a new envp array and add
		variables that are missing */

	addcnt = 0;
	for (elp = envlist; elp->name != NULL; elp++)
	{
		if (elp->ok == 0)
			addcnt++;
	}

	if (addcnt == 0)
		*nenvpp = (char **) envp;
	else
	{
		cnt += addcnt + 1;
#ifdef TET_STRICT_POSIX_THREADS
		if (IS_CHILD_OF_MULTITHREAD_PARENT)
		{
			if (cnt > TET_EXEC_MAXENVS) {
				tet_error(0, msg);
				tet_error(0, "environment contains too many variables in tet_exec_prep()");
				tet_errno = TET_ER_2BIG;
				errno = E2BIG;
				return -1;
			}
			*nenvpp = nenv;
		}
		else
#endif
		{
			*nenvpp = (char **) malloc((size_t) (cnt * sizeof(char *)));
			TRACE2(tet_Tbuf, 6, "allocate new envp = %s",
				tet_i2x(*nenvpp));
			if (*nenvpp == NULL) {
				tet_error(errno, "can't allocate memory for newenvp in tet_exec_prep()");
				tet_errno = TET_ER_ERR;
				errno = ENOMEM;
				return -1;
			}
		}

		for (cnt = 0; *envp != NULL; cnt++, envp++)
			*(*nenvpp + cnt) = *envp;

		for (elp = envlist; elp->name != NULL; elp++)
		{
			if (elp->ok == 0)
				*(*nenvpp + cnt++) = elp->curptr;
		}

		*(*nenvpp + cnt) = NULL;
	}

	return(0);
}

/*
**	tet_exec_cleanup() - free memory allocated by tet_exec_prep()
**
**	NOTE: this function may only call async-safe functions when
**	it is called from the child of a multi-threaded parent
**	when running in strict POSIX mode.
*/

void tet_exec_cleanup(envp, newargv, newenvp)
char **envp, **newargv, **newenvp;
{
	int n;

	if (
#ifdef TET_STRICT_POSIX_THREADS
		!IS_CHILD_OF_MULTITHREAD_PARENT &&
#endif
		newargv
	) {
		for (n = 1; n < TET_TCMC_USER_ARGS; n++)
			if (*(newargv + n)) {
				TRACE3(tet_Tbuf, 6, "free new arg %s = %s",
					tet_i2a(n), tet_i2x(*(newargv + n)));
				free(*(newargv + n));
			}
		TRACE2(tet_Tbuf, 6, "free newargv = %s", tet_i2x(newargv));
		free((char *) newargv);
	}

	if (
#ifdef TET_STRICT_POSIX_THREADS
		!IS_CHILD_OF_MULTITHREAD_PARENT &&
#endif
		newenvp && newenvp != envp
	) {
		TRACE2(tet_Tbuf, 6, "free newenvp = %s", tet_i2x(newenvp));
		free((char *) newenvp);
	}
}



#ifndef _WIN32	/* -WIN32-CUT-LINE- */

/*
**	tet_exec() - the API function - not supported on Windows NT
**
**	there is no return if successful
**
**	return -1 with tet_errno set on error
**
**	NOTE: this function may only call async-safe functions when
**	it is called from the child of a multi-threaded parent
**	when running in strict POSIX mode.
*/

TET_IMPORT int tet_exec(file, argv, envp)
char *file, *argv[], *envp[];
{
	int rc, errsave;
	char **newargv = (char **) 0, **newenvp = (char **) 0;

	tet_check_api_status(TET_CHECK_API_INITIALISED | TET_EXEC_CALL);

	if (!file || !*file || !argv || !envp) {
		tet_errno = TET_ER_INVAL;
		return(-1);
	}

#  ifdef TET_STRICT_POSIX_THREADS
	if (!IS_CHILD_OF_MULTITHREAD_PARENT)
#  endif
	{
		API_LOCK;
	}
	rc = tet_exec_prep(file, argv, envp, &newargv, &newenvp);
	errsave = errno;
#  ifdef TET_STRICT_POSIX_THREADS
	if (!IS_CHILD_OF_MULTITHREAD_PARENT)
#  endif
	{
		API_UNLOCK;
	}

	if (rc == 0) {
		ASSERT(newargv != (char **) 0);
		ASSERT(newenvp != (char **) 0);
#  ifdef TET_STRICT_POSIX_THREADS
		if (!IS_CHILD_OF_MULTITHREAD_PARENT)
#  endif
		{
			tet_logoff();
		}
		errno = 0;
		rc = execve(file, newargv, newenvp);
		errsave = errno;
#  ifndef TET_LITE	/* -START-LITE-CUT- */
#    ifdef TET_STRICT_POSIX_THREADS
		if (!IS_CHILD_OF_MULTITHREAD_PARENT)
#    endif
		{
			API_LOCK;
			if (tet_xdlogon() == 0)
				(void) tet_xdxrsend(tet_xrid);
			(void) tet_sdlogon();
			API_UNLOCK;
		}
#  endif		/* -END-LITE-CUT- */
		switch (errsave) {
		case E2BIG:
			tet_errno = TET_ER_2BIG;
			break;
		case EACCES:
			tet_errno = TET_ER_PERM;
			break;
		case ENOENT:
			tet_errno = TET_ER_NOENT;
			break;
		case ENAMETOOLONG:
		case ENOEXEC:
		case ENOTDIR:
			tet_errno = TET_ER_INVAL;
			break;
		default:
			error(errsave, "tet_exec() received unexpected errno value from execve()",
				(char *) 0);
			tet_errno = TET_ER_ERR;
			break;
		}
	}

	tet_exec_cleanup(envp, newargv, newenvp);

	if (rc < 0)
		errno = errsave;
	return(rc);
}

#endif		/* -WIN32-CUT-LINE- */


#ifdef TET_STRICT_POSIX_THREADS

/*
**	as_strncmp() - a version of strncmp() that does not call
**		any non async-signal safe functions
*/

static int as_strncmp(s1, s2, n)
char *s1, *s2;
size_t n;
{
	for (; *s1 && *s2 && n != 0; s1++, s2++, n--)
		if ((unsigned char) *s1 != (unsigned char) *s2)
			return((unsigned char) *s1 > (unsigned char) *s2 ? 1 : -1);

	if (!n || (!*s1 && !*s2))
		return(0);
	else
		return(*s1 ? 1 : -1);
}

#endif

