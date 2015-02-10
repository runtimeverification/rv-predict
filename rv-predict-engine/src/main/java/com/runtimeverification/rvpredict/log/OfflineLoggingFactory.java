package com.runtimeverification.rvpredict.log;

import com.runtimeverification.rvpredict.config.Configuration;
import org.apache.tools.ant.DirectoryScanner;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * An implementation of the {@link LoggingFactory} interface used for
 * offline prediction.
 * 
 * Metadata and events are written and read from files in the 
 * {@link com.runtimeverification.rvpredict.config.Configuration#outdir} directory.
 *
 * @author Traian SF
 */
public class OfflineLoggingFactory implements LoggingFactory {
    /**
     * termination for files holding events
     */
    public static final String TRACE_SUFFIX = "trace.bin";
    public static final String ZIP_EXTENSION = ".gz";
    public static final String METADATA_BIN = "metadata.bin";
    private static final AtomicInteger logFileId = new AtomicInteger();
    private final Configuration config;
    private Collection<EventInputStream> inputStreams;
    private Iterator<EventInputStream> inputStreamsIterator;
    private final Set<Integer> volatileFieldIds = new HashSet<>();
    private final Map<Integer, String> varIdToVarSig = new HashMap<>();
    private final Map<Integer, String> locIdToStmtSig = new HashMap<>();

    public OfflineLoggingFactory(Configuration config) {
        this.config = config;
    }

    /**
     * The file names end with {@link OfflineLoggingFactory#TRACE_SUFFIX}, having as a prefix the unique
     * id of the thread generating them.
     */
    private static String[] getTraceFiles(String directory) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setIncludes(new String[]{"*" + TRACE_SUFFIX + "*"});
        scanner.setBasedir(directory);
        scanner.setCaseSensitive(false);
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    /**
     * Cleans all preexisting trace files from the specified <code>directory</code>
     */
    public static void removeTraceFiles(String directory) {
        for (String fname : getTraceFiles(directory)) {
            try {
                Files.delete(Paths.get(directory, fname));
            } catch (IOException e) {
                System.err.println("Cannot delete trace file " + fname + "from dir. " + directory);
            }
        }
    }

    @Override
    public EventOutputStream createEventOutputStream() throws IOException {
        int id = logFileId.incrementAndGet();
        OutputStream outputStream = new FileOutputStream(Paths.get(config.outdir,
                id + "_" + TRACE_SUFFIX
                        + (config.zip ? ZIP_EXTENSION : "")).toFile());
        if (config.zip) {
            outputStream = new GZIPOutputStream(outputStream,true);
        }
        return new EventOutputStream(new BufferedOutputStream(
                outputStream));
    }

    @Override
    public void finishLogging() {
    }

    @Override
    public EventInputStream getInputStream() throws InterruptedException, IOException {
        if (inputStreams == null) {
            inputStreams = new LinkedList<>();
            String[] files = getTraceFiles(config.outdir);
            for (String file : files) {
                File f = Paths.get(config.outdir, file).toFile();
                InputStream in = new FileInputStream(f);
                if (file.endsWith(ZIP_EXTENSION)) {
                    in = new GZIPInputStream(in);
                }
                EventInputStream inputStream = new EventInputStream(
                        new BufferedInputStream(in));
                inputStreams.add(inputStream);
            }
        }
        if (inputStreamsIterator == null) {
            inputStreamsIterator = inputStreams.iterator();
        }
        if (!inputStreamsIterator.hasNext()) return null;
        return inputStreamsIterator.next();
    }

    @Override
    public String getStmtSig(int locId) {
        if (locIdToStmtSig.isEmpty()) readMetadata();
        return locIdToStmtSig.get(locId);
    }

    @Override
    public boolean isVolatile(int fieldId) {
        if (volatileFieldIds.isEmpty()) readMetadata();
        return volatileFieldIds.contains(fieldId);
    }

    @Override
    public String getVarSig(int fieldId) {
        if (varIdToVarSig.isEmpty()) readMetadata();
        return varIdToVarSig.get(fieldId);
    }

    @SuppressWarnings("unchecked")
    public void readMetadata() {
        try {
            ObjectInputStream metadataIS = new ObjectInputStream(new BufferedInputStream(
                    new FileInputStream(Paths.get(config.outdir, METADATA_BIN).toFile())));
            List<Map.Entry<Integer, String>> list;
            while (true) {
                try {
                    volatileFieldIds.addAll((Collection<Integer>) metadataIS.readObject());
                } catch (EOFException e) {
                    break;
                }
                list = (List<Map.Entry<Integer, String>>) metadataIS.readObject();
                for (Map.Entry<Integer, String> entry : list) {
                    varIdToVarSig.put(entry.getKey(), entry.getValue());
                }
                list = (List<Map.Entry<Integer, String>>) metadataIS.readObject();
                for (Map.Entry<Integer, String> entry : list) {
                    locIdToStmtSig.put(entry.getKey(), entry.getValue());
                }
            }
            return;
        } catch (FileNotFoundException e) {
            System.err.println("Error: Metadata file not found.");
            System.err.println(e.getMessage());
        } catch (ClassNotFoundException | IOException e) {
            System.err.println("Error: Metadata for the logged execution is corrupted.");
            System.err.println(e.getMessage());
        }
        System.exit(1);
    }

    @Override
    public ObjectOutputStream createMetadataOS() throws IOException {
        return new ObjectOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream(Paths.get(config.outdir, METADATA_BIN).toFile())));
    }

    @Override
    public EventPipe createEventPipe() {
        return new BufferedEventPipe();
    }


}
