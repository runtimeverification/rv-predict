#include <err.h>
#include <fcntl.h>	/* for open(2) */
#include <libelf.h>
#include <libdwarf.h>
#include <stdio.h>
#include <stdlib.h>

#include "foo.h"

static pqr_t pqr = {.p = 0, .q = 1, .r = 2};

static xyz_t xyz = {.x = {3, 4, 5}, .y = {6, 7, 8}, .z = {9, 10, 11}};

int
main(int argc, char **argv)
{
	int fd, rc;
	Dwarf_Debug dbg;
	Dwarf_Error dwerr;
	void *cfa = __builtin_dwarf_cfa();

	fd = open(argv[0], O_RDONLY);

	if (fd == -1)
		err(EXIT_FAILURE, "%s: open(\"%s\")", __func__, argv[0]);

	rc = dwarf_init(fd, DW_DLC_READ, NULL, NULL, &dbg, &dwerr);

	if (rc != DW_DLV_OK) {
		errx(EXIT_FAILURE, "%s: dwarf_elf_init: %s", __func__,
		    dwarf_errmsg(dwerr));
	}
	foo();
	printf("&pqr = %p\n", &pqr);
	printf("&xyz = %p\n", &xyz);
	printf("DWARF Canonical Frame Address (CFA) %p\n", cfa);
	dwarf_finish(dbg, NULL);
	return EXIT_SUCCESS;
}
