/* This program simply enters an infinite loop.  It's intended to
 * run until it is cancelled with `SIGINT`, so it doesn't block
 * or ignore any signals.
 */

#include <err.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>

#include "nbcompat.h"

int
main(void)
{
	for (;;);

	return EXIT_SUCCESS;
}
