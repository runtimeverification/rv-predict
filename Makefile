##===- lib/Transforms/Instrumentation/Makefile -------------*- Makefile -*-===##
# 
#                     The LLVM Compiler Infrastructure
#
# This file was developed by the LLVM research group and is distributed under
# the University of Illinois Open Source License. See LICENSE.TXT for details.
# 
##===----------------------------------------------------------------------===##
LEVEL = ../../..
LIBRARYNAME = LLVMInstrumentation 
PARALLEL_DIRS = ProfilePaths 
BUILD_ARCHIVE = 1
DONT_BUILD_RELINKED = 1

include $(LEVEL)/Makefile.common

