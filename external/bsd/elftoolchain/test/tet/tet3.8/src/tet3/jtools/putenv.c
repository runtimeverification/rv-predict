/*
 *	SCCS: @(#)putenv.c	1.1 (99/09/02)
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
static char sccsid[] = "@(#)putenv.c	1.1 (99/09/02) TETware release 3.8";
#endif

/************************************************************************

SCCS:		@(#)putenv.c	1.1 99/09/02 TETware release 3.8
NAME:		putenv.c
PRODUCT:	TETware
AUTHOR:		Matthew Hails, UniSoft Ltd.
DATE CREATED:	13 Aug 1999
SYNOPSIS:

	int jt_putenv(char *string)

DESCRIPTION:
	jt_putenv() changes or adds a value to the environment.

************************************************************************/

#include <stdlib.h>

/*
 * jt_putenv()
 *
 * Changes or adds a value to the environment.
 *
 *	string	String of form `name=value'. This string becomes part of the
 *		environment, so altering this string alters the environment.
 *
 * Returns 0 on success, non-zero on failure.
 */
int
jt_putenv(char *string)
{
#ifdef _WIN32	/* -START-WIN32-CUT- */
	return _putenv(string);
#else		/* -END-WIN32-CUT */
	return putenv(string);
#endif		/* -WIN32-CUT-LINE- */
}
