#include <err.h>
#include <inttypes.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>	/* for EXIT_FAILURE */
#include <unistd.h>	/* for STDIN_FILENO */

#include "legacy.h"

#define TYPE_TO_NAME(__op)	[__op] = {.name = #__op}

struct {
	legacy_op_t op; 
	const char *name;
} opnames[256] = {
	  TYPE_TO_NAME(READ)
	, TYPE_TO_NAME(WRITE)
	, TYPE_TO_NAME(ATOMIC_READ)
	, TYPE_TO_NAME(ATOMIC_WRITE)
	, TYPE_TO_NAME(ATOMIC_READ_THEN_WRITE)
	, TYPE_TO_NAME(WRITE_LOCK)
	, TYPE_TO_NAME(WRITE_UNLOCK)
	, TYPE_TO_NAME(READ_LOCK)
	, TYPE_TO_NAME(READ_UNLOCK)
	, TYPE_TO_NAME(WAIT_REL)
	, TYPE_TO_NAME(WAIT_ACQ)
	, TYPE_TO_NAME(START)
	, TYPE_TO_NAME(JOIN)
	, TYPE_TO_NAME(CLINIT_ENTER)
	, TYPE_TO_NAME(CLINIT_EXIT)
	, TYPE_TO_NAME(INVOKE_METHOD)
	, TYPE_TO_NAME(FINISH_METHOD)
	, TYPE_TO_NAME(PRE_LOCK)
	, TYPE_TO_NAME(FORK)
};

static const char *
op_to_name(int op)
{
	if (op < 0 || (int)__arraycount(opnames) <= op ||
	    opnames[op].name == NULL)
		return "<unknown>";
	return opnames[op].name;
}

int
main(int argc, char **argv)
{
	legacy_event_t ev[1024];
	ssize_t i, nevs, nread;

	while ((nread = read(STDIN_FILENO, &ev[0], sizeof(ev))) != -1) {
		if (nread % sizeof(ev[0]) != 0)
			errx(EXIT_FAILURE, "%s: short read", __func__);
		if (nread == 0)
			return EXIT_SUCCESS;
		nevs = nread / sizeof(ev[0]);
		for (i = 0; i < nevs; i++) {
			legacy_event_t *e = &ev[i];
			printf("tid %" PRIu64 " gid 0x%016" PRIx64 " stmtid %" PRIu32 " addr %" PRIx64 " value %" PRIx64 " type %s\n", e->tid, e->gid, e->stmtid, e->addr, e->value, op_to_name(e->type));
		}
	}
	err(EXIT_FAILURE, "%s: read", __func__);
}
