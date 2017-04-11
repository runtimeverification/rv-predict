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

	void *cfa = __builtin_dwarf_cfa(); /* == llvm.eh.dwarf.cfa */

	printf("xyz.x.p, array[1][2].y.q = %d, %d, &s - cfa = %td\n", xyz.x.p, array[1][2].y.q,
	    (char *)&xyz - (char *)cfa);
	printf("&xyz = %p\n", &xyz);
	printf("&cfa = %p\n", &cfa);
	printf("&array[0] = %p\n", &array[0]);
	printf("DWARF Canonical Frame Address (CFA) = %p\n", cfa);
}


