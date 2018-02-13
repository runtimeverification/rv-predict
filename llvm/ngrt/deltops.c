#include "nbcompat.h"
#include "tracefmt.h"
#include "trace.h"

static __section(".text") deltops_t deltops = { .matrix = { { 0 } } };

deltop_t *
rvp_vec_and_op_to_deltop(int jmpvec, rvp_op_t op)
{
	deltop_t *deltop =
	    &deltops.matrix[__arraycount(deltops.matrix) / 2 + jmpvec][op];

	if (deltop < &deltops.matrix[0][0] ||
		     &deltops.matrix[RVP_NJMPS - 1][RVP_NOPS - 1] < deltop)
		return NULL;
	
	return deltop;
}
