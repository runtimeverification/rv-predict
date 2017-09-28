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

        long normalizedPc = pc - minDeltaAndEventType;

        return new DeltaAndEventType(
                CompactEventReader.Type.fromInt(toIntExact(normalizedPc % eventCount)),
                toIntExact(normalizedPc / eventCount - Constants.JUMPS_IN_DELTA / 2));
    }

    CompactEventReader.Type getEventType() {
        return eventType;
    }
    int getPcDelta() { return pcDelta; }
}
