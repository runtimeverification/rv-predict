/* Copyright (c) 2016 Runtime Verification, Inc.  All rights reserved. */

#include <sched.h>	/* for sched_yield() */
#include <sys/param.h>	/* for MAX */
#include <limits.h>	/* for INT_MAX */
#include <stdlib.h>	/* for NULL */
#ifndef lpcq_atomic 
#define lpcq_atomic
#endif /* lpcq_atomic */
#include "lpcq.h"

static inline void * lpcq_atomic volatile *
lpcq_nextp(int nextofs, void *item)
{
	return (void * lpcq_atomic volatile *)((char *)item + nextofs);
}

void
lpcq_init(lpcq_t *q, int nextofs)
{
	q->nextofs = nextofs;
	q->head = NULL;
	q->tailp = &q->head;
}

void *
lpcq_get(lpcq_t *q)
{
	void *item;

	item = q->head;

	if (item == NULL)
		return NULL;

	if (q->tailp == &q->head)
		return NULL;

	void * lpcq_atomic volatile *nextp = lpcq_nextp(q->nextofs, item);
	q->head = *nextp;
	if (q->tailp == nextp) {
		q->tailp = &q->head;
	}
	return item;
}

void
lpcq_put(lpcq_t *q, void *item)
{
	void * lpcq_atomic volatile *nextp = lpcq_nextp(q->nextofs, item);

	*nextp = NULL;
	void * lpcq_atomic volatile *otailp = q->tailp;
	q->tailp = nextp;
	*otailp = item;
}

lpcq_iter_t
lpcq_getall(lpcq_t *q)
{
	if (q->tailp == &q->head)
		return (lpcq_iter_t){.item = NULL, .nextofs = 0, .lastnextp = NULL};

	void *item = q->head;

	void * lpcq_atomic volatile *otailp = q->tailp;
	q->tailp = &q->head;

	return (lpcq_iter_t){.item = item,
	        .nextofs = q->nextofs,
		.lastnextp = otailp};
}

void *
lpcq_next(lpcq_iter_t *i)
{
	void *head = i->item;

	if (head == NULL)
		return NULL;

	void * lpcq_atomic volatile *nextp = lpcq_nextp(i->nextofs, head);
	if (nextp == i->lastnextp) {
		i->item = NULL;
		return head;
	}

	i->item = *nextp;
	return head;
}
