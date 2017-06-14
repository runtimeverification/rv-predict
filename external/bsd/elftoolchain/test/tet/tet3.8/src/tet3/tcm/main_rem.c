/*
 *	SCCS: @(#)main_rem.c	1.2 (99/09/15)
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
static char sccsid_main_rem[] = "@(#)main_rem.c	1.2 (99/09/15) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)main_rem.c	1.2 99/09/15 TETware release 3.8
NAME:		main_rem.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	July 1999

DESCRIPTION:
	Remote process controller main() function.

MODIFICATIONS:

************************************************************************/

/*
** This file is a component of the TCM (tcm.o) and/or one of the child
** process controllers (tcmchild.o and tcmrem.o).
** On UNIX systems, these .o files are built using ld -r.
** There is no equivalent to ld -r in MSVC, so on Win32 systems each .c
** file is #included in a scratch .c or .cpp file and a single object
** file built from that.
**
** This imposes some restictions on the contents of this file:
**
**	+ Since this file might be included in a C++ program, all
**	  functions must have both ANSI C and common C definitions.
**
**	+ The scope of static variables and functions encompasses all
**	  the source files, not just this file.
**	  So all static variables and functions must have unique names.
*/


/*
** note no other .h files are allowed here -
** if any are needed, remove tet_api.h and use tcmhdrs.h instead
*/
#include "tet_api.h"

/*
**	main() is the main program for the remote process controller
**	It is simply a wrapper for tet_tcmrem_main().
*/

#ifdef TET_PROTOTYPES
int main(int argc, char **argv)
#else
int main(argc, argv)
int argc;
char **argv;
#endif
{
	tet_tcmrem_main(argc, argv);

	/* NOTREACHED */
	return(1);
}

