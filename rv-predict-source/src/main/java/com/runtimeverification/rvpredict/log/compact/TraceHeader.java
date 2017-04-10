package com.runtimeverification.rvpredict.log.compact;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class TraceHeader {
    private final ByteOrder byteOrder;
    private final int pointerWidthInBytes;
    private final int defaultDataWidthInBytes;

    public TraceHeader(InputStream stream) throws IOException, InvalidTraceDataException {
        byte[] magic = read("magic", stream, 4);
        checkBytesValue("magic", magic, "RVP_");

        // TODO(virgil): The program must give up if it does not recognize
        // the version number, but it can't do that before knowing the byte order.
        // For now, I'll assume that the file contains the byte order regardless
        // of version.
        byte[] versionNumberBytes = read("version number", stream, 4);

        byte[] byteOrderBytes = read("byte order", stream, 4);
        if (byteOrderBytes[0] == (int)'0') {
            checkBytesValue("byte order", byteOrderBytes, "0123");
            byteOrder = ByteOrder.LITTLE_ENDIAN;
        } else {
            checkBytesValue("byte order", byteOrderBytes, "3210");
            byteOrder = ByteOrder.BIG_ENDIAN;
        }

        int versionNumber = ByteBuffer.wrap(versionNumberBytes)
                .order(byteOrder).getInt();
        if (versionNumber != 0) {
            throw new InvalidTraceDataException("Unknown version: " + versionNumber);
        }

        byte[] otherBytes = read("header end", stream, 4);
        pointerWidthInBytes = otherBytes[0];
        defaultDataWidthInBytes = otherBytes[1];
    }

    public int getPointerWidthInBytes() {
        return pointerWidthInBytes;
    }

    private static void checkBytesValue(
            String name, byte[] bytes, String expected) throws InvalidTraceDataException {
        byte[] expectedBytes = expected.getBytes();
        if (!Arrays.equals(bytes, expectedBytes)) {
            throw new InvalidTraceDataException(
                    "Invalid " + name + ": actual=" + Arrays.toString(bytes)
                    + " expected=" + Arrays.toString(expectedBytes) + "(" + expected + ").");
        }
    }

    private static byte[] read(
            String description, InputStream stream, int count)
            throws IOException, InvalidTraceDataException {
        byte[] bytes = new byte[count];
        if (count != stream.read(bytes)) {
            throw new InvalidTraceDataException("Short read for " + description + ".");
        }
        return bytes;
    }

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public int getDefaultDataWidthInBytes() {
        return defaultDataWidthInBytes;
    }
}
