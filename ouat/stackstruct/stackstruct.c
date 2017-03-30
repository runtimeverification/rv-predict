#include <err.h>
#include <fcntl.h>	/* for open(2) */
#include <libelf.h>
#include <libdwarf.h>
#include <stdio.h>
#include <stdlib.h>

#include "foo.h"

int
main(int argc, char **argv)
{
	int fd, rc;
	Dwarf_Debug dbg;
	Dwarf_Error dwerr;
	void *frmaddr = __builtin_frame_address(0);

	fd = open(argv[0], O_RDONLY);

	if (fd == -1)
		err(EXIT_FAILURE, "%s: open(\"%s\")", __func__, argv[0]);

	rc = dwarf_init(fd, DW_DLC_READ, NULL, NULL, &dbg, &dwerr);

	if (rc != DW_DLV_OK) {
		errx(EXIT_FAILURE, "%s: dwarf_elf_init: %s", __func__,
		    dwarf_errmsg(dwerr));
	}
	foo();
	printf("frame address %p\n", frmaddr);
	dwarf_finish(dbg, NULL);
	return EXIT_SUCCESS;
}
