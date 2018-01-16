#include <err.h>	/* for err(3) */
#include <pthread.h>	/* for pthread_create(3) */
#include <signal.h>	/* for sigaction(2), sigemptyset(3), etc. */
#include <stdatomic.h>	/* for _Atomic */
#include <stdbool.h>	/* for bool */
#include <stdint.h>	/* for uint.._t */
#include <stdlib.h>	/* for EXIT_FAILURE */
#include <string.h>	/* for memset(3) */
#include <unistd.h>	/* for write(2) */

#include "nbcompat.h"

struct _txdesc;
typedef struct _txdesc txdesc_t;

struct _txdesc {
	txdesc_t *txd_next;
	const void *txd_buf;
	volatile uint16_t txd_flags_length;
#define	TXD_FLAG_NIC_OWNS	__BIT(15)
#define	TXD_LENGTH_MASK		__BITS(14, 0)
};

/* Device registers: */
static struct {
	txdesc_t * volatile _Atomic txhead;
	volatile _Atomic uint32_t cmdsts;
#define	CMDSTS_CMD_RUN		__BIT(0)
#define	CMDSTS_STS_RUNNING	__BIT(16)
	volatile _Atomic uint32_t intr;
#define	INTR_TX_FINISHED		__BIT(0)
} registers;

/* Tx descriptors (txdesc_t) are objects in RAM shared by driver and device */
txdesc_t ring[8]; 

volatile _Atomic bool txbusy = false;
pthread_t driver_thd, device_thd;

static void
ring_init(void)
{
	int i;
	const int last = __arraycount(ring) - 1;

	memset(ring, 0, sizeof(ring));

	for (i = 0; i < last; i++) {
		ring[i].txd_next = &ring[i + 1];
	}
	ring[last].txd_next = &ring[0];
	registers.txhead = &ring[0];
}

static void
isr(int signum __unused)
{
	if ((registers.intr & INTR_TX_FINISHED) != 0) {
		registers.intr &= ~INTR_TX_FINISHED;
		txbusy = false;
	}
}

static void
intr_init(void)
{
	struct sigaction sa;
	sigset_t mask;

	if (sigemptyset(&mask) == -1)
		err(EXIT_FAILURE, "%s: sigemptyset", __func__);

	sa = (struct sigaction){.sa_handler = &isr, .sa_mask = mask};

	if (sigaction(SIGUSR1, &sa, NULL) == -1)
		err(EXIT_FAILURE, "%s: sigaction", __func__);
}

static void
ring_start(void)
{
	registers.intr = 0;
	registers.cmdsts |= CMDSTS_CMD_RUN;
}

static void *
driver(void *arg __unused)
{
	volatile uint16_t *flagsp;
	int i, tail;
	const char message[] = "Hello, world!\n";
	sigset_t blkset, oldset;
	txdesc_t *txd;

	ring_init();
	intr_init();

	ring_start();

	sigemptyset(&blkset);	// TBD check error
	sigaddset(&blkset, SIGUSR1);	// TBD check error

	for (i = tail = 0;; i++, tail++) {
		if (tail == __arraycount(ring))
			tail = 0;

		if (message[i] == '\0')
			i = 0;

		txd = &ring[tail];

		flagsp = &txd->txd_flags_length;
		while ((*flagsp & TXD_FLAG_NIC_OWNS) != 0) {
			pthread_sigmask(SIG_BLOCK, &blkset, &oldset);
			if ((*flagsp & TXD_FLAG_NIC_OWNS) != 0) {
				txbusy = true;
				while (txbusy)
					sigsuspend(&oldset); // TBD check error
			}
			pthread_sigmask(SIG_SETMASK, &oldset, NULL);
		}
		txd->txd_buf = &message[i];
		txd->txd_flags_length = __SHIFTIN(1, TXD_LENGTH_MASK);
		txd->txd_flags_length |= TXD_FLAG_NIC_OWNS;
	}
	return NULL;
}

static void *
device(void *arg __unused)
{
	while ((registers.cmdsts & CMDSTS_CMD_RUN) == 0)
		;	// TBD backoff

	registers.cmdsts |= CMDSTS_STS_RUNNING;

	while ((registers.cmdsts & CMDSTS_CMD_RUN) != 0) {
		txdesc_t *txd = registers.txhead;

		if ((txd->txd_flags_length & TXD_FLAG_NIC_OWNS) == 0)
			continue;	// TBD backoff

		// TBD check error
		write(STDOUT_FILENO, txd->txd_buf,
		    __SHIFTOUT(txd->txd_flags_length, TXD_LENGTH_MASK));

		txd->txd_flags_length &= ~TXD_FLAG_NIC_OWNS;
		registers.txhead = txd->txd_next;
		registers.intr |= INTR_TX_FINISHED;
		pthread_kill(driver_thd, SIGUSR1);
	}

	registers.cmdsts &= ~CMDSTS_STS_RUNNING;
	return NULL;
}

int
main(void)
{
	int rc;

	if ((rc = pthread_create(&driver_thd, NULL, driver, NULL)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_create: %s", __func__,
		    strerror(rc));
	}
	if ((rc = pthread_create(&device_thd, NULL, device, NULL)) != 0) {
		errx(EXIT_FAILURE, "%s: pthread_create: %s", __func__,
		    strerror(rc));
	}
	pthread_join(device_thd, NULL);
	pthread_join(driver_thd, NULL);
	return EXIT_SUCCESS;
}
