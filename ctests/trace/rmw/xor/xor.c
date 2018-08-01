#include <assert.h>	/* for PRIu32 */
#include <inttypes.h>	/* for PRIu32 */
#include <stdatomic.h>	/* for uint8_t, uint32_t */
#include <stdint.h>	/* for uint8_t, uint32_t */
#include <stdio.h>
#include <stdlib.h>
#include "nbcompat.h"

static const uint16_t initial_u16 = 0x40;
static const uint32_t initial_u32 = 0x41;
static const uint64_t initial_u64 = 0x43;

static const uint16_t end_u16 = 0x1000;
static const uint32_t end_u32 = 0x100;
static const uint64_t end_u64 = (uint64_t)0xf << 32;

int
main(void)
{
	volatile _Atomic uint16_t u16 = initial_u16;
	volatile _Atomic uint32_t u32 = initial_u32;
	volatile _Atomic uint64_t u64 = initial_u64;

	assert(atomic_fetch_xor(&u16, end_u16 ^ initial_u16) == initial_u16);
	assert(atomic_fetch_xor(&u32, end_u32 ^ initial_u32) == initial_u32);
	assert(atomic_fetch_xor(&u64, end_u64 ^ initial_u64) == initial_u64);
	assert(u16 == end_u16);
	assert(u32 == end_u32);
	assert(u64 == end_u64);

	return EXIT_SUCCESS;
}