#include <string.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include "nbcompat.h"



char gstr [9];

int
main(void)
{
	char lstr[5];

        /* set global to 1 */
	memset(&gstr,1,9*sizeof(char));
	
   	/* set local to 3 */
	memset(&lstr,3,5*sizeof(char));

	if ((gstr [7] + lstr[4]) != 4)
	{
		return EXIT_FAILURE;
	}
	return EXIT_SUCCESS;
}
