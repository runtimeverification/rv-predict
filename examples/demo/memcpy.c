#include <pthread.h>
#include <stdio.h>
#include <string.h>	/* for memcpy */

int x;

void *
assignfn(void *arg)
{
	x = 5;
	return NULL;
}

void *
copyfn(void *arg)
{
	int y = 10;
	memcpy(&x, &y, sizeof(x));
	return NULL;
}

int
main(int argc, char *argv[])
{
	pthread_t assign, copy;

	pthread_create(&assign, NULL, assignfn, NULL);
	pthread_create(&copy, NULL, copyfn, NULL);
         
	pthread_join(assign, NULL);
	pthread_join(copy, NULL);

	printf("x = %d\n", x);
	return 0;
}   
