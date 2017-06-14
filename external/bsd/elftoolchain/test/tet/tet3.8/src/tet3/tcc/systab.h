/*
 *	SCCS: @(#)systab.h	1.5 (00/04/03)
 *
 *	UniSoft Ltd., London, England
 *
 * (C) Copyright 1996 X/Open Company Limited
 *
 * All rights reserved.  No part of this source code may be reproduced,
 * stored in a retrieval system, or transmitted, in any form or by any
 * means, electronic, mechanical, photocopying, recording or otherwise,
 * except as stated in the end-user licence agreement, without the prior
 * permission of the copyright owners.
 * A copy of the end-user licence agreement is contained in the file
 * Licence which accompanies this distribution.
 * 
 * X/Open and the 'X' symbol are trademarks of X/Open Company Limited in
 * the UK and other countries.
 */

/************************************************************************

SCCS:   	@(#)systab.h	1.5 00/04/03 TETware release 3.8
NAME:		systab.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	definitions related to the tcc systems table

MODIFICATIONS:

	Andrew Dingwall, UniSoft Ltd., July 1998
	Added support for configuration variable expansion.
 
	Andrew Dingwall, UniSoft Ltd., March 2000
	Moved the per-system configuration lists out of the systab.

************************************************************************/

/*
**	tcc systems table
**
**	a systab element is allocated for the local system and for
**	each remote system to which tcc might need to connect
*/

/*
** structure of the tcc systems table
** the sy_next and sy_last elements must be first and second so as to
** enable the systems table to be manipulated by the llist routines
*/
struct systab {
	struct systab *sy_next;	/* ptr to next element in the table */
	struct systab *sy_last;	/* ptr to prev element in the table */
	long sy_magic;		/* magic number */
	int sy_sysid;		/* system id */
	int sy_activity;	/* system's idea of current activity */
	char *sy_cwd;		/* tccd's current working directory */
	char *sy_sfdir;		/* saved files directory */
	char *sy_rcfname;	/* tet_codes file name */
#ifndef TET_LITE	/* -START-LITE-CUT- */
	int sy_cfmodes;		/* operation modes for which tccd has
				   been configured (TCC_BUILD, etc.) */
	int sy_currcfmode;	/* current config mode set by tet_tcsetconf()
				   (TC_CONF_BUILD, etc.) */
	int *sy_sys;		/* system name list last sent to tccd */
	int sy_nsys;		/* no of entries in sy_sys */
#endif /* !TET_LITE */	/* -END-LITE-CUT- */
};

#define SY_MAGIC	0x73595374

/* extern function declarations */
extern struct systab *syfind PROTOLIST((int));
extern int sychdir PROTOLIST((struct systab *, char *));

