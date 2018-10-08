#include <assert.h>	/* for PRIu32 */
#include <inttypes.h>	/* for PRIu32 */
#include <stdatomic.h>	/* for uint8_t, uint32_t */
#include <stdint.h>	/* for uint8_t, uint32_t */
#include <stdio.h>
#include <stdlib.h>
#include "nbcompat.h"


static const uint8_t  initial_u8  = 0x10;
static const uint16_t initial_u16 = 0x1020;
static const uint32_t initial_u32 = 0x10203040;
static const uint64_t initial_u64 = 0x1020304050607080;

static const uint8_t  end_u8  = 0x11;
static const uint16_t end_u16 = 0x1102;
static const uint32_t end_u32 = 0x11020304;
static const uint64_t end_u64 = 0x1102030405060708;

int
main(void)
{
	volatile _Atomic uint8_t  u8  = initial_u8;
	volatile _Atomic uint16_t u16 = initial_u16;
	volatile _Atomic uint32_t u32 = initial_u32;
	volatile _Atomic uint64_t u64 = initial_u64;

	assert(atomic_fetch_add(&u8,  end_u8  - initial_u8)  == initial_u8);
	assert(atomic_fetch_add(&u16, end_u16 - initial_u16) == initial_u16);
	assert(atomic_fetch_add(&u32, end_u32 - initial_u32) == initial_u32);
	assert(atomic_fetch_add(&u64, end_u64 - initial_u64) == initial_u64);

	assert(u8  == end_u8);
	assert(u16 == end_u16);
	assert(u32 == end_u32);
	assert(u64 == end_u64);

	return EXIT_SUCCESS;
}
