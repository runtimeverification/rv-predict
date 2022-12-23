#include <stdatomic.h>
#include <stdint.h>	/* for uint8_t, uint32_t */
#include <stdlib.h>	/* for EXIT_SUCCESS */

#include "nbcompat.h"

int
main(void)
{
	volatile uint8_t x = 5, y = 7;

	x = 9;
	atomic_signal_fence(memory_order_seq_cst);
	y = 11;

	return EXIT_SUCCESS;
}
