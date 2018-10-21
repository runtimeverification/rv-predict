#ifndef _RVP_HB_STATE_H_
#define _RVP_HB_STATE_H_

#include "tracefmt.h"
#include "vc.h"

class hb_state {

private:
	size_t thread_counter;
	size_t vc_size;

public:
	// index of each thread in the vectorclocks.
	std::map<uint32_t, std::size_t> thread_to_vc_index;

	// clocks for each thread
	std::map<uint32_t, VectorClock> thread_vc;

	// clocks for each lock.
	std::map<rvp_addr_t, VectorClock> lock_vc;

	// write clocks for each address.
	// We assume all loads and stores are identified by their start addresses.
	// TODO(umang): Do a fine grained analysis later.
	std::map<rvp_addr_t, VectorClock> write_vc;

	// read clocks for each address.
	// We assume all loads and stores are identified by their start addresses.
	// TODO(umang): Do a fine grained analysis later.
	std::map<rvp_addr_t, VectorClock> read_vc;

	// last-write clock for each address.
	// We assume all loads and stores are identified by their start addresses.
	// TODO(umang): Do a fine grained analysis later.
	std::map<rvp_addr_t, VectorClock> lastwrite_vc

	// Default constructor
	hb_state();

private:
	// resize all the vector clocks to new_size if this->vc.size < new_size.
	// Side effect: this->vc_size is assigned max(this->vc_size, new_size)
	void resize_vectorclocks(size_t new_size);

	// Return the VC index of the thread tid.
	// Side effect: If tid is a new thread, resize all VectorClocks if required.
	// Side effect: Also add new entry to thread_vc
	std::size_t check_and_add_thread(uint32_t tid);

	// If lock is a newly seen lock, add new entry to lock_vc.
	void check_and_add_lock(rvp_addr_t lock);

	// If addr is a newly read address, add new entry to read_vc.
	void check_and_add_load_addr(rvp_addr_t addr);

	// If addr is a newly written address, add new entry to write_vc and lastwrite_vc.
	void check_and_add_store_addr(rvp_addr_t addr);
};

#endif /* _RVP_HB_STATE_H_ */