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

import com.google.common.util.concurrent.Uninterruptibles;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.trace.EventType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Class encapsulating functionality for recording events
 *
 * @author TraianSF
 *
 */
public class LoggingEngine {

    private volatile boolean shutdown = false;

    private final LoggingFactory loggingFactory;

    private final LoggingTask predictionServer;

    private final MetadataLogger metadataLogger;

    private final List<EventDisruptor> disruptors = new ArrayList<>();

    private final ThreadLocalDisruptor threadLocalDisruptor = new ThreadLocalDisruptor();

    public LoggingEngine(LoggingFactory loggingFactory, LoggingTask predictionServer) {
        this.loggingFactory = loggingFactory;
        this.predictionServer = predictionServer;

        metadataLogger = Configuration.online ? null : new MetadataLogger(this);
    }

    public LoggingFactory getLoggingFactory() {
        return loggingFactory;
    }

    public void startLogging() {
        if (!Configuration.online) {
            Thread metadataLoggerThread = new Thread(metadataLogger, "Metadata logger");
            metadataLogger.setOwner(metadataLoggerThread);
            metadataLoggerThread.setDaemon(true);
            metadataLoggerThread.start();
        }
    }

    /**
     * Method invoked at the end of the logging task, to insure that
     * all data is recorded before concluding.
     */
    public void finishLogging() throws IOException, InterruptedException {
        shutdown = true;

        Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        for (EventDisruptor disruptor : disruptors) {
            disruptor.shutdown();
        }

        if (!Configuration.online) {
            metadataLogger.finishLogging();
        }

        if (Configuration.profile) {
            EventsProfiler.printEventStats();
        }

        if (Configuration.online) {
            predictionServer.finishLogging();
        }
    }

    public void startPredicting() {
        Thread predictionServerThread = new Thread(predictionServer, "Prediction main thread");
        predictionServer.setOwner(predictionServerThread);
        predictionServerThread.setDaemon(true);
        predictionServerThread.start();
    }

    /**
     * Logs an event item to the trace.
     *
     * @see {@link EventItem} for a more elaborate description of the
     *      parameters.
     */
    public void logEvent(long gid, long tid, int locId, int addrl, int addrr, long value,
            EventType eventType) {
        if (!shutdown) {
            threadLocalDisruptor.get()
                    .publishEvent(gid, tid, locId, addrl, addrr, value, eventType);
        }
    }

    private class ThreadLocalDisruptor extends ThreadLocal<EventDisruptor> {
        @Override
        protected EventDisruptor initialValue() {
            EventDisruptor disruptor = EventDisruptor.create(loggingFactory);
            disruptors.add(disruptor);
            return disruptor;
       }
    }

}
