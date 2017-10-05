#include <link.h>

#include "buf.h"
#include "ring.h"
#include "thread.h"
#include "trace.h"

typedef RVP_BUF_TYPE(12 + PATH_MAX) rvp_buf_with_one_path_t;

#define RVP_BUF_WITH_ONE_PATH_INITIALIZER \
RVP_BUF_GENERIC_INITIALIZER(rvp_buf_with_one_path_t)

static uint32_t shared_library_count = 0;

static int
log_shared_library(struct dl_phdr_info *info, size_t size, void *data)
{
	rvp_ring_t *r = rvp_ring_for_curthr();
	rvp_buf_with_one_path_t b_library = RVP_BUF_WITH_ONE_PATH_INITIALIZER;

	int j;
	uint32_t this_library_id = shared_library_count;
	shared_library_count++;

	rvp_buf_put_voidptr(
		&b_library, rvp_vec_and_op_to_deltop(0, RVP_OP_SHARED_LIBRARY));
	
	rvp_buf_put(&b_library, this_library_id);  // library_id
	rvp_buf_put_string(&b_library, info->dlpi_name);
	rvp_ring_put_buf(r, b_library);

	for (j = 0; j < info->dlpi_phnum; j++) {
		// TODO(virgil): filter out segments that are not interesting, e.g. by
		// looking at info->dlpi_phdr[j].p_type.
		void* addr = (void *)(info->dlpi_addr + info->dlpi_phdr[j].p_vaddr);
		Elf32_Word size = info->dlpi_phdr[j].p_memsz;
		rvp_buf_t b_segment = RVP_BUF_INITIALIZER;

		rvp_ring_t *r = rvp_ring_for_curthr();
		rvp_buf_put_pc_and_op(
			&b_segment, &r->r_lastpc, r->r_lastpc, RVP_OP_SHARED_LIBRARY_SEGMENT);
		rvp_buf_put(&b_segment, this_library_id);
		rvp_buf_put_voidptr(&b_segment, addr);
		rvp_buf_put(&b_segment, size);
		rvp_ring_put_buf(r, b_segment);
	}

	return 0;
}

void rvpredict_log_shared_libraries()
{
	dl_iterate_phdr(log_shared_library, NULL);
}
