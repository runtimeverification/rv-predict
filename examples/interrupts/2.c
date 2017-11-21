#include <err.h>
#include <signal.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "lib.h"
#include "nbcompat.h"

/* Interrupt data-race category 2: a low-priority thread is interrupted
 * during a store; the interrupt handler performs a load; the interrupt
 * handler potentially reads a value that is different both from the
 * original value and from the value the low-priority thread was
 * writing.
 */

struct {
	_Atomic bool protected;
	int count;
} shared = {.protected = false, .count = 0};

static void
handler(int signum __unused)
{
	const char msg[] = "shared.count == 10\n";

	if (shared.count == 10)
		(void)write(STDOUT_FILENO, msg, strlen(msg));
}

int
main(void)
{
	int i;
	sigset_t oldset;

	pthread_sigmask(SIG_SETMASK, NULL, &oldset);
	establish(handler);

	for (i = 1; i <= 10; i++) {
		shared.count = i;
		pause();
	}

	pthread_sigmask(SIG_SETMASK, &oldset, NULL);
	return EXIT_SUCCESS;
}
