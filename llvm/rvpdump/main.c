#include <sys/types.h>	/* for open(2) */
#include <sys/stat.h>	/* for open(2) */
#include <err.h>
#include <fcntl.h>	/* for open(2) */
#include <stdio.h>
#include <stdlib.h>	/* for EXIT_* */
#include <unistd.h>	/* for STDIN_FILENO */

#include "reader.h"

static void
usage(const char *progname)
{
	fprintf(stderr, "usage: %s [<trace file>]\n", progname);
	exit(EXIT_FAILURE);
}

int
main(int argc, char **argv)
{
	int fd;
	const char *inputname;

	if (argc > 2) {
		usage(argv[0]);
	} else if (argc == 2) {
		inputname = argv[1];
		fd = open(inputname, O_RDONLY);
		if (fd == -1) {
			err(EXIT_FAILURE, "%s: open(\"%s\")",
			    __func__, inputname);
		}
	} else {
		fd = STDIN_FILENO;
		inputname = "<stdin>";
	}

	rvp_trace_dump(fd);

	return EXIT_SUCCESS;
}
