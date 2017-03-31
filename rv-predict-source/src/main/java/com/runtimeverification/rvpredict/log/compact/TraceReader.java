package com.runtimeverification.rvpredict.log.compact;

import com.runtimeverification.rvpredict.log.compact.datatypes.Address;

import java.io.*;
import java.nio.*;
import java.nio.file.Path;
import java.util.List;

import static java.lang.Math.toIntExact;

public class TraceReader implements Closeable {
    private final InputStream inputStream;
    private final TraceHeader traceHeader;
    private final int minDeltaAndEventType;
    private final int maxDeltaAndEventType;
    private final Context context;
    private final CompactEventReader compactEventReader;

    private List<CompactEvent> firstEvent;

    public TraceReader(Path path) throws IOException, InvalidTraceDataException {
        File file = path.toFile();
        inputStream = new BufferedInputStream(new FileInputStream(file));
        traceHeader = new TraceHeader(inputStream);
        TraceData traceData = new TraceData(traceHeader);
        read("first event", traceData, false);
        minDeltaAndEventType = toIntExact(traceData.getPc().getAsLong())
                - (Constants.JUMPS_IN_DELTA / 2) * CompactEventReader.Type.getNumberOfValues();
        maxDeltaAndEventType = minDeltaAndEventType
                + Constants.JUMPS_IN_DELTA * CompactEventReader.Type.getNumberOfValues() - 1;
        context = new Context(minDeltaAndEventType);
        compactEventReader = new CompactEventReader();
        DeltaAndEventType deltaAndEventType = DeltaAndEventType.parseFromPC(
                minDeltaAndEventType, maxDeltaAndEventType, traceData.getPc());
        if (deltaAndEventType == null
                || deltaAndEventType.getEventType() != CompactEventReader.Type.THREAD_BEGIN) {
            throw new InvalidTraceDataException("All traces should start with begin, this one starts with "
                    + (deltaAndEventType == null ? "a jump" : deltaAndEventType.getEventType())
                    + ".");
        }
        firstEvent = compactEventReader.begin(
                context, traceData.getThreadId().getAsLong(), traceData.getGeneration().getAsLong());
    }

    public List<CompactEvent> getNextEvents()
            throws InvalidTraceDataException, IOException {
        if (firstEvent != null) {
            List<CompactEvent> event = firstEvent;
            firstEvent = null;
            return event;
        }
        Address pc = new Address(traceHeader);
        if (!read("event identifier", pc, true)) {
            return null;
        }
        DeltaAndEventType deltaAndEventType =
                DeltaAndEventType.parseFromPC(minDeltaAndEventType, maxDeltaAndEventType, pc);
        if (deltaAndEventType == null) {
            return CompactEventReader.jump(context, pc.getAsLong());
        }
        // TODO: Handle this correctly when a new thread begins. Currently it updates the pc of the last thread.
        context.updatePcWithDelta(deltaAndEventType.getJumpDelta());
        return deltaAndEventType.getEventType().read(context, compactEventReader, traceHeader, inputStream);
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }

    private boolean read(String name, ReadableData data, boolean allowEof)
            throws IOException, InvalidTraceDataException {
        // TODO(virgil): I could optimize this and not create the array each time. On the other hand,
        // java is supposed to be good at optimizing these things.
        byte[] bytes = new byte[data.size()];
        if (bytes.length == 0) {
            throw new RuntimeException("Cannot read data of size 0.");
        }
        int readCount = inputStream.read(bytes);
        if (allowEof && readCount == -1) {
            return false;
        }
        if (bytes.length != readCount) {
            throw new InvalidTraceDataException("Short read while reading " + name
                    + ", wanted " + bytes.length + " bytes but got " + readCount + ".");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(traceHeader.getByteOrder());
        data.read(buffer);
        return true;
    }

    private static class DeltaAndEventType {
        private final CompactEventReader.Type eventType;
        private final int jumpDelta;

        private DeltaAndEventType(CompactEventReader.Type eventType, int jumpDelta) {
            this.eventType = eventType;
            this.jumpDelta = jumpDelta;
        }

        private static DeltaAndEventType parseFromPC(
                int minDeltaAndEventType, int maxDeltaAndEventType, Address address) {
            long pc = address.getAsLong();
            if (pc < minDeltaAndEventType || pc > maxDeltaAndEventType) {
                return null;
            }
            int eventCount = CompactEventReader.Type.getNumberOfValues();
            return new DeltaAndEventType(
                    CompactEventReader.Type.fromInt(toIntExact(pc % eventCount)),
                    toIntExact(pc / eventCount - eventCount / 2));
        }

        private CompactEventReader.Type getEventType() {
            return eventType;
        }

        private int getJumpDelta() {
            return jumpDelta;
        }
    }
}
