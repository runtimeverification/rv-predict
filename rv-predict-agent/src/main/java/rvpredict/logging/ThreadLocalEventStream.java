package rvpredict.logging;

import rvpredict.db.EventOutputStream;
import rvpredict.db.TraceCache;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Traian on 26.11.2014.
 */
public class ThreadLocalEventStream extends ThreadLocal<EventOutputStream> {
    public ConcurrentHashMap<Long, EventOutputStream> getStreamsMap() {
        return streamsMap;
    }

    private final ConcurrentHashMap<Long,EventOutputStream> streamsMap = new ConcurrentHashMap<>();

    public ThreadLocalEventStream(String directory) {
        super();
        this.directory = directory;
    }

    @Override
    protected EventOutputStream initialValue() {
        EventOutputStream newTraceOs = getNewTraceOs(1);
        streamsMap.put(Thread.currentThread().getId(),newTraceOs);
        return newTraceOs;
    }

    @Override
    public void set(EventOutputStream value) {
        super.set(value);
        streamsMap.put(Thread.currentThread().getId(),value);
    }

    private final String directory;

    public EventOutputStream getNewTraceOs(long gid) {
        try {
            return new EventOutputStream(new BufferedOutputStream(
                    new FileOutputStream(Paths.get(directory,
                            Thread.currentThread().getId() + "_"
                                    + gid + "_"
                                    +TraceCache.TRACE_SUFFIX).toFile())));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
