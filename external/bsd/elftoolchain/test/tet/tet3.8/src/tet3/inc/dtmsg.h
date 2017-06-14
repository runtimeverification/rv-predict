/*
 *      SCCS:  @(#)dtmsg.h	1.22 (03/03/26) 
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

SCCS:   	@(#)dtmsg.h	1.22 03/03/26 TETware release 3.8
NAME:		dtmsg.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	common definitions relating to all DTET interprocess messages

	the value of DTM_VERSION is defined here - care must be taken to
	ensure that this value is incremented WHENEVER the format of any
	DTET interprocess message changes

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, August 1993
	Added defines for OP_RCOPY message request code

	Denis McConalogue, UniSoft Limited, September 1993
	Added defines for OP_XRCLOSE message request code

	Andrew Dingwall, UniSoft Ltd., November 1993
	added support for the FIFO transport interface

	Andrew Dingwall, UniSoft Ltd., October 1994
	added support for tet_msync()

	Geoff Clare/Andrew Dingwall, UniSoft Ltd., July 1996
	Changes for TETWare.

	Andrew Dingwall, UniSoft Ltd., September 1996
	added support for OP_RMALLDIRS

	Geoff Clare, UniSoft Ltd., Oct 1996
	Portability fixes.

	Andrew Dingwall, UniSoft Ltd., June 1997
	added support for OP_XRSEND;
	moved struct dtmhdr outside of struct dtmsg
	(it seems that some C++ compilers prefer it that way!)

	Andrew Dingwall, The Open Group, March 2003
	Added support for OP_FWRITE, OP_UTIME, OP_TSFTYPE and OP_FTIME.


************************************************************************/

/*
**	common message header element - all TETware interprocess messages
**	start with this header
**	
**	NOTE:
**	if you change this structure, be sure to update the element sizes
**	and initialisation code defined below, and change the version
**	number as well
*/

/* dtmsg version number - used to generate message magic number */
#define DTM_VERSION	6

/* structure of a TETware message header in internal format */
struct dtmhdr {
	long dmh_magic;		/* magic number - must be 1st */
	short dmh_req;		/* request code - must be 2nd */
	short dmh_rc;		/* return code - must be 3rd */
	long dmh_seq;		/* message sequence number */
	short dmh_sysid;	/* system code */
	long dmh_pid;		/* pid on that system */
	short dmh_ptype;	/* process type - see below */
	short dmh_mtype;	/* message type - see below */
	short dmh_len;		/* data length */
};

/* structure of a TETware message in internal format */
struct dtmsg {
	struct dtmhdr dm_hdr; 		/* message header */
	char *dm_data;			/* ptr to msg data */
};

/* short names for dm_hdr members */
#define dm_magic	dm_hdr.dmh_magic
#define dm_req		dm_hdr.dmh_req
#define dm_rc		dm_hdr.dmh_rc
#define dm_seq		dm_hdr.dmh_seq
#define dm_sysid	dm_hdr.dmh_sysid
#define dm_pid		dm_hdr.dmh_pid
#define dm_ptype	dm_hdr.dmh_ptype
#define dm_mtype	dm_hdr.dmh_mtype
#define dm_len		dm_hdr.dmh_len

/* sizes of objects in machine-independent messages */
#define SHORTSIZE	2
#define LONGSIZE	4

/* dtmhdr element positions for use on machine-independent byte streams */
#define DM_MAGIC	0
#define DM_REQ		(DM_MAGIC + LONGSIZE)
#define DM_RC		(DM_REQ + SHORTSIZE)
#define DM_SEQ		(DM_RC + SHORTSIZE)
#define DM_SYSID	(DM_SEQ + LONGSIZE)
#define DM_PID		(DM_SYSID + SHORTSIZE)
#define DM_PTYPE	(DM_PID + LONGSIZE)
#define DM_MTYPE	(DM_PTYPE + SHORTSIZE)
#define DM_LEN		(DM_MTYPE + SHORTSIZE)
#define DM_HDRSZ	(DM_LEN + SHORTSIZE)

#if TET_LDST
/* dtmhdr structure description */
#define DTMHDR_DESC	{ ST_LONG(1),	DM_MAGIC }, \
			{ ST_SHORT(1),	DM_REQ }, \
			{ ST_SHORT(1),	DM_RC }, \
			{ ST_LONG(1),	DM_SEQ }, \
			{ ST_SHORT(1),	DM_SYSID }, \
			{ ST_LONG(1),	DM_PID }, \
			{ ST_SHORT(1),	DM_PTYPE }, \
			{ ST_SHORT(1),	DM_MTYPE }, \
			{ ST_SHORT(1),	DM_LEN }

/* stdesc initialisation for dtmhdr structure */
#define DTMHDR_INIT(st, hp, n, nst, stlen) \
		st[n++].st_stoff = (char *) &hp->dmh_magic - (char *) hp; \
		st[n++].st_stoff = (char *) &hp->dmh_req - (char *) hp; \
		st[n++].st_stoff = (char *) &hp->dmh_rc - (char *) hp; \
		st[n++].st_stoff = (char *) &hp->dmh_seq - (char *) hp; \
		st[n++].st_stoff = (char *) &hp->dmh_sysid - (char *) hp; \
		st[n++].st_stoff = (char *) &hp->dmh_pid - (char *) hp; \
		st[n++].st_stoff = (char *) &hp->dmh_ptype - (char *) hp; \
		st[n++].st_stoff = (char *) &hp->dmh_mtype - (char *) hp; \
		stlen = n; \
		st[n++].st_stoff = (char *) &hp->dmh_len - (char *) hp; \
		nst = n;
#endif

/* generate a message magic number */
#define DTM_MAGIC	((long) 't' << 24 | LONGSIZE << 20 | \
				SHORTSIZE << 16 | DTM_VERSION)

/*
**	message request codes - all +ve values
**
**	you may add new values but please don't change or delete
**	existing values unless you also increment DTM_VERSION above
**
**	if you add a new value here, be sure to add an entry in
**	dtet2lib/reqcode.c as well
*/
#define OP_LOGON	1	/* log on to server */ 
#define OP_LOGOFF	2	/* log off server */ 
#define OP_NULL		3	/* discard data successfully */ 
#define OP_SNGET	4	/* get snid for an auto sync session */ 
#define OP_SNSYS	5	/* send auto sync system name list */ 
#define OP_ASYNC	6	/* auto sync request */ 
#define OP_USYNC	7	/* user sync request */ 
#define OP_SYSID	8	/* assign system id (to tccd) */ 
#define OP_SYSNAME	9	/* send system name list (to tccd) */ 
#define OP_TSINFO	10	/* send transport-specific information */ 
#define OP_TRACE	11	/* send trace flags */ 
#define OP_EXEC		12	/* exec a process */ 
#define OP_WAIT		13	/* wait for a process */ 
#define OP_KILL		14	/* send signal to process */ 
#define OP_XROPEN	15	/* open xres file */ 
#define OP_XRSYS	16	/* send xres system name list */ 
#define OP_ICSTART	17	/* signal IC start (to xresd) */ 
#define OP_TPSTART	18	/* signal TP start (to xresd) */ 
#define OP_ICEND	19	/* signal IC end (to xresd) */ 
#define OP_TPEND	20	/* signal TP end (to xresd) */ 
#define OP_XRES		21	/* send data for xres file */ 
#define OP_RESULT	22	/* send result for xres file */ 
#define OP_CFNAME	23	/* register config file name */ 
#define OP_RCFNAME	24	/* return config file name */ 
#define OP_SNDCONF	25	/* send config info to tccd */ 
#define OP_RCVCONF	26	/* receive config info from tccd */ 
#define OP_CONFIG	27	/* assign config variables */ 
#define OP_TFOPEN	28	/* open transfer file */ 
#define OP_TFCLOSE	29	/* close transfer file */ 
#define OP_TFWRITE	30	/* write to transfer file */ 
#define OP_PUTENV	31	/* put strings in the environment */ 
#define OP_ACCESS	32	/* check accessibility of a file */
#define OP_MKDIR	33	/* make a directory */ 
#define OP_RMDIR	34	/* remove a directory */ 
#define OP_CHDIR	35	/* change directory */ 
#define OP_FOPEN	36	/* open a text or binary file for writing */ 
#define OP_FCLOSE	37	/* close a text or binary file */ 
#define OP_GETS		38	/* read strings from a text file */ 
#define OP_PUTS		39	/* write strings to a text file */ 
#define OP_LOCKFILE	40	/* create a lock file */ 
#define OP_SHARELOCK	41	/* create a shared lock */
#define OP_MKTMPDIR	42	/* make a temp sub directory */ 
#define OP_UNLINK	43	/* unlink a file */ 
#define OP_RXFILE	44	/* remote file transfer */ 
#define OP_MKSDIR	45	/* make save files directory */
#define OP_TSFILES	46	/* transfer save files */
#define OP_CODESF	47	/* send tet codes file name to XRESD */
#define OP_RCOPY 	48	/* recursive copy files from source to dest */
#define OP_XRCLOSE	49	/* close xres file */ 
#define OP_CONNECT	50	/* FIFO tslib connect message */
#define OP_ATTENTION	51	/* FIFO tslib attention message */
#define OP_SETCONF	52	/* set tccd configuration mode */
#define OP_MKALLDIRS	53	/* make directories recursively */
#define OP_TIME		54	/* return the system time */
#define OP_RMALLDIRS	55	/* remove directories recursively */
#define OP_SNRM		56	/* remove a snid (auto sync session) */ 
#define OP_XRSEND	57	/* send XRID to XRESD after logon */
#define OP_FWRITE	58	/* write to a text or binary file */
#define OP_UTIME	59	/* set file access and mod times */
#define OP_TSFTYPE	60	/* define transfer save file types */
#define OP_FTIME	61	/* return file access and mod times */

#if TESTING
#define OP_PRINT	99	/* print request for testing */
#endif

/*
**	message return codes - all -ve values (except ER_OK which should be 0)
**
**	the first `block' of values have (+ve) equivalents in tet_api.h,
**	and these must be kept in sync
**
**      you may add new values but please don't change or delete
**	existing values unless you also increment DTM_VERSION above
**
**	if you add a new value here, be sure to add an entry in
**	dtet2lib/repcode.c as well
*/
#define ER_OK		0	/* ok - success */
#define ER_ERR		-1	/* general error code */
#define ER_MAGIC	-2	/* bad magic number */
#define ER_LOGON	-3	/* not logged on */
#define ER_RCVERR	-4	/* receive message error */
#define ER_REQ		-5	/* unknown request code */
#define ER_TIMEDOUT	-6	/* request timed out */
#define ER_DUPS		-7	/* request contained duplicate IDs */
#define ER_SYNCERR	-8	/* sync completed unsuccessfully */
#define ER_INVAL	-9	/* invalid request parameter */
#define ER_TRACE	-10	/* tracing not configured */
#define ER_WAIT		-11	/* process not terminated */
#define ER_XRID		-12	/* bad xrid in xresd request */
#define ER_SNID		-13	/* bad snid in syncd request */
#define ER_SYSID	-14	/* sysid not in system name list */
#define ER_INPROGRESS	-15	/* event in progress */
#define ER_DONE		-16	/* event finished or already happened */
#define ER_CONTEXT	-17	/* request out of context */
#define ER_PERM		-18	/* priv request/kill error */
#define ER_FORK		-19	/* can't fork */
#define ER_NOENT	-20	/* no such file or directory */
#define ER_PID		-21	/* no such process */
#define ER_SIGNUM	-22	/* bad signal number */
#define ER_FID		-23	/* bad file id */
#define ER_INTERN	-24	/* server internal error */
#define ER_ABORT	-25	/* abort TCM on TP end */
#define ER_2BIG		-26	/* argument list too long */

/*
** common unix errno equivalents -
** used in conjunction with tet_maperrno() and tet_unmaperrno()
*/
#define ER_ERRNO_BASE	-100	/* errno base */
#define ER_EPERM	-101	/* EPERM */
#define ER_ENOENT	-102	/* ENOENT */
#define ER_ESRCH	-103	/* ESRCH */
#define ER_EINTR	-104	/* EINTR */
#define ER_EIO		-105	/* EIO */
#define ER_ENXIO	-106	/* ENXIO */
#define ER_E2BIG	-107	/* E2BIG */
#define ER_ENOEXEC	-108	/* ENOEXEC */
#define ER_EBADF	-109	/* EBADF */
#define ER_ECHILD	-110	/* ECHILD */
#define ER_EAGAIN	-111	/* EAGAIN */
#define ER_ENOMEM	-112	/* ENOMEM */
#define ER_EACCES	-113	/* EACCES */
#define ER_EFAULT	-114	/* EFAULT */
#define ER_ENOTBLK	-115	/* ENOTBLK */
#define ER_EBUSY	-116	/* EBUSY */
#define ER_EEXIST	-117	/* EEXIST */
#define ER_EXDEV	-118	/* EXDEV */
#define ER_ENODEV	-119	/* ENODEV */
#define ER_ENOTDIR	-120	/* ENOTDIR */
#define ER_EISDIR	-121	/* EISDIR */
#define ER_EINVAL	-122	/* EINVAL */
#define ER_ENFILE	-123	/* ENFILE */
#define ER_EMFILE	-124	/* EMFILE */
#define ER_ENOTTY	-125	/* ENOTTY */
#define ER_ETXTBSY	-126	/* ETXTBSY */
#define ER_EFBIG	-127	/* EFBIG */
#define ER_ENOSPC	-128	/* ENOSPC */
#define ER_ESPIPE	-129	/* ESPIPE */
#define ER_EROFS	-130	/* EROFS */
#define ER_EMLINK	-131	/* EMLINK */
#define ER_EPIPE	-132	/* EPIPE */
#define ER_ENOTEMPTY	-133	/* ENOTEMPTY */

/* see if a server reply code represents an errno value */
#define IS_ER_ERRNO(x)	((x) < ER_ERRNO_BASE)


/*
**	process types - used in dm_ptype and in server tables
**
**      you may add new values but please don't change or delete
**	existing values unless you also increment DTM_VERSION above
**
**	if you add a new value here, be sure to add an entry in
**	dtet2lib/ptype.c as well
*/
#define PT_NOPROC	1	/* no process */
#define PT_MTCC		2	/* master tcc */
#define PT_STCC		3	/* slave tcc */
#define PT_MTCM		4	/* master tcm */
#define PT_STCM		5	/* slave tcm */
#define PT_XRESD	6	/* xres daemon */
#define PT_SYNCD	7	/* sync daemon */
#define PT_STAND	8	/* stand-alone programs */


/*
**	message data types - used in dm_mtype
**	only used by the FIFO transport interface for now,
**	but will be used by all transport interfaces one day
**
**      you may add new values but please don't change or delete
**	existing values unless you also increment DTM_VERSION above
*/
#define MT_UNKNOWN	1	/* initial message type - set in sproc.c */
#define MT_NODATA	2	/* message contains no data (dm_len == 0) */
#define MT_AVMSG	3	/* character string message (avmsg.h) */
#define MT_VALMSG	4	/* numeric value message (valmsg.h) */
#define MT_BTMSG	5	/* binary transfer message (btmsg.h) */
#define MT_TSINFO_IN	6	/* INET tsinfo message (tsinfo_in.h) */
#define MT_TSINFO_XT	7	/* XTI tsinfo message (tsinfo_xt.h) */
#define MT_TSINFO_FI	8	/* FIFO tsinfo message (tsinfo_fi.h) */
#define MT_SYNMSG	9	/* numeric values with USYNC message data */


/* timeout times for server communication requests */
#define LO_DELAY	120	/* for logon requests */
#define TALK_DELAY	60	/* for other immediate return requests */


/* extern function declarations */
extern int tet_bs2dtmhdr PROTOLIST((char *, struct dtmhdr *, int));
extern int tet_dtmhdr2bs PROTOLIST((struct dtmhdr *, char *));
extern int tet_dmlen2bs PROTOLIST((int, char *));

