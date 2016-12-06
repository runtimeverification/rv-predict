#include <stdlib.h>
#include "spcq.h"

#if defined(SPCQ_ORDERED)
#define	spcq_order_acquire memory_order_acquire
#define	spcq_order_release memory_order_release
#else
#define	spcq_order_acquire memory_order_relaxed
#define	spcq_order_release memory_order_relaxed
#endif /* defined(SPCQ_ORDERED) */

#ifndef offsetof
#define offsetof(__type, __member)	\
	(size_t)((char *)&((__type)NULL)->__member - (char *)((__type)NULL))
#endif

spcq_t *
spcq_alloc(int nitems)
{
	spcq_t *q;

	if ((q = malloc(offsetof(spcq_t, items[nitems]))) == NULL)
		return NULL;

	q->producer = q->consumer = 0;
	q->nitems = nitems;
	return q;
}

bool
spcq_put(spcq_t *q, void *item)
{
	int prev = q->producer;
	int next = (prev == q->nitems - 1) ? 0 : (prev + 1);

	if (next == q->consumer)
		return false;

	q->items[q->producer] = item;
	atomic_store_explicit(&q->producer, next, spcq_order_release);
	return true;
}

void *
spcq_get(spcq_t *q)
{
	int prev = q->consumer;
	int next = (prev == q->nitems - 1) ? 0 : (prev + 1);

	if (prev == atomic_load_explicit(&q->producer, spcq_order_acquire))
		return NULL;

	void *item = q->items[prev];
	atomic_store_explicit(&q->consumer, next, spcq_order_release);
	return item;
}
