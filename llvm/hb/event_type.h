#ifndef _RVP_EVENT_TYPE_H_
#define _RVP_EVENT_TYPE_H_

#include "tracefmt.h"

// TODO(umang): Deal with read-modify-write separately.
enum event_type {
	EVENT_TYPE_READ = 0,
	EVENT_TYPE_WRITE = 1,
	EVENT_TYPE_ACQUIRE = 2,
	EVENT_TYPE_RELEASE = 3,
	EVENT_TYPE_FORK = 4,
	EVENT_TYPE_JOIN = 5,
	EVENT_TYPE_UNKNOWN = 6
};

event_type rvp_op_to_event_type(rvp_op_t op){
	switch(op){
		case RVP_OP_LOAD1:
		case RVP_OP_LOAD2:
		case RVP_OP_LOAD4:
		case RVP_OP_LOAD8:
		case RVP_OP_LOAD16:
		case RVP_OP_ATOMIC_LOAD1:
		case RVP_OP_ATOMIC_LOAD2:
		case RVP_OP_ATOMIC_LOAD4:
		case RVP_OP_ATOMIC_LOAD8:
		case RVP_OP_ATOMIC_LOAD16:
			return EVENT_TYPE_READ;

		case RVP_OP_STORE1:
		case RVP_OP_STORE2:
		case RVP_OP_STORE4:
		case RVP_OP_STORE8:
		case RVP_OP_STORE16:
		case RVP_OP_ATOMIC_STORE1:
		case RVP_OP_ATOMIC_STORE2:
		case RVP_OP_ATOMIC_STORE4:
		case RVP_OP_ATOMIC_STORE8:
		case RVP_OP_ATOMIC_STORE16:
			return EVENT_TYPE_WRITE;

		case RVP_OP_FORK:
			return EVENT_TYPE_FORK;

		case RVP_OP_JOIN:
			return EVENT_TYPE_FORK;

		case RVP_OP_ACQUIRE:
			return EVENT_TYPE_ACQUIRE;

		case RVP_OP_RELEASE:
			return EVENT_TYPE_RELEASE;

		// TODO(umang): RVP_OP_ATOMIC_RMW get labelled as UNKNOWN.
		default:
			return EVENT_TYPE_UNKNOWN;
	
	}
}

#endif /* _RVP_EVENT_TYPE_H_ */