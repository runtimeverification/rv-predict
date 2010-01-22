##===- lib/Transforms/Instrumentation/Makefile -------------*- Makefile -*-===##
#
#                     The LLVM Compiler Infrastructure
#
# This file is distributed under the University of Illinois Open Source
# License. See LICENSE.TXT for details.
#
##===----------------------------------------------------------------------===##

LEVEL = ../../..
LIBRARYNAME = LLVMInstrumentation
BUILD_ARCHIVE = 1
CXXFLAGS = -fno-rtti

include $(LEVEL)/Makefile.common

