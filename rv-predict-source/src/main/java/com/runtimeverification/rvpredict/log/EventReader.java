package com.runtimeverification.rvpredict.log;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import net.jpountz.lz4.LZ4BlockInputStream;

/**
 * An event input stream lets an application to read {@link Event} from an
 * underlying input stream in a portable way.
 *
 * @author TraianSF
 * @author YilongL
 */
public class EventReader implements IEventReader {

    private final LZ4BlockInputStream in;

    private final ByteBuffer byteBuffer = ByteBuffer.allocate(Event.SIZEOF);

    private Event lastReadEvent;

    public EventReader(Path path) throws IOException {
        in = LZ4Utils.createDecompressionStream(path);
        readEvent();
    }

    @Override
    public Event readEvent() throws IOException {
        int bytes;
        int off = 0;
        int len = Event.SIZEOF;
        while ((bytes = in.read(byteBuffer.array(), off, len)) != len) {
            if (bytes == -1) {
                lastReadEvent = null;
                throw new EOFException();
            }
            off += bytes;
            len -= bytes;
        }
        lastReadEvent = new Event(
                byteBuffer.getLong(),
                byteBuffer.getLong(),
                byteBuffer.getInt(),
                byteBuffer.getLong(),
                byteBuffer.getLong(),
                EventType.values()[byteBuffer.get()]);
        byteBuffer.clear();
        return lastReadEvent;
    }

    @Override
    public Event lastReadEvent() {
        return lastReadEvent;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

}
