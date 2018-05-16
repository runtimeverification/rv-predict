
#include <assert.h>	/* for PRIu32 */
#include <inttypes.h>	/* for PRIu32 */
#include <stdatomic.h>	/* for uint8_t, uint32_t */
#include <stdint.h>	/* for uint8_t, uint32_t */
#include <stdio.h>
#include <stdlib.h>
#include "nbcompat.h"

static const uint8_t  initial_u8 = 0x39;
static const uint16_t initial_u16 = 0x40;
static const uint32_t initial_u32 = 0x41;
static const uint64_t initial_u64 = 0x43;

static const uint8_t  end_u8 = 0x10;
static const uint16_t end_u16 = 0x1000;
static const uint32_t end_u32 = 0x100;
static const uint64_t end_u64 = (uint64_t)0xf << 32;

_Atomic uint8_t  glbl_u8  = initial_u8; 
_Atomic uint16_t glbl_u16 = initial_u16;
_Atomic uint32_t glbl_u32 = initial_u32;
_Atomic uint64_t glbl_u64 = initial_u64;

uint8_t  glbl_u_u8;
uint16_t glbl_u_u16;
uint32_t glbl_u_u32;
uint64_t glbl_u_u64;


int
main(void)
{

	uint8_t  u8  = initial_u8; 
	uint16_t u16 = initial_u16;
	uint32_t u32 = initial_u32;
	uint64_t u64 = initial_u64;

	uint8_t  lcl_u8;
	uint16_t lcl_u16;
	uint32_t lcl_u32;
	uint64_t lcl_u64;

	lcl_u8  = u8;
	lcl_u16 = u16;
	lcl_u32 = u32;
	lcl_u64 = u64;


	glbl_u8  = 0xfe;
	glbl_u16 = u16;
	glbl_u32 = u32;
	glbl_u64 = u64;


	glbl_u_u8  = end_u8;
	glbl_u_u16 = end_u16;
	glbl_u_u32 = end_u32;
	glbl_u_u64 = end_u64;


	return EXIT_SUCCESS;
}
