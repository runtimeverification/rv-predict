// Copyright (c) 2016 Runtime Verification, Inc. (RV-Predict team). All Rights Reserved.

/// This file contains the dummy implementation of the `transmit_data`
/// interface that simply prints out every event to stdout.
///
/// Note that a real implementation will be written in the same language
/// (i.e., most likely C) as the system-under-test. Here, to simplify
/// the inclusion of the C++ header "WireFormat.h", we intentionally
/// choose C++ as our implementation language and provide a C wrapper.
#ifdef __cplusplus
extern "C" {
#endif

#include <cstddef>
#include <cstdio>
#include "WireFormat.h"

void
transmit_data(void *data, size_t len)
{
//    std::printf("*data = %p, len = %zu\n", data, len);
    switch (static_cast<Header*>(data)->opcode) {
        case ISR_ENTRY:
            std::printf("Entering ISR: prio = %u\n",
                        static_cast<ISREntry*>(data)->common.prio);
            break;
        case ISR_EXIT:
            std::printf("Exiting ISR: prio = %u\n",
                        static_cast<ISRExit*>(data)->common.prio);
            break;
        case DISABLE_IRQ:
            std::printf("IRQ disabled: prio = %u\n",
                        static_cast<DisableIRQ*>(data)->common.prio);
            break;
        case ENABLE_IRQ:
            std::printf("IRQ enabled: prio = %u\n",
                        static_cast<EnableIRQ*>(data)->common.prio);
            break;
        case WRITE4:
            std::printf("Write %u to location %lu\n",
                        static_cast<Write4*>(data)->value,
                        static_cast<Write4*>(data)->common.addr);
            break;
        default:
            break;
    }
}

#ifdef __cplusplus
}  // extern "C"
#endif
