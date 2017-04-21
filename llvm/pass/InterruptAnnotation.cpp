// Copyright (c) 2016 Runtime Verification, Inc. (RV-Predict team). All Rights Reserved.
/* vim: set tabstop=4 shiftwidth=4 : */

#include <set>
#include "InterruptAnnotation.h"

#include "llvm/IR/Constants.h"
#include "llvm/IR/DiagnosticInfo.h"
#include "llvm/IR/IRBuilder.h"
#include "llvm/IR/LegacyPassManager.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/Support/Format.h"
#include "llvm/Transforms/IPO/PassManagerBuilder.h"

#include "Diagnostic.h"

using namespace llvm;

namespace RVPredict {

/**
 * Print all annotated functions for debugging.
 */
void
InterruptAnnotation::debugDump()
{
    for (auto it = ISRPrioMap.begin(); it != ISRPrioMap.end(); ++it) {
        errs() << format("%s : isr@%u\n", it->first().data(),
                         it->second);
    }
    for (auto it = DisableIRQFnMap.begin(); it != DisableIRQFnMap.end(); ++it) {
        errs() << format("%s : disableIRQ@%u\n", it->first().data(),
                         it->second);
    }
    for (auto it = EnableIRQFnMap.begin(); it != EnableIRQFnMap.end(); ++it) {
        errs() << format("%s : enableIRQ@%u\n", it->first().data(),
                         it->second);
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
InterruptAnnotation::getDisableIRQPrioLevel(Function &F, uint8_t &prio) const
{
    auto it = DisableIRQFnMap.find(F.getName());
    if (it == DisableIRQFnMap.end()) {
        return false;
    }
    prio = it->second;
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
InterruptAnnotation::getEnableIRQPrioLevel(Function &F, uint8_t &prio) const
{
    auto it = EnableIRQFnMap.find(F.getName());
    if (it == EnableIRQFnMap.end()) {
        return false;
    }
    prio = it->second;
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
InterruptAnnotation::getISRPrioLevel(Function &F, uint8_t &prio) const
{
    auto it = ISRPrioMap.find(F.getName());
    if (it == ISRPrioMap.end()) {
        return  false;
    }
    prio = it->second;
    return true;
}

void
InterruptAnnotation::getAnalysisUsage(AnalysisUsage &AU) const
{
    // This module pass doesn't modify the IR.
    AU.setPreservesAll();
}

bool
InterruptAnnotation::runOnModule(Module &M)
{
    // Extract the Clang-level "annotate" attribute based on the trick below:
    // http://homes.cs.washington.edu/~bholt/posts/llvm-quick-tricks.html
    GlobalVariable *global_annotations =
            M.getNamedGlobal("llvm.global.annotations");
    if (global_annotations == NULL) {
        return false;
    }

    ConstantArray *ca = cast<ConstantArray>(global_annotations->getOperand(0));
    for (auto it = ca->op_begin(); it != ca->op_end(); ++it) {
		//
		// object_annotation = { [ Function function, ... ],
		//                       [ [ char[] annotation, ... ], ...], ... }
		//
        ConstantStruct *object_annotation = cast<ConstantStruct>(*it);
		//
		// anno_ctnr = [ annotation, ... ]
		//
		auto f = dyn_cast<Function>(object_annotation->getOperand(0)->getOperand(0));
		if (f == nullptr)
			continue;

		GlobalVariable *anno_ctnr = cast<GlobalVariable>(
			object_annotation->getOperand(1)->getOperand(0));

		StringRef annotation = cast<ConstantDataArray>(
				cast<GlobalVariable>(anno_ctnr)->getOperand(0))
				->getAsCString();
		std::pair<StringRef, StringRef> Pair = annotation.split('@');
		uint8_t prio;
		/* true on failure, ugh. */
		if (!Pair.second.getAsInteger(10, prio))
			;
		else if (Pair.first.equals("isr") || Pair.first.equals("disableIRQ") ||
		         Pair.first.equals("enableIRQ")) {
			auto first_insn = f->getEntryBlock().getFirstNonPHI();
#if 1
			auto debug_loc = first_insn->getDebugLoc();
#else
			const MDNode *subprogram = f->getSubprogram();
			auto debug_loc = (subprogram != NULL)
			    ? new DebugLoc(subprogram)
				: nullptr;
#endif
			IRBuilder<> builder(first_insn);
			std::string arg_message;
			llvm::raw_string_ostream sstr(arg_message);

			sstr << f->getName().str() << " has malformed annotation " 
			    << Pair.first << "@" << Pair.second;

#if 1
			if (debug_loc) {
				debug_loc.print(sstr);
				builder.getContext().diagnose(
					DiagnosticInfoFatalError(*f, debug_loc, sstr.str()));
			}
#endif
			builder.getContext().emitError(
				first_insn,
			    "expected {isr|disableIRQ|enableIRQ}@{decimal digits}");
		}
		StringRef fname = f->getName();
		if (Pair.first.equals("isr")) {
			ISRPrioMap.insert(std::make_pair(fname, prio));
		} else if (Pair.first.equals("disableIRQ")) {
			DisableIRQFnMap.insert(std::make_pair(fname, prio));
		} else if (Pair.first.equals("enableIRQ")) {
			EnableIRQFnMap.insert(std::make_pair(fname, prio));
		}

    }

    debugDump();
    return false;
}

char InterruptAnnotation::ID = 0;
static RegisterPass<InterruptAnnotation> _("rvpinterrupts", "RV-Predict interrupt annotation pass", false, false);

// Auto-registration of ModulePass
// https://github.com/sampsyo/llvm-pass-skeleton/issues/7
static void registerThisPass(const PassManagerBuilder &,
                             legacy::PassManagerBase &PM) {
    PM.add(new InterruptAnnotation());
}

static RegisterStandardPasses __(PassManagerBuilder::EP_ModuleOptimizerEarly,
                                registerThisPass);

static RegisterStandardPasses ___(PassManagerBuilder::EP_EnabledOnOptLevel0,
                                 registerThisPass);

} // namespace RVPredict

