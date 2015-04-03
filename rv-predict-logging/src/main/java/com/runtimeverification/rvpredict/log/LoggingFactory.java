package com.runtimeverification.rvpredict.log;

import java.io.IOException;

/**
 * Interface for abstracting I/O operations
 *
 * @author TraianSF
 */
public interface LoggingFactory {

    /**
     * method to signal to objects implementing this interface that logging is completed.
     */
    void finishLogging();

    /**
     * Retrieves an input stream associated to a stream logging events from an execution.
     * @return the next available {@link EventReader} or {@code null}
     *         if there will be no more streams available.
     * @throws InterruptedException if the thread was interrupted while
     *         waiting for a stream to become available
     * @throws IOException If the next available stream cannot be open.
     */
    EventReader getEventReader() throws InterruptedException, IOException;

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
