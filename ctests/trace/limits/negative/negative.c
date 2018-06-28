#include <stdlib.h>

#include "nbcompat.h"

int
main(void)
{
	volatile int x __unused;

	x = 999;

	return EXIT_SUCCESS;
}
