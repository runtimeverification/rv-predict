#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <unistd.h>

#define NUM_THREADS 4

long accum = 0;

static void *square(void *param) {
    int x = *(int *)param;
    accum += x * x;
    return NULL;
}

int main() {
    pthread_t threads[NUM_THREADS];
    int *params[NUM_THREADS];

    for (long t = 0; t < NUM_THREADS; t++) {
        params[t] = malloc(sizeof(int));
        *params[t] = t + 1;
        pthread_create(&threads[t], NULL, square, (void *)params[t]);
    }

    for (long t = 0; t < NUM_THREADS; t++) {
        pthread_join(threads[t], NULL);
        free(params[t]);
    }

    printf("%ld\n", accum);
    return 0;
}
