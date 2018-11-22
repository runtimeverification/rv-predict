#ifndef _RVP_HB_STATE_H_
#define _RVP_HB_STATE_H_

#include<map>
#include<queue>

#include "tracefmt.h"
#include "vc.h"

typedef VectorClock* VC_ptr;

#define MAX_ACCESS_BUF_LEN 100

class hb_state {

private:
	size_t thread_counter;
	size_t vc_size;

public:
	// index of each thread in the vectorclocks.
	std::map<uint32_t, std::size_t>* thread_to_vc_index;

	// clocks for each thread
	std::map<uint32_t, VC_ptr>* thread_vc;

	// clocks for each lock.
	std::map<rvp_addr_t, VC_ptr>* lock_vc;

	// write clocks for each address.
	// We assume all loads and stores are identified by their start addresses.
	// TODO(umang): Do a fine grained analysis later.
	std::map<rvp_addr_t, VC_ptr>* write_vc;

	// read clocks for each address.
	// We assume all loads and stores are identified by their start addresses.
	// TODO(umang): Do a fine grained analysis later.
	std::map<rvp_addr_t, VC_ptr>* read_vc;

	// last-write clock for each address.
	// We assume all loads and stores are identified by their start addresses.
	// TODO(umang): Do a fine grained analysis later.
	std::map<rvp_addr_t, VC_ptr>* lastwrite_vc;

	// (read) queue of time-stamps, one for each address.
	// Each such queue is bounded in length (by MAX_ACCESS_BUF_LEN), and denotes the
	// timestamps of the latest reads on that address.
	std::map<rvp_addr_t, std::queue<VC_ptr>* >* read_access_queue;

	// (write) queue of time-stamps, one for each address.
	// Each such queue is bounded in length (by MAX_ACCESS_BUF_LEN), and denotes the
	// timestamps of the latest writes on that address.
	std::map<rvp_addr_t, std::queue<VC_ptr>* >* write_access_queue;

	// Default constructor
	hb_state();

private:
	// resize all the vector clocks to new_size if this->vc.size < new_size.
	// Side effect: this->vc_size is assigned max(this->vc_size, new_size)
	void resize_vectorclocks(size_t new_size);

public:
	// Return the VC index of the thread tid.
	// Side effect: If tid is a new thread, resize all VectorClocks if required.
	// Side effect: Also add new entry to thread_vc
	std::size_t check_and_add_thread(uint32_t tid);

	// If lock is a newly seen lock, add new entry to lock_vc and return false, else return true.
	bool check_and_add_lock(rvp_addr_t& lock);

	// If addr is a newly read address, add new entry to read_vc and return false, else return true.
	bool check_and_add_read_addr(rvp_addr_t& addr);

	// If addr is a newly written address, add new entry to write_vc and lastwrite_vc, and also return false, else return true.
	bool check_and_add_write_addr(rvp_addr_t& addr);

	// Creates a new VC_ptr using the VC stored at 'vc_ptr'. This new VC_ptr is added to
	// the queue 'read_access_queue(addr)', while maintaining the size of the queue.
	void insert_timestamp_in_read_queue(rvp_addr_t addr, VC_ptr vc_ptr);

	// Creates a new VC_ptr using the VC stored at 'vc_ptr'. This new VC_ptr is added to
	// the queue 'write_access_queue(addr)', while maintaining the size of the queue.
	void insert_timestamp_in_write_queue(rvp_addr_t addr, VC_ptr vc_ptr);
};

#endif /* _RVP_HB_STATE_H_ */
