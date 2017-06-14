/*
 *      SCCS:  @(#)etab.h	1.5 (96/11/04) 
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

SCCS:   	@(#)etab.h	1.5 96/11/04 TETware release 3.8
NAME:		etab.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	executed process table description

MODIFICATIONS:

************************************************************************/

/*
**	Executed process table.
**
**	An entry is made in this table for each process started by OP_EXEC.
**	Storage for an element is allocated by etalloc() and freed by etfree().
**	An element is added to the table by etadd() and removed by etrm().
*/

/*
**	structure of the executed process process table - this is a linked list
**
**	the next and last pointers muts be first so as to allow the use of the
**	llist routines to manipulate the table
*/
struct etab {
	struct etab *et_next;		/* ptr to next element in list */
	struct etab *et_last;		/* ptr to last element in list */
	struct ptab *et_ptab;		/* ptr to parent process ptab */
	int et_pid;			/* pid of exec'd process */
	int et_state;			/* process state */
	int et_status;			/* status returned by wait() */
};

/* values for et_state (discrete values) */
#define ES_RUNNING	1
#define ES_TERMINATED	2


/* extern function declarations */
void etadd PROTOLIST((struct etab *));
struct etab *etalloc PROTOLIST((void));
struct etab *etfind PROTOLIST((int));
void etfree PROTOLIST((struct etab *));
void etrm PROTOLIST((struct etab *));

