/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_INTR_H_
#define _RVP_INTR_H_

#include <stdbool.h>
#include <stdint.h>

typedef void (*rvp_intr_handler_t)(void);

typedef struct _rvp_static_intr {
	rvp_intr_handler_t		si_handler;
	volatile _Atomic int32_t	si_prio;
	volatile _Atomic int		si_signum;
	volatile _Atomic int		si_nactive;
	volatile _Atomic uint32_t	si_times;
} rvp_static_intr_t;

void rvp_static_intr_fire_all(void);
void __rvpredict_intr_register(void (*)(void), int32_t);
struct itimerspec rvp_static_intr_interval(void);

void __rvpredict_intr_personality_init(void);
void __rvpredict_intr_personality_reinit(void);
void __rvpredict_intr_personality_enable(void);
int __rvpredict_intr_personality_splhigh(void);
void __rvpredict_intr_personality_splx(int);
void __rvpredict_intr_personality_fire_all(void);
void __rvpredict_intr_personality_disable(void);

extern const char __data_registers_begin;
extern const char __data_registers_end;
extern int rvp_static_nintrs;
extern int rvp_static_nassigned;
extern bool rvp_static_intr_debug;
extern rvp_static_intr_t rvp_static_intr[128];
extern const char __rvpredict_intr_personality_name[];

#endif /* _RVP_INTR_H_ */

