#include "hb_state.h"

hb_state::hb_state(){
	thread_counter = 0;
	vc_size = DEFAULT_THREADS;
	thread_to_vc_index = new std::map<uint32_t, std::size_t>() ;
	thread_vc = new std::map<uint32_t, VC_ptr> ();
	lock_vc = new std::map<rvp_addr_t, VC_ptr> ();
	read_vc = new std::map<rvp_addr_t, VC_ptr> ();
	write_vc = new std::map<rvp_addr_t, VC_ptr> ();
	lastwrite_vc = new std::map<rvp_addr_t, VC_ptr> ();
	read_access_queue = new std::map<rvp_addr_t, std::queue<VC_ptr>* > ();
	write_access_queue = new std::map<rvp_addr_t, std::queue<VC_ptr>* > ();
}

void hb_state::resize_vectorclocks(size_t new_size){
	
	if(vc_size >= new_size) return;

	for(std::map<uint32_t, VC_ptr>::iterator thread_vc_it = thread_vc->begin(); thread_vc_it != thread_vc->end(); ++ thread_vc_it){
		(thread_vc_it->second)->resize(new_size);
	}

	for(std::map<rvp_addr_t, VC_ptr>::iterator lock_vc_it = lock_vc->begin(); lock_vc_it != lock_vc->end(); ++ lock_vc_it){
		(lock_vc_it->second)->resize(new_size);
	}

	for(std::map<rvp_addr_t, VC_ptr>::iterator read_vc_it = read_vc->begin(); read_vc_it != read_vc->end(); ++ read_vc_it){
		(read_vc_it->second)->resize(new_size);
	}

	for(std::map<rvp_addr_t, VC_ptr>::iterator write_vc_it = write_vc->begin(); write_vc_it != write_vc->end(); ++ write_vc_it){
		(write_vc_it->second)->resize(new_size);
	}

	for(std::map<rvp_addr_t, VC_ptr>::iterator lastwrite_vc_it = lastwrite_vc->begin(); lastwrite_vc_it != lastwrite_vc->end(); ++ lastwrite_vc_it){
		(lastwrite_vc_it->second)->resize(new_size);
	}

	vc_size = new_size;
}

void insert_timestamp_in_queue(std::map<rvp_addr_t, std::queue<VC_ptr>* >* mp, rvp_addr_t addr, VC_ptr vc_ptr){
	VC_ptr new_vc = new VectorClock(*vc_ptr);
	std::map<rvp_addr_t, std::queue<VC_ptr>* >::iterator it = mp->find(addr);
	if(it == mp->end()){
		(*mp)[addr] = new std::queue<VC_ptr> ();
	}
	(*mp)[addr]->push(new_vc);
	std::size_t len = (*mp)[addr]->size();
	if(len > MAX_ACCESS_BUF_LEN){
		(*mp)[addr]->pop();
	}
}

void hb_state::insert_timestamp_in_read_queue(rvp_addr_t addr, VC_ptr vc_ptr){
	insert_timestamp_in_queue(read_access_queue, addr, vc_ptr);
}

void hb_state::insert_timestamp_in_write_queue(rvp_addr_t addr, VC_ptr vc_ptr){
	insert_timestamp_in_queue(write_access_queue, addr, vc_ptr);
}

std::size_t hb_state::check_and_add_thread(uint32_t tid){
	std::map<uint32_t, size_t>::iterator it = thread_to_vc_index->find(tid);
	size_t tid_index = -1;
	if(it == thread_to_vc_index->end()){
		(*thread_to_vc_index)[tid] = thread_counter;
		tid_index = thread_counter;
		thread_counter ++ ;
		if(thread_counter > vc_size){
			// Assumed invariant: thread_counter = vc_size + 1
			resize_vectorclocks(vc_size + DEFAULT_THREADS); // this call also changes vc_size
		}
		VC_ptr tid_vc = new VectorClock(vc_size);
		tid_vc->set_index(tid_index, 1);
		(*thread_vc)[tid] = tid_vc;
	}
	else {
		tid_index = it->second;
	}
	return tid_index;
}

bool hb_state::check_and_add_lock(rvp_addr_t& lock){
	std::map<rvp_addr_t, VC_ptr>::iterator it = lock_vc->find(lock);
	if(it == lock_vc->end()){
		(*lock_vc)[lock] = new VectorClock(vc_size);
		return false;
	}
	else return true;
}

bool hb_state::check_and_add_read_addr(rvp_addr_t& addr){
	std::map<rvp_addr_t, VC_ptr>::iterator it = read_vc->find(addr);
	if(it == read_vc->end()){
		(*read_vc)[addr] = new VectorClock(vc_size);
		return false;
	}
	else return true;
}

bool hb_state::check_and_add_write_addr(rvp_addr_t& addr){
	std::map<rvp_addr_t, VC_ptr>::iterator it = write_vc->find(addr);
	if(it == write_vc->end()){
		(*write_vc)[addr] = new  VectorClock(vc_size);
		// Assumed invariant: \forall addr, addr \in write_vc.keys() iff addr \in lastwrite_vc.keys()
		(*lastwrite_vc)[addr] = new VectorClock(vc_size);
		return false;
	}
	else return true;
}