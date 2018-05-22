/*
 * xchange_816: Test atomic exchange of 8 and 16 bit words
 */
#include <assert.h>	/* for PRIu32 */
#include <inttypes.h>	/* for PRIu32 */
#include <stdatomic.h>	/* for uint8_t, uint32_t */
#include <stdint.h>	/* for uint8_t, uint32_t */
#include <stdio.h>
#include <stdlib.h>
#include "nbcompat.h"
/*
 * Perform atomic exchange to both global and local variables
 */

volatile _Atomic uint8_t  initial_u8 = 0x33;
volatile _Atomic uint16_t initial_u16 = 0x7032;
volatile _Atomic uint32_t initial_u32 = 0x70800034;
volatile _Atomic uint64_t initial_u64 = 0x70080001fffe0038;

volatile _Atomic uint8_t  u_glbl_u8     = 0x08;
volatile _Atomic uint16_t u_glbl_u16    = 0x10 ;
volatile _Atomic uint32_t u_glbl_u32    = 0x20 ;
volatile _Atomic uint64_t u_glbl_u64    = 0x40 ;

int
main(void)
{
	volatile _Atomic uint8_t   u_lcl_u8  = initial_u8;
	volatile _Atomic uint16_t  u_lcl_u16 = initial_u16;
	volatile _Atomic uint32_t  u_lcl_u32 = initial_u32;
	volatile _Atomic uint64_t  u_lcl_u64 = initial_u64;

	 atomic_exchange(&u_glbl_u64, 0x10080001eeef0064);
	 atomic_exchange(&u_glbl_u32, 0x10400032 );
	 atomic_exchange(&u_glbl_u16, 0x1006); 
	 atomic_exchange(&u_glbl_u8 , 0x10); 

	 atomic_exchange(&u_lcl_u64, 0x50080001eeef0064);
	 atomic_exchange(&u_lcl_u32, 0x50400032 );
	 atomic_exchange(&u_lcl_u16, 0x5006); 
	 atomic_exchange(&u_lcl_u8 , 0x50); 

	/* Now do some normal assignments to ensures that nothing is messed up */
	u_lcl_u64  = u_glbl_u64+1;
	u_lcl_u32  = u_glbl_u32+1;
	u_lcl_u16  = u_glbl_u16+1; 
	u_lcl_u8   = u_glbl_u8 +1;

	return EXIT_SUCCESS;
}
