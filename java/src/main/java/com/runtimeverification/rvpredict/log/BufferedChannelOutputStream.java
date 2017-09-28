package com.runtimeverification.rvpredict.log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

/**
 * Alternative to {@link MappedByteBufferOutputStream} on Windows machine
 * because Windows does not work well with {@link MappedByteBuffer}.
 *
 * @author YilongL
 *
 */
public class BufferedChannelOutputStream extends OutputStream {

    private static final int PAGE_SIZE = 4096;

    private final RandomAccessFile file;

    private final FileChannel channel;

    private final ByteBuffer buffer = ByteBuffer.allocate(PAGE_SIZE).order(ByteOrder.nativeOrder());

    public BufferedChannelOutputStream(Path path) throws FileNotFoundException {
        file = new RandomAccessFile(path.toFile(), "rw");
        channel = file.getChannel();
    }

    @Override
    public void write(int b) throws IOException {
        buffer.put((byte) b);
        if (!buffer.hasRemaining()) {
            flush();
        }
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        while (true) {
            int bytes = Math.min(len, buffer.remaining());
            buffer.put(b, off, bytes);
            if (!buffer.hasRemaining()) {
                flush();
                len -= bytes;
                off += bytes;
            } else {
                assert len == 0;
                return;
            }
        }
    }

    @Override
    public void flush() throws IOException {
        buffer.flip(); // set to read mode
        channel.write(buffer);
        buffer.clear();
    }

    @Override
    public void close() throws IOException {
        flush();
        channel.close();
        file.close();
    }

}
