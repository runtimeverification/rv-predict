/*
 *      SCCS:  @(#)tfproc.c	1.13 (03/03/26) 
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
static char sccsid[] = "@(#)tfproc.c	1.13 (03/03/26) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)tfproc.c	1.13 03/03/26 TETware release 3.8
NAME:		tfproc.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	file transfer request processing functions

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., November 1993
	enhancements for FIFO transport interface

	Andrew Dingwall, UniSoft Ltd., March 1998
	Avoid passing a -ve precision value to sprintf().

	Andrew Dingwall, The Open Group, March 2003
	Added support for binary transfer mode.

************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <time.h>
#include <sys/types.h>
#include <sys/stat.h>
#ifdef _WIN32	/* -START-WIN32-CUT- */
#  include <io.h>
#else		/* -END-WIN32-CUT- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "llist.h"
#include "avmsg.h"
#include "btmsg.h"
#include "valmsg.h"
#include "error.h"
#include "bstring.h"
#include "xresd.h"
#include "dtetlib.h"

#ifndef NOTRACE
#include "ltoa.h"
#endif


#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/*
**	Transfer file table.
**
**	An entry is allocated in the transfer file table for each transfer
**	file opened by a call to OP_TFOPEN.
**	Storage for an element is allocated by tfalloc() and freed by
**	tffree().
**	An element is added to the table by tfadd() and removed by tfrm().
*/

struct tftab {
	struct tftab *tf_next;		/* ptr to next element in list */
	struct tftab *tf_last;		/* ptr to prev element in list */
	int tf_id;			/* id for xfer requests */
	struct ptab *tf_ptab;		/* ptr to owner's ptab */
	char *tf_name;			/* xfer file name */
	int tf_mode;			/* xfer file mode */
	FILE *tf_fp;			/* fp for xfer file */
};

static struct tftab *tftab;		/* ptr to head of xfer file table */


/* static function declarations */
static int dotfclose PROTOLIST((struct tftab *));
static int op_tfo2 PROTOLIST((struct ptab *, struct tftab *, char *, int));
static void tfadd PROTOLIST((struct tftab *));
static struct tftab *tfalloc PROTOLIST((void));
static struct tftab *tffind PROTOLIST((int));
static void tffree PROTOLIST((struct tftab *));
static void tfrm PROTOLIST((struct tftab *));


/*
**	op_tfopen() - process an OP_TFOPEN request
*/

void op_tfopen(pp)
register struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;
	register struct tftab *tp;
	char *tfname;

	/* do some sanity checks on the request message */
	if (mp->av_argc != OP_TFOPEN_ARGC || !AV_TFNAME(mp)) {
		pp->ptm_rc = ER_INVAL;
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
		return;
	}

	TRACE2(tet_Txresd, 4, "OP_TFOPEN: file = \"%s\"", AV_TFNAME(mp));

	/* remove a leading '/' if present */
	tfname = AV_TFNAME(mp);
	while (*tfname && isdirsep(*tfname))
		tfname++;

	/* get a new tftab element for this request */
	if ((tp = tfalloc()) == (struct tftab *) 0) {
		pp->ptm_rc = ER_ERR;
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
		return;
	}
	tp->tf_mode = (int) AV_MODE(mp);

	if ((pp->ptm_rc = op_tfo2(pp, tp, tfname, AV_FLAG(mp))) == ER_OK) {
		tp->tf_ptab = pp;
		tfadd(tp);
		pp->ptm_mtype = MT_VALMSG;
		pp->ptm_len = valmsgsz(OP_TFOPEN_NVALUE);
	}
	else {
		tffree(tp);
		pp->ptm_mtype = MT_NODATA;
		pp->ptm_len = 0;
	}
}

/*
**	op_tfo2() - extend the op_tfopen() processing
**
**	return ER_OK if successful or other ER_* error code on error
*/

static int op_tfo2(pp, tp, tfname, binflag)
struct ptab *pp;
register struct tftab *tp;
char *tfname;
int binflag;
{
	register struct valmsg *rp;
	register char *p;
	int len;
	char tfpath[MAXPATH + 1];
	char *mode;
	char sep;
	extern char *Tet_savedir;

	/* make sure that the savedir is accessible */
	if (tet_eaccess(Tet_savedir, 02) < 0) {
		error(errno, "can't write in", Tet_savedir);
		return(ER_ERR);
	}

	/* construct the transfer file name and store it */
	len = (int) sizeof tfpath - (int) strlen(Tet_savedir) - 2;
	(void) sprintf(tfpath, "%.*s/%.*s",
		(int) sizeof tfpath - 2, Tet_savedir, TET_MAX(len, 0), tfname);
	if ((tp->tf_name = tet_strstore(tfpath)) == (char *) 0)
		return(ER_ERR);
	TRACE2(tet_Txresd, 6, "op_tfo2(): full path = \"%s\"\n", tp->tf_name);

	/* make sure that the message buffer is big enough for the reply */
	if (BUFCHK(&pp->ptm_data, &pp->pt_mdlen, valmsgsz(OP_TFOPEN_NVALUE)) < 0)
		return(ER_ERR);
	rp = (struct valmsg *) pp->ptm_data;

	/* make any required directories */
	for (p = tp->tf_name; *p; p++)
		;
	while (--p >= tp->tf_name)
		if (isdirsep(*p))
			break;
	if (p > tp->tf_name) {
		sep = *p;
		*p = '\0';
		if (tet_mkalldirs(tp->tf_name) < 0)
			return(ER_ERR);
		*p = sep;
	}

	/* open the transfer file */
	mode = binflag ? "wb" : "w";
	if ((tp->tf_fp = fopen(tp->tf_name, mode)) == NULL) {
		error(errno, "can't open", tp->tf_name);
		return(ER_ERR);
	}

	/* all ok so fill in the reply message and return */
	TRACE2(tet_Txresd, 4, "OP_TFOPEN: return fid = %s", tet_i2a(tp->tf_id));
	VM_XFID(rp) = (long) tp->tf_id;
	rp->vm_nvalue = OP_TFOPEN_NVALUE;
	return(ER_OK);
}

/*
**	op_tfclose() - process an OP_TFCLOSE request
*/

void op_tfclose(pp)
register struct ptab *pp;
{
	register struct valmsg *mp = (struct valmsg *) pp->ptm_data;
	register struct tftab *tp;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	TRACE2(tet_Txresd, 4, "OP_TFCLOSE: fid = %s", tet_i2a(VM_XFID(mp)));

	/* find the tftab element */
	if ((tp = tffind((int) VM_XFID(mp))) == (struct tftab *) 0) {
		pp->ptm_rc = ER_FID;
		return;
	}

	pp->ptm_rc = dotfclose(tp);
}

/*
**	op_tfwrite() - process an OP_TFWRITE request
*/

void op_tfwrite(pp)
register struct ptab *pp;
{
	register struct btmsg *mp = (struct btmsg *) pp->ptm_data;
	register struct tftab *tp;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	TRACE3(tet_Txresd, 4, "OP_TFWRITE: fid = %s, count = %s",
		tet_i2a(mp->bt_fid), tet_i2a(mp->bt_count));

	/* find the tftab element */
	if ((tp = tffind((int) mp->bt_fid)) == (struct tftab *) 0) {
		pp->ptm_rc = ER_FID;
		return;
	}

	/* make sure that count is plausible */
	if (!mp->bt_count || mp->bt_count > sizeof mp->bt_data) {
		pp->ptm_rc = ER_INVAL;
		return;
	}

	/* write the data */
	if (fwrite(mp->bt_data, sizeof mp->bt_data[0], (int) mp->bt_count, tp->tf_fp) != (int) mp->bt_count || fflush(tp->tf_fp) == EOF) {
		error(errno, "write failed on", tp->tf_name);
		pp->ptm_rc = ER_ERR;
		return;
	}

	pp->ptm_rc = ER_OK;
	return;
}

/*
**	tfdead() - tftab processing when a connection closes
*/

void tfdead(pp)
register struct ptab *pp;
{
	register struct tftab *tp;
	register int done;

	do {
		done = 1;
		for (tp = tftab; tp; tp = tp->tf_next)
			if (tp->tf_ptab == pp) {
				(void) dotfclose(tp);
				done = 0;
				break;
			}
	} while (!done);
}

/*
**	dotfclose() - internal part of op_tfclose()
**
**	return ER_OK if successful or other ER_* error code on error
*/

static int dotfclose(tp)
register struct tftab *tp;
{
	register int rc;

	/* perform the close operation */
	if (fclose(tp->tf_fp) == EOF) {
		error(errno, "fclose failed on", tp->tf_name);
		rc = ER_ERR;
	}
	else
		rc = ER_OK;

	/* update the file mode */
	if (CHMOD(tp->tf_name, (mode_t) tp->tf_mode) < 0)
		error(errno, "warning: can't chmod", tp->tf_name);

	/* remove the xfer table entry and free it */
	tfrm(tp);
	tffree(tp);

	return(rc);
}

/*
**	tfalloc() - allocate an xfer file table element and return a pointer
**	thereto
**
**	return (struct tftab *) 0 on error
*/

static struct tftab *tfalloc()
{
	register struct tftab *tp;
	static int tfid = 0;

	if ((tp = (struct tftab *) malloc(sizeof *tp)) == (struct tftab *) 0) {
		error(errno, "can't allocate tftab element", (char *) 0);
		return((struct tftab *) 0);
	}
	TRACE2(tet_Tbuf, 6, "allocate tftab = %s", tet_i2x(tp));
	bzero((char *) tp, sizeof *tp);

	tp->tf_id = ++tfid;

	return(tp);
}

/*
**	tffree() - free an xfer file table element
*/

static void tffree(tp)
struct tftab *tp;
{
	TRACE2(tet_Tbuf, 6, "free tftab = %s", tet_i2x(tp));

	if (tp) {
		if (tp->tf_name) {
			TRACE2(tet_Tbuf, 6, "free tftab name = %s",
				tet_i2x(tp->tf_name));
			free(tp->tf_name);
		}
		free((char *) tp);
	}
}

/*
**	tfadd() - insert an element in the tftab list
*/

static void tfadd(tp)
struct tftab *tp;
{
	tet_listinsert((struct llist **) &tftab, (struct llist *) tp);
}

/*
**	tfrm() - remove an element from the tftab list
*/

static void tfrm(tp)
struct tftab *tp;
{
	tet_listremove((struct llist **) &tftab, (struct llist *) tp);
}

/*
**	tffind() - find a tftab element matching id and return a pointer
**		thereto
**
**	return (struct tftab *) 0 if none can be found
*/

static struct tftab *tffind(id)
register int id;
{
	register struct tftab *tp;

	for (tp = tftab; tp; tp = tp->tf_next)
		if (tp->tf_id == id)
			break;

	return(tp);
}

