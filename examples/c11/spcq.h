/* Copyright (c) 2016,2017,2018 Runtime Verification, Inc.
 * All rights reserved.
 */
#include <stdatomic.h>
#include <stdbool.h>

typedef struct _spcq {
	int nitems;
	volatile _Atomic int consumer;
	volatile _Atomic int producer;
	void * volatile items[];
} spcq_t;

spcq_t *spcq_alloc(int);
bool spcq_put(spcq_t *, void *);
void *spcq_get(spcq_t *);

