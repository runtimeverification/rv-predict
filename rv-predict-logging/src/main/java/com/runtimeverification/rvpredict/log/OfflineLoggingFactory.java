package com.runtimeverification.rvpredict.log;

import com.runtimeverification.rvpredict.config.Configuration;
import org.apache.tools.ant.DirectoryScanner;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

/**
 * An implementation of the {@link LoggingFactory} interface used for
 * offline prediction.
 *
 * Metadata and events are written and read from files in the
 * {@link Configuration#outdir} directory.
 *
 * @author Traian SF
 */
public class OfflineLoggingFactory implements LoggingFactory {
    /**
     * termination for files holding events
     */
    public static final String TRACE_SUFFIX = "trace.bin";
    private final Configuration config;
    private Collection<EventReader> readers;
    private Iterator<EventReader> readersIter;

    public OfflineLoggingFactory(Configuration config) {
        this.config = config;
    }

    /**
     * The file names end with {@link OfflineLoggingFactory#TRACE_SUFFIX}, having as a prefix the unique
     * id of the thread generating them.
     */
    public static String[] getTraceFiles(String directory) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setIncludes(new String[]{"*" + TRACE_SUFFIX + "*"});
        scanner.setBasedir(directory);
        scanner.setCaseSensitive(false);
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    @Override
    public EventReader getEventReader() throws IOException {
        if (readers == null) {
            readers = new LinkedList<>();
            String[] files = getTraceFiles(config.getLogDir());
            for (String file : files) {
                EventReader reader = new EventReader(Paths.get(config.getLogDir(), file));
                readers.add(reader);
            }
        }
        if (readersIter == null) {
            readersIter = readers.iterator();
        }
        if (!readersIter.hasNext()) return null;
        return readersIter.next();
    }

}
