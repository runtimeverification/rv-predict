#ifndef _RVP_SIGUTIL_H_
#define _RVP_SIGUTIL_H_

#include <signal.h>
#include <stdbool.h>

#include "rvpsignal.h"

bool sigeqset(const sigset_t *, const sigset_t *);
rvp_sigblockset_t *intern_sigset(const sigset_t *);

#endif /* _RVP_SIGUTIL_H_ */
