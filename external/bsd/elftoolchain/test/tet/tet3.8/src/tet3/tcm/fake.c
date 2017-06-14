/*
 *      SCCS:  @(#)fake.c	1.5 (99/09/03) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1992 X/Open Company Limited
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
static char sccsid_fake[] = "@(#)fake.c	1.5 (99/09/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)fake.c	1.5 99/09/03 TETware release 3.8
NAME:		fake.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	fake client-specific functions for the distributed TCM

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1999
	moved TCM code out of the API library

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
**	+ The only .h file that may appear in this file is tcmhdrs.h;
**	  all other .h files that are needed must be #included in there.
**
**	+ The scope of static variables and functions encompasses all
**	  the source files, not just this file.
**	  So all static variables and functions must have unique names.
*/


/*
** all the header files are included by tcmhdrs.h
** don't include any other header files directly
*/
#include "tcmhdrs.h"


#ifdef NEEDsrcFile
#  ifdef srcFile
#    undef srcFile
#  endif
#  define srcFile srcFile_fake
   static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/*
**	tet_ss_serverloop() - server-specific server loop
**
**	this may be called from tet_si_servwait() if non-blocking message i/o
**	would block
**
**	tcm does not do non-blocking i/o, so this should never occur
*/

#ifdef PROTOTYPES
TET_EXPORT int tet_ss_serverloop(void)
#else
TET_EXPORT int tet_ss_serverloop()
#endif
{
	error(0, "internal error - serverloop called!", (char *) 0);
	return(-1);
}

/*
**	tet_ss_process() - server-specific request process routine
**
**	would be called from tet_si_service() when state is PS_PROCESS
**
**	tcm only uses tet_si_clientloop() which itself returns as soon as a
**	process reaches this state, so tet_ss_process() should never be called
**/

#ifdef PROTOTYPES
TET_EXPORT void tet_ss_process(struct ptab *pp)
#else
TET_EXPORT void tet_ss_process(pp)
struct ptab *pp;
#endif
{
	error(0, "internal error - tet_ss_process called!",
		tet_r2a(&pp->pt_rid));
}

