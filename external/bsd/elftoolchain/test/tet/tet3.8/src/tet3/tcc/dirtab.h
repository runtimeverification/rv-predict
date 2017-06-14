/*
 *	SCCS: @(#)dirtab.h	1.3 (98/03/05)
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

SCCS:   	@(#)dirtab.h	1.3 98/03/05 TETware release 3.8
NAME:		dirtab.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	definition of the scenario directive table

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., March 1998
	added support for number ranges in directive arguments

************************************************************************/

/*
**	scenario directive table
**
**	each directive has an entry in the scenario directive table
**	each entry defines the directive's name and symbolic value,
**	together with some flags which are used when checking syntax in
**	the scenario file
**
**	some directives may not appear within the scope of other directives;
**	each directive's dirtab entry includes a pointer to a list of
**	other directives which this one may enclose
*/

/* structure of scenario directive table */
struct dirtab {
	char *dt_name;		/* directive name */
	int dt_directive;	/* directive token value */
	int dt_match;		/* token value of matching directive */
	int dt_flags;		/* directve flags - see below */
	int *dt_enc;		/* list of directives which this directive
				   may enclose */
	int dt_nenc;		/* no of entries in dt_inc */
};

/* scenario directive tokens */
#define SD_INCLUDE		1
#define SD_PARALLEL		2
#define SD_END_PARALLEL		3
#define SD_REPEAT		4
#define SD_END_REPEAT		5
#define SD_RANDOM		6
#define SD_END_RANDOM		7
#define SD_TIMED_LOOP		8
#define SD_END_TIMED_LOOP	9
#define SD_VARIABLE		10
#define SD_END_VARIABLE		11
#define SD_SEQUENTIAL		12
#define SD_END_SEQUENTIAL	13
#ifndef TET_LITE	/* -START-LITE-CUT- */
#  define SD_REMOTE		14
#  define SD_END_REMOTE		15
#  define SD_DISTRIBUTED	16
#  define SD_END_DISTRIBUTED	17
#endif /* !TET_LITE */	/* -END-LITE-CUT- */

/* scenario directive flags - used in the first syntax checker */
#define SDF_OPT_ARG		00001	/* may take one optional argument */
#define SDF_OPT_MARG		00002	/* may take one or more optional
					   arguments */
#define SDF_NEED_ARG		00004	/* must have exactly one argument */
#define SDF_NEED_MARG		00010	/* must have one or more arguments */
#define SDF_NUMERIC_ARGS	00020	/* arg(s) must be non-negative
					   numbers */
#define SDF_NRANGE_ARGS		00040	/* arg(s) must be non-negative
					   numbers or number ranges */
#define SDF_VARFMT_ARGS		00100	/* arg(s) must be of the form
					   name=value */
#define SDF_OPT_ATTACH		00200	/* may have an attached element */
#define SDF_NEED_ATTACH		00400	/* must have an attached element */
#define SDF_ENDDIR		01000	/* an end token is implied after
					   processing an attached test list
					   or file name */

/* function declarations */
extern struct dirtab *getdirbyname PROTOLIST((char *));
extern struct dirtab *getdirbyvalue PROTOLIST((int));

