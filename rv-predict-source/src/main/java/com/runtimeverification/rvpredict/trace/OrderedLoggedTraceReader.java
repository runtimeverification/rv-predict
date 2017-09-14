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
import java.util.PriorityQueue;

public class OrderedLoggedTraceReader implements IEventReader {
    private final Configuration config;
    private ReadonlyEventInterface lastEvent;

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
        readers.forEach((reader) -> {
            try { reader.close(); } catch (IOException ignored) {}
        });
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
            return Long.signum(nextEvent.getEventId() - orderedEventReader.nextEvent.getEventId());
        }

        public ReadonlyEventInterface poll() {
            return nextEvent;
        }
    }

    private PriorityQueue<OrderedEventReader> readers;

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
