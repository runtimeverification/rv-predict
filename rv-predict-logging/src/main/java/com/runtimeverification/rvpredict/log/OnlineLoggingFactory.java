package com.runtimeverification.rvpredict.log;

import com.runtimeverification.rvpredict.metadata.Metadata;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A {@link LoggingFactory} for online prediction.
 *
 * Using a {@link SimpleEventPipe}, to make data available asap to prediction
 *
 * Whenever an {@link EventOutputStream} is requested, it is created based
 * on an {@link java.io.PipedOutputStream}. At the same time, a {@link java.io.PipedInputStream}
 * is created and queued.  These are later used as a base for an {@link EventInputStream} are
 * whenever one is requested through {@link #getInputStream()}
 *
 * @author TraianSF
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
        throw new UnsupportedOperationException("Not implemented for online prediction");
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

    @Override
    public String getStmtSig(int locId) {
        return Metadata.locIdToStmtSig.get(locId);
    }

    @Override
    public boolean isVolatile(int fieldId) {
        return Metadata.volatileVariableIds.contains(fieldId);
    }

    @Override
    public String getVarSig(int fieldId) {
        return Metadata.varSigs.get(fieldId);
    }

}
