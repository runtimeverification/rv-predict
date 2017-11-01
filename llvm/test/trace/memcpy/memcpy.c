#include <string.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include "nbcompat.h"

typedef char payload_t[8 + 4 + 2 + 1];

static struct {
	payload_t src __aligned(sizeof(uint64_t));
	payload_t dst __aligned(sizeof(uint64_t));
} disjoint = {.src = "0123456789abcd"};

static union {
	payload_t src;
	struct {
		uint64_t pad;
		payload_t dst;
	};
} lap1 = {.src = "0123456789abcd"};

static union {
	payload_t dst;
	struct {
		uint64_t pad;
		payload_t src;
	};
} lap2 = {.src = "0123456789abcd"};

int
main(void)
{
	/* expect forwards copy */
	memcpy(&disjoint.dst, &disjoint.src, sizeof(disjoint.dst));
	printf("dst = %s\n", disjoint.dst);

	/* expect backwards copy */
	memcpy(&lap1.dst, &lap1.src, sizeof(lap1.dst));
	printf("lap1 dst = %s\n", lap1.dst);

	/* expect forwards copy */
	memcpy(&lap2.dst, &lap2.src, sizeof(lap2.dst));
	printf("lap2 dst = %s\n", lap2.dst);
	return EXIT_SUCCESS;
}
