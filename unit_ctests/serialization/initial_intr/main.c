#include <stdio.h>
#include <stdlib.h>
#ifdef __NetBSD__
#include <sys/endian.h>	/* for htobe32() */
#else
#include <endian.h>	/* for htobe32() */
#endif

#include "ring.h"
#include "trace.h"

const uint32_t tid = 0xdeadbeef;
const uint32_t idepth = 7;
uint32_t items[5];
uint32_t intr_items[5];

rvp_ring_stats_t rs = {
	  .rs_ring_sleeps = 0
	, .rs_ring_spins = 0
	, .rs_iring_spins = 0
}, intr_rs = {
	  .rs_ring_sleeps = 0
	, .rs_ring_spins = 0
	, .rs_iring_spins = 0
};

int
main(void)
{
	rvp_ring_t intr = {
		  .r_last = &intr_items[__arraycount(intr_items) - 1]
		, .r_items = intr_items
		, .r_consumer = &intr_items[__arraycount(intr_items) - 3]
		, .r_producer = &intr_items[__arraycount(intr_items) - 2]
		, .r_lastpc = NULL	// expected to be unused
		, .r_lgen = 0		// expected to be unused
		, .r_next = NULL	// expected to be unused
		// clean: no sequence is using this ring, and it has no
		// items that need to be serialized
		//
		// dirty: no sequence is using this ring, however, it
		// contains items that need to be serialized
		//
		// in-use: a sequence is logging on this ring; it
		// may or may not contains items that need to be serialized
		, .r_state = RVP_RING_S_DIRTY
		, .r_tid = tid
		, .r_idepth = idepth + 1
		, .r_iring = {
			  .ir_producer = &intr.r_iring.ir_items[0]
			, .ir_consumer = &intr.r_iring.ir_items[0]
			, .ir_items = {{
				  .it_interruptor = NULL
				, .it_interrupted_idx = 0
				, .it_interruptor_sidx = 0
				, .it_interruptor_eidx = 0
			  }}
		  }
		, .r_sigdepth = {
			  .deltop = 0
			, .depth = idepth + 1
		  }
		, .r_stats = &rs
	};

	rvp_ring_t r = {
		  .r_last = &items[__arraycount(items) - 1]
		, .r_items = items
		, .r_consumer = &items[__arraycount(items) - 2]
		, .r_producer = &items[__arraycount(items) - 2]
		, .r_lastpc = NULL	// expected to be unused
		, .r_lgen = 0		// expected to be unused
		, .r_next = NULL	// expected to be unused
		// clean: no sequence is using this ring, and it has no
		// items that need to be serialized
		//
		// dirty: no sequence is using this ring, however, it
		// contains items that need to be serialized
		//
		// in-use: a sequence is logging on this ring; it
		// may or may not contains items that need to be serialized
		, .r_state = RVP_RING_S_DIRTY
		, .r_tid = tid
		, .r_idepth = idepth
		, .r_iring = {
			  .ir_consumer = &r.r_iring.ir_items[0]
			, .ir_producer = &r.r_iring.ir_items[1]
			, .ir_items = {{
				  .it_interruptor = &intr
				, .it_interrupted_idx = __arraycount(items) - 2
				, .it_interruptor_sidx = __arraycount(intr_items) - 3
				, .it_interruptor_eidx = __arraycount(intr_items) - 2
			  }}
		  }
		, .r_sigdepth = {
			  .deltop = 0
			, .depth = idepth
		  }
		, .r_stats = &intr_rs
	};

	rvp_lastctx_t lc = (rvp_lastctx_t){
		  .lc_tid = tid
		, .lc_idepth = idepth
	};
	const uint32_t step = 0x01010101;

	intr_items[__arraycount(intr_items) - 3] =	htobe32(0xa0b0c0d0 | 0 * step);
	if (rvp_ring_nfull(&r) != 0)
		printf("ring `r` should contain no items, but it does not\n");
	if (rvp_ring_nfull(&intr) != 1) {
		printf("ring `intr` should contain one item, "
		    "but it does not\n");
	}
	if (!rvp_ring_flush_to_fd(&r, STDOUT_FILENO, &lc))
		printf("rvp_ring_flush_to_fd unexpectedly failed\n");
	if (rvp_ring_flush_to_fd(&r, STDOUT_FILENO, &lc))
		printf("rvp_ring_flush_to_fd unexpectedly succeeded\n");
	return EXIT_SUCCESS;
}
