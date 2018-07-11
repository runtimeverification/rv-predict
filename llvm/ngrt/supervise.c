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

#include "init.h"
#include "interpose.h"
#include "nbcompat.h"	/* for __arraycount() */
#include "supervise.h"

static const char *tmproot = "/tmp";
static const char *self_exe_pathname = "/proc/self/exe";
static const char *trace_var = "RVP_TRACE_FILE";
static const int killer_signum[] = {SIGHUP, SIGINT, SIGQUIT, SIGPIPE, SIGALRM,
    SIGTERM};
int rvp_analysis_fd = -1;

volatile _Atomic bool __read_mostly rvp_initialized = false;

const char *product_name = "RV-Predict/C";

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
	int i;
	sigset_t s;

	if (sigemptyset(&s) == -1)
		goto errexit;

	for (i = 0; i < __arraycount(killer_signum); i++) {
		if (sigaddset(&s, killer_signum[i]) == -1)
			goto errexit;
	}

	if ((errno = real_pthread_sigmask(SIG_BLOCK, &s, omask)) == 0)
		return;
errexit:
	err(EXIT_FAILURE, "%s could not block signals", product_name);
}

static void
ignore_signals(struct sigaction *action, size_t nactions)
{
	int i;
	struct sigaction sa;

	memset(&sa, '\0', sizeof(sa));
	sa.sa_handler = SIG_IGN;
	if (sigemptyset(&sa.sa_mask) == -1)
		goto errexit;

	if (nactions < __arraycount(killer_signum)) {
		errx(EXIT_FAILURE,
		    "%s: too few `struct sigaction` to hold state", __func__);
	}

	for (i = 0; i < __arraycount(killer_signum); i++) {
		if (real_sigaction(killer_signum[i], &sa, &action[i]) == -1)
			goto errexit;
	}

	return;

errexit:
	err(EXIT_FAILURE, "%s could not ignore signals", product_name);
}

static void
reset_signals(const struct sigaction *action)
{
	int i;

	for (i = 0; i < __arraycount(killer_signum); i++) {
		if (real_sigaction(killer_signum[i], &action[i], NULL) == -1) {
			err(EXIT_FAILURE, "%s could not reset signals",
			    product_name);
		}
	}
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

static void
sigchld(int signo __unused)
{
	return;
}

static void
prepare_to_wait_for_analysis(struct sigaction *action, sigset_t *setp)
{
	int i;
	struct sigaction sa = {.sa_handler = sigchld};

	if (sigemptyset(&sa.sa_mask) == -1)
		err(EXIT_FAILURE, "%s.%d: sigemptyset", __func__, __LINE__);

	if (sigemptyset(setp) == -1)
		err(EXIT_FAILURE, "%s.%d: sigemptyset", __func__, __LINE__);

	for (i = 0; i < __arraycount(killer_signum); i++) {
		if (sigaddset(setp, killer_signum[i]) == -1) {
			err(EXIT_FAILURE, "%s.%d: sigaddset", __func__,
			    __LINE__);
		}
	}
	if (sigaddset(setp, SIGCHLD) == -1)
		err(EXIT_FAILURE, "%s.%d: sigaddset", __func__, __LINE__);

	if (real_pthread_sigmask(SIG_BLOCK, setp, NULL) == -1)
		err(EXIT_FAILURE, "%s: pthread_sigmask", __func__);
	if (real_sigaction(SIGCHLD, &sa, NULL) == -1)
		err(EXIT_FAILURE, "%s: sigaction", __func__);

	reset_signals(action);
}

static int __printflike(1, 2)
dbg_printf(const char *fmt, ...)
{
	va_list ap;
	int rc;

	if (!rvp_debug_supervisor)
		return 0;

	va_start(ap, fmt);
	rc = vfprintf(stderr, fmt, ap);
	va_end(ap);
	return rc;
}

static void
rvp_online_analysis_start(void)
{
	struct sigaction action[__arraycount(killer_signum)];
	int astatus, sstatus;
	pid_t supervisee_pid, analysis_pid, parent_pid;
	int pipefd[2];
	sigset_t waitset;
	pid_t quit_pid;
	int signo;

	char *const binpath =
	    get_binary_path(/* (argc > 0) ? argv[0] : NULL */);

	if (binpath == NULL) {
		errx(EXIT_FAILURE, "%s could not find a path to the "
		    "executable binary that it was running", product_name);
	}

	if (pipe(pipefd) == -1) {
		err(EXIT_FAILURE,
		    "%s could not create an event pipeline", product_name);
	}
	const int piperd = pipefd[0];
	const int pipewr = pipefd[1];

	// TBD Avoid using some arbitrary file in the analysis.
	// Check binpath for an RV-Predict/C runtime symbol, and see if
	// its address matches the address of our own copy?

	if ((supervisee_pid = fork()) == -1) {
		err(EXIT_FAILURE,
		    "%s could not fork a supervisee process", product_name);
	}

	ignore_signals(action, __arraycount(action));

	/* the child process runs the program: return to its main routine */
	if (supervisee_pid == 0) {
		rvp_analysis_fd = pipewr;
		(void)close(piperd);
		reset_signals(action);
		return;
	}

	if (close(pipewr) == -1) {
		err(EXIT_FAILURE,
		    "%s could not close event pipeline inlet", product_name);
	}

	analysis_pid = fork();

	if (analysis_pid == 0) {
		char cmdname[] = "rvpa", tracename[] = "/dev/stdin";
		char *const args[] = {cmdname, binpath, tracename, NULL};

		/* We move the analyzer into its own process group
		 * so that signals generated on the terminal (e.g.,
		 * Control-C -> SIGINT) are not delivered to it.
		 */
		if (setpgid(0, 0) == -1) {
			err(EXIT_FAILURE,
			    "%s could not put the analyzer into its own "
			    "process group", product_name);
		}

		reset_signals(action);

		/* establish read end of pipe as rvpa's stdin */
		if (dup2(piperd, STDIN_FILENO) == -1) {
			err(EXIT_FAILURE, "%s could not establish "
			    "the event pipeline as the analysis input",
			    product_name);
		}
		(void)close(piperd);

		if (execvp("rvpa", args) == -1) {
			err(EXIT_FAILURE,
			    "%s could not start an analysis process",
			    product_name);
		}
		// unreachable
	}

	if (close(piperd) == -1) {
		err(EXIT_FAILURE,
		    "%s could not close event pipeline outlet", product_name);
	}

	/* wait for the supervisee to finish */
	while (waitpid(supervisee_pid, &sstatus, WUNTRACED) == -1) {
		if (errno == EINTR)
			continue;
		err(EXIT_FAILURE, "%s failed unexpectedly while it "
		    "waited for the instrumented program",
		    product_name);
	}

	parent_pid = getppid();

	dbg_printf("%s.%d: supervisee finished, parent PID %d\n",
	    __func__, __LINE__, parent_pid);

	/* Now that the supervisee (the program under test) has
	 * finished, the supervisor blocks the signals in the
	 * `killer_signum` set, reestablishes default signal disposition,
	 * and then waits either for a signal in `killer_signum` to arrive,
	 * or for SIGCHLD.
	 *
	 * When signals in `killer_signum` arrive, the supervisor forwards
	 * them to the analysis process and continues waiting.
	 *
	 * When SIGCHLD arrives, the supervisor checks to see if the
	 * analysis process has finished.
	 *
	 * It is possible for the analysis process to finish during the
	 * interval when SIGCHLD is ignored, in which case the supervisor
	 * will never receive a SIGCHLD for the analysis.  So the supervisor
	 * checks whether or not the analysis process still runs before it
	 * waits for signals.
	 */

	prepare_to_wait_for_analysis(action, &waitset);
	if (parent_pid == 1) {
		if (WIFSIGNALED(sstatus)) {
			signo = WTERMSIG(sstatus);
			dbg_printf(
			    "%s belongs to init, forwarding %s to analysis\n",
			    product_name, strsignal(signo));
		} else {
			signo = SIGHUP;
			dbg_printf(
			    "%s belongs to init, killing analysis with %s\n",
			    product_name, strsignal(signo));
		}
		if (kill(-analysis_pid, signo) == -1) {
			err(EXIT_FAILURE, "%s.%d: kill",
			    __func__, __LINE__);
		}
	}

	if (analysis_pid == -1) {
		fprintf(stderr, "%s could not start the analysis process.\n",
		    product_name);
		goto supervisee_report;
	} else if ((quit_pid = waitpid(analysis_pid, &astatus, WNOHANG)) == -1) {
		err(EXIT_FAILURE, "%s: waitpid(analyzer)",
		    product_name);
	} else if (quit_pid == analysis_pid) {
		;
	} else while (sigwait(&waitset, &signo) == 0) {
		dbg_printf("%s: got signal %d (%s)\n", product_name,
		    signo, strsignal(signo));
		if (signo != SIGCHLD) {
			if (kill(-analysis_pid, signo) == -1) {
				err(EXIT_FAILURE, "%s.%d: kill",
				    __func__, __LINE__);
			}
			dbg_printf("%s: forwarded signal %d\n",
			    product_name, signo);
			continue;
		}
		/* wait for the analysis to finish */
		if (waitpid(analysis_pid, &astatus, 0) == -1) {
			err(EXIT_FAILURE,
			    "%s failed unexpectedly while it waited for the "
			    "analyzer to finish",
			    product_name);
		}
		break;
	}

	dbg_printf("%s.%d: analyzer finished\n", __func__, __LINE__);

	/* Print the status of the analyzer if it was cancelled
	 * by a signal.
	 */
	if (WIFSIGNALED(astatus)) {
		fprintf(stderr, "analyzer: %s", strsignal(WTERMSIG(astatus)));
#ifdef WCOREDUMP
		if (WCOREDUMP(astatus))
			fprintf(stderr, " (core dumped)");
#endif /* WCOREDUMP */
		fputc('\n', stderr);
	}

supervisee_report:

	/* Print the status of the supervisee if it was cancelled
	 * by a signal.
	 */
	if (WIFSIGNALED(sstatus)) {
		fprintf(stderr, "%s", strsignal(WTERMSIG(sstatus)));
#ifdef WCOREDUMP
		if (WCOREDUMP(sstatus))
			fprintf(stderr, " (core dumped)");
#endif /* WCOREDUMP */
		fputc('\n', stderr);
	}

	if (WIFSIGNALED(sstatus))
		exit(125);	// following xargs(1) here. :-)
	if (WIFEXITED(sstatus))
		exit(WEXITSTATUS(sstatus));
	exit(EXIT_SUCCESS);
}

void
rvp_supervision_start(void)
{
	int status;
	pid_t pid;
	const long path_max = pathconf(self_exe_pathname, _PC_NAME_MAX);
	char *binbase, *tmpdir, *tracename;
	int nwritten;
	sigset_t omask;

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
