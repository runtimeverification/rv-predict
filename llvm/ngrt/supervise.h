#ifndef _RVP_SUPERVISE_H_
#define _RVP_SUPERVISE_H_

#include <signal.h>
#include <stdbool.h>

extern bool rvp_trace_only;
extern bool rvp_online_analysis;
extern bool rvp_debug_supervisor;
extern int rvp_analysis_fd;
/* 
 * In init.h are two inline function that access these initialize bool
 * void ensure_locks_initialized() -=> if rvp_real_locks_initialised is false
 *                                     invoke rvp_lock_prefork_init 
 *                                            Which sets things up so pre-initialization
 *                                            can procede.
 * bool ring_operational() -=> returns the value of rvp_initialized.
 */
extern _Atomic bool rvp_initialized; /* Declared in supervise.c, set in thread.c */ 

int sigaddset_killers(sigset_t *);

void rvp_supervision_start(void);
void rvp_online_analysis_start(void);
void rvp_offline_analysis_start(void);
char *get_binary_path(void);
extern const char *self_exe_pathname;
extern const char *product_name;
void reset_signals(struct sigaction **);
void ignore_signals(struct sigaction **);

#endif /* _RVP_SUPERVISE_H_ */
