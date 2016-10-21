// Copyright (c) 2016 Runtime Verification, Inc. (RV-Predict team). All Rights Reserved.

#ifndef RVPREDICT_RVPREDICTINSTRUMENT_H
#define RVPREDICT_RVPREDICTINSTRUMENT_H

#include "llvm/IR/Constant.h"
#include "llvm/Pass.h"

using namespace llvm;

namespace RVPredict {

/**
 * This is the main instrumentation pass which does basically two
 * things: 1) insert calls to runtime library for (almost) every
 * memory access; and 2) insert calls at the entry/exit of each
 * ISR and IRQ-enabling/disabling functions.
 */
class RVPredictInstrument : public FunctionPass {
  public:
    static char ID;

  private:
    Constant *RtFuncDisableIRQ;
    Constant *RtFuncEnableIRQ;
    Constant *RtFuncISREntry;
    Constant *RtFuncISRExit;
    // The sizes of accesses (#bytes) are powers of two: 1, 2, 4, 8, 16.
#define NUM_ACCESS_SIZES 5
    Constant *RtFuncRead[NUM_ACCESS_SIZES];
    Constant *RtFuncWrite[NUM_ACCESS_SIZES];

  public:
    RVPredictInstrument()
        : FunctionPass(ID)
    {}

    virtual void getAnalysisUsage(AnalysisUsage &Info) const;
    virtual bool runOnFunction(Function &F);

  private:
    bool addrContainsConstData(Value *Addr);
    bool addrContainsSharedGlobalVar(Value *Addr);
    void chooseInstructionsToInstrument(
            const SmallVectorImpl<Instruction*> &Local,
            SmallVectorImpl<Instruction *> &All, const DataLayout &DL);
    int getMemoryAccessFuncIndex(Value *Addr, const DataLayout &DL);
    void initializeCallbacks(Module &M);
    bool instrumentLoadOrStore(Instruction *I, const DataLayout &DL);
};

} // namespace RVPredict

#endif // RVPREDICT_RVPREDICTINSTRUMENT_H
