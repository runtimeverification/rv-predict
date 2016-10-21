// Copyright (c) 2016 Runtime Verification, Inc. (RV-Predict team). All Rights Reserved.

#include <cstddef>

#include "Runtime.h"
#include "WireFormat.h"

#define CALLERPC ((uptr)__builtin_return_address(0))

/// The board-specific function for transmitting data to the host PC.
/// It will be implemented in C.
extern "C" void transmit_data(void *data, size_t len);

/// Transmit an event as a binary blob.
#define TRANSMIT_EVENT(EventTy, ...) do {   \
    EventTy e{CALLERPC, ##__VA_ARGS__};     \
    transmit_data(&e, sizeof(EventTy));     \
} while (0)

void
__rv_disable_irq(u8 prio)
{
    TRANSMIT_EVENT(DisableIRQ, prio);
}

void
__rv_enable_irq(u8 prio)
{
    TRANSMIT_EVENT(EnableIRQ, prio);
}

void
__rv_isr_entry(u8 prio)
{
    TRANSMIT_EVENT(ISREntry, prio);
}

void
__rv_isr_exit(u8 prio)
{
    TRANSMIT_EVENT(ISRExit, prio);
}

void
__rv_read1(void *addr, u8 val)
{
    TRANSMIT_EVENT(Read1, (uptr)addr, val);
}

void
__rv_read2(void *addr, u16 val)
{
    TRANSMIT_EVENT(Read2, (uptr)addr, val);
}

void
__rv_read4(void *addr, u32 val)
{
    TRANSMIT_EVENT(Read4, (uptr)addr, val);
}

void
__rv_read8(void *addr, u64 val)
{
    TRANSMIT_EVENT(Read8, (uptr)addr, val);
}

void
__rv_read16(void *addr, u128 val)
{
    TRANSMIT_EVENT(Read16, (uptr)addr, val);
}

void
__rv_write1(void *addr, u8 val)
{
    TRANSMIT_EVENT(Write1, (uptr)addr, val);
}

void
__rv_write2(void *addr, u16 val)
{
    TRANSMIT_EVENT(Write2, (uptr)addr, val);
}

void
__rv_write4(void *addr, u32 val)
{
    TRANSMIT_EVENT(Write4, (uptr)addr, val);
}

void
__rv_write8(void *addr, u64 val)
{
    TRANSMIT_EVENT(Write8, (uptr)addr, val);
}

void
__rv_write16(void *addr, u128 val)
{
    TRANSMIT_EVENT(Write16, (uptr)addr, val);
}