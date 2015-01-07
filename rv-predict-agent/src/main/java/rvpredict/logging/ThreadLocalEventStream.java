package rvpredict.logging;

import rvpredict.config.Configuration;
import rvpredict.db.EventOutputStream;
import rvpredict.db.TraceCache;

import java.io.*;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import rvpredict.db.EventOutputStream;
import rvpredict.db.TraceCache;

/**
 * Class extending {@link java.lang.ThreadLocal} to handle thread-local output
 * to {@link rvpredict.db.EventOutputStream} in a given directory.
 *
 * @author TraianSF
 */
public class ThreadLocalEventStream extends ThreadLocal<EventOutputStream> {
    private final String directory;
    private final ConcurrentHashMap<Integer,EventOutputStream> streamsMap = new ConcurrentHashMap<>();
    private final boolean zip;
    private static final AtomicInteger threadId = new AtomicInteger();

    /**
     * Accessor to the map of streams indexed by thread identifier
     * @return  a map containing all thread-local streams as values indexed by thread id.
     */
    public ConcurrentHashMap<Integer, EventOutputStream> getStreamsMap() {
        return streamsMap;
    }

    public ThreadLocalEventStream(Configuration config) {
        super();
        this.directory = config.outdir;
        this.zip = config.zip;
    }

    @Override
    protected EventOutputStream initialValue() {
        try {
            int id = threadId.incrementAndGet();
            OutputStream outputStream = new FileOutputStream(Paths.get(directory,
                    id + "_" + TraceCache.TRACE_SUFFIX
                            + (zip?TraceCache.ZIP_EXTENSION:"")).toFile());
            if (zip) {
                outputStream = new GZIPOutputStream(outputStream,true);
            }
            EventOutputStream eventOutputStream = new EventOutputStream(new BufferedOutputStream(
                    outputStream), id);
            streamsMap.put(id,eventOutputStream);
            return eventOutputStream;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) { // GZIPOutputStream exception
            e.printStackTrace();
        }
        return null;
    }

}
