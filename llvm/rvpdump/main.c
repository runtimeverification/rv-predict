#include <sys/types.h>	/* for open(2) */
#include <sys/stat.h>	/* for open(2) */
#include <err.h>
#include <fcntl.h>	/* for open(2) */
#include <stdio.h>
#include <stdlib.h>	/* for EXIT_* */
#include <string.h>	/* strcmp(3) */
#include <unistd.h>	/* for STDIN_FILENO */

#include "nbcompat.h"
#include "reader.h"

static void __dead
usage(const char *progname)
{
	fprintf(stderr, "usage: %s [-t <plain|legacy>] [<trace file>]\n",
	    progname);
	exit(EXIT_FAILURE);
}

int
main(int argc, char **argv)
{
	int ch, fd;
	const char *inputname;
	const char *progname = argv[0];
	rvp_output_type_t otype = RVP_OUTPUT_PLAIN_TEXT;

	while ((ch = getopt(argc, argv, "t:")) != -1) {
		switch (ch) {
		case 't':
			if (strcmp(optarg, "legacy") == 0)
				otype = RVP_OUTPUT_LEGACY_BINARY;
			else if (strcmp(optarg, "plain") == 0)
				otype = RVP_OUTPUT_PLAIN_TEXT;
			else
				usage(progname);
			break;
		default: /* '?' */
			usage(progname);
		}
	}

	argc -= optind;
	argv += optind;
 
	if (argc > 1) {
		usage(progname);
	} else if (argc == 1) {
		inputname = argv[0];
		fd = open(inputname, O_RDONLY);
		if (fd == -1) {
			err(EXIT_FAILURE, "%s: open(\"%s\")",
			    __func__, inputname);
		}
	} else {
		fd = STDIN_FILENO;
		inputname = "<stdin>";
	}

	rvp_trace_dump(otype, fd);

	return EXIT_SUCCESS;
}
