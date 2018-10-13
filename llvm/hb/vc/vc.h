#ifndef _RVP_VC_H_
#define _RVP_VC_H_

#include<vector>

#define DEFAULT_THREADS 16

class VectorClock{
private:
	std::vector<long> clock;

public:
	// Default constructor.
	VectorClock ();
	
	// Copy constructor.
	VectorClock (VectorClock& fromVC);

	// Destructor.
	~VectorClock ();

	// Returns the size of the vector clock.
	int get_dimension();

	// Returns (a copy of) the vector `clock`.
	std::vector<long> get_clock();

	// Returns true if and only if 
	// `\forall t \in [0, ..,clock.size()-1], clock[t] <= rhs.clock[t]`.
	bool le(VectorClock& rhs);

	// Updates each component of the clock such that 
	// `\forall t \in [0, ..,rhs.clock.size()-1] clock[t] = max(rhs.clock[t], old_clock[t]`.
	// Assert failure when clock.size() != rhs.clock.size()
	void join_with(VectorClock& rhs);

	// Updates the clock such that 
	// `\forall t \in [0, ..,rhs.clock.size()-1] clock[t] = rhs.clock[t]`.
	void copy_from(VectorClock& rhs);

	// If dim <= clock.size(), no effect.
	// Otherwise, clock is resized to dim. The new components are initialized to 0.
	void resize(int dim);

	// Increments the ind'th index of clock by 1.
	// Assert failure if clock.size() < ind + 1
	void inc_index(int ind);

	// Changes the entry at the ind'th index of clock to val.
	// Assert failure if clock.size() < ind + 1.
	void update_index(int ind, long val);
};


#endif /* _RVP_VC_H_ */