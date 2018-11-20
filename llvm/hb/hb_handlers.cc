#include "hb_handlers.h"
#include<iostream>

bool hb_handler(hb_state* state_ptr, uint32_t tid, rvp_op_t op, const rvp_ubuf_t* decor){

	std::size_t tid_idx = state_ptr->check_and_add_thread(tid);
	switch(op){
		case RVP_OP_LOAD1:
		case RVP_OP_LOAD2:
		case RVP_OP_LOAD4:
		case RVP_OP_ATOMIC_LOAD1:
		case RVP_OP_ATOMIC_LOAD2:
		case RVP_OP_ATOMIC_LOAD4:
			return hb_handler_read(state_ptr, tid, tid_idx, decor->ub_load1_2_4_store1_2_4.addr);

		case RVP_OP_LOAD8:
		case RVP_OP_LOAD16: // TODO(umang): Ask david what field corresponds to 16 byte wide load/stores
		case RVP_OP_ATOMIC_LOAD8:
		case RVP_OP_ATOMIC_LOAD16: // TODO(umang): Ask david what field corresponds to 16 byte wide load/stores
			return hb_handler_read(state_ptr, tid, tid_idx, decor->ub_load8_store8.addr);

		case RVP_OP_STORE1:
		case RVP_OP_STORE2:
		case RVP_OP_STORE4:
		case RVP_OP_ATOMIC_STORE1:
		case RVP_OP_ATOMIC_STORE2:
		case RVP_OP_ATOMIC_STORE4:
			return hb_handler_write(state_ptr, tid, tid_idx, decor->ub_load1_2_4_store1_2_4.addr);

		case RVP_OP_STORE8:
		case RVP_OP_STORE16:
		case RVP_OP_ATOMIC_STORE8:
		case RVP_OP_ATOMIC_STORE16:
			return hb_handler_write(state_ptr, tid, tid_idx, decor->ub_load8_store8.addr);

		case RVP_OP_FORK:
			return hb_handler_fork(state_ptr, tid, tid_idx, decor->ub_fork_join_switch.tid);

		case RVP_OP_JOIN:
			return hb_handler_join(state_ptr, tid, tid_idx, decor->ub_fork_join_switch.tid);

		case RVP_OP_ACQUIRE:
			return hb_handler_acquire(state_ptr, tid, tid_idx, decor->ub_acquire_release.addr);

		case RVP_OP_RELEASE:
			return hb_handler_release(state_ptr, tid, tid_idx, decor->ub_acquire_release.addr);

		// TODO(umang): RVP_OP_ATOMIC_RMW get labelled as UNKNOWN.
		default:
			return false;			
	}
}

bool exists_key_in_map(std::map<rvp_addr_t, VC_ptr>* map_ptr, rvp_addr_t& key){
	return map_ptr->find(key) != map_ptr->end();
}

bool hb_handler_read(hb_state* state_ptr, uint32_t tid, std::size_t t_idx, rvp_addr_t addr){
	bool race_detected = false;
	VC_ptr C_t = (state_ptr->thread_vc)->find(tid)->second;
	if(exists_key_in_map(state_ptr->write_vc, addr)){
		VC_ptr W_x = (state_ptr->write_vc)->find(addr)->second;
		if(!W_x->le(*C_t)){
			race_detected = true;
		}
	}
	bool unused = state_ptr->check_and_add_read_addr(addr);
	VC_ptr R_x = (state_ptr->read_vc)->find(addr)->second;
	R_x->set_index(t_idx, C_t->get_index(t_idx));
	return race_detected;
}

bool hb_handler_write(hb_state* state_ptr, uint32_t tid, std::size_t t_idx, rvp_addr_t addr){
	bool race_detected = false;
	VC_ptr C_t = (state_ptr->thread_vc)->find(tid)->second;
	if(exists_key_in_map(state_ptr->read_vc, addr)){
		VC_ptr R_x = (state_ptr->read_vc)->find(addr)->second;
		if(!R_x->le(*C_t)){
			race_detected = true;
		}
	}
	bool exists_addr_in_write_map =	state_ptr->check_and_add_write_addr(addr);
	VC_ptr W_x = (state_ptr->write_vc)->find(addr)->second;
	if(!W_x->le(*C_t)){
		race_detected = true;
	}
	W_x->join_with(*C_t);
	return race_detected;
}

bool hb_handler_acquire(hb_state* state_ptr, uint32_t tid, std::size_t t_idx, rvp_addr_t lock){
	VC_ptr C_t = (state_ptr->thread_vc)->find(tid)->second;
	bool unused = state_ptr->check_and_add_lock(lock);
	VC_ptr L_l = (state_ptr->lock_vc)->find(lock)->second;
	C_t->join_with(*L_l);
	return false;
}

bool hb_handler_release(hb_state* state_ptr, uint32_t tid, std::size_t t_idx, rvp_addr_t lock){
	VC_ptr C_t = (state_ptr->thread_vc)->find(tid)->second;
	// TODO(umang): maybe assume that the lock would have been seen already?
	bool unused = state_ptr->check_and_add_lock(lock);
	VC_ptr L_l = (state_ptr->lock_vc)->find(lock)->second;
	L_l->copy_from(*C_t);
	return false;
}

bool hb_handler_fork(hb_state* state_ptr, uint32_t tid, std::size_t t_idx, uint32_t child){
	VC_ptr C_t = (state_ptr->thread_vc)->find(tid)->second;
	std::size_t unused = state_ptr->check_and_add_thread(child);
	VC_ptr C_child = (state_ptr->thread_vc)->find(child)->second;
	C_child->join_with(*C_t);
	return false;
}

bool hb_handler_join(hb_state* state_ptr, uint32_t tid, std::size_t t_idx, uint32_t child){
	VC_ptr C_t = (state_ptr->thread_vc)->find(tid)->second;
	// TODO(umang): maybe assume that the child thread would have been seen at this point?
	std::size_t unused = state_ptr->check_and_add_thread(child);
	VC_ptr C_child = (state_ptr->thread_vc)->find(child)->second;
	C_t->join_with(*C_child);
	return false;
}