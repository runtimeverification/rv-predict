#include <err.h>
#include <inttypes.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>	/* for EXIT_FAILURE */
#include <unistd.h>	/* for STDIN_FILENO */

#include "nbcompat.h"

static void __dead
usage(const char *progname)
{
	fprintf(stderr, "usage: %s [trace-file]\n", progname);
	exit(EXIT_FAILURE);
}

int
main(int argc, char **argv)
{
	uint64_t id;
	FILE *inf;
	const char *progname = argv[0], *fname;

	switch (argc) {
	case 1:
		inf = stdin;
		break;
	case 2:
		fname = argv[1];
		if ((inf = fopen(fname, "r")) == NULL) {
			err(EXIT_FAILURE, "%s: fopen(\"%s\", ...)", __func__,
			    fname);
		}
		break;
	default:
		usage(progname);
	}

	while (fread(&id, sizeof(id), 1, inf) != 0) {
		char buf[1024];
		char *p = &buf[0];

		for (;;) {
			int c = fgetc(inf);
			if (c == EOF) {
				// records end with '\0', so quit with error
				*p++ = '\0';
				errx(EXIT_FAILURE,
				    "truncated record: %" PRIu64 ": %s...\n", id, buf);
			} else if (c == '\0') {
				*p++ = '\0';
				printf("%#016" PRIx64 ": %s\n", id, buf);
				break;
			} else {
				*p++ = (char)c;
			}
		}
	}
	if (ferror(inf) != 0)
		errx(EXIT_FAILURE, "%s: error reading input", __func__);

	return 0;
}
