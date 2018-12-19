#include <sys/types.h>	/* for open(2) */
#include <sys/stat.h>	/* for open(2) */
#include <err.h>
#include <errno.h>
#include <fcntl.h>	/* for open(2) */
#include <inttypes.h>	/* for intmax_t */
#include <limits.h>	/* for SIZE_MAX */
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>	/* for EXIT_* */
#include <string.h>	/* strcmp(3) */
#include <unistd.h>	/* for STDIN_FILENO */

#include "nbcompat.h"
#include "reader.h"
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include "merge_util.h"
#include "merge.h"
#include "op_to_info.h"

char const* output_dir;
uint32_t max_tid;

static void __dead
usage(const char *progname)
{
	fprintf(stderr,
	    "usage: %s [-b] [-g] "
	    "[-t <binary|plain|symbol-friendly>] [-n #traces] "
	   	"[-d <output dir>]\n"
	    "[<trace file>]\n", progname);
	exit(EXIT_FAILURE);
}

/*
static void
reemit_binary_init(const rvp_pstate_t *ps __unused,
    const rvp_trace_header_t *th)
{
	char filepath[1024];
	sprintf(filepath, "%s/%d.bin", output_dir, ps->ps_curthread);
	int filedesc = open(filepath, O_CREAT | O_WRONLY | O_APPEND, 0777);

	const ssize_t nwritten = write(filedesc, th, sizeof(*th));

	if (nwritten != sizeof(*th))
		err(EXIT_FAILURE, "%s: write", __func__);
}
*/

static void
reemit_binary_op(const rvp_pstate_t *ps __unused, const rvp_ubuf_t *ub,
    rvp_op_t op, bool is_load __unused, int field_width __unused)
{
	char filepath[1024];
	sprintf(filepath, "%s/%d.bin", output_dir, ps->ps_curthread);
	int filedesc;
	if( access( filepath, F_OK ) != -1 ) {
    	filedesc = open(filepath, O_WRONLY | O_APPEND, 0777);
	} else {
	    filedesc = open(filepath, O_CREAT | O_WRONLY | O_APPEND, 0777);
	    ssize_t nw = write(filedesc, &expected_trace_header, sizeof(expected_trace_header));
	    if(ps->ps_curthread > max_tid){
	    	max_tid = ps->ps_curthread;
	    }
	}

	printf("tid = %d, file = %s\n", ps->ps_curthread, filepath);

	const op_info_t *oi = &op_to_info[op];
	const ssize_t nwritten = write(filedesc, ub, oi->oi_reclen);

	if (nwritten != oi->oi_reclen)
		err(EXIT_FAILURE, "%s: write", __func__);
}

static void
reemit_binary_nop(const rvp_pstate_t *ps __unused, const rvp_ubuf_t *ub)
{

	char filepath[1024];
	sprintf(filepath, "%s/%d.bin", output_dir, ps->ps_curthread);
	int filedesc;
	if( access( filepath, F_OK ) != -1 ) {
    	filedesc = open(filepath, O_WRONLY | O_APPEND, 0777);
	} else {
	    filedesc = open(filepath, O_CREAT | O_WRONLY | O_APPEND, 0777);
	    ssize_t nw = write(filedesc, &expected_trace_header, sizeof(expected_trace_header));
	}

	printf("tid = %d, file = %s\n", ps->ps_curthread, filepath);

	const ssize_t nwritten =
	    write(filedesc, &ub->ub_pc, sizeof(ub->ub_pc));

	if (nwritten != sizeof(ub->ub_pc))
		err(EXIT_FAILURE, "%s: write", __func__);
}

static const rvp_emitters_t binary = {
	  .init = NULL
	, .emit_nop = reemit_binary_nop
	, .emit_op = reemit_binary_op
	, .dataptr_to_string = NULL
	, .insnptr_to_string = NULL
};

int
main(int argc, char **argv)
{
	int ch, fd;
	const char *inputname;
	output_dir = "trace_dir";
	const char *progname = argv[0];
	rvp_output_params_t op = {
	  .op_type = RVP_OUTPUT_PLAIN_TEXT
	, .op_emit_generation = false
	, .op_emit_bytes = false
	, .op_nrecords = SIZE_MAX
	};
	intmax_t tmpn;
	char *end;

	while ((ch = getopt(argc, argv, "bgn:t:d:")) != -1) {
		switch (ch) {
		case 'n':
			errno = 0;
			tmpn = strtoimax(optarg, &end, 10);
			if (errno != 0) {
				err(EXIT_FAILURE, "could not parse -n %s",
				    optarg);
			}
			if (end == optarg) {
				errx(EXIT_FAILURE, "no numeric characters "
				    "in -n %s", optarg);
			}
			if (*end != '\0') {
				errx(EXIT_FAILURE, "extraneous characters "
				    "after -n %jd", tmpn);
			}
			if (tmpn < 0 || SIZE_MAX < tmpn)
				errx(EXIT_FAILURE, "-n %jd: out range", tmpn);
			op.op_nrecords = tmpn;
			break;
		case 'b':
			op.op_emit_bytes = true;
			break;
		case 'g':
			op.op_emit_generation = true;
			break;
		case 't':
			if (strcmp(optarg, "binary") == 0)
				op.op_type = RVP_OUTPUT_BINARY;
			else if (strcmp(optarg, "plain") == 0)
				op.op_type = RVP_OUTPUT_PLAIN_TEXT;
			else if (strcmp(optarg, "symbol-friendly") == 0)
				op.op_type = RVP_OUTPUT_SYMBOL_FRIENDLY;
			else
				usage(progname);
			break;
		case 'd':
			output_dir = optarg;
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

	const rvp_emitters_t *emitters = &binary;
	size_t most_nrecords = op.op_nrecords;
	// switch (op->op_type) {
	// case RVP_OUTPUT_BINARY:
	// 	emitters = &binary;
	// 	break;
	// case RVP_OUTPUT_SYMBOL_FRIENDLY:
	// 	emitters = &symbol_friendly;
	// 	break;
	// case RVP_OUTPUT_PLAIN_TEXT:
	// 	emitters = &plain_text;
	// 	break;
	// default:
	// 	errx(EXIT_FAILURE, "%s: unknown output type %d", __func__,
	// 	     op->op_type);
	// }

	mkdir(output_dir, 0777);

	max_tid = 0;

	rvp_trace_dump_with_emitters(true, true, most_nrecords, emitters, fd);

	return EXIT_SUCCESS;
}
