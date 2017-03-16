package com.runtimeverification.rvpredict.log.compact;

import java.io.*;
import java.nio.*;

public class TraceReader implements Closeable {
    InputStream inputStream = null;

    TraceHeader traceHeader;
    TraceData traceData;

    public TraceReader(String path) throws IOException, InvalidTraceDataException {
        File file = new File(path);
        inputStream = new BufferedInputStream(new FileInputStream(file));
        traceHeader = new TraceHeader(inputStream);
        traceHeader.checkValid();
        traceData = new TraceData(traceHeader);
        read(traceData);
    }

    public void read(ReadableData data) throws IOException, InvalidTraceDataException {
        byte[] bytes = new byte[data.size()];
        inputStream.read(bytes);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        data.read(buffer);
    }

    public Event getNextEvent() throws InvalidTraceDataException {
        Address pc = new Address(traceHeader);
        // TODO(virgil): What does deltop mean?
        if (addressIsNotDeltop(pc)) {
            // TODO(virgil): Update state.
            return Event.jump(pc);
        }
        Deltop deltop = new Deltop(pc);
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }

    private class Deltop {
        private final Event.Type eventType;
        private final int jmpvec;
    }
}
