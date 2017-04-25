package com.runtimeverification.rvpredict.log.compact;

import static java.lang.Math.toIntExact;

class DeltaAndEventType {
    private final CompactEventReader.Type eventType;
    private final int pcDelta;
    private DeltaAndEventType(CompactEventReader.Type eventType, int pcDelta) {
        this.eventType = eventType;
        this.pcDelta = pcDelta;
    }

    static DeltaAndEventType parseFromPC(long minDeltaAndEventType, long maxDeltaAndEventType, long pc) {
        if (pc < minDeltaAndEventType || pc > maxDeltaAndEventType) {
            return null;
        }
        int eventCount = CompactEventReader.Type.getNumberOfValues();
        return new DeltaAndEventType(
                CompactEventReader.Type.fromInt(toIntExact(pc % eventCount)),
                toIntExact(pc / eventCount - eventCount / 2));
    }

    CompactEventReader.Type getEventType() {
        return eventType;
    }
    int getPcDelta() { return pcDelta; }
}
