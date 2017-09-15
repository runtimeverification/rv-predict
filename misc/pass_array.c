#include <stdio.h>
#include <stdlib.h>

#include "nbcompat.h"

double
sum(double mat[5][5])
{
	double sum = 0.0;
	int i, j;

	for (i = 0; i < 5; i++) for (j = 0; j < 5; j++) {
		sum += mat[i][j];
	}
	return sum;
}

int
main(int argc __unused, char **argv __unused)
{
	double mat[5][5] = {{1},
			    {0, 1},
			    {0, 0, 1},
			    {0, 0, 0, 1},
			    {0, 0, 0, 0, 1}};

	printf("sum = %g", sum(mat));
	return EXIT_SUCCESS;
}
