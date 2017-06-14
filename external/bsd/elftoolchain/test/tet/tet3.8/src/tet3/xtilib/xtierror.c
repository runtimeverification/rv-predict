/*
 *      SCCS:  @(#)xtierror.c	1.8 (99/09/03) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1993 X/Open Company Limited
 *
 * All rights reserved.  No part of this source code may be reproduced,
 * stored in a retrieval system, or transmitted, in any form or by any
 * means, electronic, mechanical, photocopying, recording or otherwise,
 * except as stated in the end-user licence agreement, without the prior
 * permission of the copyright owners.
 *
 * X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
 * the UK and other countries.
 */

#ifndef lint
static char sccsid[] = "@(#)xtierror.c	1.8 (99/09/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)xtierror.c	1.8 99/09/03 TETware release 3.8
NAME:		xtierror.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Ltd.
DATE CREATED:	July 1993

DESCRIPTION:
	error message formatting and printing function for XTI/TLI error
	codes.

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., March 1998
	replaced references to sys_errlist[] and sys_nerr with
	a call to strerror()

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
	Protected references to constants defined in XTI but not in TLI
	with #ifdefs so as to enable this file to be compiled on a system
	that only supports TLI.
 

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <sys/types.h>
#include <time.h>
#include <xti.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "ltoa.h"
#include "globals.h"
#include "dtetlib.h"
#include "xtilib_xt.h"


/*
**	tet_xtierror() - XTI/TLI generic error handler
*/

void tet_xtierror(errnum, file, line, s1, s2)
int errnum, line;
char *file, *s1, *s2;
{
	char *s3;
	int save_errno = errno;

	(void) fprintf(stderr, "%s (%s, %d): %s",
		tet_progname, tet_basename(file), line, s1);
	if (s2 && *s2)
		(void) fprintf(stderr, " %s", s2);
	if (errnum > 0) {
		if (errnum == TSYSERR) {
			if ((s3 = strerror(save_errno)) == (char *) 0)
				(void) fprintf(stderr, ", errno = %s",
					tet_errname(save_errno));
			else
				(void) fprintf(stderr, ": %s", s3);
		}
		else 
			(void) fprintf(stderr, ": %s", tet_xterrno2a(errnum));
	}

	(void) putc('\n', stderr);
	(void) fflush(stderr);

	errno = 0;
	t_errno = 0;

}

/*
**	tet_xtifatal - XTI/TLI generic fatal error handler
*/
void tet_xtifatal(errnum, file, line, s1, s2)
int errnum, line;
char *file, *s1, *s2;
{
	tet_xtierror(errnum, file, line, s1, s2);
	exit(1);
}

/*
**	tet_xterrno2a() - convert XTI t_errno to printable form
*/

char *tet_xterrno2a(n)
int n;
{
	static char fmt[] = "Error %d";
	static char buf[sizeof fmt + LNUMSZ];
	
	switch (n) {
	case TBADADDR :
		return ("TBADADDR") ;
	
	case TBADOPT :
		return ("TBADOPT") ;
	
	case TACCES :
		return ("TACCES") ;
	
	case TBADF :
		return ("TBADF") ;
	
	case TNOADDR :
		return ("TNOADDR") ;
	
	case TOUTSTATE :
		return ("TOUTSTATE") ;
	
	case TBADSEQ :
		return ("TBADSEQ") ;
	
	case TSYSERR :
		return ("TSYSERR") ;
	
	case TLOOK :
		return ("TLOOK") ;
	
	case TBADDATA :
		return ("TBADDATA") ;
	
	case TBUFOVFLW :
		return ("TBUFOVFLW") ;
	
	case TFLOW :
		return ("TFLOW") ;
	
	case TNODATA :
		return ("TNODATA") ;
	
	case TNODIS :
		return ("TNODIS") ;
	
	case TNOUDERR :
		return ("TNOUDERR") ;
	
	case TBADFLAG :
		return ("TBADFLAG") ;
	
	case TNOREL :
		return ("TNOREL") ;
	
	case TNOTSUPPORT :
		return ("TNOTSUPPORT") ;
	
	case TSTATECHNG :
		return ("TSATECHNG") ;
	
#ifdef TNOSTRUCTYPE
	case TNOSTRUCTYPE :
		return ("TNOSTRUCTYPE") ;
#endif

#ifdef TBADNAME
	case TBADNAME :
		return ("TBADNAME") ;
#endif

#ifdef TBADQLEN
	case TBADQLEN :
		return ("TBADQLEN") ;
#endif

#ifdef TADDRBUSY
	case TADDRBUSY :
		return ("TADDRBUSY") ;
#endif

	default :
		(void) sprintf(buf, fmt, n);
		return (buf);
	}
}

/*
**	tet_xtev2a() - convert XTI event to printable form
*/

char *tet_xtev2a(n)
int n;

{

	static char fmt[] = "Event %d";
	static char buf[sizeof fmt + LNUMSZ];
	
	switch (n) {
	case T_LISTEN:
		return ("T_LISTEN") ;
	
	case T_CONNECT:
		return ("T_CONNECT") ;
	
	case T_DATA:
		return ("T_DATA") ;
	
	case T_EXDATA:
		return ("T_EXDATA");

	case T_DISCONNECT:
		return ("T_DISCONNECT") ;
	
	case T_UDERR:
		return ("T_UDERR");
	
	case T_ORDREL:
		return ("T_ORDREL") ;
	
	case T_GODATA:
		return ("T_GODATA");

#ifdef T_GOEXDATA
	case T_GOEXDATA:
		return ("T_GOEXDATA");
#endif

	default :
		(void) sprintf(buf, fmt, n);
		return (buf);
	
	}
}

