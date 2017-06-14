/*
 *      SCCS:  @(#)tcm_xt.c	1.9 (99/09/03) 
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

#ifndef lint
static char sccsid_tcm_xt[] = "@(#)tcm_xt.c	1.9 (99/09/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcm_xt.c	1.9 99/09/03 TETware release 3.8
NAME:		tcm_xt.c
PRODUCT:	TETware
AUTHOR:		Denis McConalogue, UniSoft Ltd.
DATE CREATED:	April 1993

DESCRIPTION:
	client-specific functions for tcm XTI version

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., December 1993
	added malloc tracing
	added ptm_mtype assignment

	Geoff Clare, UniSoft Ltd., Oct 1996
	restructured tcm source to avoid "ld -r"

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
	Andrew Dingwall, UniSoft Ltd., July 1999
	moved TCM code out of the API library

************************************************************************/

/*
** This file is a component of the TCM (tcm.o) and/or one of the child
** process controllers (tcmchild.o and tcmrem.o).
** On UNIX systems, these .o files are built using ld -r.
** There is no equivalent to ld -r in MSVC, so on Win32 systems each .c
** file is #included in a scratch .c or .cpp file and a single object
** file built from that.
**
** This imposes some restictions on the contents of this file:
**
**	+ Since this file might be included in a C++ program, all
**	  functions must have both ANSI C and common C definitions.
**
**	+ The only .h file that may appear in this file is tcmhdrs.h;
**	  all other .h files that are needed must be #included in there.
**
**	+ The scope of static variables and functions encompasses all
**	  the source files, not just this file.
**	  So all static variables and functions must have unique names.
*/


/* include the XTI-specific header files */
#define TET_TCM_XT_C

/*
** all the header files are included by tcmhdrs.h
** don't include any other header files directly
*/
#include "tcmhdrs.h"


#ifdef NEEDsrcFile
#  ifdef srcFile
#    undef srcFile
#  endif
#  define srcFile srcFile_tcm_xt
   static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* global variables */
TET_EXPORT int tet_tpi_mode = -1;	/* transport provider mode	*/
TET_EXPORT char *tet_tpname = (char *) 0;
					/* transport provider name	*/


/* static function declarations */
static void storetsinfo PROTOLIST((char *, struct ptab **, int));


/*
**	tet_ts_tcminit() - tcm transport-specific environment argument
**		processing
*/

#ifdef PROTOTYPES
void tet_ts_tcminit(void)
#else
void tet_ts_tcminit()
#endif
{
	register char **ap;
	register char *envstring, *p;
	char **args;
	register int nargs;
	static char envname[] = "TET_TSARGS";

	/* get the dtet ts args out of the environment and count them */
	if ((envstring = getenv(envname)) == (char *) 0 || !*envstring)
		fatal(0, envname, "null or not set");
	nargs = 1;
	for (p = envstring; *p; p++)
		if (isspace(*p))
			nargs++;

	/* allocate some space for argument pointers */
	errno = 0;
	if ((args = (char **) malloc(nargs * sizeof *args)) == (char **) 0)
		fatal(errno, "can't get memory for arg list", (char *) 0);
	TRACE2(tet_Tbuf, 6, "allocate ts env args = %s", tet_i2x(args));

	/* split the arg string into fields */
	nargs = tet_getargs(envstring, args, nargs);

	/* process each argument in turn */
	for (ap = args; nargs > 0; ap++, nargs--) {
		if (*(p = *ap) != '-')
			continue;
		switch (*++p) {
		case 'x':
			storetsinfo(*ap, &tet_xdptab, PT_XRESD);
			break;
		case 'y':
			storetsinfo(*ap, &tet_sdptab, PT_SYNCD);
			break;
		case 'M':
			tet_tpi_mode = atoi(*ap+2);
			break;
		case 'P':
			tet_tpname = tet_strstore(*ap+2);
			break;
		default:
			fatal(0, "bad ts env argument", *ap);
			/* NOTREACHED */
		}
	}

	TRACE2(tet_Tbuf, 6, "free ts env args = %s", tet_i2x(args));
	free((char *) args);
}

/*
**	storetsinfo() - store ts info for server process
**
**	the ts info is stored indirectly through *paddr
*/

#ifdef PROTOTYPES
static void storetsinfo(char *arg, struct ptab **paddr, int ptype)
#else
static void storetsinfo(arg, paddr, ptype)
char *arg;
struct ptab **paddr;
int ptype;
#endif
{
	register struct ptab	*pp;
	register struct tptab	*tp;
	register char	*addr;
	register struct netbuf	*np;

	/* make addr point to the XTI address string */
	addr = arg + 2;
	if (!*addr)
		fatal(0, "bad ts env arg format", arg);

	/* get a ptab structure and fill it in */
	if ((pp = tet_ptalloc()) == (struct ptab *) 0)
		exit(1);
	
	pp->ptr_sysid = 0;
	pp->ptr_ptype = ptype;
	pp->pt_flags  = PF_SERVER;
	tp = (struct tptab *) pp->pt_tdata;

	/* fill in the XTI address structure */
	if ((np = tet_lname2addr(addr)) == (struct netbuf *)0)
		fatal(0, "bad format XTI address", arg);

	errno = 0;
	if ((tp->tp_call.buf = (char *) malloc(np->maxlen)) == (char *) 0)
		fatal(errno, "can't allocate address buffer", (char *) 0);

	TRACE2(tet_Tbuf, 6, "allocate tp_call.buf = %s",
		tet_i2x(tp->tp_call.buf));
	
	tp->tp_call.maxlen = np->maxlen;
	tp->tp_call.len    = np->len;
	(void) memcpy(tp->tp_call.buf, np->buf, np->len);

	*paddr = pp;
}

/*
**	tet_ss_tsconnect() - server-specific connect processing
**
**	return 0 if successful or -1 on error
*/

#ifdef PROTOTYPES
int tet_ss_tsconnect(struct ptab *pp)
#else
int tet_ss_tsconnect(pp)
struct ptab *pp;
#endif
{
	switch (pp->ptr_ptype) {
	case PT_SYNCD:
		if (tet_sdptab)
			return(0);
		break;
	case PT_XRESD:
		if (tet_xdptab)
			return(0);
		break;
	case PT_STCC:
		return(tet_gettccdaddr(pp));
	}

	error(0, "don't know how to connect to", tet_ptptype(pp->ptr_ptype));
	return(-1);
}

/*
**	tet_ss_tsinfo() - construct a tsinfo message relating to a server
**		process
**
**	return 0 if successful or -1 on error
*/

#ifdef PROTOTYPES
int tet_ss_tsinfo(struct ptab *pp, int ptype)
#else
int tet_ss_tsinfo(pp, ptype)
struct ptab *pp;
int ptype;
#endif
{
	register struct tptab *tp;
	register struct tsinfo *mp;
	extern int tet_tpi_mode;

	if ((mp = (struct tsinfo *) tet_ti_msgbuf(pp, sizeof *mp)) == (struct tsinfo *) 0)
		return(-1);

	/* make tp point to the tptab for the server -
		tet_sdptab and tet_xdptab were set up if the corresponding
		arguments were in the TET_TSARGS environment variable */
	tp = (struct tptab *) 0;
	switch (ptype) {
	case PT_SYNCD:
		if (tet_sdptab)
			tp = (struct tptab *) tet_sdptab->pt_tdata;
		break;
	case PT_XRESD:
		if (tet_xdptab)
			tp = (struct tptab *) tet_xdptab->pt_tdata;
		break;
	}

	if (!tp) {
		error(0, "no tsinfo for", tet_ptptype(ptype));
		return(-1);
	}

	mp->ts_ptype = ptype;

	/* all ok so copy over the data and return */
	switch (tet_tpi_mode) {
#ifdef TCPTPI
	case TPI_TCP:

	  mp->ts.inet.ts_addr =
		ntohl(((struct sockaddr_in *)tp->tp_call.buf)->sin_addr.s_addr);
	  mp->ts.inet.ts_port =
		ntohs(((struct sockaddr_in *)tp->tp_call.buf)->sin_port);
	
	  break;

#endif
#ifdef OSITPI
	case TPI_OSICO:

	  if (tp->tp_call.len > sizeof (mp->ts.osico.ts_nsap)) {
		error(0, "address too big for tsinfo buffer", (char *)0);
		return (-1);
	  }
	  mp->ts.osico.ts_len = tp->tp_call.len;
	  (void) memcpy(mp->ts.osico.ts_nsap, tp->tp_call.buf, tp->tp_call.len);

	  break;
#endif
	default:
		fatal(0,"invalid tet_tpi_mode or not supported", tet_i2a(tet_tpi_mode));
		/* NOTREACHED */
	}

	pp->ptm_mtype = MT_TSINFO_XT;
	pp->ptm_len = sizeof *mp;
	return(0);
}

/*
**	tet_tcm_ts_tsinfolen() - return length of a machine-independent tsinfo
**		structure
*/

#ifdef PROTOTYPES
int tet_tcm_ts_tsinfolen(void)
#else
int tet_tcm_ts_tsinfolen()
#endif
{
	switch (tet_tpi_mode) {
#ifdef TCPTPI
	case TPI_TCP:

		return(TS_INET_TSINFOSZ);
		break;
#endif
#ifdef OSITPI
	case TPI_OSICO:

		return(TS_OSICO_TSINFOSZ);
		break;
#endif
	default:
		fatal(0,"invalid tet_tpi_mode, or not supported", (char *)0);
		/* NOTREACHED */
	}
}

/*
**	tet_tcm_ts_tsinfo2bs() - call tet_tsinfo2bs()
*/

#ifdef PROTOTYPES
int tet_tcm_ts_tsinfo2bs(char *from, char *to)
#else
int tet_tcm_ts_tsinfo2bs(from, to)
char *from, *to;
#endif
{
	return(tet_tsinfo2bs((struct tsinfo *) from, to));
}

