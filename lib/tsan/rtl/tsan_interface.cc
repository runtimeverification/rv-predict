//===-- tsan_interface.cc -------------------------------------------------===//
//
//                     The LLVM Compiler Infrastructure
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
//
// This file is a part of ThreadSanitizer (TSan), a race detector.
//
//===----------------------------------------------------------------------===//

#include "tsan_interface.h"
#include "tsan_interface_ann.h"
#include "tsan_rtl.h"
#include "sanitizer_common/sanitizer_internal_defs.h"

#define CALLERPC ((uptr)__builtin_return_address(0))

using namespace __tsan;  // NOLINT

typedef u16 uint16_t;
typedef u32 uint32_t;
typedef u64 uint64_t;

void __tsan_init() {
  Initialize(cur_thread());
}

void __tsan_read16(void *addr) {
  RVSaveMemAccEvent(READ, (uptr)addr, *((u64*)addr), CALLERPC);
  RVSaveMemAccEvent(READ, (uptr)addr + 8, *((u64*)addr + 8), CALLERPC);
}

void __tsan_write16(void *addr, void *val) {
  RVSaveMemAccEvent(WRITE, (uptr)addr, (u64)val, CALLERPC);
  RVSaveMemAccEvent(WRITE, (uptr)addr+8, (u64)val, CALLERPC);
  //TODO(TraianSF): We're recording the latter event with the same value here
}

void __tsan_read16_pc(void *addr, void *pc) {
  RVSaveMemAccEvent(READ, (uptr)addr, *((u64*)addr), CALLERPC);
  RVSaveMemAccEvent(READ, (uptr)addr + 8, *((u64*)addr + 8), CALLERPC);
}

void __tsan_write16_pc(void *addr, void *pc, void *val) {
  RVSaveMemAccEvent(WRITE, (uptr)addr, (u64)val, (uptr)pc);
  RVSaveMemAccEvent(WRITE, (uptr)addr + 8, (u64)val, (uptr)pc);
  //TODO(TraianSF): We're recording the latter event with the same value here
}

// __tsan_unaligned_read/write calls are emitted by compiler.

void __tsan_unaligned_read2(const void *addr) {
  RVSaveMemAccEvent(READ, (uptr)addr, *((u16*)addr), CALLERPC);
}

void __tsan_unaligned_read4(const void *addr) {
  RVSaveMemAccEvent(READ, (uptr)addr, *((u32*)addr), CALLERPC);
}

void __tsan_unaligned_read8(const void *addr) {
  RVSaveMemAccEvent(READ, (uptr)addr, *((u64*)addr), CALLERPC);
}

void __tsan_unaligned_read16(const void *addr) {
  RVSaveMemAccEvent(READ, (uptr)addr, *((u64*)addr), CALLERPC);
  RVSaveMemAccEvent(READ, (uptr)addr + 8, *((u64*)addr + 8), CALLERPC);
}

void __tsan_unaligned_write2(void *addr, void *val) {
  RVSaveMemAccEvent(WRITE, (uptr)addr, (u16)(u64)val, CALLERPC);
}

void __tsan_unaligned_write4(void *addr, void *val) {
  RVSaveMemAccEvent(WRITE, (uptr)addr, (u32)(u64)val, CALLERPC);
}

void __tsan_unaligned_write8(void *addr, void *val) {
  RVSaveMemAccEvent(WRITE, (uptr)addr, (u64)val, CALLERPC);
}

void __tsan_unaligned_write16(void *addr, void *val) {
  RVSaveMemAccEvent(WRITE, (uptr)addr, (u64)val, CALLERPC);
  RVSaveMemAccEvent(WRITE, (uptr)addr + 8, (u64)val, CALLERPC);
  //TODO(TraianSF): We're recording the latter event with the same value here
}

// __sanitizer_unaligned_load/store are for user instrumentation.

extern "C" {
SANITIZER_INTERFACE_ATTRIBUTE
u16 __sanitizer_unaligned_load16(const uu16 *addr) {
  __tsan_unaligned_read2(addr);
  return *addr;
}

SANITIZER_INTERFACE_ATTRIBUTE
u32 __sanitizer_unaligned_load32(const uu32 *addr) {
  __tsan_unaligned_read4(addr);
  return *addr;
}

SANITIZER_INTERFACE_ATTRIBUTE
u64 __sanitizer_unaligned_load64(const uu64 *addr) {
  __tsan_unaligned_read8(addr);
  return *addr;
}

SANITIZER_INTERFACE_ATTRIBUTE
void __sanitizer_unaligned_store16(uu16 *addr, u16 v) {
  __tsan_unaligned_write2(addr, (void*) v);
  *addr = v;
}

SANITIZER_INTERFACE_ATTRIBUTE
void __sanitizer_unaligned_store32(uu32 *addr, u32 v) {
  __tsan_unaligned_write4(addr, (void*)v);
  *addr = v;
}

SANITIZER_INTERFACE_ATTRIBUTE
void __sanitizer_unaligned_store64(uu64 *addr, u64 v) {
  __tsan_unaligned_write8(addr,  (void*)v);
  *addr = v;
}
}  // extern "C"

void __tsan_acquire(void *addr) {
  Acquire(cur_thread(), CALLERPC, (uptr)addr);
}

void __tsan_release(void *addr) {
  Release(cur_thread(), CALLERPC, (uptr)addr);
}
