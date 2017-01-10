#include <err.h>
#include <stdlib.h>

#include "notimpl.h"

void
not_implemented(const char *fname)
{
	errx(EXIT_FAILURE, "%s: not implemented", fname);
}
