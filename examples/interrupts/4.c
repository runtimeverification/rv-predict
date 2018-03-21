/* Copyright (c) 2016,2017,2018 Runtime Verification, Inc.
 * All rights reserved.
 */
#include <err.h>
#include <libgen.h>
#include <signal.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <inttypes.h>

#include "lib.h"
#include "nbcompat.h"

/* Interrupt data-race category 4:
 *
 * A low-priority thread is interrupted while it loads a shared bit, L;
 * the interrupt handler performs a store to a nearby second bit, M; the
 * bit accesses compile to wider accesses (e.g., byte or word accesses)
 * that overlap.  The thread potentially reads a value that is different
 * both from the previous value and from the value the interrupt handler
 * wrote.
 */

struct {
	uint32_t b0:1;
	uint32_t b1:1;
	uint32_t b2:1;
	uint32_t b3:1;
	uint32_t b4:1;
	uint32_t b5:1;
	uint32_t b6:1;
	uint32_t b7:1;
	uint32_t b8:1;
	uint32_t b9:1;
	uint32_t b10:1;
	uint32_t b11:1;
	uint32_t b12:1;
	uint32_t b13:1;
	uint32_t b14:1;
	uint32_t b15:1;
	uint32_t b16:1;
	uint32_t b17:1;
	uint32_t b18:1;
	uint32_t b19:1;
	uint32_t b20:1;
	uint32_t b21:1;
	uint32_t b22:1;
	uint32_t b23:1;
	uint32_t b24:1;
	uint32_t b25:1;
	uint32_t b26:1;
	uint32_t b27:1;
	uint32_t b28:1;
	uint32_t b29:1;
	uint32_t b30:1;
	uint32_t b31:1;
} shared_bitfield = {.b0 = 1, .b2 = 1, .b31 = 1};

static void
handler(int signum __unused)
{
	shared_bitfield.b0 ^= 1;
}

int
main(int argc __unused, char **argv)
{
	int i;
	sigset_t oldset;

	pthread_sigmask(SIG_SETMASK, NULL, &oldset);
	establish(handler, basename(argv[0])[0] == 'r');

	for (i = 0; i < 10; i++) {
		printf("b1 = %u\n", shared_bitfield.b1);
		pause();
	}

	pthread_sigmask(SIG_SETMASK, &oldset, NULL);
	return EXIT_SUCCESS;
}
