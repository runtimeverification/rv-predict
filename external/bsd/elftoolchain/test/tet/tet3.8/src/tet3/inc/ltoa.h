/*
 *      SCCS:  @(#)ltoa.h	1.8 (98/08/28) 
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

SCCS:   	@(#)ltoa.h	1.8 98/08/28 TETware release 3.8
NAME:		ltoa.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	a header file for use with the num-to-ascii conversion functions

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., February 1994
	prepended tet_ to function names
	added macros to map short names

	Geoff Clare, UniSoft Ltd., August 1996
	Use sizeof to determine buffer length required.

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

#include <limits.h>	/* for CHAR_BIT */

/* macros for use with non-long quantities */
#define tet_i2a(x)	(tet_l2a((long) (x)))	/* convert int to ascii */

/* length of largest decimal long, including sign and null terminator */
#define LNUMSZ		(2 + (sizeof(long)*CHAR_BIT*10 + 32)/33)

#define tet_i2o(x)	(tet_l2o((long) (x)))	/* convert int to octal ascii*/

/* length of largest octal long, including leading 0 and null terminator */
#define LONUMSZ		(2 + (sizeof(long)*CHAR_BIT + 2)/3)

#define tet_i2x(x)	(tet_l2x((long) (x)))	/* convert int to hex ascii */

/* length of largest hex long, including leading 0x and null terminator */
#define LXNUMSZ		(3 + (sizeof(long)*CHAR_BIT + 3)/4)

#define NLBUF		5		/* no of times each function may be
					   called before re-using static
					   buffers */


/* extern function declarations */
TET_IMPORT_FUNC(char *, tet_l2a, PROTOLIST((long)));
TET_IMPORT_FUNC(char *, tet_l2o, PROTOLIST((long)));
TET_IMPORT_FUNC(char *, tet_l2x, PROTOLIST((long)));

