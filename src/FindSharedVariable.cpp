// Copyright (c) 2016 Runtime Verification, Inc. (RV-Predict team). All Rights Reserved.

#include "FindSharedVariable.h"
#include "GatherAnnotation.h"

#include "llvm/IR/Instructions.h"
#include "llvm/IR/InstIterator.h"
#include "llvm/IR/LegacyPassManager.h"
#include "llvm/IR/Operator.h"
#include "llvm/Transforms/IPO/PassManagerBuilder.h"

using namespace llvm;

namespace RVPredict {

void
FindSharedVariable::getAnalysisUsage(AnalysisUsage &Info) const
{
    Info.addRequired<GatherAnnotation>();
    Info.setPreservesAll();
}

bool
FindSharedVariable::isSharedVariable(GlobalVariable *G)
{
    if (StoredInISR.find(G) != StoredInISR.end()) {
        // Written in ISR and accessed in task.
        return StoredInTask.find(G) != StoredInTask.end()
            || LoadedInTask.find(G) != LoadedInTask.end();
    } else if (LoadedInISR.find(G) != LoadedInISR.end()) {
        // Read in ISR and written in task.
        return StoredInTask.find(G) != StoredInTask.end();
    }
    return false;
}

bool
FindSharedVariable::runOnModule(Module &M)
{
    for (auto &F : M) {
        uint8_t Prio;
        bool IsISR = getAnalysis<GatherAnnotation>().getISRPrioLevel(F, Prio);
        for (inst_iterator It = inst_begin(F); It != inst_end(F); ++It) {
            Instruction *I = &(*It);
            Value *Addr = nullptr;
            bool IsWrite = false;
            if (StoreInst *Store = dyn_cast<StoreInst>(I)) {
                IsWrite = true;
                Addr = Store->getPointerOperand();
            } else if (LoadInst *Load = dyn_cast<LoadInst>(I)) {
                Addr = Load->getPointerOperand();
            }
            if (!Addr) {
                continue;
            }

            if (GetElementPtrInst *GEP = dyn_cast<GetElementPtrInst>(Addr)) {
                Addr = GEP->getPointerOperand();
            } else if (GEPOperator *GEP = dyn_cast<GEPOperator>(Addr)) {
                Addr = GEP->getPointerOperand();
            }

            if (GlobalVariable *GV = dyn_cast<GlobalVariable>(Addr)) {
                if (IsWrite) {
                    if (IsISR) {
                        StoredInISR.insert(GV);
                    } else {
                        StoredInTask.insert(GV);
                    }
                } else {
                    if (IsISR) {
                        LoadedInISR.insert(GV);
                    } else {
                        LoadedInTask.insert(GV);
                    }
                }
            }
        }
    }
    return false;
}

char FindSharedVariable::ID = 0;
static RegisterPass<FindSharedVariable> _("pass1", "pass1", false, false);

// Auto-registration of ModulePass
static void registerThisPass(const PassManagerBuilder &,
                             legacy::PassManagerBase &PM) {
    PM.add(new FindSharedVariable());
}

static RegisterStandardPasses __(PassManagerBuilder::EP_ModuleOptimizerEarly,
                                 registerThisPass);

static RegisterStandardPasses ___(PassManagerBuilder::EP_EnabledOnOptLevel0,
                                  registerThisPass);

} // namespace RVPredict
