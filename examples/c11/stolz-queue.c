#include <err.h>	/* errx(3) */
#include <signal.h>	/* sigaction(2) */
#include <stdatomic.h>
#include <stdbool.h>	/* false */
#include <pthread.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>	/* EXIT_FAILURE */
#include <string.h>	/* memset */
#include <time.h>

#include "nbcompat.h"

volatile _Atomic bool finished = ATOMIC_VAR_INIT(false);

typedef uint32_t u32;

static inline void
report(const char *fmt, ...)
{
	va_list ap;
	va_start(ap, fmt);
	(void)vprintf(fmt, ap);
	va_end(ap);
}

// Shared data structure
#define BUFFER_SIZE	10

typedef struct ringbuffer {
    char buffer[BUFFER_SIZE];
    _Atomic u32 write_idx;
    _Atomic u32 read_idx;
} ringbuffer;

u32 buffer_next(u32 idx) {
    //return (idx+1) % BUFFER_SIZE;
    if (idx < BUFFER_SIZE - 1){
        return idx+1;
    } else {
        return 0;
    }
}

volatile ringbuffer buffer = {
    .write_idx = ATOMIC_VAR_INIT(0),
    .read_idx = ATOMIC_VAR_INIT(0)
};


void* prvProducerTask( void *pvParameters );
void* prvConsumerTask1( void *pvParameters );
void* prvConsumerTask2( void *pvParameters );

#define TIMESPEC(ns) {.tv_sec = ns/1000000UL, .tv_nsec = ns%1000000UL}

//const struct timespec xOneSecond = TIMESPEC( 1000UL );
//const struct timespec xOneHalfSecond = TIMESPEC( 1500UL );
//const struct timespec xHalfSecond = TIMESPEC( 500UL );
// accelerate scenario
const struct timespec xOneSecond = TIMESPEC( 100UL );
const struct timespec xOneHalfSecond = TIMESPEC( 150UL );
const struct timespec xHalfSecond = TIMESPEC( 50UL );

static void
int_handler(int signum __unused)
{
	atomic_store(&finished, true);
}

int main( void )
{
    pthread_t producerHandle;
    pthread_t consumer11Handle;
    pthread_t consumer12Handle;
    pthread_t consumer21Handle;
    pthread_t consumer22Handle;
    /* The function that implements the task. */
    pthread_create( &producerHandle, NULL, prvProducerTask, NULL );
    pthread_create( &consumer11Handle, NULL, prvConsumerTask1, (void*) 1 );
    pthread_create( &consumer12Handle, NULL, prvConsumerTask1, (void*) 2 );
    pthread_create( &consumer21Handle, NULL, prvConsumerTask2, (void*) 1 );
    pthread_create( &consumer22Handle, NULL, prvConsumerTask2, (void*) 2 );

   pthread_join(producerHandle, NULL);
   pthread_join(consumer11Handle, NULL);
   pthread_join(consumer12Handle, NULL);
   pthread_join(consumer22Handle, NULL);
   pthread_join(consumer21Handle, NULL);
}

char data = 'a';
char accquire_data() {
    nanosleep( &xOneSecond, NULL );
    if (data > 'z' ) data = 'a';
    return data++;
}


void* prvProducerTask( void *pvParameters )
{
    (void)pvParameters;
    int i;
    u32 write_idx = 0;
    u32 read_idx = 0;
    
    report("Producer startet.\n");
    
    for (i = 0; i < 100 && !finished; i++) {
        write_idx = atomic_load(&buffer.write_idx);
        read_idx = atomic_load(&buffer.read_idx);
        
        if ( buffer_next(write_idx) != read_idx ) {
            buffer.buffer[write_idx] = accquire_data();
            atomic_store(&buffer.write_idx, buffer_next(write_idx));
            report("Producer: Value %c written to buffer[%d].\n", buffer.buffer[write_idx], write_idx);
        }
    }
    atomic_store(&finished, true);
    return NULL;
}

void* prvConsumerTask1( void *pvParameters )
{
    u32 id = (u32) pvParameters;
    char data = '#';
    u32 local_read_idx = 0;
    u32 next_read_idx = 0;

    report("Consumer %d: started.\n", id);

    for(;;) {
        local_read_idx = atomic_load(&buffer.read_idx);
        if (local_read_idx != atomic_load(&buffer.write_idx)) {
            data = buffer.buffer[local_read_idx];
            next_read_idx = buffer_next(local_read_idx);
            
            u32 expected = local_read_idx;
            _Bool test = atomic_compare_exchange_weak(&buffer.read_idx, &expected, next_read_idx);
            if (test) {
                /* Delay for some time, depending on data. */
                if (data<'u') nanosleep ( &xOneHalfSecond, NULL );
                if (data>'f') nanosleep ( &xOneSecond, NULL );
                report("Consumer 1-%d: processed data %c from buffer[%d]\n", id, data, local_read_idx);
            }
        } else if (finished)
		break;
    }
    return NULL;
}

void* prvConsumerTask2( void *pvParameters )
{
    u32 id = (u32) pvParameters;
    char data = '#';
    u32 local_read_idx = 0;
    u32 next_read_idx = 0;

    report("Consumer %d: started.\n", id);

    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    if (sigemptyset(&sa.sa_mask) == -1)
        errx(EXIT_FAILURE, "%s: sigemptyset", __func__);
    sa.sa_handler = int_handler;
    if (sigaction(SIGINT, &sa, NULL) == -1)
        errx(EXIT_FAILURE, "%s: sigaction", __func__);

    for(;;) {
        local_read_idx = atomic_load(&buffer.read_idx);
        if (local_read_idx != atomic_load(&buffer.write_idx)) {
            data = buffer.buffer[local_read_idx];
            next_read_idx = buffer_next(local_read_idx);
            
            u32 expected = local_read_idx;
            _Bool test = atomic_compare_exchange_weak(&buffer.read_idx, &expected, next_read_idx);
            if (test) {
                /* Delay for some time, depending on data. */
                if (data<'n') nanosleep ( &xOneSecond, NULL );
                if (data>'i') nanosleep ( &xOneHalfSecond, NULL );
                report("Consumer 2-%d: processed data %c from buffer[%d]\n", id, data, local_read_idx);
            }
        } else if (finished)
		break;
    }
    return NULL;
}
