/*
 *      SCCS:  @(#)server.h	1.8 (98/08/28) 
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

SCCS:   	@(#)server.h	1.8 98/08/28 TETware release 3.8
NAME:		server.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	declarations of server-specific functions that may be called by
	library routines

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, September 1993
	added prototypes for ss_disconnect().

	Andrew Dingwall, UniSoft Ltd., December 1993
	removed disconnect stuff

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
	Only make this stuff visible when compiling Distributed TETware.
 

************************************************************************/

#ifndef TET_LITE	/* -START-LITE-CUT- */

extern int	tet_ss_argproc PROTOLIST((char *, char *));
extern void	tet_ss_cleanup PROTOLIST((void));
TET_EXPORT_FUNC(void, tet_ss_connect, PROTOLIST((struct ptab *)));
TET_EXPORT_FUNC(void, tet_ss_dead, PROTOLIST((struct ptab *)));
extern void	tet_ss_initdaemon PROTOLIST((void));
extern void	tet_ss_logoff PROTOLIST((struct ptab *));
extern int	tet_ss_logon PROTOLIST((struct ptab *));
extern void	tet_ss_newptab PROTOLIST((struct ptab *));
TET_EXPORT_FUNC(void, tet_ss_process, PROTOLIST((struct ptab *)));
TET_EXPORT_FUNC(int, tet_ss_ptalloc, PROTOLIST((struct ptab *)));
TET_EXPORT_FUNC(void, tet_ss_ptfree, PROTOLIST((struct ptab *)));
extern void	tet_ss_procrun PROTOLIST((void));
TET_EXPORT_FUNC(int, tet_ss_serverloop, PROTOLIST((void)));
extern void	tet_ss_serverproc PROTOLIST((struct ptab *));
extern void	tet_ss_timeout PROTOLIST((struct ptab *));
TET_EXPORT_FUNC(int, tet_ss_tsinfo, PROTOLIST((struct ptab *, int)));

#endif			/* -END-LITE-CUT- */

