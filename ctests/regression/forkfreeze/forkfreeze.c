/* The RV-Predict/C runtime had a bug where child processes created by
 * `fork(2)` would freeze with full ring-buffers because they had no
 * serialization thread.
 *
 * As of 24 Aug 2018 there is a stopgap fix: in a child process, the
 * Predict runtime creates a new serialization thread that empties
 * ring-buffers to /dev/null.  (A new relay thread is also created.)
 * Events in the child process will not be traced, however, the process
 * will continue to run.
 */

#include <err.h>
#include <signal.h>
#include <stdlib.h>
#include <unistd.h>	/* for fork(2) */
#include <sys/types.h>	/* for waitpid(2) */
#include <sys/wait.h>	/* for waitpid(2) */

#include "nbcompat.h"

volatile static int z;

int
main(void)
{
	pid_t child;
	int i, status;
	const int times = 1000 * 1000;

	for (i = 0; i < times; i++)
		z = i;

	if ((child = fork()) == -1)
		err(EXIT_FAILURE, "%s: fork", __func__);

	if (child == 0) {
		for (i = 0; i < times; i++)
			z = i;
	} else if (waitpid(child, &status, 0) == -1)
		err(EXIT_FAILURE, "%s: waitpid", __func__);

	return EXIT_SUCCESS;
}
