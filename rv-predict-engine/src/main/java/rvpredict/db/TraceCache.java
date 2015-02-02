package rvpredict.db;

import rvpredict.logging.LoggingFactory;

import java.io.EOFException;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Class adding a transparency layer between the prediction engine and the
 * filesystem holding the trace log.
 * A trace log consists from a collection of streams, each holding all the events
 * corresponding to a single thread.
 * @author TraianSF
 */
public class TraceCache {
    private final Map<Long,Map.Entry<EventInputStream,EventItem>> indexes;
    private final LoggingFactory loggingFactory;

    /**
     * Creates a new {@code TraceCahce} structure for a trace log.
     *
     * @param loggingFactory suppling additional information about the nature of the logs.
     */
    public TraceCache(LoggingFactory loggingFactory) {
        this.loggingFactory = loggingFactory;
        indexes = new HashMap<>();
    }

    /**
     * Returns the event whose unique identifier in the logged
     * trace is given by {@code index}.
     * This method assumes the trace is read in sequential order,
     * hence one of the keys in the {@link #indexes} table is equal
     * to {@code index}.
     * Moreover, it is assumed that {@code index < traceSize}.
     * @param index  index of the event to be read
     * @return the event requested
     */
    public EventItem getEvent(long index) throws IOException, InterruptedException {
        if (!indexes.containsKey(index)) {
            updateIndexes(index);
        }
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

    private void updateIndexes(long index) throws IOException, InterruptedException {
        EventItem event;
        do {
            EventInputStream inputStream = loggingFactory.getInputStream();
            event = inputStream.readEvent();
            indexes.put(event.GID, new AbstractMap.SimpleEntry<>(inputStream, event));
        } while (event.GID != index);
    }

}
