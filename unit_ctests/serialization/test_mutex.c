#include <pthread.h>
#include "nbcompat.h"

int
real_pthread_mutex_lock(pthread_mutex_t *m __unused)
{
	return 0;
}

int
real_pthread_mutex_unlock(pthread_mutex_t *m __unused)
{
	return 0;
}
