package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.EventReader;
import com.runtimeverification.rvpredict.log.IEventReader;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.InvalidTraceDataException;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Class for reading a trace from disk in the order given by event ids
 *
 * @author TraianSF
 */
public class OrderedLoggedTraceReader implements IEventReader {
    private final Configuration config;
    private ReadonlyEventInterface lastEvent;
    private PriorityQueue<OrderedEventReader> readers;

    public OrderedLoggedTraceReader(Configuration config) {
        this.config = config;
        try {
            setup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ReadonlyEventInterface readEvent() throws IOException {
        if (readers.isEmpty()) throw new EOFException();
        OrderedEventReader reader = readers.poll();
        lastEvent = reader.readEvent();
        if (reader.poll() != null) {
            readers.add(reader);
        } else {
            reader.close();
        }
        return lastEvent;
    }

    @Override
    public ReadonlyEventInterface lastReadEvent() {
        return lastEvent;
    }

    @Override
    public void close() throws IOException {
        Optional<IOException> exception = readers.stream().map(reader -> {
            try { reader.close(); } catch (IOException ex) { return Optional.of(ex); }
            return Optional.<IOException>empty();
        }).reduce(Optional.empty(), (e1,e2) -> e2.isPresent() ? e2 : e1);
        if (exception.isPresent()) throw exception.get();
    }

    class OrderedEventReader implements IEventReader, Comparable<OrderedEventReader> {
        IEventReader reader;
        ReadonlyEventInterface nextEvent;
        ReadonlyEventInterface lastEvent;

        OrderedEventReader(IEventReader reader) throws IOException {
            this.reader = reader;
            nextEvent = reader.readEvent();
        }

        @Override
        public ReadonlyEventInterface readEvent() throws IOException {
            if (nextEvent == null) throw new EOFException();
            lastEvent = nextEvent;
            try {
                nextEvent = reader.readEvent();
            }
            catch (IOException e) {
                nextEvent = null;
            }
            return lastEvent;
        }

        @Override
        public ReadonlyEventInterface lastReadEvent() {
            return lastEvent;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }

        @Override
        public int compareTo(OrderedEventReader orderedEventReader) {
            return Long.compare(nextEvent.getEventId(), orderedEventReader.nextEvent.getEventId());
        }

        public ReadonlyEventInterface poll() {
            return nextEvent;
        }
    }

    public void setup() throws IOException {
        readers = new PriorityQueue<>();
        int logFileId = 0;
        if (config.isCompactTrace()) {
            try {
                readers.add(new OrderedEventReader(new CompactEventReader(config.getCompactTraceFilePath())));
            } catch (InvalidTraceDataException e) {
                throw new IOException(e);
            }
            return;
        }
        while (true) {
            Path path = config.getTraceFilePath(logFileId++);
            if (!path.toFile().exists()) {
                break;
            }
            OrderedEventReader reader = new OrderedEventReader(new EventReader(path));
            if (reader.poll() != null) {
                readers.add(reader);
            } else {
                reader.close();
            }
        }
    }
}
