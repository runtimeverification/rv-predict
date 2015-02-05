package rvpredict.logging;

import rvpredict.db.EventItem;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Extension of the DataOutputStream class specialized for Events
 * This class is used for writing events to disk.
 * It is created by a {@link rvpredict.logging.LoggingServer} and made available
 * to a unique {@link rvpredict.logging.LoggerThread} object.
 * Hence, a single thread would be using a class object for its entire duration.
 *
 * @author TraianSF
 */
public class EventOutputStream extends DataOutputStream {
    private static final int FLUSH_LIMIT = 1024*1024;
    private int flushCount = 0;

    /**
     * Creates an EventOutputStream that uses the specified
     * underlying OutputStream.
     *
     * @param in the specified input stream
     */
    public EventOutputStream(OutputStream in) {
        super(in);
    }

    /**
     * Writes an {@link rvpredict.db.EventItem} to the underlying output stream.
     * Maintains a counter  of events written from the previous flush and flushes if
     * {@link rvpredict.logging.EventOutputStream#FLUSH_LIMIT} is reached.
     *
     * @param      event   an {@link rvpredict.db.EventItem} to be written.
     * @exception  java.io.IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public final void writeEvent(EventItem event) throws IOException {
        writeLong(event.GID);
        writeLong(event.TID);
        writeInt(event.ID);
        writeLong(event.ADDRL);
        writeInt(event.ADDRR);
        writeLong(event.VALUE);
        writeByte(event.TYPE.ordinal());
        if (++flushCount >= FLUSH_LIMIT) {
            flush();
            flushCount = 0;
        }
    }
}
