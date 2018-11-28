#include <err.h>
#include <limits.h>	/* for SIZE_MAX */
#include <stdio.h>	/* for printf(3) */
#include <stdlib.h>	/* for EXIT_FAILURE */
#include <unistd.h>	/* for STDIN_FILENO */
#include <fcntl.h>	/* for open(2) */

#include "reader.h"	
#include "hb_handlers.h"
#include<iostream>

hb_state* state_ptr;

extern "C" void hb_perform_nop(const rvp_pstate_t *ps, const rvp_ubuf_t *ub)
{
	return;
}

extern "C" void hb_perform_op(const rvp_pstate_t *ps, const rvp_ubuf_t *ub,
    rvp_op_t op, bool is_load, int field_width)
{
	bool has_race = hb_handler(state_ptr, ps->ps_curthread, op, ub);
	if(has_race){
		printf("Race occured by %d thread \n", ps->ps_curthread);
	}
}

int
main(int argc, char* argv[])
{
	const rvp_emitters_t hb_emitters = {
		  .init = NULL
		, .emit_nop = hb_perform_nop
		, .emit_op = hb_perform_op
		, .dataptr_to_string = NULL
		, .insnptr_to_string = NULL
	};

	int fd;
	const char *inputname;
	if (argc == 2) {
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

	state_ptr = new hb_state();

	rvp_trace_dump_with_emitters(false, false, sizeof(rvp_ubuf_t), &hb_emitters, fd);
}
