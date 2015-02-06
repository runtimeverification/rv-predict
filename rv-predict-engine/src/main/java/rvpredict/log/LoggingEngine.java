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

import rvpredict.config.Configuration;
import rvpredict.engine.main.RVPredict;
import rvpredict.trace.EventType;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class encapsulating functionality for recording events
 *
 * @author TraianSF
 *
 */
public class LoggingEngine {

    private final AtomicLong globalEventID  = new AtomicLong(0);
    private final LoggingServer loggingServer;
    private final RVPredict predictionServer;
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
        if (config.profile) {
            EventStats.printEventStats();
        }

        if (Configuration.online) {
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
        loggingServer = new LoggingServer(this);
    }

    public void startLogging() {
        Thread loggingServerThread = new Thread(loggingServer, "Logging server");
        loggingServer.setOwner(loggingServerThread);
        loggingServerThread.setDaemon(true);
        loggingServerThread.start();
    }

    private RVPredict startPredicting() {
       RVPredict predictionServer = null;
        try {
            predictionServer = new RVPredict(config, loggingFactory);
        } catch (IOException | ClassNotFoundException e) {
            assert false : "These exceptions should only be thrown for offline prediction";
        }
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
        loggingServer.writeEvent(e);
    }

    public LoggingFactory getLoggingFactory() {
        return loggingFactory;
    }

    public Configuration getConfig() {
        return config;
    }
}
