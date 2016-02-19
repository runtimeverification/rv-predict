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
  RVReadInteger((uptr)addr, 16, CALLERPC);
}

void __tsan_write16(void *addr, void *val) {
  RVWriteInteger((uptr)addr, 16, CALLERPC, val);
}

void __tsan_read16_pc(void *addr, void *pc) {
  RVReadInteger((uptr)addr, 16, (uptr)pc);
}

void __tsan_write16_pc(void *addr, void *pc, void *val) {
  RVWriteInteger((uptr)addr, 16, (uptr)pc, val);
}

// __tsan_unaligned_read/write calls are emitted by compiler.

void __tsan_unaligned_read(const void *addr, const int size) {
  for(int i = 0; i < size; ++i) {
    RVSaveMemAccEvent(READ, (uptr)((const u8*)addr + i), *((const u8*)addr + i), CALLERPC);
  }
}

void __tsan_unaligned_read2(const void *addr) {
  RVReadInteger((uptr)addr, 2, CALLERPC);
}

void __tsan_unaligned_read4(const void *addr) {
  RVReadInteger((uptr)addr, 4, CALLERPC);
}

void __tsan_unaligned_read8(const void *addr) {
  RVReadInteger((uptr)addr, 8, CALLERPC);
}

void __tsan_unaligned_read16(const void *addr) {
  RVReadInteger((uptr)addr, 16, CALLERPC);
}

void __tsan_unaligned_write(void *addr, void *val, const int size) {
  val = &val;
  for(int i = 0; i < size; ++i) {
    RVSaveMemAccEvent(WRITE, (uptr)((u8*)addr + i), *((u8*)val + i), CALLERPC);
  }
}

void __tsan_unaligned_write2(void *addr, void *val) {
  RVWriteInteger((uptr)addr, 2, CALLERPC, val);
}

void __tsan_unaligned_write4(void *addr, void *val) {
  RVWriteInteger((uptr)addr, 4, CALLERPC, val);
}

void __tsan_unaligned_write8(void *addr, void *val) {
  RVWriteInteger((uptr)addr, 8, CALLERPC, val);
}

void __tsan_unaligned_write16(void *addr, void *val) {
  RVWriteInteger((uptr)addr, 16, CALLERPC, val);
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
  __tsan_unaligned_write2(addr, (void*)v);
  *addr = v;
}

SANITIZER_INTERFACE_ATTRIBUTE
void __sanitizer_unaligned_store32(uu32 *addr, u32 v) {
  __tsan_unaligned_write4(addr, (void*)v);
  *addr = v;
}

SANITIZER_INTERFACE_ATTRIBUTE
void __sanitizer_unaligned_store64(uu64 *addr, u64 v) {
  __tsan_unaligned_write8(addr, (void*)v);
  *addr = v;
}
}  // extern "C"

void __tsan_acquire(void *addr) {
  Acquire(cur_thread(), CALLERPC, (uptr)addr);
}

void __tsan_release(void *addr) {
  Release(cur_thread(), CALLERPC, (uptr)addr);
}
