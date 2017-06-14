/*
 *      SCCS:  @(#)xtilib_xt.h	1.7 (98/08/28) 
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

/************************************************************************

SCCS:   	@(#)xtilib_xt.h	1.7 98/08/28 TETware release 3.8
NAME:		xtilib_xt.h
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Ltd.
DATE CREATED:	April 1993

DESCRIPTION:
	declarations for extern xtilib functions not declared in other
	header files

	these functions are xti-specific; the transport-independent
	interfaces to the transport-specific library are declared in
	tslib.h

MODIFICATIONS:

	Denis McConalogue, UniSoft Limited, December 1993
	added SAME_XTIADDR macro, t_alloc() macros

	Andrew Dingwall, UniSoft Ltd., November 1994
	updated t_alloc() structure type names in line with latest XTI spec

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#define MAX_CONN_IND	10

#ifndef MAX_ADDRL
#  define MAX_ADDRL	64
#endif

#define SAME_XTIADDR(p, q) (((p)->addr.len == (q)->addr.len) && \
	 (memcmp((p)->addr.buf, (q)->addr.buf, (q)->addr.len) == 0))

#define T_ALLOC_BIND(fd) ((struct t_bind *) t_alloc(fd, T_BIND, T_ADDR))
#define T_ALLOC_CALL(fd) ((struct t_call *) t_alloc(fd, T_CALL, T_ADDR))
#define T_ALLOC_DIS(fd)  ((struct t_discon *) t_alloc(fd, T_DIS,  T_ADDR))

#define xt_error(errnum, s1, s2) \
	tet_xtierror(errnum, srcFile, __LINE__, s1, s2)
#define xt_fatal(errnum, s1, s2) \
	tet_xtifatal(errnum, srcFile, __LINE__, s1, s2)

#ifndef NEEDsrcFile
#  define NEEDsrcFile
#endif

/* extern data items */
TET_EXPORT_DATA(char *, tet_tpname);
TET_EXPORT_DATA(int, tet_tpi_mode);
extern struct t_call *tet_calls[];

/* extern function declarations */
extern char *tet_addr2lname PROTOLIST((struct netbuf *));
extern unsigned long tet_inetoul PROTOLIST((char *));
TET_IMPORT_FUNC(struct netbuf *, tet_lname2addr, PROTOLIST((char *)));
extern int tet_mode2i PROTOLIST((char *));
TET_IMPORT_FUNC(int, tet_gettccdaddr, PROTOLIST((struct ptab *)));
extern void tet_ts_listen PROTOLIST((int));
extern int tet_ts_nbio PROTOLIST((struct ptab *));
extern void tet_ts_accept PROTOLIST((int));
extern char *tet_xterrno2a PROTOLIST((int));
extern char *tet_xtev2a PROTOLIST((int));
extern void tet_xtierror PROTOLIST((int, char *, int, char *, char *));
extern void tet_xtifatal PROTOLIST((int, char *, int, char *, char *));

