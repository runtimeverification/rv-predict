// Copyright (c) 2016, 2017 Runtime Verification, Inc.  All rights reserved.

#ifndef _RVP_GATHERANNOTATION_H_
#define _RVP_GATHERANNOTATION_H_

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
 * \code __attribute__((annotate("isr@PrioLevel")) \endcode
 * 2) enable/disable_IRQ annotation:
 * \code __attribute__((annotate("enableIRQ@PrioLevel")) \endcode
 * \code __attribute__((annotate("disableIRQ@PrioLevel")) \endcode
 */
class InterruptAnnotation : public ModulePass {
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
    InterruptAnnotation()
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

#endif // _RVP_GATHERANNOTATION_H_
