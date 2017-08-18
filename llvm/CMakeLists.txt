cmake_minimum_required(VERSION 2.8)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

set(CMAKE_BUILD_TYPE Debug)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

find_package(LLVM 3.8 REQUIRED CONFIG)

#
# The following couple of lines are necessary for cmake
# to find LLVM commands such as add_llvm_loadable_module(...)
# on Mac OS X (El Capitan).
#
list(APPEND CMAKE_MODULE_PATH "${LLVM_CMAKE_DIR}")	# for include(AddLLVM) 
include(AddLLVM)			# for add_llvm_loadable_module(...)

add_definitions(${LLVM_DEFINITIONS})
include_directories(${LLVM_INCLUDE_DIRS})
link_directories(${LLVM_LIBRARY_DIRS})

add_subdirectory(pass)
add_subdirectory(runtime)
