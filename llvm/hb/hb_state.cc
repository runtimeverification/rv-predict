#include "hb_state.h"

hb_state::hb_state(){
	thread_counter = 0;
	vc_size = DEFAULT_THREADS;
	thread_to_vc_index = std::map<uint32_t, std::size_t>() ;
	thread_vc = std::map<uint32_t, VectorClock> ();
	lock_vc = std::map<rvp_addr_t, VectorClock> ();
	read_vc = std::map<rvp_addr_t, VectorClock> ();
	write_vc = std::map<rvp_addr_t, VectorClock> ();
	lastwrite_vc = std::map<rvp_addr_t, VectorClock> ();
}

void hb_state::resize_vectorclocks(size_t new_size){
	
	if(vc_size >= new_size) return;

	for(std::map<uint32_t, VectorClock>::iterator thread_vc_it = thread_vc.begin(); thread_vc_it != thread_vc.end(); ++ thread_vc_it){
		(thread_vc_it->second).resize(new_size);
	}

	for(std::map<rvp_addr_t, VectorClock>::iterator lock_vc_it = lock_vc.begin(); lock_vc_it != lock_vc.end(); ++ lock_vc_it){
		(lock_vc_it->second).resize(new_size);
	}

	for(std::map<rvp_addr_t, VectorClock>::iterator read_vc_it = read_vc.begin(); read_vc_it != read_vc.end(); ++ read_vc_it){
		(read_vc_it->second).resize(new_size);
	}

	for(std::map<rvp_addr_t, VectorClock>::iterator write_vc_it = write_vc.begin(); write_vc_it != write_vc.end(); ++ write_vc_it){
		(write_vc_it->second).resize(new_size);
	}

	for(std::map<rvp_addr_t, VectorClock>::iterator lastwrite_vc_it = lastwrite_vc.begin(); lastwrite_vc_it != lastwrite_vc.end(); ++ lastwrite_vc_it){
		(lastwrite_vc_it->second).resize(new_size);
	}

	vc_size = new_size;
}

std::size_t hb_state::check_and_add_thread(uint32_t tid){
	std::map<uint32_t, size_t>::iterator it = thread_to_vc_index.find(tid);
	size_t tid_index = -1;
	if(it == thread_to_vc_index.end()){
		thread_to_vc_index[tid] = thread_counter;
		tid_index = thread_counter;
		thread_counter ++ ;
		if(thread_counter > vc_size){
			// Assumed invariant: thread_counter = vc_size + 1
			resize_vectorclocks(vc_size + DEFAULT_THREADS); // this call also changes vc_size
		}
		thread_vc[tid] = VectorClock(vc_size);
	}
	else tid_index = it->second();

	return tid_index;
}

void hb_state::check_and_add_lock(rvp_addr_t lock){
	std::map<rvp_addr_t, VectorClock>::iterator it = lock_vc.find(addr);
	if(it == lock_vc.end()){
		lock_vc[addr] = VectorClock(vc_size);
	}
}

void hb_state::check_and_add_load_addr(rvp_addr_t addr){
	std::map<rvp_addr_t, VectorClock>::iterator it = read_vc.find(addr);
	if(it == read_vc.end()){
		read_vc[addr] = VectorClock(vc_size);
	}
}

void hb_state::check_and_add_store_addr(rvp_addr_t addr){
	std::map<rvp_addr_t, VectorClock>::iterator it = write_vc.find(addr);
	if(it == write_vc.end()){
		write_vc[addr] = VectorClock(vc_size);
		// Assumed invariant: \forall addr, addr \in write_vc.keys() iff addr \in lastwrite_vc.keys()
		lastwrite_vc[addr] = VectorClock(vc_size);
	}
}