/* Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved. */

#ifndef _RVP_INTR_H_
#define _RVP_INTR_H_

#include <stdbool.h>
#include <stdint.h>

struct _rvp_intr_personality {
	const char *ip_name;
	void (*ip_init)(void);
	void (*ip_reinit)(void);
	void (*ip_enable)(void);
	int (*ip_splhigh)(void);
	void (*ip_splx)(int);
	void (*ip_fire_all)(void);
	void (*ip_disable)(void);
};

typedef void (*rvp_intr_handler_t)(void);

typedef struct _rvp_static_intr {
	rvp_intr_handler_t		si_handler;
	volatile _Atomic int32_t	si_prio;
	volatile _Atomic int		si_signum;
	volatile _Atomic int		si_nactive;
	volatile _Atomic uint32_t	si_times;
} rvp_static_intr_t;

typedef struct _rvp_intr_personality rvp_intr_personality_t;

void rvp_static_intr_fire_all(void);
void __rvpredict_intr_register(void (*)(void), int32_t);
void __rvpredict_static_intr_handler(int);
struct itimerspec rvp_static_intr_interval(void);

extern const char __data_registers_begin;
extern const char __data_registers_end;
extern int rvp_static_nintrs;
extern int rvp_static_nassigned;
extern bool rvp_static_intr_debug;
extern rvp_static_intr_t rvp_static_intr[128];

extern const rvp_intr_personality_t basic_intr_personality;
extern const rvp_intr_personality_t renesas_78k0_intr_personality;

#endif /* _RVP_INTR_H_ */

