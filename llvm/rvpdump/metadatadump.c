#include <err.h>
#include <inttypes.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>	/* for EXIT_FAILURE */
#include <unistd.h>	/* for STDIN_FILENO */

int
main(int argc, char **argv)
{
	uint64_t id;

	while (fread(&id, sizeof(id), 1, stdin) != 0) {
		char buf[1024];
		char *p = &buf[0];

		for (;;) {
			int c = fgetc(stdin);
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
	if (ferror(stdin) != 0)
		errx(EXIT_FAILURE, "%s: error reading input", __func__);

	return 0;
}
