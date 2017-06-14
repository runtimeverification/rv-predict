/*
 *      SCCS:  @(#)synreq.h	1.12 (99/09/02) 
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

SCCS:   	@(#)synreq.h	1.12 99/09/02 TETware release 3.8
NAME:		synreq.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	descriptions of structures and values used in sync requests and replies

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., October 1992
	Moved ASYNC timeout from several TCM source files to here

	Andrew Dingwall, UniSoft Ltd., October 1992
	added support for tet_msync()

	Andrew Dingwall, UniSoft Ltd., August 1996
	added support for tetware-style syncs

	Andrew Dingwall, UniSoft Ltd., July 1998
	Use (icno + 1) when generating an autosync number so as to enable
	IC 0 to be specified.
	Added support for shared API libraries.
 
	Andrew Dingwall, UniSoft Ltd., July 1999
	now that we build tcm.o with ld -r, tcm code is no longer in the
	library and so tet_tcm_async() doesn't need to be exported

************************************************************************/

#ifndef TET_LITE	/* -START-LITE-CUT- */

/* sync votes - must match TET_SV_* values in tet_api.h */
#define SV_YES		1
#define SV_NO		2

/* sync states - must match TET_SS_* values in tet_api.h */
#define SS_NOTSYNCED	1
#define SS_SYNCYES	2
#define SS_SYNCNO	3
#define SS_TIMEDOUT	4
#define SS_DEAD		5

/* values for use with TCM autosyncs */
#define SV_SYNC_TIMEOUT	60	/* sync timeout for IC/TP start */

/* values for use with exec auto-syncs */
#define SV_EXEC_SPNO	1L	/* sync point number */
#define SV_EXEC_TIMEOUT	60	/* sync timeout */

/* generate a sync point number for auto sync requests -
	15 bits icno, 15 bits tpno, 1 bit start/end */
#define S_ICBITS	15
#define S_TPBITS	15
#define S_SEBITS	1
#define S_ICSHIFT	(S_TPSHIFT + S_TPBITS)
#define S_TPSHIFT	S_SEBITS
#define S_ICMASK	(~0L << S_ICSHIFT)
#define S_TPMASK	((~S_ICMASK << S_TPSHIFT) & ~S_ICMASK)
#define S_SEMASK	(~(S_ICMASK | S_TPMASK))

#define MK_ASPNO(icno, tpno, flag) \
	(((long) ((icno) + 1) << S_ICSHIFT) | \
		(((long) (tpno) << S_TPSHIFT) & S_TPMASK) | (flag))

/* IC and TP start and end flags for use with MK_ASPNO */
#define S_ICSTART	0	/* IC start */
#define S_ICEND		1	/* IC end */
#define S_TPSTART	0	/* TP start */
#define S_TPEND		1	/* TP end */

/* extract icno, tpno and flag from sync point number */
#define EX_ICNO(spno) \
	((int) ((unsigned long) (spno & S_ICMASK) >> S_ICSHIFT) - 1)
#define EX_TPNO(spno) \
	((int) ((unsigned long) (spno & S_TPMASK) >> S_TPSHIFT))
#define EX_FLAG(spno) \
	((int) (spno & S_SEMASK))


/* sync request/result structure used with tet_sdasync() and tet_sdusync() */
struct synreq {
	int sy_sysid;			/* system id */
	union {
		long sy_long;
		int sy_int;
	} sy_un;
};

/* short names for sy_un members */
#define sy_ptype	sy_un.sy_int	/* process type (u/sync request) */
#define sy_spno		sy_un.sy_long	/* sync point number (ok reply) */
#define sy_state	sy_un.sy_int	/* sync state (error reply) */


/* user sync message data structure used with tet_sdusync() and in syncd -
	this is an adjunct to struct tet_synmsg in tet_api.h */
struct synmsg {
	long sm_spno;		/* message's sync point number */
	char *sm_data;		/* ptr to sync message data */
	int sm_mdlen;		/* length of *sm_data */
	int sm_dlen;		/* no of bytes in sync message */
	int sm_sysid;		/* id of sync message sending system */
	int sm_flags;		/* flags - see below */
};

/* values for sm_flags (a bit field) */
#define SM_SNDMSG	001
#define SM_RCVMSG	002
#define SM_DUP		004
#define SM_TRUNC	010


/* extern function declarations */
TET_IMPORT_FUNC(int, tet_sdasync,
	PROTOLIST((long, long, long, int, int, struct synreq *, int *)));
extern int tet_sdusync PROTOLIST((long, long, long, int, int, struct synreq *,
	int, struct synmsg *));
extern int tet_tcm_async PROTOLIST((long, int, int, struct synreq *, int *));

/* extern data items */
extern struct synreq * tet_synreq;

#endif	/* -END-LITE-CUT- */

