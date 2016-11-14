add_llvm_loadable_module(RVPredictInstrumentation
#  AddressSanitizer.cpp
#  BoundsChecking.cpp
#  DataFlowSanitizer.cpp
#  GCOVProfiling.cpp
#  MemorySanitizer.cpp
#  Instrumentation.cpp
#  InstrProfiling.cpp
#  SafeStack.cpp
#  SanitizerCoverage.cpp
  ThreadSanitizer.cpp

  ADDITIONAL_HEADER_DIRS
  ${LLVM_MAIN_INCLUDE_DIR}/llvm/Transforms
  )
