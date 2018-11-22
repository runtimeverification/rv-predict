#include "vc.h"

VectorClock::VectorClock(){
	clock = *(new std::vector<long>(DEFAULT_THREADS, 0L));
}

VectorClock::VectorClock(std::size_t n){
	clock = *(new std::vector<long>(n, 0L));
}

VectorClock::VectorClock(VectorClock& from){
	clock = *(new std::vector<long>(from.clock));
}

VectorClock::~VectorClock (){
	clock.~vector<long>();
}

std::size_t VectorClock::get_dimension(){
	return clock.size();
}

std::vector<long>& VectorClock::get_clock(){
	return clock;
}

bool VectorClock::le(VectorClock& rhs){
	std::size_t rhs_size = rhs.clock.size();
	std::size_t clock_size = clock.size();
	bool is_lte = true;
	for(int i = 0; i < clock_size && i < rhs_size; i++){
		if(clock[i] > rhs.clock[i]){
			is_lte = false;
			break;
		}
	}
	if(clock_size > rhs_size && is_lte){
		for(int i = rhs_size; i < clock_size; i++){
			if(clock[i] > 0){
				is_lte = false;
				break;
			}
		}
	}
	return is_lte;
}

void VectorClock::join_with(VectorClock& rhs){
	std::size_t clock_size = clock.size();
	std::size_t rhs_size = rhs.clock.size();
	if(clock_size < rhs_size){
		resize(rhs_size);
	}
	for(int i = 0; i < rhs_size; i++){
		if(rhs.clock[i] > clock[i]){
			clock[i] = rhs.clock[i];
		}
	}
}

void VectorClock::copy_from(VectorClock& rhs){
	clock = std::vector<long>(rhs.clock);
}

void VectorClock::resize(std::size_t dim){
	if(clock.size() > dim){
		clock.resize(dim, 0);
	}
}

void VectorClock::inc_index(std::size_t ind){
	if(clock.size() < ind + 1){
		resize(ind + 1);
	}
	clock[ind] = clock[ind] + 1;
}

void VectorClock::set_index(std::size_t ind, long val){
	if(clock.size() < ind + 1){
		resize(ind + 1);
	}
	clock[ind] = val;
}

long VectorClock::get_index(std::size_t ind){
	if(clock.size() < ind + 1){
		return 0;
	}
	return clock[ind];
}