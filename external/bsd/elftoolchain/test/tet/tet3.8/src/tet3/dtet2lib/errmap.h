/*
 *      SCCS:  @(#)errmap.h	1.4 (96/09/30)
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

SCCS:   	@(#)errmap.h	1.4 96/09/30 TETware release 3.8
NAME:		errmap.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	January 1993

DESCRIPTION:
	definition of the error map structure

MODIFICATIONS:

************************************************************************/

/* error map structure - this structure is used to map between:
	1) errno values and DTET message reply codes in functions
		maperr() and unmaperr()
	2) errno values and symbolic errno names in function tet_errname()
*/

struct errmap {
	int em_errno;
	int em_repcode;
	char *em_errname;
};

/* the error map itself and its size (in errmap.c) */
extern struct errmap tet_errmap[];
extern int tet_Nerrmap;

