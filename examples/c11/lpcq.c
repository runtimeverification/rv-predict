/* Copyright (c) 2016,2017,2018 Runtime Verification, Inc.
 * All rights reserved.
 */

#include <sched.h>	/* for sched_yield() */
#include <sys/param.h>	/* for MAX */
#include <limits.h>	/* for INT_MAX */
#include <stdlib.h>	/* for NULL */
#ifndef lpcq_atomic 
#define lpcq_atomic
#endif /* lpcq_atomic */
#include "lpcq.h"

/* lpcq: (l)inked (p)roducer-(c)onsumer (q)ueue
 *
 * lpcq is a library for keeping a first-in-first-out (FIFO) queues as
 * singly-linked lists.  Items on a queue are linked through internal
 * pointers.
 *
 * `lpcq` producer and consumer threads must arrange to synchronize
 * their calls to `lpcq_put` and `lpcq_get`.  Otherwise, the queue will
 * be corrupted.
 *
 * lpcq_t: storage type for a queue head
 *
 * lpcq_init(lpcq_t *q, int offset): initialize queue `q` to hold
 *     items that form a singly-linked list through the internal pointer
 *     at `offset` bytes from the first byte of each item.
 *
 * void lpcq_put(lpcq_t *q, void *item): insert `item` at the
 *     tail of `q`
 *
 * void *lpcq_get(lpcq_t *q): remove the item at the head of `q`
 *     and return it, or return NULL if the queue is empty.
 *
 * lpcq_iter_t lpcq_getall(lpcq_t *q): remove all items from the
 *     head of `q`.  Return an iterator for the items, lpcq_iter_t. 
 *
 * void *lpcq_next(lpcq_iter_t *iter): return the first item on the
 *     iterator `iter` and advance the iterator by one item, or
 *     return NULL if no items remain.   
 */

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
