package rvpredict.logging;

import rvpredict.db.EventInputStream;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * {@link rvpredict.logging.SimpleEventPipe} factory.
 *
 * @author Traian SF
 */
public class OnlineLoggingFactory implements LoggingFactory {
    private static final PipedInputStream END_INPUT_STREAM = new PipedInputStream();
    private BlockingQueue<PipedInputStream> eventInputStreams = new LinkedBlockingQueue<>();
    
    @Override
    public EventPipe createEventPipe() {
        return new SimpleEventPipe();
    }

    @Override
    public ObjectOutputStream createMetadataOS() throws IOException {
        return new ObjectOutputStream(new PipedOutputStream());
    }

    @Override
    public EventOutputStream createEventOutputStream() throws IOException {
        final PipedOutputStream outputStream = new PipedOutputStream();
        eventInputStreams.add(new PipedInputStream(outputStream));
        return new EventOutputStream(outputStream);
    }

    @Override
    public void finishLogging() {
        eventInputStreams.add(END_INPUT_STREAM);
    }

    @Override
    public EventInputStream getInputStream() throws InterruptedException {
        PipedInputStream stream = eventInputStreams.take();
        if (stream == END_INPUT_STREAM) return null;
        return new EventInputStream(stream);
    }
}
