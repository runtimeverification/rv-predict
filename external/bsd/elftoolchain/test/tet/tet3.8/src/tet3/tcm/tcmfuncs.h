/*
 *      SCCS:  @(#)tcmfuncs.h	1.12 (99/09/03) 
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

/************************************************************************

SCCS:   	@(#)tcmfuncs.h	1.12 99/09/03 TETware release 3.8
NAME:		tcmfuncs.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	declarations of extern tcm-specific functions not declared in
	other header files

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd, October 1992
	added tet_callfuncname() declaration
	
	Added declarations of ictp.c and [ms]tcmdist.c functions.

	Andrew Dingwall, UniSoft Ltd., August 1996
	changes for tetware tcc

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/


extern void tet_tcminit PROTOLIST((int, char **));
extern char *tet_callfuncname PROTOLIST((void));
extern void tet_check_apilib_version PROTOLIST((void));
extern void tet_dtcmerror PROTOLIST((int, char *, int, char *, char *));
extern int tet_tcmc_main PROTOLIST((int, char **));

#ifndef TET_LITE        /* -START-LITE-CUT- */
   extern int tet_tcmptype PROTOLIST((void));
   extern int tet_tcm_ts_tsinfo2bs PROTOLIST((char *, char *));
   extern int tet_tcm_ts_tsinfolen PROTOLIST((void));
   extern void tet_ts_tcminit PROTOLIST((void));
#endif                  /* -END-LITE-CUT- */

#if defined(_WIN32) && defined(TET_SHLIB)	/* -START-WIN32-CUT- */
   extern void tet_w32dynlink PROTOLIST((void));
   TET_IMPORT_FUNC(void, tet_w32dlcheck, PROTOLIST((void)));
#endif						/* -END-WIN32-CUT- */

#ifndef TET_LITE        /* -START-LITE-CUT- */
   extern int tet_psysid;
#endif                  /* -END-LITE-CUT- */

