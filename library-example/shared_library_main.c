#include <pthread.h>

extern void shared_call( void (*f) () );
void dump_libraries();

int x = 0;

void increment() {
    x++;
}

void* runIncrement(void * ignored) {
    shared_call(increment);
    return NULL;
}

int main() {
    dump_libraries();
	pthread_t thread;
    pthread_create(&thread, NULL, runIncrement, NULL);
    increment();
    pthread_join(thread, NULL);
}
