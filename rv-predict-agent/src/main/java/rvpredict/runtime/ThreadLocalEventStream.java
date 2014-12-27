package rvpredict.runtime;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Paths;

import rvpredict.db.EventOutputStream;
import rvpredict.db.TraceCache;
import rvpredict.runtime.bootstrap.java.util.concurrent.ConcurrentHashMap;

/**
 * Class extending {@link java.lang.ThreadLocal} to handle thread-local output
 * to {@link rvpredict.db.EventOutputStream} in a given directory.
 *
 * @author TraianSF
 */
public class ThreadLocalEventStream extends ThreadLocal<EventOutputStream> {
    private final String directory;
    private final ConcurrentHashMap<Long,EventOutputStream> streamsMap = new ConcurrentHashMap<>();

    /**
     * Accessor to the map of streams indexed by thread identifier
     * @return  a map containing all thread-local streams as values indexed by thread id.
     */
    public ConcurrentHashMap<Long, EventOutputStream> getStreamsMap() {
        return streamsMap;
    }

    /**
     * Class constructor initializing the directory
     * @param directory  location where the thread local event streams should be saved to
     */
    public ThreadLocalEventStream(String directory) {
        super();
        this.directory = directory;
    }

    @Override
    protected EventOutputStream initialValue() {
        try {
            EventOutputStream newTraceOs = new EventOutputStream(new BufferedOutputStream(
                    new FileOutputStream(Paths.get(directory,
                            Thread.currentThread().getId() + "_"
                                    +TraceCache.TRACE_SUFFIX).toFile())));
            streamsMap.put(Thread.currentThread().getId(),newTraceOs);
            return newTraceOs;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

}
