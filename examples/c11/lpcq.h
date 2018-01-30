/* Copyright (c) 2016,2017,2018 Runtime Verification, Inc.
 * All rights reserved.
 */

#ifndef _LPCQ_H_
#define _LPCQ_H_

#ifndef lpcq_atomic
#error "lpcq_atomic is not defined; it should be _Atomic or the empty string"
#endif /* lpcq_atomic */

typedef struct _lpcq {
	void * lpcq_atomic volatile head;
	void * lpcq_atomic volatile * lpcq_atomic volatile tailp;
	int nextofs;
} lpcq_t;

typedef struct _lpcq_iter {
	void *item;
	int nextofs;
	void * lpcq_atomic volatile * lpcq_atomic lastnextp;
} lpcq_iter_t;

void lpcq_init(lpcq_t *, int);
void *lpcq_get(lpcq_t *);
void lpcq_put(lpcq_t *, void *);
lpcq_iter_t lpcq_getall(lpcq_t *);
void *lpcq_next(lpcq_iter_t *);

#endif /* _LPCQ_H_ */
