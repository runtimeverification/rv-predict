/* Copyright (c) 2016,2017,2018 Runtime Verification, Inc.
 * All rights reserved.
 */
#include <signal.h>	/* sigset(3) */

void signals_changemask(int, int, sigset_t *);
void signals_unmask(int, sigset_t *);
void signals_mask(int, sigset_t *);
void signals_restore(const sigset_t *);
