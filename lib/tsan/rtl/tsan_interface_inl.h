//===-- tsan_interface_inl.h ------------------------------------*- C++ -*-===//
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
#include "tsan_rtl.h"

#define CALLERPC ((uptr)__builtin_return_address(0))

using namespace __tsan;  // NOLINT

void __tsan_read1(void *addr) {
  RVSaveMemAccEvent(READ, (uptr)addr, *(u8*)addr, CALLERPC);
}

void __tsan_read2(void *addr) {
  RVSaveMemAccEvent(READ, (uptr)addr, *(u16*)addr, CALLERPC);
}

void __tsan_read4(void *addr) {
  RVSaveMemAccEvent(READ, (uptr)addr, *(u32*)addr, CALLERPC);
}

void __tsan_read8(void *addr) {
  RVSaveMemAccEvent(READ, (uptr)addr, *(u64*)addr, CALLERPC);
}

void __tsan_write1(void *addr, void *val) {
  RVSaveMemAccEvent(WRITE, (uptr)addr, (u8)(u64)val, CALLERPC);
}

void __tsan_write2(void *addr, void *val) {
  RVSaveMemAccEvent(WRITE, (uptr)addr, (u16)(u64)val, CALLERPC);
}

void __tsan_write4(void *addr, void *val) {
  RVSaveMemAccEvent(WRITE, (uptr)addr, (u32)(u64)val, CALLERPC);
}

void __tsan_write8(void *addr, void *val) {
  RVSaveMemAccEvent(WRITE, (uptr)addr, (u64)val, CALLERPC);
}

void __tsan_read1_pc(void *addr, void *pc) {
  RVSaveMemAccEvent(READ, (uptr)addr, *(u8*)addr, (uptr)pc);
}

void __tsan_read2_pc(void *addr, void *pc) {
  RVSaveMemAccEvent(READ, (uptr)addr, *(u16*)addr, (uptr)pc);
}

void __tsan_read4_pc(void *addr, void *pc) {
  RVSaveMemAccEvent(READ, (uptr)addr, *(u32*)addr, (uptr)pc);
}

void __tsan_read8_pc(void *addr, void *pc) {
  RVSaveMemAccEvent(READ, (uptr)addr, *(u64*)addr, (uptr)pc);
}

void __tsan_write1_pc(void *addr, void *pc, void *val) {
  RVSaveMemAccEvent(WRITE, (uptr)addr, (u8)(u64)val, (uptr)pc);
}

void __tsan_write2_pc(void *addr, void *pc, void *val) {
  RVSaveMemAccEvent(WRITE, (uptr)addr, (u16)(u64)val, (uptr)pc);
}

void __tsan_write4_pc(void *addr, void *pc, void *val) {
  RVSaveMemAccEvent(WRITE, (uptr)addr, (u32)(u64)val, (uptr)pc);
}

void __tsan_write8_pc(void *addr, void *pc, void *val) {
  RVSaveMemAccEvent(WRITE, (uptr)addr, (u64)val, (uptr)pc);
}

void __tsan_vptr_update(void **vptr_p, void *new_val) {
  CHECK_EQ(sizeof(vptr_p), 8);
  RVSaveMemAccEvent(WRITE, (uptr)vptr_p, (u64)new_val, CALLERPC);
}

void __tsan_vptr_read(void **vptr_p) {
  CHECK_EQ(sizeof(vptr_p), 8);
  RVSaveMemAccEvent(WRITE, (uptr)vptr_p, *(u64*)vptr_p, CALLERPC);
}

void __tsan_func_entry(void *pc) {
  RVSaveMetaEvent(INVOKE_METHOD, (uptr)pc - 1);
  ThreadState* thr = cur_thread();
  FuncEntry(thr, (uptr)pc);
}

void __tsan_func_exit() {
  ThreadState *thr = cur_thread();
  RVSaveMetaEvent(FINISH_METHOD, getCallerStackLocation(thr));
  FuncExit(thr);
}

void __tsan_read_range(void *addr, uptr size) {
  MemoryAccessRange(cur_thread(), CALLERPC, (uptr)addr, size, false);
}

void __tsan_write_range(void *addr, uptr size) {
  MemoryAccessRange(cur_thread(), CALLERPC, (uptr)addr, size, true);
}
