#include <libelf.h>
#include <libdwarf.h>
#include <stdio.h>
#include <stdlib.h>

#include "foo.h"

void
foo(void)
{
	xyz_t xyz = {.x = {3, 4, 5}, .y = {6, 7, 8}, .z = {9, 10, 11}};

	void *cfa = __builtin_dwarf_cfa(); /* == llvm.eh.dwarf.cfa */

	printf("xyz.x.p, xyz.y.q = %d, %d, &s - cfa = %td\n", xyz.x.p, xyz.y.q,
	    (char *)&xyz - (char *)cfa);
	printf("&xyz = %p\n", &xyz);
	printf("DWARF Canonical Frame Address (CFA) = %p\n", cfa);
}


