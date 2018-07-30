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
#include <sys/stat.h>	/* for lstat(2), fts(3) */
#include <sys/types.h>	/* for waitpid(2), fts(3) */
#include <fts.h>
#include <sys/wait.h>
#include <unistd.h>	/* for pathconf(2), pipe(2), readlink(2) */

#include "interpose.h"	/* for real_pthread_sigmask */
#include "supervise.h"

static const char *tmproot = "/tmp";
static const char *trace_var = "RVP_TRACE_FILE";

static void
restore_signals(sigset_t *omask)
{
	if ((errno = real_pthread_sigmask(SIG_SETMASK, omask, NULL)) != 0) {
		err(EXIT_FAILURE,
		    "%s could not restore the signal mask", product_name);
	}
}

static void
block_signals(sigset_t *omask)
{
	sigset_t s;

	if (sigemptyset(&s) == -1)
		goto errexit;

	if (sigaddset_killers(&s) == -1)
		goto errexit;

	if ((errno = real_pthread_sigmask(SIG_BLOCK, &s, omask)) == 0)
		return;
errexit:
	err(EXIT_FAILURE, "%s could not block signals", product_name);
}

static void
cleanup(char *tmpdir)
{
	FTS *tree;
	FTSENT *entry;

	char *paths[] = {tmpdir, NULL};

	if ((tree = fts_open(paths, FTS_PHYSICAL, NULL)) == NULL) {
		err(EXIT_FAILURE, "%s could not clean up its "
		    "temporary directory at %s", product_name, tmpdir);
	}

	while ((entry = fts_read(tree)) != NULL) {
		switch (entry->fts_info) {
		case FTS_F:
		case FTS_SL:
		case FTS_SLNONE:
		case FTS_DEFAULT:
			if (unlink(entry->fts_path) == -1 && errno != ENOENT) {
				warn("%s encountered "
				    "an error at non-directory path %s",
				    product_name, entry->fts_path);
			}
			break;
		case FTS_DNR:
		case FTS_ERR:
		case FTS_NS:
			warnx("%s encountered an error "
			    "at path %s: %s", product_name, entry->fts_path,
			    strerror(entry->fts_errno));
			break;
		case FTS_DP:
			if (rmdir(entry->fts_path) == -1) {
				err(EXIT_FAILURE, "%s could not "
				    "remove a temporary directory",
				    product_name);
			}
			break;
		default:
			continue;
		}
	}
}

void
rvp_offline_analysis_start(void)
{
	int status;
	pid_t pid;
	const long path_max = pathconf(self_exe_pathname, _PC_NAME_MAX);
	char *binbase, *tmpdir, *tracename;
	int nwritten;
	sigset_t omask;

	char *const binpath =
	    get_binary_path(/* (argc > 0) ? argv[0] : NULL */);

	if (binpath == NULL) {
		errx(EXIT_FAILURE, "%s could not find a path to the "
		    "executable binary that it was running", product_name);
	}

	if ((tmpdir = malloc(path_max + 1)) == NULL ||
	    (tracename = malloc(path_max + 1)) == NULL) {
		errx(EXIT_FAILURE,
		    "%s could not allocate two buffers of %ld bytes",
		    product_name, path_max + 1);
	}

	if ((binbase = strdup(binpath)) == NULL) {
		errx(EXIT_FAILURE,
		    "%s could not allocate %ld bytes storage", product_name,
		    strlen(binpath) + 1);
	}

	nwritten = snprintf(tmpdir, path_max + 1, "%s/rvprt-%s.XXXXXX",
	    tmproot, basename(binbase));

	if (nwritten < 0 || nwritten >= path_max + 1) {
		errx(EXIT_FAILURE,
		    "%s could not create a temporary directory name",
		    product_name);
	}

	if (mkdtemp(tmpdir) == NULL) {
		err(EXIT_FAILURE,
		    "%s could not create a temporary directory", product_name);
	}

	nwritten = snprintf(tracename, path_max + 1, "%s/rvpredict.trace",
	    tmpdir);

	if (nwritten < 0 || nwritten >= path_max + 1) {
		errx(EXIT_FAILURE,
		    "%s could not create a trace filename", product_name);
	}

	if (setenv(trace_var, tracename, 1) != 0) {
		err(EXIT_FAILURE,
		    "%s could not export a trace filename to "
		    "the environment", product_name);
	}

	free(tracename);
	free(binbase);

	// TBD Avoid using some arbitrary file in the analysis.
	// Check binpath for an RV-Predict/C runtime symbol, and see if
	// its address matches the address of our own copy?

	if ((pid = fork()) == -1) {
		err(EXIT_FAILURE,
		    "%s could not fork a supervisor process", product_name);
	}

	block_signals(&omask);

	/* the child process runs the program: return to its main routine */
	if (pid == 0) {
		restore_signals(&omask);
		return;
	}

	/* wait for the child to finish */
	while (waitpid(pid, &status, 0) == -1) {
		if (errno == EINTR)
			continue;
		err(EXIT_FAILURE,
		    "%s failed unexpectedly "
		    "while it waited for the instrumented program",
		    product_name);
	}

	if (WIFSIGNALED(status)) {
		fprintf(stderr, "%s", strsignal(WTERMSIG(status)));
#ifdef WCOREDUMP
		if (WCOREDUMP(status))
			fprintf(stderr, " (core dumped)");
#endif /* WCOREDUMP */
		fputc('\n', stderr);
	}

	if ((pid = fork()) == -1) {
		err(EXIT_FAILURE,
		    "%s could not fork an analyzer", product_name);
	}

	if (pid == 0) {
		char cmdname[] = "rvpa";
		char *const args[] = {cmdname, binpath, NULL};

		if (chdir(tmpdir) == -1) {
			err(EXIT_FAILURE, "%s could not change to "
			    "its temporary directory", product_name);
		}
		if (execvp("rvpa", args) == -1) {
			err(EXIT_FAILURE,
			    "%s could not start an analyzer",
			    product_name);
		}
		// unreachable
	}

	/* wait for the child to finish */
	while (waitpid(pid, NULL, 0) == -1) {
		if (errno == EINTR)
			continue;
		err(EXIT_FAILURE,
		    "%s failed unexpectedly "
		    "while it waited for the analyzer to finish",
		    product_name);
	}

	cleanup(tmpdir);

	if (WIFSIGNALED(status))
		exit(125);	// following xargs(1) here. :-)
	if (WIFEXITED(status))
		exit(WEXITSTATUS(status));
	exit(EXIT_SUCCESS);
}
