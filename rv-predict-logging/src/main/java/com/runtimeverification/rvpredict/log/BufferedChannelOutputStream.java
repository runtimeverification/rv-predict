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
            buffer.flip();
            channel.write(buffer);
            buffer.clear();
        }
    }

    // TODO(TraianSF): check if overriding write(byte[],int,int) will further improve performance

    @Override
    public void flush() { }

    @Override
    public void close() throws IOException {
        buffer.flip();
        channel.write(buffer);
        channel.close();
        file.close();
    }

}
