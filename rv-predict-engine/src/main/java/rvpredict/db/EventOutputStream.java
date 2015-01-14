package rvpredict.db;

import rvpredict.config.Configuration;

import java.io.*;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

/**
 * Extension of the DataOutputStream class specialized for {@link rvpredict.db.EventItem}
 * @author TraianSF
 *
 */
public class EventOutputStream {

    private static final AtomicInteger threadId = new AtomicInteger();
    private static final ConcurrentHashMap<Integer,EventOutputStream> streamsMap = new ConcurrentHashMap<>();
    private final DataOutputStream dataOutputStream;

    /**
     * Creates a new event output stream.
     * THe thread counter is incremented.
     * The counter <code>eventsWrittenCount</code> is set to zero.
     * The new output stream is registered into a map.
     *
     * @param config The configuration options for this logging session
     * @see java.io.FilterOutputStream#out
     */
    public EventOutputStream(Configuration config) {
        DataOutputStream dataOutputStream = null;
        try {
            int id = threadId.incrementAndGet();
            OutputStream outputStream = new FileOutputStream(Paths.get(config.outdir,
                    id + "_" + TraceCache.TRACE_SUFFIX
                            + (config.zip ? TraceCache.ZIP_EXTENSION : "")).toFile());
            if (config.zip) {
                outputStream = new GZIPOutputStream(outputStream,true);
            }
            dataOutputStream = new DataOutputStream(new BufferedOutputStream(
                    outputStream));
            streamsMap.put(id,this);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) { // GZIPOutputStream exception
            e.printStackTrace();
        }
        this.dataOutputStream = dataOutputStream;
    }

    /**
     * Accessor to the map of streams indexed by thread identifier
     * @return  a map containing all thread-local streams as values indexed by thread id.
     */
    public static ConcurrentHashMap<Integer, EventOutputStream> getStreamsMap() {
        return streamsMap;
    }

    /**
     * Writes an {@link rvpredict.db.EventItem} to the underlying output stream.
     *
     * @param      event   an {@link rvpredict.db.EventItem} to be written.
     *                     If no exception is thrown, the counter
     *                     <code>eventsWrittenCount</code> is incremented by
     *                     {@link rvpredict.db.EventItem#SIZEOF}.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public final void writeEvent(EventItem event) throws IOException {
        dataOutputStream.writeLong(event.GID);
        dataOutputStream.writeLong(event.TID);
        dataOutputStream.writeInt(event.ID);
        dataOutputStream.writeLong(event.ADDRL);
        dataOutputStream.writeLong(event.ADDRR);
        dataOutputStream.writeLong(event.VALUE);
        dataOutputStream.writeByte(event.TYPE.ordinal());
        eventsWrittenCount++;
    }

    public long getEventsWrittenCount() {
        return eventsWrittenCount;
    }

    private long eventsWrittenCount = 0;

    public void flush() throws IOException {
        dataOutputStream.flush();
    }

    public void close() throws IOException {
        dataOutputStream.close();
    }
}
