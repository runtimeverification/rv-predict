package com.runtimeverification.rvpredict.log.compact;

import java.io.*;
import java.nio.*;
import java.util.HashMap;
import java.util.List;

import static java.lang.Math.toIntExact;

public class TraceReader implements Closeable {
    private InputStream inputStream = null;

    private TraceHeader traceHeader;
    private TraceData traceData;
    private final int minDeltaAndEventType;
    private final int maxDeltaAndEventType;
    private List<CompactEvent> firstEvent;
    private Context context;

    public TraceReader(String path) throws IOException, InvalidTraceDataException {
        File file = new File(path);
        inputStream = new BufferedInputStream(new FileInputStream(file));
        traceHeader = new TraceHeader(inputStream);
        traceData = new TraceData(traceHeader);
        read("first event", traceData);
        context = new Context();
        firstEvent = CompactEvent.begin(context, traceData.getPc(), traceData.getThreadId());
        minDeltaAndEventType = toIntExact(traceData.getPc())
                - (Constants.JUMPS_IN_DELTA / 2) * CompactEvent.Type.getNumberOfValues();
        maxDeltaAndEventType = minDeltaAndEventType
                + Constants.JUMPS_IN_DELTA * CompactEvent.Type.getNumberOfValues() - 1;
    }

    public List<CompactEvent> getNextEvents(Context context)
            throws InvalidTraceDataException, IOException {
        if (firstEvent != null) {
            List<CompactEvent> event = firstEvent;
            firstEvent = null;
            return event;
        }
        Address pc = new Address(traceHeader);
        DeltaAndEventType deltaAndEventType = DeltaAndEventType.parseFromPC(pc.getAsLong());
        if (deltaAndEventType == null) {
            // TODO(virgil): Update state.
            return CompactEvent.jump(context, pc.getAsLong());
        }
        context.updatePcWithDelta(deltaAndEventType.getJumpDelta());
        return deltaAndEventType.getEventType().read(context, traceHeader, inputStream);
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
        private final CompactEvent.Type eventType;
        private final int jumpDelta;

        private DeltaAndEventType(CompactEvent.Type eventType, int jumpDelta) {
            this.eventType = eventType;
            this.jumpDelta = jumpDelta;
        }

        private DeltaAndEventType parseFromPC(long pc) {
            if (pc < minDeltaAndEventType || pc > maxDeltaAndEventType) {
                return null;
            }
            int eventCount = CompactEvent.Type.getNumberOfValues();
            return new DeltaAndEventType(
                    CompactEvent.Type.fromInt(toIntExact(pc % eventCount)),
                    toIntExact(pc / eventCount - eventCount / 2));
        }

        private CompactEvent.Type getEventType() {
            return eventType;
        }

        public int getJumpDelta() {
            return jumpDelta;
        }
    }
}
