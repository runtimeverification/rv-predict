// Copyright (c) 2016 Runtime Verification, Inc. (RV-Predict team). All Rights Reserved.

#include <set>
#include "GatherAnnotation.h"

#include "llvm/IR/Constants.h"
#include "llvm/IR/LegacyPassManager.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/Support/Format.h"
#include "llvm/Transforms/IPO/PassManagerBuilder.h"

using namespace llvm;

namespace RVPredict {

/**
 * Print all annotated functions for debugging.
 */
void
GatherAnnotation::debugDump()
{
    for (StringMap<uint8_t>::iterator It = ISRPrioMap.begin();
            It != ISRPrioMap.end(); ++It) {
        errs() << format("%s : ISR@%u\n", It->first().data(),
                         It->second);
    }
    for (StringMap<uint8_t>::iterator It = DisableIRQFnMap.begin();
            It != DisableIRQFnMap.end(); ++It) {
        errs() << format("%s : disableIRQ@%u\n", It->first().data(),
                         It->second);
    }
    for (StringMap<uint8_t>::iterator It = EnableIRQFnMap.begin();
            It != EnableIRQFnMap.end(); ++It) {
        errs() << format("%s : enableIRQ@%u\n", It->first().data(),
                         It->second);
    }
}

/**
 * Get the IRQ priority level that an IRQ-disabling function disables.
 *
 * @param F
 *      The IRQ-disabling function.
 * @param[out] prio
 *      The priority level to be disabled.
 * @return
 *      False if the given function is not annotated as an IRQ-disabling
 *      function; otherwise, True.
 */
bool
GatherAnnotation::getDisableIRQPrioLevel(Function &F, uint8_t &prio) const
{
    StringMap<uint8_t>::const_iterator It = DisableIRQFnMap.find(F.getName());
    if (It == DisableIRQFnMap.end()) {
        return false;
    }
    prio = It->second;
    return true;
}

/**
 * Get the IRQ priority level that an IRQ-enabling function enables.
 *
 * @param F
 *      The IRQ-enabling function.
 * @param[out] prio
 *      The priority level to be enabled.
 * @return
 *      False if the given function is not annotated as an IRQ-enabling
 *      function; otherwise, True.
 */
bool
GatherAnnotation::getEnableIRQPrioLevel(Function &F, uint8_t &prio) const
{
    StringMap<uint8_t>::const_iterator It = EnableIRQFnMap.find(F.getName());
    if (It == EnableIRQFnMap.end()) {
        return false;
    }
    prio = It->second;
    return true;
}

/**
 * Get the priority level of an ISR.
 *
 * @param F
 *      The ISR function.
 * @param[out] prio
 *      The priority level of the ISR.
 * @return
 *      False if the given function is not annotated as an ISR
 *      function; otherwise, True.
 */
bool
GatherAnnotation::getISRPrioLevel(Function &F, uint8_t &prio) const
{
    StringMap<uint8_t>::const_iterator It = ISRPrioMap.find(F.getName());
    if (It == ISRPrioMap.end()) {
        return  false;
    }
    prio = It->second;
    return true;
}

void
GatherAnnotation::getAnalysisUsage(AnalysisUsage &AU) const
{
    // This module pass doesn't modify the IR.
    AU.setPreservesAll();
}

bool
GatherAnnotation::runOnModule(Module &M)
{
    // Extract the Clang-level "annotate" attribute based on the trick below:
    // http://homes.cs.washington.edu/~bholt/posts/llvm-quick-tricks.html
    GlobalVariable *GlobalAnnoVar =
            M.getNamedGlobal("llvm.global.annotations");
    if (!GlobalAnnoVar) {
        return false;
    }

    ConstantArray *ca = cast<ConstantArray>(GlobalAnnoVar->getOperand(0));
    for (ConstantArray::op_iterator It = ca->op_begin();
            It != ca->op_end(); ++It) {
        ConstantStruct *ConstStruct = cast<ConstantStruct>(*It);
        if (Function *Fn = dyn_cast<Function>(
                ConstStruct->getOperand(0)->getOperand(0))) {
            GlobalVariable *GV = cast<GlobalVariable>(
                    ConstStruct->getOperand(1)->getOperand(0));
            StringRef Anno = cast<ConstantDataArray>(
                    cast<GlobalVariable>(GV)->getOperand(0))
                    ->getAsCString();
            std::pair<StringRef, StringRef> Pair = Anno.split('@');
            uint8_t Prio;
            if (!Pair.second.getAsInteger(10, Prio)) {
                StringRef FnName = Fn->getName();
                if (Pair.first.equals("isr")) {
                    ISRPrioMap.insert(std::make_pair(FnName, Prio));
                } else if (Pair.first.equals("disableIRQ")) {
                    DisableIRQFnMap.insert(std::make_pair(FnName, Prio));
                } else if (Pair.first.equals("enableIRQ")) {
                    EnableIRQFnMap.insert(std::make_pair(FnName, Prio));
                }
            }
        }

    }

    debugDump();
    return false;
}

char GatherAnnotation::ID = 0;
static RegisterPass<GatherAnnotation> X("pass0", "pass0", false, false);

// Auto-registration of ModulePass
// https://github.com/sampsyo/llvm-pass-skeleton/issues/7
static void registerThisPass(const PassManagerBuilder &,
                             legacy::PassManagerBase &PM) {
    PM.add(new GatherAnnotation());
}

static RegisterStandardPasses _(PassManagerBuilder::EP_ModuleOptimizerEarly,
                                registerThisPass);

static RegisterStandardPasses __(PassManagerBuilder::EP_EnabledOnOptLevel0,
                                 registerThisPass);

} // namespace RVPredict

