/*
 *      SCCS:  @(#)llist.c	1.6 (96/11/04) 
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
static char sccsid[] = "@(#)llist.c	1.6 (96/11/04) TETware release 3.8";
#endif

/************************************************************************

SCCS:   	@(#)llist.c	1.6 96/11/04 TETware release 3.8
NAME:		llist.c
PRODUCT:	TETware
AUTHOR:		Andrew Dingwall, UniSoft Ltd.
DATE CREATED:	April 1992

DESCRIPTION:
	functions to manipulate doubly linked lists

MODIFICATIONS:

************************************************************************/


#include "dtmac.h"
#include "llist.h"
#include "error.h"

#ifdef NEEDsrcFile
static char srcFile[] = __FILE__;	/* file name for error reporting */
#endif

/*
**	tet_listinsert() - insert new element at the head of a linked list
*/

void tet_listinsert(head, elem)
register struct llist **head, *elem;
{
	ASSERT(head);
	ASSERT(elem);

	if (*head) 
		(*head)->last = elem;

	elem->next = *head;
	elem->last = (struct llist *) 0;

	*head = elem;
}

/*
**	tet_listremove() - remove an element from a linked list
*/

void tet_listremove(head, elem)
register struct llist **head, *elem;
{
	ASSERT(head);
	ASSERT(elem);

	if (elem->next)
		elem->next->last = elem->last;

	if (elem->last)
		elem->last->next = elem->next;
	else {
		ASSERT(elem == *head);
		*head = elem->next;
	}

	elem->last = elem->next = (struct llist *) 0;
}

