#ifndef _RVP_SUPERVISE_H_
#define _RVP_SUPERVISE_H_

#include <stdbool.h>

extern bool rvp_trace_only;
extern bool rvp_online_analysis;
extern bool rvp_debug_supervisor;
extern int rvp_analysis_fd;
extern volatile _Atomic bool rvp_initialized;
extern const int killer_signum[];

void rvp_supervision_start(void);
void rvp_online_analysis_start(void);
void rvp_offline_analysis_start(void);
char *get_binary_path(void);
extern const char *self_exe_pathname;

#endif /* _RVP_SUPERVISE_H_ */
