package com.runtimeverification.rvpredict.log;

import java.io.IOException;

public interface ILoggingEngine {

    /**
     * Method invoked at the end of the logging task, to insure that
     * all data is recorded before concluding.
     */
    void finishLogging() throws IOException;

    /**
     * Logs an event to the trace.
     *
     * @see {@link Event} for a more elaborate description of the
     *      parameters.
     */
    void log(EventType eventType, int locId, int addr1, int addr2, long value1, long value2);

}
