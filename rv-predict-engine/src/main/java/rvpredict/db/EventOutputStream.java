package rvpredict.db;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Extension of the DataOutputStream class specialized for {@link rvpredict.db.EventItem}
 * @author TraianSF
 *
 */
public class EventOutputStream extends DataOutputStream {

    /**
     * Creates a new event output stream to write events to the specified
     * underlying output stream. The counter <code>written</code> is
     * set to zero.
     *
     * @param out the underlying output stream, to be saved for later
     *            use.
     * @see java.io.FilterOutputStream#out
     */
    public EventOutputStream(OutputStream out) {
        super(out);
        eventsWrittenCount = 0;
    }

    /**
     * Writes an {@link rvpredict.db.EventItem} to the underlying output stream.
     *
     * @param      event   an {@link rvpredict.db.EventItem} to be written.
     *                     If no exception is thrown, the counter
     *                     <code>written</code> is incremented by
     *                     {@link rvpredict.db.EventItem#SIZEOF}.
     * @exception  IOException  if an I/O error occurs.
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
        eventsWrittenCount++;
    }

    public long getEventsWrittenCount() {
        return eventsWrittenCount;
    }

    private long eventsWrittenCount;
}
