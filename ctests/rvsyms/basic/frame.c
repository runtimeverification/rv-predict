#include <sys/cdefs.h>

#include "frame.h"

const void *
program_counter(void)
{
	return __builtin_return_address(0);
}

static void
innerinnerframe(int *xp)
{
	(void)*xp;
}

void
innerframe(const void **pc, const void **cfa)
{
	int x = 0;

	innerinnerframe(&x);
	*pc = program_counter();
	*cfa = __builtin_dwarf_cfa();
	innerinnerframe(&x);
}

bool
bracketed(const void *cfa1, const void *cfa2, const void *addr0)
{
	const char *addr = addr0, *lower, *upper;

	if ((const char *)cfa1 < (const char *)cfa2) {
		lower = cfa1;
		upper = cfa2;
	} else {
		lower = cfa2;
		upper = cfa1;
	}

	return lower < addr && addr < upper;
}
