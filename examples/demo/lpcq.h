#ifndef _LPCQ_H_
#define _LPCQ_H_

/* lpcq: (l)inked (p)roducer-(c)onsumer (q)ueue
 *
 * lpcq is a library for keeping a first-in-first-out (FIFO) queues as
 * singly-linked lists.  Items on a queue are linked through internal
 * pointers.
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
typedef struct _lpcq {
	void * volatile head;
	void * volatile * volatile tailp;
	int nextofs;
} lpcq_t;

typedef struct _lpcq_iter {
	void *item;
	int nextofs;
	void * volatile *lastnextp;
} lpcq_iter_t;

void lpcq_init(lpcq_t *, int);
void *lpcq_get(lpcq_t *);
void lpcq_put(lpcq_t *, void *);
lpcq_iter_t lpcq_getall(lpcq_t *);
void *lpcq_next(lpcq_iter_t *);

#endif /* _LPCQ_H_ */
