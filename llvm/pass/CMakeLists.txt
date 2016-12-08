add_llvm_loadable_module(rvpinstrument
  RVPredictInstrumentation.cpp

  ADDITIONAL_HEADER_DIRS
  ${LLVM_MAIN_INCLUDE_DIR}/llvm/Transforms
  )
