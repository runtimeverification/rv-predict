/*
 *      SCCS:  @(#)tsinfo_in.h	1.7 (98/08/28) 
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

SCCS:   	@(#)tsinfo_in.h	1.7 98/08/28 TETware release 3.8
NAME:		tsinfo_in.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	a header file describing the structure of the DTET interprocess
	INET transport-specific information message

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
************************************************************************/

/*
**	structure of an INET OP_TSINFO request message
**
**	NOTE:
**	if you change this structure, be sure to update the element sizes
**	and initialisation code defined below, and change the version
**	number in dtmsg.h as well
**
**	(Internet address and port number are stored in host byte order)
*/

struct tsinfo {
	short ts_ptype;			/* process type */
	long ts_addr;			/* Internet address */
	unsigned short ts_port;		/* port number */
};

/* tsinfo element positions for use on machine-independent data streams */
#define TS_PTYPE	0
#define TS_ADDR		(TS_PTYPE + SHORTSIZE)
#define TS_PORT		(TS_ADDR + LONGSIZE)
#define TS_TSINFOSZ	(TS_PORT + SHORTSIZE)

#if TET_LDST
/* tsinfo structure description */
#define TSINFO_DESC	{ ST_SHORT(1),	TS_PTYPE }, \
			{ ST_LONG(1),	TS_ADDR }, \
			{ ST_USHORT(1),	TS_PORT }

/* stdesc initialisation for tsinfo structure */
#define TSINFO_INIT(st, sp, n, nst) \
			st[n++].st_stoff = (int) &sp->ts_ptype; \
			st[n++].st_stoff = (int) &sp->ts_addr; \
			st[n++].st_stoff = (int) &sp->ts_port; \
			nst = n;
#endif


/* extern function declarations */
extern int tet_bs2tsinfo PROTOLIST((char *, int, struct tsinfo **, int *));
TET_IMPORT_FUNC(int, tet_tsinfo2bs, PROTOLIST((struct tsinfo *, char *)));

