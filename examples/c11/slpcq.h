/* Copyright (c) 2016 Runtime Verification, Inc.  All rights reserved. */

#ifndef _SLPCQ_H_
#define _SLPCQ_H_

/* slpcq: (l)inked (p)roducer-(c)onsumer (q)ueue
 *
 * slpcq is a library for keeping a first-in-first-out (FIFO) queues as
 * singly-linked lists.  Items on a queue are linked through internal
 * pointers.
 *
 * slpcq_t: storage type for a queue head
 *
 * slpcq_init(slpcq_t *q, int offset): initialize queue `q` to hold
 *     items that form a singly-linked list through the internal pointer
 *     at `offset` bytes from the first byte of each item.
 *
 * void slpcq_put(slpcq_t *q, void *item): insert `item` at the
 *     tail of `q`
 *
 * void *slpcq_get(slpcq_t *q): remove the item at the head of `q`
 *     and return it, or return NULL if the queue is empty.
 *
 * slpcq_iter_t slpcq_getall(slpcq_t *q): remove all items from the
 *     head of `q`.  Return an iterator for the items, slpcq_iter_t. 
 *
 * void *slpcq_next(slpcq_iter_t *iter): return the first item on the
 *     iterator `iter` and advance the iterator by one item, or
 *     return NULL if no items remain.   
 */
typedef struct _slpcq {
	void * _Atomic volatile head;
	void * _Atomic volatile * _Atomic volatile tailp;
	int nextofs;
} slpcq_t;

typedef struct _slpcq_iter {
	void *item;
	int nextofs;
	void * _Atomic volatile * _Atomic lastnextp;
} slpcq_iter_t;

void slpcq_init(slpcq_t *, int);
void *slpcq_get(slpcq_t *);
void slpcq_put(slpcq_t *, void *);
slpcq_iter_t slpcq_getall(slpcq_t *);
void *slpcq_next(slpcq_iter_t *);

#endif /* _SLPCQ_H_ */
