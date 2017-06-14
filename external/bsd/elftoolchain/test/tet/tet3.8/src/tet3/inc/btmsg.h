/*
 *      SCCS:  @(#)btmsg.h	1.7 (96/11/04) 
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

SCCS:   	@(#)btmsg.h	1.7 96/11/04 TETware release 3.8
NAME:		btmsg.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	a header file describing the structure of the DTET interprocess
	binary transfer message

MODIFICATIONS:
	Geoff Clare, UniSoft Ltd., Oct 1996
	Portability fixes.

************************************************************************/

/*
**	structure of a binary file transfer message
**
**	NOTE:
**	if you change this structure, be sure to update the element sizes,
**	dummy structure and initialisation code defined below,
**	and change the version number in dtmsg.h as well
*/

#define BT_DLEN		1024

struct btmsg {
	unsigned short bt_fid;		/* destination file id */
	unsigned short bt_count;	/* number of data bytes */
	char bt_data[BT_DLEN];		/* file data */
};

/* btmsg element positions for use on machine-independent data streams */
#define BT_FID		0
#define BT_COUNT	(BT_FID + SHORTSIZE)
#define BT_DATA		(BT_COUNT + SHORTSIZE)
#define BT_BTMSGSZ	(BT_DATA + BT_DLEN)


#if TET_LDST

/* btmsg structure description */
#define BTMSG_DESC	{ ST_USHORT(1),		BT_FID }, \
			{ ST_USHORT(1),		BT_COUNT }, \
			{ ST_CHAR(BT_DLEN), 	BT_DATA }

/* stdesc initialisation for btmsg structure */
#define BTMSG_INIT(st, bp, n, nst) \
		st[n++].st_stoff = (char *) &bp->bt_fid - (char *) bp; \
		st[n++].st_stoff = (char *) &bp->bt_count - (char *) bp; \
		st[n++].st_stoff = (char *) &bp->bt_data[0] - (char *) bp; \
		nst = n;

#endif /* TET_LDST */


/* extern function declarations */
extern int tet_bs2btmsg PROTOLIST((char *, int, struct btmsg **, int *));
extern int tet_btmsg2bs PROTOLIST((struct btmsg *, char *));

