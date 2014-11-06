package rvpredict.db;

import org.apache.tools.ant.DirectoryScanner;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Created by Traian on 04.11.2014.
 */
public class TraceCache {
    public static final String TRACE_SUFFIX = "trace.gz";
    public static final int TRACE_SUFFIX_LENGTH = TRACE_SUFFIX.length();
    private final String directory;
    long[] indexes;
    List<EventItem> cache;
    long startIndex;
    long traceSize;

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
            ObjectInputStream gzipInputStream = new ObjectInputStream(
                    new GZIPInputStream( new FileInputStream(Paths.get(directory, fname).toFile())));
            cache = (List<EventItem>) gzipInputStream.readObject();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
        startIndex = indexes[i];
        long offset = index - startIndex;
        if (offset >= cache.size()) return -1;
        return (int) offset;
    }


}
