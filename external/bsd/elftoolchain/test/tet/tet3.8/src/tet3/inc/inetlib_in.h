/*
 *      SCCS:  @(#)inetlib_in.h	1.7 (98/08/28) 
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

SCCS:   	@(#)inetlib_in.h	1.7 98/08/28 TETware release 3.8
NAME:		inetlib_in.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	declarations for extern inetlib functions not declared in other
	header files

	these functions are inet-specific; the transport-independent
	interfaces to the transport-specific library are declared in
	tslib.h

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/


TET_IMPORT_FUNC(struct in_addr *, tet_gethostaddr, PROTOLIST((char *)));
extern struct in_addr *tet_getlocalhostaddr PROTOLIST((void));
TET_IMPORT_FUNC(int, tet_gettccdaddr, PROTOLIST((struct ptab *)));
extern int tet_gettccdport PROTOLIST((void));
extern void tet_ts_accept PROTOLIST((SOCKET));
extern void tet_ts_listen PROTOLIST((SOCKET));
extern int tet_ts_nbio PROTOLIST((struct ptab *));

