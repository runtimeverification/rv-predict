/*
 *      SCCS:  @(#)valmsg.h	1.13 (03/03/26) 
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

SCCS:   	@(#)valmsg.h	1.13 03/03/26 TETware release 3.8
NAME:		valmsg.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	a header file describing the structure and usage of the
	DTET interprocess numeric value message

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, September 1993
	added support for OP_XRCLOSE message request code

	Andrew Dingwall, UniSoft Ltd., October 1994
	added support for message data in OP_USYNC messages

	Geoff Clare, UniSoft Ltd., Oct 1996
	Portability fixes.

	Andrew Dingwall, UniSoft Ltd., June 1997
	added support for OP_XRSEND request

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.

	Andrew Dingwall, The Open Group, March 2003
	Added support for OP_FTIME.

 
************************************************************************/

/*
**	structure of a prototypical numeric value message
**
**	NOTE:
**	if you change this structure, be sure to update the element sizes
**	and initialisation code defined below, and change the version
**	number in dtmsg.h as well
*/

struct valmsg {
	unsigned short vm_nvalue;	/* number of elements in vm_value */
	long vm_value[1];		/* start of array of values */
};

/* valmsg element positions for use on machine-independent data streams */
/* fixed part */
#define VM_NVALUE	0
#define VM_VALUESTART	(VM_NVALUE + SHORTSIZE)
/* variable part */
#define VM_VALUE	0
#define VM_VALUESZ	(VM_VALUE + LONGSIZE)

/* size of machine-independent message containing n vm_value elements */
#define VM_VALMSGSZ(n)	(VM_VALUESTART + ((n) * VM_VALUESZ))

/* size of a valmsg structure containing n vm_value elements */
#define valmsgsz(n)	((int) (sizeof (struct valmsg) + \
				(sizeof (long) * ((n) - 1))))

/* a user sync message consists of a valmsg containing n vm_value
	elements followed by m bytes of opaque data */
#define VM_SYNMSGSZ(n, m)	(VM_VALMSGSZ(n) + (int) (m))
#define synmsgsz(n, m)		(valmsgsz(n) + (int) (m))

#if TET_LDST
/* valmsg structure description */
#define VALMSG_DESC	{ ST_USHORT(1),	VM_NVALUE }, \
			{ ST_LONG(1),	VM_VALUE }

/* stdesc initialisation for valmsg structure */
#define VALMSG_INIT(st, sp, n, nst, fixed) \
		st[n++].st_stoff = (char *) &sp->vm_nvalue - (char *) sp; \
		fixed = n; \
		st[n++].st_stoff = (char *) &sp->vm_value[0] - (char *) sp; \
		nst = n;
#endif


/*
**	definitions of valmsg elements and message sizes for use with various
**		requests
**
**	if you change any of these definitions, be sure to increment the
**	version number in dtmsg.h as well
**	please be very careful when changing existing definitions,
**		particularly those used by more than one request
*/

/* OP_SYSNAME requests n sysname items */
#define VM_SYSNAME(mp, n)	((mp)->vm_value[(n)])
#define OP_SYSNAME_NSNAME(mp)	((int) (mp)->vm_nvalue)
#define OP_SYSNAME_NVALUE(n)	(n)

/* OP_SYSID requests a sysid */
#define VM_SYSID(mp)		((mp)->vm_value[0])
#define OP_SYSID_NVALUE		1

/* OP_EXEC returns a pid 
   OP_WAIT requests a pid and a timeout, and returns a status 
   OP_KILL requests a pid and a signum */
#define VM_PID(mp)		((mp)->vm_value[0])
#define VM_WTIMEOUT(mp)		((mp)->vm_value[1])
#define VM_SIGNUM(mp)		((mp)->vm_value[1])
#define VM_STATUS(mp)		((mp)->vm_value[1])
#define OP_EXEC_NVALUE		1
#define OP_WAIT_NVALUE		2
#define OP_KILL_NVALUE		2

/* OP_XROPEN returns an xrid 
   OP_XRSEND requests an xrid
   OP_XRSYS requests an xrid and n sysids */
#define VM_XRID(mp)		((mp)->vm_value[0])
#define VM_XRFIXED		1
#define VM_XSYSID(mp, n)	((mp)->vm_value[VM_XRFIXED + (n)])
#define OP_XRSYS_NSYS(mp)	((int) (mp)->vm_nvalue - VM_XRFIXED)
#define OP_XROPEN_NVALUE	VM_XRFIXED
#define OP_XRSEND_NVALUE	VM_XRFIXED
#define OP_XRSYS_NVALUE(n)	(VM_XRFIXED + (n))

/* OP_XRCLOSE requests an xrid */
/* #define VM_XRID(mp)		((mp)->vm_value[0]) in OP_XROPEN above */
#define OP_XRCLOSE_NVALUE	1

/* OP_TFOPEN returns an xfid
   OP_TFCLOSE requests an xfid */
#define VM_XFID(mp)		((mp)->vm_value[0])
#define OP_TFOPEN_NVALUE	1
#define OP_TFCLOSE_NVALUE	1

/* OP_SNGET returns a snid
   OP_SNRM requests a snid
   OP_SNSYS requests a snid and n sysids
   OP_ASYNC and OP_USYNC request a snid, xrid, spno, vote and timeout;
	both return n sysid/spno pairs if successful
	or n sysid/state pairs on error
   OP_USYNC requests n sysid/ptype pairs as well
   OP_USYNC requests and returns msync flags, msync data length and
	message data as well
   OP_USYNC returns message sending sysid as well */
/* #define VM_XRID(mp)		((mp)->vm_value[0]) in OP_XROPEN above */
#define VM_SNID(mp)		((mp)->vm_value[1])
#define VM_SPNO(mp)		((mp)->vm_value[2])
#define VM_SVOTE(mp)		((mp)->vm_value[3])
#define VM_STIMEOUT(mp)		((mp)->vm_value[4])
#define VM_MSFLAGS(mp)		((mp)->vm_value[5])
#define VM_MSSYSID(mp)		((mp)->vm_value[6])
#define VM_MSDLEN(mp)		((mp)->vm_value[7])
#define VM_SNFIXED		8
#define VM_SSYSID(mp, n)	((mp)->vm_value[VM_SNFIXED + ((n) * 2)])
#define VM_SPTYPE(mp, n)	((mp)->vm_value[VM_SNFIXED + ((n) * 2) + 1])
#define VM_RSPNO		VM_SPTYPE
#define VM_STATE		VM_SPTYPE
#define VM_MSDATA(mp)		((char *) &(mp)->vm_value[(mp)->vm_nvalue])
#define OP_SNSYS_NSYS(mp)	((int) ((mp)->vm_nvalue - VM_SNFIXED) / 2)
#define OP_AUSYNC_NSYS(mp)	((int) ((mp)->vm_nvalue - VM_SNFIXED) / 2)
#define OP_SNGET_NVALUE		2
#define OP_SNRM_NVALUE		2
#define OP_SNSYS_NVALUE(n)	(VM_SNFIXED + ((n) * 2))
#define OP_AUSYNC_NVALUE(n)	(VM_SNFIXED + ((n) * 2))

/* OP_ICSTART requests an xrid, an icno, an activity and a TPcount */
/* #define VM_XRID(mp)		((mp)->vm_value[0]) in OP_XROPEN above */
#define VM_ICNO(mp)		((mp)->vm_value[1])
#define VM_ACTIVITY(mp)		((mp)->vm_value[2])
#define VM_TPCOUNT(mp)		((mp)->vm_value[3])
#define OP_ICSTART_NVALUE	4

/* OP_TPSTART requests an xrid and a tpno */
/* #define VM_XRID(mp)		((mp)->vm_value[0]) in OP_XROPEN above */
#define VM_TPNO(mp)		((mp)->vm_value[1])
#define OP_TPSTART_NVALUE	2

/* OP_ICEND and OP_TPEND request an xrid */
/* #define VM_XRID(mp)		((mp)->vm_value[0]) in OP_XROPEN above */
#define OP_ICEND_NVALUE		1
#define OP_TPEND_NVALUE		1

/* OP_RESULT requests an xrid and a result */
/* #define VM_XRID(mp)		((mp)->vm_value[0]) in OP_XROPEN above */
#define VM_RESULT(mp)		((mp)->vm_value[1])
#define OP_RESULT_NVALUE	2

/* OP_FOPEN returns a file ID
   OP_FCLOSE requests a file ID
   OP_GETS requests a file ID and a number of lines */
#define VM_FID(mp)		((mp)->vm_value[0])
#define VM_NLINES(mp)		((mp)->vm_value[1])
#define OP_FOPEN_NVALUE		1
#define OP_FCLOSE_NVALUE	1
#define OP_GETS_NVALUE		2

/* OP_SETCONF requests a mode */
#define VM_MODE(mp)		((mp)->vm_value[0])
#define OP_SETCONF_NVALUE	1

/* OP_TIME returns a time */
#define VM_TIME(mp)		((mp)->vm_value[0])
#define OP_TIME_NVALUE		1

/* OP_FTIME returns an access time and a mod time */
#define VM_ATIME(mp)		((mp)->vm_value[0])
#define VM_MTIME(mp)		((mp)->vm_value[1])
#define OP_FTIME_NVALUE		2


/* extern function declarations */
TET_IMPORT_FUNC(int, tet_bs2synmsg,
	PROTOLIST((char *, int, struct valmsg **, int *)));
TET_IMPORT_FUNC(int, tet_bs2valmsg,
	PROTOLIST((char *, int, struct valmsg **, int *)));
TET_IMPORT_FUNC(int, tet_synmsg2bs, PROTOLIST((struct valmsg *, char *)));
TET_IMPORT_FUNC(int, tet_valmsg2bs, PROTOLIST((struct valmsg *, char *)));

