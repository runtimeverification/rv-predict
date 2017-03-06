#include <err.h>
#include <stdlib.h>

#include "nbcompat.h"
#include "notimpl.h"

void __dead
not_implemented(const char *fname)
{
	errx(EXIT_FAILURE, "%s: not implemented", fname);
}
