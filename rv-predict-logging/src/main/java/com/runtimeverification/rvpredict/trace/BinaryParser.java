package com.runtimeverification.rvpredict.trace;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;

/**
 *
 * Utility class for parsing information out of files in binary format
 *
 * @author ericpts
 *
 */

public class BinaryParser implements Closeable {

    private final ByteBuffer byteBuffer = ByteBuffer.allocate(8).order(ByteOrder.nativeOrder());

    private BufferedInputStream in;

    public BinaryParser(File file) throws IOException {
        in = new BufferedInputStream(new FileInputStream(file));
    }

    public BinaryParser(Path path) throws IOException {
        this(path.toFile());
    }

    public final int readByte() throws IOException {
        int rd = in.read();
        if(rd == -1) {
            throw new EOFException();
        }
        return rd;
    }

    private final void putBytes(int n) throws IOException {
        for(int i = 0; i < n; ++i) {
            byteBuffer.put((byte)readByte());
        }
        byteBuffer.flip();
    }

    public final Long readLong() throws IOException {
        putBytes(Long.BYTES);
        Long ret = byteBuffer.getLong();
        byteBuffer.clear();
        return ret;
    }

    public final Integer readInt() throws IOException {
        putBytes(Integer.BYTES);
        Integer ret = byteBuffer.getInt();
        byteBuffer.clear();
        return ret;
    }

    public final String readString() throws IOException {
        StringBuilder sb = new StringBuilder();

        int rd = readByte();
        while(rd != 0) {
            char c = (char) rd;
            sb.append(c);
            rd = readByte();
        }

        return sb.toString();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
