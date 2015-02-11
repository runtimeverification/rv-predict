package com.runtimeverification.rvpredict.log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Extension of the DataInputStream class specialized for Events
 * @author TraianSF
 */
public class EventInputStream extends DataInputStream {
    /**
     * Creates an EventInputStream that uses the specified
     * underlying InputStream.
     *
     * @param in the specified input stream
     */
    public EventInputStream(InputStream in) {
        super(in);
    }

    /**
     * Reads {@link EventItem#SIZEOF} bytes and returns an {@code EventItem}.
     *
     * @return     the {@code EventItem} read.
     * @exception java.io.EOFException  if this stream reaches the end before reading all the bytes.
     * @exception  IOException   if an I/O error occurs.
     */
    public EventItem readEvent() throws IOException {
        return EventItem.readFrom(this);
    }
}
