package rvpredict.logging;

import rvpredict.config.Configuration;
import rvpredict.db.EventOutputStream;
import rvpredict.db.TraceCache;

import java.io.*;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
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
    private final ConcurrentHashMap<Long,EventOutputStream> streamsMap = new ConcurrentHashMap<>();
    private final boolean zip;

    /**
     * Accessor to the map of streams indexed by thread identifier
     * @return  a map containing all thread-local streams as values indexed by thread id.
     */
    public ConcurrentHashMap<Long, EventOutputStream> getStreamsMap() {
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
            OutputStream outputStream = new FileOutputStream(Paths.get(directory,
                    Thread.currentThread().getId() + "_" + TraceCache.TRACE_SUFFIX
                            + (zip?TraceCache.ZIP_EXTENSION:"")).toFile());
            if (zip) {
                outputStream = new GZIPOutputStream(outputStream,true);
            }
            EventOutputStream eventOutputStream = new EventOutputStream(new BufferedOutputStream(
                    outputStream));
            streamsMap.put(Thread.currentThread().getId(),eventOutputStream);
            return eventOutputStream;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) { // GZIPOutputStream exception
            e.printStackTrace();
        }
        return null;
    }

}
