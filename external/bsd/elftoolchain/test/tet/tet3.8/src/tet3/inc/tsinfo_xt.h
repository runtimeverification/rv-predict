/*
 *      SCCS:  @(#)tsinfo_xt.h	1.7 (98/08/28) 
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

SCCS:   	@(#)tsinfo_xt.h	1.7 98/08/28 TETware release 3.8
NAME:		tsinfo_xt.h
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Ltd.
DATE CREATED:	April 1993

DESCRIPTION:
	a header file describing the structure of the DTET interprocess
	XTI transport-specific information message

MODIFICATIONS:
	Denis McConalogue, UniSoft Limited, September 1993
	remove prototypes for gettccdaddr() and ts_nbio().

	Andrew Dingwall, UniSoft Ltd., March 1997
	remove #ifndef __hpux from #include <arpa/inet.h>
	since current HP-UX implementations now have this file

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

/*
**	structures of OP_TSINFO request messages for the transport interfaces
**	supported by the XTI based DTET.
**
**	NOTE:
**	if you change this structure, be sure to update the element sizes
**	and initialisation code defined below, and change the version
**	number in dtmsg.h as well
**
**	(Internet address and port number are stored in *network* byte order)
*/

extern void tet_ts_accept PROTOLIST((int));
extern void tet_ts_listen PROTOLIST((int));


#define TPI_TCP         1
#define TPI_OSICO       2
#define TPI_OSICL       3

#ifdef TCPTPI
#  include <netinet/in.h>
#  include <sys/socket.h>
#  include <arpa/inet.h>
#endif

struct tsinfo_inet {
	long ts_addr;			/* Internet address 		*/
	unsigned short ts_port;		/* port number 			*/
};
struct tsinfo_osico {
	unsigned short	ts_len;
	char		ts_nsap[MAX_ADDRL];
};

struct tsinfo {
	short ts_ptype;			/* process type 		*/
	union {
	      struct tsinfo_inet   inet;
	      struct tsinfo_osico  osico;
	} ts;				/* transport specific stuff 	*/
};


/* tsinfo element positions for use on machine-independent data streams */
#define TS_INET_PTYPE		0
#define TS_INET_ADDR		(TS_INET_PTYPE + SHORTSIZE)
#define TS_INET_PORT		(TS_INET_ADDR + LONGSIZE)
#define TS_INET_TSINFOSZ	(TS_INET_PORT + SHORTSIZE)

#define TS_OSICO_PTYPE		0
#define TS_OSICO_LEN		(TS_OSICO_PTYPE + SHORTSIZE)
#define TS_OSICO_NSAP		(TS_OSICO_LEN + SHORTSIZE)
#define TS_OSICO_TSINFOSZ	(TS_OSICO_NSAP + MAX_ADDRL)

#if TET_LDST
/* tsinfo structure description */
#define TSINFO_INET_DESC { ST_SHORT(1),	TS_INET_PTYPE }, \
			 { ST_LONG(1),	TS_INET_ADDR }, \
			 { ST_USHORT(1),TS_INET_PORT }

#define TSINFO_OSICO_DESC { ST_SHORT(1),TS_OSICO_PTYPE }, \
			 { ST_SHORT(1),	TS_OSICO_LEN }, \
			 { ST_CHAR(MAX_ADDRL), TS_OSICO_NSAP }

/* stdesc initialisation for tsinfo structure */
#define TSINFO_INET_INIT(st, sp, n, nst) \
			st[n++].st_stoff = (int) &sp->ts_ptype; \
			st[n++].st_stoff = (int) &sp->ts.inet.ts_addr; \
			st[n++].st_stoff = (int) &sp->ts.inet.ts_port; \
			nst = n;
#define TSINFO_OSICO_INIT(st, sp, n, nst) \
			st[n++].st_stoff = (int) &sp->ts_ptype; \
			st[n++].st_stoff = (int) &sp->ts.osico.ts_len; \
			st[n++].st_stoff = (int) &sp->ts.osico.ts_nsap; \
			nst = n;
#endif


/* extern function declarations */
extern int tet_bs2tsinfo PROTOLIST((char *, int, struct tsinfo **, int *));
TET_IMPORT_FUNC(int, tet_tsinfo2bs, PROTOLIST((struct tsinfo *, char *)));

