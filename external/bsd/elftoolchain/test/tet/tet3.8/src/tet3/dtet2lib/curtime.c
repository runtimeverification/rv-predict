/*
 *      SCCS:  @(#)curtime.c	1.3 (05/12/01) 
 *
 * (C) Copyright 2005 The Open Group
 *
 * All rights reserved.  No part of this source code may be reproduced,
 * stored in a retrieval system, or transmitted, in any form or by any
 * means, electronic, mechanical, photocopying, recording or otherwise,
 * except as stated in the end-user licence agreement, without the prior
 * permission of the copyright owners.
 *
 * The Open Group and Boundaryless Information Flow are trademarks and
 * UNIX is a registered trademark of The Open Group in the United States
 * and other countries.
 */

#ifndef lint
static char sccsid[] = "@(#)curtime.c	1.3 (05/12/01) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)curtime.c	1.3 05/12/01 TETware release 3.8
NAME:		curtime.c
PRODUCT:	TETware
AUTHOR:		Geoff Clare, The Open Group
DATE CREATED:	June 2005

DESCRIPTION:
	function to generate timestamps

MODIFICATIONS:
        David Scholefield, The Open Group, November 2005
        Added Win32 support.

************************************************************************/

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <time.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <windows.h>
#else		/* -END-WIN32-CUT- */
#  ifndef USE_GETTIMEOFDAY
#    if (defined(_XOPEN_SOURCE) && defined(_XOPEN_SOURCE_EXTENDED)) || \
        (defined(_XOPEN_SOURCE) && _XOPEN_SOURCE+0 >= 500)
#      define USE_GETTIMEOFDAY
#    endif
#  endif
#  ifdef USE_GETTIMEOFDAY
#    include <sys/time.h>
#  else
#    include <sys/types.h>
#    include <unistd.h>
#  endif
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtetlib.h"
#include "error.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

static int fullcurtime PROTOLIST((char *, unsigned));
static int nomilli_full PROTOLIST((char *));

/*
**	tet_curtime() - generate a timestamp in short or full format
**
**	return 0 if successful or -1 on error
**
**	A timestamp for the current (date and) time is generated in one
**	of the following formats:
**
**		Short: HH:MM:SS
**		Full:  YYYY-MM-DDTHH:MM:SS[.sss]
**
**	where the T in the full format is a literal letter T, and the
**	millisecond part is only included on systems that support a
**	standard mechanism to obtain it.
**
**	The supplied buffer must be big enough to hold at least the
**	short format.  If the full format is selected and the buffer
**	is too small for the whole thing, only the last part of it
**	(excluding milliseconds) is written to the buffer.
*/
TET_IMPORT int tet_curtime(buf, bufsize, fullflag)
char *buf;        /* destination buffer for the timestamp */
unsigned bufsize; /* size of buf */
int fullflag;     /* true if the full format is to be used */
{
	time_t t;
	struct tm *tp;

	if (bufsize < sizeof "HH:MM:SS")
	{
		error(0, "buffer too small in tet_curtime()", (char *) 0);
		return -1;
	}

	if (fullflag)
		return fullcurtime(buf, bufsize);

	(void) time(&t);
	tp = localtime(&t);
	if (tp == NULL)
		return -1;

	(void) sprintf(buf, "%02d:%02d:%02d", tp->tm_hour,
		tp->tm_min, tp->tm_sec);

	return 0;
}

#ifdef PROTOTYPES
static int fullcurtime(char *buf, unsigned bufsize)
#else
static int fullcurtime(buf, bufsize)
char *buf;
unsigned bufsize;
#endif
{
	/* generate full timestamp, with milliseconds if possible */

#ifdef _WIN32	/* -START-WIN32-CUT- */
        SYSTEMTIME st;  /* Windows Time Structure */
#endif		/* -END-WIN32-CUT- */
	size_t len;
	char localbuf[sizeof "YYYY-MM-DDTHH:MM:SS.sss"];

	if (bufsize <= sizeof "YYYY-MM-DDTHH:MM:SS")
	{
		/* buffer is too small to include milliseconds */
		if (nomilli_full(localbuf) == -1)
			return -1;
	}
	else

#ifdef _WIN32	/* -START-WIN32-CUT- */
	{
		/* Retrieve the current system date and time.  */
		GetSystemTime(&st);
		/* Fill out localbuf buffer */
		(void) sprintf(localbuf, "%04d-%02d-%02dT%02d:%02d:%02d.%03ld",
			    st.wYear, st.wMonth, st.wDay,
			    st.wHour, st.wMinute, st.wSecond,
			    (long)(st.wMilliseconds));
	}
#else		/* -END-WIN32-CUT- */

#  if defined(USE_GETTIMEOFDAY)

	/* Use gettimeofday() to obtain subsecond resolution time */
	{
		struct tm *tp;
		struct timeval tval;

		(void) gettimeofday(&tval, (void *)0);
		tp = localtime(&tval.tv_sec);
		if (tp == NULL)
			return -1;

		(void) sprintf(localbuf, "%04d-%02d-%02dT%02d:%02d:%02d.%03ld",
			tp->tm_year+1900, tp->tm_mon+1, tp->tm_mday,
			tp->tm_hour, tp->tm_min, tp->tm_sec,
			(long)(tval.tv_usec/1000)%1000);
	}

#  elif (_POSIX_C_SOURCE >= 200112 && defined(_POSIX_TIMERS) && \
	 _POSIX_TIMERS+0 != -1) || \
	(_POSIX_C_SOURCE >= 199309 && _POSIX_C_SOURCE <= 199506)

	/* Use clock_gettime(), if available, for subsecond resolution time */
	{
		struct timespec tspec;

		if (sysconf(_SC_TIMERS) == -1 ||
		    clock_gettime(CLOCK_REALTIME, &tspec) == -1)
		{
			/* fall back to no-millisecond version */
			if (nomilli_full(localbuf) == -1)
				return -1;
		}
		else
		{
			struct tm *tp;

			tp = localtime(&tspec.tv_sec);
			if (tp == NULL)
				return -1;

			(void) sprintf(localbuf,
				"%04d-%02d-%02dT%02d:%02d:%02d.%03ld",
				tp->tm_year+1900, tp->tm_mon+1, tp->tm_mday,
				tp->tm_hour, tp->tm_min, tp->tm_sec,
				(long)(tspec.tv_nsec/1000000)%1000);
		}
	}

#  else

	/* no subsecond resolution time available */
	{
		if (nomilli_full(localbuf) == -1)
			return -1;
	}

#  endif

#endif		/* -WIN32-CUT-LINE- */

	/* The desired timestamp is now in localbuf */
	/* Copy as much of (the end of) it as will fit into buf */

	if (bufsize >= sizeof localbuf || bufsize > (len = strlen(localbuf)))
	{
		(void) strcpy(buf, localbuf);
	}
	else
	{
		(void) strcpy(buf, &localbuf[len + 1 - bufsize]);
	}

	return 0;
}

#ifdef PROTOTYPES
static int nomilli_full(char *buf)
#else
static int nomilli_full(buf)
char *buf;
#endif
{
	/* generate full timestamp without milliseconds part */

	time_t t;
	struct tm *tp;

	(void) time(&t);
	tp = localtime(&t);
	if (tp == NULL)
		return -1;

	(void) sprintf(buf, "%04d-%02d-%02dT%02d:%02d:%02d",
		tp->tm_year+1900, tp->tm_mon+1, tp->tm_mday,
		tp->tm_hour, tp->tm_min, tp->tm_sec);

	return 0;
}

