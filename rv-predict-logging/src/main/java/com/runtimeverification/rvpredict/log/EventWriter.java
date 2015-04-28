package com.runtimeverification.rvpredict.log;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;

import com.runtimeverification.rvpredict.config.Configuration.OS;
import com.runtimeverification.rvpredict.trace.EventType;

public class EventWriter implements Closeable {

    public static final int COMPRESS_BLOCK_SIZE = 8 * 1024 * 1024; // 8MB

    private static final LZ4Compressor FAST_COMPRESSOR = LZ4Factory.fastestInstance().fastCompressor();

    private final LZ4BlockOutputStream out;

    private boolean isWriting;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private final ByteBuffer byteBuffer = ByteBuffer.allocate(Event.SIZEOF);

    public EventWriter(Path path) throws IOException {
        this.out = new LZ4BlockOutputStream(
            OS.current() == OS.WINDOWS ?
                    new BufferedChannelOutputStream(path) :
                    new MappedByteBufferOutputStream(path),
                COMPRESS_BLOCK_SIZE,
                FAST_COMPRESSOR);
    }

    public void write(long gid, long tid, int locId, int addrl, int addrr, long value,
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
                .putInt(addrl)
                .putInt(addrr)
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
