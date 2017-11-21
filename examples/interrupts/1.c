#include <err.h>
#include <signal.h>
#include <stdbool.h>
#include <stdlib.h>
#include <unistd.h>

#include "lib.h"
#include "nbcompat.h"

/* Interrupt data-race category 1: a low-priority thread is interrupted
 * during a load; the interrupt handler performs a store; the thread
 * potentially reads a value that is different both from the previous value
 * and from the value the interrupt handler wrote.
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
main(void)
{
	sigset_t oldset;

	pthread_sigmask(SIG_SETMASK, NULL, &oldset);
	establish(handler);

	while (shared.count < 10)
		pause();

	pthread_sigmask(SIG_SETMASK, &oldset, NULL);
	return EXIT_SUCCESS;
}
