#include <stdlib.h>

int
main(void)
{
	volatile int x;
	int i;

	for (i = 0; i < 1000000; i++)
		x = i;

	return EXIT_SUCCESS;
}
