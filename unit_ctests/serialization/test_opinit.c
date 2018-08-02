#include "nbcompat.h"
#include "init.h"
#include "rvpendian.h"
#include "trace.h"

rvp_jumpless_op_t rvp_jumpless_op;

void __attribute__((constructor))
rvp_deltop_init(void) 
{
        rvp_jumpless_op = (rvp_jumpless_op_t ){
                  .jo_sigdepth = htobe64(0xaa00bb00cc00dd00)
                , .jo_switch = htobe64(0x0aa00bb00cc00dd0)
        };
}
