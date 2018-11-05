/*
 * xchange_test: Test atomic exchange of 32 and 64 bit words. 
 */
#include <assert.h>	/* for PRIu32 */
#include <inttypes.h>	/* for PRIu32 */
#include <stdatomic.h>	
#include <stdbool.h>	
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include "nbcompat.h"

uint8_t  initial_u8 =  0x41;
uint16_t initial_u16 = 0x42;
uint32_t initial_u32 = 0x44;
uint64_t initial_u64 = 0x48;

/* I should probably switch to atomic_ and not use 'volatile _Atomic' */
volatile _Atomic uint8_t  u_glbl_u8     = 0x41;
volatile _Atomic uint16_t u_glbl_u16    = 0x42;
volatile _Atomic uint32_t u_glbl_u32    = 0x44;
volatile _Atomic uint64_t u_glbl_u64    = 0x48;

int
main(void)
{
	bool pass8, pass16, pass32, pass64;
	uint8_t u8;
	uint16_t u16;
	uint32_t u32;
	uint64_t u64;
	volatile _Atomic uint8_t lcl_u8;
	volatile _Atomic uint16_t lcl_u16;
	volatile _Atomic uint32_t lcl_u32;
	volatile _Atomic uint64_t lcl_u64;



	lcl_u8  = initial_u8;
	lcl_u16 = initial_u16;
	lcl_u32 = initial_u32;
	lcl_u64 = initial_u64;

	pass8  = atomic_compare_exchange_strong(&lcl_u8, &initial_u8, 0x11 ); 
	pass16 = atomic_compare_exchange_strong(&lcl_u16, &initial_u16, 0x12 );
	pass32 = atomic_compare_exchange_strong(&lcl_u32, &initial_u32, 0x14 );
	pass64 = atomic_compare_exchange_strong(&lcl_u64, &initial_u64, 0x18 );
	assert(pass8 && pass16 && pass32 && pass64);
	assert(lcl_u8 + lcl_u16 + lcl_u32 + lcl_u64 == 4 * 0x10 + 1 + 2 + 4 + 8);

	u8  = atomic_exchange(&u_glbl_u8,  0x31);
	u16 = atomic_exchange(&u_glbl_u16, 0x32);
	u32 = atomic_exchange(&u_glbl_u32, 0x34);
	u64 = atomic_exchange(&u_glbl_u64, 0x38);

	assert(u8 + u16 + u32 + u64 == 4 * 0x40 + 1 + 2 + 4 + 8);
	assert(u_glbl_u8 + u_glbl_u16 + u_glbl_u32 + u_glbl_u64 == 4 * 0x30 + 1 + 2 + 4 + 8);


	return EXIT_SUCCESS;
}
