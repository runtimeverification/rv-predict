#include "hb_state.h"
#include "event_type.h"

bool hb_handler(hb_state* state_ptr, uint32 tid, event_type etype, rvp_ubuf_t& decor){

	std::size_t tid_idx = state_ptr->check_and_add_thread(tid);
	switch(etype){
		case EVENT_TYPE_READ:
			return hb_handler_read(state_ptr, tid, tid_idx, decor.)
	}
}

bool exists_key_in_map(std::map<rvp_addr_t, VC_ptr>* map_ptr, rvp_addr_t& key){
	return map_ptr->find(key) != map_ptr->end();
}

bool hb_handler_read(hb_state* state_ptr, uint32 tid, std::size_t t_idx, rvp_addr_t& addr){
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

bool hb_handler_write(hb_state* state_ptr, uint32 tid, std::size_t t_idx, rvp_addr_t& addr){
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

bool hb_handler_acquire(hb_state* state_ptr, uint32 tid, std::size_t t_idx, rvp_addr_t lock){
	VC_ptr C_t = (state_ptr->thread_vc)->find(tid)->second;
	bool unused = state_ptr->check_and_add_lock(lock);
	VC_ptr L_l = (state_ptr->lock_vc)->find(lock)->second;
	C_t->join_with(*L_l);
	return false;
}

bool hb_handler_release(hb_state* state_ptr, uint32 tid, std::size_t t_idx, rvp_addr_t lock){
	VC_ptr C_t = (state_ptr->thread_vc)->find(tid)->second;
	// TODO(umang): maybe assume that the lock would have been seen already?
	bool unused = state_ptr->check_and_add_lock(lock);
	VC_ptr L_l = (state_ptr->lock_vc)->find(lock)->second;
	L_l->copy_from(C_t);
	return false;
}

bool hb_handler_fork(hb_state* state_ptr, uint32 tid, std::size_t t_idx, uint32 child){
	VC_ptr C_t = (state_ptr->thread_vc)->find(tid)->second;
	std::size_t unused = state_ptr->check_and_add_thread(child);
	VC_ptr C_child = (state_ptr->lock_vc)->find(child)->second;
	C_child->join_with(*C_t);
	return false;
}

bool hb_handler_join(hb_state* state_ptr, uint32 tid, std::size_t t_idx, uint32 child){
	VC_ptr C_t = (state_ptr->thread_vc)->find(tid)->second;
	// TODO(umang): maybe assume that the child thread would have been seen at this point?
	std::size_t unused = state_ptr->check_and_add_thread(child);
	VC_ptr C_child = (state_ptr->lock_vc)->find(child)->second;
	C_t->join_with(*C_child);
	return false;
}