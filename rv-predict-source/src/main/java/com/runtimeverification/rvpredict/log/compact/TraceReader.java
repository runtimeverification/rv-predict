package com.runtimeverification.rvpredict.log.compact;

import com.runtimeverification.rvpredict.log.Event;

import java.io.*;
import java.nio.*;
import java.util.List;

import static java.lang.Math.toIntExact;

public class TraceReader implements Closeable {
    private InputStream inputStream = null;

    private TraceHeader traceHeader;
    private final int minDeltaAndEventType;
    private final int maxDeltaAndEventType;
    private List<CompactEvent> firstEvent;
    private Context context;

    public TraceReader(String path) throws IOException, InvalidTraceDataException {
        File file = new File(path);
        inputStream = new BufferedInputStream(new FileInputStream(file));
        traceHeader = new TraceHeader(inputStream);
        TraceData traceData = new TraceData(traceHeader);
        read("first event", traceData);
        minDeltaAndEventType = toIntExact(traceData.getPc().getAsLong())
                - (Constants.JUMPS_IN_DELTA / 2) * CompactEvent.Type.getNumberOfValues();
        maxDeltaAndEventType = minDeltaAndEventType
                + Constants.JUMPS_IN_DELTA * CompactEvent.Type.getNumberOfValues() - 1;
        context = new Context();
        DeltaAndEventType deltaAndEventType = DeltaAndEventType.parseFromPC(
                minDeltaAndEventType, maxDeltaAndEventType, traceData.getPc());
        if (deltaAndEventType == null
                || deltaAndEventType.getEventType() != CompactEvent.Type.THREAD_BEGIN) {
            throw new InvalidTraceDataException("All traces should start with begin.");
        }
        context.setJumpDeltaForBegin(deltaAndEventType.getJumpDelta());
        firstEvent = CompactEvent.begin(
                minDeltaAndEventType, context, traceData.getThreadId(), traceData.getGeneration());
    }

    public List<CompactEvent> getNextEvents()
            throws InvalidTraceDataException, IOException {
        if (firstEvent != null) {
            List<CompactEvent> event = firstEvent;
            firstEvent = null;
            return event;
        }
        Address pc = new Address(traceHeader);
        read("event identifier", pc);
        DeltaAndEventType deltaAndEventType =
                DeltaAndEventType.parseFromPC(minDeltaAndEventType, maxDeltaAndEventType, pc);
        if (deltaAndEventType == null) {
            return CompactEvent.jump(context, pc.getAsLong());
        }
        if (deltaAndEventType.getEventType() == CompactEvent.Type.THREAD_BEGIN) {
            context.setJumpDeltaForBegin(deltaAndEventType.getJumpDelta());
        } else {
            context.updatePcWithDelta(deltaAndEventType.getJumpDelta());
        }
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

    private static class DeltaAndEventType {
        private final CompactEvent.Type eventType;
        private final int jumpDelta;

        private DeltaAndEventType(CompactEvent.Type eventType, int jumpDelta) {
            this.eventType = eventType;
            this.jumpDelta = jumpDelta;
        }

        private static DeltaAndEventType parseFromPC(
                int minDeltaAndEventType, int maxDeltaAndEventType, Address address) {
            long pc = address.getAsLong();
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

        private int getJumpDelta() {
            return jumpDelta;
        }
    }
}
