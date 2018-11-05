#ifndef _RVP_HB_HANDLERS_H_
#define _RVP_HB_HANDLERS_H_

#include "hb_state.h"
#include "reader.h"

bool hb_handler(hb_state* state_ptr, uint32_t tid, rvp_op_t op, rvp_ubuf_t* decor);

bool hb_handler_read(hb_state* state_ptr, uint32_t tid, std::size_t t_idx, rvp_addr_t addr);

bool hb_handler_write(hb_state* state_ptr, uint32_t tid, std::size_t t_idx, rvp_addr_t addr);

bool hb_handler_acquire(hb_state* state_ptr, uint32_t tid, std::size_t t_idx, rvp_addr_t lock);

bool hb_handler_release(hb_state* state_ptr, uint32_t tid, std::size_t t_idx, rvp_addr_t lock);

bool hb_handler_fork(hb_state* state_ptr, uint32_t tid, std::size_t t_idx, uint32_t child);

bool hb_handler_join(hb_state* state_ptr, uint32_t tid, std::size_t t_idx, uint32_t child);

#endif /* _RVP_HB_HANDLERS_H_ */