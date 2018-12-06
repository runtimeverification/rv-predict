#include <err.h>
#include <errno.h>
#include <limits.h>	/* for SSIZE_MAX */
#include <signal.h>	/* for pthread_sigmask(3) */
#include <stdio.h>
#include <stdarg.h>	/* for vfprintf(3) */
#include <stdint.h>	/* for intmax_t */
#include <stdlib.h>	/* for STDIN_FILENO */
#include <string.h>	/* for strdup(3), strlen(3) */
#include <sys/param.h>	/* for MIN */
#include <sys/wait.h>
#include <unistd.h>	/* for pathconf(2), pipe(2), readlink(2) */

#include "init.h"
#include "interpose.h"
#include "nbcompat.h"	/* for __arraycount() */
#include "supervise.h"

const char *self_exe_pathname = "/proc/self/exe";
static const int killer_signum[] = {SIGHUP, SIGINT, SIGQUIT, SIGPIPE, SIGALRM,
    SIGTERM};
int rvp_analysis_fd = -1;

bool _Atomic __read_mostly rvp_initialized = false;

const char *product_name = "RV-Predict/C";

void
ignore_signals(struct sigaction **actionp)
{
	int i;
	struct sigaction sa;
	struct sigaction *action;

	memset(&sa, '\0', sizeof(sa));
	sa.sa_handler = SIG_IGN;
	if (sigemptyset(&sa.sa_mask) == -1)
		goto errexit;

	action = calloc(__arraycount(killer_signum), sizeof(struct sigaction));
	if (action == NULL) {
		errx(EXIT_FAILURE,
		    "%s: could not allocate `struct sigaction` to hold state",
		    __func__);
	}

	for (i = 0; i < __arraycount(killer_signum); i++) {
		if (real_sigaction(killer_signum[i], &sa, &action[i]) == -1)
			goto errexit;
	}

	*actionp = action;

	return;

errexit:
	err(EXIT_FAILURE, "%s could not ignore signals", product_name);
}

void
reset_signals(struct sigaction **actionp)
{
	int i;
	struct sigaction *action = *actionp;

	*actionp = NULL;

	for (i = 0; i < __arraycount(killer_signum); i++) {
		if (real_sigaction(killer_signum[i], &action[i], NULL) == -1) {
			err(EXIT_FAILURE, "%s could not reset signals",
			    product_name);
		}
	}
	free(action);
}

int
sigaddset_killers(sigset_t *s)
{
	int i;
	for (i = 0; i < __arraycount(killer_signum); i++) {
		if (sigaddset(s, killer_signum[i]) == -1)
			return -1;
	}
	return 0;
}

char *
get_binary_path(void)
{
	char *linkname, *tightname;
	const long path_max = pathconf(self_exe_pathname, _PC_NAME_MAX);

	if (path_max == -1) {
		err(EXIT_FAILURE, "%s could not find out the "
		    "maximum filename length", product_name);
	}

	linkname = malloc(path_max + 1);
	if (linkname == NULL) {
		errx(EXIT_FAILURE, "%s could not allocate "
		    "memory for the link name in %s", product_name,
		    self_exe_pathname); 
	}

	const ssize_t nread = readlink(self_exe_pathname, linkname,
	    path_max + 1);

	if (nread == -1) {
#if 0
		/* this is where we can compensate for missing
		 * /proc by using argv[0]
		 */
		if (errno == ENOENT)
			return argv0;
#endif
		err(EXIT_FAILURE,
		    "%s could not read the link name from %s",
		    product_name, self_exe_pathname);
	}

	if (nread > path_max) {
		free(linkname);
		errx(EXIT_FAILURE, "%s read a link at %s that was bigger than "
		    "expected: %zd bytes", product_name, self_exe_pathname,
		    nread);
	}
	linkname[nread] = '\0';
	/* Fit the buffer more tightly to the link name. */
	if (nread + 1 < path_max + 1 &&
	    (tightname = realloc(linkname, nread + 1)) != NULL)
		return tightname;
	return linkname;
}

void
rvp_supervision_start(void)
{
	/* __rvpredict_init() set rvp_trace_only to true if the
	 * environment variable RVP_TRACE_ONLY is set to "yes".
	 * Resume running the main routine immediately if it is "yes"
	 * so that we drop a trace file into the working directory.
	 */
	if (rvp_trace_only)
		return;

	if (rvp_online_analysis) {
		rvp_online_analysis_start();
		return;
	}

	rvp_offline_analysis_start();
}
