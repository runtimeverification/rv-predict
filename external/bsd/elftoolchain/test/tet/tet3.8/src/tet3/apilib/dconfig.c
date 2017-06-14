/*
 *	SCCS: @(#)dconfig.c	1.16 (99/11/15)
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
static char sccsid[] = "@(#)dconfig.c	1.16 (99/11/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)dconfig.c	1.16 99/11/15
NAME:		'C' API configuration variable functions
PRODUCT:	TETware
AUTHOR:		Geoff Clare, UniSoft Ltd.
DATE CREATED:	27 July 1990
SYNOPSIS:

	char *tet_getvar(char *name);

	void tet_config(void);

DESCRIPTION:

	Tet_getvar() obtains the value of the named configuration
	variable.  It returns NULL if the variable has not been set.

	Tet_config() is not part of the API.  It is used by other
	API functions to read configuration variables from the file
	specified by the communication variable TET_CONFIG and makes
	them available to tet_getvar().  If the file cannot be opened
	or read an error message is produced and no variables will be
	set.  Tests using tet_getvar() are expected to report the
	NULL value returned in this case.

MODIFICATIONS:

	June 1992
	DTET development - this file is derived from TET release 1.10
	
	Denis McConalogue, UniSoft Limited, August 1993
	changed dtet to tet2 in #include

	Andrew Dingwall, UniSoft Ltd., December 1993
	removed tet_putenv() - no longer used anywhere

	Geoff Clare, UniSoft Ltd., August 1996
	Missing <stdio.h> (needed for sprintf)

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
	Andrew Dingwall, UniSoft Ltd., July 1999
	added call to tet_check_api_status();

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <sys/types.h>
#include <string.h>
#include <errno.h>
#include "dtmac.h"
#include "ltoa.h"
#include "dtetlib.h"
#include "tet_api.h"
#include "apilib.h"


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* the configuration variable list */
static char **varptrs;
static int lvarptrs, nvarptrs;

TET_IMPORT char *tet_getvar(name)
char *name;
{
	/* return value of specified configuration variable */

	char **cur;
	char *cp;
	size_t len;

	tet_check_api_status(TET_CHECK_API_INITIALISED);

	if (!nvarptrs)
		return ((char *) NULL);

	/* varptrs is an array of strings of the form: "VAR=value" */
	len = strlen(name);
	for (cur = varptrs; *cur != NULL; cur++)
	{
		cp = *cur;
		if (strncmp(cp, name, len) == 0 && cp[len] == '=')
			return &cp[len+1];
	}

	return ((char *) NULL);
}

TET_IMPORT void tet_config()
{
	FILE *fp;
	char *file;
	int err;
	char buf[1024];
	register char *p;
	char **vp;
	int lcount;
	static char fmt[] = "ignored bad format configuration variable assignment at line %d in file %.*s";
	char msg[MAXPATH + LNUMSZ + sizeof fmt];

	/* determine the config file name from the environment */
	file = getenv("TET_CONFIG");
	if (file == NULL || *file == '\0')
		return;

	/* open the file */
	if ((fp = fopen(file, "r")) == (FILE *) 0) {
		err = errno;
		(void) sprintf(msg, "could not open config file \"%.*s\"",
			MAXPATH, file);
		tet_error(err, msg);
		return;
	}

	/* free any existing allocated string storage */
	if (nvarptrs > 0)
		for (vp = varptrs; vp < varptrs + nvarptrs; vp++)
			if (*vp) {
				TRACE2(tet_Tbuf, 6, "free *vp = %s",
					tet_i2x(*vp));
				free(*vp);
			}

	lcount = nvarptrs = 0;
	while (fgets(buf, sizeof buf, fp) != (char *) 0) {
		lcount++;
		for (p = buf; *p; p++)
			if (*p == '\r' || *p == '\n' || *p == '#') {
				*p = '\0';
				break;
			}
		while (--p >= buf)
			if (isspace(*p))
				*p = '\0';
			else
				break;
		if (p < buf)
			continue;
		if (!tet_equindex(buf)) {
			(void) sprintf(msg, fmt, lcount, MAXPATH, file);
			tet_error(0, msg);
			continue;
		}
		if (BUFCHK((char **) &varptrs, &lvarptrs,
			   (nvarptrs + 2) * (int) sizeof *varptrs) < 0)
			break;
		if ((p = tet_strstore(buf)) == (char *) 0)
			break;
		*(varptrs + nvarptrs++) = p;
		*(varptrs + nvarptrs) = (char *) 0;
	}

	/* finally, close the file and return */
	(void) fclose(fp);
}

