package rvpredict.logging;

import rvpredict.config.Configuration;
import rvpredict.db.EventInputStream;
import rvpredict.db.TraceCache;

import java.io.*;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

/**
 * {@link rvpredict.logging.BufferedEventPipe} factory.
 *
 * @author Traian SF
 */
public class OfflineLoggingFactory implements LoggingFactory {
    private static final AtomicInteger logFileId = new AtomicInteger();
    private final Configuration config;
    private Collection<EventInputStream> inputStreams;
    private Iterator<EventInputStream> inputStreamsIterator;

    public OfflineLoggingFactory(Configuration config) {
        this.config = config;
    }

    public EventOutputStream createEventOutputStream() throws IOException {
        int id = logFileId.incrementAndGet();
        OutputStream outputStream = new FileOutputStream(Paths.get(config.outdir,
                id + "_" + TraceCache.TRACE_SUFFIX
                        + (config.zip ? TraceCache.ZIP_EXTENSION : "")).toFile());
        if (config.zip) {
            outputStream = new GZIPOutputStream(outputStream,true);
        }
        EventOutputStream eventOutputStream = new EventOutputStream(new BufferedOutputStream(
                outputStream));
        return eventOutputStream;
    }

    @Override
    public void finishLogging() {
    }

    @Override
    public EventInputStream getInputStream() throws InterruptedException {
        if (inputStreams == null) {
//                    String[] files = getTraceFiles(directory);
//        indexes = new HashMap<>(files.length);
//        for (String file : files) {
//            try {
//                File f = Paths.get(directory, file).toFile();
//                InputStream in = new FileInputStream(f);
//                if (file.endsWith(ZIP_EXTENSION)) {
//                    in = new GZIPInputStream(in);
//                }
//                EventInputStream inputStream = new EventInputStream(
//                       new BufferedInputStream(in));
//
        }
        if (inputStreamsIterator == null) {
            inputStreamsIterator = inputStreams.iterator();
        }
        if (!inputStreamsIterator.hasNext()) return null;
        return inputStreamsIterator.next();
    }

    @Override
    public ObjectOutputStream createMetadataOS() throws IOException {
        return new ObjectOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream(Paths.get(config.outdir, rvpredict.db.DBEngine.METADATA_BIN).toFile())));
    }

    @Override
    public EventPipe createEventPipe() {
        return new BufferedEventPipe();
    }
}
