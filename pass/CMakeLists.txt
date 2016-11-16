add_llvm_loadable_module(rvpinstrument
#  AddressSanitizer.cpp
#  BoundsChecking.cpp
#  DataFlowSanitizer.cpp
#  GCOVProfiling.cpp
#  MemorySanitizer.cpp
#  Instrumentation.cpp
#  InstrProfiling.cpp
#  SafeStack.cpp
#  SanitizerCoverage.cpp
  RVPredictInstrumentation.cpp

  ADDITIONAL_HEADER_DIRS
  ${LLVM_MAIN_INCLUDE_DIR}/llvm/Transforms
  )
