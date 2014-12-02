package rvpredict.db;

import org.apache.tools.ant.DirectoryScanner;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Class adding a transparency layer between the prediction engine and the filesystem holding the trace log.
 * A trace log consists from a collection of files, each holding the events corresponding to
 * a thread
 * The file names end with {@link #TRACE_SUFFIX}, having as a prefix the unique id of the thread.
 * @author TraianSF
 */
public class TraceCache {
    public static final String TRACE_SUFFIX = "trace.bin";
    final Map<Long,Map.Entry<EventInputStream,EventItem>> indexes;
    private final long traceSize;

    /**
     * Creates a new cache structure for a trace log in a given directory
     * @param directory  location on filesystem where the trace log can be found
     */
    public TraceCache(String directory) {
        long traceSize = 0;
        String[] files = getTraceFiles(directory);
        indexes = new HashMap<>(files.length);
        for (String file : files) {
            try {
                File f = Paths.get(directory, file).toFile();
                traceSize += f.length();
                EventInputStream inputStream = new EventInputStream(
                       new BufferedInputStream( new FileInputStream(f)));
                EventItem event = inputStream.readEvent();
                indexes.put(event.GID, new AbstractMap.SimpleEntry<>(inputStream,event));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.traceSize = traceSize / EventItem.SIZEOF;
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
     * Returns the event whose unique identifier in the logged trace is given by {@code index}.
     * This method assumes the trace is read in sequential order, hence one of the keys
     * in the {@link #indexes} table is equal to {@code index}.
     * @param index  index of the event to be read
     * @return the event requested or <code>null</code>
     */
    public EventItem getEvent(long index) {
        Map.Entry<EventInputStream,EventItem> entry = indexes.remove(index);

        assert entry != null : "Index not (yet) available. Attempting to read events out of order?";
        EventItem event = entry.getValue();
        try {
            EventItem newEvent = entry.getKey().readEvent();
            entry.setValue(newEvent);
            indexes.put(newEvent.GID, entry);
        } catch (EOFException e) {
            // EOF is expected.
        } catch (IOException e) {
            e.printStackTrace();
        }
        return event;
    }

}
