#include <err.h>
#include <fcntl.h>	/* for open(2) */
#include <libelf.h>
#include <stdio.h>
#include <stdlib.h>

#include "nbcompat.h"
#include "foo.h"
#include "frame.h"

static pqr_t pqr = {.p = 0, .q = 1, .r = 2};

static xyz_t xyz = {.x = {3, 4, 5}, .y = {6, 7, 8}, .z = {9, 10, 11}};

int
main(int argc __unused, char **argv __unused)
{
	int rc;
	void *cfa = __builtin_dwarf_cfa();

	foo();
	printf("[%p]\n", (const void *)&pqr);
	printf("pqr.p at %s\n", __FILE__);
	printf("[%p]\n", (const void *)&xyz);
	printf("xyz.x.p at %s\n", __FILE__);
#if 0
	printf("DWARF Canonical Frame Address (CFA) %p\n", cfa);
#endif
	return EXIT_SUCCESS;
}
