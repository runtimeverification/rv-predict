#include <err.h>
#include <libgen.h>
#include <signal.h>
#include <stdbool.h>
#include <stdlib.h>
#include <unistd.h>

#include "lib.h"
#include "nbcompat.h"

/* Interrupt data-race category 1:
 *
 * A low-priority thread is interrupted while it loads a shared memory
 * location, L; the interrupt handler performs a store that overlaps L;
 * the thread potentially reads a value that is different both from the
 * previous value and from the value the interrupt handler wrote.
 */

struct {
	_Atomic bool protected;
	int count;
} shared = {.protected = false, .count = 0};

static void
handler(int signum __unused)
{
	shared.count = 10;
}

int
main(int argc __unused, char **argv)
{
	sigset_t oldset;

	pthread_sigmask(SIG_SETMASK, NULL, &oldset);
	establish(handler, basename(argv[0])[0] == 'r');

	while (shared.count < 10)
		pause();

	pthread_sigmask(SIG_SETMASK, &oldset, NULL);
	return EXIT_SUCCESS;
}
