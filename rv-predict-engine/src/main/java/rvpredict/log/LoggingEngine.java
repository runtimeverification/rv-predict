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
package rvpredict.log;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import rvpredict.config.Configuration;
import rvpredict.trace.EventType;

/**
 * Class encapsulating functionality for recording events
 *
 * @author TraianSF
 *
 */
public class LoggingEngine {

    private final AtomicLong globalEventID  = new AtomicLong(0);
    private final LoggingServer loggingServer;
    private final PredictionServer predictionServer;
    private final Configuration config;
    private volatile boolean shutdown = false;
    private final LoggingFactory loggingFactory;


    /**
     * Method invoked at the end of the logging task, to insure that
     * all data is recorded before concluding.
     */
    public void finishLogging() throws IOException, InterruptedException {
        shutdown = true;
        loggingServer.finishLogging();

        if (config.online) {
            predictionServer.finishLogging();
        }
    }

    public LoggingEngine(Configuration config) {
        this.config = config;
        if (Configuration.online) {
            loggingFactory = new OnlineLoggingFactory();
            predictionServer = startPredicting();
        } else {
            loggingFactory = new OfflineLoggingFactory(config);
            predictionServer = null;
        }
        loggingServer = startLogging();
    }

    private LoggingServer startLogging() {
        final LoggingServer loggingServer = new LoggingServer(this);
        Thread loggingServerThread = new Thread(loggingServer, "Logging server");
        loggingServer.setOwner(loggingServerThread);
        loggingServerThread.setDaemon(true);
        loggingServerThread.start();
        return loggingServer;
    }
    
    private PredictionServer startPredicting() {
        final PredictionServer predictionServer = new PredictionServer(this);
        Thread predictionServerThread = new Thread(predictionServer, "Prediction main thread");
        predictionServer.setOwner(predictionServerThread);
        predictionServerThread.setDaemon(true);
        predictionServerThread.start();
        return predictionServer;
    }

    /**
     * Logs an {@link EventItem} to the trace.
     *
     * @see EventItem#EventItem(long, long, int, long, int, long, rvpredict.trace.EventType)
     *      for a more elaborate description of the parameters.
     *
     * @param eventType  type of event being recorded
     * @param id location id of the event
     * @param addrl  additional information identifying the event
     * @param addrr additional information identifying the event
     * @param value data involved in the event
     */
    public void saveEvent(EventType eventType, int id, long addrl, int addrr, long value) {
        if (shutdown) return;

        long gid = globalEventID.incrementAndGet();
        long tid = Thread.currentThread().getId();
        EventItem e = new EventItem(gid, tid, id, addrl, addrr, value, eventType);
        loggingServer.getOutputStream().writeEvent(e);
    }

    /**
     * Wrapper for {@link #saveEvent(rvpredict.trace.EventType, int, long, int, long)}
     * The missing arguments default to 0.
     */
    public void saveEvent(EventType eventType, int locId, long arg) {
        saveEvent(eventType, locId, arg, 0, 0);
    }

    /**
     * Wrapper for {@link #saveEvent(rvpredict.trace.EventType, int, long, int, long)}
     * The missing arguments default to 0.
     */
    public void saveEvent(EventType eventType, int locId) {
        saveEvent(eventType, locId, 0, 0, 0);
    }

    public long getGlobalEventID() {
        return globalEventID.get();
    }

    public LoggingFactory getLoggingFactory() {
        return loggingFactory;
    }

    public Configuration getConfig() {
        return config;
    }
}
