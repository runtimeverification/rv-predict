#include <libelf.h>
#include <libdwarf.h>
#include <stdio.h>
#include <stdlib.h>

#include "foo.h"

void
foo(void)
{
	struct {
		int b;
		int a;
	} s = {0, 0};

	void *cfa = __builtin_dwarf_cfa(); /* == llvm.eh.dwarf.cfa */

	printf("s.a, s.b = %d, %d, &s - cfa = %td\n", s.a, s.b,
	    (char *)&s - (char *)cfa);
	printf("&s = %p\n", &s);
	printf("DWARF Canonical Frame Address (CFA) = %p\n", cfa);
}


