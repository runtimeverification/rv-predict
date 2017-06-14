/*
 *	SCCS: @(#)ftype.c	1.1 (03/03/26)
 *
 *	The Open Group, Reading, England
 *
 * Copyright (c) 2003 The Open Group
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
static char sccsid[] = "@(#)ftype.c	1.1 (03/03/26) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)ftype.c	1.1 03/03/26 TETware release 3.8
NAME:		ftype.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, The Open Group
DATE CREATED:	March 2003

DESCRIPTION:
	functions to store and access file type information

MODIFICATIONS:

************************************************************************/

#include <stdio.h>
#include <string.h>
#include "dtmac.h"
#include "error.h"
#include "ltoa.h"
#include "dtetlib.h"
#include "ftype.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* the file type list itself */
static struct tet_ftype *ftype;
static int lftype;
static int Nftype;

/* location pointer used by tet_getftent()/tet_setftent() */
static struct tet_ftype *nextftp;


/*
**	tet_addftype() - add a file type to the list
**
**	return 0 ifn successful or -1 on error
*/
int tet_addftype(suffix, type)
char *suffix;
int type;
{
	struct tet_ftype *ftp;
	int needlen;

	ASSERT(suffix && *suffix);

	switch (type) {
	case TET_FT_ASCII:
	case TET_FT_BINARY:
		break;
	default:
		error(0, "unexpected file type", tet_i2a(type));
		return(-1);
	}

	/* see if we already have an entry in the list */
	if ((ftp = tet_getftbysuffix(suffix)) != (struct tet_ftype *) 0) {
		ftp->ft_ftype = type;
		return(0);
	}

	/* here to create a new entry */
	needlen = (Nftype + 1) * sizeof *ftype;
	if (BUFCHK((char **) &ftype, &lftype, needlen) < 0)
		return(-1);
	ftp = ftype + Nftype++;
	ftp->ft_ftype = type;
	if ((ftp->ft_suffix = tet_strstore(suffix)) == (char *) 0)
		return(-1);

	return(0);
}

/*
**	tet_getftype() - get the file type of the named file
**
**	return	TET_FT_ASCII or TET_FT_BINARY if the file name suffix is known
**		0 if the file name has no suffix or if the file name suffix
**		is not known
**		-1 if the list has not yet been set up
*/
int tet_getftype(fname)
char *fname;
{
	struct tet_ftype *ftp;
	char *suffix;

	/* return now if the list is not yet set up */
	if (Nftype <= 0)
		return(-1);

	/* get the file name suffix */
	if ((suffix = strrchr(tet_basename(fname), '.')) == (char *) 0)
		return(0);
	else
		suffix++;
	if (!*suffix)
		return(0);

	/* get the list entry for this suffix */
	if ((ftp = tet_getftbysuffix(suffix)) == (struct tet_ftype *) 0)
		return(0);

	return(ftp->ft_ftype);
}

/*
**	tet_getftbysuffix() - return a ptr to the file type entry for the
**		specified suffix
**
**	return (struct tet_ftype) 0) if not found
*/
struct tet_ftype *tet_getftbysuffix(suffix)
char *suffix;
{
	struct tet_ftype *ftp;

	/* look up the suffix in the list */
	for (ftp = ftype; ftp < ftype + Nftype; ftp++)
		if (ftp->ft_suffix && strcmp(ftp->ft_suffix, suffix) == 0)
			return(ftp);

	return((struct tet_ftype *) 0);
}

/*
**	tet_getftent() - return the next file type entry
**
**	return (struct tet_ftype *) 0 at end-of-list
*/

struct tet_ftype *tet_getftent(void)
{
	struct tet_ftype *ftp;

	if (!nextftp)
		tet_setftent();

	if (Nftype > 0)
		while (nextftp < ftype + Nftype) {
			ftp = nextftp++;
			if (ftp->ft_suffix)
				return(ftp);
		}

	return((struct tet_ftype *) 0);
}

/*
**	tet_setftent() - rewind the tet_getftent() location to the
**		start of the list
*/

void tet_setftent(void)
{
	nextftp = ftype;
}

