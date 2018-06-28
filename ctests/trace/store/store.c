#include <assert.h>	/* for PRIu32 */
#include <inttypes.h>	/* for PRIu32 */
#include <stdatomic.h>	/* for uint8_t, uint32_t */
#include <stdint.h>	/* for uint8_t, uint32_t */
#include <stdio.h>
#include <stdlib.h>

#include "nbcompat.h"

static const uint8_t initial_u8 = 0x39;
static const uint16_t initial_u16 = 0x40;
static const uint32_t initial_u32 = 0x41;
static const uint64_t initial_u64 = 0x43;

static const uint8_t end_u8 = 0x10;
static const uint16_t end_u16 = 0x1000;
static const uint32_t end_u32 = 0x100;
static const uint64_t end_u64 = (uint64_t)0xf << 32;

int
main(void)
{
	volatile _Atomic uint8_t u8 __unused = initial_u8;
	volatile _Atomic uint16_t u16 __unused = initial_u16;
	volatile _Atomic uint32_t u32 __unused = initial_u32;
	volatile _Atomic uint64_t u64 __unused = initial_u64;

	u8 = end_u8;
	/* In `expect.out`, the next store is ascribed to the previous line
	 * instead of its own line.  Clang bug?
	 */
	u16 = end_u16;
	u32 = end_u32;
	u64 = end_u64;

	return EXIT_SUCCESS;
}
