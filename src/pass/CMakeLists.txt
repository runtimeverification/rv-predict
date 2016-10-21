#find_package(LLVM REQUIRED CONFIG)

#SET(CMAKE_CXX_FLAGS "-Wall -fno-rtti")

#add_library(rv-instrument SHARED GatherAnnotation.cpp FindSharedVariable.cpp RVPredictInstrument.cpp)
set(LLVM_LINK_COMPONENTS Core Support)

add_llvm_loadable_module(rv-instrument GatherAnnotation.cpp FindSharedVariable.cpp RVPredictInstrument.cpp)

# Get proper shared-library behavior (where symbols are not necessarily
# resolved when the shared library is linked) on OS X.
if(APPLE)
    set_target_properties(rv-instrument PROPERTIES
        LINK_FLAGS "-undefined dynamic_lookup"
    )
endif(APPLE)

