package com.runtimeverification.rvpredict.log;

import java.io.IOException;

import com.runtimeverification.rvpredict.trace.EventType;

public interface ILoggingEngine {

    /**
     * Method invoked at the end of the logging task, to insure that
     * all data is recorded before concluding.
     */
    void finishLogging() throws IOException;

    /**
     * Logs an event item to the trace.
     *
     * @see {@link EventItem} for a more elaborate description of the
     *      parameters.
     */
    void log(EventType eventType, int locId, int addrl, int addrr, long value1, long value2);

}
