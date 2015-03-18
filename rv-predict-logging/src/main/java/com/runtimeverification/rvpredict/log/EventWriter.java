package com.runtimeverification.rvpredict.log;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.runtimeverification.rvpredict.trace.EventType;

public class EventWriter {

    private final EventOutputStream out;

    private boolean isWriting;

    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public EventWriter(EventOutputStream out) {
        this.out = out;
    }

    public void write(long gid, long tid, int locId, int addrl, int addrr, long value,
            EventType eventType) throws IOException {
        if (isWriting) {
            throw new RuntimeException("This method is not supposed to be reentrant!");
        }

        if (shutdown.get()) {
            return;
        }

        isWriting = true;
        try {
            out.writeLong(gid);
            out.writeLong(tid);
            out.writeInt(locId);
            out.writeInt(addrl);
            out.writeInt(addrr);
            out.writeLong(value);
            out.writeByte(eventType.ordinal());
        } finally {
            isWriting = false;
        }
    }

    public void shutdown() throws IOException {
        shutdown.set(true);
        out.close();
    }

}
