// Copyright (c) 2016 Runtime Verification, Inc. (RV-Predict team). All Rights Reserved.

#include "RVPredictInstrument.h"
#include "GatherAnnotation.h"
#include "FindSharedVariable.h"

#include "llvm/Analysis/CaptureTracking.h"
#include "llvm/Analysis/ValueTracking.h"
#include "llvm/IR/IRBuilder.h"
#include "llvm/IR/LegacyPassManager.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/Transforms/IPO/PassManagerBuilder.h"

using namespace llvm;

namespace RVPredict {

/**
 * Check if an address contains constant data.
 */
bool
RVPredictInstrument::addrContainsConstData(Value *Addr) {
    // If this is a GEP, just analyze its pointer operand.
    if (GetElementPtrInst *GEP = dyn_cast<GetElementPtrInst>(Addr)) {
        Addr = GEP->getPointerOperand();
    } else if (GEPOperator *GEP = dyn_cast<GEPOperator>(Addr)) {
        Addr = GEP->getPointerOperand();
    }

    if (GlobalVariable *GV = dyn_cast<GlobalVariable>(Addr)) {
        if (GV->isConstant()) {
            // Reads from constant globals can not race with any writes.
            return true;
        }
    }
    // Check vtable read if we have to deal with bare-metal C++.
    return false;
}

/**
 * Check if an address contains shared global variable.
 */
bool
RVPredictInstrument::addrContainsSharedGlobalVar(Value *Addr) {
    // If this is a GEP, just analyze its pointer operand.
    if (GetElementPtrInst *GEP = dyn_cast<GetElementPtrInst>(Addr)) {
        Addr = GEP->getPointerOperand();
    } else if (GEPOperator *GEP = dyn_cast<GEPOperator>(Addr)) {
        Addr = GEP->getPointerOperand();
    }

    if (GlobalVariable *GV = dyn_cast<GlobalVariable>(Addr)) {
        return getAnalysis<FindSharedVariable>().isSharedVariable(GV);
    }
    return false;
}

/**
 * From all the load/store instructions inside a portion of basic block
 * that has no function call, decide which of them should be instrumented.
 *
 * Furthermore, this function attempts to avoid instrumenting accesses
 * that can be proved to be redundant to the analysis.
 *
 * @param Local
 *      The load/store instructions to be chosen from.
 * @param[out] All
 *      Holds all load/store instructions that are chosen across multiple
 *      runs of this function.
 * @param DL
 *      The data layout.
 */
void
RVPredictInstrument::chooseInstructionsToInstrument(
        const SmallVectorImpl<Instruction*> &Local,
        SmallVectorImpl<Instruction*> &All,
        const DataLayout &DL) {
    // An instrumentation is said to be redundant if it can be removed without
    // causing any false positive or missing bug (i.e., the bug will be caught
    // anyway by detecting race involving another access).

    // The current reasoning for redundant accesses is rather weak. So far,
    // we only handle 1) reads of global constant data, and 2) reads/writes
    // of (non-escaping) stack variables.

    // It is also questionable if it's worth spending time here because a more
    // effective optimization is to identify and only instrument shared memory
    // locations.
    for (SmallVectorImpl<Instruction*>::const_iterator It = Local.begin();
            It != Local.end(); ++It) {
        Instruction *I = *It;

        // Do not instrument read of global constant data.
        if (LoadInst *Load = dyn_cast<LoadInst>(I)) {
            if (addrContainsConstData(Load->getPointerOperand())) {
                continue;
            }
        }

        // Do not instrument non-shared global variables.
        Value *Addr = isa<StoreInst>(*I)
                ? cast<StoreInst>(I)->getPointerOperand()
                : cast<LoadInst>(I)->getPointerOperand();
        if (!addrContainsSharedGlobalVar(Addr)) {
            continue;
        }

        // Do not instrument read/write of non-escaping stack variable.
        if (isa<AllocaInst>(GetUnderlyingObject(Addr, DL)) &&
            !PointerMayBeCaptured(Addr, true, true)) {
            // The variable is addressable but not captured, so it cannot be
            // referenced from an ISR and participate in a data race
            // (see llvm/Analysis/CaptureTracking.h for details).
            continue;
        }

        All.push_back(I);
    }
}

void
RVPredictInstrument::getAnalysisUsage(AnalysisUsage &AU) const
{
    // This function pass invalidates all other passes.
    AU.addRequired<GatherAnnotation>();
    AU.addRequired<FindSharedVariable>();
}

/**
 * Decide which runtime function should be used to instrument
 * this memory access.
 *
 * @param Addr
 *      The memory address of the access.
 * @param DL
 *      The data layout.
 * @return
 *      The index that points to the right runtime function.
 */
int
RVPredictInstrument::getMemoryAccessFuncIndex(Value *Addr,
                                              const DataLayout &DL)
{
    Type *ElemTy = cast<PointerType>(Addr->getType())->getElementType();
    assert(ElemTy->isSized());
    uint32_t TypeSize = DL.getTypeStoreSizeInBits(ElemTy);
    if (TypeSize != 8  && TypeSize != 16 &&
        TypeSize != 32 && TypeSize != 64 && TypeSize != 128) {
        // Ignore all unusual sizes.
        return -1;
    }
    return static_cast<int>(countTrailingZeros(TypeSize / 8));
}

void
RVPredictInstrument::initializeCallbacks(Module &M)
{
    IRBuilder<> IRB(M.getContext());

#define VOID IRB.getVoidTy()
#define VOID_PTR IRB.getInt8PtrTy()
    // TODO: maybe some macro magic to further simplify the initializations?
#define UINT8 IRB.getInt8Ty(), nullptr
#define UINT16 IRB.getInt16Ty(), nullptr
#define UINT32 IRB.getInt32Ty(), nullptr
#define UINT64 IRB.getInt64Ty(), nullptr
#define UINT128 IRB.getInt128Ty(), nullptr

    RtFuncDisableIRQ = M.getOrInsertFunction("__rv_disable_irq", VOID, UINT8);
    RtFuncEnableIRQ = M.getOrInsertFunction("__rv_enable_irq", VOID, UINT8);

    RtFuncISREntry = M.getOrInsertFunction("__rv_isr_entry", VOID, UINT8);
    RtFuncISRExit = M.getOrInsertFunction("__rv_isr_exit", VOID, UINT8);

    RtFuncRead[0] = M.getOrInsertFunction("__rv_read1", VOID, VOID_PTR, UINT8);
    RtFuncRead[1] = M.getOrInsertFunction("__rv_read2", VOID, VOID_PTR, UINT16);
    RtFuncRead[2] = M.getOrInsertFunction("__rv_read4", VOID, VOID_PTR, UINT32);
    RtFuncRead[3] = M.getOrInsertFunction("__rv_read8", VOID, VOID_PTR, UINT64);
    RtFuncRead[4] = M.getOrInsertFunction("__rv_read16", VOID, VOID_PTR, UINT128);

    RtFuncWrite[0] = M.getOrInsertFunction("__rv_write1", VOID, VOID_PTR, UINT8);
    RtFuncWrite[1] = M.getOrInsertFunction("__rv_write2", VOID, VOID_PTR, UINT16);
    RtFuncWrite[2] = M.getOrInsertFunction("__rv_write4", VOID, VOID_PTR, UINT32);
    RtFuncWrite[3] = M.getOrInsertFunction("__rv_write8", VOID, VOID_PTR, UINT64);
    RtFuncWrite[4] = M.getOrInsertFunction("__rv_write16", VOID, VOID_PTR, UINT128);
}

/**
 * Instrument a load or store instruction with a call to the runtime library.
 *
 * @param I
 *      The load or store instruction.
 * @param DL
 *      The data layout.
 * @return
 *      True on success; otherwise, false.
 */
bool
RVPredictInstrument::instrumentLoadOrStore(Instruction *I,
                                           const DataLayout &DL)
{
    IRBuilder<> IRB(I);
    // The memory location we are accessing.
    Value *Addr;
    // The value returned by/passed to the load/store instruction.
    Value *Val;

    bool IsWrite = isa<StoreInst>(*I);
    if (IsWrite) {
        StoreInst *Store = cast<StoreInst>(I);
        Addr = Store->getPointerOperand();
        Val = Store->getValueOperand();
    } else {
        LoadInst *Load = cast<LoadInst>(I);
        Addr = Load->getPointerOperand();
        Val = Load;
    }

    // Pick the correct runtime function to instrument.
    int Idx = getMemoryAccessFuncIndex(Addr, DL);
    if (Idx < 0)
        return false;
    Value *OnAccessFunc = IsWrite ? RtFuncWrite[Idx] : RtFuncRead[Idx];

    // Cast the address pointer to void* (and insert the cast instruction).
    Addr = IRB.CreatePointerCast(Addr, IRB.getInt8PtrTy());
    // Cast the value to an unsigned integer of the same size.
    Instruction *CastInsn = CastInst::CreateBitOrPointerCast(Val,
            IRB.getIntNTy(1 << static_cast<uint>(Idx + 3)));

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
    return true;
}

bool
RVPredictInstrument::runOnFunction(Function &F)
{
    initializeCallbacks(*F.getParent());
    SmallVector<Instruction*, 8> RetVec;
    SmallVector<Instruction*, 8> AllLoadsAndStores;
    SmallVector<Instruction*, 8> LocalLoadsAndStores;

    const DataLayout &DL = F.getParent()->getDataLayout();

    // Traverse all instructions, collect loads/stores/returns, check for calls.
    for (auto &BB : F) {
        for (auto &Inst : BB) {
            if (isa<LoadInst>(Inst) || isa<StoreInst>(Inst)) {
                LocalLoadsAndStores.push_back(&Inst);
            } else if (isa<ReturnInst>(Inst)) {
                RetVec.push_back(&Inst);
            } else if (isa<CallInst>(Inst) || isa<InvokeInst>(Inst)) {
                // Break up the basic block with function calls
                chooseInstructionsToInstrument(LocalLoadsAndStores,
                        AllLoadsAndStores, DL);
                LocalLoadsAndStores.clear();
            }
        }
        chooseInstructionsToInstrument(LocalLoadsAndStores,
                AllLoadsAndStores, DL);
        LocalLoadsAndStores.clear();
    }

    bool HasInstrumentedAccesses = false;
    for (auto &Inst : AllLoadsAndStores) {
        HasInstrumentedAccesses |= instrumentLoadOrStore(Inst, DL);
    }

    // TODO: clean up the following code
    GatherAnnotation &GAAnalysis = getAnalysis<GatherAnnotation>();
    uint8_t Prio;
    if (GAAnalysis.getISRPrioLevel(F, Prio)) {
        // Insert calls at ISR entry/exit points if there were
        // instrumented accesses.
        if (!HasInstrumentedAccesses) {
            return false;
        }

        IRBuilder<> IRB(F.getEntryBlock().getFirstNonPHI());
        Value *PrioValue = ConstantInt::get(IRB.getInt8Ty(), Prio);
        IRB.CreateCall(RtFuncISREntry, PrioValue);
        for (auto &RetInst : RetVec) {
            IRBuilder<> IRBRet(RetInst);
            IRBRet.CreateCall(RtFuncISRExit, PrioValue);
        }
        return true;
    } else if (GAAnalysis.getDisableIRQPrioLevel(F, Prio)) {
        // Insert calls after disabling the IRQ (i.e., before return)
        IRBuilder<> IRB(F.getEntryBlock().getFirstNonPHI());
        Value *PrioValue = ConstantInt::get(IRB.getInt8Ty(), Prio);
        for (auto &RetInst : RetVec) {
            IRBuilder<> IRBRet(RetInst);
            IRBRet.CreateCall(RtFuncDisableIRQ, PrioValue);
        }
        return true;
    } else if (GAAnalysis.getEnableIRQPrioLevel(F, Prio)) {
        // Insert the call before enabling the IRQ
        IRBuilder<> IRB(F.getEntryBlock().getFirstNonPHI());
        Value *PrioValue = ConstantInt::get(IRB.getInt8Ty(), Prio);
        IRB.CreateCall(RtFuncEnableIRQ, PrioValue);
        return true;
    }
    return HasInstrumentedAccesses;
}

char RVPredictInstrument::ID = 0;
static RegisterPass<RVPredictInstrument> _("rv-instrument",
        "RV-Predict[Bare-metal] instrumentation pass", false, false);

/// Registering this function pass at extension point `EP_EarlyAsPossible`      
/// results in a segmentation fault at runtime because the LLVM PassManager    
/// somehow schedules the module passes this function pass depends on        
/// to run too early (even before fields in the `Module` are properly
/// initialized).  To workaround this problem, specify this pass to have
/// the same extension points as the module passes it depends on.

static void registerThisPass(const PassManagerBuilder &, legacy::PassManagerBase &PM) {
    PM.add(new RVPredictInstrument());
}

static RegisterStandardPasses __(PassManagerBuilder::EP_ModuleOptimizerEarly,
                                 registerThisPass);

static RegisterStandardPasses ___(PassManagerBuilder::EP_EnabledOnOptLevel0,
                                  registerThisPass);

} // namespace RVPredict
