// Copyright (c) 2016, 2017 Runtime Verification, Inc.  All rights reserved.

#ifndef _RVP_GATHERANNOTATION_H_
#define _RVP_GATHERANNOTATION_H_

#include "llvm/ADT/StringSet.h"
#include "llvm/ADT/StringRef.h"
#include <utility>
#include <unordered_map>

#include "llvm/IR/Module.h"
#include "llvm/Pass.h"

using namespace llvm;

namespace RVPredict {

/**
 * Gather our interrupt/device-related function and variable annotations.
 *
 * There are currently three types of annotations.
 *
 * 1) Interrupt service routine (ISR) annotation:
 * \code __attribute__((annotate("rvp-isr-<source>@<priority>")) \endcode
 * 2) Device register annotation:
 * \code __attribute__((annotate("rvp-register")) \endcode
 * 3) Enable/disable IRQ annotation:
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
    StringSet<> registerSet;

    /// This map stores the priority level each IRQ-enabling
    /// function enables.
    StringMap<uint8_t> EnableIRQFnMap;

    /// This map stores the priority level of each interrupt
    /// service routine (ISR).
    StringMap<StringMap<uint8_t> > isrPriorityMap;

  public:
    InterruptAnnotation()
        : ModulePass(ID)
        , DisableIRQFnMap()
        , EnableIRQFnMap()
        , isrPriorityMap()
        , registerSet()
    {}

  private:
    void debugDump();

  public:
    bool isRegister(GlobalVariable &v) const;
    bool getDisableIRQPrioLevel(Function &F, uint8_t &prio) const;
    bool getEnableIRQPrioLevel(Function &F, uint8_t &prio) const;
    std::vector<uint8_t> getISRPrioLevels(Function &F) const;
    virtual void getAnalysisUsage(AnalysisUsage &AU) const;
    virtual bool runOnModule(Module &M);
};

}

#endif // _RVP_GATHERANNOTATION_H_
