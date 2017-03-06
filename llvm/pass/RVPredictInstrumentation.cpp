/*
 * Copyright (c) 2017 Runtime Verification, Inc.  All rights reserved.
 */
//===-- RVPredictInstrument.cpp - race detector -------------------------------===//
//
//                     The LLVM Compiler Infrastructure
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
//
// This file is a part of RVPredictInstrument, a race detector.
//
// The instrumentation phase is quite simple:
//   - Insert calls to run-time library before every memory access.
//      - Optimizations may apply to avoid instrumenting some of the accesses.
//   - Insert calls at function entry/exit.
//   - More stuff here.
// The rest is handled by the run-time library.
//===----------------------------------------------------------------------===//

#include "llvm/Transforms/Instrumentation.h"
#include "llvm/ADT/SmallSet.h"
#include "llvm/ADT/SmallString.h"
#include "llvm/ADT/SmallVector.h"
#include "llvm/ADT/Statistic.h"
#include "llvm/ADT/StringExtras.h"
#include "llvm/Analysis/CaptureTracking.h"
#include "llvm/Analysis/ValueTracking.h"
#include "llvm/IR/DataLayout.h"
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

#define DEBUG_TYPE "rvpinstrument"

static cl::opt<bool>  ClInstrumentMemoryAccesses(
    "rvpredict-instrument-memory-accesses", cl::init(true),
    cl::desc("Instrument memory accesses"), cl::Hidden);
static cl::opt<bool>  ClInstrumentFuncEntryExit(
    "rvpredict-instrument-func-entry-exit", cl::init(true),
    cl::desc("Instrument function entry and exit"), cl::Hidden);
static cl::opt<bool>  ClInstrumentAtomics(
    "rvpredict-instrument-atomics", cl::init(true),
    cl::desc("Instrument atomics"), cl::Hidden);
static cl::opt<bool>  ClInstrumentMemIntrinsics(
    "rvpredict-instrument-memintrinsics", cl::init(true),
    cl::desc("Instrument memintrinsics (memset/memcpy/memmove)"), cl::Hidden);

STATISTIC(NumInstrumentedReads, "Number of instrumented reads");
STATISTIC(NumInstrumentedWrites, "Number of instrumented writes");
STATISTIC(NumOmittedReadsBeforeWrite,
          "Number of reads ignored due to following writes");
STATISTIC(NumAccessesWithBadSize, "Number of accesses with bad size");
STATISTIC(NumInstrumentedVtableWrites, "Number of vtable ptr writes");
STATISTIC(NumInstrumentedVtableReads, "Number of vtable ptr reads");
STATISTIC(NumOmittedReadsFromConstantGlobals,
          "Number of reads from constant globals");
STATISTIC(NumOmittedReadsFromVtable, "Number of vtable reads");
STATISTIC(NumOmittedNonCaptured, "Number of accesses ignored due to capturing");

static const char *const kRVPredictModuleCtorName = "rvpredict.module_ctor";
static const char *const kRVPredictInitName = "__rvpredict_init";

namespace RVPredict {

/// RVPredictInstrument: instrument the code in module to find races.
class RVPredictInstrument : public FunctionPass {
public:
  RVPredictInstrument() : FunctionPass(ID) {}
  const char *getPassName() const override;
  bool runOnFunction(Function &F) override;
  bool doInitialization(Module &M) override;
  GlobalVariable *createOrdering(IRBuilder<> *, AtomicOrdering);
  static char ID;  // Pass identification, replacement for typeid.

 private:
  void initializeCallbacks(Module &M);
  bool instrumentLoadOrStore(Instruction *I, const DataLayout &DL);
  bool instrumentAtomic(Instruction *I, const DataLayout &DL);
  bool instrumentMemIntrinsic(Instruction *I);
  void chooseInstructionsToInstrument(SmallVectorImpl<Instruction *> &Local,
                                      SmallVectorImpl<Instruction *> &All,
                                      const DataLayout &DL);
  bool addrPointsToConstantData(Value *Addr);
  int getMemoryAccessFuncIndex(Value *Addr, const DataLayout &DL);

  Type *intptr_type;
  // Callbacks to run-time library are computed in doInitialization.
  Function *fnenter;
  Function *fnexit;
  // Accesses sizes are powers of two: 1, 2, 4, 8, 16.
  static const size_t kNumberOfAccessSizes = 5;
  Function *load[kNumberOfAccessSizes];
  Function *store[kNumberOfAccessSizes];
  Function *unaligned_load[kNumberOfAccessSizes];
  Function *unaligned_store[kNumberOfAccessSizes];
  Function *atomic_load[kNumberOfAccessSizes];
  Function *atomic_store[kNumberOfAccessSizes];
  Function *atomic_rmw[AtomicRMWInst::LAST_BINOP + 1][kNumberOfAccessSizes];
  Function *atomic_cas[kNumberOfAccessSizes];
  Function *atomic_thread_fence;
  Function *atomic_signal_fence;
  Function *vptr_update;
  Function *vptr_load;
  Function *memmovefn, *memcpyfn, *memsetfn;
  Function *ctorfn;
  GlobalVariable *order_relaxed;
  GlobalVariable *order_acquire;
  GlobalVariable *order_release;
  GlobalVariable *order_acq_rel;
  GlobalVariable *order_seq_cst;

};

char RVPredictInstrument::ID = 0;
static RegisterPass<RVPredictInstrument> _("rvpinstrument",
        "RV-Predict instrumentation pass", false, false);

// Registering this function pass at extension point `EP_EarlyAsPossible`
// results in a segmentation fault at runtime because the LLVM PassManager
// somehow schedules the module passes this function pass depends on
// to run too early (even before fields in the `Module` are properly
// initialized).  To workaround this problem, specify this pass to have
// the same extension points as the module passes it depends on.

static void registerThisPass(const PassManagerBuilder &,
    legacy::PassManagerBase &PM) {
        PM.add(new RVPredictInstrument());
}

static RegisterStandardPasses __(PassManagerBuilder::EP_ModuleOptimizerEarly,
                                 registerThisPass);

static RegisterStandardPasses ___(PassManagerBuilder::EP_EnabledOnOptLevel0,
                                  registerThisPass);



const char *
RVPredictInstrument::getPassName() const
{
	return "RVPredictInstrument";
}

void
RVPredictInstrument::initializeCallbacks(Module &m)
{
	IRBuilder<> builder(m.getContext());
	auto memory_order_type = builder.getInt32Ty();
	auto void_type = builder.getVoidTy();
	auto int8_ptr_type = builder.getInt8PtrTy();

	/* void __rvpredict_func_entry(void *);
	 * void __rvpredict_func_exit(void *);
	 */
	fnenter = checkSanitizerInterfaceFunction(
	    m.getOrInsertFunction( "__rvpredict_func_entry", void_type,
	        int8_ptr_type, nullptr));
	fnexit = checkSanitizerInterfaceFunction(
	    m.getOrInsertFunction("__rvpredict_func_exit", void_type,
	        int8_ptr_type, nullptr));

	for (size_t i = 0; i < kNumberOfAccessSizes; ++i) {
		const size_t byte_size = 1 << i;
		const size_t bit_size = byte_size * 8;
		auto byte_size_str = itostr(byte_size);
		Type *type = Type::getIntNTy(m.getContext(), bit_size);
		Type *ptr_type = type->getPointerTo();

		SmallString<32> load_name("__rvpredict_load" + byte_size_str);
		load[i] = checkSanitizerInterfaceFunction(
		    m.getOrInsertFunction(load_name,
			void_type, int8_ptr_type, nullptr));

		/* For m the width of the access in bytes, and n the width
		 * in bits, the instrumentation may insert calls to any of
		 * these functions: 
		 *
		 * void __rvpredict_store{m}(uint{n}_t *addr, uint{n}_t val);
		 *
		 * void __rvpredict_load{m}(uint{n}_t *addr, uint{n}_t val);
		 *
		 * void __rvpredict_unaligned_store{m}(uint{n}_t *addr,
		 *     uint{n}_t val);
		 *
		 * void __rvpredict_unaligned_load{m}(uint{n}_t *addr,
		 *     uint{n}_t val);
		 *
		 * void __rvpredict_atomic_store{m}(uint{n}_t *addr,
		 *     uint{n}_t val, int32_t memory_order);
		 *
		 * void __rvpredict_atomic_load{m}(uint{n}_t *addr,
		 *     uint{n}_t val, int32_t memory_order);
		 *
		 * uint{n}_t __rvpredict_atomic_cas{m}(uint{n}_t *addr,
		 *     uint{n}_t expected, uint{n}_t desired,
		 *     int32_t memory_order_success,
		 *     int32_t memory_order_failure);
		 *
		 * uint{n}_t __rvpredict_atomic_exchange{m}(uint{n}_t *addr,
		 *     uint{n}_t val, int32_t memory_order);
		 *
		 * void __rvpredict_atomic_fetch_{add,
		 *     sub, and, or, xor, nand}{m}(uint{n}_t *addr,
		 *     uint{n}_t oval, uint{n}_t arg, int32_t memory_order);
		 */
		SmallString<32> store_name("__rvpredict_store" + byte_size_str);
		store[i] = checkSanitizerInterfaceFunction(
		    m.getOrInsertFunction(store_name,
		        void_type, ptr_type, type, nullptr));

		SmallString<64> unaligned_load_name(
		    "__rvpredict_unaligned_load" + byte_size_str);
		unaligned_load[i] = checkSanitizerInterfaceFunction(
		    m.getOrInsertFunction(unaligned_load_name,
		        void_type, ptr_type, type, nullptr));

		SmallString<64> unaligned_store_name(
		    "__rvpredict_unaligned_store" + byte_size_str);
		unaligned_store[i] = checkSanitizerInterfaceFunction(
		    m.getOrInsertFunction(unaligned_store_name,
		        void_type, ptr_type, type, nullptr));

		SmallString<32> atomic_load_name("__rvpredict_atomic_load" +
		    byte_size_str);
		atomic_load[i] = checkSanitizerInterfaceFunction(
		    m.getOrInsertFunction(atomic_load_name, void_type,
		    ptr_type, type, memory_order_type, nullptr));

		SmallString<32> atomic_store_name("__rvpredict_atomic_store" +
		    byte_size_str);
		atomic_store[i] = checkSanitizerInterfaceFunction(
		    m.getOrInsertFunction(atomic_store_name,
		        void_type, ptr_type, type, memory_order_type, nullptr));

		for (int op = AtomicRMWInst::FIRST_BINOP;
		     op <= AtomicRMWInst::LAST_BINOP;
		     op++) {
			atomic_rmw[op][i] = nullptr;
			const char *prefix = nullptr;
			if (op == AtomicRMWInst::Xchg)
				prefix = "__rvpredict_atomic_exchange";
			else if (op == AtomicRMWInst::Add)
				prefix = "__rvpredict_atomic_fetch_add";
			else if (op == AtomicRMWInst::Sub)
				prefix = "__rvpredict_atomic_fetch_sub";
			else if (op == AtomicRMWInst::And)
				prefix = "__rvpredict_atomic_fetch_and";
			else if (op == AtomicRMWInst::Or)
				prefix = "__rvpredict_atomic_fetch_or";
			else if (op == AtomicRMWInst::Xor)
				prefix = "__rvpredict_atomic_fetch_xor";
			else if (op == AtomicRMWInst::Nand)
				prefix = "__rvpredict_atomic_fetch_nand";
			else
				continue;
			SmallString<32> rmw_name(prefix + byte_size_str);
			atomic_rmw[op][i] =
			    checkSanitizerInterfaceFunction(
			        m.getOrInsertFunction(rmw_name, void_type,
				    ptr_type, type, type, memory_order_type,
				    nullptr));
		}

		SmallString<32> atomic_cas_name("__rvpredict_atomic_cas" +
		    byte_size_str);
		atomic_cas[i] = checkSanitizerInterfaceFunction(
		    m.getOrInsertFunction(atomic_cas_name, type, ptr_type,
		        type, type, memory_order_type, memory_order_type,
			nullptr));
	}
	vptr_update = checkSanitizerInterfaceFunction(
	    m.getOrInsertFunction("__rvpredict_vptr_update",
	        void_type, int8_ptr_type,
		int8_ptr_type, nullptr));
	vptr_load = checkSanitizerInterfaceFunction(
	    m.getOrInsertFunction("__rvpredict_vptr_load",
	        void_type, int8_ptr_type, nullptr));
	atomic_thread_fence = checkSanitizerInterfaceFunction(
	    m.getOrInsertFunction("__rvpredict_atomic_thread_fence",
	        void_type, memory_order_type, nullptr));
	atomic_signal_fence = checkSanitizerInterfaceFunction(
	    m.getOrInsertFunction("__rvpredict_atomic_signal_fence",
	        void_type, memory_order_type, nullptr));

	memmovefn = checkSanitizerInterfaceFunction(
	    m.getOrInsertFunction("memmove",
	        int8_ptr_type, int8_ptr_type,
		int8_ptr_type, intptr_type, nullptr));
	memcpyfn = checkSanitizerInterfaceFunction(
	    m.getOrInsertFunction("memcpy",
	        int8_ptr_type, int8_ptr_type,
		int8_ptr_type, intptr_type, nullptr));
	memsetfn = checkSanitizerInterfaceFunction(
	    m.getOrInsertFunction("memset",
	        int8_ptr_type, int8_ptr_type,
		builder.getInt32Ty(), intptr_type, nullptr));
}

bool RVPredictInstrument::doInitialization(Module &M) {
  const DataLayout &DL = M.getDataLayout();
  intptr_type = DL.getIntPtrType(M.getContext());
  std::tie(ctorfn, std::ignore) = createSanitizerCtorAndInitFunctions(
      M, kRVPredictModuleCtorName, kRVPredictInitName, /*InitArgTypes=*/{},
      /*InitArgs=*/{});

  IRBuilder<> builder(M.getContext());
  order_relaxed = new GlobalVariable(M, builder.getInt32Ty(), true,
      GlobalValue::ExternalLinkage, 0, "__rvpredict_memory_order_relaxed");
  order_acquire = new GlobalVariable(M, builder.getInt32Ty(), true,
      GlobalValue::ExternalLinkage, 0, "__rvpredict_memory_order_acquire");
  order_release = new GlobalVariable(M, builder.getInt32Ty(), true,
      GlobalValue::ExternalLinkage, 0, "__rvpredict_memory_order_release");
  order_acq_rel = new GlobalVariable(M, builder.getInt32Ty(), true,
      GlobalValue::ExternalLinkage, 0, "__rvpredict_memory_order_acq_rel");
  order_seq_cst = new GlobalVariable(M, builder.getInt32Ty(), true,
      GlobalValue::ExternalLinkage, 0, "__rvpredict_memory_order_seq_cst");
  order_relaxed->setAlignment(4);
  order_acquire->setAlignment(4);
  order_release->setAlignment(4);
  order_acq_rel->setAlignment(4);
  order_seq_cst->setAlignment(4);

  appendToGlobalCtors(M, ctorfn, 0);

  return true;
}

static bool isVtableAccess(Instruction *I) {
  if (MDNode *Tag = I->getMetadata(LLVMContext::MD_tbaa))
    return Tag->isTBAAVtableAccess();
  return false;
}

bool RVPredictInstrument::addrPointsToConstantData(Value *Addr) {
  // If this is a GEP, just analyze its pointer operand.
  if (GetElementPtrInst *GEP = dyn_cast<GetElementPtrInst>(Addr))
    Addr = GEP->getPointerOperand();

  if (GlobalVariable *GV = dyn_cast<GlobalVariable>(Addr)) {
    if (GV->isConstant()) {
      // Reads from constant globals can not race with any writes.
      NumOmittedReadsFromConstantGlobals++;
      return true;
    }
  } else if (LoadInst *L = dyn_cast<LoadInst>(Addr)) {
    if (isVtableAccess(L)) {
      // Reads from a vtable pointer can not race with any writes.
      NumOmittedReadsFromVtable++;
      return true;
    }
  }
  return false;
}

// Instrumenting some of the accesses may be proven redundant.
// Currently handled:
//  - read-before-write (within same BB, no calls between)
//  - not captured variables
//
// We do not handle some of the patterns that should not survive
// after the classic compiler optimizations.
// E.g. two reads from the same temp should be eliminated by CSE,
// two writes should be eliminated by DSE, etc.
//
// 'Local' is a vector of insns within the same BB (no calls between).
// 'All' is a vector of insns that will be instrumented.
void RVPredictInstrument::chooseInstructionsToInstrument(
    SmallVectorImpl<Instruction *> &Local, SmallVectorImpl<Instruction *> &All,
    const DataLayout &DL) {
  //RV Predict: Commenting out WriteTargets as we want to instrument all reads
  //SmallSet<Value*, 8> WriteTargets;
  // Iterate from the end.
  for (SmallVectorImpl<Instruction*>::reverse_iterator It = Local.rbegin(),
       E = Local.rend(); It != E; ++It) {
    Instruction *I = *It;
    if (StoreInst *Store = dyn_cast<StoreInst>(I)) {
      // WriteTargets.insert(Store->getPointerOperand());
    } else {
      LoadInst *Load = cast<LoadInst>(I);
      Value *Addr = Load->getPointerOperand();
      //if (WriteTargets.count(Addr)) {
      //  // We will write to this temp, so no reason to analyze the read.
      //  NumOmittedReadsBeforeWrite++;
      //  continue;
      //}
      if (addrPointsToConstantData(Addr)) {
        // Addr points to some constant data -- it can not race with any writes.
        continue;
      }
    }
    Value *Addr = isa<StoreInst>(*I)
        ? cast<StoreInst>(I)->getPointerOperand()
        : cast<LoadInst>(I)->getPointerOperand();
    if (isa<AllocaInst>(GetUnderlyingObject(Addr, DL)) &&
        !PointerMayBeCaptured(Addr, true, true)) {
      // The variable is addressable but not captured, so it cannot be
      // referenced from a different thread and participate in a data race
      // (see llvm/Analysis/CaptureTracking.h for details).
      NumOmittedNonCaptured++;
      continue;
    }
    All.push_back(I);
  }
  Local.clear();
}

static bool isAtomic(Instruction *I) {
  if (LoadInst *LI = dyn_cast<LoadInst>(I))
    return LI->isAtomic() && LI->getSynchScope() == CrossThread;
  if (StoreInst *SI = dyn_cast<StoreInst>(I))
    return SI->isAtomic() && SI->getSynchScope() == CrossThread;
  if (isa<AtomicRMWInst>(I))
    return true;
  if (isa<AtomicCmpXchgInst>(I))
    return true;
  if (isa<FenceInst>(I))
    return true;
  return false;
}

bool
RVPredictInstrument::runOnFunction(Function &F)
{
        // This is required to prevent instrumenting call to
        // __rvpredict_init from within the module constructor.
	if (&F == ctorfn)
		return false;
	Module &m = *F.getParent();
	initializeCallbacks(m);
	SmallVector<Instruction *, 8> RetVec;
	SmallVector<Instruction *, 8> AllLoadsAndStores;
	SmallVector<Instruction *, 8> LocalLoadsAndStores;
	SmallVector<Instruction *, 8> AtomicAccesses;
	SmallVector<Instruction *, 8> MemIntrinCalls;
	SmallVector<CallInst *, 8> fncalls;
	bool didInstrument = false;
	bool hasCalls = false;
	const DataLayout &DL = m.getDataLayout();
	struct {
		Function *exitfn, *createfn, *joinfn,
		    *lockfn, *trylockfn, *unlockfn;

		Value *createstubfn, *joinstubfn, *exitstubfn,
		    *lockstubfn, *trylockstubfn, *unlockstubfn;
	} pthreads = {
		  .exitfn = m.getFunction("pthread_exit")
		, .createfn = m.getFunction("pthread_create")
		, .joinfn = m.getFunction("pthread_join")
		, .lockfn = m.getFunction("pthread_mutex_lock")
		, .trylockfn = m.getFunction("pthread_mutex_trylock")
		, .unlockfn = m.getFunction("pthread_mutex_unlock")
		, .createstubfn = (pthreads.createfn == nullptr)
		    ? nullptr
		    : m.getOrInsertFunction("__rvpredict_pthread_create",
		        pthreads.createfn->getFunctionType())
		, .joinstubfn = (pthreads.joinfn == nullptr)
		    ? nullptr
		    : m.getOrInsertFunction("__rvpredict_pthread_join",
					    pthreads.joinfn->getFunctionType())
		, .exitstubfn = (pthreads.exitfn == nullptr)
		    ? nullptr
		    : m.getOrInsertFunction("__rvpredict_pthread_exit",
					    pthreads.exitfn->getFunctionType())
		, .lockstubfn = (pthreads.lockfn == nullptr)
		    ? nullptr
		    : m.getOrInsertFunction("__rvpredict_pthread_mutex_lock",
					    pthreads.lockfn->getFunctionType())
		, .trylockstubfn = (pthreads.trylockfn == nullptr)
		    ? nullptr
		    : m.getOrInsertFunction("__rvpredict_pthread_mutex_trylock",
			pthreads.trylockfn->getFunctionType())
		, .unlockstubfn = (pthreads.unlockfn == nullptr)
		    ? nullptr
		    : m.getOrInsertFunction("__rvpredict_pthread_mutex_unlock",
			pthreads.unlockfn->getFunctionType())
	};

	// Traverse all instructions, collect loads/stores/returns, check for calls.
	for (auto &bblock : F) {
		for (auto &insn : bblock) {
			if (isAtomic(&insn))
				AtomicAccesses.push_back(&insn);
			else if (isa<LoadInst>(insn) || isa<StoreInst>(insn))
				LocalLoadsAndStores.push_back(&insn);
			else if (isa<ReturnInst>(insn))
				RetVec.push_back(&insn);
			else if (isa<CallInst>(insn) || isa<InvokeInst>(insn)) {
				/*
				 * TBD
				 *
                                 * It is probably necessary to intercept
                                 * calls via InvokeInst, too.  Any other
                                 * instructions?
				 */
				if (isa<MemIntrinsic>(insn))
					MemIntrinCalls.push_back(&insn);
				else if (auto ci = dyn_cast<CallInst>(&insn)) {
                                        /* Cannot mutate the instructions
                                         * while iterating them.
                                         * Put them on a queue for
                                         * processing, later.
					 */
					fncalls.push_back(ci);
				}

				hasCalls = true;
				chooseInstructionsToInstrument(
				    LocalLoadsAndStores, AllLoadsAndStores, DL);
			}
		}
		chooseInstructionsToInstrument(LocalLoadsAndStores,
		    AllLoadsAndStores, DL);
	}

	for (auto ci : fncalls) {
		/* TBD:
		 *
		 * If the call is indirect, then the
		 * CallInst will have no Function to
		 * compare with.  It may nevertheless have
		 * a FunctionType, I think.  In that case,
		 * we can compare its FunctionType with
		 * the type of each function we want to
		 * intercept, and insert a direct call to
		 * a function that compares the run-time
		 * call target with functions that we
		 * intercept and intercepts on a match.
		 */
		Function *calledfn = ci->getCalledFunction();

		Value *replacefn = nullptr;
		if (calledfn == nullptr) {
			/* do nothing */
		} else if (calledfn == pthreads.createfn) {
			replacefn = pthreads.createstubfn;
		} else if (calledfn == pthreads.exitfn) {
			replacefn = pthreads.exitstubfn;
		} else if (calledfn == pthreads.joinfn) {
			replacefn = pthreads.joinstubfn;
		} else if (calledfn == pthreads.lockfn) {
			replacefn = pthreads.lockstubfn;
		} else if (calledfn == pthreads.trylockfn) {
			replacefn = pthreads.trylockstubfn;
		} else if (calledfn == pthreads.unlockfn) {
			replacefn = pthreads.unlockstubfn;
		}
		if (replacefn != nullptr) {
			auto nargs = ci->getNumArgOperands();
			Value **args = new Value *[nargs];
			for (auto i = 0; i < nargs; i++) {
				args[i] = ci->getArgOperand(i);
			}
			auto replace_insn = CallInst::Create(
			    replacefn,
			    ArrayRef<Value *>(args, nargs));
			delete args;
			ReplaceInstWithInst(ci, replace_insn);
		}
	}

	// We have collected all loads and stores.
	// FIXME: many of these accesses do not need to be checked for races
	// (e.g. variables that do not escape, etc).

        // Instrument memory accesses only if we want to report bugs in
        // the function.
	if (ClInstrumentMemoryAccesses) {
		for (auto insn : AllLoadsAndStores) {
			didInstrument |= instrumentLoadOrStore(insn, DL);
		}
	}

	// Instrument atomic memory accesses in any case (they can be used to
	// implement synchronization).
	if (ClInstrumentAtomics) {
		for (auto insn : AtomicAccesses) {
			didInstrument |= instrumentAtomic(insn, DL);
		}
	}

	if (ClInstrumentMemIntrinsics) {
		for (auto insn : MemIntrinCalls) {
			didInstrument |= instrumentMemIntrinsic(insn);
		}
	}

        // Instrument function entry/exit points if there were
        // instrumented accesses.
	if ((didInstrument || hasCalls) && ClInstrumentFuncEntryExit) {
		IRBuilder<> IRB(F.getEntryBlock().getFirstNonPHI());
		Value *ReturnAddress = IRB.CreateCall(
		Intrinsic::getDeclaration(F.getParent(),
		    Intrinsic::returnaddress),
		IRB.getInt32(0));
		IRB.CreateCall(fnenter, ReturnAddress);
		for (auto RetInst : RetVec) {
			IRBuilder<> IRBRet(RetInst);
			IRBRet.CreateCall(fnexit, ReturnAddress);
		}
		didInstrument = true;
	}
	return didInstrument;
}

bool
RVPredictInstrument::instrumentLoadOrStore(Instruction *I,
                                            const DataLayout &DL)
{
  IRBuilder<> IRB(I);
  bool IsWrite = isa<StoreInst>(*I);
  Value *Addr;
  Value *Val;
  if (IsWrite) {
      StoreInst *Store = cast<StoreInst>(I);
      Addr = Store->getPointerOperand();
      Val = Store->getValueOperand();
  } else {
      LoadInst *Load = cast<LoadInst>(I);
      Addr = Load->getPointerOperand();
      Val = Load;
  }
  Type *OrigTy = cast<PointerType>(Addr->getType())->getElementType();
  const uint32_t TypeSize = DL.getTypeStoreSizeInBits(OrigTy);
  Type *Ty = Type::getIntNTy(IRB.getContext(), TypeSize);
  Type *PtrTy = Ty->getPointerTo();
  int Idx = getMemoryAccessFuncIndex(Addr, DL);
  if (Idx < 0)
    return false;
  if (IsWrite) {
    // Val may be a vector type if we are storing several vptrs at once.
    // In this case, just take the first element of the vector since this is
    // enough to find vptr races.
    if (isa<VectorType>(Val->getType()))
      Val = IRB.CreateExtractElement(
          Val, ConstantInt::get(IRB.getInt32Ty(), 0));
    if (Val->getType()->isFloatingPointTy()) {
      Value* alloca = IRB.CreateAlloca(Val->getType());
      StoreInst* tmpstore = IRB.CreateStore(Val, alloca);
      Value* tmpload = IRB.CreateBitCast(tmpstore->getPointerOperand(), PtrTy);
      Val = IRB.CreateLoad(tmpload);
    }
#if 0
    /* Not sure what this int-to-pointer conversion was for. --dyoung */
    if (Val->getType()->isIntegerTy())
      Val = IRB.CreateIntToPtr(Val, IRB.getInt8PtrTy());
#endif
    if (isVtableAccess(I)) {
      DEBUG(dbgs() << "  VPTR : " << *I << "\n");
      // Call vptr_update.
      IRB.CreateCall(vptr_update,
                     {IRB.CreatePointerCast(Addr, IRB.getInt8PtrTy()),
                      IRB.CreatePointerCast(Val, IRB.getInt8PtrTy())});
      NumInstrumentedVtableWrites++;
      return true;
    }
  }
  if (!IsWrite && isVtableAccess(I)) {
    IRB.CreateCall(vptr_load,
                   IRB.CreatePointerCast(Addr, IRB.getInt8PtrTy()));
    NumInstrumentedVtableReads++;
    return true;
  }
  const unsigned Alignment = IsWrite
      ? cast<StoreInst>(I)->getAlignment()
      : cast<LoadInst>(I)->getAlignment();
  Value *OnAccessFunc = nullptr;
  if (Alignment == 0 || Alignment >= 8 || (Alignment % (TypeSize / 8)) == 0)
    OnAccessFunc = IsWrite ? store[Idx] : load[Idx];
  else
    OnAccessFunc = IsWrite ? unaligned_store[Idx] : unaligned_load[Idx];

  // Cast the address pointer to intN_t * (and insert the cast instruction).
  Addr = IRB.CreatePointerCast(Addr, PtrTy);
  /* Cast the value to an unsigned integer of the same size.
   * XXX Sometimes this produces a redundant cast from typeof(self) to
   * typeof(self).
   */
  Instruction *CastInsn = CastInst::CreateBitOrPointerCast(Val, Ty);

  if (IsWrite) {
      // Insert the call before the store instruction.
      CastInsn->insertBefore(I);
      IRB.CreateCall(OnAccessFunc, {Addr, CastInsn});
  } else {
      // Insert the call after the load instruction.
      CastInsn->insertAfter(I);
      CallInst::Create(OnAccessFunc, {Addr, CastInsn})
              ->insertAfter(CastInsn);
  }
  /*
  if (IsWrite) {
    IRB.CreateCall(OnAccessFunc, {Addr, StoredValue});
  } else {
//    IRB.CreateCall(OnAccessFunc, IRB.CreatePointerCast(Addr, IRB.getInt8PtrTy()));
    CallInst::Create(OnAccessFunc, {Addr, I})
            ->insertAfter(I);
  }
  */
  if (IsWrite) NumInstrumentedWrites++;
  else         NumInstrumentedReads++;
  return true;
}

GlobalVariable *
RVPredictInstrument::createOrdering(IRBuilder<> *IRB, AtomicOrdering ord)
{
	switch (ord) {
	case AtomicOrdering::NotAtomic:
		llvm_unreachable("unexpected atomic ordering!");
	case AtomicOrdering::Unordered:             
	case AtomicOrdering::Monotonic:
		return order_relaxed;
	case AtomicOrdering::Acquire:
		return order_acquire;
	case AtomicOrdering::Release:
		return order_release;
	case AtomicOrdering::AcquireRelease:
		return order_acq_rel;
	case AtomicOrdering::SequentiallyConsistent:
		return order_seq_cst;
	}
}

// If a memset intrinsic gets inlined by the code gen, we will miss races on it.
// So, we either need to ensure the intrinsic is not inlined, or instrument it.
// We do not instrument memset/memmove/memcpy intrinsics (too complicated),
// instead we simply replace them with regular function calls, which are then
// intercepted by the run-time.
// Since rvpredict is running after everyone else, the calls should not be
// replaced back with intrinsics. If that becomes wrong at some point,
// we will need to call e.g. __rvpredict_memset to avoid the intrinsics.
bool RVPredictInstrument::instrumentMemIntrinsic(Instruction *I) {
  IRBuilder<> IRB(I);
  if (MemSetInst *M = dyn_cast<MemSetInst>(I)) {
    IRB.CreateCall(
        memsetfn,
        {IRB.CreatePointerCast(M->getArgOperand(0), IRB.getInt8PtrTy()),
         IRB.CreateIntCast(M->getArgOperand(1), IRB.getInt32Ty(), false),
         IRB.CreateIntCast(M->getArgOperand(2), intptr_type, false)});
    I->eraseFromParent();
  } else if (MemTransferInst *M = dyn_cast<MemTransferInst>(I)) {
    IRB.CreateCall(
        isa<MemCpyInst>(M) ? memcpyfn : memmovefn,
        {IRB.CreatePointerCast(M->getArgOperand(0), IRB.getInt8PtrTy()),
         IRB.CreatePointerCast(M->getArgOperand(1), IRB.getInt8PtrTy()),
         IRB.CreateIntCast(M->getArgOperand(2), intptr_type, false)});
    I->eraseFromParent();
  }
  return false;
}

// Both llvm and RVPredictInstrument atomic operations are based on C++11/C1x
// standards.  For background see C++11 standard.  A slightly older, publicly
// available draft of the standard (not entirely up-to-date, but close enough
// for casual browsing) is available here:
// http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2011/n3242.pdf
// The following page contains more background information:
// http://www.hpl.hp.com/personal/Hans_Boehm/c++mm/

bool
RVPredictInstrument::instrumentAtomic(Instruction *insn, const DataLayout &dl)
{
	IRBuilder<> builder(insn);

	if (auto li = dyn_cast<LoadInst>(insn)) {
		Value *addr = li->getPointerOperand();
		int idx = getMemoryAccessFuncIndex(addr, dl);
		if (idx < 0)
			return false;
		const size_t ByteSize = 1 << idx;
		const size_t BitSize = ByteSize * 8;
		Type *Ty = Type::getIntNTy(builder.getContext(), BitSize);
		Type *PtrTy = Ty->getPointerTo();
		Value *args[] = {
		    builder.CreatePointerCast(addr, PtrTy),
		    li,
		    createOrdering(&builder, li->getOrdering())};
		CallInst *ci = CallInst::Create(atomic_load[idx], args);
		ci->insertAfter(insn);
	} else if (auto si = dyn_cast<StoreInst>(insn)) {
		Value *addr = si->getPointerOperand();
		int idx = getMemoryAccessFuncIndex(addr, dl);
		if (idx < 0)
			return false;
		const size_t ByteSize = 1 << idx;
		const size_t BitSize = ByteSize * 8;
		Type *Ty = Type::getIntNTy(builder.getContext(), BitSize);
		Type *PtrTy = Ty->getPointerTo();
		Value *args[] = {
		    builder.CreatePointerCast(addr, PtrTy),
		    builder.CreateIntCast(si->getValueOperand(), Ty, false),
		    createOrdering(&builder, si->getOrdering())};
		CallInst *ci = CallInst::Create(atomic_store[idx], args);
		ci->insertBefore(insn);
	} else if (auto rmwi = dyn_cast<AtomicRMWInst>(insn)) {
		Value *addr = rmwi->getPointerOperand();
		int idx = getMemoryAccessFuncIndex(addr, dl);
		if (idx < 0)
			return false;
		Function *F = atomic_rmw[rmwi->getOperation()][idx];
		if (!F)
			return false;
		const size_t ByteSize = 1 << idx;
		const size_t BitSize = ByteSize * 8;
		Type *Ty = Type::getIntNTy(builder.getContext(), BitSize);
		Type *PtrTy = Ty->getPointerTo();
		Value *args[] = {
		    builder.CreatePointerCast(addr, PtrTy),
		    rmwi,
		    builder.CreateIntCast(rmwi->getValOperand(), Ty, false),
		    createOrdering(&builder, rmwi->getOrdering())};
		CallInst *ci = CallInst::Create(F, args);
		ci->insertAfter(insn);
	} else if (auto casi = dyn_cast<AtomicCmpXchgInst>(insn)) {
		Value *addr = casi->getPointerOperand();
		int idx = getMemoryAccessFuncIndex(addr, dl);
		if (idx < 0)
			return false;
		const size_t ByteSize = 1 << idx;
		const size_t BitSize = ByteSize * 8;
		Type *Ty = Type::getIntNTy(builder.getContext(), BitSize);
		Type *PtrTy = Ty->getPointerTo();
		Value *args[] = {
		    builder.CreatePointerCast(addr, PtrTy),
		    builder.CreateIntCast(casi->getCompareOperand(), Ty, false),
		    builder.CreateIntCast(casi->getNewValOperand(), Ty, false),
		    createOrdering(&builder, casi->getSuccessOrdering()),
		    createOrdering(&builder, casi->getFailureOrdering())};
		CallInst *ci = builder.CreateCall(atomic_cas[idx], args);
		Value *Success = builder.CreateICmpEQ(ci,
		    casi->getCompareOperand());

		Value *res = builder.CreateInsertValue(
		    UndefValue::get(casi->getType()), ci, 0);
		res = builder.CreateInsertValue(res, Success, 1);

		insn->replaceAllUsesWith(res);
		insn->eraseFromParent();
	} else if (auto fi = dyn_cast<FenceInst>(insn)) {
		Value *args[] = {createOrdering(&builder, fi->getOrdering())};
		Function *f = (fi->getSynchScope() == SingleThread)
		    ? atomic_signal_fence
		    : atomic_thread_fence;
		CallInst *ci = CallInst::Create(f, args);
		ReplaceInstWithInst(insn, ci);
	}
	return true;
}

int RVPredictInstrument::getMemoryAccessFuncIndex(Value *Addr,
                                              const DataLayout &DL) {
  Type *OrigPtrTy = Addr->getType();
  Type *OrigTy = cast<PointerType>(OrigPtrTy)->getElementType();
  assert(OrigTy->isSized());
  uint32_t TypeSize = DL.getTypeStoreSizeInBits(OrigTy);
  if (TypeSize != 8  && TypeSize != 16 &&
      TypeSize != 32 && TypeSize != 64 && TypeSize != 128) {
    NumAccessesWithBadSize++;
    // Ignore all unusual sizes.
    return -1;
  }
  size_t Idx = countTrailingZeros(TypeSize / 8);
  assert(Idx < kNumberOfAccessSizes);
  return Idx;
}
}  // namespace
