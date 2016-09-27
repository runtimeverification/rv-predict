// Copyright (c) 2016 Runtime Verification, Inc. (RV-Predict team). All Rights Reserved.

#ifndef RVPREDICT_RUNTIME_H
#define RVPREDICT_RUNTIME_H

extern "C" {

#include "common_defs.h"

void __rv_disable_irq(u8 prio);
void __rv_enable_irq(u8 prio);

void __rv_isr_entry(u8 prio);
void __rv_isr_exit(u8 prio);

void __rv_read1(void *addr, u8 val);
void __rv_read2(void *addr, u16 val);
void __rv_read4(void *addr, u32 val);
void __rv_read8(void *addr, u64 val);
void __rv_read16(void *addr, u128 val);

void __rv_write1(void *addr, u8 val);
void __rv_write2(void *addr, u16 val);
void __rv_write4(void *addr, u32 val);
void __rv_write8(void *addr, u64 val);
void __rv_write16(void *addr, u128 val);

}

#endif // RVPREDICT_RUNTIME_H
