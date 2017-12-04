#ifndef _RVP_DIAGNOSTIC_H_
#define _RVP_DIAGNOSTIC_H_

#include "llvm/Transforms/Instrumentation.h"
#include "llvm/ADT/SmallSet.h"
#include "llvm/ADT/SmallString.h"
#include "llvm/ADT/SmallVector.h"
#include "llvm/ADT/Statistic.h"
#include "llvm/ADT/StringExtras.h"
#include "llvm/Analysis/CaptureTracking.h"
#include "llvm/Analysis/ValueTracking.h"
#include "llvm/IR/DataLayout.h"
#include "llvm/IR/DiagnosticInfo.h"
#include "llvm/IR/Function.h"
#include "llvm/IR/IRBuilder.h"
#include "llvm/IR/IntrinsicInst.h"
#include "llvm/IR/Intrinsics.h"
#include "llvm/IR/LLVMContext.h"
#include "llvm/IR/Metadata.h"
#include "llvm/IR/Module.h"
#include "llvm/IR/Type.h"
#include "llvm/IR/LegacyPassManager.h"
#include "llvm/Support/CommandLine.h"
#include "llvm/Support/Debug.h"
#include "llvm/Support/MathExtras.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/Transforms/Utils/BasicBlockUtils.h"
#include "llvm/Transforms/Utils/ModuleUtils.h"
#include "llvm/Transforms/IPO/PassManagerBuilder.h"

using namespace llvm;

namespace RVPredict {

	class DiagnosticInfoFatalError : public DiagnosticInfoOptimizationBase {
		std::string msg;
	public:
		DiagnosticInfoFatalError(
		    StringRef pass_name, const Function &Fn, const DebugLoc &DLoc,
		    const Twine &Msg) :
		DiagnosticInfoOptimizationBase(DK_OptimizationFailure, DS_Error,
		    pass_name.str().c_str(), "RvPredictInternal", Fn, DLoc) {
			msg = Msg.getSingleStringRef().str();
		}
		virtual std::string getMsg(void) const {
			return msg;
		}
		virtual bool isEnabled(void) const {
			return true;
		}
	};
	class DiagnosticInfoRemark : public DiagnosticInfoOptimizationBase {
		std::string msg;
	public:
		DiagnosticInfoRemark(
		    StringRef pass_name, const Function &Fn, const DebugLoc &DLoc,
		    const Twine &Msg) :
		DiagnosticInfoOptimizationBase(DK_OptimizationRemark, DS_Remark,
		    pass_name.str().c_str(), "RvPredictInternal", Fn, DLoc) {
			msg = Msg.getSingleStringRef().str();
		}
		virtual std::string getMsg(void) const {
			return msg;
		}
		virtual bool isEnabled(void) const {
			return true;
		}
	};

}

#endif /* _RVP_DIAGNOSTIC_H_ */
