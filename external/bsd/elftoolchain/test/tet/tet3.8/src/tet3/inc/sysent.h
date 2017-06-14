/*
 *      SCCS:  @(#)sysent.h	1.7 (98/08/28) 
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1992 X/Open Company Limited
 * (C) Copyright 1994 UniSoft Limited
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

SCCS:   	@(#)sysent.h	1.7 98/08/28 TETware release 3.8
NAME:		sysent.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	a header file for use with the systems file access functions

MODIFICATIONS:

	Denis McConalogue, UniSoft Ltd.
	XTI enhancements - allow TCCD addresses to be included in the
	systems file.

	Andrew Dingwall, UniSoft Ltd., December 1993
	enhancements for FIFO transport interface

	Andrew Dingwall, UniSoft Ltd., July 1998
	Removed vestiges of the FIFO transport interface.
	Added support to enable the tccd port number to be specified
	in the systems file when the INET transport is used.


************************************************************************/

/*
**	DTET systems are known internally by (numeric) sysid
**	a sysid is mapped to a system (or host) name by an entry in the
**	systems file
**	a system name may be used by transport-specific routines to
**	determine how to connect to that system
**
**	a test case can access some of these fields by using
**	the tet_getsysbyid() API function 
*/

/* structure of a systems table entry */
struct sysent {
	int sy_sysid;		/* system id */
	char *sy_name;		/* system name */
	char *sy_tccd;		/* INET port number or XTI address
				   for the TCCD daemon */
};


/* extern function declarations */
extern void tet_libendsysent PROTOLIST((void));
extern struct sysent *tet_libgetsysbyid PROTOLIST((int));
extern struct sysent *tet_libgetsysent PROTOLIST((void));
extern int tet_libsetsysent PROTOLIST((void));

