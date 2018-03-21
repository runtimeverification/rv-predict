/* Copyright (c) 2016,2017,2018 Runtime Verification, Inc.
 * All rights reserved.
 */
#include <err.h>
#include <libgen.h>
#include <signal.h>
#include <stdatomic.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <inttypes.h>

#include "lib.h"
#include "nbcompat.h"

/* Interrupt data-race category 5:
 *
 * A low-priority thread is interrupted while it loads a shared array
 * element, L; the interrupt handler performs a store to a nearby second
 * array element, M; the array accesses compile to wider accesses (e.g.,
 * byte or word accesses) that overlap.  The thread potentially reads
 * a value that is different both from the previous value and from the
 * value the interrupt handler wrote.
 */

uint8_t shared_array[8];

static void
handler(int signum __unused)
{
	shared_array[0] ^= 1;
}

int
main(int argc __unused, char **argv)
{
	int i;
	sigset_t oldset;

	pthread_sigmask(SIG_SETMASK, NULL, &oldset);
	establish(handler, basename(argv[0])[0] == 'r');

	for (i = 0; i < 10; i++) {
		printf("shared_array[1] = %" PRIu8 "\n", shared_array[1]);
		pause();
	}

	pthread_sigmask(SIG_SETMASK, &oldset, NULL);
	return EXIT_SUCCESS;
}
