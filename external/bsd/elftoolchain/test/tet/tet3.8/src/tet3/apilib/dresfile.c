/*
 *	SCCS: @(#)dresfile.c	1.35 (04/11/04)
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
static char sccsid[] = "@(#)dresfile.c	1.35 (04/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)dresfile.c	1.35 04/11/04
NAME:		'C' API results file functions
PRODUCT:	TETware
AUTHOR:		Geoff Clare, UniSoft Ltd.
DATE CREATED:	25 July 1990
SYNOPSIS:

	void tet_infoline(char *data);
	int  tet_minfoline(char **lines, int nlines);
	int  tet_printf(char *, ...);
	void tet_result(int result);
	void tet_setcontext(void);
	void tet_setblock(void);
	int  tet_vprintf(char *, va_list);

	void tet_error(int errno_val, char *msg);
	void tet_merror(int errno_val, char **msgs, int nmsgs);
	long tet_context;
	long tet_block;

DESCRIPTION:

	Tet_minfoline() outputs the specified lines of data to the
	execution results file, prefixed by the current context, block
	and sequence numbers.  The data should not contain any newline
	characters.  Tet_minfoline() returns 0 on success, -1 on error.

	Tet_infoline(data) is equivalent to tet_minfoline(&data, 1),
	except that it exits on error instead of returning an error
	indication.

	Tet_printf() and tet_vprintf() are equivalent to printf() and
	vprintf(), with the output written as infolines in the journal
	file.  The output can contain newline characters.

	Tet_result() specifies the result code which is to be entered
	in the execution results file for the current test purpose.
	It stores the result in a temporary file which is later read
	by tet_tpend().

	Tet_setcontext() sets the current context to the current
	process ID and (in the non-thread API only) resets the block and
	sequence numbers to 1.

	Tet_setblock() increments the current block number and
	resets the sequence number to 1.

	Tet_error() and tet_merror() are not part of the API,
	but are made public to support "other language" APIs.
	They are used by API functions to report errors to stderr and
	the results file.

	Tet_context and tet_block are not part of the API,
	but are made public to support "other language" APIs.
	They are used by API functions to access the current context and
	block numbers.

	NOTE:
	Functions and data items that should be included in
	TCMs and child processes may appear in this file.
	Fucntions and data items that should only be included in TCMs
	(and not child processes) should not appear in this file.
	


MODIFICATIONS:
	
	June 1992
	DTET development - this file is derived from TET release 1.10

	Andrew Dingwall, UniSoft Ltd., October 1992
	Moved non-API functions for IC/TP start/end to tcm/ictp.c
	because these are specific to parent TCMs and do different things
	in MTCM and STCMs.

	All vestages of the local execution results file and temporary
	execution results file removed - in the DTET, all the processing
	associated with these files is done by XRESD.

	Denis McConalogue, UniSoft Limited, August 1993
	changed dtet to tet2 in #includes

	Andrew Dingwall, UniSoft Ltd., February 1994
	save and restore tet_xderrno in tet_error() so as to allow
	recursive calls from dtcmerror() in tcmfuncs.c

	Andrew Dingwall, UniSoft Ltd, July 1995
	if communication with XRESD fails in tet_infoline(), append the
	original infoline text to the error message

	Geoff Clare, UniSoft Ltd, July-August 1996
	Changes for TETWare.

	Geoff Clare, UniSoft Ltd, Sept 1996
	Changes for TETWare-Lite.

	Geoff Clare, UniSoft Ltd, Oct 1996
	Added check on tet_thistest in tet_result().
	Added ic_num argument to tet_tpstart().

	Andrew Dingwall, UniSoft Ltd., October 1996
	removed tet_putenv() - duplicates function of the same name
	in dtet2lib

	Andrew Dingwall, UniSoft Ltd., May 1997
	port to Windows 95

	Andrew Dingwall, UniSoft Ltd., June 1997
	added tet_merror() to support atomic message processing
	in tet_syncreport()

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
	Functions that should only be included in a TCM moved from
	here to ictp.c.
 
	Andrew Dingwall, UniSoft Ltd., July 1999
	added support for 'other language' APIs

	Andrew Dingwall, UniSoft Ltd., October 1999
	added support for strict POSIX threads

	Andrew Dingwall, The Open Group, January 2002
	Fixed a problem in tet_vprintf() in which a va_list object was
	being used more than once.

	Geoff Clare, The Open Group, November 2004
	Avoid bogus "unexpected EOF" warning message in tet_vprintf()
	when va_copy is not defined and the formatted infoline is
	empty (i.e. nothing was written to the temporary file).

************************************************************************/

#include <stdlib.h>
#if defined (__STDC__) || defined (_WIN32)
#  include <stdarg.h>
#else
#  include <varargs.h>
#endif
#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <process.h>
#  include <io.h>
#else		/* -END-WIN32-CUT- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include <errno.h>
#include <string.h>
#include <ctype.h>
#include <time.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "dtthr.h"
#include "error.h"
#include "ltoa.h"
#include "globals.h"
#include "servlib.h"
#include "dtetlib.h"
#include "tet_api.h"
#include "tet_jrnl.h"
#include "apilib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;
#endif

#define MODE666 (mode_t) \
	(S_IRUSR|S_IWUSR|S_IRGRP|S_IWGRP|S_IROTH|S_IWOTH)


/* static function declarations */
static void tet_merr_stdchan PROTOLIST((int, char **, int));
static void tet_merr_stderr PROTOLIST((int, char **, int));
static void tet_merr_sc2 PROTOLIST((int, char *, char *));
static void tet_merr_sc3 PROTOLIST((int, char *, char *));
#ifdef TET_STRICT_POSIX_THREADS
   static void write2stderr PROTOLIST((char *));
#endif


TET_IMPORT int	tet_combined_ok = 0; /* true if OK to write to the xres file */
TET_IMPORT long	tet_activity = -1;
TET_IMPORT long	tet_context = 0;
#ifdef TET_LITE	/* -LITE-CUT-LINE- */
TET_IMPORT FILE *tet_resfp;
TET_IMPORT FILE *tet_tmpresfp;
#endif		/* -LITE-CUT-LINE- */
#ifndef TET_THREADS
long	tet_block = 0;
long	tet_sequence = 0;
#else /* TET_THREADS */

TET_IMPORT long tet_next_block;
TET_IMPORT tet_thread_key_t	tet_block_key;
TET_IMPORT tet_thread_key_t	tet_sequence_key;

TET_IMPORT long *tet_thr_block()
{
	/* find tet_block address for this thread */

	void *rtval;

#  ifdef TET_STRICT_POSIX_THREADS
	static long child_tet_block;

	if (IS_CHILD_OF_MULTITHREAD_PARENT)
		return(&child_tet_block);
#  endif

	rtval = 0;
	TET_THR_GETSPECIFIC(tet_block_key, &rtval);
	if (rtval == 0)
	{
		/* No tet_block has been set up for this thread - probably
		   because it was not created with tet_thr_create().
		   Try and allocate a new tet_block. */

		rtval = malloc(sizeof(long));
		TET_THR_SETSPECIFIC(tet_block_key, rtval);
		rtval = 0;
		TET_THR_GETSPECIFIC(tet_block_key, &rtval);
		if (rtval == 0)
			fatal(0, "could not set up tet_block for new thread in tet_thr_block", (char *)0);
		*((long *)rtval) = 0;
	}

	return (long *)rtval;
}

TET_IMPORT long *tet_thr_sequence()
{
	/* find tet_sequence address for this thread */

	void *rtval;

#  ifdef TET_STRICT_POSIX_THREADS
	static long child_tet_sequence;

	if (IS_CHILD_OF_MULTITHREAD_PARENT)
		return(&child_tet_sequence);
#  endif

	rtval = 0;
	TET_THR_GETSPECIFIC(tet_sequence_key, &rtval);
	if (rtval == 0)
	{
		/* No tet_sequence has been set up for this thread - probably
		   because it was not created with tet_thr_create().
		   Try and allocate a new tet_sequence. */

		rtval = malloc(sizeof(long));
		TET_THR_SETSPECIFIC(tet_sequence_key, rtval);
		rtval = 0;
		TET_THR_GETSPECIFIC(tet_sequence_key, &rtval);
		if (rtval == 0)
			fatal(0, "could not set up tet_sequence for new thread in tet_thr_sequence", (char *)0);
		*((long *)rtval) = 0;
	}

	return (long *)rtval;
}
#endif /* TET_THREADS */

static int
output(lineptrs, nlines)
char **lineptrs;
int nlines;
{
	/* For TETware-Lite all execution results file output comes
	   through here.  For non-Lite only infolines and TCM errors. */

#ifndef TET_LITE /* -START-LITE-CUT- */

	if (tet_xdxresv(tet_xrid, lineptrs, nlines) < 0) {
		switch (tet_xderrno) {
		case ER_INVAL:
		case ER_ERR:
			break;
		default:
			tet_combined_ok = 0;
			break;
		}
		tet_errno = -tet_xderrno;
		return -1;
	}

#else /* -END-LITE-CUT- */

	size_t len;

	if (tet_resfp == NULL)
	{
		/* assume this is an exec'ed program - pick up result
		   file path from environment */
		
		char *cp = getenv("TET_RESFILE");
		if (cp == NULL || *cp == '\0')
		{
			tet_combined_ok = 0;
			fatal(0, "TET_RESFILE not set in environment",
				(char *) 0);
		}

		tet_resfp = fopen(cp, "a");
		if (tet_resfp == NULL)
		{
			tet_combined_ok = 0;
			error(errno,
			    "could not open results file for appending: ", cp);
			tet_errno = TET_ER_ERR;
			return -1;
		}
		tet_combined_ok = 1;
	}

	while (nlines-- > 0)
	{
		len = strlen(*lineptrs);
		if (fwrite((void *)*lineptrs, (size_t)1, len, tet_resfp) != len ||
			putc('\n', tet_resfp) == EOF)
		{
			tet_combined_ok = 0;
			error(errno, "error writing to results file",
				(char *) 0);
			tet_errno = TET_ER_ERR;
			return -1;
		}
		lineptrs++;
	}

	if (fflush(tet_resfp) != 0)
	{
		tet_combined_ok = 0;
		error(errno, "error writing to results file", (char *)0);
		tet_errno = TET_ER_ERR;
		return -1;
	}
#endif /* -LITE-CUT-LINE- */

	return 0;
}

/*
**	tet_infoline() - send a TCM information line to the combined
**		results file
*/

TET_IMPORT void tet_infoline(data)
char *data;
{
	static char fmt[] = "tet_infoline(): can't send info line to XRESD: \"%.128s\"";
	char errbuf[sizeof fmt + 128];

	/*
	** no call to tet_check_api_initialised() here because tet_infoline()
	** is async-signal safe until the call to tet_minfoline()
	*/

	if (data == NULL)
		data = "(null pointer)";

	if (tet_minfoline(&data, 1) != 0)
	{
		(void) sprintf(errbuf, fmt, data);
		tet_error(-tet_errno, errbuf);
		tet_exit(EXIT_FAILURE);
	}
}

/*
**	tet_minfoline() - send multiple TCM information lines to the
**		combined results file
*/

TET_IMPORT int tet_minfoline(lines, nlines)
char **lines;
int nlines;
{
	int lnum, noutlines, bufpos, rval;
	char header[128];
	char linebuf[TET_JNL_LEN];
	char *outbuf = NULL;
	int buflen = 0;
	int *lineoffsets = NULL;
	int offslen = 0;
	char **lineptrs = NULL;

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	if (lines == NULL || nlines < 0)
	{
		tet_errno = TET_ER_INVAL;
		return -1;
	}
	if (nlines == 0) /* nothing to write */
		return 0;

	API_LOCK;

	if (tet_context == 0)
		tet_setcontext();

	/* Assemble buffer containing journal lines to be output */

	noutlines = 0;	/* number of lines to be output */
	bufpos = 0;	/* start position of current line in outbuf */

	for (lnum = 0; lnum < nlines; lnum++)
	{
		int len;

		if (lines[lnum] == NULL)
			continue;

		/* generate the info line preamble and format the line */
		(void) sprintf(header, "%d|%ld %d %03d%05ld %ld %ld|", 
			TET_JNL_TC_INFO, tet_activity, tet_thistest, 
			tet_mysysid, tet_context, tet_block, tet_sequence++);
		tet_msgform(header, lines[lnum], linebuf);

		len = strlen(linebuf) + 1;

		/* add the line to output buffer */
		if (BUFCHK((char **) &outbuf, &buflen, buflen+len) < 0 ||
		    BUFCHK((char **) &lineoffsets, &offslen, offslen+(int)sizeof(*lineoffsets)) < 0)
		{
			if (outbuf != NULL)
			{
				TRACE2(tet_Tbuf, 6, "free outbuf = %s",
					tet_i2x(outbuf));
				free((void *)outbuf);
			}
			if (lineoffsets != NULL)
			{
				TRACE2(tet_Tbuf, 6, "free lineoffsets = %s",
					tet_i2x(lineoffsets));
				free((void *)lineoffsets);
			}
			tet_errno = TET_ER_ERR;
			API_UNLOCK;
			return(-1);
		}
		(void) strcpy(&outbuf[bufpos], linebuf);

		/* remember offset from start of outbuf */
		/* (can't save pointer, as outbuf may move when grown) */
		lineoffsets[noutlines] = bufpos;

		bufpos += len;
		noutlines++;
	}

	if (noutlines == 0) /* nothing to write */
	{
		TRACE1(tet_Ttcm, 4, "line pointers passed to tet_minfoline() were all NULL");
		API_UNLOCK;
		return 0;
	}

	errno = 0;
	if ((lineptrs = (char **) malloc((size_t)(noutlines * sizeof(*lineptrs)))) == NULL)
	{
		tet_error(errno, "can't allocate lineptrs in tet_minfoline()");
		TRACE2(tet_Tbuf, 6, "free outbuf = %s", tet_i2x(outbuf));
		free((void *)outbuf);
		TRACE2(tet_Tbuf, 6, "free lineoffsets = %s",
			tet_i2x(lineoffsets));
		free((void *)lineoffsets);
		tet_errno = TET_ER_ERR;
		API_UNLOCK;
		return -1;
	}
	TRACE2(tet_Tbuf, 6, "allocate lineptrs = %s", tet_i2x(lineptrs));

	/* Set up line pointers into output buffer */
	for (lnum = 0; lnum < noutlines; lnum++)
		lineptrs[lnum] = outbuf + lineoffsets[lnum];

	TRACE2(tet_Tbuf, 6, "free lineoffsets = %s", tet_i2x(lineoffsets));
	free((void *)lineoffsets);

	/* output the lines to the results file */
	rval = output(lineptrs, noutlines);

	TRACE2(tet_Tbuf, 6, "free outbuf = %s", tet_i2x(outbuf));
	free((void *)outbuf);
	TRACE2(tet_Tbuf, 6, "free lineptrs = %s", tet_i2x(lineptrs));
	free((void *)lineptrs);

	API_UNLOCK;
	return rval;
}

/*
**	tet_vprintf() - write formatted information lines to the
**		combined results file
*/

TET_IMPORT int tet_vprintf(format, ap1)
char *format;
va_list ap1;
{
	int lnum, noutlines, outpos, rval;
	char defaultbuf[16*1024];
	char linebuf[TET_JNL_LEN];
	char *outbuf = NULL;
	int outbuflen = 0;
	char *inbuf, *inptr;
	int inbuflen = 0;
	int *lineoffsets = NULL;
	int offslen = 0;
	char **lineptrs = NULL;
	FILE *fp = (FILE *) 0;
#ifdef va_copy
	va_list ap2;
#  ifdef _WIN32	/* -START-WIN32-CUT- */
	static char devnull[] = "nul";
#  else		/* -END-WIN32-CUT- */
	static char devnull[] = "/dev/null";
#  endif	/* -WIN32-CUT-LINE- */
#endif
	char *tfname;
	int err, nchars;
	static char fmt1[] = "can't open %.*s in tet_vprintf()";
	static char fmt2[] = "write error on %.*s in tet_vprintf()";
	static char fmt3[] = "read error on %.*s in tet_vprintf()";
	static char fmt4[] = "unexpected EOF on %.*s in tet_vprintf()";
	static char fmt5[] = "format file %.*s is empty in tet_vprintf()";
	/* NOTE: this only works because fmt5 is the longest of the formats */
	char errmsg[sizeof fmt5 + MAXPATH];

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	if (format == NULL)
	{
		tet_errno = TET_ER_INVAL;
		return -1;
	}

	API_LOCK;

	if (tet_context == 0)
		tet_setcontext();

	/*
	** format the line into the inbuf
	**
	** we used to adopt this strategy:
	**	print the line to /dev/null to find out how big it is
	**	make sure that the receiving buffer is big enough
	**	format the line using vsprintf()
	**
	** but that uses ap twice, which is not allowed
	**
	** C99 defines a macro va_copy which can be used to take a copy
	** of a va_list object
	** so now we do this:
	**	if va_copy is available:
	**		use that to take a copy of ap and hand the copy off
	**		to vsprintf(), using the original strategy;
	** 	otherwise:
	**		print the line to a temporary file and just read that
	**		back in to the inbuf
	*/

#ifdef va_copy
	/* take a copy of the va_list object */
	va_copy(ap2, ap1);
#endif

	/* determine the name of the file to write to */
#ifdef va_copy
	tfname = devnull;
#else
	tfname = tet_mktfname("tetprf");
#endif

	/* First find out how big a buffer we need for the formatted output */
	inbuf = defaultbuf;
	inbuflen = (int) sizeof defaultbuf;
	if (!tfname)
	{
		; /* nothing */
	}
	else if ((fp = fopen(tfname, "w+")) == NULL)
	{
		sprintf(errmsg, fmt1, MAXPATH, tfname);
		tet_error(errno, errmsg);
	}
	else
	{
		if (
			(nchars = vfprintf(fp, format, ap1)) < 0 ||
			fflush(fp) == EOF
		) {
			sprintf(errmsg, fmt2, MAXPATH, tfname);
			tet_error(errno, errmsg);
		}

		if (nchars >= (int) sizeof defaultbuf)
		{
			/* need a larger buffer */
			inbuflen = nchars + 1;
			errno = 0;
			if ((inbuf = malloc((size_t) inbuflen)) == NULL)
			{
				tet_error(errno, "can't allocate inbuf in tet_vprintf()");
				tet_errno = TET_ER_ERR;
				fclose(fp);
#ifndef va_copy
				(void) UNLINK(tfname);
#endif
				API_UNLOCK;
				return -1;
			}
			TRACE2(tet_Tbuf, 6, "allocate inbuf = %s",
				tet_i2x(inbuf));
		}
	}

#ifdef va_copy
	/* use vsprintf() to do the formatting */
	nchars = vsprintf(inbuf, format, ap2);
#else
	/* read the formatted line into inbuf */
	if (fp && nchars <= 0) {
		/* nothing was written to the temporary file */
		*inbuf = '\0';
	}
	else if (fp) {
		/* normal processing */
		rewind(fp);
		nchars = (int) fread((void *) inbuf, 1,
			(size_t) (inbuflen - 1), fp);
		if (nchars == 0) {
			if (ferror(fp)) {
				err = errno;
				sprintf(errmsg, fmt3, MAXPATH, tfname);
			}
			else if (feof(fp)) {
				err = 0;
				sprintf(errmsg, fmt4, MAXPATH, tfname);
			}
			else {
				err = 0;
				sprintf(errmsg, fmt5, MAXPATH, tfname);
			}
			tet_error(err, errmsg);
		}
		*(inbuf + nchars) = '\0';
	}
	else {
		/* if tfname could not be opened */
		nchars = vsprintf(inbuf, format, ap1);
	}
#endif

	/*
	** close the format file and unlink a temporary file
	** that was created earlier
	*/
	if (fp) {
		fclose(fp);
#ifndef va_copy
		if (tfname)
			(void) UNLINK(tfname);
#endif
	}

#ifndef va_copy
	/* free the tfname */
	TRACE2(tet_Tbuf, 6, "free tfname = %s", tet_i2x(tfname));
	if (tfname)
		free((void *) tfname);
#endif


	/*
	** ensure that we haven't overflowed inbuf -
	** this could happen if the fopen(tfname) failed above
	** (there's no point trying to continue with corrupted memory)
	*/
	if (nchars >= inbuflen)
		fatal(0, "vsprintf() overflowed buffer in tet_vprintf",
			(char *) 0);


	/* Assemble buffer containing journal lines to be output */

	noutlines = 0;	/* number of lines to be output */
	outpos = 0;	/* start position of current line in outbuf */

	inptr = inbuf;
	while (*inptr != '\0' || noutlines == 0)
	{
		char *endp;
		int len, prelen;

		/* find length of next line
		   (can be zero if vsprintf produced no output) */
		endp = strchr(inptr, '\n');
		if (endp == NULL)
			len = strlen(inptr);
		else
			len = endp - inptr;

		/* generate the info line preamble */
		(void) sprintf(linebuf, "%d|%ld %d %03d%05ld %ld %ld|", 
			TET_JNL_TC_INFO, tet_activity, tet_thistest, 
			tet_mysysid, tet_context, tet_block, tet_sequence++);

		/*
		 * If line is too long find where to break it (preferably
		 * at whitespace, although note that no whitespace is
		 * removed, in case the presence/absence of a whitespace
		 * character in the output is important).
		 */
		prelen = strlen(linebuf);
		if (len + prelen >= sizeof(linebuf))
		{
			len = sizeof(linebuf) - prelen - 1;
			for (endp = &inptr[len]; endp > inptr; endp--)
			{
				if (isspace((int)(unsigned char)*endp))
					break;
			}
			if (endp > inptr) /* whitespace found */
				len = endp - inptr;
		}

		/* assemble the complete line and add it to output buffer */

		(void) strncat(linebuf, inptr, (size_t)len);
		if (*(inptr += len) == '\n')
			inptr++; /* now points to start of next line */
		len = strlen(linebuf) + 1; /* length including the null */
		if (BUFCHK((char **) &outbuf, &outbuflen, outbuflen+len) < 0 ||
		    BUFCHK((char **) &lineoffsets, &offslen, offslen+(int)sizeof(*lineoffsets)) < 0)
		{
			if (inbuf != defaultbuf)
			{
				TRACE2(tet_Tbuf, 6, "free inbuf = %s",
					tet_i2x(inbuf));
				free((void *)inbuf);
			}
			if (outbuf != NULL)
			{
				TRACE2(tet_Tbuf, 6, "free outbuf = %s",
					tet_i2x(outbuf));
				free((void *)outbuf);
			}
			if (lineoffsets != NULL)
			{
				TRACE2(tet_Tbuf, 6, "free lineoffsets = %s",
					tet_i2x(lineoffsets));
				free((void *)lineoffsets);
			}
			tet_errno = TET_ER_ERR;
			API_UNLOCK;
			return(-1);
		}
		(void) strcpy(&outbuf[outpos], linebuf);

		/* remember offset from start of outbuf */
		/* (can't save pointer, as outbuf may move when grown) */
		lineoffsets[noutlines] = outpos;

		outpos += len;
		noutlines++;
	}

	if (inbuf != defaultbuf)
	{
		TRACE2(tet_Tbuf, 6, "free inbuf = %s", tet_i2x(inbuf));
		free((void *)inbuf);
	}

	errno = 0;
	if ((lineptrs = (char **) malloc((size_t)(noutlines * sizeof(*lineptrs)))) == NULL)
	{
		tet_error(errno, "can't allocate lineptrs in tet_vprintf()");
		TRACE2(tet_Tbuf, 6, "free outbuf = %s", tet_i2x(outbuf));
		free((void *)outbuf);
		TRACE2(tet_Tbuf, 6, "free lineoffsets = %s",
			tet_i2x(lineoffsets));
		free((void *)lineoffsets);
		tet_errno = TET_ER_ERR;
		API_UNLOCK;
		return -1;
	}
	TRACE2(tet_Tbuf, 6, "allocate lineptrs = %s", tet_i2x(lineptrs));

	/* Set up line pointers into output buffer */
	for (lnum = 0; lnum < noutlines; lnum++)
		lineptrs[lnum] = outbuf + lineoffsets[lnum];

	TRACE2(tet_Tbuf, 6, "free lineoffsets = %s", tet_i2x(lineoffsets));
	free((void *)lineoffsets);

	/* output the lines to the results file */
	if (output(lineptrs, noutlines) < 0)
		rval = -1;
	else
		rval = outpos; /* number of bytes written to journal */

	TRACE2(tet_Tbuf, 6, "free outbuf = %s", tet_i2x(outbuf));
	free((void *)outbuf);
	TRACE2(tet_Tbuf, 6, "free lineptrs = %s", tet_i2x(lineptrs));
	free((void *)lineptrs);

	API_UNLOCK;
	return rval;
}

/*
**	tet_printf() - write formatted information lines to the
**		combined results file
*/

#if defined (__STDC__) || defined (_WIN32)
TET_IMPORT int tet_printf(char *format, ...)
{
	int rval;
	va_list ap;

#ifdef TET_STRICT_POSIX_THREADS
	tet_check_api_status(0);
#endif

	va_start(ap, format);
	rval = tet_vprintf(format, ap);
	va_end(ap);

	return rval;
}
#else /* !(__STDC__ || _WIN32) */
TET_IMPORT int tet_printf(va_alist)
va_dcl
{
	char *format;
	int rval;
	va_list ap;

#ifdef TET_STRICT_POSIX_THREADS
	tet_check_api_status(0);
#endif

	va_start(ap);
	format = va_arg(ap, char *);
	rval = tet_vprintf(format, ap);
	va_end(ap);

	return rval;
}
#endif /* !(__STDC__ || _WIN32) */

/*
**	tet_result() - send a test purpose result to XRESD or tmpfile
*/

TET_IMPORT void tet_result(result)
int result;
{
#ifdef TET_LITE	/* -LITE-CUT-LINE- */
	char *resname;
#endif		/* -LITE-CUT-LINE- */
	char errmsg[128];

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	if (tet_thistest == 0)
	{
		(void) sprintf(errmsg,
			"tet_result(%d) called from test case startup or cleanup function",
			result);
		tet_error(0, errmsg);
		return;
	}

	API_LOCK;

#ifndef TET_LITE /* -START-LITE-CUT- */

	/* Call generic function to tell xresd the result */
	if (tet_xdresult(tet_xrid, result) < 0)
	{
		switch (tet_xderrno) {
		case ER_INVAL:
		case ER_SYSID:
		case ER_DONE:
		case ER_ERR:
			break;
		default:
			tet_combined_ok = 0;
			break;
		}
		(void) sprintf(errmsg,
			"tet_result(): can't send result %d to XRESD",
			result);
		tet_error(tet_xderrno, errmsg);
		tet_exit(EXIT_FAILURE);
	}

#else /* -END-LITE-CUT- */

	/*
	 * Look up supplied code in results code file to check it's valid.
	 * Write the result code to a temporary result file to be picked
	 * up later by tet_tpend().  This mechanism is used rather than
	 * writing directly to the execution results file to ensure that only
	 * one result code appears there.
	 */
	
	resname = tet_get_code(result, (int *)NULL);
	if (resname == NULL)
	{
		(void) sprintf(errmsg,
			"INVALID result code %d passed to tet_result()",
			result);
		tet_error(0, errmsg);
		result = TET_NORESULT;
	}

	if (tet_tmpresfp == NULL)
	{
		/* assume this is an exec'ed program - pick up temp result
		   file path from environment */
		
		char *cp = getenv("TET_TMPRESFILE");
		if (cp == NULL || *cp == '\0')
			fatal(0, "TET_TMPRESFILE not set in environment",
				(char *)0);

		tet_tmpresfp = fopen(cp, "ab");
		if (tet_tmpresfp == NULL)
			fatal(errno, "could not open temp result file for appending:",
				cp);
	}

	if (fwrite((void *)&result, sizeof(result), (size_t)1, tet_tmpresfp) != 1 ||
	    fflush(tet_tmpresfp) != 0)
		fatal(errno, "write failed on temp result file", (char *)0);

#endif /* -LITE-CUT-LINE- */

	API_UNLOCK;
}

TET_IMPORT void tet_setcontext()
{
	/* Set current context to process ID and */
	/* (non-thread API only) reset block & sequence */

	pid_t pid;

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	API_LOCK;

#ifdef _WIN32	/* -START-WIN32-CUT- */
	pid = (pid_t) ((unsigned) GETPID() % 10000000);
#else		/* -END-WIN32-CUT- */
	pid = GETPID();
#endif		/* -WIN32-CUT-LINE- */

	if (tet_context != (long) pid)
	{
		tet_context = (long) pid;
#ifndef TET_THREADS
		tet_block = 1;
		tet_sequence = 1;
#else /* TET_THREADS */
		/*
		 * In the threads API, all the threads keep their
		 * current block numbers (to ensure different threads
		 * are still distinguishable).
		 * We might as well reset the sequence number for
		 * the calling thread.  To reset the other sequence
		 * numbers would mean keeping a per-thread `last
		 * context number' and noticing the change of context
		 * in tet_thr_sequence().  It is not worth the overhead.
		 */
		tet_sequence = 1;
#endif /* TET_THREADS */
	}

	API_UNLOCK;
}

TET_IMPORT void tet_setblock()
{
	/* Increment current block & reset sequence number within block */

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	API_LOCK;

#ifndef TET_THREADS
	tet_block++;
#else /* TET_THREADS */
	tet_next_block++;
	tet_block = tet_next_block;
#endif /* TET_THREADS */

	tet_sequence = 1;

	API_UNLOCK;
}

/*
**	tet_error(), tet_merror() - print API error messages
**		to the standard channel
**
**	in TETware-Lite the messages are printed to the
**	execution results file
**
**	in Distributed TETware the messages are sent to XRESD for printing
**	using an operation which ensures that messages from other systems
**	don't get mixed up with these messages
**
**	if the operation fails the standard channel is disabled and
**	tet_error() is called recursively to report the failure;
**	then the TCM exits
**
**	when the standard channel is disabled, messages are printed
**	to stderr instead
**
**	errnum may be +ve to report a Unix errno value, or -ve to
**	report a DTET server error reply code or API error code.
**	N.B. This means that to report tet_errno values, they must
**	be negated: tet_error(-tet_errno, msg).
**
**	These functions are async-signal safe when called in a child process
**	whose parent is multi-threaded when in strict POSIX threads mode.
**	Unfortunately, the best we can do under these circumstances is to
**	write the message to stderr.
*/

TET_IMPORT void tet_error(errnum, msg)
int errnum;
char *msg;
{
	tet_merror(errnum, &msg, 1);
}

TET_IMPORT void tet_merror(errnum, msgs, nmsgs)
int errnum, nmsgs;
char **msgs;
{
#ifdef TET_STRICT_POSIX_THREADS
	if (IS_CHILD_OF_MULTITHREAD_PARENT) {
		tet_as_merror(errnum, msgs, nmsgs);
		return;
	}
#endif

	API_LOCK;

	if (tet_combined_ok != 1)
		tet_merr_stderr(errnum, msgs, nmsgs);
	else
		tet_merr_stdchan(errnum, msgs, nmsgs);

	API_UNLOCK;
}

/*
**	tet_merr_stderr() - print API error messages to stderr when 
**		the standard channel is not available
*/

static void tet_merr_stderr(errnum, msgs, nmsgs)
int errnum, nmsgs;
char **msgs;
{
	/* print each message in turn */
	for (; nmsgs > 0; nmsgs--, msgs++) {
		if (!*msgs && !errnum)
			continue;
		(void) fprintf(stderr, "%s: %s",
			tet_basename(tet_pname), *msgs ? *msgs : "(NULL)");
		if (errnum > 0) {
			(void) fprintf(stderr, ", errno = %d (%s)", 
				errnum, tet_errname(errnum));
		}
		else if (errnum < 0) {
			(void) fprintf(stderr, ", reply code = %s",
				tet_ptrepcode(errnum));
		}
		(void) fprintf(stderr, "\n");
		errnum = 0;
	}
	(void) fflush(stderr);
}

/*
**	tet_merr_stdchan() - print API error messages to the standard channel
**
**	note that this function may be called recursively so no static
**	storage allowed here
*/

static void tet_merr_stdchan(errnum, msgs, nmsgs)
int errnum, nmsgs;
char **msgs;
{
	register char **lp, **msgp;
	register int n;
	int errtmp, errors;
	char errbuf[TET_JNL_LEN];
	char **lines;

	/* take a short cut if there is only one message to print */
	if (nmsgs == 1) {
		tet_merr_sc2(errnum, *msgs, errbuf);
		return;
	}

	/*
	** here when multiple messages must be printed -
	** allocate storage for the list of line pointers
	*/
	errors = 0;
	errno = 0;
	if ((lines = (char **) malloc(nmsgs * sizeof *lines)) == (char **) 0) {
		error(errno, "can't allocate memory for error message pointers",
			(char *) 0);
		errors++;
	}
	else
		TRACE2(tet_Tbuf, 6, "allocate error message pointers = %s",
			tet_i2x(lines));
	lp = lines;

	/*
	** format each message line in turn -
	** the error value is only printed on the first line
	*/
	errtmp = errnum;
	for (n = 0, msgp = msgs; n < nmsgs; n++, msgp++) {
		if (!*msgp && !errtmp)
			continue;
		tet_merr_sc3(errtmp, *msgp, errbuf);
		if (lines && (*lp++ = tet_strstore(errbuf)) == (char *) 0) {
			errors++;
			break;
		}
		errtmp = 0;
	}

	/*
	** if there were no memory allocation errors, 
	** output the lines all at once
	*/
	if (lines && !errors)
		tet_routput(lines, nmsgs);

	/* then free all the storage allocated here */
	if (lines) {
		for (lp = lines; lp < lines + nmsgs; lp++)
			if (*lp) {
				TRACE2(tet_Tbuf, 6, "free mx_line = %s",
					tet_i2x(*lp));
				free(*lp);
			}
		TRACE2(tet_Tbuf, 6, "free mx_lines = %s", tet_i2x(lines));
		free((char *) lines);
	}

	/*
	** if there were memory allocation errors, it's just possible
	** that we have actually run out of memory
	**
	** in this case it's possible that we can still print each line
	** individually
	*/
	if (errors) {
		errtmp = errnum;
		for (n = 0, msgp = msgs; n < nmsgs; n++, msgp++) {
			if (!*msgp && !errtmp)
				continue;
			tet_merr_sc2(errtmp, *msgp, errbuf);
			errtmp = 0;
		}
	}
}

/*
**	tet_merr_sc2() - extend the tet_merr_stdchan() processing
**
**	format a single message and output it to the standard channel
*/

static void tet_merr_sc2(errnum, msg, outbuf)
int errnum;
char *msg, *outbuf;
{
	tet_merr_sc3(errnum, msg, outbuf);
	tet_routput(&outbuf, 1);
}

/*
**	tet_merr_sc3() - extend the tet_merr_stdchan() processing
**		some more
**
**	format a single message line
*/

static void tet_merr_sc3(errnum, msg, outbuf)
int errnum;
char *msg, *outbuf;
{
	register char *p;
	char header[128];

	/*
	** format error message for the results file -
	** put errno first so as to avoid it being lost by truncation
	*/
	p = header;
	(void) sprintf(p, "%d|%ld|system %d", TET_JNL_TCM_INFO,
		tet_activity, tet_mysysid);
	p += strlen(p);
	if (errnum > 0)
		(void) sprintf(p, ", errno = %d (%s)",
			errnum, tet_errname(errnum));
	else if (errnum < 0)
		(void) sprintf(p, ", reply code = %s", 
			tet_ptrepcode(errnum));
	p += strlen(p);
	(void) sprintf(p, ": ");

	/* Check the message format and write it into outbuf */
	tet_msgform(header, msg ? msg : "(NULL)", outbuf);
}

/*
**	tet_routput() - "reliable" call to output()
**		send error message lines to the standard channel
**
**	if this operation fails, report the error and exit
*/

TET_IMPORT void tet_routput(lines, nlines)
char **lines;
int nlines;
{
#ifdef TET_LITE	/* -LITE-CUT-LINE- */

#  define ERRMSG \
     "unable to write the following message to the tet_xres file"
#  define ERRNUM	errno

#else		/* -START-LITE-CUT- */

	int save_xderrno = tet_xderrno;

#  define ERRMSG \
     "unable to send the following message to XRESD"
#  define ERRNUM	tet_xderrno

#endif		/* -END-LITE-CUT- */

	/*
	** send the message to the tet_xres file (in TETware-LITE)
	** or to XRESD (in Distributed TETware)
	**
	** in Distributed TETware the previous value of tet_xderrno is saved
	** and restored afterwards because a call to tet_error() might be
	** reporting on a previous XRESD operation
	*/
	if (output(lines, nlines) < 0) {
		/* no longer OK to write to the standard channel */
		tet_combined_ok = 0;
		tet_error(ERRNUM, ERRMSG);
		tet_merror(0, lines, nlines);
		tet_exit(EXIT_FAILURE);
	}

#ifndef TET_LITE /* -START-LITE-CUT- */
	tet_xderrno = save_xderrno;
#endif /* -END-LITE-CUT- */

}


/*
**	tet_msgform() - format a TCM journal line -
**		translate newlines to tabs,
**		make sure that the line does not exceed 512 bytes
**		as required by the spec
**
**	on return, the formatted line is stored in outbuf
*/

TET_IMPORT void tet_msgform(header, data, outbuf)
char *header, *data, *outbuf;
{
	register char *p1, *p2;
	static char fmt[] =
		"warning: results file line truncated - prefix: %.*s";
	char errmsg[128];

	p2 = outbuf;

	/* copy over the header preamble */
	for (p1 = header; *p1 && p2 < &outbuf[TET_JNL_LEN - 1]; p1++, p2++)
		*p2 = *p1;

	/* copy over the data, performing translations */
	for (p1 = data; *p1 && p2 < &outbuf[TET_JNL_LEN - 1]; p1++, p2++)
		switch (*p1) {
		case '\n':
			*p2 = '\t';
			break;
		default:
			*p2 = *p1;
			break;
		}

	/* terminate the line, removing trailing while space */
	do {
		*p2-- = '\0';
	} while (isspace((int)(unsigned char)*p2));

	/* see if the line was truncated */
	if (*p1) {
		(void) sprintf(errmsg, fmt, (int) (sizeof errmsg - sizeof fmt),
			header);
		tet_error(0, errmsg);
	}
}

#ifdef TET_STRICT_POSIX_THREADS

/*
**	tet_as_merror() - a version of tet_merror() that only calls
**		async-signal safe functions
**
**	NOTE: tet_errname() and tet_ptrepcode() can call sprintf() which
**	isn't guaranteed to be async-signal safe.
**	But sprintf() is only called if errnum isn't one of the known values.
**	It's probably best to leave these calls in because in the normal way
**	sprintf() won't get called and the information printed is rather useful.
*/

TET_IMPORT void tet_as_merror(errnum, msgs, nmsgs)
int errnum, nmsgs;
char **msgs;
{
	/* print each message in turn */
	for (; nmsgs > 0; nmsgs--, msgs++) {
		if (!*msgs && !errnum)
			continue;
		write2stderr(tet_basename(tet_pname));
		write2stderr(": ");
		write2stderr(*msgs ? *msgs : "(NULL)");
		if (errnum > 0) {
			write2stderr(", errno = ");
			write2stderr(tet_i2a(errnum));
			write2stderr(" (");
			write2stderr(tet_errname(errnum));
			write2stderr(")");
		}
		else if (errnum < 0) {
			write2stderr(", reply code = ");
			write2stderr(tet_ptrepcode(errnum));
		}
		write2stderr("\n");
		errnum = 0;
	}
}

/*
**	write2stderr() - async-signal safe function to write a string
**		on the stderr
*/

static void write2stderr(s)
char *s;
{
	char *p;

	for (p = s; *p; p++)
		;

	(void) write(2, (void *) s, (size_t) (p - s));
}

#endif

#ifdef TET_LITE	/* -LITE-CUT-LINE- */

/*
**	get_code() - look up result code, return name if found, otherwise NULL.
**
**	If abortflag is not NULL then set (*abortflag) to true if
**	corresponding action is to abort
*/

TET_IMPORT char *tet_get_code(result, abortflag)
int result;
int *abortflag;
{
	char *fname;
	static int read_done = 0;

	if (!read_done)
	{
		/* file name is specified by TET_CODE communication variable */
		fname = getenv("TET_CODE");
		if (fname != NULL && *fname != '\0')
			(void) tet_readrescodes(fname);
		read_done++;
	}

	return tet_getresname(result, abortflag);
}

#endif		/* -LITE-CUT-LINE- */

