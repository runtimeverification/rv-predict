# CMake build for CompilerRT.
#
# This build assumes that CompilerRT is checked out into the
# 'projects/compiler-rt' inside of an LLVM tree.
# Standalone build system for CompilerRT is not yet ready.
#
# An important constraint of the build is that it only produces libraries
# based on the ability of the host toolchain to target various platforms.

# Built as a standalone project.
project(CompilerRT C CXX)

# Top level target used to build all compiler-rt libraries.
add_custom_target(compiler-rt ALL)

# Take output dir and install path from the user.
set(COMPILER_RT_OUTPUT_DIR ${CMAKE_CURRENT_BINARY_DIR} CACHE PATH
  "Path where built compiler-rt libraries should be stored.")
set(COMPILER_RT_EXEC_OUTPUT_DIR ${CMAKE_CURRENT_BINARY_DIR}/bin CACHE PATH
  "Path where built compiler-rt executables should be stored.")
set(COMPILER_RT_INSTALL_PATH ${CMAKE_INSTALL_PREFIX} CACHE PATH
  "Path where built compiler-rt libraries should be installed.")
option(COMPILER_RT_INCLUDE_TESTS "Generate and build compiler-rt unit tests." OFF)
option(COMPILER_RT_ENABLE_WERROR "Fail and stop if warning is triggered" OFF)
# Use a host compiler to compile/link tests.
set(COMPILER_RT_TEST_COMPILER ${CMAKE_C_COMPILER} CACHE PATH "Compiler to use for testing")

# Find Python interpreter.
set(Python_ADDITIONAL_VERSIONS 2.7 2.6 2.5)
include(FindPythonInterp)
if(NOT PYTHONINTERP_FOUND)
  message(FATAL_ERROR "
    Unable to find Python interpreter required testing. Please install Python
    or specify the PYTHON_EXECUTABLE CMake variable.")
endif()

# Define default arguments to lit.
set(LIT_ARGS_DEFAULT "-sv")
if (MSVC OR XCODE)
  set(LIT_ARGS_DEFAULT "${LIT_ARGS_DEFAULT} --no-progress-bar")
endif()
set(LLVM_LIT_ARGS "${LIT_ARGS_DEFAULT}" CACHE STRING "Default options for lit")

if("${COMPILER_RT_TEST_COMPILER}" MATCHES "clang[+]*$")
  set(COMPILER_RT_TEST_COMPILER_ID Clang)
elseif("${COMPILER_RT_TEST_COMPILER}" MATCHES "clang.*.exe$")
  set(COMPILER_RT_TEST_COMPILER_ID Clang)
else()
  set(COMPILER_RT_TEST_COMPILER_ID GNU)
endif()

# Tests using XFAIL use the first value in COMPILER_RT_TEST_TARGET_TRIPLE
set(COMPILER_RT_TEST_TARGET_TRIPLE ${TARGET_TRIPLE} CACHE STRING
    "Default triple for cross-compiled executables")
string(REPLACE "-" ";" TARGET_TRIPLE_LIST ${COMPILER_RT_TEST_TARGET_TRIPLE})
list(GET TARGET_TRIPLE_LIST 0 COMPILER_RT_TEST_TARGET_ARCH)
list(GET TARGET_TRIPLE_LIST 1 COMPILER_RT_TEST_TARGET_OS)
list(GET TARGET_TRIPLE_LIST 2 COMPILER_RT_TEST_TARGET_ABI)
# Determine if test target triple is specified explicitly, and doesn't match the
# default.
if(NOT COMPILER_RT_TEST_TARGET_TRIPLE STREQUAL TARGET_TRIPLE)
  set(COMPILER_RT_HAS_EXPLICIT_TEST_TARGET_TRIPLE TRUE)
else()
  set(COMPILER_RT_HAS_EXPLICIT_TEST_TARGET_TRIPLE FALSE)
endif()

if ("${COMPILER_RT_TEST_TARGET_ABI}" STREQUAL "androideabi")
  set(ANDROID 1)
endif()

string(TOLOWER ${CMAKE_SYSTEM_NAME} COMPILER_RT_OS_DIR)
set(COMPILER_RT_LIBRARY_OUTPUT_DIR
  ${COMPILER_RT_OUTPUT_DIR}/lib/${COMPILER_RT_OS_DIR})
set(COMPILER_RT_LIBRARY_INSTALL_DIR
  ${COMPILER_RT_INSTALL_PATH}/lib/${COMPILER_RT_OS_DIR})

# Add path for custom compiler-rt modules.
set(CMAKE_MODULE_PATH
  "${CMAKE_CURRENT_SOURCE_DIR}/cmake"
  "${CMAKE_CURRENT_SOURCE_DIR}/cmake/Modules"
  ${CMAKE_MODULE_PATH}
  )
include(CompilerRTUtils)

set(COMPILER_RT_SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR})
set(COMPILER_RT_BINARY_DIR ${CMAKE_CURRENT_BINARY_DIR})

# We support running instrumented tests when we're not cross compiling
# and target a UNIX-like system or Windows.
# We can run tests on Android even when we are cross-compiling.
if(("${CMAKE_HOST_SYSTEM}" STREQUAL "${CMAKE_SYSTEM}" AND (UNIX OR WIN32)) OR ANDROID
   OR COMPILER_RT_EMULATOR)
  option(COMPILER_RT_CAN_EXECUTE_TESTS "Can we execute instrumented tests" ON)
else()
  option(COMPILER_RT_CAN_EXECUTE_TESTS "Can we execute instrumented tests" OFF)
endif()

option(COMPILER_RT_DEBUG "Build runtimes with full debug info" OFF)
# COMPILER_RT_DEBUG_PYBOOL is used by lit.common.configured.in.
pythonize_bool(COMPILER_RT_DEBUG)

#================================
# Setup Compiler Flags
#================================
include(CheckIncludeFile)
check_include_file(unwind.h HAVE_UNWIND_H)

include(config-ix)

if(MSVC)
  append_string_if(COMPILER_RT_HAS_W3_FLAG /W3 CMAKE_C_FLAGS CMAKE_CXX_FLAGS)
else()
  append_string_if(COMPILER_RT_HAS_WALL_FLAG -Wall CMAKE_C_FLAGS CMAKE_CXX_FLAGS)
endif()
if(COMPILER_RT_ENABLE_WERROR)
  append_string_if(COMPILER_RT_HAS_WERROR_FLAG -Werror CMAKE_C_FLAGS CMAKE_CXX_FLAGS)
  append_string_if(COMPILER_RT_HAS_WX_FLAG /WX CMAKE_C_FLAGS CMAKE_CXX_FLAGS)
endif()

append_string_if(COMPILER_RT_HAS_STD_CXX11_FLAG -std=c++11 CMAKE_CXX_FLAGS)

# Emulate C99 and C++11's __func__ for MSVC prior to 2013 CTP.
if(NOT COMPILER_RT_HAS_FUNC_SYMBOL)
  add_definitions(-D__func__=__FUNCTION__)
endif()

# Provide some common commmandline flags for Sanitizer runtimes.
append_list_if(COMPILER_RT_HAS_FPIC_FLAG -fPIC SANITIZER_COMMON_CFLAGS)
append_list_if(COMPILER_RT_HAS_FNO_BUILTIN_FLAG -fno-builtin SANITIZER_COMMON_CFLAGS)
append_list_if(COMPILER_RT_HAS_FNO_EXCEPTIONS_FLAG -fno-exceptions SANITIZER_COMMON_CFLAGS)
if(NOT COMPILER_RT_DEBUG)
append_list_if(COMPILER_RT_HAS_FOMIT_FRAME_POINTER_FLAG -fomit-frame-pointer SANITIZER_COMMON_CFLAGS)
endif()
append_list_if(COMPILER_RT_HAS_FUNWIND_TABLES_FLAG -funwind-tables SANITIZER_COMMON_CFLAGS)
append_list_if(COMPILER_RT_HAS_FNO_STACK_PROTECTOR_FLAG -fno-stack-protector SANITIZER_COMMON_CFLAGS)
append_list_if(COMPILER_RT_HAS_FNO_SANITIZE_SAFE_STACK_FLAG -fno-sanitize=safe-stack SANITIZER_COMMON_CFLAGS)
append_list_if(COMPILER_RT_HAS_FVISIBILITY_HIDDEN_FLAG -fvisibility=hidden SANITIZER_COMMON_CFLAGS)
append_list_if(COMPILER_RT_HAS_FNO_FUNCTION_SECTIONS_FLAG -fno-function-sections SANITIZER_COMMON_CFLAGS)
append_list_if(COMPILER_RT_HAS_FNO_LTO_FLAG -fno-lto SANITIZER_COMMON_CFLAGS)

if(MSVC)
  # Replace the /M[DT][d] flags with /MT, and strip any definitions of _DEBUG,
  # which cause definition mismatches at link time.
  # FIXME: In fact, sanitizers should support both /MT and /MD, see PR20214.
  if(COMPILER_RT_HAS_MT_FLAG)
    foreach(flag_var
      CMAKE_CXX_FLAGS CMAKE_CXX_FLAGS_DEBUG CMAKE_CXX_FLAGS_RELEASE
      CMAKE_CXX_FLAGS_MINSIZEREL CMAKE_CXX_FLAGS_RELWITHDEBINFO)
      string(REGEX REPLACE "/M[DT]d" "/MT" ${flag_var} "${${flag_var}}")
      string(REGEX REPLACE "/MD" "/MT" ${flag_var} "${${flag_var}}")
      string(REGEX REPLACE "/D_DEBUG" "" ${flag_var} "${${flag_var}}")
    endforeach()
  endif()
  append_list_if(COMPILER_RT_HAS_Oy_FLAG /Oy- SANITIZER_COMMON_CFLAGS)
  append_list_if(COMPILER_RT_HAS_GS_FLAG /GS- SANITIZER_COMMON_CFLAGS)
endif()

append_list_if(COMPILER_RT_DEBUG -DSANITIZER_DEBUG=1 SANITIZER_COMMON_CFLAGS)

# Build with optimization, unless we're in debug mode. If we're using MSVC,
# always respect the optimization flags set by CMAKE_BUILD_TYPE instead.
if(NOT COMPILER_RT_DEBUG AND NOT MSVC)
  list(APPEND SANITIZER_COMMON_CFLAGS -O3)
endif()

# Determine if we should restrict stack frame sizes.
# Stack frames on PowerPC and Mips and in debug biuld can be much larger than
# anticipated.
# FIXME: Fix all sanitizers and add -Wframe-larger-than to
# SANITIZER_COMMON_FLAGS
if(COMPILER_RT_HAS_WFRAME_LARGER_THAN_FLAG AND NOT COMPILER_RT_DEBUG
   AND NOT ${LLVM_NATIVE_ARCH} STREQUAL "PowerPC"
   AND NOT ${LLVM_NATIVE_ARCH} STREQUAL "Mips")
  set(SANITIZER_LIMIT_FRAME_SIZE TRUE)
else()
  set(SANITIZER_LIMIT_FRAME_SIZE FALSE)
endif()

# Build sanitizer runtimes with debug info.
if(COMPILER_RT_HAS_GLINE_TABLES_ONLY_FLAG AND NOT COMPILER_RT_DEBUG)
  list(APPEND SANITIZER_COMMON_CFLAGS -gline-tables-only)
elseif(COMPILER_RT_HAS_G_FLAG)
  list(APPEND SANITIZER_COMMON_CFLAGS -g)
elseif(COMPILER_RT_HAS_Zi_FLAG)
  list(APPEND SANITIZER_COMMON_CFLAGS /Zi)
endif()

# Turn off several warnings.
append_list_if(COMPILER_RT_HAS_WGNU_FLAG -Wno-gnu SANITIZER_COMMON_CFLAGS)
append_list_if(COMPILER_RT_HAS_WVARIADIC_MACROS_FLAG -Wno-variadic-macros SANITIZER_COMMON_CFLAGS)
append_list_if(COMPILER_RT_HAS_WC99_EXTENSIONS_FLAG -Wno-c99-extensions SANITIZER_COMMON_CFLAGS)
append_list_if(COMPILER_RT_HAS_WNON_VIRTUAL_DTOR_FLAG -Wno-non-virtual-dtor SANITIZER_COMMON_CFLAGS)
append_list_if(COMPILER_RT_HAS_WD4146_FLAG /wd4146 SANITIZER_COMMON_CFLAGS)
append_list_if(COMPILER_RT_HAS_WD4291_FLAG /wd4291 SANITIZER_COMMON_CFLAGS)
append_list_if(COMPILER_RT_HAS_WD4391_FLAG /wd4391 SANITIZER_COMMON_CFLAGS)
append_list_if(COMPILER_RT_HAS_WD4722_FLAG /wd4722 SANITIZER_COMMON_CFLAGS)
append_list_if(COMPILER_RT_HAS_WD4800_FLAG /wd4800 SANITIZER_COMMON_CFLAGS)
if(APPLE)
  macro(find_darwin_sdk_dir var sdk_name)
    # Let's first try the internal SDK, otherwise use the public SDK.
    execute_process(
      COMMAND xcodebuild -version -sdk ${sdk_name}.internal Path
      OUTPUT_VARIABLE ${var}
      OUTPUT_STRIP_TRAILING_WHITESPACE
      ERROR_FILE /dev/null
    )
    if(${var} STREQUAL "")
      execute_process(
        COMMAND xcodebuild -version -sdk ${sdk_name} Path
        OUTPUT_VARIABLE ${var}
        OUTPUT_STRIP_TRAILING_WHITESPACE
        ERROR_FILE /dev/null
      )
    endif()
  endmacro()

  find_darwin_sdk_dir(OSX_SDK_DIR macosx)
  find_darwin_sdk_dir(IOSSIM_SDK_DIR iphonesimulator)

  set(SANITIZER_COMMON_SUPPORTED_OS osx)
  string(REGEX MATCH "-mmacosx-version-min=([.0-9]+)"
         MACOSX_VERSION_MIN_FLAG "${CMAKE_CXX_FLAGS}")
  if(MACOSX_VERSION_MIN_FLAG)
    set(SANITIZER_MIN_OSX_VERSION "${CMAKE_MATCH_1}")
  elseif(CMAKE_OSX_DEPLOYMENT_TARGET)
    set(SANITIZER_MIN_OSX_VERSION ${CMAKE_OSX_DEPLOYMENT_TARGET})
  else()
    set(SANITIZER_MIN_OSX_VERSION 10.9)
    if(IOSSIM_SDK_DIR)
      list(APPEND SANITIZER_COMMON_SUPPORTED_OS iossim)
    endif()
  endif()
  if(SANITIZER_MIN_OSX_VERSION VERSION_LESS "10.7")
    message(FATAL_ERROR "Too old OS X version: ${SANITIZER_MIN_OSX_VERSION}")
  endif()

  set(CMAKE_OSX_DEPLOYMENT_TARGET "") # We evaluate target OS X version above.
  set(DARWIN_osx_CFLAGS -mmacosx-version-min=${SANITIZER_MIN_OSX_VERSION}
    -stdlib=libc++)
  set(DARWIN_iossim_CFLAGS
    -stdlib=libc++
    -mios-simulator-version-min=7.0 -isysroot ${IOSSIM_SDK_DIR})
  set(DARWIN_osx_LINKFLAGS -mmacosx-version-min=${SANITIZER_MIN_OSX_VERSION}
    -stdlib=libc++ -lc++ -lc++abi)
  set(DARWIN_iossim_LINKFLAGS
    -stdlib=libc++ -lc++ -lc++abi
    -Wl,-ios_simulator_version_min,7.0.0
    -mios-simulator-version-min=7.0
    -isysroot ${IOSSIM_SDK_DIR})

  if(OSX_SDK_DIR)
    list(APPEND DARWIN_osx_CFLAGS -isysroot ${OSX_SDK_DIR})
    list(APPEND DARWIN_osx_LINKFLAGS -isysroot ${OSX_SDK_DIR})
  endif()
endif()

if(APPLE AND SANITIZER_MIN_OSX_VERSION VERSION_LESS "10.9")
  # Mac OS X prior to 10.9 had problems with exporting symbols from
  # libc++/libc++abi.
  set(SANITIZER_CAN_USE_CXXABI FALSE)
else()
  set(SANITIZER_CAN_USE_CXXABI TRUE)
endif()
pythonize_bool(SANITIZER_CAN_USE_CXXABI)

add_subdirectory(include)

set(COMPILER_RT_HAS_LIBCXX_SOURCES FALSE)

add_subdirectory(lib)
