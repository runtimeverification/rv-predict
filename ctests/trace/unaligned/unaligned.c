#include <inttypes.h>	/* for PRIu32 */
#include <string.h>
#include <stdint.h>	/* for uint8_t, uint32_t */
#include <stdio.h>
#include <stdlib.h>
#include "nbcompat.h"

struct _pair;
typedef struct _pair pair_t;

struct _pair {
	uint8_t misalign;
	uint32_t a;
	uint32_t b;
} __packed;

static const pair_t initial = {.a = 5, .b = 7};

int
main(void)
{
	volatile pair_t p = initial;

	printf("%" PRIu32 " * %" PRIu32 " = %" PRIu32 "\n",
	    p.a, p.b, p.a * p.b);

	return EXIT_SUCCESS;
}
