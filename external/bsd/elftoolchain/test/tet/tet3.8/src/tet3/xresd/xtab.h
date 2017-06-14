/*
 *      SCCS:  @(#)xtab.h	1.7 (05/06/27) 
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

SCCS:   	@(#)xtab.h	1.7 05/06/27 TETware release 3.8
NAME:		xtab.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	execution results file table description

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., November 1994
	perform Abort processing as each result is registered

	Geoff Clare, The Open Group, June 2005
	Added XF_FULL_TIMESTAMPS flag value.

************************************************************************/


/*
**	Execution results file table.
**
**	An element is allocated in the execution results file table for each
**	execution results file (xres file) opened by a call to OP_XROPEN.
**	Storage for an element is allocated by xtalloc() and freed by
**	xtfree().
**	An element is added to the table by xtadd() and removed by xtrm().
*/

/* per-user details structure for the execution results file table
	(really per-system since there can be more than one process per system)
*/
struct uxtab {
	int ux_sysid;			/* system id */
	struct ptab *ux_ptab;		/* ptr to first user's ptab entry */
	int ux_state;			/* process state - see below */
	int ux_result;			/* TP result code */
};

/* values for ux_state (discrete values) */
#define XS_NOTREPORTED	1
#define XS_REPORTED	2
#define XS_DEAD		3

/*
**	structure of the execution results file table
**
**	the next and last pointers must be first so as to allow the
**	use of the llist routines to manipulate the table
*/

struct xtab {
	struct xtab *xt_next;		/* ptr to next element in list */
	struct xtab *xt_last;		/* ptr to last element in list */
	long xt_xrid;			/* id for xres requests */
	struct ptab *xt_ptab;		/* ptr to owner's ptab */
	char *xt_xfname;		/* tet_xres file name */
	FILE *xt_xfp;			/* fp for tet_xres file */
	struct uxtab *xt_ud;		/* ptr to per-user details */
	int xt_nud;			/* no of active xt_ud elements */
	int xt_udlen;			/* no of bytes in xt_ud */
	int xt_icno;			/* current IC number */
	long xt_activity;		/* current TCC activity number */
	int xt_tpcount;			/* expected number of TPs in this IC */
	int xt_tpno;			/* current TP number */
	int xt_result;			/* TP result */
	int xt_flags;			/* flags - see below */
};

/* values for xt_flags (a bit field) */
#define XF_ICINPROGRESS		001	/* IC is in progress */
#define XF_ICDONE		002	/* IC finished */
#define XF_TPINPROGRESS		004	/* TP is in progress */
#define XF_TPDONE		010	/* TP finished */
#define XF_TCABORT		020	/* return ER_ABORT in tpend() */
#define XF_FULL_TIMESTAMPS      040     /* use full timestamps */


/* extern function declarations */
extern int icend PROTOLIST((struct xtab *));
extern int tpend PROTOLIST((struct xtab *));
extern int uxtalloc PROTOLIST((struct xtab *, int));
extern void xtadd PROTOLIST((struct xtab *));
extern struct xtab *xtalloc PROTOLIST((void));
extern struct xtab *xtfind PROTOLIST((long));
extern void xtfree PROTOLIST((struct xtab *));
extern void xtrm PROTOLIST((struct xtab *));

