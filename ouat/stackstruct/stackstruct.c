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
	int fd;
	Dwarf_Debug dbg;
	Dwarf_Error dwerr;
	void *frmaddr = __builtin_frame_address(0);

	setprogname(argv[0]);

	fd = open(argv[0], O_RDONLY);

	if (fd == -1)
		err(EXIT_FAILURE, "%s: open(\"%s\")", __func__, argv[0]);

/*
/	Elf *elf = elf_begin(fd, ELF_C_READ, NULL);
	if (elf == NULL) {
		errx(EXIT_FAILURE, "%s: elf_begin: %s", __func__,
		    elf_errmsg(elf_errno()));
	}
*/
	if (dwarf_init(fd, DW_DLC_READ, NULL, NULL, &dbg, &dwerr) != DW_DLV_OK) {
		errx(EXIT_FAILURE, "%s: dwarf_elf_init: %s", __func__,
		    dwarf_errmsg(dwerr));
	}
	foo();
	printf("frame address %p\n", frmaddr);
	dwarf_finish(dbg, NULL);
	return EXIT_SUCCESS;
}
