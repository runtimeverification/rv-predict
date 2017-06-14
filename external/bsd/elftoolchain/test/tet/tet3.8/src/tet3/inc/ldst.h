/*
 *      SCCS:  @(#)ldst.h	1.7 (96/11/04) 
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

SCCS:   	@(#)ldst.h	1.7 96/11/04 TETware release 3.8
NAME:		ldst.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	a header file for use with internal to machine-independent format data
	conversion functions

MODIFICATIONS:

************************************************************************/

/*
**	DTET processes may run on machines with different word sizes, byte
**	order and alignment requirements.
**	Data communicated between processes that may be on different machines
**	is converted between internal and machine-indepdendent format by a set
**	of routines in dtet2lib
**
**	In order to maintain compatability between machines, IPC structures
**	that hold binary data should contain only short and long qantities.
**	It is assumed that a short contains at least 16 bits and a long at
**	least 32 bits - this is true for all machines that I know of.
**
**	A set of functions are provided for each structure that may be
**	communicated between processes.
**	Each function set includes an stdesc description of the structure to
**	be converted, and functions to perform conversions in each direction.
**	Most of the structure-specific conversion functions call tet_st2bs()
**	and tet_bs2st() (in ldst.c) to perform the actual conversions;
**	these functions use information in an stdesc structure supplied
**	as part of the call.
**
**	For conversion of a variable length item from machine-indepdendent
**	to internal format, the 'to' parameter is the address of a
**	pointer to the receiving area, and the 'tolen' parameter is the
**	address of the item containing the length of the receiving area.
**	If the receiving item is found not to be big enough to receive
**	the incoming data, it is grown by a call to tet_bufchk() which
**	will update the receiving address and length items in place.
**
**	The structure descriptions and stdesc initialisation code fragments
**	themselves are defined as macros in the related header files.
**	This is an attempt to make sure that the structure definitions and
**	their descriptions keep in step.
*/


#define TET_LDST	1	/* include structure initialisation code
				   macros in other header files */

/* structure description */
struct stdesc {
	short st_type;			/* element type and number */
	int st_bsoff;			/* position on byte stream */
	int st_stoff;			/* position in structure */
};

/*
**	structure element type values -
**		element types in the top 4 bits,
**		number of elements in the bottom 12 bits
*/
#define ST_MAKETYPE(n)	((n) << 12)
#define ST_TYPEMASK	ST_MAKETYPE(~0)
#define ST_COUNTMASK	~ST_TYPEMASK
#define ST_CHARTYPE	ST_MAKETYPE(1)
#define ST_SHORTTYPE	ST_MAKETYPE(2)
#define ST_USHORTTYPE	ST_MAKETYPE(3)
#define ST_LONGTYPE	ST_MAKETYPE(4)
/* values used in structure descriptions -
	each macro describes an array of n elements of the given type */
#define ST_CHAR(n)	(ST_CHARTYPE | (ST_COUNTMASK & (n)))
#define ST_SHORT(n)	(ST_SHORTTYPE | (ST_COUNTMASK & (n)))
#define ST_USHORT(n)	(ST_USHORTTYPE | (ST_COUNTMASK & (n)))
#define ST_LONG(n)	(ST_LONGTYPE | (ST_COUNTMASK & (n)))

/*
**	macros to convert between machine and natural byte order
**		without regard to hardware object alignment requirements
**
**	the ld macros return the machine byte order value of the natural byte
**	order object pointed to by (char *) p
**
**	the st macros store a machine byte order value x in natural byte order
**	in the location pointed to by (char *) p
*/

/* fundamental constants */
#undef BYTEMASK
#define BYTEMASK	0xff
#undef BYTESHIFT
#define BYTESHIFT	8

/* the macros themselves */
#define ld16(p)		(*(p) << BYTESHIFT | (unsigned char) *((p) + 1))

#define ld16u(p)	((unsigned char) *(p) << BYTESHIFT | \
				(unsigned char) *((p) + 1))

#define ld32(p)		((long) ld16(p) << (BYTESHIFT * 2) | \
				(unsigned long) ld16u((p) + 2))

#define st16(x, p)	(*(p) = (unsigned) (x) >> BYTESHIFT, \
				*((p) + 1) = (x) & BYTEMASK)

#define st32(x, p)	(st16((unsigned long) (x) >> (BYTESHIFT * 2), (p)), \
				st16((x), (p) + 2))


/* extern function declarations */
extern int tet_bs2st PROTOLIST((char *, char *, struct stdesc *, int, int));
extern int tet_st2bs PROTOLIST((char *, char *, struct stdesc *, int));

