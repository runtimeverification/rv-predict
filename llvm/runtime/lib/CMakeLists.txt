# First, add the subdirectories which contain feature-based runtime libraries
# and several convenience helper libraries.

include(AddCompilerRT)
include(SanitizerUtils)

if(COMPILER_RT_HAS_INTERCEPTION)
  add_subdirectory(interception)
endif()

if(COMPILER_RT_HAS_SANITIZER_COMMON)
  add_subdirectory(sanitizer_common)
  add_subdirectory(ubsan)
endif()

if(COMPILER_RT_HAS_TSAN)
  add_subdirectory(tsan)
endif()
