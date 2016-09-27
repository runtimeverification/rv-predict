add_library(rv-instrument MODULE GatherAnnotation.cpp FindSharedVariable.cpp RVPredictInstrument.cpp)
add_library(runtime SHARED Runtime.cpp)
add_library(dummy_transmit_data OBJECT transmit_data.cpp)
#add_library(passD MODULE ThreadSanitizer.cpp)
