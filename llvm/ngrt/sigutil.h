#ifndef _RVP_SIGUTIL_H_
#define _RVP_SIGUTIL_H_

#include <signal.h>
#include <stdbool.h>

#include "rvpsignal.h"

bool sigeqset(const sigset_t *, const sigset_t *);
int sigfirstmember(const sigset_t *);

#endif /* _RVP_SIGUTIL_H_ */
