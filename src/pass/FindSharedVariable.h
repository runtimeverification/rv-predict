// Copyright (c) 2016 Runtime Verification, Inc. (RV-Predict team). All Rights Reserved.

#ifndef RVPREDICT_FINDSHAREDVARIABLE_H
#define RVPREDICT_FINDSHAREDVARIABLE_H

#include <set>
#include "llvm/Pass.h"
#include "llvm/IR/GlobalVariable.h"

using namespace llvm;

namespace RVPredict {

/**
 * Identify global variables that are shared between task-level code
 * and interrupt service routines.
 *
 * Note that if global variables can be accessed indirectly via pointers,
 * it will be impossible to implement this pass to be complete (i.e., we
 * will miss to identify some shared global variables). Therefore, for the
 * sake of simplicity, we require that global variables are always accessed
 * directly using their names.
 */
class FindSharedVariable : public ModulePass {
  public:
    static char ID;

  private:
    /// Global variables (of both single-value and aggregate types) that
    /// are read in ISR code.
    std::set<GlobalVariable*> LoadedInISR;
    /// Global variables that are read in task-level code.
    std::set<GlobalVariable*> LoadedInTask;
    /// Global variables that are written in ISR code.
    std::set<GlobalVariable*> StoredInISR;
    /// Global variables that are written in task-level code.
    std::set<GlobalVariable*> StoredInTask;

    // Strictly speaking, we can further identify which portion of an
    // aggregate type variable are shared. However, we leave this for
    // future work to simplify the implementation.

  public:
    FindSharedVariable()
        : ModulePass(ID)
        , LoadedInISR()
        , LoadedInTask()
        , StoredInISR()
        , StoredInTask()
    {}

    bool isSharedVariable(GlobalVariable *G);
    virtual void getAnalysisUsage(AnalysisUsage &Info) const;
    virtual bool runOnModule(Module &M);

};

}

#endif // RVPREDICT_FINDSHAREDVARIABLE_H
