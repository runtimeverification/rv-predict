/* Copyright (c) 2016,2017,2018 Runtime Verification, Inc.
 * All rights reserved.
 */
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>

#include "nbcompat.h"

/*
 * The following structure contains the necessary information
 * to allow the function "dotprod" to access its input data and
 * place its output into the structure.
 */

typedef struct {
	float *a, *b;
	float sum;
	int veclen;
} DOTDATA;

/* Define globally accessible variables */

#define NUMTHRDS 3
#define VECLEN   10
DOTDATA dotstr;
pthread_t callThd[NUMTHRDS];

/*
 * The function dotprod is activated when the thread is created.
 * All input to this routine is obtained from a structure
 * of type DOTDATA and all output from this function is written into
 * this structure. The benefit of this approach is apparent for the
 * multi-threaded program: when a thread is created we pass a single
 * argument to the activated function - typically this argument
 * is a thread number. All  the other information required by the
 * function is accessed from the globally accessible structure.
 */

static void *
dotprod(void *arg)
{
	/* Define and use local variables for convenience */

	int i, start, end, len;
	long offset;
	float mysum, *x, *y;
	offset = (long)arg;

	len = dotstr.veclen;
	start = offset * len;
	end   = start + len;
	x = dotstr.a;
	y = dotstr.b;

	/*
	 * Perform the dot product and assign result
	 * to the appropriate variable in the structure.
	 */

	mysum = 0;
	for (i = start; i < end; i++)
		mysum += x[i] * y[i];

	dotstr.sum += mysum;
	printf("Thread %ld did %2d to %2d:  mysum=%f global sum=%f\n",
	    offset, start, end, mysum, dotstr.sum);

	return NULL;
}

/*
 * The main program creates threads which do all the work and then
 * print out result upon completion. Before creating the threads,
 * the input data is created. Since all threads update a shared structure,
 * there is a race condition. The main thread needs to wait for
 * all threads to complete, it waits for each one of the threads. We specify
 * a thread attribute value that allow the main thread to join with the
 * threads it creates. Note also that we free up handles when they are
 * no longer needed.
 */

int
main(void)
{
	long i;
	float *a, *b;
	void *status;

	/* Assign storage and initialize values */
	a = (float *)malloc(NUMTHRDS * VECLEN * sizeof(float));
	b = (float *)malloc(NUMTHRDS * VECLEN * sizeof(float));

	for (i = 0; i < VECLEN * NUMTHRDS; i++) {
		a[i] = 1.0;
		b[i] = a[i];
	}

	dotstr.veclen = VECLEN;
	dotstr.a = a;
	dotstr.b = b;
	dotstr.sum = 0;


	/* Create threads to perform the dotproduct  */
	for(i = 0; i < NUMTHRDS; i++) {
		/*
		Each thread works on a different set of data.
		The offset is specified by 'i'. The size of
		the data for each thread is indicated by VECLEN.
		*/
		pthread_create(&callThd[i], NULL, dotprod, (void *)i);
	}

	/* Wait on the other threads */
	for(i = 0; i < NUMTHRDS; i++)
		pthread_join(callThd[i], &status);

	/* After joining, print out the results and cleanup */
	printf("Sum =  %f \n", dotstr.sum);
	free(a);
	free(b);
	return 0;
}
