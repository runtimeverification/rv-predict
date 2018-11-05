#include <err.h>
#include <limits.h>	/* for SIZE_MAX */
#include <unistd.h>	/* for STDIN_FILENO */
#include <fcntl.h>	/* for open(2) */

extern "C" {
	#include "reader.h"	
}
#include "hb_handlers.h"

extern "C" void hb_perform_nop(const rvp_pstate_t *ps, const rvp_ubuf_t *ub)
{
	return;
}

extern "C" void hb_perform_op(const rvp_pstate_t *ps, const rvp_ubuf_t *ub,
    rvp_op_t op, bool is_load, int field_width)
{
	return;
}


int
main(int argc, char* argv[])
{
	const rvp_emitters_t hb_emitters = {
		  init : NULL
		, emit_nop : NULL
		, emit_op : hb_perform_op
		, dataptr_to_string : NULL
		, insnptr_to_string : NULL
	};

	// 1 create your rvp_emitters_t emitters
	// 2 call rvp_trace_dump_with_emitters(false, SIZE_MAX, &emitters,
	//            input file descriptor);
	int fd;
	const char *inputname;
	if (argc == 1) {
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
	rvp_trace_dump_with_emitters(false, sizeof(rvp_ubuf_t), &hb_emitters, fd);
}
