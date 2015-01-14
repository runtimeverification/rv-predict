package rvpredict.logging;

import rvpredict.config.Configuration;
import rvpredict.db.EventOutputStream;
import rvpredict.db.TraceCache;

import java.io.*;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

/**
 * Class extending {@link java.lang.ThreadLocal} to handle thread-local output
 * to {@link rvpredict.db.EventOutputStream} in a given directory.
 *
 * @author TraianSF
 */
public class ThreadLocalEventStream extends ThreadLocal<EventOutputStream> {
    private final Configuration config;

    public ThreadLocalEventStream(Configuration config) {
        super();
        this.config = config;
    }

    @Override
    protected EventOutputStream initialValue() {
        return new EventOutputStream(config);
   }

}
