package com.runtimeverification.rvpredict.log.compact;

import java.io.*;
import java.nio.*;
import java.util.HashMap;

import static java.lang.Math.toIntExact;

public class TraceReader implements Closeable {
    private InputStream inputStream = null;

    private TraceHeader traceHeader;
    private TraceData traceData;
    private Map<Long, ThreadState> threadIdToState;
    private final int minDeltaAndEventType;
    private final int maxDeltaAndEventType;
    private Event firstEvent;

    public TraceReader(String path) throws IOException, InvalidTraceDataException {
        File file = new File(path);
        inputStream = new BufferedInputStream(new FileInputStream(file));
        traceHeader = new TraceHeader(inputStream);
        traceData = new TraceData(traceHeader);
        threadIdToState = new HashMap<>();
        read("first event", traceData);
        firstEvent = Event.begin(traceData.getPc(), traceData.getThreadId());
        minDeltaAndEventType = toIntExact(traceData.getPc())
                - (Constants.JUMPS_IN_DELTA / 2) * Event.Type.getNumberOfValues();
        maxDeltaAndEventType = minDeltaAndEventType
                + Constants.JUMPS_IN_DELTA * Event.Type.getNumberOfValues() - 1;
    }

    public Event getNextEvent() throws InvalidTraceDataException {
        if (firstEvent != null) {
            Event event = firstEvent;
            firstEvent = null;
            return event;
        }
        Address pc = new Address(traceHeader);
        DeltaAndEventType deltaAndEventType = DeltaAndEventType.parseFromPC(pc.getAsLong());
        if (deltaAndEventType == null) {
            // TODO(virgil): Update state.
            return Event.jump(pc.getAsLong());
        }
        return deltaAndEventType.getEventType().getReader().read(inputStream);
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }

    private void read(String name, ReadableData data) throws IOException, InvalidTraceDataException {
        // TODO(virgil): I could optimize this and not create the array each time. On the other hand,
        // java is supposed to be good at optimizing these things.
        byte[] bytes = new byte[data.size()];
        if (bytes.length != inputStream.read(bytes)) {
            throw new InvalidTraceDataException("Short read while reading " + name + ".");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(traceHeader.getByteOrder());
        data.read(buffer);
    }

    private class DeltaAndEventType {
        private final Event.Type eventType;
        private final int jumpDelta;

        private DeltaAndEventType(Event.Type eventType, int jumpDelta) {
            this.eventType = eventType;
            this.jumpDelta = jumpDelta;
        }

        private DeltaAndEventType parseFromPC(long pc) {
            if (pc < minDeltaAndEventType || pc > maxDeltaAndEventType) {
                return null;
            }
            int eventCount = Event.Type.getNumberOfValues();
            return new DeltaAndEventType(
                    Event.Type.fromInt(toIntExact(pc % eventCount)),
                    toIntExact(pc / eventCount - eventCount / 2));
        }

        private Event.Type getEventType() {
            return eventType;
        }
    }
}
