// Copyright (c) 2016 Runtime Verification, Inc. (RV-Predict team). All Rights Reserved.

#ifndef RVPREDICT_GATHERANNOTATION_H
#define RVPREDICT_GATHERANNOTATION_H

#include "llvm/IR/Module.h"
#include "llvm/Pass.h"

using namespace llvm;

namespace RVPredict {

/**
 * Gather our interrupt-related function annotations.
 *
 * There are currently two types of annotations.
 *
 * 1) ISR annotation:
 * \code __attribute__((annotate("ISR@PrioLevel")) \endcode
 * 2) enable/disable_IRQ annotation:
 * \code __attribute__((annotate("enableIRQ@PrioLevel")) \endcode
 * \code __attribute__((annotate("disableIRQ@PrioLevel")) \endcode
 */
class GatherAnnotation : public ModulePass {
  public:
    static char ID;

  private:
    /// This map stores the priority level each IRQ-disabling
    /// function disables.
    StringMap<uint8_t> DisableIRQFnMap;

    /// This map stores the priority level each IRQ-enabling
    /// function enables.
    StringMap<uint8_t> EnableIRQFnMap;

    /// This map stores the priority level of each interrupt
    /// service routine (ISR).
    StringMap<uint8_t> ISRPrioMap;

  public:
    GatherAnnotation()
        : ModulePass(ID)
        , DisableIRQFnMap()
        , EnableIRQFnMap()
        , ISRPrioMap()
    {}

  private:
    void debugDump();

  public:
    bool getDisableIRQPrioLevel(Function &F, uint8_t &prio) const;
    bool getEnableIRQPrioLevel(Function &F, uint8_t &prio) const;
    bool getISRPrioLevel(Function &F, uint8_t &prio) const;
    virtual void getAnalysisUsage(AnalysisUsage &AU) const;
    virtual bool runOnModule(Module &M);
};

}

#endif // RVPREDICT_GATHERANNOTATION_H