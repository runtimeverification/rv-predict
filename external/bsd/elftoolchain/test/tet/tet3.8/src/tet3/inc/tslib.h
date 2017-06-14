/*
 *      SCCS:  @(#)tslib.h	1.9 (99/11/15) 
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

SCCS:   	@(#)tslib.h	1.9 99/11/15 TETware release 3.8
NAME:		tslib.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	declarations for function interfaces to the transport-specific
	function library

	all transport-specific libraries should implement all of these
	interfaces

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, September 1993
	added prototypes for ts_disconnect()

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
	Andrew Dingwall, UniSoft Ltd., October 1999
	only declare things that use struct ptab when ptab.h has
	previously been included

************************************************************************/

/* only include this stuff in Distributed TETware */
#ifndef TET_LITE	/* -START-LITE-CUT- */

   /* extern function declarations */
   extern void tet_ts_cleanup PROTOLIST((void));
   TET_IMPORT_FUNC(void, tet_ts_startup, PROTOLIST((void)));
   extern int tet_ts_tcmputenv PROTOLIST((void));
#  ifdef TET_STRUCT_PTAB_DEFINED
     TET_IMPORT_FUNC(void, tet_ts_connect, PROTOLIST((struct ptab *)));
     extern void tet_ts_dead PROTOLIST((struct ptab *));
     extern void tet_ts_disconnect PROTOLIST((struct ptab *));
     extern int tet_ts_poll PROTOLIST((struct ptab *, int));
     extern int tet_ts_ptalloc PROTOLIST((struct ptab *));
     extern void tet_ts_ptfree PROTOLIST((struct ptab *));
     extern int tet_ts_rcvmsg PROTOLIST((struct ptab *));
     extern int tet_ts_sndmsg PROTOLIST((struct ptab *));
#  endif

#endif /* !TET_LITE */	/* -END-LITE-CUT- */

