/*
 *      SCCS:  @(#)fio.c	1.9 (03/03/26) 
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

#ifndef lint
static char sccsid[] = "@(#)fio.c	1.9 (03/03/26) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)fio.c	1.9 03/03/26 TETware release 3.8
NAME:		fio.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	May 1992

DESCRIPTION:
	server ascii file writing functions

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., November 1993
	Enhancements for FIFO transport interface.
	Set ftab line buffer pointer to null once it has been freed.

	Andrew Dingwall, UniSoft Ltd., March 1999
	In dofclose(), mark file as closed so that it doesn't get closed
	again in ftfree().

	Andrew Dingwall, The Open Group, March 2003
	Added support for OP_FWRITE and binary file transfer

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <time.h>
#include <sys/types.h>
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "avmsg.h"
#include "btmsg.h"
#include "valmsg.h"
#include "llist.h"
#include "error.h"
#include "bstring.h"
#include "dtetlib.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	structure of the file table
**
**	the ft_next and ft_last pointers must be first and second so as
**	to allow the use of the llist routines
*/

struct ftab {
	struct ftab *ft_next;		/* ptr to next element */
	struct ftab *ft_last;		/* ptr to last element */
	long ft_id;			/* file id */
	char *ft_name;			/* file name */
	FILE *ft_fp;			/* file stream ptr */
	char *ft_line[AV_NLINE];	/* ptrs to line buffers for
					   tet_op_gets() */
	struct ptab *ft_ptab;		/* ptr to owner's ptab */
};

static struct ftab *ftab;		/* ptr to start of the file table */


/* static function declarations */
static int dofclose PROTOLIST((struct ftab *));
static void ftadd PROTOLIST((struct ftab *));
static struct ftab *ftalloc PROTOLIST((void));
static struct ftab *ftfind PROTOLIST((long));
static void ftfree PROTOLIST((struct ftab *));
static void ftlfree PROTOLIST((struct ftab *));
static void ftrm PROTOLIST((struct ftab *));
static int op_fo2 PROTOLIST((struct ptab *, struct ftab *));


/*
**	tet_op_fopen() - open a file 
*/

void tet_op_fopen(pp)
struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;
	register struct ftab *ftp;
	register char *p;

	/* do some sanity checks on the request */
	if (mp->av_argc != OP_FOPEN_ARGC ||
		(p = AV_FNAME(mp)) == (char *) 0 || !*p ||
		(p = AV_FTYPE(mp)) == (char *) 0 ||
		!(*p == 'r' || *p == 'w' || *p == 'a') ||
		!(*++p == '+' || *p == 'b' || !*p)) {
			pp->ptm_rc = ER_INVAL;
			pp->ptm_mtype = MT_NODATA;
			pp->ptm_len = 0;
			return;
	}

	/* get a new ftab for this request */
	if ((ftp = ftalloc()) == (struct ftab *) 0) {
		pp->ptm_rc = ER_ERR;
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
		return;
	}

	if ((pp->ptm_rc = op_fo2(pp, ftp)) == ER_OK) {
		ftp->ft_ptab = pp;
		ftadd(ftp);
		pp->ptm_mtype = MT_VALMSG;
		pp->ptm_len = valmsgsz(OP_FOPEN_NVALUE);
	}
	else {
		ftfree(ftp);
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
	}
}

/*
**	op_fo2() - extend the tet_op_fopen() processing
**
**	return ER_OK if successful or other ER_* error code on error
*/

static int op_fo2(pp, ftp)
struct ptab *pp;
register struct ftab *ftp;
{
	register char *dp = pp->ptm_data;

#define mp	((struct avmsg *) dp)

	/* remember the file name */
	if ((ftp->ft_name = tet_strstore(AV_FNAME(mp))) == (char *) 0)
		return(ER_ERR);

	/* make sure that the message buffer is big enough for the reply */
	if (BUFCHK(&pp->ptm_data, &pp->pt_mdlen, valmsgsz(OP_FOPEN_NVALUE)) <0)
		return(ER_ERR);
	dp = pp->ptm_data;

	/* open the file */
	if ((ftp->ft_fp = fopen(ftp->ft_name, AV_FTYPE(mp))) == NULL) {
		error(errno, "can't open", ftp->ft_name);
		return(ER_ERR);
	}

#undef mp

#define rp	((struct valmsg *) dp)

	/* all ok so fill in the reply message and return */
	VM_FID(rp) = ftp->ft_id;
	rp->vm_nvalue = OP_FOPEN_NVALUE;
	return(ER_OK);

#undef rp

}

/*
**	tet_op_fclose() - close a file
*/

void tet_op_fclose(pp)
register struct ptab *pp;
{
	register struct valmsg *mp = (struct valmsg *) pp->ptm_data;
	register struct ftab *ftp;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* find the ftab entry for this request */
	if ((ftp = ftfind(VM_FID(mp))) == (struct ftab *) 0) {
		pp->ptm_rc = ER_FID;
		return;
	}

	/* perform the close operation */
	pp->ptm_rc = dofclose(ftp);
}

/*
**	tet_op_gets() - read lines from a file,
**		replacing each newline with a '\0'
*/

void tet_op_gets(pp)
struct ptab *pp;
{
	register char *dp = pp->ptm_data;
	register struct ftab *ftp;
	register char *p;
	register int n, nlines;
	char buf[BUFSIZ];

#define mp	((struct valmsg *) dp)

	/* do a sanity check on the request */
	if (mp->vm_nvalue != OP_GETS_NVALUE ||
		(nlines = (int) VM_NLINES(mp)) < 0) {
			pp->ptm_rc = ER_INVAL;
			pp->ptm_mtype = MT_NODATA;
			pp->ptm_len = 0;
			return;
	}

	/* find the file table element for this request */
	if ((ftp = ftfind(VM_FID(mp))) == (struct ftab *) 0) {
		pp->ptm_rc = ER_FID;
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
		return;
	}

#undef mp

	/* free up any existing line buffers */
	ftlfree(ftp);

	/* read in the lines */
	nlines = TET_MIN(nlines, AV_NLINE);
	for (n = 0; n < nlines; n++) {
		if (fgets(buf, sizeof buf, ftp->ft_fp) == NULL)
			break;
		for (p = buf; *p; p++)
			if (*p == '\n') {
				*p = '\0';
				break;
			}
		if ((ftp->ft_line[n] = tet_strstore(buf)) == (char *) 0) {
			pp->ptm_rc = ER_ERR;
			pp->ptm_mtype = MT_NODATA;
			pp->ptm_len = 0;
			return;
		}
	}
	nlines = n;

	/* check for read error, return if no lines were read */
	if (ferror(ftp->ft_fp)) {
		error(errno, "read error on", ftp->ft_name);
		if (!nlines) {
			pp->ptm_rc = ER_ERR;
			pp->ptm_mtype = MT_NODATA;
			pp->ptm_len = 0;
			return;
		}
	}

	/* make sure that the message buffer is big enough for the reply */
	if (BUFCHK(&pp->ptm_data, &pp->pt_mdlen, avmsgsz(OP_GETS_ARGC(nlines))) < 0) {
		pp->ptm_rc = ER_ERR;
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
		return;
	}
	dp = pp->ptm_data;

#define rp	((struct avmsg *) dp)

	/* all ok so fill in the reply message and return */
	rp->av_argc = OP_GETS_ARGC(nlines);
	AV_FLAG(rp) = feof(ftp->ft_fp) ? AV_DONE : AV_MORE;
	for (n = 0; n < nlines; n++)
		AV_FLINE(rp, n) = ftp->ft_line[n];

	pp->ptm_rc = ER_OK;
	pp->ptm_mtype = MT_AVMSG;
	pp->ptm_len = avmsgsz(OP_GETS_ARGC(n));

#undef rp
}

/*
**	tet_op_puts() - write lines to a file, each one followed by a newline
*/

void tet_op_puts(pp)
register struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;
	register int n;
	register struct ftab *ftp;
	static char errmsg[] = "write error on";

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* find the file table element for this request */
	if ((ftp = ftfind(AV_FID(mp))) == (struct ftab *) 0) {
		pp->ptm_rc = ER_FID;
		return;
	}

	/* write the lines */
	for (n = 0; n < OP_PUTS_NLINE(mp); n++)
		if (fprintf(ftp->ft_fp, "%s\n", AV_FLINE(mp, n)) < 0) {
			error(errno, errmsg, ftp->ft_name);
			pp->ptm_rc = ER_ERR;
			return;
		}

	/* flush the stdio buffer */
	if (fflush(ftp->ft_fp) < 0) {
		error(errno, errmsg, ftp->ft_name);
		pp->ptm_rc = ER_ERR;
		return;
	}

	pp->ptm_rc = ER_OK;
}

/*
**	tet_op_fwrite() - write binary data to a file
*/

void tet_op_fwrite(pp)
struct ptab *pp;
{
	register struct btmsg *mp = (struct btmsg *) pp->ptm_data;
	register struct ftab *ftp;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* find the file table element for this request */
	if ((ftp = ftfind(mp->bt_fid)) == (struct ftab *) 0) {
		pp->ptm_rc = ER_FID;
		return;
	}

	/* make sure that count is plausible */
	if (!mp->bt_count || mp->bt_count > sizeof mp->bt_data) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	/* write the data */
	if (
		fwrite((void *) mp->bt_data, sizeof mp->bt_data[0],
			(size_t) mp->bt_count,
			ftp->ft_fp) != (size_t) mp->bt_count ||
		fflush(ftp->ft_fp) == EOF
	) {
		error(errno, "write failed on", ftp->ft_name);
		pp->ptm_rc = ER_ERR;
		return;
	}

	pp->ptm_rc = ER_OK;
}

/*
**	tet_fiodead() - dead process handler
*/

void tet_fiodead(pp)
register struct ptab *pp;
{
	register struct ftab *ftp;
	register int done;

	do {
		done = 1;
		for (ftp = ftab; ftp; ftp = ftp->ft_next)
			if (ftp->ft_ptab == pp) {
				(void) dofclose(ftp);
				done = 0;
				break;
			}
	} while (!done);
}

/*
**	dofclose() - internal part of tet_op_fclose()
**
**	return ER_OK if successful or other ER_* error code on error
*/

static int dofclose(ftp)
register struct ftab *ftp;
{
	register int rc;

	/* close the file */
	if (fclose(ftp->ft_fp) == EOF) {
		error(errno, "fclose failed on", ftp->ft_name);
		rc = ER_ERR;
	}
	else
		rc = ER_OK;
	ftp->ft_fp = (FILE *) 0;

	/* remove the ftab entry from the table and free it */
	ftrm(ftp);
	ftfree(ftp);

	return(rc);
}

/*
**	ftalloc() - allocate a file table element
*/

static struct ftab *ftalloc()
{
	register struct ftab *ftp;
	static long fid;

	if ((ftp = (struct ftab *) malloc(sizeof *ftp)) == (struct ftab *) 0) {
		error(errno, "can't allocate ftab element", (char *) 0);
		return((struct ftab *) 0);
	}
	TRACE2(tet_Tbuf, 6, "allocate ftab = %s", tet_i2x(ftp));
	bzero((char *) ftp, sizeof *ftp);

	ftp->ft_id = ++fid;
	return(ftp);
}

/*
**	ftfree() - free storage occupied by a file table element
*/

static void ftfree(ftp)
register struct ftab *ftp;
{
	TRACE2(tet_Tbuf, 6, "free ftab = %s", tet_i2x(ftp));

	if (ftp) {
		if (ftp->ft_fp)
			(void) fclose(ftp->ft_fp);
		if (ftp->ft_name) {
			TRACE2(tet_Tbuf, 6, "free ftab fname = %s",
				tet_i2x(ftp->ft_name));
			free(ftp->ft_name);
		}
		ftlfree(ftp);
		free((char *) ftp);
	}
}

/*
**	ftlfree() - free ftab line buffers if necessary
*/

static void ftlfree(ftp)
register struct ftab *ftp;
{
	register int n;
	register char *p;

	for (n = 0; n < AV_NLINE; n++)
		if ((p = ftp->ft_line[n]) != (char *) 0) {
			TRACE3(tet_Tbuf, 6, "free ftab line buf %s = %s",
				tet_i2a(n), tet_i2x(p));
			free(p);
			ftp->ft_line[n] = (char *) 0;
		}
}

/*
**	ftadd() - insert an element in the file table
*/

static void ftadd(ftp)
struct ftab *ftp;
{
	tet_listinsert((struct llist **) &ftab, (struct llist *) ftp);
}

/*
**	ftrm() - remove an element from the file table
*/

static void ftrm(ftp)
struct ftab *ftp;
{
	tet_listremove((struct llist **) &ftab, (struct llist *) ftp);
}

/*
**	ftfind() - find an entry in the file table and return a pointer
**		thereto
**
**	return (struct ftab *) 0 if not found
*/

static struct ftab *ftfind(fid)
register long fid;
{
	register struct ftab *ftp;

	for (ftp = ftab; ftp; ftp = ftp->ft_next) {
		if (ftp->ft_id == fid)
			break;
	}

	return(ftp);
}

