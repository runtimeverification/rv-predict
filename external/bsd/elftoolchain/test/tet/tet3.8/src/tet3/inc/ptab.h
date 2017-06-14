/*
 *      SCCS:  @(#)ptab.h	1.11 (03/03/26) 
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

SCCS:   	@(#)ptab.h	1.11 03/03/26 TETware release 3.8
NAME:		ptab.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	per-process information table description

REQUIRES PRIOR INCLUSION OF:
	<time.h>	(for time_t)
	"dtmac.h"
	"dtmsg.h"

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., November 1993
	modifications for use with FIFO transport library

	Geoff Clare, UniSoft Ltd., October 1996
	Added <time.h> (needed when compiled with _POSIX_SOURCE).

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
	Only make the contents of this file visible when building
	Distributed TETware.

	Andrew Dingwall, UniSoft Ltd., July 1999
	declare tet_ptab in this file rather than having declarations
	scattered around the various .c files
 
	Andrew Dingwall, UniSoft Ltd., October 1999
	added #define TET_STRUCT_PTAB_DEFINED so that other header files 
	can conditionally exclude declarations that use struct ptab

************************************************************************/

#ifndef TET_LITE	/* -START-LITE-CUT- */

/*
**	Per-process information table.
**
**	A process table element is allocated for each connection to
**	another process.
**	Storage for an element is allocated by tet_ptalloc() and freed
**	by tet_ptfree().
**
**	Servers keep client process table elements in a global process table
**	which takes the form of a linked list.
**	An element is inserted in the table by tet_ptadd() and removed
**	by tet_ptrm().
**	Clients sometimes keep server process table elements in
**	standard places rather than in the global process table.
**
**	For a process acting as a server, an element is allocated
**	automatically (by the transport-specific accept routine) each time an
**	incoming connection is accepted.
**	A pointer to the element is then passed to the server-specific
**	tet_ss_newptab() function which may store it in some appropriate
**	place - usually in the global process table.
**
**	For a process acting as a client, an element must be allocated and
**	initialised before calling tet_ts_connect().
**	tet_ts_connect() then calls the client-specific tet_ss_tsconnect()
**	function which should fill in transport-specific details such as
**	the address of the server process.
*/

/* remote process details structure */
struct remid {
	int re_sysid;		/* system id */
	long re_pid;		/* process id on that system */
	int re_ptype;		/* process type (in dtmsg.h) */
};

/*
**	process table structure - this is a linked list
**
**	the next and last pointers must be first so as to allow the
**	use of the llist routines to manipulate the table
*/

struct ptab {
	struct ptab *pt_next;		/* ptr to next element in list */
	struct ptab *pt_last;		/* ptr to prev element in list */
	long pt_magic;			/* magic number */
	int pt_state;			/* process state - see below */
	int pt_flags;			/* flags - see below */
	struct remid pt_rid;		/* remote sys/proc id */
	struct dtmsg pt_msg;		/* buffer for message i/o */
	int pt_mdlen;			/* size of pt_msg data buffer */
	int pt_savreq;			/* client saved request code */
	long pt_seqno;			/* client request sequence number */
	time_t pt_timeout;		/* timeout time */
	char *pt_tdata;			/* ptr to transport-specific data */
	char *pt_sdata;			/* ptr to server-specific data */
};

/* short names for pt_rid members */
#define ptr_sysid	pt_rid.re_sysid
#define ptr_pid		pt_rid.re_pid
#define ptr_ptype	pt_rid.re_ptype

/* short names for pt_msg members */
#define ptm_hdr		pt_msg.dm_hdr
#define ptm_magic	pt_msg.dm_magic
#define ptm_req		pt_msg.dm_req
#define ptm_rc		pt_msg.dm_rc
#define ptm_seq		pt_msg.dm_seq
#define ptm_sysid	pt_msg.dm_sysid
#define ptm_pid		pt_msg.dm_pid
#define ptm_ptype	pt_msg.dm_ptype
#define ptm_mtype	pt_msg.dm_mtype
#define ptm_len		pt_msg.dm_len
#define ptm_data	pt_msg.dm_data

#define PT_MAGIC	0x50746142	/* ptab magic number */

#define TET_STRUCT_PTAB_DEFINED


/*
**	process states
**
**	if you add an entry here, be sure to add an entry in
**	dtet2lib/ptstate.c as well
*/
#define PS_DEAD		1	/* dead process */
#define PS_IDLE		2	/* waiting for response from remote system */
#define PS_RCVMSG	3	/* receiving a message */
#define PS_SNDMSG	4	/* sending a message */
#define PS_PROCESS	5	/* processing a message */
#define PS_WAITSYNC	6	/* waiting for sync */
#define PS_CONNECT	7	/* connecting to remote system */

/*
**	values for pt_flags (a bit field)
**
**	if you add an entry here, be sure to add an entry in
**	dtet2lib/ptflags.c as well
*/
#define PF_ATTENTION	000001		/* process needs servicing */
#define PF_INPROGRESS	000002		/* message i/o started */
#define PF_IODONE	000004		/* message i/o complete */
#define PF_IOERR	000010		/* error during message i/o */
#define PF_TIMEDOUT	000020		/* timeout expired */
#define PF_SERVER	000040		/* process is a server, not client */
#define PF_CONNECTED	000100		/* connected to server */
#define PF_LOGGEDON	000200		/* logged on */
#define PF_LOGGEDOFF	000400		/* logged off */
#define PF_RCVHDR	001000		/* receiving message header */
#define PF_SNDHDR	002000		/* sending message header (FIFO) */
#define PF_NBIO		004000		/* using non-blocking i/o */
#define PF_SERVWAIT	010000		/* in tet_si_servwait() */

/* timeout values for tet_ts_poll() calls */
#define LONGDELAY	3600		/* if proc table is not empty */
#define SHORTDELAY	60		/* if proc table is empty */


/* pointer to the head of the per-process information table */
extern struct ptab *tet_ptab;

/* pointers to ptab elements for SYNCD and XRESD */
TET_EXPORT_DATA(struct ptab *, tet_sdptab);
TET_EXPORT_DATA(struct ptab *, tet_xdptab);


/* extern function declarations */
extern void		tet_fiodead PROTOLIST((struct ptab *));
extern struct ptab *	tet_getnextptbyptype PROTOLIST((int, struct ptab *));
extern struct ptab *	tet_getptbysyspid PROTOLIST((int, long));
extern struct ptab *	tet_getptbysysptype PROTOLIST((int, int));
extern void		tet_op_fclose PROTOLIST((struct ptab *));
extern void		tet_op_fopen PROTOLIST((struct ptab *));
extern void		tet_op_fwrite PROTOLIST((struct ptab *));
extern void		tet_op_gets PROTOLIST((struct ptab *));
extern void		tet_op_puts PROTOLIST((struct ptab *));
extern void		tet_ptadd PROTOLIST((struct ptab *));
TET_IMPORT_FUNC(struct ptab *, tet_ptalloc, PROTOLIST((void)));
extern void		tet_ptfree PROTOLIST((struct ptab *));
extern void		tet_ptrm PROTOLIST((struct ptab *));
TET_IMPORT_FUNC(char *, tet_r2a, PROTOLIST((struct remid *)));
extern void		tet_si_clientloop PROTOLIST((struct ptab *, int));
extern void		tet_si_serverproc PROTOLIST((struct ptab *));
extern void		tet_si_service PROTOLIST((struct ptab *));
extern void		tet_si_servwait PROTOLIST((struct ptab *, int));
extern void		tet_so_dead PROTOLIST((struct ptab *));
extern int		tet_ti_logoff PROTOLIST((struct ptab *, int));
extern int		tet_ti_logon PROTOLIST((struct ptab *));
TET_IMPORT_FUNC(char *, tet_ti_msgbuf, PROTOLIST((struct ptab *, int)));
extern int		tet_ti_talk PROTOLIST((struct ptab *, int));

#endif /* -END-LITE-CUT- */

