package rvpredict.db;

import org.apache.tools.ant.DirectoryScanner;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class adding a transparency layer between the prediction engine and the filesystem holding the trace log.
 * A trace log consists from a collection of files holding segments of the trace.
 * The file names end with {@link #TRACE_SUFFIX}, having as a prefix the unique id of the first event in the
 * segment contained in that file.
 * @author TraianSF
 */
public class TraceCache {
    public static final String TRACE_SUFFIX = "trace.bin";
    public static final int TRACE_SUFFIX_LENGTH = TRACE_SUFFIX.length();
    private final String directory;
    private final long[] indexes;
    private List<EventItem> cache;
    private long startIndex;
    private final long traceSize;

    /**
     * Creates a new cache structure for a trace log in a given directory
     * @param directory  location on filesystem where the trace log can be found
     */
    public TraceCache(String directory) {
        this.directory = directory;
        String[] files = getTraceFiles(directory);
        indexes = new long[files.length];
        int i = 0;
        for (String file : files) {
            indexes[i++] = Long.valueOf(file.substring(0, file.length() - TRACE_SUFFIX_LENGTH ));
        }
        Arrays.sort(indexes);
        rebaseCache(indexes[indexes.length - 1]);
        traceSize = startIndex + cache.size() - 1;
    }

    static String[] getTraceFiles(String directory) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setIncludes(new String[]{"*" + TRACE_SUFFIX});
        scanner.setBasedir(directory);
        scanner.setCaseSensitive(false);
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    /**
     * Cleans all preexisting trace files from the specified <code>directory</code>
     * @param directory
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

    public long getTraceSize() {
        return traceSize;
    }

    /**
     * Returns the event whose unique identifier in the logged trace is given by <code>index</code>.
     * If the event is not found in the current cache, the corresponding file containing it will
     * be loaded from the filesystem
     * @param index  index of the event to be read
     * @return the event requested or <code>null</code>
     */
    public EventItem getEvent(long index) {
        long loffset = index - startIndex;
        int offset;
        if (loffset >= cache.size() || loffset < 0) {
            offset = rebaseCache(index);
            if (offset < 0) return null;
        } else {
            offset = (int) loffset;
        }
        return cache.get(offset);
    }

    private int rebaseCache(long index) {
        int i = Arrays.binarySearch(indexes, index);
        if (i < 0) {
            i = - i - 2;
            if (i < 0) return -1;
        }
        String fname = indexes[i] + TRACE_SUFFIX;
        try {
            EventInputStream inputStream = new EventInputStream(
                    new BufferedInputStream( new FileInputStream(Paths.get(directory, fname).toFile())));
            cache = new ArrayList<>();
            while (true) {
                try {
                    cache.add(inputStream.readEvent());
                } catch (EOFException _) {
                    break;
                }
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        startIndex = indexes[i];
        long offset = index - startIndex;
        if (offset >= cache.size()) return -1;
        return (int) offset;
    }


}
