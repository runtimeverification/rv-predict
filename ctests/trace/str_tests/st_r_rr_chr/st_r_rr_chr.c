/*
 * test strchar -find 1st occurance of character
 *      strrchr -find last occurance of character
 */

#include <stdlib.h>
#include <assert.h>
#include <string.h>

char str0 [5] ="\00";
char str1  []  = "A";
char str3  [] = "A23";
char str5  [] = "A23B5";
char str19 [] = "0123456789abcdef123";


int
main(void)
{
	int c; /* The character searched for is passed as an integer */
	char* pp;
	
	c='a';
	assert(strchr(str0,c)==NULL && strrchr(str0,c)==NULL);
	pp=&str5[3];
	c='B';
	assert(strchr(str5,c)==pp && strrchr(str5,c)==pp);
	assert(strlen(str3)==3);
	assert(strlen(str5)==5);
	assert(strlen(str19)==19);

	return 0;
}
