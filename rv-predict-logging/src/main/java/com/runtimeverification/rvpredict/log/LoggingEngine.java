/*******************************************************************************
 * Copyright (c) 2013 University of Illinois
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package com.runtimeverification.rvpredict.log;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.trace.EventType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class encapsulating functionality for recording events
 *
 * @author TraianSF
 *
 */
public class LoggingEngine {

    private volatile boolean shutdown = false;

    private final LoggingFactory loggingFactory;

    private final MetadataLogger metadataLogger;

    private final List<EventWriter> eventWriters = new ArrayList<>();

    private final ThreadLocalEventWriter threadLocalEventWriter = new ThreadLocalEventWriter();

    private final FastEventProfiler eventProfiler;

    public LoggingEngine(LoggingFactory loggingFactory, LoggingTask predictionServer) {
        this.loggingFactory = loggingFactory;
        metadataLogger = new MetadataLogger(this);
        eventProfiler = Configuration.profile ? new FastEventProfiler() : null;
    }

    public LoggingFactory getLoggingFactory() {
        return loggingFactory;
    }

    public void startLogging() {
        Thread metadataLoggerThread = new Thread(metadataLogger, "Metadata logger");
        metadataLogger.setOwner(metadataLoggerThread);
        metadataLoggerThread.setDaemon(true);
        metadataLoggerThread.start();
    }

    /**
     * Method invoked at the end of the logging task, to insure that
     * all data is recorded before concluding.
     */
    public void finishLogging() throws IOException, InterruptedException {
        shutdown = true;

        synchronized (eventWriters) {
            for (EventWriter writer : eventWriters) {
                writer.close();
            }
        }

        metadataLogger.finishLogging();

        if (Configuration.profile) {
            eventProfiler.printProfilingResult();
        }
    }

    /**
     * Logs an event item to the trace.
     *
     * @see {@link EventItem} for a more elaborate description of the
     *      parameters.
     */
    public void log(EventType eventType, long gid, long tid, int locId, int addrl, int addrr,
            long value) {
        EventWriter writer = threadLocalEventWriter.get();
        if (writer != null) {
            try {
                writer.write(gid, tid, locId, addrl, addrr, value, eventType);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Updates the event profiler with the location of the new event.
     */
    public void profile(int locId) {
        eventProfiler.update(locId);
    }

    private class ThreadLocalEventWriter extends ThreadLocal<EventWriter> {
        @Override
        protected EventWriter initialValue() {
            synchronized (eventWriters) {
                if (shutdown) {
                    System.err.printf("[Warning] JVM exits before %s finishes;"
                            + " no trace from this thread is logged.%n",
                            Thread.currentThread().getName());
                    return null;
                } else {
                    try {
                        EventWriter eventWriter = loggingFactory.createEventWriter();
                        eventWriters.add(eventWriter);
                        return eventWriter;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
       }
    }

}
