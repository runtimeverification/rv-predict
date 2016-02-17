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
  RVReadInteger((uptr)addr, 1, CALLERPC);
}

void __tsan_read2(void *addr) {
  RVReadInteger((uptr)addr, 2, CALLERPC);
}

void __tsan_read4(void *addr) {
  RVReadInteger((uptr)addr, 4, CALLERPC);
}

void __tsan_read8(void *addr) {
  RVReadInteger((uptr)addr, 8, CALLERPC);
}

void __tsan_write1(void *addr, void *val) {
  RVWriteInteger((uptr)addr, 1, CALLERPC, val);
}

void __tsan_write2(void *addr, void *val) {
  RVWriteInteger((uptr)addr, 2, CALLERPC, val);
}

void __tsan_write4(void *addr, void *val) {
  RVWriteInteger((uptr)addr, 4, CALLERPC, val);
}

void __tsan_write8(void *addr, void *val) {
  RVWriteInteger((uptr)addr, 8, CALLERPC, val);
}

void __tsan_read1_pc(void *addr, void *pc) {
  RVReadInteger((uptr)addr, 1, (uptr)pc);
}

void __tsan_read2_pc(void *addr, void *pc) {
  RVReadInteger((uptr)addr, 2, (uptr)pc);
}

void __tsan_read4_pc(void *addr, void *pc) {
  RVReadInteger((uptr)addr, 4, (uptr)pc);
}

void __tsan_read8_pc(void *addr, void *pc) {
  RVReadInteger((uptr)addr, 8, (uptr)pc);
}

void __tsan_write1_pc(void *addr, void *pc, void *val) {
  RVWriteInteger((uptr)addr, 1, (uptr)pc, val);
}

void __tsan_write2_pc(void *addr, void *pc, void *val) {
  RVWriteInteger((uptr)addr, 2, (uptr)pc, val);
}

void __tsan_write4_pc(void *addr, void *pc, void *val) {
  RVWriteInteger((uptr)addr, 4, (uptr)pc, val);
}

void __tsan_write8_pc(void *addr, void *pc, void *val) {
  RVWriteInteger((uptr)addr, 8, (uptr)pc, val);
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
  RVSaveMemoryAccessRange(READ, (uptr)addr, size, CALLERPC, false);
}

void __tsan_write_range(void *addr, uptr size) {
  RVSaveMemoryAccessRange(WRITE, (uptr)addr, size, CALLERPC, false);
}
