#include <err.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>

#include "supervise.h"

void
__rvpredict_main_entry(int argc, char **argv)
{
	int status;
	pid_t pid;

	if ((pid = fork()) == -1) {
		err(EXIT_FAILURE,
		    "RV-Predict/C could not fork an analysis process");
	}
	/* the child process runs the program */
	if (pid == 0)
		return;
	/* the parent waits for the child to finish */
	if (waitpid(pid, &status, 0) == -1) {
		err(EXIT_FAILURE,
		    "RV-Predict/C failed unexpectedly "
		    "while it waited for the instrumented program");
	}
	if (execvp("rvpa", {"rvpa", argv[0], NULL}) == -1) {
		err(EXIT_FAILURE,
		    "RV-Predict/C could not start the analyzer");
	}
	return EXIT_FAILURE;
}
