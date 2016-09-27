#ifndef RVPREDICT_WIREFORMAT_H
#define RVPREDICT_WIREFORMAT_H

#include "common_defs.h"

enum Opcode {
    FUNC_ENTRY      = 7,
    FUNC_EXIT       = 8,
    ISR_ENTRY       = 9,
    ISR_EXIT        = 10,
    DISABLE_IRQ     = 11,
    ENABLE_IRQ      = 12,
    READ1           = 13,
    READ2           = 14,
    READ4           = 15,
    READ8           = 16,
    READ16          = 17,
    WRITE1          = 18,
    WRITE2          = 19,
    WRITE4          = 20,
    WRITE8          = 21,
    WRITE16         = 22,
} __attribute__((packed));

/// Each event starts with this structure.
struct Header {
    Opcode opcode;
    uptr pc;
} __attribute__((packed));

/// Each interrupt-related event starts with this structure.
struct IRQCommon {
    Header header;
    u8 prio;

    IRQCommon(Opcode opcode, uptr pc, u8 prio)
        : header{opcode, pc}
        , prio(prio)
    {}
} __attribute__((packed));

/// Each read and write event starts with this structure.
struct RWCommon {
    Header header;
    uptr addr;

    RWCommon(Opcode opcode, uptr pc, uptr addr)
        : header{opcode, pc}
        , addr(addr)
    {}
} __attribute__((packed));

struct ISREntry {
    IRQCommon common;

    ISREntry(uptr pc, u8 prio)
        : common{Opcode::ISR_ENTRY, pc, prio}
    {}
} __attribute__((packed));

struct ISRExit {
    IRQCommon common;

    ISRExit(uptr pc, u8 prio)
        : common{Opcode::ISR_EXIT, pc, prio}
    {}
} __attribute__((packed));

struct DisableIRQ {
    IRQCommon common;

    DisableIRQ(uptr pc, u8 prio)
        : common{Opcode::DISABLE_IRQ, pc, prio}
    {}
} __attribute__((packed));

struct EnableIRQ {
    IRQCommon common;

    EnableIRQ(uptr pc, u8 prio)
        : common{Opcode::ENABLE_IRQ, pc, prio}
    {}
} __attribute__((packed));

struct Read1 {
    RWCommon common;
    u8 value;

    Read1(uptr pc, uptr addr, u8 value)
        : common{Opcode::READ1, pc, addr}
        , value(value)
    {}
} __attribute__((packed));

struct Read2 {
    RWCommon common;
    u16 value;

    Read2(uptr pc, uptr addr, u16 value)
        : common{Opcode::READ2, pc, addr}
        , value(value)
    {}
} __attribute__((packed));

struct Read4 {
    RWCommon common;
    u32 value;

    Read4(uptr pc, uptr addr, u32 value)
        : common{Opcode::READ4, pc, addr}
        , value(value)
    {}
} __attribute__((packed));

struct Read8 {
    RWCommon common;
    u64 value;

    Read8(uptr pc, uptr addr, u64 value)
        : common{Opcode::READ8, pc, addr}
        , value(value)
    {}
} __attribute__((packed));

struct Read16 {
    RWCommon common;
    u128 value;

    Read16(uptr pc, uptr addr, u128 value)
        : common{Opcode::READ16, pc, addr}
        , value(value)
    {}
} __attribute__((packed));

struct Write1 {
    RWCommon common;
    u8 value;

    Write1(uptr pc, uptr addr, u8 value)
        : common{Opcode::WRITE1, pc, addr}
        , value(value)
    {}
} __attribute__((packed));

struct Write2 {
    RWCommon common;
    u16 value;

    Write2(uptr pc, uptr addr, u16 value)
        : common{Opcode::WRITE2, pc, addr}
        , value(value)
    {}
} __attribute__((packed));

struct Write4 {
    RWCommon common;
    u32 value;

    Write4(uptr pc, uptr addr, u32 value)
        : common{Opcode::WRITE4, pc, addr}
        , value(value)
    {}
} __attribute__((packed));

struct Write8 {
    RWCommon common;
    u64 value;

    Write8(uptr pc, uptr addr, u64 value)
        : common{Opcode::WRITE8, pc, addr}
        , value(value)
    {}
} __attribute__((packed));

struct Write16 {
    RWCommon common;
    u128 value;

    Write16(uptr pc, uptr addr, u128 value)
        : common{Opcode::WRITE16, pc, addr}
        , value(value)
    {}
} __attribute__((packed));

#endif // RVPREDICT_WIREFORMAT_H
