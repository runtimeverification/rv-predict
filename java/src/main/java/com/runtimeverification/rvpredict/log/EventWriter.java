package com.runtimeverification.rvpredict.log;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import net.jpountz.lz4.LZ4BlockOutputStream;

public class EventWriter implements Closeable {

    private final LZ4BlockOutputStream out;

    private boolean isWriting;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private final ByteBuffer byteBuffer = ByteBuffer.allocate(Event.SIZEOF);

    public EventWriter(Path path) throws IOException {
        this.out = LZ4Utils.createCompressionStream(path);
    }

    public void write(long gid, long tid, int locId, long addr, long value,
            EventType eventType) throws IOException {
        if (isWriting) {
            throw new RuntimeException("This method is not supposed to be reentrant!");
        }

        if (closed.get()) {
            return;
        }

        isWriting = true;
        try {
            byteBuffer.putLong(gid)
                .putLong(tid)
                .putInt(locId)
                .putLong(addr)
                .putLong(value)
                .put((byte) eventType.ordinal());
            out.write(byteBuffer.array());
            byteBuffer.clear();
        } finally {
            isWriting = false;
        }
    }

    @Override
    public void close() throws IOException {
        closed.set(true);
        LockSupport.parkNanos(1);
        out.close();
    }

}
