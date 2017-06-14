/*
 *      SCCS:  @(#)llist.h	1.5 (96/11/04) 
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

SCCS:   	@(#)llist.h	1.5 96/11/04 TETware release 3.8
NAME:		llist.h
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	a header file for use with the linked list manipulation functions

MODIFICATIONS:

************************************************************************/

/*
** prototypical linked list structure -
** used by the tet_listinsert() and tet_listremove() library routines
*/

struct llist {
	struct llist *next;	/* ptr to next element - must be 1st */
	struct llist *last;	/* ptr to previous element - must be 2nd */
	/* char data[]; */
};


/* extern function declarations */
extern void tet_listinsert PROTOLIST((struct llist **, struct llist *));
extern void tet_listremove PROTOLIST((struct llist **, struct llist *));

