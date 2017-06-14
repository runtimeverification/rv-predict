/*
 *      SCCS:  @(#)config.c	1.13 (02/01/18) 
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
static char sccsid[] = "@(#)config.c	1.13 (02/01/18) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)config.c	1.13 02/01/18 TETware release 3.8
NAME:		config.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	tccd configuration variable request processing functions

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., November 1993
	Enhancements for FIFO transport interface.
	Add/update TET_ROOT and TET_EXECUTE environment variables when
	they are received in an OP_CONFIG request.

	Andrew Dingwall, UniSoft Ltd., August 1996
	changed for tetware-style configuration

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for shared API libraries.
 
	Andrew Dingwall, UniSoft Ltd., March 2000
	Added some extra trace messages.


************************************************************************/


#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/types.h>
#include <time.h>
#ifndef _WIN32	/* -WIN32-CUT-LINE- */
#  include <unistd.h>
#endif		/* -WIN32-CUT-LINE- */
#include "dtmac.h"
#include "dtmsg.h"
#include "ptab.h"
#include "avmsg.h"
#include "valmsg.h"
#include "ltoa.h"
#include "llist.h"
#include "error.h"
#include "globals.h"
#include "bstring.h"
#include "tccd.h"
#include "config.h"
#include "dtetlib.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif


/* values for cflags below (a bit field) */
#define CF_SINPROGRESS	001		/* sndconf in progress */
#define CF_SDONE	002		/* sndconf done */
#define CF_RINPROGRESS	004		/* rcvconf in progress */
#define CF_RDONE	010		/* rcvconf done */
#define CF_CINPROGRESS	020		/* config in progress */
#define CF_CDONE	040		/* config done */

static char *cfname;			/* config file name */
static int cflags = CF_RDONE;		/* config state flags */
static char *tet_config[TC_NCONF_MODES];/* value of TET_CONFIG for each
					   configuration mode */
static int currmode;			/* the current configuration mode */

#ifndef NOTRACE
static char *mode_name[] = { "BUILD", "EXEC", "CLEAN" };
#define Nmodename	(sizeof mode_name / sizeof mode_name[0])
#define MODE_NAME(mode)	(((mode) >= 0 && (mode) < Nmodename) ? \
				mode_name[mode] : tet_i2a(mode))
#endif

/*
**	structure of the config table
**
**	the cf_next and cf_last pointers must be first and second so as
**	to allow the use of the llist routines
*/

struct ctab {
	struct ctab *ct_next;		/* ptr to next element */
	struct ctab *ct_last;		/* ptr to last element */
	char *ct_string;		/* variable string */
	int ct_flags;			/* flags (see below) */
};

/* values for cf_flags (a bit field) */
#define CF_PRIORITY	01		/* remote setting has priority */
#define CF_PURGE	02		/* entry may be removed once reply
					   message has been sent */

static struct ctab *ctab;		/* ptr to start of config table */


/* static function declarations */
static void ctadd PROTOLIST((struct ctab *));
static struct ctab *ctaddupdate PROTOLIST((char *));
static struct ctab *ctalloc PROTOLIST((void));
static struct ctab *ctfind PROTOLIST((char *));
static void ctfree PROTOLIST((struct ctab *));
static void ctpurge PROTOLIST((void));
static void ctrm PROTOLIST((struct ctab *));
static int ctupdate PROTOLIST((struct ctab *, char *));
static int op_c2 PROTOLIST((int));
static int op_c3 PROTOLIST((char *));
static int op_c4 PROTOLIST((FILE *, char *));
static int op_sc2 PROTOLIST((struct ptab *));
static int procline PROTOLIST((char *));


/*
**	op_cfname() - receive the config file name for use in config
**		variable exchange
*/

void op_cfname(pp)
register struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;
	register int n;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* make sure we do this only once per config variable exchange */
	if (cflags & CF_RDONE) {
		if (cfname) {
			TRACE2(tet_Tbuf, 6, "free cfname = %s",
				tet_i2x(cfname));
			free(cfname);
			cfname = (char *) 0;
		}
	}
	else {
		pp->ptm_rc = ER_CONTEXT;
		return;
	}

	/* do some sanity checks on the request message */
	if (OP_CFNAME_NCFNAME(mp) != TC_NCFNAME) {
		pp->ptm_rc = ER_INVAL;
		return;
	}
	for (n = 0; n < TC_NCFNAME; n++)
		if (!AV_CFNAME(mp, n) || !*AV_CFNAME(mp, n)) {
			pp->ptm_rc = ER_INVAL;
			return;
		}

	/* remember the file name for later */
	if ((cfname = tet_strstore(AV_CFNAME(mp, 0))) == (char *) 0) {
		pp->ptm_rc = ER_ERR;
		return;
	}
	TRACE2(tet_Ttccd, 4, "config file name = \"%s\"", cfname);

	cflags &= ~CF_RDONE;
	pp->ptm_rc = ER_OK;
}

/*
**	op_sndconf() - receive config info from client as part of
**		a config variable exchange
**
**	the terminology is confusing - SEND and RECEIVE are defined relative
**	to the client
*/

void op_sndconf(pp)
register struct ptab *pp;
{
	register char *p;
	register int rc;
	register struct ctab *cp;
	FILE *fp;
	char buf[BUFSIZ];

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* remove old entries from the config table */
	ctpurge();

	/* make sure that we have a config file name */
	if (!cfname) {
		pp->ptm_rc = ER_CONTEXT;
		return;
	}

	/* make sure that an op_config is not in progress */
	if (cflags & CF_CINPROGRESS) {
		pp->ptm_rc = ER_INPROGRESS;
		return;
	}

	/* make sure that we receive only one set of config lines
		before sending the merged ones back */
	if (cflags & CF_SDONE) {
		pp->ptm_rc = ER_DONE;
		return;
	}

	/* perform common processing and set the reply code */
	switch (op_sc2(pp)) {
	case 0:
		/* no more messages */
		break;
	case 1:
		/* more messages to come */
		cflags |= CF_SINPROGRESS;
		/* fall through */
	default:
		return;
	}

	/* open the local config file */
	if ((fp = fopen(cfname, "r")) == NULL) {
		error(errno, "can't open", cfname);
		pp->ptm_rc = ER_ERR;
		return;
	}

	/* read the local config file and add/update the config table */
	rc = ER_OK;
	while (fgets(buf, sizeof buf, fp) != NULL) {
		for (p = buf; *p; p++)
			if (*p == '\n') {
				*p = '\0';
				break;
			}
		if (buf[0] == '\0' || buf[0] == '#')
			continue;
		if (!tet_equindex(buf)) {
			error(0, "bad format local config variable:", buf);
			continue;
		}
		TRACE2(tet_Ttccd, 4, "local config variable: \"%s\"", buf);
		if ((cp = ctfind(buf)) != (struct ctab *) 0) {
			if (cp->ct_flags & CF_PRIORITY)
				TRACE1(tet_Ttccd, 6,
					"local variable overridden");
			else {
				TRACE1(tet_Ttccd, 6,
					"remote variable overridden");
				if (ctupdate(cp, buf) < 0)
					rc = ER_ERR;
			}
		}
		else if (ctaddupdate(buf) == (struct ctab *) 0)
			rc = ER_ERR;
	}

	(void) fclose(fp);
	cflags = (cflags & ~CF_SINPROGRESS) | CF_SDONE;
	pp->ptm_rc = rc;
}

/*
**	op_rcvconf() - return config info to client as part of a
**		config variable exchange
*/

void op_rcvconf(pp)
struct ptab *pp;
{
	register struct avmsg *rp;
	register struct ctab *cp;
	register int n;

	/* error replies contain no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* purge old entries from the config table */
	ctpurge();

	/* make sure that the request context is appropriate */
	if ((cflags & (CF_SDONE | CF_CINPROGRESS)) != CF_SDONE) {
		pp->ptm_rc = ER_INPROGRESS;
		return;
	}
	else if (cflags & CF_RDONE) {
		pp->ptm_rc = ER_DONE;
		return;
	}

	/* make sure that the message buffer is big enough for the reply */
	if (BUFCHK(&pp->ptm_data, &pp->pt_mdlen, avmsgsz(OP_CONF_ARGC(AV_NLINE))) < 0) {
		pp->ptm_rc = ER_ERR;
		return;
	}
	rp = (struct avmsg *) pp->ptm_data;

	/* for each config table entry, copy the string to the reply message
		and mark the entry as sent
		the order of the config table is LIFO with duplicates
		eliminated; the table is searched from the bottom upwards
		so as to preserve the order in which the config lines were
		received by op_sndconf */
	for (cp = ctab; cp; cp = cp->ct_next)
		if (!cp->ct_next)
			break;
	for (n = 0; cp && n < AV_NLINE; n++, cp = cp->ct_last) {
		AV_CLINE(rp, n) = cp->ct_string;
		cp->ct_flags |= CF_PURGE;
	}

	rp->av_argc = OP_CONF_ARGC(n);
	if (cp) {
		AV_FLAG(rp) = (long) AV_MORE;
		cflags |= CF_RINPROGRESS;
	}
	else {
		AV_FLAG(rp) = (long) AV_DONE;
		cflags = (cflags & ~(CF_RINPROGRESS | CF_SDONE)) | CF_RDONE;
	}

	pp->ptm_rc = ER_OK;
	pp->ptm_mtype = MT_AVMSG;
	pp->ptm_len = avmsgsz(OP_CONF_ARGC(n));
}

/*
**	op_config() - store configuration variables in a tmp file and
**		put the filename in the environment ready for an exec
*/

void op_config(pp)
register struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;
	register struct ctab *cp;
	register int mode;

	/* remove old entries from the config table */
	ctpurge();

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* make sure that a config exchange is not in progress */
	if (cflags & (CF_SINPROGRESS | CF_SDONE | CF_RINPROGRESS)) {
		pp->ptm_rc = ER_CONTEXT;
		return;
	}

	/* do a sanity check on the mode */
	switch (AV_MODE(mp)) {
	case TC_CONF_BUILD:
	case TC_CONF_EXEC:
	case TC_CONF_CLEAN:
		mode = TC_CONF_MODE(AV_MODE(mp));
		break;
	default:
		pp->ptm_rc = ER_INVAL;
		return;
	}

	/* perform common processing and set the reply code */
	switch (op_sc2(pp)) {
	case 0:
		/* no more messages */
		break;
	case 1:
		/* more messages to come */
		cflags = (cflags & ~CF_CDONE) | CF_CINPROGRESS;
		/* fall through */
	default:
		return;
	}

	/* here to process the config variables */
	if ((pp->ptm_rc = op_c2(mode)) == ER_OK)
		cflags = (cflags & ~CF_CINPROGRESS) | CF_CDONE;

	/* remove all the config variables */
	for (cp = ctab; cp; cp = cp->ct_next)
		cp->ct_flags |= CF_PURGE;
	ctpurge();
}

/*
**	op_c2() - extend the op_config() processing
**
**	return ER_OK if successful or other ER_* error code on error
*/

static int op_c2(mode)
int mode;
{
	register char *fname;
	register int rc;

	/* get a tmp name for the config file */
	if ((fname = tet_mktfname("tccd")) == (char *) 0)
		return(ER_ERR);

	if ((rc = op_c3(fname)) == ER_OK) {
		if (tet_config[mode]) {
			(void) UNLINK(tet_config[mode]);
			TRACE3(tet_Tbuf, 6, "free tet_config[%s] = %s",
				tet_i2a(mode), tet_i2x(tet_config[mode]));
			free(tet_config[mode]);
		}
		tet_config[mode] = fname;
	}
	else {
		TRACE2(tet_Tbuf, 6, "free tmp config file name = %s",
			tet_i2x(fname));
		free(fname);
	}

	return(rc);
}

/*
**	op_c3() - extend the op_config() processing some more
**
**	return ER_OK if successful or other ER_* error code on error
*/

static int op_c3(fname)
char *fname;
{
	register int rc;
	FILE *fp;

	/* open the tmp config file
		and write out the config variables */
	if ((fp = fopen(fname, "w")) == NULL) {
		error(errno, "can't open", fname);
		rc = ER_ERR;
	}
	else {
		rc = op_c4(fp, fname);
		if (fclose(fp) == EOF) {
			error(errno, "fclose error on", fname);
			rc = ER_ERR;
		}
	}

	/* remove the file if any of the above failed */
	if (rc != ER_OK)
		(void) UNLINK(fname);

	return(rc);
}

/*
**	op_c4() - extend the op_config() processing yet more
**
**	return ER_OK if successful or other ER_* error code on error
*/

static int op_c4(fp, fname)
FILE *fp;
char *fname;
{
	register struct ctab *cp;

	/* write out the config lines in the order they were received
		to the tmp config file */
	for (cp = ctab; cp; cp = cp->ct_next)
		if (!cp->ct_next)
			break;
	while (cp) {
		if (fprintf(fp, "%s\n", cp->ct_string) < 0) {
			error(errno, "write error on", fname);
			return(ER_ERR);
		}
		cp = cp->ct_last;
	}

	return(ER_OK);
}

/*
**	op_sc2() - common routine for op_sndconf() and op_config()
**
**	return	 1 if there are more lines to come
**		 0 if this is the last message
**		-1 on error
*/

static int op_sc2(pp)
struct ptab *pp;
{
	register struct avmsg *mp = (struct avmsg *) pp->ptm_data;
	register int n, rc;
	register char *s;

	/* see if there are any more messages to come */
	switch ((int) AV_FLAG(mp)) {
	case AV_MORE:
	case AV_DONE:
		break;
	default:
		error(0, "unexpected flag", tet_l2o(AV_FLAG(mp)));
		pp->ptm_rc = ER_INVAL;
		return(-1);
	}

	/* store the config variables */
	pp->ptm_rc = ER_OK;
	for (n = 0; n < OP_CONF_NLINE(mp); n++) {
		if ((s = AV_CLINE(mp, n)) != (char *) 0 &&
			(rc = procline(s)) != ER_OK)
				pp->ptm_rc = rc;
	}

	return((int) AV_FLAG(mp) == AV_DONE ? 0 : 1);
}

/*
**	procline() - process a config line received from a client
**
**	return ER_OK if successful or other ER_* error code on error
*/

static int procline(s)
char *s;
{
	register struct ctab *cp;
	register char *p1, *p2;
	static char errmsg[] = "received bad format config variable:";

	/* see if this is a REMnnn config name for this system; if it is,
		p1 will point to the char after the TET_REMnnn_ prefix */
	if ((p1 = tet_remvar(s, tet_mysysid)) == (char *) 0) {
		error(0, errmsg, s);
		return(ER_INVAL);
	}

	/* make sure that there is still both a name and a value part */
	if ((p2 = tet_equindex(p1)) == (char *) 0 || p2 == p1) {
		error(0, errmsg, s);
		return(ER_INVAL);
	}

#ifndef NOTRACE
	TRACE2(tet_Ttccd, 4, "receive config variable \"%s\"", s);
	if (p1 != s) {
		TRACE2(tet_Ttccd, 6, "maps locally to \"%s\"", p1); 
	}
#endif

	/* allocate a new config table element or update an existing one */
	if ((cp = ctaddupdate(p1)) == (struct ctab *) 0)
		return(ER_ERR);

	/* if this was a REMnnn variable, mark it as having priority over
		a local assignment */
	if (p1 != s)
		cp->ct_flags |= CF_PRIORITY;
	else
		cp->ct_flags &= ~CF_PRIORITY;

	return(ER_OK);
}

/*
**	ctalloc() - allocate a config table element
*/

static struct ctab *ctalloc()
{
	register struct ctab *cp;

	if ((cp = (struct ctab *) malloc(sizeof *cp)) == (struct ctab *) 0) {
		error(errno, "can't allocate ctab element", (char *) 0);
		return((struct ctab *) 0);
	}
	TRACE2(tet_Tbuf, 6, "allocate ctab = %s", tet_i2x(cp));
	bzero((char *) cp, sizeof *cp);

	return(cp);
}

/*
**	ctfree() - free storage occupied by a config table element
*/

static void ctfree(cp)
struct ctab *cp;
{
	TRACE2(tet_Tbuf, 6, "free ctab = %s", tet_i2x(cp));

	if (cp) {
		if (cp->ct_string) {
			TRACE2(tet_Tbuf, 6, "free ctstring = %s",
				tet_i2x(cp->ct_string));
			free(cp->ct_string);
		}
		free((char *) cp);
	}
}

/*
**	ctadd() - insert an element in the config table
*/

static void ctadd(cp)
struct ctab *cp;
{
	tet_listinsert((struct llist **) &ctab, (struct llist *) cp);
}

/*
**	ctrm() - remove an element from the config table
*/

static void ctrm(cp)
struct ctab *cp;
{
	tet_listremove((struct llist **) &ctab, (struct llist *) cp);
}

/*
**	ctfind() - find an entry in the config table and return a pointer
**		thereto
**
**	return (struct ctab *) 0 if not found
**
**	the s parameter may be delimited by either '=' or '\0'
*/

static struct ctab *ctfind(s)
char *s;
{
	register char *p1, *p2;
	register struct ctab *cp;

	for (cp = ctab; cp; cp = cp->ct_next) {
		if (cp->ct_flags & CF_PURGE)
			continue;
		for (p1 = s, p2 = cp->ct_string; *p1 && *p2; p1++, p2++) {
			if (*p1 != *p2 || *p1 == '=')
				break;
		}
		if ((!*p1 || *p1 == '=') && (!*p2 || *p2 == '='))
			break;
	}

	return(cp);
}

/*
**	ctaddupdate() - add a new entry to the config table or update an
**		existing one
**
**	return a pointer to the entry or (struct ctab *) 0 on error
*/

static struct ctab *ctaddupdate(s)
char *s;
{
	register struct ctab *cp;

	/* find or allocate a config table entry */
	if ((cp = ctfind(s)) == (struct ctab *) 0) {
		if ((cp = ctalloc()) == (struct ctab *) 0)
			return((struct ctab *) 0);
		else 
			ctadd(cp);
	}

	/* add the variable string */
	if (ctupdate(cp, s) < 0) {
		ctrm(cp);
		ctfree(cp);
		return((struct ctab *) 0);
	}

	return(cp);
}

/*
**	ctupdate() - update a config table element with new config string
**
**	return zero if successful or -1 on error
*/

static int ctupdate(cp, s)
struct ctab *cp;
char *s;
{
	register char *oldstring;

	oldstring = cp->ct_string;

	if ((cp->ct_string = tet_strstore(s)) == (char *) 0) {
		cp->ct_string = oldstring;
		return(-1);
	}

	if (oldstring) {
		TRACE2(tet_Tbuf, 6, "free oldstring = %s", tet_i2x(oldstring));
		free(oldstring);
	}

	return(0);
}

/*
**	ctpurge() - remove old entries from the config table
**
**	this is because storage allocated to an entry cannot be freed until
**	after a OP_RCVCONF reply message has been sent to the client
*/

static void ctpurge()
{
	register struct ctab *cp;
	register int done;

	do {
		done = 1;
		for (cp = ctab; cp; cp = cp->ct_next)
			if (cp->ct_flags & CF_PURGE) {
				ctrm(cp);
				ctfree(cp);
				done = 0;
				break;
			}
	} while (!done);
}

/*
**	op_setconf() - set the current configuration mode
*/

void op_setconf(pp)
register struct ptab *pp;
{
	register struct valmsg *mp = (struct valmsg *) pp->ptm_data;
	register int mode;
	static char envname[] = "TET_CONFIG";
	static char *var;
	static int lvar;

	/* all reply messages have no data */
	pp->ptm_mtype = MT_NODATA;
	pp->ptm_len = 0;

	/* do a sanity check on the mode */
	switch (VM_MODE(mp)) {
	case TC_CONF_BUILD:
	case TC_CONF_EXEC:
	case TC_CONF_CLEAN:
		ASSERT(CONF_MODE_OK(VM_MODE(mp), tet_config));
		mode = TC_CONF_MODE(VM_MODE(mp));
		break;
	default:
		pp->ptm_rc = ER_INVAL;
		return;
	}

	/* make sure that an op_config is not in progress */
	if (cflags & CF_CINPROGRESS) {
		pp->ptm_rc = ER_INPROGRESS;
		return;
	}

	/* make sure that we have a configuration for this mode */
	if (!tet_config[mode]) {
		pp->ptm_rc = ER_CONTEXT;
		return;
	}

	/* return now if the requested mode is already in effect */
	if (VM_MODE(mp) == currmode) {
		TRACE2(tet_Ttccd, 4, "SETCONF: mode %s already in effect",
			MODE_NAME(mode));
		pp->ptm_rc = ER_OK;
		return;
	}

	/*
	** here to change the configuration mode
	**
	** ensure that the environment string is big enough,
	** then format the variable and put it in the environment
	*/
	if (BUFCHK(&var, &lvar, (int) (sizeof envname + strlen(tet_config[mode]) + 1)) < 0) {
		pp->ptm_rc = ER_ERR;
		return;
	}
	(void) sprintf(var, "%s=%s", envname, tet_config[mode]);
	if (tet_putenv(var) < 0) {
		pp->ptm_rc = ER_ERR;
		return;
	}

	TRACE3(tet_Ttccd, 4, "SETCONF: new mode = %s, file = %s",
		MODE_NAME(mode), tet_config[mode]);

	/* all ok so remember the current mode and return success */
	currmode = VM_MODE(mp);
	pp->ptm_rc = ER_OK;
}

/*
**	config_cleanup() - remove all the temporary configuration files
*/

void config_cleanup()
{
	register int n;

	for (n = 0; n < sizeof tet_config / sizeof tet_config[0]; n++)
		if (tet_config[n])
			(void) UNLINK(tet_config[n]);
}

