/*
 * test strlen
 */

#include <assert.h>
#include <string.h>

char str1  []  = "A";
char str3  [] = "A23";
char str5  [] = "A23B5";
char str19 [] = "0123456789abcdef123";


int
main(void)
{
	assert(strlen(str1)==1);
	assert(strlen(str3)==3);
	assert(strlen(str5)==5);
	assert(strlen(str19)==19);

	return 0;
}
