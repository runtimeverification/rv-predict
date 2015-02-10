package com.runtimeverification.rvpredict.log;

import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Interface for abstracting I/O operations
 * 
 * @author TraianSF
 */
public interface LoggingFactory {
    /**
     * Creates an {@link EventPipe} used for communicating events from
     * one (producer) thread to another (logging) thread
     * @see ThreadLocalEventStream#initialValue() for creation
     * @see LoggingEngine#saveEvent(com.runtimeverification.rvpredict.trace.EventType, int, long, int, long) for producer
     * @see Logger for consumer
     * @return a new {@link EventPipe}
     */
    EventPipe createEventPipe();

    /**
     * Creates a new stream for logging metadata.
     * @return a new {@link java.io.ObjectOutputStream} to save metadata into.
     * @see MetadataLogger
     * @throws IOException if the stream cannot be created.
     */
    ObjectOutputStream createMetadataOS() throws IOException;

    /**
     * Creates a new stream for logging events.
     * @return a new {@link EventOutputStream}
     * @throws IOException if stream cannot be created.
     */
    EventOutputStream createEventOutputStream() throws IOException;

    /**
     * method to signal to objects implementing this interface that logging is completed.
     */
    void finishLogging();

    /**
     * Retrieves an input stream associated to a stream logging events from an execution.
     * @return the next available {@link EventInputStream} or {@code null}
     *         if there will be no more streams available.
     * @throws InterruptedException if the thread was interrupted while 
     *         waiting for a stream to become available
     * @throws IOException If the next available stream cannot be open.
     */
    EventInputStream getInputStream() throws InterruptedException, IOException;

    /**
     * Metadata accessor: retrieves the statement signature given a location identifier
     */
    String getStmtSig(int locId);

    /**
     * Metadata accessor: tells whether the field identified by the argument is volatile or not 
     */
    boolean isVolatile(int fieldId);

    /**
     * Metadata accessor: retrieves the signature corresponding to a field identifier.
     */
    String getVarSig(int fieldId);
}
