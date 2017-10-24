package com.runtimeverification.rvpredict.log.compact;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.Arrays;

public class TraceHeader {
    private final ByteOrder byteOrder;
    private final int pointerWidthInBytes;
    private final int defaultDataWidthInBytes;

    public TraceHeader(InputStream stream) throws IOException, InvalidTraceDataException {
        byte[] magic = read4Bytes("magic", stream);
        checkBytesValue("magic", magic, "RVP_");

        // The version number is made of 4 bytes, major, minor, teeny, tiny, in this order.
        byte[] versionNumberBytes = read4Bytes("version number", stream);
        if (versionNumberBytes[0] != 0
                || versionNumberBytes[1] != 0
                || versionNumberBytes[2] != 0
                || versionNumberBytes[3] != 3) {
            throw new InvalidTraceDataException("Unknown version: " + Arrays.toString(versionNumberBytes));
        }

        byte[] byteOrderBytes = read4Bytes("byte order", stream);
        if (byteOrderBytes[0] == (int)'0') {
            checkBytesValue("byte order", byteOrderBytes, "0123");
            byteOrder = ByteOrder.LITTLE_ENDIAN;
        } else {
            checkBytesValue("byte order", byteOrderBytes, "3210");
            byteOrder = ByteOrder.BIG_ENDIAN;
        }

        byte[] otherBytes = read4Bytes("header end", stream);
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

    private static byte[] read4Bytes(String description, InputStream stream)
            throws IOException, InvalidTraceDataException {
        byte[] bytes = new byte[4];
        if (4 != stream.read(bytes)) {
            throw new InvalidTraceDataException("Short read for " + description + ".");
        }
        return bytes;
    }

    ByteOrder getByteOrder() {
        return byteOrder;
    }

    public int getDefaultDataWidthInBytes() {
        return defaultDataWidthInBytes;
    }
}
