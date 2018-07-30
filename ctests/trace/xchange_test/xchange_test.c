/*
 * xchange_test: Test atomic exchange of 32 and 64 bit words. 
 * Also _sync_val_compare_and_swap
 */
#include <assert.h>	/* for PRIu32 */
#include <inttypes.h>	/* for PRIu32 */
#include <stdatomic.h>	/* for uint8_t, uint32_t */
#include <stdint.h>	/* for uint8_t, uint32_t */
#include <stdio.h>
#include <stdlib.h>
#include "nbcompat.h"

volatile _Atomic uint8_t  initial_u8 = 0x39;
volatile _Atomic uint16_t initial_u16 = 0x40;
volatile _Atomic uint32_t initial_u32 = 0x41;
volatile _Atomic uint64_t initial_u64 = 0x43;

uint16_t u_glbl_u16    = 0x16;
uint16_t u_glbl_u16_ll = 0x1e;
uint16_t u_glbl_u16_mm = 0x1f;

volatile _Atomic uint32_t u_glbl_u32    = 0x32;
volatile _Atomic uint64_t u_glbl_u64    = 0x64;

int
main(void)
{
	uint8_t  u8  = initial_u8; 
	uint16_t u16 = initial_u16;
	uint32_t u32 = initial_u32;
	uint64_t u64 = initial_u64;

	uint8_t  lcl_u8 __unused;
	uint16_t lcl_u16 __unused;
	uint32_t lcl_u32 __unused;
	uint64_t lcl_u64 __unused;

	lcl_u8  = u8;
	lcl_u16 = u16;
	lcl_u32 = u32;
	lcl_u64 = u64;

	u32 = __sync_val_compare_and_swap(&lcl_u32, 1, 2 );

	u32 = atomic_exchange(&u_glbl_u32, 0x32);
	u64 = atomic_exchange(&u_glbl_u64, 0x64);

	return EXIT_SUCCESS;
}
