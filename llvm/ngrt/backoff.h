#ifndef _RVP_BACKOFF_H_
#define _RVP_BACKOFF_H_

enum {
	  RVP_BACKOFF_SHORTEST = 32
	, RVP_BACKOFF_LONGEST = 16384
};

typedef struct _rvp_backoff {
	uint32_t b_limit;
} rvp_backoff_t;

static inline void
rvp_backoff_first(rvp_backoff_t *b)
{
	b->b_limit = RVP_BACKOFF_SHORTEST;
}

static inline void
rvp_backoff_next(rvp_backoff_t *b)
{
	if (RVP_BACKOFF_LONGEST - b->b_limit >= b->b_limit)
		b->b_limit += b->b_limit;
}

static inline void
rvp_backoff_pause(const rvp_backoff_t *b)
{
	volatile uint32_t count;

	for (count = 0; count < b->b_limit; count++)
		;	// TBD architecture's pause instruction
}

#endif /* _RVP_BACKOFF_H_ */
