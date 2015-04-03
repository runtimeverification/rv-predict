package com.runtimeverification.rvpredict.log;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.metadata.Metadata;

import org.apache.tools.ant.DirectoryScanner;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
    public static final String METADATA_BIN = "metadata.bin";
    private static final AtomicInteger logFileId = new AtomicInteger();
    private final Configuration config;
    private Collection<EventReader> readers;
    private Iterator<EventReader> readersIter;
    private Metadata metadata;

    public OfflineLoggingFactory(Configuration config) {
        this(config, false);
    }

    public OfflineLoggingFactory(Configuration config, boolean isWrite) {
        this.config = config;
        if (!isWrite) {
            readMetadata();
        }
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
    public EventWriter createEventWriter() throws IOException {
        int id = logFileId.incrementAndGet();
        Path path = Paths.get(config.getLogDir(), id + "_" + TRACE_SUFFIX);
        return new EventWriter(path);
    }

    @Override
    public void finishLogging() {
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

    @Override
    public String getStmtSig(int locId) {
        return metadata.getLocationSig(locId);
    }

    @Override
    public boolean isVolatile(int fieldId) {
        return metadata.isVolatile(fieldId);
    }

    @Override
    public String getVarSig(int fieldId) {
        return metadata.getVariableSig(fieldId);
    }

    private void readMetadata() {
        try (ObjectInputStream metadataIS = new ObjectInputStream(new BufferedInputStream(
                new FileInputStream(Paths.get(config.getLogDir(), METADATA_BIN).toFile())))) {
            metadata = (Metadata) metadataIS.readObject();
        } catch (FileNotFoundException e) {
            System.err.println("Error: Metadata file not found.");
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (ClassNotFoundException | IOException e) {
            System.err.println("Error: Metadata for the logged execution is corrupted.");
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public ObjectOutputStream createMetadataOS() throws IOException {
        return new ObjectOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream(Paths.get(config.getLogDir(), METADATA_BIN).toFile())));
    }

}
