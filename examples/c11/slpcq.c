/* Copyright (c) 2016 Runtime Verification, Inc.  All rights reserved. */

#include <sched.h>	/* for sched_yield() */
#include <sys/param.h>	/* for MAX */
#include <limits.h>	/* for INT_MAX */
#include <stdatomic.h>
#include <stdlib.h>	/* for NULL */
#ifndef lpcq_atomic
#define lpcq_atomic _Atomic
#endif /* lpcq_atomic */
#include "lpcq.h"

/* slpcq: (s)ynchronized (l)inked (p)roducer-(c)onsumer (q)ueue
 *
 * slpcq is a library for keeping a first-in-first-out (FIFO) queues as
 * singly-linked lists.  Items on a queue are linked through internal
 * pointers.
 *
 * `slpcq` types and functions use the same prefix as `lpcq` so that
 * they are interchangeable in data-race detection tests.
 *
 * It is safe for multiple producers and a single consumer to
 * concurrently access a single `slpcq`: the `lpcq_put` and `lpcq_get`
 * routines synchronize through careful use of atomic compare-and-set
 * operations.
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

static inline void * _Atomic volatile *
lpcq_nextp(int nextofs, void *item)
{
	return (void * _Atomic volatile *)((char *)item + nextofs);
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
	void * _Atomic volatile *tailp;
	void * _Atomic volatile *nextp;

	item = q->head;

	/* TBD read barrier */

	if (item == NULL)
		return NULL;

	do {
		nextp = lpcq_nextp(q->nextofs, item);

		/* TBD read barrier */

		tailp = q->tailp;

		if (tailp == &q->head)
			return NULL;

		q->head = *nextp;
		if (tailp != nextp)
			return item;

	} while (!atomic_compare_exchange_strong(&q->tailp, &nextp, &q->head));

	return item;
}

void
lpcq_put(lpcq_t *q, void *item)
{
	void * _Atomic volatile *nextp = lpcq_nextp(q->nextofs, item);

	*nextp = NULL;
	void * _Atomic volatile *otailp = q->tailp;
	while (!atomic_compare_exchange_strong(&q->tailp, &otailp, nextp))
		;	// TBD backoff
	*otailp = item;
}

#if 1
lpcq_iter_t
lpcq_getall(lpcq_t *q)
{
	void *item = q->head;

	if (item == NULL)
		return (lpcq_iter_t){.item = NULL, .nextofs = 0, .lastnextp = NULL};

	void * _Atomic volatile *otailp = q->tailp;

	if (otailp == &q->head)
		return (lpcq_iter_t){.item = NULL, .nextofs = 0, .lastnextp = NULL};

	while (!atomic_compare_exchange_strong(&q->tailp, &otailp, &q->head))
		;	// TBD backoff

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
#endif
