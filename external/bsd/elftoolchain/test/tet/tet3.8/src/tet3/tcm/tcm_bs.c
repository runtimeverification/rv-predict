/*
 *      SCCS:  @(#)tcm_bs.c	1.14 (00/04/03) 
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

#ifndef lint
static char sccsid_tcm_bs[] = "@(#)tcm_bs.c	1.14 (00/04/03) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tcm_bs.c	1.14 00/04/03 TETware release 3.8
NAME:		tcm_bs.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	tcm-specific functions to convert DTET interprocess messages between
	machine-independent and internal format

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., August 1996
	added support for OP_TIME

	Geoff Clare, UniSoft Ltd., Oct 1996
	restructured tcm source to avoid "ld -r"

	Andrew Dingwall, UniSoft Ltd., June 1997
	added support for OP_XRSEND

	Andrew Dingwall, UniSoft Ltd., July 1998
	added support for OP_PUTENV

	Andrew Dingwall, UniSoft Ltd., June 1999
	added support for OP_ACCESS
	(used by tet_remexec())

	Andrew Dingwall, UniSoft Ltd., July 1999
	moved TCM code out of the API library

	Andrew Dingwall, UniSoft Ltd., March 2000
	added support for OP_SETCONF

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


/* include the bytestream-specific header files */
#define TET_TCM_BS_C

/*
** all the header files are included by tcmhdrs.h
** don't include any other header files directly
*/
#include "tcmhdrs.h"


#ifdef NEEDsrcFile
#  ifdef srcFile
#    undef srcFile
#  endif
#  define srcFile srcFile_tcm_bs
   static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


static char reqerr[] = "unknown request code";

/*
**	tet_ss_bs2md() - convert message data to internal format
**
**	return length of internal-format message, or -ve error code on error
*/

#ifdef PROTOTYPES
int tet_ss_bs2md(char *from, struct ptab *pp)
#else
int tet_ss_bs2md(from, pp)
char *from;
struct ptab *pp;
#endif
{
	register int rc;
	register int request = pp->pt_savreq;

	switch (request) {
	case OP_EXEC:
	case OP_WAIT:
	case OP_KILL:
	case OP_XROPEN:
	case OP_SNGET:
	case OP_FOPEN:
	case OP_TIME:
		rc = tet_bs2valmsg(from, pp->ptm_len,
			(struct valmsg **) &pp->ptm_data, &pp->pt_mdlen);
		break;
	case OP_ASYNC:
	case OP_USYNC:
		rc = tet_bs2synmsg(from, pp->ptm_len,
			(struct valmsg **) &pp->ptm_data, &pp->pt_mdlen);
		break;
	case OP_RCFNAME:
	case OP_GETS:
	case OP_RCVCONF:
	case OP_SHARELOCK:
		rc = tet_bs2avmsg(from, pp->ptm_len,
			(struct avmsg **) &pp->ptm_data, &pp->pt_mdlen);
		break;
	default:
		error(0, reqerr, tet_ptreqcode(request));
		return(ER_REQ);
	}

	return(rc < 0 ? ER_ERR : rc);
}

/*
**	tet_ss_md2bs() - convert message data to machine-independent format
**
**	return length of machine-independent message
**	or -ve error code on error
*/

#ifdef PROTOTYPES
int tet_ss_md2bs(struct ptab *pp, char **bp, int *lp, int offs)
#else
int tet_ss_md2bs(pp, bp, lp, offs)
struct ptab *pp;
char **bp;
int *lp, offs;
#endif
{
	register char *mp = pp->ptm_data;
	register int request = pp->ptm_req;
	register int len, rc;

	/* calculate outgoing data size */
	switch (request) {
	case OP_SYSID:
	case OP_SYSNAME:
	case OP_SNSYS:
	case OP_XRSYS:
	case OP_XRSEND:
	case OP_ICSTART:
	case OP_TPSTART:
	case OP_ICEND:
	case OP_TPEND:
	case OP_RESULT:
	case OP_FCLOSE:
	case OP_GETS:
	case OP_WAIT:
	case OP_KILL:
	case OP_SETCONF:
		len = VM_VALMSGSZ(((struct valmsg *) mp)->vm_nvalue);
		break;
	case OP_ASYNC:
	case OP_USYNC:
		len = VM_SYNMSGSZ(((struct valmsg *) mp)->vm_nvalue, VM_MSDLEN((struct valmsg *) mp));
		break;
	case OP_TRACE:
	case OP_EXEC:
	case OP_XROPEN:
	case OP_XRES:
	case OP_FOPEN:
	case OP_CFNAME:
	case OP_SNDCONF:
	case OP_CONFIG:
	case OP_LOCKFILE:
	case OP_SHARELOCK:
	case OP_PUTENV:
	case OP_ACCESS:
#if TESTING
	case OP_PRINT:
#endif
		len = tet_avmsgbslen((struct avmsg *) mp);
		break;
	case OP_TSINFO:
		len = tet_tcm_ts_tsinfolen();
		break;
	default:
		error(0, reqerr, tet_ptreqcode(request));
		return(ER_REQ);
	}

	/* make sure that the receiving area is big enough */
	if (BUFCHK(bp, lp, len + offs) < 0)
		return(ER_ERR);

	/* copy the data to (*bp + offs) */
	switch (request) {
	case OP_SYSID:
	case OP_SYSNAME:
	case OP_SNSYS:
	case OP_XRSEND:
	case OP_XRSYS:
	case OP_ICSTART:
	case OP_TPSTART:
	case OP_ICEND:
	case OP_TPEND:
	case OP_RESULT:
	case OP_FCLOSE:
	case OP_GETS:
	case OP_WAIT:
	case OP_KILL:
	case OP_SETCONF:
		rc = tet_valmsg2bs((struct valmsg *) mp, *bp + offs);
		break;
	case OP_ASYNC:
	case OP_USYNC:
		rc = tet_synmsg2bs((struct valmsg *) mp, *bp + offs);
		break;
	case OP_EXEC:
	case OP_TRACE:
	case OP_XROPEN:
	case OP_XRES:
	case OP_FOPEN:
	case OP_CFNAME:
	case OP_SNDCONF:
	case OP_CONFIG:
	case OP_LOCKFILE:
	case OP_SHARELOCK:
	case OP_PUTENV:
	case OP_ACCESS:
#if TESTING
	case OP_PRINT:
#endif
		rc = tet_avmsg2bs((struct avmsg *) mp, *bp + offs);
		break;
	case OP_TSINFO:
		rc = tet_tcm_ts_tsinfo2bs(mp, *bp + offs);
		break;
	default:
		error(0, reqerr, tet_ptreqcode(request));
		return(ER_REQ);
	}

	return(rc < 0 ? ER_ERR : rc);
}

