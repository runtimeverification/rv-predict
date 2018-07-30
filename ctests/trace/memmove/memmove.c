#include <string.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include "nbcompat.h"


#define sz  30

char gstr [sz];

int
main(void)
{

        /* set global to 1 */
	memset(&gstr,   1,3*sizeof(char));
	memset(&gstr[2],2,(sz-3)*sizeof(char));
	
   	/* set local to 3 */
	memmove(&gstr[2],&gstr[1],(sz / 2)*sizeof(char));

	if ((gstr [4] + gstr[5]) != 4)
	{
		return EXIT_FAILURE;
	}
	return EXIT_SUCCESS;
}
