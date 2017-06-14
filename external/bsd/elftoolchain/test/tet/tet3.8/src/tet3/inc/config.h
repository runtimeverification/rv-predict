/*
 *	SCCS: @(#)config.h	1.3 (00/04/03)
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

SCCS:   	@(#)config.h	1.3 (00/04/03) TETware release 3.8
NAME:		config.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	August 1996

DESCRIPTION:
	a header file for use with configuration requests
	in tcc and tccd

MODIFICATIONS:
	Andrew Dingwall, UniSoft Ltd., March 2000
	Added TET_STRUCT_CFLIST_DEFINED so that program-specific header
	files can determine whether or not struct cflist is defined.

************************************************************************/

/* structure of a configuration list */
struct cflist {
	char **cf_conf;		/* ptr to the start of the list */
	int cf_lconf;		/* no of bytes in *cf_conf */
	int cf_nconf;		/* no of entries in *cf_conf */
};
#define TET_STRUCT_CFLIST_DEFINED

/* configuration modes - used to select a config list or file name */
#define CONF_BUILD	1
#define CONF_EXEC	2
#define CONF_CLEAN	3
#define CONF_DIST	4

/*
** macro to translate a config mode to an offset in an array of config
** lists or file names
**
** this macro is also used to generate the mode field in a journal
** Configuration Start line (see jnl_mode() in tcc/journal.c)
*/
#define TC_CONF_MODE(n)		((n) - 1)

/*
** macro to see if a config mode is valid for a particular array
** of config lists or file names
*/
#define CONF_MODE_OK(mode, array) \
	(TC_CONF_MODE(mode) >= 0 && \
		TC_CONF_MODE(mode) < (sizeof array / sizeof array[0]))

/*
** tccd configuration modes values used with OP_CONFIG and
** OP_SETCONF requests to define or select a per-mode configuration
*/

#define TC_CONF_BUILD	CONF_BUILD
#define TC_CONF_EXEC	CONF_EXEC
#define TC_CONF_CLEAN	CONF_CLEAN

/* number of configuration modes */
#define TC_NCONF_MODES	3

/* extern function declarations */
extern int tet_tcxconfig PROTOLIST((int, char *, struct cflist *,
	struct cflist *, struct cflist *));

