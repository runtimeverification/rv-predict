#include <libelf.h>
#include <libdwarf.h>
#include <stdio.h>
#include <stdlib.h>

#include "foo.h"

void
foo(void)
{
	xyz_t xyz = {.x = {3, 4, 5}, .y = {6, 7, 8}, .z = {9, 10, 11}};
	xyz_t array[3][4];
	int iarray[7][5][2] = {{{1}}};

	void *cfa = __builtin_dwarf_cfa(); /* == llvm.eh.dwarf.cfa */

	printf("xyz.x.p, array[1][2].y.q, iarray[0][0][0] = %d, %d, %d, "
	    "&s - cfa = %td\n", xyz.x.p, array[1][2].y.q, iarray[0][0][0],
	    (char *)&xyz - (char *)cfa);
	printf("&xyz = %p\n", (const void *)&xyz);
	printf("&cfa = %p\n", (const void *)&cfa);
	printf("&array[0] = %p\n", (const void *)&array[0]);
	printf("&iarray[0] = %p\n", (const void *)&iarray[0]);
	printf("DWARF Canonical Frame Address (CFA) = %p\n", cfa);
}


