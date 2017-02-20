#ifndef _RVP_SIGNAL_H_
#define _RVP_SIGNAL_H_

#include <stdint.h>	/* for uint32_t */
#include <signal.h>	/* for siginfo_t, sigset_t */

#include "thread.h"

struct _rvp_sigblockset;
typedef struct _rvp_sigblockset rvp_sigblockset_t;

struct _rvp_sigblockset {
	uint32_t		bs_number;
	sigset_t		bs_sigset;
	rvp_sigblockset_t	*bs_next;
};

typedef struct _rvp_signal {
	int			s_signum;
	rvp_sigblockset_t	*s_blockset;
	void			(*s_handler)(int);
	void			(*s_sigaction)(int, siginfo_t *, void *);
} rvp_signal_t;

rvp_ring_t *rvp_signal_ring_get(rvp_thread_t *);
void rvp_signal_ring_put(rvp_thread_t *, rvp_ring_t *);

rvp_signal_t *rvp_signal_lookup(int);

int __rvpredict_sigaction(int, const struct sigaction *, struct sigaction *);
int __rvpredict_sigprocmask(int, const sigset_t *, sigset_t *);
int __rvpredict_pthread_sigmask(int, const sigset_t *, sigset_t *);

uint32_t rvp_sigblocksets_emit(int, uint32_t);
rvp_sigblockset_t *intern_sigset(const sigset_t *);
void rvp_signal_rings_replenish(void);
bool rvp_signal_rings_flush_to_fd(int);

#endif /* _RVP_SIGNAL_H_ */
