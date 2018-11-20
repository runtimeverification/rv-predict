#include <assert.h>

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
	//TODO: Have a compile-time debug flag and enable this assert only when the flag is on.
	assert(clock.size() == rhs.clock.size());
	bool is_lte = true;
	for(int i = 0; i <clock.size(); i++){
		if(clock[i] > rhs.clock[i]){
			is_lte = false;
			break;
		}
	}
	return is_lte;
}

void VectorClock::join_with(VectorClock& rhs){
	//TODO: Have a compile-time debug flag and enable this assert only when the flag is on.
	assert(clock.size() == rhs.clock.size());
	for(int i = 0; i <clock.size(); i++){
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
	//TODO: Have a compile-time debug flag and enable this assert only when the flag is on.
	assert(clock.size() >= ind + 1);
	clock[ind] = clock[ind] + 1;
}

void VectorClock::set_index(std::size_t ind, long val){
	//TODO: Have a compile-time debug flag and enable this assert only when the flag is on.
	assert(clock.size() >= ind + 1);
	clock[ind] = val;
}

long VectorClock::get_index(std::size_t ind){
	//TODO: Have a compile-time debug flag and enable this assert only when the flag is on.
	assert(clock.size() >= ind + 1);
	return clock[ind];
}