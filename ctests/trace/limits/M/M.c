#include <stdlib.h>

#include "nbcompat.h"

int
main(void)
{
	volatile int x __unused;
	int i;

	for (i = 0; i < 1000000; i++)
		x = i;

	return EXIT_SUCCESS;
}
