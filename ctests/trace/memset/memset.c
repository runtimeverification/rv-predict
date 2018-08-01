#include <string.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <err.h>
#include "nbcompat.h"


#define sziibgn 8
int iibgn  []={0,1,2,3,4,6,7,8};
//int lngths []={1,2,3,4,5,7,8,9};

int testit(int start, int n2set){
	char *cc = calloc(start+n2set+2, sizeof(char));
	int ii;
	char c = 0x41 + start; /* a,b,c,... */

	memset(&cc[start], c, n2set*sizeof(char));

	/* aa should contain zero or more 0x00 bytes, 'n2set' bytes of 'c', and a byte of zero */
	/* leading zeros */
	for(ii=0;ii<start;ii++){
		if(cc[ii] != 0)
			return EXIT_FAILURE;
	}
	/* Did everything get set correctly */
	for(ii=start;ii<start+n2set;ii++){
		if(cc[ii] != c)
			return EXIT_FAILURE;
	}
	/* Make sure that too many didn't get copied */
	if(cc[start+n2set] != 0)
		return EXIT_FAILURE;

	return EXIT_SUCCESS;
}
int
main(void)
{
	int ii, jj;

	for(ii=0;ii<sziibgn;ii++)
		for(jj=0;jj<sziibgn;jj++){
			if( testit(iibgn[ii],iibgn[jj]+1) == EXIT_FAILURE)
			return EXIT_FAILURE;
		}
	return EXIT_SUCCESS;
}
